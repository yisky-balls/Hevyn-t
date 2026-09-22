package com.yifeng.commandcenter.widget

import android.content.Context
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.ActiveWorkoutRepository
import com.yifeng.commandcenter.data.LikelyNextRoutineResolver
import com.yifeng.commandcenter.data.RoutineRecommendation
import com.yifeng.commandcenter.data.RoutineRepository
import com.yifeng.commandcenter.history.WorkoutHistoryRepository

internal sealed interface WorkoutWidgetState {
    data class Idle(val recommendation: RoutineRecommendation?) : WorkoutWidgetState

    data class Active(
        val workout: ActiveWorkout,
        val completedSets: Int,
        val totalSets: Int,
    ) : WorkoutWidgetState
}

internal object WorkoutWidgetStateLoader {
    suspend fun load(context: Context): WorkoutWidgetState {
        ActiveWorkoutRepository(context).load()?.let { workout ->
            val sets = workout.exercises.flatMap { it.sets }
            return WorkoutWidgetState.Active(
                workout = workout,
                completedSets = sets.count { it.completed },
                totalSets = sets.size,
            )
        }

        val routineRepository = RoutineRepository(context)
        routineRepository.initialize()
        val history = WorkoutHistoryRepository(context).sessions()
        val recommendation = LikelyNextRoutineResolver.resolve(
            folders = routineRepository.summaries(),
            history = history,
        ).recommendation
        return WorkoutWidgetState.Idle(recommendation)
    }
}
