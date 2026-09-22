package com.yifeng.commandcenter.history

import com.yifeng.commandcenter.data.ActiveExercise
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.formatWeight

object PreviousSetResolver {
    fun resolve(
        workout: ActiveWorkout,
        history: List<WorkoutHistoryGraph>,
    ): Map<String, PreviousSetPerformance> = buildMap {
        workout.exercises.forEach { activeExercise ->
            val historicalExercise = latestMatchingExercise(
                activeExercise = activeExercise,
                history = history.filter { it.session.completedAtMillis < workout.startedAtMillis },
            ) ?: return@forEach
            activeExercise.sets.forEachIndexed { setPosition, set ->
                historicalExercise.sets
                    .firstOrNull { it.position == setPosition && it.completed }
                    ?.let { previousSet ->
                        put(
                            set.id,
                            PreviousSetPerformance(
                                weightKg = previousSet.weightKg,
                                repetitions = previousSet.repetitions,
                            ),
                        )
                    }
            }
        }
    }

    fun resolveRows(
        workout: ActiveWorkout,
        rows: List<HistoricalExerciseSetRow>,
    ): Map<String, PreviousSetPerformance> = buildMap {
        val latestFirst = rows.sortedByDescending(HistoricalExerciseSetRow::completedAtMillis)
        workout.exercises.forEach { activeExercise ->
            val normalizedName = normalizeExerciseName(activeExercise.name)
            val matchedExerciseId = latestFirst.firstOrNull { row ->
                (activeExercise.sourceRoutineExerciseId != null &&
                    row.sourceRoutineExerciseId == activeExercise.sourceRoutineExerciseId) ||
                    (normalizedName.isNotEmpty() && normalizeExerciseName(row.exerciseName) == normalizedName)
            }?.workoutExerciseId ?: return@forEach
            val matchedRows = latestFirst.filter { it.workoutExerciseId == matchedExerciseId }
            activeExercise.sets.forEachIndexed { setPosition, set ->
                matchedRows.firstOrNull { it.setPosition == setPosition }?.let { previousSet ->
                    put(
                        set.id,
                        PreviousSetPerformance(previousSet.weightKg, previousSet.repetitions),
                    )
                }
            }
        }
    }

    private fun latestMatchingExercise(
        activeExercise: ActiveExercise,
        history: List<WorkoutHistoryGraph>,
    ): WorkoutHistoryExerciseGraph? {
        val latestFirst = history.sortedByDescending { it.session.completedAtMillis }
        val normalizedName = normalizeExerciseName(activeExercise.name)
        latestFirst.forEach { graph ->
            graph.exercises.firstOrNull {
                ((activeExercise.sourceRoutineExerciseId != null &&
                    it.exercise.sourceRoutineExerciseId == activeExercise.sourceRoutineExerciseId) ||
                    (normalizedName.isNotEmpty() &&
                        normalizeExerciseName(it.exercise.name) == normalizedName)) &&
                    it.sets.any(WorkoutHistorySet::completed)
            }?.let { return it }
        }
        return null
    }

    fun resolveExerciseHistory(
        workout: ActiveWorkout,
        history: List<WorkoutHistoryGraph>,
    ): Map<String, ExerciseHistorySummary> = buildMap {
        val completedBefore = history
            .filter { it.session.completedAtMillis < workout.startedAtMillis && !it.session.isLegacySummaryOnly }
            .sortedByDescending { it.session.completedAtMillis }

        workout.exercises.forEach { activeExercise ->
            val normalizedName = normalizeExerciseName(activeExercise.name)
            val sessions = mutableListOf<HistoricalExerciseSession>()
            completedBefore.forEach { graph ->
                val matchingExercise = graph.exercises.firstOrNull { exerciseGraph ->
                    ((activeExercise.sourceRoutineExerciseId != null &&
                        exerciseGraph.exercise.sourceRoutineExerciseId == activeExercise.sourceRoutineExerciseId) ||
                        (normalizedName.isNotEmpty() &&
                            normalizeExerciseName(exerciseGraph.exercise.name) == normalizedName)) &&
                        exerciseGraph.sets.any(WorkoutHistorySet::completed)
                } ?: return@forEach

                val completedSets = matchingExercise.sets
                    .filter(WorkoutHistorySet::completed)
                    .sortedBy { it.position }
                    .map { PreviousSetPerformance(it.weightKg, it.repetitions) }

                if (completedSets.isNotEmpty()) {
                    sessions.add(
                        HistoricalExerciseSession(
                            completedAtMillis = graph.session.completedAtMillis,
                            sets = completedSets,
                        )
                    )
                }
            }

            if (sessions.isNotEmpty()) {
                put(
                    activeExercise.id,
                    ExerciseHistorySummary(
                        lastSession = sessions.firstOrNull(),
                        recentSessions = sessions.take(3),
                    ),
                )
            }
        }
    }

    fun resolveExerciseHistoryRows(
        workout: ActiveWorkout,
        rows: List<HistoricalExerciseSetRow>,
    ): Map<String, ExerciseHistorySummary> = buildMap {
        val latestFirst = rows
            .filter { it.completedAtMillis < workout.startedAtMillis }
            .sortedByDescending(HistoricalExerciseSetRow::completedAtMillis)

        workout.exercises.forEach { activeExercise ->
            val normalizedName = normalizeExerciseName(activeExercise.name)
            val matchedRows = latestFirst.filter { row ->
                (activeExercise.sourceRoutineExerciseId != null &&
                    row.sourceRoutineExerciseId == activeExercise.sourceRoutineExerciseId) ||
                    (normalizedName.isNotEmpty() && normalizeExerciseName(row.exerciseName) == normalizedName)
            }
            if (matchedRows.isEmpty()) return@forEach

            val sessions = matchedRows
                .groupBy { it.workoutExerciseId }
                .values
                .map { exerciseRows ->
                    val completedAt = exerciseRows.first().completedAtMillis
                    val sets = exerciseRows
                        .sortedWith(compareBy({ it.setPosition }, { it.completedAtMillis }))
                        .map { PreviousSetPerformance(it.weightKg, it.repetitions) }
                    HistoricalExerciseSession(
                        completedAtMillis = completedAt,
                        sets = sets,
                    )
                }
                .sortedByDescending { it.completedAtMillis }

            put(
                activeExercise.id,
                ExerciseHistorySummary(
                    lastSession = sessions.firstOrNull(),
                    recentSessions = sessions.take(3),
                ),
            )
        }
    }

    fun evaluateProgression(
        currentWeight: String?,
        currentReps: String?,
        previous: PreviousSetPerformance?,
    ): ProgressionTrend {
        if (previous == null) return ProgressionTrend.NONE
        val prevWeight = previous.weightKg
        val prevReps = previous.repetitions
        if (prevWeight == null && prevReps == null) return ProgressionTrend.NONE

        val currWeight = currentWeight?.toDoubleOrNull()
        val currReps = currentReps?.toIntOrNull()
        if (currWeight == null || currReps == null) return ProgressionTrend.NONE

        val pWeight = prevWeight ?: 0.0
        val pReps = prevReps ?: 0

        return when {
            currWeight > pWeight && currReps >= pReps -> ProgressionTrend.IMPROVED
            currWeight == pWeight && currReps > pReps -> ProgressionTrend.IMPROVED
            currWeight < pWeight && currReps <= pReps -> ProgressionTrend.REGRESSED
            currWeight == pWeight && currReps < pReps -> ProgressionTrend.REGRESSED
            else -> ProgressionTrend.NEUTRAL
        }
    }

    fun format(performance: PreviousSetPerformance?): String {
        if (performance == null) return "—"
        val weight = performance.weightKg?.let(::formatWeight) ?: "—"
        val repetitions = performance.repetitions?.toString() ?: "—"
        return "$weight × $repetitions"
    }

}

internal fun normalizeExerciseName(name: String): String = buildString {
    name.trim().lowercase().forEach { character ->
        if (character.isLetterOrDigit()) append(character)
    }
}