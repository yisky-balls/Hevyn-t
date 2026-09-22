package com.yifeng.commandcenter

import com.yifeng.commandcenter.data.ActiveExercise
import com.yifeng.commandcenter.data.ActiveWorkoutOperations
import com.yifeng.commandcenter.data.Routine
import com.yifeng.commandcenter.data.RoutineExercise
import com.yifeng.commandcenter.data.RoutineWorkoutMapper
import com.yifeng.commandcenter.data.WorkoutSet
import com.yifeng.commandcenter.history.HistoricalExerciseSetRow
import com.yifeng.commandcenter.history.PreviousSetPerformance
import com.yifeng.commandcenter.history.PreviousSetResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineExecutionFlowTest {
    @Test
    fun saved_routine_starts_prepared_with_history_and_accepts_an_extra_exercise() {
        val exerciseIds = listOf("rdl", "extension", "squat", "curl")
        val routine = Routine(
            id = "lower-2",
            name = "LOWER 2",
            exercises = exerciseIds.map { exerciseId ->
                RoutineExercise(
                    id = exerciseId,
                    name = exerciseId,
                    sets = List(3) { position ->
                        WorkoutSet(id = "$exerciseId-template-$position")
                    },
                )
            },
        )
        val previousRows = exerciseIds.flatMapIndexed { exerciseIndex, exerciseId ->
            List(3) { position ->
                HistoricalExerciseSetRow(
                    workoutExerciseId = "previous-$exerciseId",
                    sourceRoutineExerciseId = exerciseId,
                    exerciseName = exerciseId,
                    completedAtMillis = 1_000L,
                    setPosition = position,
                    weightKg = 60.0 + exerciseIndex * 10 + position * 5,
                    repetitions = 10 - position,
                )
            }
        }

        val today = RoutineWorkoutMapper.fromRoutine(
            routine = routine,
            startedAtMillis = 2_000L,
            workoutId = "today",
        )
        val previous = PreviousSetResolver.resolveRows(today, previousRows)

        assertEquals(exerciseIds, today.exercises.map { it.id })
        assertEquals(listOf(3, 3, 3, 3), today.exercises.map { it.sets.size })
        assertTrue(today.exercises.flatMap { it.sets }.all {
            it.weight.isEmpty() && it.reps.isEmpty() && !it.completed
        })
        assertEquals(12, previous.size)
        assertEquals(
            PreviousSetPerformance(85.0, 9),
            previous[today.exercises[2].sets[1].id],
        )

        val firstSet = today.exercises.first().sets.first()
        val copied = ActiveWorkoutOperations.copyPreviousSet(firstSet, previous.getValue(firstSet.id))
        assertEquals("60", copied.weight)
        assertEquals("10", copied.reps)
        assertFalse(copied.completed)

        val withExtra = ActiveWorkoutOperations.addExercise(
            today,
            ActiveExercise(
                id = "extra",
                sourceRoutineExerciseId = "calf-raise",
                name = "Calf raise",
                sets = List(3) { WorkoutSet(id = "extra-$it") },
            ),
        )
        assertEquals(exerciseIds + "extra", withExtra.exercises.map { it.id })
    }
}
