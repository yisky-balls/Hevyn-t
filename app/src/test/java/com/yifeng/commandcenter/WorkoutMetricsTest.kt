package com.yifeng.commandcenter

import com.yifeng.commandcenter.data.ActiveExercise
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.WorkoutMetricsCalculator
import com.yifeng.commandcenter.data.WorkoutSet
import com.yifeng.commandcenter.data.formatWeight
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutMetricsTest {
    @Test
    fun `calculates decimal volume from completed sets only`() {
        val workout = workout(
            WorkoutSet(weight = "82.5", reps = "4", completed = true),
            WorkoutSet(weight = "100", reps = "3", completed = false),
            WorkoutSet(weight = "20.25", reps = "2", completed = true),
        )

        val metrics = WorkoutMetricsCalculator.calculate(workout)

        assertEquals(2, metrics.completedSets)
        assertEquals(370.5, metrics.volumeKg, 0.0)
    }

    @Test
    fun `malformed completed values count as a set but add no invalid volume`() {
        val workout = workout(
            WorkoutSet(weight = "not-a-weight", reps = "8", completed = true),
            WorkoutSet(weight = "50", reps = "", completed = true),
        )

        val metrics = WorkoutMetricsCalculator.calculate(workout)

        assertEquals(2, metrics.completedSets)
        assertEquals(0.0, metrics.volumeKg, 0.0)
    }

    @Test
    fun `formats weights without redundant decimal zeros`() {
        assertEquals("120", formatWeight(120.0))
        assertEquals("82.5", formatWeight(82.5))
        assertEquals("1.25", formatWeight(1.25))
        assertEquals("0", formatWeight(0.0))
    }

    @Test
    fun `rounds repeating decimals and floating point noise cleanly`() {
        assertEquals("105.33", formatWeight(105.33333333333333))
        assertEquals("105.3", formatWeight(105.33333333333333, maxDecimals = 1))
        assertEquals("28.3", formatWeight(28.33333333333333, maxDecimals = 1))
        assertEquals("82.5", formatWeight(82.50000000000001))
        assertEquals("91.3", formatWeight(91.25, maxDecimals = 1))
        assertEquals("0", formatWeight(-0.001, maxDecimals = 1))
        assertEquals("0", formatWeight(Double.NaN))
    }

    private fun workout(vararg sets: WorkoutSet) = ActiveWorkout(
        routineName = "Metrics",
        exercises = listOf(ActiveExercise(name = "Lift", sets = sets.toList())),
    )
}