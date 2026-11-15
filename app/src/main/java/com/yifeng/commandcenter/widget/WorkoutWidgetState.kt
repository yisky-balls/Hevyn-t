package com.yifeng.commandcenter.widget

import android.content.Context
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.ActiveWorkoutRepository
import com.yifeng.commandcenter.data.LikelyNextRoutineResolver
import com.yifeng.commandcenter.data.NextSetTarget
import com.yifeng.commandcenter.data.RoutineRecommendation
import com.yifeng.commandcenter.data.RoutineRepository
import com.yifeng.commandcenter.data.WorkoutProgress
import com.yifeng.commandcenter.data.formatWeight
import com.yifeng.commandcenter.history.WorkoutHistoryRepository
import com.yifeng.commandcenter.timer.RestTimerManager

internal sealed interface WorkoutWidgetState {
    data class Idle(
        val recommendation: RoutineRecommendation?,
        val nextRoutineName: String,
        val exerciseCount: Int,
        val previewExercises: List<String> = emptyList(),
    ) : WorkoutWidgetState

    data class Active(
        val workout: ActiveWorkout,
        val completedSets: Int,
        val totalSets: Int,
        val target: NextSetTarget?,
        val currentExerciseName: String,
        val currentSetNumber: Int,
        val totalSetsInExercise: Int,
        val weightKg: String,
        val reps: String,
        val isRestActive: Boolean,
        val restTimerEndMillis: Long?,
        val restTimerDurationSeconds: Int?,
    ) : WorkoutWidgetState
}

internal object WorkoutWidgetStateLoader {
    suspend fun load(context: Context): WorkoutWidgetState {
        ActiveWorkoutRepository(context).load()?.let { workout ->
            val sets = workout.exercises.flatMap { it.sets }
            val completedCount = sets.count { it.completed }
            val totalCount = sets.size
            val target = WorkoutProgress.firstUnfinishedSet(workout)
            val currentExercise = target?.exerciseIndex?.let { workout.exercises.getOrNull(it) }
                ?: workout.exercises.firstOrNull()

            val restState = RestTimerManager.getState(context)
            val now = System.currentTimeMillis()
            val isRestActive = restState != null && restState.endMillis > now

            val rawWeight = target?.weight?.ifBlank { null }
                ?: currentExercise?.sets?.firstOrNull()?.weight?.ifBlank { null }
                ?: "0"
            val weightVal = rawWeight.toDoubleOrNull()?.let(::formatWeight) ?: rawWeight
            val repsVal = target?.reps?.ifBlank { "0" }
                ?: currentExercise?.sets?.firstOrNull()?.reps?.ifBlank { "0" }
                ?: "0"

            return WorkoutWidgetState.Active(
                workout = workout,
                completedSets = completedCount,
                totalSets = totalCount,
                target = target,
                currentExerciseName = target?.exerciseName ?: currentExercise?.name ?: "Workout",
                currentSetNumber = target?.setNumber ?: totalCount,
                totalSetsInExercise = currentExercise?.sets?.size ?: 1,
                weightKg = weightVal,
                reps = repsVal,
                isRestActive = isRestActive,
                restTimerEndMillis = restState?.endMillis,
                restTimerDurationSeconds = restState?.durationSeconds,
            )
        }

        val routineRepository = RoutineRepository(context)
        routineRepository.initialize()
        val history = WorkoutHistoryRepository(context).sessions()
        val recommendation = LikelyNextRoutineResolver.resolve(
            folders = routineRepository.summaries(),
            history = history,
        ).recommendation

        val nextName = recommendation?.routine?.name ?: "CHOOSE ROUTINE"
        val count = recommendation?.routine?.exerciseCount ?: 0
        val preview = recommendation?.routine?.let { rec ->
            routineRepository.routine(rec.id)?.exercises?.take(3)?.map { ex ->
                "• ${ex.name} (${ex.sets.size} sets)"
            }
        } ?: emptyList()

        return WorkoutWidgetState.Idle(
            recommendation = recommendation,
            nextRoutineName = nextName,
            exerciseCount = count,
            previewExercises = preview,
        )
    }
}
