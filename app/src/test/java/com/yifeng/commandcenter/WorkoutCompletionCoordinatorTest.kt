package com.yifeng.commandcenter

import com.yifeng.commandcenter.data.ActiveExercise
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.WorkoutSet
import com.yifeng.commandcenter.history.WorkoutCompletionCoordinator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutCompletionCoordinatorTest {
    private val workout = ActiveWorkout(
        id = "session",
        routineName = "Upper",
        startedAtMillis = 1_000L,
        exercises = listOf(
            ActiveExercise(
                name = "Bench",
                sets = listOf(WorkoutSet(weight = "80", reps = "5", completed = true)),
            ),
        ),
    )

    @Test
    fun successfulTransactionClearsActiveWorkoutAfterSave() = runBlocking {
        val events = mutableListOf<String>()
        val coordinator = WorkoutCompletionCoordinator(
            saveGraph = { events += "saved" },
            clearActiveWorkout = { events += "cleared" },
        )

        assertTrue(coordinator.finish(workout, 2_000L).isSuccess)
        assertTrue(events == listOf("saved", "cleared"))
    }

    @Test
    fun transactionFailureRetainsActiveWorkout() = runBlocking {
        var cleared = false
        var canceledFinishing = false
        val coordinator = WorkoutCompletionCoordinator(
            saveGraph = { error("disk full") },
            clearActiveWorkout = { cleared = true },
            cancelFinishing = { canceledFinishing = true },
        )

        assertTrue(coordinator.finish(workout, 2_000L).isFailure)
        assertFalse(cleared)
        assertTrue(canceledFinishing)
    }

    @Test
    fun emptyWorkoutDoesNotBeginSaveOrClear() = runBlocking {
        val events = mutableListOf<String>()
        val coordinator = WorkoutCompletionCoordinator(
            saveGraph = { events += "saved" },
            clearActiveWorkout = { events += "cleared" },
            beginFinishing = {
                events += "began"
                true
            },
        )
        val emptyWorkout = workout.copy(
            exercises = workout.exercises.map { exercise ->
                exercise.copy(sets = exercise.sets.map { it.copy(completed = false) })
            },
        )

        assertTrue(coordinator.finish(emptyWorkout, 2_000L).isFailure)
        assertTrue(events.isEmpty())
    }

    @Test
    fun cancellationPropagatesAndLeavesFinishingMarkerForReconciliation() = runBlocking {
        var canceledFinishing = false
        val coordinator = WorkoutCompletionCoordinator(
            saveGraph = { throw CancellationException("activity recreated") },
            clearActiveWorkout = {},
            cancelFinishing = { canceledFinishing = true },
        )

        try {
            coordinator.finish(workout, 2_000L)
            throw AssertionError("CancellationException was not propagated")
        } catch (_: CancellationException) {
            // Expected.
        }
        assertFalse(canceledFinishing)
    }
}