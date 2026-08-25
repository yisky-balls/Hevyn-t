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