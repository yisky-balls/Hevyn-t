package com.yifeng.commandcenter.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Home
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
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yifeng.commandcenter.timer.RestTimerManager
import com.yifeng.commandcenter.ui.theme.HevyntDisplayFont
import com.yifeng.commandcenter.ui.theme.HevyntOutline
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceRaised
import com.yifeng.commandcenter.ui.theme.HevyntTextSecondary
import kotlinx.coroutines.flow.collectLatest

@Composable
fun FitnessApp(viewModel: HevyntAppViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val pageStateHolder = rememberSaveableStateHolder()
    val homeScrollState = rememberScrollState()
    val workoutListState = rememberLazyListState()
    val journalListState = rememberLazyListState()
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
        viewModel::notificationPermissionResult,
    )
    LaunchedEffect(viewModel) {
        viewModel.effects.collectLatest { effect ->
            when (effect) {
                HevyntAppEffect.RequestNotificationPermission -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }
            }
        }
    }

    state.activeWorkout?.takeIf { state.showRoutineNameDialog }?.let { workout ->
        RoutineNameDialog(
            initialName = workout.routineName,
            isEditing = state.routineBuildTarget?.routineId != null,
            onDismiss = viewModel::dismissRoutineNameDialog,
            onSave = viewModel::saveBuiltRoutine,
        )
    }

    state.finishPrompt?.let { prompt ->
        HevyntDialog(
            onDismissRequest = viewModel::dismissFinishPrompt,
            title = {
                Text(
                    if (prompt == FinishPrompt.Unfinished) "UNFINISHED SETS" else "NO COMPLETED SETS",
                    fontFamily = HevyntDisplayFont,
                )
            },
            text = {
                Text(
                    if (prompt == FinishPrompt.Unfinished) {
                        "Some sets are unfinished. Continue the workout or save them unchecked."
                    } else {
                        "Empty workouts are never saved. Return to the workout or discard it."
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmFinishPrompt) {
                    Text(
                        if (prompt == FinishPrompt.Unfinished) "SAVE WORKOUT" else "DISCARD",
                        color = if (prompt == FinishPrompt.Empty) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissFinishPrompt) {
                    Text(if (prompt == FinishPrompt.Unfinished) "CONTINUE WORKOUT" else "RETURN")
                }
            },
        )
    }

    state.errorMessage?.let { message ->
        HevyntDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text("SOMETHING WENT WRONG", fontFamily = HevyntDisplayFont) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissError) { Text("OK") }
            },
        )
    }

    if (state.showDeleteHistoryDialog) {
        HevyntDialog(
            onDismissRequest = viewModel::dismissDeleteHistory,
            title = { Text("DELETE SESSION?", fontFamily = HevyntDisplayFont) },
            text = { Text("This removes the workout and its app-owned photo from this device.") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDeleteHistory) {
                    Text("DELETE", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissDeleteHistory) { Text("CANCEL") }
            },
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (state.recapGraph == null && (state.activeWorkout == null || !state.isWorkoutExpanded)) {
                Column {
                    state.activeWorkout?.takeIf { !state.isWorkoutExpanded }?.let { workout ->
                        MinimizedWorkoutBar(
                            workout = workout,
                            onResume = viewModel::resumeWorkout,
                            onDiscard = viewModel::discardWorkout,
                        )
                    }
                    AppBottomBar(
                        selectedPage = state.selectedPage,
                        onPageSelected = viewModel::selectPage,
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val workout = state.activeWorkout
            val recap = state.recapGraph
            val selectedGraph = state.selectedHistoryGraph
            if (recap != null) {
                WorkoutRecapScreen(
                    graph = recap,
                    photoFile = recap.photos.firstOrNull()?.let { state.journalPhotoFiles[it.id] },
                    isPhotoBusy = state.isPhotoBusy,
                    errorMessage = state.errorMessage,
                    onPhotoSelected = viewModel::selectRecapPhoto,
                    onRemovePhoto = viewModel::removeRecapPhoto,
                    onDone = viewModel::finishRecap,
                )
                return@Box
            }
            val contentKey = if (workout != null && state.isWorkoutExpanded) {
                "active-workout:${workout.id}"
            } else {
                "page:${state.selectedPage}"
            }
            pageStateHolder.SaveableStateProvider(contentKey) {
                when {
                    workout != null && state.isWorkoutExpanded -> ActiveWorkoutScreen(
                        workout = workout,
                        previousSets = state.previousSets,
                        exerciseSuggestions = state.exerciseSuggestions,
                        isRoutineBuilder = state.routineBuildTarget != null,
                        onRequestExerciseSuggestions = viewModel::requestExerciseSuggestions,
                        onAddExercise = viewModel::addExercise,
                        onExerciseNameChanged = viewModel::changeExerciseName,
                        onExerciseNotesChanged = viewModel::changeExerciseNotes,
                        onExerciseRestChanged = viewModel::changeExerciseRest,
                        onExerciseRemoved = viewModel::removeExercise,
                        onSetChanged = viewModel::changeSet,
                        onSetCompletionChanged = { exerciseId, setId, completed ->
                            viewModel.changeSetCompletion(
                                exerciseId = exerciseId,
                                setId = setId,
                                completed = completed,
                                canPostNotifications = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS,
                                    ) == PackageManager.PERMISSION_GRANTED,
                            )
                        },
                        onSetRemoved = viewModel::removeSet,
                        onSetAdded = viewModel::addSet,
                        onFinish = viewModel::finishOrSaveRoutine,
                        onMinimize = viewModel::minimizeWorkout,
                        onCancel = viewModel::discardWorkout,
                    )

                    state.selectedPage == AppPage.Home -> HomeScreen(
                        routines = state.routines,
                        workoutHistory = state.workoutHistory,
                        scrollState = homeScrollState,
                        onOpenPrograms = viewModel::openPrograms,
                        onStartRoutine = viewModel::startRoutine,
                        onOpenSession = viewModel::openHistorySession,
                        onOpenDay = viewModel::openDay,
                    )

                    state.selectedPage == AppPage.Workout -> WorkoutProgramsScreen(
                        folders = state.routineFolders,
                        likelyNext = state.likelyNextRoutine,
                        activeWorkout = state.activeWorkout,
                        listState = workoutListState,
                        onContinueWorkout = viewModel::resumeWorkout,
                        onStartRoutine = viewModel::startRoutine,
                        onBuildRoutine = viewModel::buildRoutine,
                        onCreateFolder = viewModel::createFolder,
                        onRenameFolder = viewModel::renameFolder,
                        onDeleteFolder = viewModel::deleteFolder,
                        onDeleteRoutine = viewModel::deleteRoutine,
                        onStartEmpty = viewModel::startEmptyWorkout,
                    )

                    selectedGraph != null -> JournalDetailScreen(
                        graph = selectedGraph,
                        photoFile = selectedGraph.photos.firstOrNull()?.let {
                            state.journalPhotoFiles[it.id]
                        },
                        onBack = viewModel::goBackFromHistory,
                        onDelete = viewModel::requestDeleteHistory,
                    )

                    else -> JournalScreen(
                        entries = state.journalEntries,
                        photoFiles = state.journalPhotoFiles,
                        filter = state.journalFilter,
                        listState = journalListState,
                        onClearFilter = viewModel::clearJournalFilter,
                        onOpenSession = viewModel::openHistorySession,
                    )
                }
            }

            state.timerState
                ?.takeIf { state.isWorkoutExpanded }
                ?.let { timer -> workout?.let { RestTimerManager.resolveState(timer, it) } }
                ?.let { timer ->
                    RestTimerBar(
                        state = timer,
                        modifier = Modifier.align(Alignment.BottomCenter),
                        onSubtract = viewModel::subtractRestTime,
                        onAdd = viewModel::addRestTime,
                        onSkip = viewModel::skipRestTime,
                    )
                }

            if (state.isWorkoutSaving) {
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
                        Text("SAVING WORKOUT", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
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
    Surface(color = HevyntSurfaceRaised) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(HevyntOutline),
            )
            Row(modifier = Modifier.fillMaxWidth().height(74.dp)) {
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
                    Icon(
                        Icons.AutoMirrored.Outlined.List,
                        contentDescription = null,
                        modifier = Modifier.size(25.dp),
                    )
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
    val contentColor = if (selected) MaterialTheme.colorScheme.primary else HevyntTextSecondary
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