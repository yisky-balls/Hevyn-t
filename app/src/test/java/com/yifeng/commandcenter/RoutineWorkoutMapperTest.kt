package com.yifeng.commandcenter

import com.yifeng.commandcenter.data.Routine
import com.yifeng.commandcenter.data.RoutineExercise
import com.yifeng.commandcenter.data.RoutineWorkoutMapper
import com.yifeng.commandcenter.data.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
        assertEquals(listOf("", ""), workout.exercises[0].sets.map { it.weight })
        assertEquals(listOf("", ""), workout.exercises[0].sets.map { it.reps })
        assertEquals(120, workout.exercises[0].restSeconds)
        assertFalse(workout.exercises.flatMap { it.sets }.any { it.completed })
    }

    @Test
    fun `started workout immediately contains all planned exercises and rows`() {
        val routine = Routine(
            id = "lower-2",
            name = "LOWER 2",
            exercises = listOf("rdl", "extension", "squat", "curl").map { id ->
                RoutineExercise(
                    id = id,
                    name = id,
                    sets = List(3) { index ->
                        WorkoutSet("$id-$index", "80", "8", completed = true)
                    },
                )
            },
        )

        val workout = RoutineWorkoutMapper.fromRoutine(routine, 123L, "workout")

        assertEquals(listOf("rdl", "extension", "squat", "curl"), workout.exercises.map { it.id })
        assertEquals(listOf(3, 3, 3, 3), workout.exercises.map { it.sets.size })
        assertTrue(workout.exercises.flatMap { it.sets }.all { set ->
            set.weight.isEmpty() && set.reps.isEmpty() && !set.completed
        })
    }

    @Test
    fun `routine editor preserves template performance fields without completion`() {
        val routine = Routine(
            id = "routine",
            name = "Routine",
            exercises = listOf(
                RoutineExercise(
                    id = "rdl",
                    name = "RDL",
                    sets = listOf(WorkoutSet("set", "80", "8", completed = true)),
                ),
            ),
        )

        val draft = RoutineWorkoutMapper.forEditing(routine, 123L, "draft")
        val set = draft.exercises.single().sets.single()

        assertEquals("80", set.weight)
        assertEquals("8", set.reps)
        assertFalse(set.completed)
    }
}