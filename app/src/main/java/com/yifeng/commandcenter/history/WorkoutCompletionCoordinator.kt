package com.yifeng.commandcenter.history

import com.yifeng.commandcenter.data.ActiveWorkout
import kotlinx.coroutines.CancellationException

class WorkoutCompletionCoordinator(
    private val saveGraph: suspend (WorkoutHistoryGraph) -> Unit,
    private val clearActiveWorkout: suspend () -> Unit,
    private val beginFinishing: suspend (ActiveWorkout) -> Boolean = { true },
    private val cancelFinishing: suspend (String) -> Unit = {},
) {
    suspend fun finish(
        workout: ActiveWorkout,
        completedAtMillis: Long,
    ): Result<WorkoutHistoryGraph> {
        val draft = CompletedWorkoutMapper.fromActiveWorkout(workout, completedAtMillis)
        if (draft.graph.session.completedSetCount == 0) {
            return Result.failure(
                IllegalArgumentException("Complete at least one set before saving this workout."),
            )
        }
        val started = runCatching { beginFinishing(workout) }
            .getOrElse { return Result.failure(it) }
        if (!started) {
            return Result.failure(
                IllegalStateException("The active workout changed before it could be saved."),
            )
        }
        try {
            saveGraph(draft.graph)
        } catch (cancellation: CancellationException) {
            // The Room transaction may already have committed. Keep the durable finishing
            // marker so startup reconciliation can either clear or safely unfreeze the workout.
            throw cancellation
        } catch (error: Throwable) {
            runCatching { cancelFinishing(workout.id) }
            return Result.failure(error)
        }
        return try {
            clearActiveWorkout()
            Result.success(draft.graph)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            // Room has committed. Preserve the marker so startup reconciliation clears the
            // matching active session instead of allowing edits that would not reach history.
            Result.failure(error)
        }
    }
}