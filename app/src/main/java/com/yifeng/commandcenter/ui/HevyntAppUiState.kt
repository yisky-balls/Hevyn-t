package com.yifeng.commandcenter.ui

import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.RoutineBuildTarget
import com.yifeng.commandcenter.data.RoutineFolderSummary
import com.yifeng.commandcenter.data.RoutineRecommendation
import com.yifeng.commandcenter.data.RoutineSummary
import com.yifeng.commandcenter.data.WorkoutDateRange
import com.yifeng.commandcenter.data.WorkoutPlanPreview
import com.yifeng.commandcenter.history.ExerciseHistorySummary
import com.yifeng.commandcenter.history.ExerciseSuggestion
import com.yifeng.commandcenter.history.PreviousSetPerformance
import com.yifeng.commandcenter.history.WorkoutHistoryGraph
import com.yifeng.commandcenter.history.WorkoutHistorySession
import com.yifeng.commandcenter.history.WorkoutJournalEntry
import com.yifeng.commandcenter.timer.RestTimerState
import java.io.File

enum class AppPage { Home, Workout, Journal }

enum class FinishPrompt { Unfinished, Empty }

sealed interface HevyntAppEffect {
    data object RequestNotificationPermission : HevyntAppEffect
}

data class HevyntAppUiState(
    val selectedPage: AppPage = AppPage.Workout,
    val routineFolders: List<RoutineFolderSummary> = emptyList(),
    val likelyNextRoutine: RoutineRecommendation? = null,
    val activeWorkout: ActiveWorkout? = null,
    val isWorkoutExpanded: Boolean = true,
    val routineBuildTarget: RoutineBuildTarget? = null,
    val showRoutineNameDialog: Boolean = false,
    val workoutHistory: List<WorkoutHistorySession> = emptyList(),
    val journalEntries: List<WorkoutJournalEntry> = emptyList(),
    val journalPhotoFiles: Map<String, File> = emptyMap(),
    val journalFilter: WorkoutDateRange? = null,
    val selectedHistoryGraph: WorkoutHistoryGraph? = null,
    val recapGraph: WorkoutHistoryGraph? = null,
    val finishPrompt: FinishPrompt? = null,
    val showDeleteHistoryDialog: Boolean = false,
    val errorMessage: String? = null,
    val isWorkoutSaving: Boolean = false,
    val isPhotoBusy: Boolean = false,
    val previousSets: Map<String, PreviousSetPerformance> = emptyMap(),
    val exerciseHistory: Map<String, ExerciseHistorySummary> = emptyMap(),
    val exerciseSuggestions: List<ExerciseSuggestion> = emptyList(),
    val timerState: RestTimerState? = null,
    val workoutPlanPreview: WorkoutPlanPreview? = null,
    val workoutPlanImportError: String? = null,
) {
    val routines: List<RoutineSummary> get() = routineFolders.flatMap(RoutineFolderSummary::routines)
}
