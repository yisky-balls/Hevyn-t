package com.yifeng.commandcenter.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.ActiveWorkoutOperations
import com.yifeng.commandcenter.data.ActiveWorkoutRepository
import com.yifeng.commandcenter.data.AppBackupManager
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
import com.yifeng.commandcenter.data.SavedExercisesDraft
import com.yifeng.commandcenter.data.SavedExercisesOperations
import com.yifeng.commandcenter.data.WorkoutDateRange
import com.yifeng.commandcenter.data.WorkoutPlanCoordinator
import com.yifeng.commandcenter.data.WorkoutPlanParseResult
import com.yifeng.commandcenter.data.WorkoutPlanPreview
import com.yifeng.commandcenter.data.WorkoutSet
import com.yifeng.commandcenter.history.CompletedWorkoutMapper
import com.yifeng.commandcenter.history.ExerciseAutocomplete
import com.yifeng.commandcenter.history.ExerciseProgressCalculator
import com.yifeng.commandcenter.history.ExerciseProgressMetric
import com.yifeng.commandcenter.history.ExerciseProgressPeriod
import com.yifeng.commandcenter.history.ExerciseProgressPreferences
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
import com.yifeng.commandcenter.timer.RestTimerPreferences
import com.yifeng.commandcenter.timer.RestTimerState
import com.yifeng.commandcenter.timer.SoftLockAccessibilityService
import android.content.SharedPreferences
import com.yifeng.commandcenter.glyph.GlyphDeviceSupport
import com.yifeng.commandcenter.glyph.GlyphPreferences
import com.yifeng.commandcenter.glyph.GlyphRestTimerController
import com.yifeng.commandcenter.metrics.BodyMetricsPeriod
import com.yifeng.commandcenter.metrics.BodyMetricsRepository
import com.yifeng.commandcenter.metrics.BodyMetricsSyncOutcome
import com.yifeng.commandcenter.metrics.BodyMetricsSyncState
import com.yifeng.commandcenter.program.ProgramSheetRepository
import com.yifeng.commandcenter.program.ProgramSheetState
import com.yifeng.commandcenter.program.ProgramSheetSyncOutcome
import com.yifeng.commandcenter.program.ProgramSheetWorkoutMapper
import com.yifeng.commandcenter.widget.WorkoutWidgetUpdateDispatcher
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
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
    private val bodyMetricsRepository = BodyMetricsRepository(application.applicationContext)
    private val programSheetRepository = ProgramSheetRepository(application.applicationContext)
    private val exerciseProgressPreferences = ExerciseProgressPreferences(application.applicationContext)
    private var bodyMetricsSyncJob: Job? = null
    private var programSheetSyncJob: Job? = null
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

    // --- Nothing Glyph integration (Phone 4a) ---
    val isGlyphSupported: Boolean = GlyphDeviceSupport.isSupported()
    val glyphController: GlyphRestTimerController? = if (isGlyphSupported) {
        GlyphRestTimerController(context).also { it.init() }
    } else null
    private var glyphUpdateJob: Job? = null

    private val restoredSession = activeWorkoutRepository.loadSession()
    private val initialBodyMetrics = bodyMetricsRepository.loadCachedState()
    private val initialProgramSheet = programSheetRepository.loadCachedState()
    private val _uiState = MutableStateFlow(
        HevyntAppUiState(
            activeWorkout = restoredSession?.workout,
            isWorkoutExpanded = restoredSession == null || activeWorkoutRepository.isExpanded(),
            routineBuildTarget = restoredSession?.routineBuildTarget,
            timerState = RestTimerManager.getState(context),
            isGlyphSupported = isGlyphSupported,
            isGlyphTimerEnabled = GlyphPreferences.isTimerEnabled(context),
            isGlyphPulseEnabled = GlyphPreferences.isPulseOnComplete(context),
            isSoftLockSupported = SoftLockAccessibilityService.isSupportedOnDevice,
            isSoftLockEnabled = RestTimerPreferences.isSoftLockOnCompleteEnabled(context),
            isSoftLockServiceEnabled = SoftLockAccessibilityService.isEnabledInSettings(context),
            bodyMetrics = initialBodyMetrics,
            programSheet = initialProgramSheet,
            selectedProgressExerciseKey = exerciseProgressPreferences.getSelectedExerciseKey(),
            exerciseProgressMetric = exerciseProgressPreferences.getSelectedMetric(),
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

    private val glyphPrefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == GlyphPreferences.KEY_TIMER_ENABLED) {
            val enabled = GlyphPreferences.isTimerEnabled(context)
            if (_uiState.value.isGlyphTimerEnabled != enabled) {
                _uiState.update { it.copy(isGlyphTimerEnabled = enabled) }
                if (!enabled) {
                    glyphUpdateJob?.cancel()
                    glyphController?.turnOff()
                } else {
                    val state = _uiState.value.timerState
                    glyphController?.updateTimer(state)
                    scheduleGlyphUpdates(state)
                }
            }
        } else if (key == GlyphPreferences.KEY_PULSE_ON_COMPLETE) {
            _uiState.update { it.copy(isGlyphPulseEnabled = GlyphPreferences.isPulseOnComplete(context)) }
        }
    }

    init {
        GlyphPreferences.registerListener(context, glyphPrefsListener)
        observeRoutineLibrary()
        observeJournal()
        observeExerciseProgress()
        importLegacyHistory()
        recoverInterruptedFinish(activeWorkoutRepository.pendingFinishWorkoutId())
        refreshDerivedWorkoutState()
        scheduleTimerCompletion(_uiState.value.timerState)
        syncBodyMetricsIfStale()
    }

    override fun onCleared() {
        unregisterWorkoutListener()
        unregisterTimerListener()
        GlyphPreferences.unregisterListener(context, glyphPrefsListener)
        glyphUpdateJob?.cancel()
        glyphController?.shutdown()
        super.onCleared()
    }

    fun selectPage(page: AppPage) {
        _uiState.update { it.copy(selectedPage = page) }
        if (page == AppPage.Workout) {
            syncProgramSheetIfStale()
        }
    }

    fun openPrograms() {
        _uiState.update { it.copy(selectedPage = AppPage.Workout, selectedHistoryGraph = null) }
        syncProgramSheetIfStale()
    }

    fun openJournal() {
        _uiState.update {
            it.copy(
                selectedPage = AppPage.Journal,
                journalFilter = null,
                selectedHistoryGraph = null,
            )
        }
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
                    val baseWorkout = RoutineWorkoutMapper.fromRoutine(routine)
                    val workout = ProgramSheetWorkoutMapper.applySheetTargetsToWorkout(
                        workout = baseWorkout,
                        week = _uiState.value.programSheet.activeWeek,
                    )
                    val (previousSets, exerciseHistory) = withContext(Dispatchers.IO) {
                        workoutHistoryRepository.workoutHistoryContext(workout)
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
                            workoutHistoryRepository.workoutHistoryContext(result.workout)
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

    fun reorderRoutines(folderId: String, orderedRoutineIds: List<String>) {
        if (orderedRoutineIds.size < 2) return
        _uiState.update { state ->
            state.copy(
                routineFolders = state.routineFolders.map { folder ->
                    if (folder.id != folderId) {
                        folder
                    } else {
                        val byId = folder.routines.associateBy { it.id }
                        val reordered = orderedRoutineIds.mapNotNull(byId::get) +
                            folder.routines.filter { it.id !in orderedRoutineIds }
                        folder.copy(routines = reordered)
                    }
                },
            )
        }
        viewModelScope.launch {
            runCatching {
                routineMutationMutex.withLock {
                    ensureRoutineLibraryReady()
                    routineRepository.reorderRoutines(folderId, orderedRoutineIds)
                }
            }.onFailure { showError("Routine order could not be saved.") }
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
            refreshDerivedWorkoutState()
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

    fun selectHistoryPhoto(uri: Uri) {
        val selected = _uiState.value.selectedHistoryGraph ?: return
        _uiState.update { it.copy(isPhotoBusy = true, errorMessage = null) }
        viewModelScope.launch {
            runCatching {
                workoutPhotoManager.replaceFromPicker(
                    sessionId = selected.session.id,
                    contentResolver = context.contentResolver,
                    uri = uri,
                )
            }.onSuccess { savedPhoto ->
                val graph = workoutHistoryRepository.graph(selected.session.id)
                val file = withContext(Dispatchers.IO) {
                    workoutPhotoManager.existingFile(savedPhoto)
                }
                _uiState.update {
                    it.copy(
                        selectedHistoryGraph = graph,
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

    fun removeHistoryPhoto() {
        val selected = _uiState.value.selectedHistoryGraph ?: return
        val photo = selected.photos.firstOrNull() ?: return
        _uiState.update { it.copy(isPhotoBusy = true) }
        viewModelScope.launch {
            runCatching { workoutPhotoManager.remove(photo) }
                .onSuccess {
                    val graph = workoutHistoryRepository.graph(selected.session.id)
                    _uiState.update {
                        it.copy(
                            selectedHistoryGraph = graph,
                            journalPhotoFiles = it.journalPhotoFiles - photo.id,
                        )
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

    fun dismissBackupStatus() {
        _uiState.update { it.copy(backupStatusMessage = null) }
    }

    fun exportFullBackup(destinationUri: Uri) {
        if (_uiState.value.isBackupBusy) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBackupBusy = true, errorMessage = null, backupStatusMessage = null) }
            val result = withContext(Dispatchers.IO) {
                AppBackupManager.exportBackupToUri(context, destinationUri)
            }
            result.onSuccess { count ->
                _uiState.update {
                    it.copy(
                        isBackupBusy = false,
                        backupStatusMessage = "Backup saved ($count files: workouts, routines, settings & photos).",
                    )
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isBackupBusy = false) }
                showError(error.message ?: "Backup could not be exported.")
            }
        }
    }

    fun importFullBackup(sourceUri: Uri) {
        if (_uiState.value.isBackupBusy) return
        viewModelScope.launch {
            _uiState.update { it.copy(isBackupBusy = true, errorMessage = null, backupStatusMessage = null) }
            val result = withContext(Dispatchers.IO) {
                AppBackupManager.importBackupFromUri(context, sourceUri)
            }
            result.onSuccess {
                _effects.tryEmit(HevyntAppEffect.RestartAfterBackupRestore)
            }.onFailure { error ->
                _uiState.update { it.copy(isBackupBusy = false) }
                showError(error.message ?: "Backup could not be imported.")
            }
        }
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

    fun setSoftLockEnabled(enabled: Boolean) {
        RestTimerPreferences.setSoftLockOnCompleteEnabled(context, enabled)
        val serviceEnabled = SoftLockAccessibilityService.isEnabledInSettings(context)
        _uiState.update {
            it.copy(
                isSoftLockEnabled = enabled,
                isSoftLockServiceEnabled = serviceEnabled,
            )
        }
        RestTimerManager.refresh(context)
    }

    fun openSoftLockAccessibilitySettings() {
        SoftLockAccessibilityService.openAccessibilitySettings(context)
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
                        syncFinishedWorkoutToRoutine(interrupted.workout)
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
            val (previousSets, exerciseHistory) = withContext(Dispatchers.IO) {
                workoutHistoryRepository.workoutHistoryContext(workout)
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
                isSavedExercisesOpen = false,
                savedExercisesDraft = null,
                showDiscardSavedExercisesConfirm = false,
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
                    syncFinishedWorkoutToRoutine(workout)
                    maybeAdvanceProgramSheetWeek(workout)
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
                    suggestionsStale = true
                }
                .onFailure {
                    showError("The workout could not be saved. Your active workout is still available.")
                }
            _uiState.update { it.copy(isWorkoutSaving = false) }
        }
    }

    private suspend fun syncFinishedWorkoutToRoutine(workout: ActiveWorkout) {
        if (_uiState.value.programSheet.isConfigured) return
        val routineId = workout.sourceRoutineId ?: return
        runCatching {
            routineMutationMutex.withLock {
                ensureRoutineLibraryReady()
                val (folderId, existingRoutine) = routineRepository.routineWithFolder(routineId)
                    ?: return@withLock
                val updatedRoutine = RoutineWorkoutMapper.syncRoutineFromWorkout(
                    existingRoutine = existingRoutine,
                    workout = workout,
                ) ?: return@withLock
                routineRepository.saveRoutine(
                    folderId = folderId,
                    routine = updatedRoutine,
                    existingRoutineId = existingRoutine.id,
                )
            }
        }
    }

    private fun setTimerState(state: RestTimerState?) {
        _uiState.update { it.copy(timerState = state) }
        scheduleTimerCompletion(state)

        // --- Nothing Glyph integration ---
        if (_uiState.value.isGlyphTimerEnabled) {
            glyphController?.updateTimer(state)
        } else {
            glyphController?.turnOff()
        }
        scheduleGlyphUpdates(state)
    }

    private fun scheduleTimerCompletion(state: RestTimerState?) {
        timerCompletionJob?.cancel()
        val expectedEndMillis = state?.endMillis ?: return
        timerCompletionJob = viewModelScope.launch {
            delay((expectedEndMillis - System.currentTimeMillis()).coerceAtLeast(0L))
            // Timer expired — notify Glyph before completing (which clears state)
            val pulseEnabled = _uiState.value.isGlyphPulseEnabled && _uiState.value.isGlyphTimerEnabled
            glyphController?.onTimerComplete(pulseEnabled)
            RestTimerManager.complete(context, expectedEndMillis)
            setTimerState(RestTimerManager.getState(context))
        }
    }

    /**
     * Runs a steady 500ms ticker while a rest timer is active.
     * [GlyphRestTimerController.updateTimer] internally compares against [lastSegmentCount],
     * so it only communicates with the SDK when the segment count actually changes.
     */
    private fun scheduleGlyphUpdates(state: RestTimerState?) {
        glyphUpdateJob?.cancel()
        if (state == null || glyphController == null || !_uiState.value.isGlyphTimerEnabled) return

        glyphUpdateJob = viewModelScope.launch {
            while (isActive) {
                delay(500)
                val current = _uiState.value.timerState ?: break
                glyphController.updateTimer(current)
            }
        }
    }

    fun startRestTimerForExercise(exerciseId: String) {
        val exercise = _uiState.value.activeWorkout?.exercises?.firstOrNull { it.id == exerciseId } ?: return
        RestTimerManager.start(
            context = context,
            durationSeconds = exercise.restSeconds,
            completedExerciseId = exercise.id,
            completedSetId = "",
            exerciseName = exercise.name,
        )
        setTimerState(RestTimerManager.getState(context))
    }

    fun setGlyphTimerEnabled(enabled: Boolean) {
        GlyphPreferences.setTimerEnabled(context, enabled)
        _uiState.update { it.copy(isGlyphTimerEnabled = enabled) }
        if (!enabled) {
            glyphUpdateJob?.cancel()
            glyphController?.turnOff()
        } else {
            val state = _uiState.value.timerState
            glyphController?.updateTimer(state)
            scheduleGlyphUpdates(state)
        }
        WorkoutWidgetUpdateDispatcher.request(context)
    }

    fun setGlyphPulseEnabled(enabled: Boolean) {
        GlyphPreferences.setPulseOnComplete(context, enabled)
        _uiState.update { it.copy(isGlyphPulseEnabled = enabled) }
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

    fun openSavedExercises() {
        if (_uiState.value.activeWorkout != null) {
            showError("Finish or discard the active workout before managing saved exercises.")
            return
        }
        viewModelScope.launch {
            runCatching {
                ensureRoutineLibraryReady()
                val folders = routineRepository.folders()
                _uiState.update {
                    it.copy(
                        isSavedExercisesOpen = true,
                        savedExercisesDraft = SavedExercisesDraft(
                            initialFolders = folders,
                            currentFolders = folders,
                        ),
                        showDiscardSavedExercisesConfirm = false,
                    )
                }
            }.onFailure {
                showError("Could not load saved exercises.")
            }
        }
    }

    fun editSavedExerciseDetails(oldName: String, newName: String, notes: String, restSeconds: Int) {
        _uiState.update { current ->
            val draft = current.savedExercisesDraft ?: return@update current
            val updated = SavedExercisesOperations.renameExercise(
                folders = draft.currentFolders,
                oldName = oldName,
                newName = newName,
                newNotes = notes,
                newRestSeconds = restSeconds,
            )
            val updatedRenames = SavedExercisesOperations.recordHistoryRename(
                existing = draft.historyRenames,
                oldName = oldName,
                newName = newName,
            )
            current.copy(
                savedExercisesDraft = draft.copy(
                    currentFolders = updated,
                    historyRenames = updatedRenames,
                ),
            )
        }
    }

    fun mergeSavedExercises(sourceName: String, targetName: String) {
        _uiState.update { current ->
            val draft = current.savedExercisesDraft ?: return@update current
            val updated = SavedExercisesOperations.mergeExercises(
                folders = draft.currentFolders,
                sourceName = sourceName,
                targetName = targetName,
            )
            val updatedRenames = SavedExercisesOperations.recordHistoryRename(
                existing = draft.historyRenames,
                oldName = sourceName,
                newName = targetName,
            )
            current.copy(
                savedExercisesDraft = draft.copy(
                    currentFolders = updated,
                    historyRenames = updatedRenames,
                ),
            )
        }
    }

    fun deleteSavedExercise(name: String) {
        _uiState.update { current ->
            val draft = current.savedExercisesDraft ?: return@update current
            val updated = SavedExercisesOperations.deleteExercise(
                folders = draft.currentFolders,
                name = name,
            )
            current.copy(savedExercisesDraft = draft.copy(currentFolders = updated))
        }
    }

    fun deleteSavedExerciseInstance(routineId: String, exerciseId: String) {
        _uiState.update { current ->
            val draft = current.savedExercisesDraft ?: return@update current
            val updated = SavedExercisesOperations.deleteExerciseInstance(
                folders = draft.currentFolders,
                routineId = routineId,
                exerciseId = exerciseId,
            )
            current.copy(savedExercisesDraft = draft.copy(currentFolders = updated))
        }
    }

    fun deduplicateAllSavedExercises() {
        _uiState.update { current ->
            val draft = current.savedExercisesDraft ?: return@update current
            val summariesBefore = draft.exercises
            val updated = SavedExercisesOperations.deduplicateAll(draft.currentFolders)
            var updatedRenames = draft.historyRenames
            summariesBefore.forEach { summary ->
                if (summary.hasCasingOrWhitespaceVariants) {
                    updatedRenames = SavedExercisesOperations.recordHistoryRename(
                        existing = updatedRenames,
                        oldName = summary.canonicalName,
                        newName = summary.canonicalName,
                    )
                }
            }
            current.copy(
                savedExercisesDraft = draft.copy(
                    currentFolders = updated,
                    historyRenames = updatedRenames,
                ),
            )
        }
    }

    fun commitSavedExercises() {
        val draft = _uiState.value.savedExercisesDraft ?: return
        viewModelScope.launch {
            runCatching {
                routineMutationMutex.withLock {
                    ensureRoutineLibraryReady()
                    routineRepository.replaceFolders(draft.currentFolders)
                    if (draft.historyRenames.isNotEmpty()) {
                        workoutHistoryRepository.renameExercises(draft.historyRenames)
                    }
                }
            }.onSuccess {
                _uiState.update {
                    it.copy(
                        isSavedExercisesOpen = false,
                        savedExercisesDraft = null,
                        showDiscardSavedExercisesConfirm = false,
                    )
                }
                suggestionsStale = true
            }.onFailure {
                showError("Could not commit exercise changes.")
            }
        }
    }

    fun handleBackFromSavedExercises() {
        val draft = _uiState.value.savedExercisesDraft
        if (draft != null && draft.hasChanges) {
            _uiState.update { it.copy(showDiscardSavedExercisesConfirm = true) }
        } else {
            _uiState.update {
                it.copy(
                    isSavedExercisesOpen = false,
                    savedExercisesDraft = null,
                    showDiscardSavedExercisesConfirm = false,
                )
            }
        }
    }

    fun confirmDiscardSavedExercises() {
        _uiState.update {
            it.copy(
                isSavedExercisesOpen = false,
                savedExercisesDraft = null,
                showDiscardSavedExercisesConfirm = false,
            )
        }
    }

    fun dismissDiscardSavedExercisesConfirm() {
        _uiState.update { it.copy(showDiscardSavedExercisesConfirm = false) }
    }

    // --- Body Metrics (Google Sheets sync) ---

    fun onAppResumed() {
        val serviceEnabled = SoftLockAccessibilityService.isEnabledInSettings(context)
        if (_uiState.value.isSoftLockServiceEnabled != serviceEnabled) {
            _uiState.update { it.copy(isSoftLockServiceEnabled = serviceEnabled) }
            RestTimerManager.refresh(context)
        }
        syncBodyMetricsIfStale()
    }

    fun openBodyMetrics() {
        val metrics = _uiState.value.bodyMetrics
        if (!metrics.isConfigured) {
            _uiState.update { it.copy(showBodyMetricsConfigDialog = true) }
        } else {
            _uiState.update { it.copy(isBodyMetricsOpen = true) }
            syncBodyMetricsIfStale()
        }
    }

    fun closeBodyMetrics() {
        _uiState.update { it.copy(isBodyMetricsOpen = false) }
    }

    fun selectBodyMetricsPeriod(period: BodyMetricsPeriod) {
        _uiState.update { it.copy(bodyMetricsPeriod = period) }
    }

    fun openBodyMetricsConfig() {
        _uiState.update { it.copy(showBodyMetricsConfigDialog = true) }
    }

    fun dismissBodyMetricsConfig() {
        _uiState.update { it.copy(showBodyMetricsConfigDialog = false) }
    }

    fun saveBodyMetricsSheetUrl(rawUrl: String) {
        val trimmed = rawUrl.trim()
        bodyMetricsRepository.saveSheetUrl(trimmed)
        _uiState.update {
            it.copy(
                showBodyMetricsConfigDialog = false,
                isBodyMetricsOpen = true,
                bodyMetrics = it.bodyMetrics.copy(
                    sheetUrl = trimmed,
                    lastError = null,
                ),
            )
        }
        refreshBodyMetrics(force = true)
    }

    fun disconnectBodyMetricsSheet() {
        bodyMetricsSyncJob?.cancel()
        bodyMetricsRepository.clearConfiguration()
        _uiState.update {
            it.copy(
                showBodyMetricsConfigDialog = false,
                isBodyMetricsOpen = false,
                bodyMetrics = BodyMetricsSyncState(),
            )
        }
    }

    fun refreshBodyMetrics(force: Boolean = true) {
        val current = _uiState.value.bodyMetrics
        if (!current.isConfigured) return
        if (!force && current.isSyncing) return
        if (force && bodyMetricsSyncJob?.isActive == true) {
            bodyMetricsSyncJob?.cancel()
        }
        val targetUrl = current.sheetUrl
        bodyMetricsSyncJob = viewModelScope.launch {
            _uiState.update {
                it.copy(bodyMetrics = it.bodyMetrics.copy(isSyncing = true, lastError = null))
            }
            when (val outcome = bodyMetricsRepository.syncFromUrl(targetUrl)) {
                is BodyMetricsSyncOutcome.Success -> {
                    _uiState.update {
                        it.copy(
                            bodyMetrics = it.bodyMetrics.copy(
                                sheetUrl = targetUrl,
                                entries = outcome.entries,
                                lastSyncedAtMillis = outcome.syncedAtMillis,
                                isSyncing = false,
                                lastError = null,
                            ),
                        )
                    }
                }
                is BodyMetricsSyncOutcome.Failure -> {
                    _uiState.update {
                        it.copy(
                            bodyMetrics = it.bodyMetrics.copy(
                                isSyncing = false,
                                lastError = outcome.message,
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun syncBodyMetricsIfStale() {
        val current = _uiState.value.bodyMetrics
        if (!current.isConfigured || current.isSyncing) return
        if (bodyMetricsRepository.isStale(current.lastSyncedAtMillis)) {
            refreshBodyMetrics(force = false)
        }
    }

    // --- Google Sheet Program ---

    fun openProgramSheetConfig() {
        _uiState.update { it.copy(showProgramSheetConfigDialog = true) }
    }

    fun dismissProgramSheetConfig() {
        _uiState.update { it.copy(showProgramSheetConfigDialog = false) }
    }

    fun saveProgramSheetUrl(rawUrl: String) {
        val trimmed = rawUrl.trim()
        _uiState.update {
            it.copy(
                showProgramSheetConfigDialog = false,
                programSheet = it.programSheet.copy(
                    sheetUrl = trimmed,
                    lastError = null,
                ),
            )
        }
        refreshProgramSheet(force = true)
    }

    fun disconnectProgramSheet() {
        programSheetSyncJob?.cancel()
        programSheetRepository.clearConfiguration()
        _uiState.update {
            it.copy(
                showProgramSheetConfigDialog = false,
                programSheet = ProgramSheetState(),
            )
        }
    }

    fun selectProgramSheetWeek(weekNumber: Int) {
        programSheetRepository.setActiveWeek(weekNumber)
        _uiState.update {
            it.copy(programSheet = it.programSheet.copy(activeWeekNumber = weekNumber))
        }
    }

    private fun syncProgramSheetIfStale() {
        val current = _uiState.value.programSheet
        if (!current.isConfigured || current.isSyncing) return
        if (programSheetRepository.isStale(current.lastSyncedAtMillis)) {
            refreshProgramSheet(force = false)
        }
    }

    private fun maybeAdvanceProgramSheetWeek(workout: ActiveWorkout) {
        val current = _uiState.value.programSheet
        if (!current.hasData) return
        val nextWeek = ProgramSheetWorkoutMapper.nextWeekAfterFinishing(workout, current) ?: return
        selectProgramSheetWeek(nextWeek)
    }

    fun refreshProgramSheet(force: Boolean = true) {
        val current = _uiState.value.programSheet
        if (!current.isConfigured) return
        if (!force && current.isSyncing) return
        if (force && programSheetSyncJob?.isActive == true) {
            programSheetSyncJob?.cancel()
        }
        val targetUrl = current.sheetUrl
        programSheetSyncJob = viewModelScope.launch {
            _uiState.update {
                it.copy(programSheet = it.programSheet.copy(isSyncing = true, lastError = null))
            }
            when (val outcome = programSheetRepository.syncFromUrl(targetUrl)) {
                is ProgramSheetSyncOutcome.Success -> {
                    _uiState.update {
                        it.copy(
                            programSheet = it.programSheet.copy(
                                sheetUrl = targetUrl,
                                weeks = outcome.weeks,
                                activeWeekNumber = outcome.activeWeekNumber,
                                lastSyncedAtMillis = outcome.syncedAtMillis,
                                isSyncing = false,
                                lastError = null,
                            ),
                        )
                    }
                }
                is ProgramSheetSyncOutcome.Failure -> {
                    _uiState.update {
                        it.copy(
                            programSheet = it.programSheet.copy(
                                isSyncing = false,
                                lastError = outcome.message,
                            ),
                        )
                    }
                }
            }
        }
    }

    fun startProgramSheetDay(weekNumber: Int, dayName: String) {
        if (resumeExistingWorkoutIfPresent() || workoutStartJob?.isActive == true) return
        val week = _uiState.value.programSheet.weeks.firstOrNull { it.weekNumber == weekNumber } ?: return
        val day = week.days.firstOrNull { it.dayName == dayName } ?: return
        workoutStartJob = viewModelScope.launch {
            try {
                runCatching {
                    val workout = ProgramSheetWorkoutMapper.fromSheetDay(week = week, day = day)
                    val (previousSets, exerciseHistory) = withContext(Dispatchers.IO) {
                        workoutHistoryRepository.workoutHistoryContext(workout)
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
                    showError("This workout could not be started from the Google Sheet.")
                }
            } finally {
                preparedRoutineStart = null
            }
        }
    }

    // --- Exercise Progress ---

    private fun observeExerciseProgress() {
        viewModelScope.launch {
            workoutHistoryRepository.observeCompletedExerciseSets().collectLatest { rows ->
                val options = withContext(Dispatchers.Default) {
                    ExerciseProgressCalculator.buildOptions(rows)
                }
                _uiState.update { current ->
                    val currentKey = current.selectedProgressExerciseKey
                    val resolvedKey = when {
                        currentKey != null && options.any { it.normalizedKey == currentKey } -> currentKey
                        else -> options.firstOrNull()?.normalizedKey
                    }
                    current.copy(
                        exerciseProgressRows = rows,
                        exerciseProgressOptions = options,
                        selectedProgressExerciseKey = resolvedKey,
                    )
                }
            }
        }
    }

    fun openExerciseProgress() {
        _uiState.update { it.copy(isExerciseProgressOpen = true) }
    }

    fun closeExerciseProgress() {
        _uiState.update { it.copy(isExerciseProgressOpen = false) }
    }

    fun openExerciseProgressPicker() {
        _uiState.update { it.copy(showExerciseProgressPicker = true) }
    }

    fun dismissExerciseProgressPicker() {
        _uiState.update { it.copy(showExerciseProgressPicker = false) }
    }

    fun selectProgressExercise(normalizedKey: String) {
        exerciseProgressPreferences.setSelectedExerciseKey(normalizedKey)
        _uiState.update {
            it.copy(
                selectedProgressExerciseKey = normalizedKey,
                showExerciseProgressPicker = false,
            )
        }
    }

    fun selectExerciseProgressMetric(metric: ExerciseProgressMetric) {
        exerciseProgressPreferences.setSelectedMetric(metric)
        _uiState.update { it.copy(exerciseProgressMetric = metric) }
    }

    fun selectExerciseProgressPeriod(period: ExerciseProgressPeriod) {
        _uiState.update { it.copy(exerciseProgressPeriod = period) }
    }

    private fun showError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    private suspend fun ensureRoutineLibraryReady() {
        val result = routineInitialization.await()
        if (result is RoutineLibraryImportResult.CorruptLegacyData) throw result.cause
    }
}