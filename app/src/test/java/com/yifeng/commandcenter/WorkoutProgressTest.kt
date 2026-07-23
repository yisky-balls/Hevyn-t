package com.yifeng.commandcenter

import com.yifeng.commandcenter.data.ActiveExercise
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.WorkoutProgress
import com.yifeng.commandcenter.data.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WorkoutProgressTest {
    @Test
    fun `next target stays in the current exercise`() {
        val workout = workout(
            ActiveExercise(
                id = "bench",
                name = "Bench Press",
                sets = listOf(set("one"), set("two", weight = "90", reps = "4")),
            )
        )

        val target = WorkoutProgress.nextSetAfter(workout, "bench", "one")

        assertEquals("two", target?.setId)
        assertEquals(2, target?.setNumber)
    }

    @Test
    fun `next target advances to the next exercise`() {
        val workout = workout(
            ActiveExercise(id = "bench", name = "Bench", sets = listOf(set("one"))),
            ActiveExercise(id = "row", name = "Row", sets = listOf(set("two"))),
        )

        val target = WorkoutProgress.nextSetAfter(workout, "bench", "one")

        assertEquals("row", target?.exerciseId)
        assertEquals(0, target?.setIndex)
    }

    @Test
    fun `there is no target when every remaining set is complete`() {
        val workout = workout(
            ActiveExercise(id = "bench", name = "Bench", sets = listOf(set("one"))),
        )

        assertNull(WorkoutProgress.nextSetAfter(workout, "bench", "one"))
    }

    @Test
    fun `progression wraps to an earlier unfinished set before changing exercise`() {
        val workout = workout(
            ActiveExercise(
                id = "bench",
                name = "Bench",
                sets = listOf(set("one"), set("two"), set("three")),
            ),
        )

        val target = WorkoutProgress.nextSetAfter(
            workout.copy(exercises = listOf(workout.exercises[0].copy(
                sets = workout.exercises[0].sets.mapIndexed { index, set -> set.copy(completed = index == 1) }
            ))),
            "bench",
            "two",
        )

        assertEquals("three", target?.setId)
    }

    @Test
    fun `progression never changes exercise while an earlier current set is unfinished`() {
        val workout = workout(
            ActiveExercise(
                id = "bench",
                name = "Bench",
                sets = listOf(set("one"), set("two", completed = true), set("three", completed = true)),
            ),
            ActiveExercise(id = "row", name = "Row", sets = listOf(set("row-one"))),
        )

        val target = WorkoutProgress.nextSetAfter(workout, "bench", "three")

        assertEquals("one", target?.setId)
        assertEquals("bench", target?.exerciseId)
    }

    private fun workout(vararg exercises: ActiveExercise) = ActiveWorkout(
        routineName = "Test",
        exercises = exercises.toList(),
    )

    private fun set(
        id: String,
        weight: String = "80",
        reps: String = "6",
        completed: Boolean = false,
    ) = WorkoutSet(id = id, weight = weight, reps = reps, completed = completed)
}