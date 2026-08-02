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
                    .firstOrNull { it.position == setPosition }
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

    private fun latestMatchingExercise(
        activeExercise: ActiveExercise,
        history: List<WorkoutHistoryGraph>,
    ): WorkoutHistoryExerciseGraph? {
        val latestFirst = history.sortedByDescending { it.session.completedAtMillis }
        activeExercise.sourceRoutineExerciseId?.let { sourceId ->
            latestFirst.forEach { graph ->
                graph.exercises.firstOrNull {
                    it.exercise.sourceRoutineExerciseId == sourceId
                }?.let { return it }
            }
        }
        val normalizedName = normalizeExerciseName(activeExercise.name)
        if (normalizedName.isEmpty()) return null
        latestFirst.forEach { graph ->
            graph.exercises.firstOrNull {
                normalizeExerciseName(it.exercise.name) == normalizedName
            }?.let { return it }
        }
        return null
    }

    fun format(performance: PreviousSetPerformance?): String {
        if (performance == null) return "—"
        val weight = performance.weightKg?.let(::formatWeight) ?: "—"
        val repetitions = performance.repetitions?.toString() ?: "—"
        return "$weight kg × $repetitions"
    }

    internal fun normalizeExerciseName(name: String): String =
        name.trim().lowercase().replace(Regex("\\s+"), " ")
}