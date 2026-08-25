package com.yifeng.commandcenter.widget

import android.content.Context
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.ActiveWorkoutRepository
import com.yifeng.commandcenter.data.NextSetTarget
import com.yifeng.commandcenter.data.WorkoutProgress
import com.yifeng.commandcenter.history.PreviousSetPerformance
import com.yifeng.commandcenter.history.PreviousSetResolver
import com.yifeng.commandcenter.history.WorkoutHistoryGraph
import com.yifeng.commandcenter.history.WorkoutHistoryRepository
import com.yifeng.commandcenter.timer.RestTimerManager
import com.yifeng.commandcenter.timer.RestTimerState

internal sealed interface WorkoutWidgetState {
    data class Idle(val lastWorkout: WorkoutHistoryGraph?) : WorkoutWidgetState

    data class Active(
        val workout: ActiveWorkout,
        val target: NextSetTarget?,
        val previous: PreviousSetPerformance?,
    ) : WorkoutWidgetState

    data class Rest(
        val workout: ActiveWorkout,
        val timer: RestTimerState,
    ) : WorkoutWidgetState
}

internal object WorkoutWidgetStateLoader {
    suspend fun load(context: Context): WorkoutWidgetState {
        val workout = ActiveWorkoutRepository(context).load()
            ?: return WorkoutWidgetState.Idle(
                WorkoutHistoryRepository(context).latestRepeatableGraph(),
            )
        val timer = RestTimerManager.getState(context)
        if (timer != null) {
            return WorkoutWidgetState.Rest(
                workout = workout,
                timer = RestTimerManager.resolveState(timer, workout),
            )
        }
        val target = WorkoutProgress.firstUnfinishedSet(workout)
        val previous = target?.let {
            WorkoutHistoryRepository(context).previousSets(workout)[target.setId]
        }
        return WorkoutWidgetState.Active(workout, target, previous)
    }
}