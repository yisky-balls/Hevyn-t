package com.yifeng.commandcenter.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.launch
import com.yifeng.commandcenter.ui.theme.HevyntAccentContainer
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yifeng.commandcenter.timer.RestTimerManager
import com.yifeng.commandcenter.ui.theme.HevyntDisplayFont
import com.yifeng.commandcenter.ui.theme.HevyntOutline
import com.yifeng.commandcenter.ui.theme.HevyntSurface
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
    val topLevelPages = remember { listOf(AppPage.Home, AppPage.Workout, AppPage.Journal) }
    val initialPageIndex = remember { topLevelPages.indexOf(state.selectedPage).coerceAtLeast(0) }
    val pagerState = rememberPagerState(
        initialPage = initialPageIndex,
        pageCount = { topLevelPages.size },
    )
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { settledIndex ->
            val page = topLevelPages.getOrNull(settledIndex) ?: return@collect
            if (state.selectedPage != page) {
                viewModel.selectPage(page)
            }
        }
    }

    LaunchedEffect(state.selectedPage) {
        val targetIndex: Int = topLevelPages.indexOf(state.selectedPage)
        if (targetIndex != -1 && targetIndex != pagerState.currentPage) {
            pagerState.animateScrollToPage(targetIndex)
        }
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
        viewModel::notificationPermissionResult,
    )
    val openDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let { viewModel.loadWorkoutPlanFromUri(it) }
    }
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

    state.workoutPlanPreview?.let { preview ->
        HevyntDialog(
            onDismissRequest = viewModel::dismissWorkoutPlanPreview,
            title = {
                Text(
                    "IMPORT WORKOUT PLAN",
                    fontFamily = HevyntDisplayFont,
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Column {
                    Text(
                        text = preview.programName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${preview.routineCount} routines · ${preview.exerciseCount} exercises · ${preview.totalSets} planned sets",
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = HevyntTextSecondary,
                    )
                    HevyntDottedRule()
                    Spacer(Modifier.height(8.dp))
                    preview.routineNames.forEach { routineName ->
                        Text(
                            text = routineName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(vertical = 3.dp),
                        )
                    }
                    if (preview.alreadyExists) {
                        Spacer(Modifier.height(12.dp))
                        HevyntDottedRule()
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "This program is already installed.",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.error,
                        )
                        Text(
                            text = "Replace imported program? Existing unrelated routines will remain untouched.",
                            style = MaterialTheme.typography.bodySmall,
                            color = HevyntTextSecondary,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            },
            confirmButton = {
                if (preview.alreadyExists) {
                    Row {
                        TextButton(onClick = viewModel::dismissWorkoutPlanPreview) {
                            Text("KEEP EXISTING")
                        }
                        TextButton(onClick = { viewModel.confirmWorkoutPlanImport(replace = true) }) {
                            Text("REPLACE", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                        }
                    }
                } else {
                    TextButton(onClick = { viewModel.confirmWorkoutPlanImport(replace = false) }) {
                        Text("IMPORT", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissWorkoutPlanPreview) {
                    Text("CANCEL")
                }
            },
        )
    }

    state.workoutPlanImportError?.let { error ->
        HevyntDialog(
            onDismissRequest = viewModel::dismissWorkoutPlanImportError,
            title = { Text("UNABLE TO IMPORT PLAN", fontFamily = HevyntDisplayFont) },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissWorkoutPlanImportError) { Text("OK") }
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
                    HevyntBottomBar(
                        selectedPage = state.selectedPage,
                        onPageSelected = { page ->
                            coroutineScope.launch {
                                val targetIndex: Int = topLevelPages.indexOf(page)
                                if (targetIndex != -1) {
                                    pagerState.animateScrollToPage(targetIndex)
                                }
                            }
                            viewModel.selectPage(page)
                        },
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

            if (workout != null && state.isWorkoutExpanded) {
                pageStateHolder.SaveableStateProvider("active-workout:${workout.id}") {
                    ActiveWorkoutScreen(
                        workout = workout,
                        previousSets = state.previousSets,
                        exerciseHistory = state.exerciseHistory,
                        exerciseSuggestions = state.exerciseSuggestions,
                        isRoutineBuilder = state.routineBuildTarget != null,
                        onRequestExerciseSuggestions = viewModel::requestExerciseSuggestions,
                        onAddExercise = viewModel::addExercise,
                        onExerciseNameChanged = viewModel::changeExerciseName,
                        onExerciseNotesChanged = viewModel::changeExerciseNotes,
                        onExerciseRestChanged = viewModel::changeExerciseRest,
                        onExerciseRemoved = viewModel::removeExercise,
                        onExerciseMoved = viewModel::moveExercise,
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
                        onDuplicateSet = viewModel::duplicateSet,
                        onFinish = viewModel::finishOrSaveRoutine,
                        onMinimize = viewModel::minimizeWorkout,
                        onCancel = viewModel::discardWorkout,
                    )
                }
            } else {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = selectedGraph == null,
                    modifier = Modifier.fillMaxSize(),
                ) { pageIndex: Int ->
                    val page = topLevelPages[pageIndex]
                    pageStateHolder.SaveableStateProvider("page:$page") {
                        when (page) {
                            AppPage.Home -> HomeScreen(
                                routines = state.routines,
                                workoutHistory = state.workoutHistory,
                                scrollState = homeScrollState,
                                onOpenPrograms = viewModel::openPrograms,
                                onStartRoutine = viewModel::startRoutine,
                                onOpenSession = viewModel::openHistorySession,
                                onOpenDay = viewModel::openDay,
                                onRepeatLastWorkout = viewModel::repeatLastWorkout,
                            )

                            AppPage.Workout -> WorkoutProgramsScreen(
                                folders = state.routineFolders,
                                likelyNext = state.likelyNextRoutine,
                                activeWorkout = state.activeWorkout,
                                canRepeatLast = state.workoutHistory.any { !it.isLegacySummaryOnly },
                                listState = workoutListState,
                                onContinueWorkout = viewModel::resumeWorkout,
                                onStartRoutine = viewModel::startRoutine,
                                onBuildRoutine = viewModel::buildRoutine,
                                onCreateFolder = viewModel::createFolder,
                                onRenameFolder = viewModel::renameFolder,
                                onDeleteFolder = viewModel::deleteFolder,
                                onDeleteRoutine = viewModel::deleteRoutine,
                                onStartEmpty = viewModel::startEmptyWorkout,
                                onRepeatLast = viewModel::repeatLastWorkout,
                                onImportPlan = { openDocumentLauncher.launch(arrayOf("application/json", "text/*")) },
                            )

                            AppPage.Journal -> if (selectedGraph != null) {
                                JournalDetailScreen(
                                    graph = selectedGraph,
                                    photoFile = selectedGraph.photos.firstOrNull()?.let {
                                        state.journalPhotoFiles[it.id]
                                    },
                                    onBack = viewModel::goBackFromHistory,
                                    onDelete = viewModel::requestDeleteHistory,
                                )
                            } else {
                                JournalScreen(
                                    entries = state.journalEntries,
                                    photoFiles = state.journalPhotoFiles,
                                    filter = state.journalFilter,
                                    listState = journalListState,
                                    onClearFilter = viewModel::clearJournalFilter,
                                    onOpenSession = viewModel::openHistorySession,
                                )
                            }
                        }
                    }
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