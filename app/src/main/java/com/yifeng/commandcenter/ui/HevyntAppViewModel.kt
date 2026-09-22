package com.yifeng.commandcenter.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.ActiveWorkoutOperations
import com.yifeng.commandcenter.data.ActiveWorkoutRepository
import com.yifeng.commandcenter.data.ActiveWorkoutSession
import com.yifeng.commandcenter.data.LikelyNextRoutineResolver
import com.yifeng.commandcenter.data.RepeatLastWorkout
import com.yifeng.commandcenter.data.RepeatLastWorkoutResult
import com.yifeng.commandcenter.data.Routine
import com.yifeng.commandcenter.data.RoutineBuildTarget
import com.yifeng.commandcenter.data.RoutineExercise
import com.yifeng.commandcenter.data.RoutineFolderSummary
import com.yifeng.commandcenter.data.RoutineLibraryImportResult
import com.yifeng.commandcenter.data.RoutineRecommendation
import com.yifeng.commandcenter.data.RoutineRepository
import com.yifeng.commandcenter.data.RoutineWorkoutMapper
import com.yifeng.commandcenter.data.WorkoutDateRange
import com.yifeng.commandcenter.data.WorkoutPlanCoordinator
import com.yifeng.commandcenter.data.WorkoutPlanParseResult
import com.yifeng.commandcenter.data.WorkoutPlanPreview
import com.yifeng.commandcenter.data.WorkoutSet
import com.yifeng.commandcenter.history.CompletedWorkoutMapper
import com.yifeng.commandcenter.history.ExerciseAutocomplete
import com.yifeng.commandcenter.history.ExerciseSuggestion
import com.yifeng.commandcenter.history.ExerciseHistorySummary
import com.yifeng.commandcenter.history.PreparedRoutineStart
import com.yifeng.commandcenter.history.PreviousLookupKey
import com.yifeng.commandcenter.history.PreviousSetPerformance
import com.yifeng.commandcenter.history.WorkoutCompletionCoordinator
import com.yifeng.commandcenter.history.WorkoutHistoryGraph
import com.yifeng.commandcenter.history.WorkoutHistoryRepository
import com.yifeng.commandcenter.history.WorkoutHistorySession
import com.yifeng.commandcenter.history.WorkoutJournalEntry
import com.yifeng.commandcenter.history.WorkoutPhotoManager
import com.yifeng.commandcenter.history.WorkoutPhotoStore
import com.yifeng.commandcenter.timer.RestTimerManager
import com.yifeng.commandcenter.timer.RestTimerState
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class HevyntAppViewModel(application: Application) : AndroidViewModel(application) {
    private val context: Context get() = getApplication<Application>().applicationContext
    private val routineRepository = RoutineRepository(application.applicationContext)
    private val activeWorkoutRepository = ActiveWorkoutRepository(application.applicationContext)
    private val workoutHistoryRepository = WorkoutHistoryRepository(application.applicationContext)
    private val workoutPhotoManager = WorkoutPhotoManager(
        workoutHistoryRepository,
        WorkoutPhotoStore(application.applicationContext),
    )
    private val workoutPlanCoordinator = WorkoutPlanCoordinator(
        context = application.applicationContext,
        routineRepository = routineRepository,
    )
    private val routineMutationMutex = Mutex()
    private val routineInitialization: Deferred<RoutineLibraryImportResult> = viewModelScope.async(
        start = CoroutineStart.LAZY,
    ) {
        routineRepository.initialize()
    }
    private var previousSetsJob: Job? = null
    private var workoutStartJob: Job? = null
    @Volatile private var preparedRoutineStart: PreparedRoutineStart? = null
    private var suggestionsJob: Job? = null
    private var timerCompletionJob: Job? = null
    private var lastPreviousLookupKey: PreviousLookupKey? = null
    private var suggestionsStale = true

    private val restoredSession = activeWorkoutRepository.loadSession()
    private val _uiState = MutableStateFlow(
        HevyntAppUiState(
            activeWorkout = restoredSession?.workout,
            isWorkoutExpanded = restoredSession == null || activeWorkoutRepository.isExpanded(),
            routineBuildTarget = restoredSession?.routineBuildTarget,
            timerState = RestTimerManager.getState(context),
        ),
    )
    val uiState: StateFlow<HevyntAppUiState> = _uiState.asStateFlow()

    private val _effects = MutableSharedFlow<HevyntAppEffect>(extraBufferCapacity = 1)
    val effects: SharedFlow<HevyntAppEffect> = _effects.asSharedFlow()

    private val unregisterWorkoutListener = activeWorkoutRepository.registerSessionListener { session ->
        val prepared = preparedRoutineStart?.takeIf { it.workoutId == session?.workout?.id }
        if (prepared != null) {
            previousSetsJob?.cancel()
            lastPreviousLookupKey = PreviousLookupKey.from(session?.workout)
        }
        _uiState.update {
            it.copy(
                activeWorkout = session?.workout,
                routineBuildTarget = session?.routineBuildTarget,
                previousSets = prepared?.previousSets ?: it.previousSets,
                exerciseHistory = prepared?.exerciseHistory ?: it.exerciseHistory,
            )
        }
        if (prepared == null) refreshDerivedWorkoutState()
    }
    private val unregisterTimerListener = RestTimerManager.registerStateListener(context) { state ->
        setTimerState(state)
    }

    init {
        observeRoutineLibrary()
        observeJournal()
        importLegacyHistory()
        recoverInterruptedFinish(activeWorkoutRepository.pendingFinishWorkoutId())
        refreshDerivedWorkoutState()
        scheduleTimerCompletion(_uiState.value.timerState)
    }

    override fun onCleared() {
        unregisterWorkoutListener()
        unregisterTimerListener()
        super.onCleared()
    }

    fun selectPage(page: AppPage) {
        _uiState.update { it.copy(selectedPage = page) }
    }

    fun openPrograms() {
        _uiState.update { it.copy(selectedPage = AppPage.Workout, selectedHistoryGraph = null) }
    }

    fun openDay(range: WorkoutDateRange) {
        _uiState.update {
            it.copy(
                selectedPage = AppPage.Journal,
                journalFilter = range,
                selectedHistoryGraph = null,
            )
        }
    }

    fun clearJournalFilter() {
        _uiState.update { it.copy(journalFilter = null) }
    }

    fun startRoutine(routineId: String) {
        if (resumeExistingWorkoutIfPresent() || workoutStartJob?.isActive == true) return
        workoutStartJob = viewModelScope.launch {
            try {
                runCatching {
                    ensureRoutineLibraryReady()
                    val routine = checkNotNull(routineRepository.routine(routineId))
                    val workout = RoutineWorkoutMapper.fromRoutine(routine)
                    val (previousSets, exerciseHistory) = withContext(Dispatchers.IO) {
                        val p = workoutHistoryRepository.previousSets(workout)
                        val e = workoutHistoryRepository.exerciseHistory(workout)
                        p to e
                    }
                    preparedRoutineStart = PreparedRoutineStart(workout.id, previousSets, exerciseHistory)
                    val started = withContext(Dispatchers.IO) {
                        activeWorkoutRepository.startIfIdle(ActiveWorkoutSession(workout))
                    }
                    Triple(workout, previousSets to exerciseHistory, started)
                }.onSuccess { (workout, historyPair, started) ->
                    if (started) {
                        openStartedSession(
                            workout,
                            target = null,
                            preparedPreviousSets = historyPair.first,
                            preparedExerciseHistory = historyPair.second,
                        )
                    } else {
                        resumePersistedWorkout()
                    }
                }.onFailure { failure ->
                    if (failure is CancellationException) throw failure
                    showError("This routine could not be started. Your saved routines were not changed.")
                }
            } finally {
                preparedRoutineStart = null
            }
        }
    }

    fun startEmptyWorkout() {
        if (resumeExistingWorkoutIfPresent()) return
        viewModelScope.launch {
            val workout = ActiveWorkout(routineName = "Empty workout", exercises = emptyList())
            val started = withContext(Dispatchers.IO) {
                activeWorkoutRepository.startIfIdle(ActiveWorkoutSession(workout))
            }
            if (started) openStartedSession(workout, target = null) else resumePersistedWorkout()
        }
    }

    fun repeatLastWorkout() {
        if (resumeExistingWorkoutIfPresent() || workoutStartJob?.isActive == true) return
        workoutStartJob = viewModelScope.launch {
            try {
                val repeat = RepeatLastWorkout(workoutHistoryRepository, activeWorkoutRepository)
                when (val result = repeat()) {
                    is RepeatLastWorkoutResult.Started -> {
                        val (previousSets, exerciseHistory) = withContext(Dispatchers.IO) {
                            val p = workoutHistoryRepository.previousSets(result.workout)
                            val e = workoutHistoryRepository.exerciseHistory(result.workout)
                            p to e
                        }
                        openStartedSession(
                            result.workout,
                            target = null,
                            preparedPreviousSets = previousSets,
                            preparedExerciseHistory = exerciseHistory,
                        )
                    }
                    RepeatLastWorkoutResult.ActiveWorkoutExists -> {
                        resumePersistedWorkout()
                    }
                    RepeatLastWorkoutResult.NoHistory -> {
                        showError("No previous workout to repeat.")
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                showError("The previous workout could not be repeated.")
            }
        }
    }

    fun buildRoutine(folderId: String, routineId: String?) {
        if (_uiState.value.activeWorkout != null) {
            showError("Finish or discard the active workout before editing a routine.")
            return
        }
        viewModelScope.launch {
            runCatching {
                ensureRoutineLibraryReady()
                val routine = routineId?.let { checkNotNull(routineRepository.routine(it)) }
                val target = RoutineBuildTarget(folderId, routineId)
                val workout = routine?.let(RoutineWorkoutMapper::forEditing)
                    ?: ActiveWorkout(routineName = "", exercises = emptyList())
                val started = withContext(Dispatchers.IO) {
                    activeWorkoutRepository.startIfIdle(ActiveWorkoutSession(workout, target))
                }
                Triple(workout, target, started)
            }.onSuccess { (workout, target, started) ->
                if (started) openStartedSession(workout, target) else resumePersistedWorkout()
            }.onFailure {
                showError("This routine could not be opened. Your saved routines were not changed.")
            }
        }
    }

    fun saveBuiltRoutine(name: String) {
        val target = _uiState.value.routineBuildTarget ?: return
        val workout = _uiState.value.activeWorkout ?: return
        val routine = Routine(
            id = target.routineId ?: UUID.randomUUID().toString(),
            name = name,
            exercises = workout.exercises.mapNotNull { exercise ->
                exercise.name.trim().takeIf(String::isNotEmpty)?.let { exerciseName ->
                    RoutineExercise(
                        id = exercise.id,
                        name = exerciseName,
                        notes = exercise.notes,
                        restSeconds = exercise.restSeconds,
                        sets = exercise.sets.map { it.copy(completed = false) },
                    )
                }
            },
        )
        viewModelScope.launch {
            runCatching {
                routineMutationMutex.withLock {
                    ensureRoutineLibraryReady()
                    routineRepository.saveRoutine(
                        folderId = target.folderId,
                        routine = routine,
                        existingRoutineId = target.routineId,
                    )
                }
            }.onSuccess {
                clearSession()
            }.onFailure {
                showError("The routine could not be saved. Your routine draft is still available.")
            }
        }
    }

    fun createFolder(name: String) {
        viewModelScope.launch {
            runCatching {
                routineMutationMutex.withLock {
                    ensureRoutineLibraryReady()
                    routineRepository.createFolder(name)
                }
            }.onFailure { showError("The folder could not be created.") }
        }
    }

    fun renameFolder(folderId: String, name: String) {
        viewModelScope.launch {
            runCatching {
                routineMutationMutex.withLock {
                    ensureRoutineLibraryReady()
                    routineRepository.renameFolder(folderId, name)
                }
            }.onFailure { showError("The folder could not be renamed.") }
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            runCatching {
                routineMutationMutex.withLock {
                    ensureRoutineLibraryReady()
                    routineRepository.deleteFolder(folderId)
                }
            }.onFailure { showError("The folder could not be deleted.") }
        }
    }

    fun deleteRoutine(folderId: String, routineId: String) {
        viewModelScope.launch {
            runCatching {
                routineMutationMutex.withLock {
                    ensureRoutineLibraryReady()
                    routineRepository.deleteRoutine(routineId)
                }
            }.onFailure { showError("The routine could not be deleted.") }
        }
    }

    fun requestExerciseSuggestions() {
        if (!suggestionsStale || suggestionsJob?.isActive == true) return
        suggestionsJob = viewModelScope.launch {
            val catalog = withContext(Dispatchers.Default) {
                val folders = routineRepository.folders()
                val detailedHistory = workoutHistoryRepository
                    .graphsCompletedBefore(Long.MAX_VALUE)
                    .filterNot { it.session.isLegacySummaryOnly }
                ExerciseAutocomplete.catalog(folders, detailedHistory)
            }
            _uiState.update {
                it.copy(exerciseSuggestions = catalog)
            }
            suggestionsStale = false
        }
    }

    fun addExercise(suggestion: ExerciseSuggestion) = updateActiveWorkout { workout ->
        ActiveWorkoutOperations.addExercise(workout, ExerciseAutocomplete.toActiveExercise(suggestion))
    }

    fun changeExerciseName(exerciseId: String, name: String) = updateActiveWorkout {
        ActiveWorkoutOperations.updateExerciseName(it, exerciseId, name)
    }

    fun changeExerciseNotes(exerciseId: String, notes: String) = updateActiveWorkout {
        ActiveWorkoutOperations.updateExerciseNotes(it, exerciseId, notes)
    }

    fun changeExerciseRest(exerciseId: String, seconds: Int) = updateActiveWorkout {
        ActiveWorkoutOperations.updateRestSeconds(it, exerciseId, seconds)
    }

    fun removeExercise(exerciseId: String) = updateActiveWorkout {
        ActiveWorkoutOperations.removeExercise(it, exerciseId)
    }

    fun moveExercise(exerciseId: String, offset: Int) = updateActiveWorkout {
        ActiveWorkoutOperations.moveExercise(it, exerciseId, offset)
    }

    fun changeSet(exerciseId: String, setId: String, updatedSet: WorkoutSet) =
        updateActiveWorkout { workout ->
            ActiveWorkoutOperations.updateSet(workout, exerciseId, setId) { current ->
                current.copy(weight = updatedSet.weight, reps = updatedSet.reps)
            }
        }

    fun changeSetCompletion(
        exerciseId: String,
        setId: String,
        completed: Boolean,
        canPostNotifications: Boolean,
    ) {
        if (completed) {
            activeWorkoutRepository.completeSet(exerciseId, setId)?.let { result ->
                _uiState.update { it.copy(activeWorkout = result.workout) }
                if (_uiState.value.routineBuildTarget == null && result.nextTarget != null) {
                    if (!canPostNotifications) {
                        _effects.tryEmit(HevyntAppEffect.RequestNotificationPermission)
                    }
                    RestTimerManager.start(
                        context = context,
                        durationSeconds = result.restSeconds,
                        completedExerciseId = result.completedExerciseId,
                        completedSetId = result.completedSetId,
                        exerciseName = result.completedExerciseName,
                    )
                    setTimerState(RestTimerManager.getState(context))
                } else if (result.nextTarget == null) {
                    RestTimerManager.cancel(context)
                    setTimerState(null)
                }
            }
        } else {
            updateActiveWorkout { ActiveWorkoutOperations.uncompleteSet(it, exerciseId, setId) }
            _uiState.value.timerState
                ?.takeIf { it.completedExerciseId == exerciseId && it.completedSetId == setId }
                ?.let {
                    RestTimerManager.cancel(context)
                    setTimerState(null)
                }
        }
    }

    fun removeSet(exerciseId: String, setId: String) {
        activeWorkoutRepository.update {
            ActiveWorkoutOperations.removeSet(it, exerciseId, setId)
        }?.let { updated ->
            _uiState.update { it.copy(activeWorkout = updated) }
            RestTimerManager.reconcileRemovedSet(context, setId)
            setTimerState(RestTimerManager.getState(context))
        }
    }

    fun addSet(exerciseId: String) = updateActiveWorkout {
        ActiveWorkoutOperations.addSet(it, exerciseId)
    }

    fun duplicateSet(exerciseId: String, setId: String) = updateActiveWorkout {
        ActiveWorkoutOperations.duplicateSet(it, exerciseId, setId)
    }

    fun finishOrSaveRoutine() {
        val state = _uiState.value
        val workout = state.activeWorkout ?: return
        if (state.routineBuildTarget != null) {
            _uiState.update { it.copy(showRoutineNameDialog = true) }
            return
        }
        val draft = CompletedWorkoutMapper.fromActiveWorkout(workout, System.currentTimeMillis())
        when {
            draft.graph.session.completedSetCount == 0 ->
                _uiState.update { it.copy(finishPrompt = FinishPrompt.Empty) }

            draft.unfinishedSetCount > 0 ->
                _uiState.update { it.copy(finishPrompt = FinishPrompt.Unfinished) }

            else -> saveFinishedWorkout(workout)
        }
    }

    fun dismissRoutineNameDialog() {
        _uiState.update { it.copy(showRoutineNameDialog = false) }
    }

    fun dismissFinishPrompt() {
        _uiState.update { it.copy(finishPrompt = null) }
    }

    fun confirmFinishPrompt() {
        when (_uiState.value.finishPrompt) {
            FinishPrompt.Unfinished -> _uiState.value.activeWorkout?.let(::saveFinishedWorkout)
            FinishPrompt.Empty -> {
                _uiState.update { it.copy(finishPrompt = null) }
                clearSession()
            }

            null -> Unit
        }
    }

    fun minimizeWorkout() {
        _uiState.update { it.copy(isWorkoutExpanded = false) }
        activeWorkoutRepository.setExpanded(false)
    }

    fun resumeWorkout() {
        _uiState.update { it.copy(isWorkoutExpanded = true) }
        activeWorkoutRepository.setExpanded(true)
    }

    fun discardWorkout() = clearSession()

    fun openHistorySession(sessionId: String) {
        _uiState.update { it.copy(errorMessage = null) }
        viewModelScope.launch {
            val graph = workoutHistoryRepository.graph(sessionId)
            if (graph == null) {
                showError("This workout could not be opened.")
            } else {
                _uiState.update {
                    it.copy(selectedPage = AppPage.Journal, selectedHistoryGraph = graph)
                }
            }
        }
    }

    fun goBackFromHistory() {
        _uiState.update { it.copy(selectedHistoryGraph = null) }
    }

    fun requestDeleteHistory() {
        _uiState.update { it.copy(showDeleteHistoryDialog = true) }
    }

    fun dismissDeleteHistory() {
        _uiState.update { it.copy(showDeleteHistoryDialog = false) }
    }

    fun confirmDeleteHistory() {
        val sessionId = _uiState.value.selectedHistoryGraph?.session?.id ?: return
        _uiState.update { it.copy(showDeleteHistoryDialog = false) }
        viewModelScope.launch {
            runCatching { workoutPhotoManager.deleteSession(sessionId) }
                .onSuccess { _uiState.update { it.copy(selectedHistoryGraph = null) } }
                .onFailure { showError(it.message ?: "The workout could not be deleted.") }
        }
    }

    fun selectRecapPhoto(uri: Uri) {
        val recap = _uiState.value.recapGraph ?: return
        _uiState.update { it.copy(isPhotoBusy = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                workoutPhotoManager.replaceFromPicker(
                    sessionId = recap.session.id,
                    contentResolver = context.contentResolver,
                    uri = uri,
                )
            }.onSuccess { savedPhoto ->
                val graph = workoutHistoryRepository.graph(recap.session.id)
                val file = withContext(Dispatchers.IO) {
                    workoutPhotoManager.existingFile(savedPhoto)
                }
                _uiState.update {
                    it.copy(
                        recapGraph = graph,
                        journalPhotoFiles = file?.let { photoFile ->
                            it.journalPhotoFiles + (savedPhoto.id to photoFile)
                        } ?: it.journalPhotoFiles,
                    )
                }
            }.onFailure {
                showError(it.message ?: "The photo could not be saved.")
            }
            _uiState.update { it.copy(isPhotoBusy = false) }
        }
    }

    fun removeRecapPhoto() {
        val recap = _uiState.value.recapGraph ?: return
        val photo = recap.photos.firstOrNull() ?: return
        _uiState.update { it.copy(isPhotoBusy = true) }
        viewModelScope.launch {
            runCatching { workoutPhotoManager.remove(photo) }
                .onSuccess {
                    _uiState.update {
                        it.copy(recapGraph = workoutHistoryRepository.graph(recap.session.id))
                    }
                }
                .onFailure { showError(it.message ?: "The photo could not be removed.") }
            _uiState.update { it.copy(isPhotoBusy = false) }
        }
    }

    fun finishRecap(note: String) {
        val recap = _uiState.value.recapGraph ?: return
        viewModelScope.launch {
            runCatching {
                check(workoutHistoryRepository.updateNote(recap.session.id, note)) {
                    "The saved workout no longer exists."
                }
            }.onSuccess {
                _uiState.update {
                    it.copy(recapGraph = null, selectedPage = AppPage.Journal)
                }
            }.onFailure { showError(it.message ?: "The note could not be saved.") }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun notificationPermissionResult(granted: Boolean) {
        if (granted) RestTimerManager.refresh(context)
    }

    fun subtractRestTime() {
        RestTimerManager.adjust(context, -15)
        setTimerState(RestTimerManager.getState(context))
    }

    fun addRestTime() {
        RestTimerManager.adjust(context, 15)
        setTimerState(RestTimerManager.getState(context))
    }

    fun skipRestTime() {
        RestTimerManager.skip(context)
        setTimerState(null)
    }

    private fun observeRoutineLibrary() {
        viewModelScope.launch {
            routineRepository.observeSummaries().collectLatest { folders ->
                _uiState.update { state ->
                    val resolved = LikelyNextRoutineResolver.resolve(
                        folders = folders,
                        history = state.workoutHistory,
                    )
                    state.copy(
                        routineFolders = resolved.folders,
                        likelyNextRoutine = resolved.recommendation,
                    )
                }
                suggestionsStale = true
            }
        }
        viewModelScope.launch {
            val result = runCatching { routineInitialization.await() }.getOrElse {
                showError("The routine library could not be loaded. Your saved data was left unchanged.")
                return@launch
            }
            when (result) {
                is RoutineLibraryImportResult.CorruptLegacyData -> showError(
                    "Saved routine data is damaged. It was left untouched so it can be recovered.",
                )

                RoutineLibraryImportResult.AlreadyImported,
                RoutineLibraryImportResult.Imported,
                -> Unit
            }
        }
    }

    private fun observeJournal() {
        viewModelScope.launch {
            workoutHistoryRepository.observeJournalEntries().collectLatest { entries ->
                val files = withContext(Dispatchers.IO) {
                    entries.mapNotNull { entry ->
                        entry.photo?.let { photo ->
                            workoutPhotoManager.existingFile(photo)?.let { file -> photo.id to file }
                        }
                    }.toMap()
                }
                val sessions = entries.map(WorkoutJournalEntry::session)
                _uiState.update { state ->
                    val resolved = LikelyNextRoutineResolver.resolve(
                        folders = state.routineFolders,
                        history = sessions,
                    )
                    state.copy(
                        routineFolders = resolved.folders,
                        likelyNextRoutine = resolved.recommendation,
                        workoutHistory = sessions,
                        journalEntries = entries,
                        journalPhotoFiles = files,
                    )
                }
                suggestionsStale = true
                // Completed-history changes can alter PREVIOUS even when today's exercise structure
                // is unchanged, so force the compact lookup to refresh against the new Room rows.
                lastPreviousLookupKey = null
                refreshDerivedWorkoutState()
            }
        }
    }

    private fun importLegacyHistory() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    workoutHistoryRepository.importLegacySummaries(
                        routineRepository.loadWorkoutHistory(),
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                // The history DAO import is transactional and safe to retry on a later launch.
            }
        }
    }

    private fun recoverInterruptedFinish(pendingFinishId: String?) {
        if (pendingFinishId == null) return
        viewModelScope.launch {
            try {
                val interrupted = withContext(Dispatchers.IO) {
                    activeWorkoutRepository.loadSession()
                }
                if (interrupted?.workout?.id == pendingFinishId) {
                    val committedGraph = workoutHistoryRepository.graph(pendingFinishId)
                    if (committedGraph != null) {
                        val cleared = withContext(Dispatchers.IO) {
                            activeWorkoutRepository.clearFinished(pendingFinishId)
                        }
                        if (cleared) {
                            RestTimerManager.cancel(context)
                            setTimerState(null)
                            _uiState.update {
                                it.copy(
                                    activeWorkout = null,
                                    routineBuildTarget = null,
                                    recapGraph = committedGraph,
                                )
                            }
                        }
                    } else {
                        withContext(Dispatchers.IO) {
                            activeWorkoutRepository.cancelFinishingIfUnchanged(
                                workoutId = interrupted.workout.id,
                                expectedMarker = pendingFinishId,
                            )
                        }
                    }
                } else {
                    withContext(Dispatchers.IO) {
                        activeWorkoutRepository.cancelFinishing(pendingFinishId)
                        activeWorkoutRepository.clearStaleFinishingMarker()
                    }
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Throwable) {
                // Keep the durable marker; the next launch can verify the Room commit again.
            }
        }
    }

    private fun refreshDerivedWorkoutState() {
        val workout = _uiState.value.activeWorkout
        if (workout == null) {
            previousSetsJob?.cancel()
            lastPreviousLookupKey = null
            _uiState.update { it.copy(previousSets = emptyMap(), exerciseHistory = emptyMap()) }
            return
        }
        val lookupKey = PreviousLookupKey.from(workout)
        if (lookupKey == lastPreviousLookupKey) return
        lastPreviousLookupKey = lookupKey
        previousSetsJob?.cancel()
        previousSetsJob = viewModelScope.launch {
            val (previousSets, exerciseHistory) = withContext(Dispatchers.Default) {
                val p = workoutHistoryRepository.previousSets(workout)
                val e = workoutHistoryRepository.exerciseHistory(workout)
                p to e
            }
            if (PreviousLookupKey.from(_uiState.value.activeWorkout) == lookupKey) {
                _uiState.update { it.copy(previousSets = previousSets, exerciseHistory = exerciseHistory) }
            }
        }
    }

    private fun openStartedSession(
        workout: ActiveWorkout,
        target: RoutineBuildTarget?,
        preparedPreviousSets: Map<String, PreviousSetPerformance>? = null,
        preparedExerciseHistory: Map<String, ExerciseHistorySummary>? = null,
    ) {
        activeWorkoutRepository.setExpanded(true)
        if (preparedPreviousSets != null) {
            previousSetsJob?.cancel()
            lastPreviousLookupKey = PreviousLookupKey.from(workout)
        }
        _uiState.update {
            it.copy(
                activeWorkout = workout,
                routineBuildTarget = target,
                isWorkoutExpanded = true,
                selectedPage = AppPage.Workout,
                previousSets = preparedPreviousSets ?: it.previousSets,
                exerciseHistory = preparedExerciseHistory ?: it.exerciseHistory,
            )
        }
        if (preparedPreviousSets == null) refreshDerivedWorkoutState()
    }

    private fun resumePersistedWorkout() {
        val persisted = activeWorkoutRepository.loadSession()
        if (persisted == null) {
            showError("The workout could not be started. Please try again.")
            return
        }
        openStartedSession(persisted.workout, persisted.routineBuildTarget)
    }

    private fun clearSession() {
        RestTimerManager.cancel(context)
        activeWorkoutRepository.clear()
        setTimerState(null)
        _uiState.update {
            it.copy(
                activeWorkout = null,
                routineBuildTarget = null,
                showRoutineNameDialog = false,
                finishPrompt = null,
            )
        }
    }

    private fun resumeExistingWorkoutIfPresent(): Boolean {
        if (_uiState.value.activeWorkout == null) return false
        resumeWorkout()
        return true
    }

    private fun updateActiveWorkout(transform: (ActiveWorkout) -> ActiveWorkout?) {
        activeWorkoutRepository.update(transform)?.let { updated ->
            _uiState.update { it.copy(activeWorkout = updated) }
            refreshDerivedWorkoutState()
        }
    }

    private fun saveFinishedWorkout(workout: ActiveWorkout) {
        if (_uiState.value.isWorkoutSaving) return
        _uiState.update {
            it.copy(isWorkoutSaving = true, finishPrompt = null, errorMessage = null)
        }
        viewModelScope.launch {
            val coordinator = WorkoutCompletionCoordinator(
                saveGraph = workoutHistoryRepository::saveCompletedWorkout,
                beginFinishing = { candidate ->
                    withContext(Dispatchers.IO) { activeWorkoutRepository.beginFinishing(candidate) }
                },
                clearActiveWorkout = {
                    withContext(Dispatchers.IO) {
                        check(activeWorkoutRepository.clearFinished(workout.id)) {
                            "The active workout changed before it could be cleared."
                        }
                    }
                },
                cancelFinishing = { workoutId ->
                    withContext(Dispatchers.IO) {
                        activeWorkoutRepository.cancelFinishing(workoutId)
                    }
                },
            )
            coordinator.finish(workout, System.currentTimeMillis())
                .onSuccess { graph ->
                    RestTimerManager.cancel(context)
                    setTimerState(null)
                    _uiState.update {
                        it.copy(
                            activeWorkout = null,
                            routineBuildTarget = null,
                            showRoutineNameDialog = false,
                            finishPrompt = null,
                            recapGraph = graph,
                        )
                    }
                }
                .onFailure {
                    showError("The workout could not be saved. Your active workout is still available.")
                }
            _uiState.update { it.copy(isWorkoutSaving = false) }
        }
    }

    private fun setTimerState(state: RestTimerState?) {
        _uiState.update { it.copy(timerState = state) }
        scheduleTimerCompletion(state)
    }

    private fun scheduleTimerCompletion(state: RestTimerState?) {
        timerCompletionJob?.cancel()
        val expectedEndMillis = state?.endMillis ?: return
        timerCompletionJob = viewModelScope.launch {
            delay((expectedEndMillis - System.currentTimeMillis()).coerceAtLeast(0L))
            RestTimerManager.complete(context, expectedEndMillis)
            setTimerState(RestTimerManager.getState(context))
        }
    }

    fun loadWorkoutPlanFromUri(uri: Uri) {
        viewModelScope.launch {
            when (val result = workoutPlanCoordinator.loadPlanFromUri(uri)) {
                is WorkoutPlanParseResult.Failure -> {
                    _uiState.update { it.copy(workoutPlanImportError = result.error) }
                }
                is WorkoutPlanParseResult.Success -> {
                    handleParsedWorkoutPlan(result.plan)
                }
            }
        }
    }

    fun loadWorkoutPlanFromJson(json: String) {
        viewModelScope.launch {
            when (val result = workoutPlanCoordinator.loadPlanFromJson(json)) {
                is WorkoutPlanParseResult.Failure -> {
                    _uiState.update { it.copy(workoutPlanImportError = result.error) }
                }
                is WorkoutPlanParseResult.Success -> {
                    handleParsedWorkoutPlan(result.plan)
                }
            }
        }
    }

    private suspend fun handleParsedWorkoutPlan(plan: com.yifeng.commandcenter.data.WorkoutPlanDto) {
        ensureRoutineLibraryReady()
        val exists = workoutPlanCoordinator.checkPlanExists(plan.programId)
        _uiState.update {
            it.copy(
                workoutPlanPreview = WorkoutPlanPreview(
                    plan = plan,
                    alreadyExists = exists,
                ),
                workoutPlanImportError = null,
            )
        }
    }

    fun dismissWorkoutPlanPreview() {
        _uiState.update { it.copy(workoutPlanPreview = null) }
    }

    fun dismissWorkoutPlanImportError() {
        _uiState.update { it.copy(workoutPlanImportError = null) }
    }

    fun confirmWorkoutPlanImport(replace: Boolean = false) {
        val preview = _uiState.value.workoutPlanPreview ?: return
        viewModelScope.launch {
            runCatching {
                routineMutationMutex.withLock {
                    ensureRoutineLibraryReady()
                    workoutPlanCoordinator.importPlan(preview.plan, replaceExisting = replace)
                }
            }.onSuccess { success ->
                if (success) {
                    _uiState.update { it.copy(workoutPlanPreview = null) }
                } else {
                    _uiState.update {
                        it.copy(
                            workoutPlanPreview = null,
                            workoutPlanImportError = "Program '${preview.programName}' already exists.",
                        )
                    }
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        workoutPlanPreview = null,
                        workoutPlanImportError = "Failed to import plan: ${error.message}",
                    )
                }
            }
        }
    }

    private fun showError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    private suspend fun ensureRoutineLibraryReady() {
        val result = routineInitialization.await()
        if (result is RoutineLibraryImportResult.CorruptLegacyData) throw result.cause
    }
}