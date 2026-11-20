package com.yifeng.commandcenter.history

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object ExerciseProgressCalculator {

    fun buildOptions(rows: List<HistoricalExerciseSetRow>): List<ExerciseProgressOption> {
        if (rows.isEmpty()) return emptyList()
        val byKey = LinkedHashMap<String, MutableList<HistoricalExerciseSetRow>>()
        for (row in rows) {
            val key = normalizeExerciseName(row.exerciseName)
            if (key.isEmpty()) continue
            byKey.getOrPut(key) { mutableListOf() }.add(row)
        }

        return byKey.mapNotNull { (key, exerciseRows) ->
            val withNumericData = exerciseRows.filter {
                (it.weightKg != null && it.weightKg > 0.0) || (it.repetitions != null && it.repetitions > 0)
            }
            if (withNumericData.isEmpty()) return@mapNotNull null

            val latestRow = withNumericData.maxByOrNull { it.completedAtMillis } ?: return@mapNotNull null
            val distinctSessions = withNumericData.mapTo(HashSet()) { it.workoutExerciseId }.size
            val bestWeight = withNumericData.mapNotNull { it.weightKg?.takeIf { w -> w > 0.0 } }.maxOrNull()

            ExerciseProgressOption(
                normalizedKey = key,
                displayName = latestRow.exerciseName.trim(),
                sessionCount = distinctSessions,
                lastPerformedMillis = latestRow.completedAtMillis,
                bestWeightKg = bestWeight,
            )
        }.sortedWith(
            compareByDescending<ExerciseProgressOption> { it.sessionCount }
                .thenByDescending { it.lastPerformedMillis }
                .thenBy { it.displayName.lowercase() },
        )
    }

    fun summarize(
        rows: List<HistoricalExerciseSetRow>,
        exerciseKey: String,
        metric: ExerciseProgressMetric,
        period: ExerciseProgressPeriod,
        todayEpochDay: Long = LocalDate.now().toEpochDay(),
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): ExerciseProgressSummary? {
        if (exerciseKey.isBlank()) return null
        val matching = rows.filter { normalizeExerciseName(it.exerciseName) == exerciseKey }
        if (matching.isEmpty()) return null

        val displayName = matching.maxByOrNull { it.completedAtMillis }?.exerciseName?.trim().orEmpty()
            .ifBlank { exerciseKey }

        val bySession = LinkedHashMap<String, MutableList<HistoricalExerciseSetRow>>()
        for (row in matching) {
            bySession.getOrPut(row.workoutExerciseId) { mutableListOf() }.add(row)
        }

        val allSessions = bySession.values.mapNotNull { sessionRows ->
            val ordered = sessionRows.sortedBy { it.setPosition }
            val completedAt = ordered.first().completedAtMillis
            val dateEpochDay = Instant.ofEpochMilli(completedAt).atZone(zoneId).toLocalDate().toEpochDay()
            val sets = ordered.map { PreviousSetPerformance(it.weightKg, it.repetitions) }

            val weightedSets = ordered.filter { it.weightKg != null && it.weightKg > 0.0 }
            val topSet = weightedSets.maxWithOrNull(
                compareBy<HistoricalExerciseSetRow>({ it.weightKg ?: 0.0 }, { it.repetitions ?: 0 }),
            )
            val topWeight = topSet?.weightKg
            val topReps = topSet?.repetitions

            val best1Rm = weightedSets.mapNotNull { row ->
                val w = row.weightKg ?: return@mapNotNull null
                val r = row.repetitions
                estimate1RmKg(w, r)
            }.maxOrNull()

            val totalVolume = ordered.sumOf { row ->
                val w = row.weightKg?.takeIf { it > 0.0 } ?: 0.0
                val r = row.repetitions?.takeIf { it > 0 } ?: 0
                w * r
            }.takeIf { it > 0.0 }

            val point = ExerciseSessionPoint(
                workoutExerciseId = ordered.first().workoutExerciseId,
                completedAtMillis = completedAt,
                dateEpochDay = dateEpochDay,
                topWeightKg = topWeight,
                topSetReps = topReps,
                estimated1RmKg = best1Rm,
                sessionVolumeKg = totalVolume,
                sets = sets,
            )
            if (point.valueFor(metric) != null) point else null
        }.sortedBy { it.completedAtMillis }

        val allTimePr = allSessions.mapNotNull { it.valueFor(metric) }.maxOrNull()

        val cutoffDay = period.days?.let { todayEpochDay - (it - 1L) }
        val filtered = if (cutoffDay != null) {
            allSessions.filter { it.dateEpochDay >= cutoffDay }
        } else {
            allSessions
        }

        val latestValue = filtered.lastOrNull()?.valueFor(metric)
        val firstValue = filtered.firstOrNull()?.valueFor(metric)
        val delta = if (filtered.size >= 2 && latestValue != null && firstValue != null) {
            latestValue - firstValue
        } else {
            null
        }
        val periodBest = filtered.mapNotNull { it.valueFor(metric) }.maxOrNull()

        return ExerciseProgressSummary(
            exerciseKey = exerciseKey,
            exerciseName = displayName,
            metric = metric,
            period = period,
            points = filtered,
            latestValue = latestValue,
            deltaValue = delta,
            periodBest = periodBest,
            allTimePr = allTimePr,
            sessionCount = filtered.size,
        )
    }

    internal fun estimate1RmKg(weightKg: Double, reps: Int?): Double {
        if (weightKg <= 0.0) return 0.0
        val effectiveReps = (reps ?: 1).coerceAtLeast(1)
        if (effectiveReps == 1) return weightKg
        return weightKg * (1.0 + effectiveReps / 30.0)
    }
}
