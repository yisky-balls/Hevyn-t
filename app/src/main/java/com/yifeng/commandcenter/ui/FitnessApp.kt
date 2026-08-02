package com.yifeng.commandcenter.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.runtime.DisposableEffect
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.yifeng.commandcenter.data.ActiveExercise
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.ActiveWorkoutOperations
import com.yifeng.commandcenter.data.ActiveWorkoutRepository
import com.yifeng.commandcenter.data.ActiveWorkoutSession
import com.yifeng.commandcenter.data.Routine
import com.yifeng.commandcenter.data.RoutineBuildTarget
import com.yifeng.commandcenter.data.RoutineExercise
import com.yifeng.commandcenter.data.RoutineFolder
import com.yifeng.commandcenter.data.RoutineFolderOperations
import com.yifeng.commandcenter.data.RoutineRepository
import com.yifeng.commandcenter.data.WorkoutDateRange
import com.yifeng.commandcenter.timer.RestTimerManager
import com.yifeng.commandcenter.timer.RestTimerState
import com.yifeng.commandcenter.ui.theme.DisplayFont
import com.yifeng.commandcenter.ui.theme.LiftBorder
import com.yifeng.commandcenter.ui.theme.LiftMuted
import com.yifeng.commandcenter.ui.theme.LiftSurfaceRaised
import java.io.File
import java.util.UUID
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.yifeng.commandcenter.history.CompletedWorkoutMapper
import com.yifeng.commandcenter.history.WorkoutCompletionCoordinator
import com.yifeng.commandcenter.history.WorkoutHistoryGraph
import com.yifeng.commandcenter.history.WorkoutJournalEntry
import com.yifeng.commandcenter.history.WorkoutPhotoManager
import com.yifeng.commandcenter.history.WorkoutPhotoStore
import com.yifeng.commandcenter.history.WorkoutHistoryRepository
import com.yifeng.commandcenter.history.WorkoutHistorySession
import com.yifeng.commandcenter.history.PreviousSetPerformance
import com.yifeng.commandcenter.history.PreviousSetResolver

private enum class AppPage { Home, Workout, Journal }

private enum class FinishPrompt { Unfinished, Empty }

private data class AppContentState(
    val page: AppPage,
    val workoutExpanded: Boolean,
)

private data class PreviousLookupKey(
    val workoutId: String,
    val startedAtMillis: Long,
    val exercises: List<PreviousExerciseLookupKey>,
)

private data class PreviousExerciseLookupKey(
    val id: String,
    val sourceRoutineExerciseId: String?,
    val name: String,
    val setIds: List<String>,
)

@Composable
fun FitnessApp() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { RoutineRepository(context) }
    val activeWorkoutRepository = remember { ActiveWorkoutRepository(context) }
    val workoutHistoryRepository = remember { WorkoutHistoryRepository(context) }
    val workoutPhotoManager = remember {
        WorkoutPhotoManager(workoutHistoryRepository, WorkoutPhotoStore(context))
    }
    val restoredSession = remember { activeWorkoutRepository.loadSession() }
    var selectedPage by remember { mutableStateOf(AppPage.Workout) }
    var folders by remember { mutableStateOf(repository.loadFolders()) }
    var activeWorkout by remember { mutableStateOf(restoredSession?.workout) }
    var isWorkoutExpanded by remember {
        mutableStateOf(activeWorkout == null || activeWorkoutRepository.isExpanded())
    }
    var routineBuildTarget by remember { mutableStateOf(restoredSession?.routineBuildTarget) }
    var showRoutineNameDialog by remember { mutableStateOf(false) }
    var roomWorkoutHistory by remember { mutableStateOf<List<WorkoutHistorySession>>(emptyList()) }
    var journalEntries by remember { mutableStateOf<List<WorkoutJournalEntry>>(emptyList()) }
    var journalPhotoFiles by remember { mutableStateOf<Map<String, File>>(emptyMap()) }
    var journalFilter by remember { mutableStateOf<WorkoutDateRange?>(null) }
    var selectedHistoryGraph by remember { mutableStateOf<WorkoutHistoryGraph?>(null) }
    var recapGraph by remember { mutableStateOf<WorkoutHistoryGraph?>(null) }
    var finishPrompt by remember { mutableStateOf<FinishPrompt?>(null) }
    var showDeleteHistoryDialog by remember { mutableStateOf(false) }
    var historyError by remember { mutableStateOf<String?>(null) }
    var isWorkoutSaving by remember { mutableStateOf(false) }
    var isHistoryRestored by remember { mutableStateOf(false) }
    var isPhotoBusy by remember { mutableStateOf(false) }
    var previousSets by remember { mutableStateOf<Map<String, PreviousSetPerformance>>(emptyMap()) }
    var timerState by remember { mutableStateOf<RestTimerState?>(RestTimerManager.getState(context)) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) RestTimerManager.refresh(context)
    }

    DisposableEffect(activeWorkoutRepository, context) {
        val unregisterWorkout = activeWorkoutRepository.registerSessionListener { session ->
            activeWorkout = session?.workout
            routineBuildTarget = session?.routineBuildTarget
        }
        val unregisterTimer = RestTimerManager.registerStateListener(context) { state ->
            timerState = state
        }
        onDispose {
            unregisterWorkout()
            unregisterTimer()
        }
    }

    LaunchedEffect(timerState?.endMillis) {
        val expectedEndMillis = timerState?.endMillis ?: return@LaunchedEffect
        delay((expectedEndMillis - System.currentTimeMillis()).coerceAtLeast(0L))
        RestTimerManager.complete(context, expectedEndMillis)
    }

    LaunchedEffect(workoutHistoryRepository) {
        withContext(Dispatchers.IO) {
            workoutHistoryRepository.importLegacySummaries(repository.loadWorkoutHistory())
        }
        val pendingFinishId = withContext(Dispatchers.IO) {
            activeWorkoutRepository.pendingFinishWorkoutId()
        }
        if (pendingFinishId != null) {
            val interruptedSession = activeWorkoutRepository.loadSession()
            if (interruptedSession?.workout?.id == pendingFinishId) {
                val committedGraph = workoutHistoryRepository.graph(pendingFinishId)
                if (committedGraph != null) {
                    val cleared = withContext(Dispatchers.IO) {
                        activeWorkoutRepository.clearFinished(pendingFinishId)
                    }
                    if (cleared) {
                        RestTimerManager.cancel(context)
                        timerState = null
                        routineBuildTarget = null
                        activeWorkout = null
                        recapGraph = committedGraph
                    }
                } else {
                    withContext(Dispatchers.IO) {
                        activeWorkoutRepository.cancelFinishingIfUnchanged(
                            workoutId = interruptedSession.workout.id,
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
        }
        isHistoryRestored = true
        workoutHistoryRepository.observeSessions().collectLatest { sessions ->
            roomWorkoutHistory = sessions
        }
    }

    LaunchedEffect(workoutHistoryRepository) {
        workoutHistoryRepository.observeJournalEntries().collectLatest { entries ->
            journalEntries = entries
            journalPhotoFiles = withContext(Dispatchers.IO) {
                entries.mapNotNull { entry ->
                    entry.photo?.let { photo ->
                        workoutPhotoManager.existingFile(photo)?.let { file -> photo.id to file }
                    }
                }.toMap()
            }
        }
    }

    val previousLookupKey = activeWorkout?.let { workout ->
        PreviousLookupKey(
            workoutId = workout.id,
            startedAtMillis = workout.startedAtMillis,
            exercises = workout.exercises.map { exercise ->
                PreviousExerciseLookupKey(
                    id = exercise.id,
                    sourceRoutineExerciseId = exercise.sourceRoutineExerciseId,
                    name = exercise.name,
                    setIds = exercise.sets.map { it.id },
                )
            },
        )
    }
    LaunchedEffect(previousLookupKey, roomWorkoutHistory) {
        previousSets = activeWorkout?.let { workout ->
            PreviousSetResolver.resolve(
                workout = workout,
                history = workoutHistoryRepository.graphsCompletedBefore(workout.startedAtMillis),
            )
        }.orEmpty()
    }

    fun saveSession(workout: ActiveWorkout, target: RoutineBuildTarget?) {
        activeWorkout = workout
        routineBuildTarget = target
        activeWorkoutRepository.save(ActiveWorkoutSession(workout, target))
        isWorkoutExpanded = true
        activeWorkoutRepository.setExpanded(true)
    }

    fun clearSession() {
        RestTimerManager.cancel(context)
        timerState = null
        showRoutineNameDialog = false
        routineBuildTarget = null
        activeWorkout = null
        activeWorkoutRepository.clear()
    }

    fun startRoutine(routine: Routine) {
        if (activeWorkout != null) {
            isWorkoutExpanded = true
            activeWorkoutRepository.setExpanded(true)
            return
        }
        val workout = ActiveWorkout(
            sourceRoutineId = routine.id,
            routineName = routine.name,
            exercises = routine.exercises.map { exercise ->
                ActiveExercise(
                    id = exercise.id,
                    sourceRoutineExerciseId = exercise.id,
                    name = exercise.name,
                    notes = exercise.notes,
                    restSeconds = exercise.restSeconds,
                    sets = exercise.sets.map { it.copy(completed = false) },
                )
            },
        )
        saveSession(workout, null)
        selectedPage = AppPage.Workout
    }

    fun saveFolders(updated: List<RoutineFolder>) {
        folders = updated
        repository.saveFolders(updated)
    }

    fun buildRoutine(folder: RoutineFolder, routine: Routine?) {
        if (activeWorkout != null) {
            isWorkoutExpanded = true
            activeWorkoutRepository.setExpanded(true)
            return
        }
        val target = RoutineBuildTarget(folder.id, routine?.id)
        val workout = ActiveWorkout(
            sourceRoutineId = routine?.id,
            routineName = routine?.name.orEmpty(),
            exercises = routine?.exercises.orEmpty().map { exercise ->
                ActiveExercise(
                    id = exercise.id,
                    sourceRoutineExerciseId = exercise.id,
                    name = exercise.name,
                    notes = exercise.notes,
                    restSeconds = exercise.restSeconds,
                    sets = exercise.sets.map { it.copy(completed = false) },
                )
            },
        )
        saveSession(workout, target)
        selectedPage = AppPage.Workout
    }

    fun saveBuiltRoutine(name: String) {
        val target = routineBuildTarget ?: return
        val workout = activeWorkout ?: return
        val routine = Routine(
            id = target.routineId ?: UUID.randomUUID().toString(),
            name = name,
            exercises = workout.exercises.mapNotNull { exercise ->
                exercise.name.trim().takeIf { it.isNotEmpty() }?.let { exerciseName ->
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
        saveFolders(
            RoutineFolderOperations.saveRoutine(
                folders = folders,
                folderId = target.folderId,
                routine = routine,
                existingRoutineId = target.routineId,
            )
        )
        clearSession()
    }

    fun saveFinishedWorkout(workout: ActiveWorkout) {
        if (isWorkoutSaving || !isHistoryRestored) return
        isWorkoutSaving = true
        finishPrompt = null
        historyError = null
        scope.launch {
            val coordinator = WorkoutCompletionCoordinator(
                saveGraph = workoutHistoryRepository::saveCompletedWorkout,
                beginFinishing = { candidate ->
                    withContext(Dispatchers.IO) {
                        activeWorkoutRepository.beginFinishing(candidate)
                    }
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
                    timerState = null
                    showRoutineNameDialog = false
                    routineBuildTarget = null
                    activeWorkout = null
                    finishPrompt = null
                    recapGraph = graph
                }
                .onFailure {
                    historyError = "The workout could not be saved. Your active workout is still available."
                }
            isWorkoutSaving = false
        }
    }

    fun openHistorySession(sessionId: String) {
        historyError = null
        scope.launch {
            selectedHistoryGraph = workoutHistoryRepository.graph(sessionId)
                ?: run {
                    historyError = "This workout could not be opened."
                    null
                }
        }
    }

    fun updateActiveWorkout(transform: (ActiveWorkout) -> ActiveWorkout?) {
        activeWorkoutRepository.update(transform)?.let { updated ->
            activeWorkout = updated
        }
    }

    val routines = folders.flatMap { it.routines }
    activeWorkout?.takeIf { showRoutineNameDialog }?.let { workout ->
        RoutineNameDialog(
            initialName = workout.routineName,
            isEditing = routineBuildTarget?.routineId != null,
            onDismiss = { showRoutineNameDialog = false },
            onSave = ::saveBuiltRoutine,
        )
    }

    finishPrompt?.let { prompt ->
        AlertDialog(
            onDismissRequest = { finishPrompt = null },
            title = {
                Text(
                    if (prompt == FinishPrompt.Unfinished) "UNFINISHED SETS" else "NO COMPLETED SETS",
                    fontFamily = DisplayFont,
                )
            },
            text = {
                Text(
                    if (prompt == FinishPrompt.Unfinished) {
                        "Some sets are unfinished. Continue the workout or save only your completed sets."
                    } else {
                        "Empty workouts are never saved. Return to the workout or discard it."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (prompt == FinishPrompt.Unfinished) {
                            activeWorkout?.let(::saveFinishedWorkout)
                        } else {
                            finishPrompt = null
                            clearSession()
                        }
                    },
                ) {
                    Text(
                        if (prompt == FinishPrompt.Unfinished) "SAVE COMPLETED SETS" else "DISCARD",
                        color = if (prompt == FinishPrompt.Empty) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { finishPrompt = null }) {
                    Text(if (prompt == FinishPrompt.Unfinished) "CONTINUE WORKOUT" else "RETURN")
                }
            },
        )
    }

    historyError?.let { message ->
        AlertDialog(
            onDismissRequest = { historyError = null },
            title = { Text("SOMETHING WENT WRONG", fontFamily = DisplayFont) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { historyError = null }) { Text("OK") }
            },
        )
    }

    if (showDeleteHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteHistoryDialog = false },
            title = { Text("DELETE SESSION?", fontFamily = DisplayFont) },
            text = { Text("This removes the workout and its app-owned photo from this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val sessionId = selectedHistoryGraph?.session?.id ?: return@TextButton
                        showDeleteHistoryDialog = false
                        scope.launch {
                            runCatching { workoutPhotoManager.deleteSession(sessionId) }
                                .onSuccess { selectedHistoryGraph = null }
                                .onFailure { historyError = it.message ?: "The workout could not be deleted." }
                        }
                    },
                ) { Text("DELETE", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteHistoryDialog = false }) { Text("CANCEL") }
            },
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (recapGraph == null && (activeWorkout == null || !isWorkoutExpanded)) {
                Column {
                    activeWorkout?.takeIf { !isWorkoutExpanded }?.let { workout ->
                        MinimizedWorkoutBar(
                            workout = workout,
                            onResume = {
                                isWorkoutExpanded = true
                                activeWorkoutRepository.setExpanded(true)
                            },
                            onDiscard = {
                                clearSession()
                            },
                        )
                    }
                    AppBottomBar(
                        selectedPage = selectedPage,
                        onPageSelected = { page ->
                            selectedHistoryGraph = null
                            journalFilter = null
                            selectedPage = page
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val workout = activeWorkout
            val recap = recapGraph
            val selectedGraph = selectedHistoryGraph
            if (recap != null) {
                val photo = recap.photos.firstOrNull()
                WorkoutRecapScreen(
                    graph = recap,
                    photoFile = photo?.let { journalPhotoFiles[it.id] },
                    isPhotoBusy = isPhotoBusy,
                    errorMessage = historyError,
                    onPhotoSelected = { uri ->
                        isPhotoBusy = true
                        historyError = null
                        scope.launch {
                            runCatching {
                                workoutPhotoManager.replaceFromPicker(
                                    sessionId = recap.session.id,
                                    contentResolver = context.contentResolver,
                                    uri = uri,
                                )
                            }.onSuccess { savedPhoto ->
                                recapGraph = workoutHistoryRepository.graph(recap.session.id)
                                withContext(Dispatchers.IO) {
                                    workoutPhotoManager.existingFile(savedPhoto)
                                }?.let { file ->
                                    journalPhotoFiles = journalPhotoFiles + (savedPhoto.id to file)
                                }
                            }.onFailure {
                                historyError = it.message ?: "The photo could not be saved."
                            }
                            isPhotoBusy = false
                        }
                    },
                    onRemovePhoto = {
                        val currentPhoto = recap.photos.firstOrNull() ?: return@WorkoutRecapScreen
                        isPhotoBusy = true
                        scope.launch {
                            runCatching { workoutPhotoManager.remove(currentPhoto) }
                                .onSuccess { recapGraph = workoutHistoryRepository.graph(recap.session.id) }
                                .onFailure { historyError = it.message ?: "The photo could not be removed." }
                            isPhotoBusy = false
                        }
                    },
                    onDone = { note ->
                        scope.launch {
                            runCatching {
                                check(workoutHistoryRepository.updateNote(recap.session.id, note)) {
                                    "The saved workout no longer exists."
                                }
                            }
                                .onSuccess {
                                    recapGraph = null
                                    selectedPage = AppPage.Journal
                                }
                                .onFailure { historyError = it.message ?: "The note could not be saved." }
                        }
                    },
                )
                return@Box
            }
            AnimatedContent(
                targetState = AppContentState(selectedPage, workout != null && isWorkoutExpanded),
                transitionSpec = {
                    if (targetState.workoutExpanded != initialState.workoutExpanded) {
                        if (targetState.workoutExpanded) {
                            (slideInVertically(tween(280)) { it / 3 } + fadeIn(tween(220))) togetherWith
                                (slideOutVertically(tween(230)) { -it / 5 } + fadeOut(tween(180)))
                        } else {
                            (slideInVertically(tween(260)) { -it / 5 } + fadeIn(tween(220))) togetherWith
                                (slideOutVertically(tween(250)) { it / 3 } + fadeOut(tween(180)))
                        }
                    } else {
                        val forward = targetState.page.ordinal > initialState.page.ordinal
                        val direction = if (forward) 1 else -1
                        (slideInHorizontally(tween(260)) { direction * it / 5 } + fadeIn(tween(220))) togetherWith
                            (slideOutHorizontally(tween(230)) { -direction * it / 5 } + fadeOut(tween(180)))
                    }.using(SizeTransform(clip = false))
                },
                label = "app-page-transition",
            ) { contentState ->
            when {
                workout != null && contentState.workoutExpanded -> ActiveWorkoutScreen(
                    workout = workout,
                    previousSets = previousSets,
                    isRoutineBuilder = routineBuildTarget != null,
                    onAddExercise = {
                        updateActiveWorkout {
                            ActiveWorkoutOperations.addExercise(it, ActiveExercise(name = "New exercise"))
                        }
                    },
                    onExerciseNameChanged = { exerciseId, name ->
                        updateActiveWorkout {
                            ActiveWorkoutOperations.updateExerciseName(it, exerciseId, name)
                        }
                    },
                    onExerciseNotesChanged = { exerciseId, notes ->
                        updateActiveWorkout {
                            ActiveWorkoutOperations.updateExerciseNotes(it, exerciseId, notes)
                        }
                    },
                    onExerciseRestChanged = { exerciseId, seconds ->
                        updateActiveWorkout {
                            ActiveWorkoutOperations.updateRestSeconds(it, exerciseId, seconds)
                        }
                    },
                    onExerciseRemoved = { exerciseId ->
                        updateActiveWorkout {
                            ActiveWorkoutOperations.removeExercise(it, exerciseId)
                        }
                    },
                    onSetChanged = { exerciseId, setId, updatedSet ->
                        updateActiveWorkout { latest ->
                            ActiveWorkoutOperations.updateSet(latest, exerciseId, setId) { current ->
                                current.copy(weight = updatedSet.weight, reps = updatedSet.reps)
                            }
                        }
                    },
                    onSetCompletionChanged = { exerciseId, setId, completed ->
                        if (completed) {
                            activeWorkoutRepository.completeSet(exerciseId, setId)?.let { result ->
                                activeWorkout = result.workout
                                if (routineBuildTarget == null && result.nextTarget != null) {
                                    if (
                                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                                        PackageManager.PERMISSION_GRANTED
                                    ) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    RestTimerManager.start(
                                        context = context,
                                        durationSeconds = result.restSeconds,
                                        completedExerciseId = result.completedExerciseId,
                                        completedSetId = result.completedSetId,
                                        exerciseName = result.completedExerciseName,
                                    )
                                    timerState = RestTimerManager.getState(context)
                                } else if (result.nextTarget == null) {
                                    RestTimerManager.cancel(context)
                                    timerState = null
                                }
                            }
                        } else {
                            updateActiveWorkout {
                                ActiveWorkoutOperations.uncompleteSet(it, exerciseId, setId)
                            }
                            timerState
                                ?.takeIf { it.completedExerciseId == exerciseId && it.completedSetId == setId }
                                ?.let {
                                    RestTimerManager.cancel(context)
                                    timerState = null
                                }
                        }
                    },
                    onSetRemoved = { exerciseId, setId ->
                        activeWorkoutRepository.update {
                            ActiveWorkoutOperations.removeSet(it, exerciseId, setId)
                        }?.let { updated ->
                            activeWorkout = updated
                            RestTimerManager.reconcileRemovedSet(context, setId)
                            timerState = RestTimerManager.getState(context)
                        }
                    },
                    onSetAdded = { exerciseId ->
                        updateActiveWorkout { ActiveWorkoutOperations.addSet(it, exerciseId) }
                    },
                    onFinish = {
                        if (routineBuildTarget == null) {
                            val draft = CompletedWorkoutMapper.fromActiveWorkout(workout, System.currentTimeMillis())
                            when {
                                draft.graph.session.completedSetCount == 0 -> finishPrompt = FinishPrompt.Empty
                                draft.unfinishedSetCount > 0 -> finishPrompt = FinishPrompt.Unfinished
                                else -> saveFinishedWorkout(workout)
                            }
                        } else {
                            showRoutineNameDialog = true
                        }
                    },
                    onMinimize = {
                        isWorkoutExpanded = false
                        activeWorkoutRepository.setExpanded(false)
                    },
                    onCancel = {
                        clearSession()
                    },
                )

                contentState.page == AppPage.Home -> HomeScreen(
                    routines = routines,
                    workoutHistory = roomWorkoutHistory,
                    onOpenPrograms = { selectedPage = AppPage.Workout },
                    onStartRoutine = ::startRoutine,
                    onOpenSession = { sessionId ->
                        selectedPage = AppPage.Journal
                        openHistorySession(sessionId)
                    },
                    onOpenDay = { range ->
                        journalFilter = range
                        selectedHistoryGraph = null
                        selectedPage = AppPage.Journal
                    },
                )

                contentState.page == AppPage.Workout -> WorkoutProgramsScreen(
                    folders = folders,
                    onStartRoutine = ::startRoutine,
                    onBuildRoutine = ::buildRoutine,
                    onCreateFolder = { name ->
                        saveFolders(folders + RoutineFolder(name = name))
                    },
                    onRenameFolder = { folder, name ->
                        saveFolders(folders.map { if (it.id == folder.id) it.copy(name = name) else it })
                    },
                    onDeleteFolder = { folder ->
                        saveFolders(folders.filterNot { it.id == folder.id })
                    },
                    onDeleteRoutine = { folder, routine ->
                        saveFolders(
                            RoutineFolderOperations.deleteRoutine(
                                folders = folders,
                                folderId = folder.id,
                                routineId = routine.id,
                            )
                        )
                    },
                    onStartEmpty = {
                        if (activeWorkout != null) {
                            isWorkoutExpanded = true
                            activeWorkoutRepository.setExpanded(true)
                            return@WorkoutProgramsScreen
                        }
                        val workout = ActiveWorkout(
                            routineName = "Empty workout",
                            exercises = emptyList(),
                        )
                        saveSession(workout, null)
                    },
                )

                selectedGraph != null -> JournalDetailScreen(
                    graph = selectedGraph,
                    photoFile = selectedGraph.photos.firstOrNull()?.let { journalPhotoFiles[it.id] },
                    onBack = { selectedHistoryGraph = null },
                    onDelete = { showDeleteHistoryDialog = true },
                )

                else -> JournalScreen(
                    entries = journalEntries,
                    photoFiles = journalPhotoFiles,
                    filter = journalFilter,
                    onClearFilter = { journalFilter = null },
                    onOpenSession = ::openHistorySession,
                )
            }
            }

            timerState
                ?.takeIf { isWorkoutExpanded }
                ?.let { state -> workout?.let { RestTimerManager.resolveState(state, it) } }
                ?.let { state ->
                    RestTimerBar(
                        state = state,
                        modifier = Modifier.align(Alignment.BottomCenter),
                        onSubtract = {
                            RestTimerManager.adjust(context, -15)
                            timerState = RestTimerManager.getState(context)
                        },
                        onAdd = {
                            RestTimerManager.adjust(context, 15)
                            timerState = RestTimerManager.getState(context)
                        },
                        onSkip = {
                            RestTimerManager.skip(context)
                            timerState = null
                        },
                    )
                }

            if (isWorkoutSaving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.92f))
                        .clickable(onClick = {}),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "SAVING WORKOUT",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            if (!isHistoryRestored) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun AppBottomBar(
    selectedPage: AppPage,
    onPageSelected: (AppPage) -> Unit,
) {
    Surface(color = LiftSurfaceRaised) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(LiftBorder),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp),
            ) {
                AppBottomDestination(
                    label = "HOME",
                    selected = selectedPage == AppPage.Home,
                    onClick = { onPageSelected(AppPage.Home) },
                ) {
                    Icon(Icons.Outlined.Home, contentDescription = null, modifier = Modifier.size(25.dp))
                }
                AppBottomDestination(
                    label = "WORKOUT",
                    selected = selectedPage == AppPage.Workout,
                    onClick = { onPageSelected(AppPage.Workout) },
                ) {
                    Icon(Icons.Outlined.Build, contentDescription = null, modifier = Modifier.size(25.dp))
                }
                AppBottomDestination(
                    label = "JOURNAL",
                    selected = selectedPage == AppPage.Journal,
                    onClick = { onPageSelected(AppPage.Journal) },
                ) {
                    Icon(Icons.AutoMirrored.Outlined.List, contentDescription = null, modifier = Modifier.size(25.dp))
                }
            }
        }
    }
}

@Composable
private fun RowScope.AppBottomDestination(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    val contentColor = if (selected) MaterialTheme.colorScheme.primary else LiftMuted
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(color = Color.Transparent, contentColor = contentColor) { icon() }
        Spacer(Modifier.height(5.dp))
        Text(
            label,
            color = contentColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.6.sp,
        )
    }
}