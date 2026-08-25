package com.yifeng.commandcenter.data

import com.yifeng.commandcenter.history.WorkoutHistorySession

enum class RoutineRecommendationReason {
    ObservedTransition,
    LeastRecentlyPerformed,
}

data class RoutineRecommendation(
    val routine: RoutineSummary,
    val reason: RoutineRecommendationReason,
)

/** Deterministic, fully local routine-rotation inference from completed session metadata. */
object LikelyNextRoutinePredictor {
    fun predict(
        folders: List<RoutineFolderSummary>,
        history: List<WorkoutHistorySession>,
    ): RoutineRecommendation? {
        val routines = folders.flatMap(RoutineFolderSummary::routines)
            .filter { it.exerciseCount > 0 }
        if (routines.isEmpty()) return null
        val routinesById = routines.associateBy(RoutineSummary::id)
        val chronological = history
            .asSequence()
            .filterNot(WorkoutHistorySession::isLegacySummaryOnly)
            .sortedWith(compareBy(WorkoutHistorySession::completedAtMillis, WorkoutHistorySession::id))
            .toList()
        val mostRecentRoutineId = chronological.lastOrNull()
            ?.sourceRoutineId
            ?.takeIf(routinesById::containsKey)

        if (mostRecentRoutineId != null) {
            val transitions = linkedMapOf<String, Int>()
            chronological.zipWithNext().forEach { (from, to) ->
                if (from.sourceRoutineId == mostRecentRoutineId &&
                    to.sourceRoutineId != null &&
                    to.sourceRoutineId in routinesById
                ) {
                    transitions[to.sourceRoutineId] =
                        transitions.getOrDefault(to.sourceRoutineId, 0) + 1
                }
            }
            val strongestCount = transitions.values.maxOrNull()
            if (strongestCount != null) {
                val strongest = transitions.filterValues { it == strongestCount }.keys
                if (strongest.size == 1) {
                    return RoutineRecommendation(
                        routine = checkNotNull(routinesById[strongest.single()]),
                        reason = RoutineRecommendationReason.ObservedTransition,
                    )
                }
            }
        }

        val lastPerformed = chronological
            .mapNotNull { session ->
                session.sourceRoutineId
                    ?.takeIf(routinesById::containsKey)
                    ?.let { it to session.completedAtMillis }
            }
            .groupingBy { it.first }
            .fold(Long.MIN_VALUE) { latest, (_, completedAt) -> maxOf(latest, completedAt) }
        val fallbackPool = routines.filterNot {
            routines.size > 1 && it.id == mostRecentRoutineId
        }
        val neverPerformed = fallbackPool.filter { it.id !in lastPerformed }
        val fallback = when {
            neverPerformed.size == 1 -> neverPerformed.single()
            neverPerformed.isNotEmpty() -> null
            else -> {
                val oldest = fallbackPool.minOfOrNull { lastPerformed[it.id] ?: Long.MIN_VALUE }
                fallbackPool.singleOrNull { lastPerformed[it.id] == oldest }
            }
        } ?: return null
        return RoutineRecommendation(fallback, RoutineRecommendationReason.LeastRecentlyPerformed)
    }

    fun attachLastPerformed(
        folders: List<RoutineFolderSummary>,
        history: List<WorkoutHistorySession>,
    ): List<RoutineFolderSummary> {
        val lastPerformed = history
            .asSequence()
            .filterNot(WorkoutHistorySession::isLegacySummaryOnly)
            .mapNotNull { session -> session.sourceRoutineId?.let { it to session.completedAtMillis } }
            .groupingBy { it.first }
            .fold(Long.MIN_VALUE) { latest, (_, completedAt) -> maxOf(latest, completedAt) }
        return folders.map { folder ->
            folder.copy(routines = folder.routines.map { routine ->
                routine.copy(lastPerformedAtMillis = lastPerformed[routine.id])
            })
        }
    }
}