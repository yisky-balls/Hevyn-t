package com.yifeng.commandcenter

import com.yifeng.commandcenter.data.Routine
import com.yifeng.commandcenter.data.RoutineExercise
import com.yifeng.commandcenter.data.RoutineWorkoutMapper
import com.yifeng.commandcenter.data.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RoutineWorkoutMapperTest {
    @Test
    fun `started workout preserves routine exercise order and intended set structure`() {
        val routine = Routine(
            id = "routine",
            name = "Long routine name",
            exercises = listOf(
                RoutineExercise(
                    id = "press",
                    name = "Incline DB Press",
                    notes = "Controlled",
                    restSeconds = 120,
                    sets = listOf(
                        WorkoutSet("press-1", "30", "10", completed = true),
                        WorkoutSet("press-2", "28", "12"),
                    ),
                ),
                RoutineExercise(
                    id = "row",
                    name = "Row",
                    sets = listOf(WorkoutSet("row-1")),
                ),
            ),
        )

        val workout = RoutineWorkoutMapper.fromRoutine(routine, 123L, "workout")

        assertEquals("workout", workout.id)
        assertEquals("routine", workout.sourceRoutineId)
        assertEquals(listOf("press", "row"), workout.exercises.map { it.id })
        assertEquals(listOf("press-1", "press-2"), workout.exercises[0].sets.map { it.id })
        assertEquals(listOf("30", "28"), workout.exercises[0].sets.map { it.weight })
        assertEquals(120, workout.exercises[0].restSeconds)
        assertFalse(workout.exercises.flatMap { it.sets }.any { it.completed })
    }
}