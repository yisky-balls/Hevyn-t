package com.yifeng.commandcenter

import com.yifeng.commandcenter.data.ActiveExercise
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.ActiveWorkoutOperations
import com.yifeng.commandcenter.data.WorkoutSet
import com.yifeng.commandcenter.history.PreviousSetPerformance
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveWorkoutOperationsTest {
    @Test
    fun updateRestSeconds_clampsToSupportedRange() {
        val workout = workout()

        assertEquals(15, ActiveWorkoutOperations.updateRestSeconds(workout, "bench", 1)?.exercises?.first()?.restSeconds)
        assertEquals(900, ActiveWorkoutOperations.updateRestSeconds(workout, "bench", 9999)?.exercises?.first()?.restSeconds)
        assertEquals(120, ActiveWorkoutOperations.updateRestSeconds(workout, "bench", 120)?.exercises?.first()?.restSeconds)
    }

    @Test
    fun `updating one rest timer preserves every other exercise`() {
        val row = ActiveExercise(id = "row", name = "Row", restSeconds = 90, sets = listOf(set("row-one")))
        val workout = workout().copy(exercises = workout().exercises + row)

        val updated = ActiveWorkoutOperations.updateRestSeconds(workout, "bench", 240)!!

        assertEquals(240, updated.exercises[0].restSeconds)
        assertEquals(row, updated.exercises[1])
    }

    @Test
    fun `moving an exercise preserves its complete state`() {
        val bench = workout().exercises.single()
        val row = ActiveExercise(
            id = "row",
            name = "Row",
            notes = "Keep this",
            restSeconds = 90,
            sets = listOf(set("row-set", completed = true)),
        )
        val squat = ActiveExercise(id = "squat", name = "Squat", sets = listOf(set("squat-set")))
        val original = workout().copy(exercises = listOf(bench, row, squat))

        val moved = ActiveWorkoutOperations.moveExercise(original, "row", -1)!!

        assertEquals(listOf("row", "bench", "squat"), moved.exercises.map { it.id })
        assertEquals(row, moved.exercises.first())
        assertNull(ActiveWorkoutOperations.moveExercise(moved, "row", -1))
        assertNull(ActiveWorkoutOperations.moveExercise(moved, "missing", 1))
    }

    @Test
    fun removeSet_removesExactSetAndPreservesOthers() {
        val workout = workout().copy(
            exercises = listOf(
                workout().exercises.first().copy(sets = listOf(set("one"), set("two"), set("three")))
            )
        )
        val exercise = workout.exercises.first()
        val removedId = exercise.sets[1].id

        val updated = ActiveWorkoutOperations.removeSet(workout, exercise.id, removedId)!!

        assertEquals(2, updated.exercises.first().sets.size)
        assertEquals(listOf(exercise.sets[0].id, exercise.sets[2].id), updated.exercises.first().sets.map { it.id })
    }

    @Test
    fun `removing a set preserves neighboring exercises exactly`() {
        val bench = ActiveExercise(id = "bench", name = "Bench", sets = listOf(set("one"), set("two")))
        val row = ActiveExercise(id = "row", name = "Edited row", notes = "keep", restSeconds = 75, sets = listOf(set("row-one")))
        val workout = ActiveWorkout(routineName = "Test", exercises = listOf(bench, row))

        val updated = ActiveWorkoutOperations.removeSet(workout, "bench", "one")!!

        assertEquals(listOf("two"), updated.exercises[0].sets.map { it.id })
        assertEquals(row, updated.exercises[1])
    }

    @Test
    fun removeSet_allowsRemovingLastSet() {
        val workout = workout().copy(
            exercises = listOf(workout().exercises.first().copy(sets = listOf(WorkoutSet(id = "only"))))
        )
        val exercise = workout.exercises.first()

        val updated = ActiveWorkoutOperations.removeSet(workout, exercise.id, "only")!!

        assertTrue(updated.exercises.first().sets.isEmpty())
    }

    @Test
    fun `completes the selected set and returns the following target`() {
        val workout = workout()

        val result = ActiveWorkoutOperations.completeSet(workout, "bench", "one")!!

        assertTrue(result.workout.exercises[0].sets[0].completed)
        assertFalse(result.workout.exercises[0].sets[1].completed)
        assertEquals("two", result.nextTarget?.setId)
    }

    @Test
    fun `duplicate completion is ignored`() {
        val workout = workout(firstCompleted = true)

        assertNull(ActiveWorkoutOperations.completeSet(workout, "bench", "one"))
    }

    @Test
    fun `final set has no following target`() {
        val workout = ActiveWorkout(
            routineName = "Test",
            exercises = listOf(
                ActiveExercise(id = "bench", name = "Bench", sets = listOf(set("one"))),
            ),
        )

        assertNull(ActiveWorkoutOperations.completeSet(workout, "bench", "one")?.nextTarget)
    }

    @Test
    fun `copying previous performance fills both values without completing set`() {
        val current = WorkoutSet(id = "today", weight = "", reps = "", completed = false)

        val copied = ActiveWorkoutOperations.copyPreviousSet(
            current,
            PreviousSetPerformance(weightKg = 32.5, repetitions = 11),
        )

        assertEquals("32.5", copied.weight)
        assertEquals("11", copied.reps)
        assertFalse(copied.completed)
    }

    @Test
    fun `copying partial previous performance preserves entered current value`() {
        val current = WorkoutSet(weight = "40", reps = "8")

        val copied = ActiveWorkoutOperations.copyPreviousSet(
            current,
            PreviousSetPerformance(weightKg = null, repetitions = 10),
        )

        assertEquals("40", copied.weight)
        assertEquals("10", copied.reps)
    }

    private fun workout(firstCompleted: Boolean = false) = ActiveWorkout(
        routineName = "Test",
        exercises = listOf(
            ActiveExercise(
                id = "bench",
                name = "Bench",
                sets = listOf(set("one", firstCompleted), set("two")),
            ),
        ),
    )

    private fun set(id: String, completed: Boolean = false) = WorkoutSet(
        id = id,
        weight = "80",
        reps = "6",
        completed = completed,
    )
}