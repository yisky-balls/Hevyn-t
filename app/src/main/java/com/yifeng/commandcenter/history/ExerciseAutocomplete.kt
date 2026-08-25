package com.yifeng.commandcenter.history

import com.yifeng.commandcenter.data.ActiveExercise
import com.yifeng.commandcenter.data.RoutineExercise
import com.yifeng.commandcenter.data.RoutineFolder
import com.yifeng.commandcenter.data.WorkoutSet
import java.nio.charset.StandardCharsets
import java.util.UUID

/** A canonical local exercise choice assembled from routines and completed history. */
data class ExerciseSuggestion(
    val sourceExerciseId: String,
    val name: String,
    val notes: String,
    val restSeconds: Int,
    val setCount: Int,
    val lastPerformance: PreviousSetPerformance?,
    val lastUsedAtMillis: Long?,
    val useCount: Int,
)

object ExerciseAutocomplete {
    fun catalog(
        folders: List<RoutineFolder>,
        history: List<WorkoutHistoryGraph>,
    ): List<ExerciseSuggestion> {
        val aggregates = linkedMapOf<String, MutableExerciseAggregate>()

        folders.forEach { folder ->
            folder.routines.forEach { routine ->
                routine.exercises.forEach { exercise ->
                    val key = normalizeExerciseName(exercise.name)
                    if (key.isNotEmpty()) {
                        aggregates.getOrPut(key) { MutableExerciseAggregate(key) }
                            .addRoutine(exercise)
                    }
                }
            }
        }

        history.sortedBy { it.session.completedAtMillis }.forEach { graph ->
            graph.exercises.forEach { exercise ->
                val key = normalizeExerciseName(exercise.exercise.name)
                if (key.isNotEmpty()) {
                    aggregates.getOrPut(key) { MutableExerciseAggregate(key) }
                        .addHistory(graph.session.completedAtMillis, exercise)
                }
            }
        }

        return aggregates.values
            .map(MutableExerciseAggregate::toSuggestion)
            .sortedWith(baseRanking)
    }

    fun search(
        catalog: List<ExerciseSuggestion>,
        query: String,
        limit: Int = 6,
    ): List<ExerciseSuggestion> {
        val normalizedQuery = normalizeExerciseName(query)
        if (normalizedQuery.isEmpty()) return catalog.take(limit)
        return catalog.mapNotNull { suggestion ->
            matchRank(suggestion.name, normalizedQuery)?.let { rank -> rank to suggestion }
        }.sortedWith(
            compareBy<Pair<Int, ExerciseSuggestion>> { it.first }
                .thenByDescending { it.second.lastUsedAtMillis ?: Long.MIN_VALUE }
                .thenByDescending { it.second.useCount }
                .thenBy { it.second.name.lowercase() },
        ).take(limit).map { it.second }
    }

    fun custom(query: String): ExerciseSuggestion {
        val name = query.trim().replace(Regex("\\s+"), " ")
        val key = normalizeExerciseName(name)
        require(key.isNotEmpty()) { "Exercise name cannot be blank." }
        return ExerciseSuggestion(
            sourceExerciseId = stableIdentity(key),
            name = name,
            notes = "",
            restSeconds = 180,
            setCount = 3,
            lastPerformance = null,
            lastUsedAtMillis = null,
            useCount = 0,
        )
    }

    fun toActiveExercise(
        suggestion: ExerciseSuggestion,
        nextId: () -> String = { UUID.randomUUID().toString() },
    ): ActiveExercise = ActiveExercise(
        id = nextId(),
        sourceRoutineExerciseId = suggestion.sourceExerciseId,
        name = suggestion.name,
        notes = suggestion.notes,
        restSeconds = suggestion.restSeconds,
        sets = List(suggestion.setCount.coerceAtLeast(1)) {
            WorkoutSet(id = nextId())
        },
    )

    fun hasExactName(catalog: List<ExerciseSuggestion>, query: String): Boolean {
        val normalized = normalizeExerciseName(query)
        return normalized.isNotEmpty() && catalog.any {
            normalizeExerciseName(it.name) == normalized
        }
    }

    private fun matchRank(name: String, normalizedQuery: String): Int? {
        val normalizedName = normalizeExerciseName(name)
        val words = name.lowercase().split(Regex("[^\\p{L}\\p{N}]+"))
        return when {
            normalizedName == normalizedQuery -> 0
            normalizedName.startsWith(normalizedQuery) -> 1
            words.any { it.startsWith(normalizedQuery) } -> 2
            normalizedQuery in normalizedName -> 3
            else -> null
        }
    }

    private val baseRanking = compareByDescending<ExerciseSuggestion> {
        it.lastUsedAtMillis ?: Long.MIN_VALUE
    }.thenByDescending { it.useCount }
        .thenBy { it.name.lowercase() }

    private fun stableIdentity(normalizedName: String): String = UUID.nameUUIDFromBytes(
        "hevynt-exercise:$normalizedName".toByteArray(StandardCharsets.UTF_8),
    ).toString()

    private class MutableExerciseAggregate(private val normalizedName: String) {
        private var routine: RoutineExercise? = null
        private var latestHistory: WorkoutHistoryExerciseGraph? = null
        private var latestUsedAt: Long? = null
        private var uses: Int = 0

        fun addRoutine(exercise: RoutineExercise) {
            uses += 1
            if (routine == null) routine = exercise
        }

        fun addHistory(completedAtMillis: Long, exercise: WorkoutHistoryExerciseGraph) {
            uses += 1
            if (latestUsedAt == null || completedAtMillis >= latestUsedAt!!) {
                latestUsedAt = completedAtMillis
                latestHistory = exercise
            }
        }

        fun toSuggestion(): ExerciseSuggestion {
            val historical = latestHistory
            val routineExercise = routine
            val completedSets = historical?.sets.orEmpty()
                .filter { it.completed }
                .sortedBy { it.position }
            val lastPerformance = completedSets
                .firstOrNull { it.weightKg != null || it.repetitions != null }
                ?.let { PreviousSetPerformance(it.weightKg, it.repetitions) }
            return ExerciseSuggestion(
                sourceExerciseId = routineExercise?.id
                    ?: historical?.exercise?.sourceRoutineExerciseId
                    ?: stableIdentity(normalizedName),
                name = routineExercise?.name ?: historical?.exercise?.name.orEmpty(),
                notes = historical?.exercise?.note ?: routineExercise?.notes.orEmpty(),
                restSeconds = historical?.exercise?.restSeconds
                    ?: routineExercise?.restSeconds
                    ?: 180,
                setCount = historical?.sets?.size?.takeIf { it > 0 }
                    ?: routineExercise?.sets?.size?.takeIf { it > 0 }
                    ?: 3,
                lastPerformance = lastPerformance,
                lastUsedAtMillis = latestUsedAt,
                useCount = uses,
            )
        }
    }
}