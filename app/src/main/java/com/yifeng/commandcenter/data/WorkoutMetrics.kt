package com.yifeng.commandcenter.data

import java.math.BigDecimal
import java.math.RoundingMode

data class WorkoutMetrics(
    val completedSets: Int,
    val volumeKg: Double,
)

object WorkoutMetricsCalculator {
    fun calculate(workout: ActiveWorkout): WorkoutMetrics {
        var completedSets = 0
        var volume = BigDecimal.ZERO

        workout.exercises.forEach { exercise ->
            exercise.sets.forEach { set ->
                if (set.completed) {
                    completedSets++
                    val weight = set.weight.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    val reps = set.reps.toBigDecimalOrNull() ?: BigDecimal.ZERO
                    volume = volume.add(weight.multiply(reps))
                }
            }
        }

        return WorkoutMetrics(
            completedSets = completedSets,
            volumeKg = volume.toDouble(),
        )
    }
}

fun formatWeight(value: Double, maxDecimals: Int = 2): String {
    if (!value.isFinite()) return "0"
    val rounded = BigDecimal.valueOf(value)
        .setScale(maxDecimals.coerceAtLeast(0), RoundingMode.HALF_UP)
        .stripTrailingZeros()
    return if (rounded.compareTo(BigDecimal.ZERO) == 0) "0" else rounded.toPlainString()
}