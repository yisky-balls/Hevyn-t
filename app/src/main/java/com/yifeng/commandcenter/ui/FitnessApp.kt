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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.yifeng.commandcenter.data.ActiveExercise
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.ActiveWorkoutOperations
import com.yifeng.commandcenter.data.ActiveWorkoutRepository
import com.yifeng.commandcenter.data.ActiveWorkoutSession
import com.yifeng.commandcenter.data.LastWorkoutSummary
import com.yifeng.commandcenter.data.Routine
import com.yifeng.commandcenter.data.RoutineBuildTarget
import com.yifeng.commandcenter.data.RoutineExercise
import com.yifeng.commandcenter.data.RoutineFolder
import com.yifeng.commandcenter.data.RoutineFolderOperations
import com.yifeng.commandcenter.data.RoutineRepository
import com.yifeng.commandcenter.data.WorkoutMetricsCalculator
import com.yifeng.commandcenter.timer.RestTimerManager
import com.yifeng.commandcenter.timer.RestTimerState
import com.yifeng.commandcenter.ui.theme.DisplayFont
import com.yifeng.commandcenter.ui.theme.LiftMuted
import java.util.UUID
import kotlinx.coroutines.delay

private enum class AppPage { Home, Workout }

private data class AppContentState(
    val page: AppPage,
    val workoutExpanded: Boolean,
)

@Composable
fun FitnessApp() {
    val context = LocalContext.current
    val repository = remember { RoutineRepository(context) }
    val activeWorkoutRepository = remember { ActiveWorkoutRepository(context) }
    val restoredSession = remember { activeWorkoutRepository.loadSession() }
    var selectedPage by remember { mutableStateOf(AppPage.Workout) }
    var folders by remember { mutableStateOf(repository.loadFolders()) }
    var activeWorkout by remember { mutableStateOf(restoredSession?.workout) }
    var isWorkoutExpanded by remember {
        mutableStateOf(activeWorkout == null || activeWorkoutRepository.isExpanded())
    }
    var routineBuildTarget by remember { mutableStateOf(restoredSession?.routineBuildTarget) }
    var showRoutineNameDialog by remember { mutableStateOf(false) }
    var workoutHistory by remember { mutableStateOf(repository.loadWorkoutHistory()) }
    var lastWorkout by remember { mutableStateOf(workoutHistory.maxByOrNull { it.completedAtMillis }) }
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
            routineName = routine.name,
            exercises = routine.exercises.map { exercise ->
                ActiveExercise(
                    id = exercise.id,
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
            routineName = routine?.name.orEmpty(),
            exercises = routine?.exercises.orEmpty().map { exercise ->
                ActiveExercise(
                    id = exercise.id,
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

    fun finishWorkout(workout: ActiveWorkout) {
        val metrics = WorkoutMetricsCalculator.calculate(workout)
        val summary = LastWorkoutSummary(
            name = workout.routineName,
            completedAtMillis = System.currentTimeMillis(),
            completedSets = metrics.completedSets,
            volumeKg = metrics.volumeKg,
        )
        repository.saveLastWorkout(summary)
        workoutHistory = workoutHistory + summary
        lastWorkout = summary
        clearSession()
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

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (activeWorkout == null || !isWorkoutExpanded) {
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
                        onPageSelected = { selectedPage = it },
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
                            finishWorkout(workout)
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
                    workoutHistory = workoutHistory,
                    lastWorkout = lastWorkout,
                    onOpenPrograms = { selectedPage = AppPage.Workout },
                    onStartRoutine = ::startRoutine,
                )

                else -> WorkoutProgramsScreen(
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
        }
    }
}

@Composable
private fun AppBottomBar(
    selectedPage: AppPage,
    onPageSelected: (AppPage) -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(Modifier.fillMaxWidth().height(92.dp)) {
            AppBottomDestination(
                label = "HOME",
                selected = selectedPage == AppPage.Home,
                selectedColor = MaterialTheme.colorScheme.primary,
                onClick = { onPageSelected(AppPage.Home) },
            ) {
                Icon(Icons.Outlined.Home, contentDescription = null, modifier = Modifier.size(26.dp))
            }
            AppBottomDestination(
                label = "WORKOUT",
                selected = selectedPage == AppPage.Workout,
                selectedColor = MaterialTheme.colorScheme.primary,
                onClick = { onPageSelected(AppPage.Workout) },
            ) {
                Icon(Icons.Outlined.Build, contentDescription = null, modifier = Modifier.size(26.dp))
            }
        }
    }
}

@Composable
private fun RowScope.AppBottomDestination(
    label: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
    icon: @Composable () -> Unit,
) {
    val contentColor = if (selected) selectedColor else LiftMuted
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Surface(color = Color.Transparent, contentColor = contentColor) { icon() }
        Text(
            label,
            modifier = Modifier.padding(top = 8.dp),
            color = contentColor,
            fontFamily = DisplayFont,
            fontSize = 11.sp,
            letterSpacing = 1.5.sp,
        )
        Spacer(Modifier.height(6.dp))
        if (selected) SignalDot(Modifier.size(5.dp)) else Spacer(Modifier.size(5.dp))
        Spacer(Modifier.height(7.dp))
    }
}