package com.yifeng.commandcenter.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.yifeng.commandcenter.data.ActiveExercise
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.ActiveWorkoutOperations
import com.yifeng.commandcenter.data.ActiveWorkoutRepository
import com.yifeng.commandcenter.data.LastWorkoutSummary
import com.yifeng.commandcenter.data.Routine
import com.yifeng.commandcenter.data.RoutineExercise
import com.yifeng.commandcenter.data.RoutineFolder
import com.yifeng.commandcenter.data.RoutineFolderOperations
import com.yifeng.commandcenter.data.RoutineRepository
import com.yifeng.commandcenter.timer.RestTimerManager
import com.yifeng.commandcenter.timer.RestTimerState
import kotlinx.coroutines.delay

private enum class AppPage { Home, Workout }

private data class RoutineBuildTarget(
    val folderId: String,
    val routineId: String?,
)

private data class AppContentState(
    val page: AppPage,
    val workoutExpanded: Boolean,
)

@Composable
fun FitnessApp() {
    val context = LocalContext.current
    val repository = remember { RoutineRepository(context) }
    val activeWorkoutRepository = remember { ActiveWorkoutRepository(context) }
    var selectedPage by remember { mutableStateOf(AppPage.Workout) }
    var folders by remember { mutableStateOf(repository.loadFolders()) }
    var activeWorkout by remember { mutableStateOf(activeWorkoutRepository.load()) }
    var isWorkoutExpanded by remember {
        mutableStateOf(activeWorkout == null || activeWorkoutRepository.isExpanded())
    }
    var routineBuildTarget by remember { mutableStateOf<RoutineBuildTarget?>(null) }
    var showRoutineNameDialog by remember { mutableStateOf(false) }
    var workoutHistory by remember { mutableStateOf(repository.loadWorkoutHistory()) }
    var lastWorkout by remember { mutableStateOf(workoutHistory.maxByOrNull { it.completedAtMillis }) }
    var timerState by remember { mutableStateOf<RestTimerState?>(RestTimerManager.getState(context)) }
    var activeWorkoutRevision by remember { mutableLongStateOf(activeWorkoutRepository.revision()) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) RestTimerManager.refresh(context)
    }

    LaunchedEffect(Unit) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            timerState = RestTimerManager.getState(context)
            val latestRevision = activeWorkoutRepository.revision()
            if (latestRevision != activeWorkoutRevision) {
                activeWorkoutRevision = latestRevision
                activeWorkout = activeWorkoutRepository.load()
            }
            delay(500)
        }
    }

    fun startRoutine(routine: Routine) {
        if (activeWorkout != null) {
            isWorkoutExpanded = true
            activeWorkoutRepository.setExpanded(true)
            return
        }
        routineBuildTarget = null
        activeWorkout = ActiveWorkout(
            routineName = routine.name,
            exercises = routine.exercises.map { exercise ->
                ActiveExercise(name = exercise.name, restSeconds = exercise.restSeconds)
            },
        )
        activeWorkoutRepository.save(activeWorkout!!)
        isWorkoutExpanded = true
        activeWorkoutRepository.setExpanded(true)
        activeWorkoutRevision = activeWorkoutRepository.revision()
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
        routineBuildTarget = RoutineBuildTarget(folder.id, routine?.id)
        activeWorkout = ActiveWorkout(
            routineName = routine?.name.orEmpty(),
            exercises = routine?.exercises.orEmpty().map { exercise ->
                ActiveExercise(
                    id = exercise.id,
                    name = exercise.name,
                    restSeconds = exercise.restSeconds,
                )
            },
        )
        activeWorkoutRepository.save(activeWorkout!!)
        isWorkoutExpanded = true
        activeWorkoutRepository.setExpanded(true)
        activeWorkoutRevision = activeWorkoutRepository.revision()
        selectedPage = AppPage.Workout
    }

    fun saveBuiltRoutine(name: String) {
        val target = routineBuildTarget ?: return
        val workout = activeWorkout ?: return
        val routine = Routine(
            id = target.routineId ?: Routine(name = "", exercises = emptyList()).id,
            name = name,
            exercises = workout.exercises.mapNotNull { exercise ->
                exercise.name.trim().takeIf { it.isNotEmpty() }?.let { exerciseName ->
                    RoutineExercise(
                        id = exercise.id,
                        name = exerciseName,
                        restSeconds = exercise.restSeconds,
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
        showRoutineNameDialog = false
        routineBuildTarget = null
        activeWorkout = null
        activeWorkoutRepository.clear()
        activeWorkoutRevision = activeWorkoutRepository.revision()
    }

    fun finishWorkout(workout: ActiveWorkout) {
        val completedSets = workout.exercises.sumOf { exercise ->
            exercise.sets.count { it.completed }
        }
        val volume = workout.exercises.sumOf { exercise ->
            exercise.sets.filter { it.completed }.sumOf { set ->
                (set.weight.toIntOrNull() ?: 0) * (set.reps.toIntOrNull() ?: 0)
            }
        }
        val summary = LastWorkoutSummary(
            name = workout.routineName,
            completedAtMillis = System.currentTimeMillis(),
            completedSets = completedSets,
            volumeKg = volume,
        )
        repository.saveLastWorkout(summary)
        workoutHistory = workoutHistory + summary
        lastWorkout = summary
        RestTimerManager.cancel(context)
        timerState = null
        activeWorkout = null
        activeWorkoutRepository.clear()
        activeWorkoutRevision = activeWorkoutRepository.revision()
    }

    fun updateActiveWorkout(transform: (ActiveWorkout) -> ActiveWorkout?) {
        activeWorkoutRepository.update(transform)?.let { updated ->
            activeWorkout = updated
            activeWorkoutRevision = activeWorkoutRepository.revision()
        }
    }

    val routines = folders.flatMap { it.routines }
    if (showRoutineNameDialog && activeWorkout != null) {
        RoutineNameDialog(
            initialName = activeWorkout!!.routineName,
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
                    if (activeWorkout != null && !isWorkoutExpanded) {
                        AnimatedVisibility(
                            visible = timerState != null,
                            enter = slideInVertically(tween(240)) { it / 2 } + fadeIn(tween(180)),
                            exit = slideOutVertically(tween(200)) { it / 2 } + fadeOut(tween(160)),
                        ) {
                            timerState?.let { state ->
                                RestTimerBar(
                                    state = state,
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
                        MinimizedWorkoutBar(
                            workout = activeWorkout!!,
                            onResume = {
                                isWorkoutExpanded = true
                                activeWorkoutRepository.setExpanded(true)
                            },
                            onDiscard = {
                                RestTimerManager.cancel(context)
                                timerState = null
                                activeWorkout = null
                                activeWorkoutRepository.clear()
                                activeWorkoutRevision = activeWorkoutRepository.revision()
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
                                activeWorkoutRevision = activeWorkoutRepository.revision()
                                if (routineBuildTarget == null) {
                                    RestTimerManager.start(
                                        context = context,
                                        durationSeconds = result.restSeconds,
                                        completedExerciseId = result.completedExerciseId,
                                        completedSetId = result.completedSetId,
                                        exerciseName = result.completedExerciseName,
                                    )
                                    timerState = RestTimerManager.getState(context)
                                }
                            }
                        } else {
                            updateActiveWorkout {
                                ActiveWorkoutOperations.uncompleteSet(it, exerciseId, setId)
                            }
                        }
                    },
                    onSetRemoved = { exerciseId, setId ->
                        activeWorkoutRepository.update {
                            ActiveWorkoutOperations.removeSet(it, exerciseId, setId)
                        }?.let { updated ->
                            activeWorkout = updated
                            activeWorkoutRevision = activeWorkoutRepository.revision()
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
                        RestTimerManager.cancel(context)
                        showRoutineNameDialog = false
                        routineBuildTarget = null
                        activeWorkout = null
                        activeWorkoutRepository.clear()
                        activeWorkoutRevision = activeWorkoutRepository.revision()
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
                        routineBuildTarget = null
                        activeWorkout = ActiveWorkout(
                            routineName = "Empty workout",
                            exercises = emptyList(),
                        )
                        activeWorkoutRepository.save(activeWorkout!!)
                        isWorkoutExpanded = true
                        activeWorkoutRepository.setExpanded(true)
                        activeWorkoutRevision = activeWorkoutRepository.revision()
                    },
                )
            }
            }

            if (workout != null && isWorkoutExpanded && timerState != null) {
                RestTimerBar(
                    state = timerState!!,
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
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        NavigationBarItem(
            selected = selectedPage == AppPage.Home,
            onClick = { onPageSelected(AppPage.Home) },
            icon = { Icon(Icons.Outlined.Home, contentDescription = null) },
            label = { Text("Home") },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                selectedIconColor = Color.White,
                selectedTextColor = Color.White,
            ),
        )
        NavigationBarItem(
            selected = selectedPage == AppPage.Workout,
            onClick = { onPageSelected(AppPage.Workout) },
            icon = { Icon(Icons.Outlined.FitnessCenter, contentDescription = null) },
            label = { Text("Workout") },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                selectedIconColor = Color.White,
                selectedTextColor = Color.White,
            ),
        )
    }
}