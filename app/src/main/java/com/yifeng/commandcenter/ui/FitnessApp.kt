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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yifeng.commandcenter.timer.RestTimerManager
import com.yifeng.commandcenter.ui.theme.HevyntAccent
import com.yifeng.commandcenter.ui.theme.HevyntAccentContainer
import com.yifeng.commandcenter.ui.theme.HevyntError
import com.yifeng.commandcenter.ui.theme.HevyntOutline
import com.yifeng.commandcenter.ui.theme.HevyntSurfaceRaised
import com.yifeng.commandcenter.ui.theme.HevyntTextPrimary
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

    val currentSelectedPage by rememberUpdatedState(state.selectedPage)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { settledIndex ->
            val page = topLevelPages.getOrNull(settledIndex) ?: return@collect
            if (currentSelectedPage != page) {
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
        HevyntConfirmDialog(
            title = if (prompt == FinishPrompt.Unfinished) "Unfinished sets" else "No completed sets",
            message = if (prompt == FinishPrompt.Unfinished) {
                "Some sets are unfinished. Continue the workout or save them unchecked."
            } else {
                "Empty workouts are never saved. Return to the workout or discard it."
            },
            confirmText = if (prompt == FinishPrompt.Unfinished) "Save workout" else "Discard",
            dismissText = if (prompt == FinishPrompt.Unfinished) "Continue" else "Return",
            isDestructive = prompt == FinishPrompt.Empty,
            onConfirm = viewModel::confirmFinishPrompt,
            onDismiss = viewModel::dismissFinishPrompt,
        )
    }

    state.errorMessage?.let { message ->
        HevyntInfoDialog(
            title = "Something went wrong",
            message = message,
            onDismiss = viewModel::dismissError,
        )
    }

    if (state.showDeleteHistoryDialog) {
        HevyntConfirmDialog(
            title = "Delete session?",
            message = "This removes the workout and its app-owned photo from this device.",
            confirmText = "Delete",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = viewModel::confirmDeleteHistory,
            onDismiss = viewModel::dismissDeleteHistory,
        )
    }

    if (state.showDiscardSavedExercisesConfirm) {
        HevyntConfirmDialog(
            title = "Discard unsaved changes?",
            message = "You have uncommitted modifications. Discard all changes to saved exercises?",
            confirmText = "Discard",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = viewModel::confirmDiscardSavedExercises,
            onDismiss = viewModel::dismissDiscardSavedExercisesConfirm,
        )
    }

    state.workoutPlanPreview?.let { preview ->
        HevyntSheet(
            onDismiss = viewModel::dismissWorkoutPlanPreview,
            title = "Import workout plan",
            subtitle = preview.programName,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HevyntChip(text = "${preview.routineCount} routines")
                    HevyntChip(text = "${preview.exerciseCount} exercises")
                    HevyntChip(text = "${preview.totalSets} sets")
                }

                Spacer(Modifier.height(14.dp))

                HevyntSheetSection {
                    preview.routineNames.forEachIndexed { index, routineName ->
                        if (index > 0) {
                            HevyntDottedRule(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color = HevyntOutline.copy(alpha = 0.7f),
                            )
                        }
                        Text(
                            text = routineName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = HevyntTextPrimary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                }

                if (preview.alreadyExists) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = HevyntAccentContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                            Text(
                                text = "This program is already installed",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = HevyntError,
                            )
                            Text(
                                text = "Replacing it updates imported routines while keeping unrelated routines untouched.",
                                fontSize = 12.5.sp,
                                color = HevyntTextSecondary,
                                modifier = Modifier.padding(top = 3.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    HevyntTonalButton(
                        onClick = viewModel::dismissWorkoutPlanPreview,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = if (preview.alreadyExists) "Keep existing" else "Cancel",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = HevyntTextSecondary,
                        )
                    }
                    HevyntPrimaryButton(
                        onClick = { viewModel.confirmWorkoutPlanImport(replace = preview.alreadyExists) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = if (preview.alreadyExists) "Replace" else "Import",
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }

    state.workoutPlanImportError?.let { error ->
        HevyntInfoDialog(
            title = "Unable to import plan",
            message = error,
            onDismiss = viewModel::dismissWorkoutPlanImportError,
        )
    }

    if (state.showBodyMetricsConfigDialog) {
        BodyMetricsConfigSheet(
            currentUrl = state.bodyMetrics.sheetUrl,
            isConfigured = state.bodyMetrics.isConfigured,
            onSave = viewModel::saveBodyMetricsSheetUrl,
            onDisconnect = viewModel::disconnectBodyMetricsSheet,
            onDismiss = viewModel::dismissBodyMetricsConfig,
        )
    }

    if (state.showProgramSheetConfigDialog) {
        ProgramSheetConfigSheet(
            currentUrl = state.programSheet.sheetUrl,
            isConfigured = state.programSheet.isConfigured,
            onSave = viewModel::saveProgramSheetUrl,
            onDisconnect = viewModel::disconnectProgramSheet,
            onDismiss = viewModel::dismissProgramSheetConfig,
        )
    }

    if (state.showExerciseProgressPicker) {
        ExercisePickerSheet(
            options = state.exerciseProgressOptions,
            selectedExerciseKey = state.selectedProgressExerciseKey,
            onSelect = viewModel::selectProgressExercise,
            onDismiss = viewModel::dismissExerciseProgressPicker,
        )
    }

    val showBottomBar = state.recapGraph == null &&
        (state.activeWorkout == null || !state.isWorkoutExpanded) &&
        !state.isSavedExercisesOpen &&
        !state.isBodyMetricsOpen &&
        !state.isExerciseProgressOpen
    val layoutDirection = LocalLayoutDirection.current

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (showBottomBar) {
                        Modifier.padding(
                            top = innerPadding.calculateTopPadding(),
                            start = innerPadding.calculateStartPadding(layoutDirection),
                            end = innerPadding.calculateEndPadding(layoutDirection),
                        )
                    } else {
                        Modifier.padding(innerPadding)
                    },
                ),
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
                    val resolvedTimer = state.timerState?.let { RestTimerManager.resolveState(it, workout) }
                    ActiveWorkoutScreen(
                        workout = workout,
                        previousSets = state.previousSets,
                        exerciseHistory = state.exerciseHistory,
                        exerciseSuggestions = state.exerciseSuggestions,
                        timerState = resolvedTimer,
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
                        isGlyphSupported = state.isGlyphSupported,
                        isGlyphTimerEnabled = state.isGlyphTimerEnabled,
                        isGlyphPulseEnabled = state.isGlyphPulseEnabled,
                        onToggleGlyphTimer = viewModel::setGlyphTimerEnabled,
                        onToggleGlyphPulse = viewModel::setGlyphPulseEnabled,
                        onStartRestTimer = viewModel::startRestTimerForExercise,
                        onSubtractRestTime = viewModel::subtractRestTime,
                        onAddRestTime = viewModel::addRestTime,
                        onSkipRestTime = viewModel::skipRestTime,
                    )
                }
            } else if (state.isSavedExercisesOpen && state.savedExercisesDraft != null) {
                SavedExercisesScreen(
                    draft = state.savedExercisesDraft!!,
                    onEditExerciseDetails = viewModel::editSavedExerciseDetails,
                    onMergeExercises = viewModel::mergeSavedExercises,
                    onDeleteExercise = viewModel::deleteSavedExercise,
                    onDeleteExerciseInstance = viewModel::deleteSavedExerciseInstance,
                    onDeduplicateAll = viewModel::deduplicateAllSavedExercises,
                    onCommit = viewModel::commitSavedExercises,
                    onBack = viewModel::handleBackFromSavedExercises,
                )
            } else if (state.isBodyMetricsOpen) {
                BodyMetricsScreen(
                    syncState = state.bodyMetrics,
                    selectedPeriod = state.bodyMetricsPeriod,
                    onSelectPeriod = viewModel::selectBodyMetricsPeriod,
                    onRefresh = { viewModel.refreshBodyMetrics(force = true) },
                    onOpenConfig = viewModel::openBodyMetricsConfig,
                    onBack = viewModel::closeBodyMetrics,
                )
            } else if (state.isExerciseProgressOpen) {
                ExerciseProgressScreen(
                    rows = state.exerciseProgressRows,
                    options = state.exerciseProgressOptions,
                    selectedExerciseKey = state.selectedProgressExerciseKey,
                    metric = state.exerciseProgressMetric,
                    period = state.exerciseProgressPeriod,
                    onSelectMetric = viewModel::selectExerciseProgressMetric,
                    onSelectPeriod = viewModel::selectExerciseProgressPeriod,
                    onOpenPicker = viewModel::openExerciseProgressPicker,
                    onBack = viewModel::closeExerciseProgress,
                )
            } else {
                val allRoutines = remember(state.routineFolders) { state.routines }
                val canRepeatLast = remember(state.workoutHistory) {
                    state.workoutHistory.any { !it.isLegacySummaryOnly }
                }
                val onImportPlan = remember(openDocumentLauncher) {
                    { openDocumentLauncher.launch(arrayOf("application/json", "text/*")) }
                }
                val onRefreshProgramSheet = remember(viewModel) {
                    { viewModel.refreshProgramSheet(force = true) }
                }
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 2,
                    userScrollEnabled = selectedGraph == null &&
                        !state.isSavedExercisesOpen &&
                        !state.isBodyMetricsOpen &&
                        !state.isExerciseProgressOpen,
                    modifier = Modifier.fillMaxSize(),
                ) { pageIndex: Int ->
                    val page = topLevelPages[pageIndex]
                    pageStateHolder.SaveableStateProvider("page:$page") {
                        when (page) {
                            AppPage.Home -> HomeScreen(
                                routines = allRoutines,
                                workoutHistory = state.workoutHistory,
                                likelyNext = state.likelyNextRoutine,
                                activeWorkout = state.activeWorkout,
                                scrollState = homeScrollState,
                                isGlyphSupported = state.isGlyphSupported,
                                isGlyphTimerEnabled = state.isGlyphTimerEnabled,
                                isGlyphPulseEnabled = state.isGlyphPulseEnabled,
                                isSoftLockSupported = state.isSoftLockSupported,
                                isSoftLockEnabled = state.isSoftLockEnabled,
                                isSoftLockServiceEnabled = state.isSoftLockServiceEnabled,
                                bodyMetrics = state.bodyMetrics,
                                exerciseProgressRows = state.exerciseProgressRows,
                                exerciseProgressOptions = state.exerciseProgressOptions,
                                selectedProgressExerciseKey = state.selectedProgressExerciseKey,
                                exerciseProgressMetric = state.exerciseProgressMetric,
                                onContinueWorkout = viewModel::resumeWorkout,
                                onOpenPrograms = viewModel::openPrograms,
                                onOpenJournal = viewModel::openJournal,
                                onStartRoutine = viewModel::startRoutine,
                                onOpenSession = viewModel::openHistorySession,
                                onOpenDay = viewModel::openDay,
                                onRepeatLastWorkout = viewModel::repeatLastWorkout,
                                onImportPlan = onImportPlan,
                                onManageSavedExercises = viewModel::openSavedExercises,
                                onOpenBodyMetrics = viewModel::openBodyMetrics,
                                onConfigureBodyMetrics = viewModel::openBodyMetricsConfig,
                                onSelectExerciseProgressMetric = viewModel::selectExerciseProgressMetric,
                                onOpenExerciseProgressPicker = viewModel::openExerciseProgressPicker,
                                onOpenExerciseProgress = viewModel::openExerciseProgress,
                                onToggleGlyphTimer = viewModel::setGlyphTimerEnabled,
                                onToggleGlyphPulse = viewModel::setGlyphPulseEnabled,
                                onToggleSoftLock = viewModel::setSoftLockEnabled,
                                onOpenSoftLockSettings = viewModel::openSoftLockAccessibilitySettings,
                            )

                            AppPage.Workout -> WorkoutProgramsScreen(
                                folders = state.routineFolders,
                                likelyNext = state.likelyNextRoutine,
                                activeWorkout = state.activeWorkout,
                                canRepeatLast = canRepeatLast,
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
                                onImportPlan = onImportPlan,
                                onManageSavedExercises = viewModel::openSavedExercises,
                                programSheet = state.programSheet,
                                onOpenProgramSheetConfig = viewModel::openProgramSheetConfig,
                                onRefreshProgramSheet = onRefreshProgramSheet,
                                onSelectProgramSheetWeek = viewModel::selectProgramSheetWeek,
                                onStartProgramSheetDay = viewModel::startProgramSheetDay,
                                onReorderRoutines = viewModel::reorderRoutines,
                            )

                            AppPage.Journal -> if (selectedGraph != null) {
                                JournalDetailScreen(
                                    graph = selectedGraph,
                                    photoFile = selectedGraph.photos.firstOrNull()?.let {
                                        state.journalPhotoFiles[it.id]
                                    },
                                    isPhotoBusy = state.isPhotoBusy,
                                    onPhotoSelected = viewModel::selectHistoryPhoto,
                                    onRemovePhoto = viewModel::removeHistoryPhoto,
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

            if (showBottomBar) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(),
                ) {
                    state.timerState
                        ?.let { timer -> workout?.let { RestTimerManager.resolveState(timer, it) } }
                        ?.let { timer ->
                            RestTimerBar(
                                state = timer,
                                modifier = Modifier
                                    .padding(horizontal = HevyntPageHorizontalPadding, vertical = 6.dp),
                                onSubtract = viewModel::subtractRestTime,
                                onAdd = viewModel::addRestTime,
                                onSkip = viewModel::skipRestTime,
                            )
                        }
                    state.activeWorkout?.takeIf { !state.isWorkoutExpanded }?.let { minimized ->
                        MinimizedWorkoutBar(
                            workout = minimized,
                            onResume = viewModel::resumeWorkout,
                            onDiscard = viewModel::discardWorkout,
                        )
                    }
                    HevyntBottomBar(
                        selectedPage = state.selectedPage,
                        onPageSelected = viewModel::selectPage,
                    )
                }
            }

            if (state.isWorkoutSaving) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.85f))
                        .clickable(onClick = {}),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = HevyntSurfaceRaised,
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 26.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CircularProgressIndicator(
                                color = HevyntAccent,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(32.dp),
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "Saving workout…",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = HevyntTextPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}