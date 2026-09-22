package com.yifeng.commandcenter

import com.yifeng.commandcenter.data.ActiveExercise
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.WorkoutSet
import com.yifeng.commandcenter.history.PreviousSetPerformance
import com.yifeng.commandcenter.history.PreviousSetResolver
import com.yifeng.commandcenter.history.HistoricalExerciseSetRow
import com.yifeng.commandcenter.history.WorkoutHistoryExercise
import com.yifeng.commandcenter.history.WorkoutHistoryExerciseGraph
import com.yifeng.commandcenter.history.WorkoutHistoryGraph
import com.yifeng.commandcenter.history.WorkoutHistorySession
import com.yifeng.commandcenter.history.WorkoutHistorySet
import com.yifeng.commandcenter.history.ProgressionTrend
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PreviousSetResolverTest {
    @Test
    fun prefersSourceIdFromLatestCompletedSessionAndMatchesSetPosition() {
        val current = workout(
            startedAtMillis = 1_000L,
            sourceId = "routine-exercise",
            name = "Renamed press",
        )
        val resolved = PreviousSetResolver.resolve(
            current,
            listOf(
                graph(500L, "routine-exercise", "Old press", setPosition = 0, weight = 42.5, reps = 8),
                graph(900L, "routine-exercise", "Old press", setPosition = 1, weight = 45.25, reps = 6),
            ),
        )

        assertFalse(current.exercises.first().sets.first().id in resolved)
        assertEquals(
            PreviousSetPerformance(45.25, 6),
            resolved[current.exercises.first().sets[1].id],
        )
    }

    @Test
    fun fallsBackToNormalizedNameAndExcludesSessionsAtOrAfterWorkoutStart() {
        val current = workout(startedAtMillis = 1_000L, sourceId = null, name = "  Bench   Press ")
        val resolved = PreviousSetResolver.resolve(
            current,
            listOf(
                graph(900L, null, "bench press", setPosition = 0, weight = 80.0, reps = 5),
                graph(1_000L, null, "Bench Press", setPosition = 0, weight = 100.0, reps = 1),
            ),
        )

        assertEquals(
            PreviousSetPerformance(80.0, 5),
            resolved[current.exercises.first().sets.first().id],
        )
    }

    @Test
    fun latestActualNameMatchAcrossAnotherRoutineBeatsOlderSourceIdMatch() {
        val current = workout(1_000L, "routine-exercise", "Bench press")

        val resolved = PreviousSetResolver.resolve(
            current,
            listOf(
                graph(900L, null, "Bench press", 0, 100.0, 1),
                graph(800L, "routine-exercise", "Old press name", 0, 80.0, 5),
            ),
        )

        assertEquals(
            PreviousSetPerformance(100.0, 1),
            resolved[current.exercises.single().sets.first().id],
        )
    }

    @Test
    fun compactRowsResolveAllSetsFromLatestActualExercisePerformance() {
        val current = workout(1_000L, "routine-a-press", "Incline DB Press")
        val resolved = PreviousSetResolver.resolveRows(
            current,
            listOf(
                HistoricalExerciseSetRow("older", "routine-a-press", "Old name", 700L, 0, 25.0, 12),
                HistoricalExerciseSetRow("latest", "routine-b-press", "incline db press", 900L, 0, 30.0, 10),
                HistoricalExerciseSetRow("latest", "routine-b-press", "incline db press", 900L, 1, 30.0, 9),
            ),
        )

        assertEquals(PreviousSetPerformance(30.0, 10), resolved["active-set-0"])
        assertEquals(PreviousSetPerformance(30.0, 9), resolved["active-set-1"])
    }

    @Test
    fun unmatchedSourceIdStillFallsBackToNormalizedName() {
        val current = workout(1_000L, "new-source-id", "Bench press")

        val resolved = PreviousSetResolver.resolve(
            current,
            listOf(graph(900L, "old-source-id", "  bench   press ", 0, 75.0, 8)),
        )

        assertEquals(
            PreviousSetPerformance(75.0, 8),
            resolved[current.exercises.single().sets.first().id],
        )
    }

    @Test
    fun formatsNullableValuesAndMissingPerformanceAsDashes() {
        assertEquals("— × 10", PreviousSetResolver.format(PreviousSetPerformance(null, 10)))
        assertEquals("62.5 × —", PreviousSetResolver.format(PreviousSetPerformance(62.5, null)))
        assertEquals("—", PreviousSetResolver.format(null))
    }

    @Test
    fun uncheckedSnapshotRowsAreNotSuggestedAsPreviousPerformance() {
        val current = workout(1_000L, null, "Lat pull down")
        val history = graph(900L, null, "LAT PULLDOWN", 0, 70.0, 8).let { graph ->
            graph.copy(exercises = graph.exercises.map { exercise ->
                exercise.copy(sets = exercise.sets.map { it.copy(completed = false) })
            })
        }

        val resolved = PreviousSetResolver.resolve(current, listOf(history))

        assertFalse(current.exercises.single().sets.first().id in resolved)
    }

    @Test
    fun resolveExerciseHistory_resolvesFullLastSessionAndRecentSessions() {
        val current = workout(1_000L, "press-id", "Bench Press")
        val graph900 = multiSetGraph(
            completedAtMillis = 900L,
            sourceId = "press-id",
            name = "Bench Press",
            sets = listOf(80.0 to 8, 82.5 to 6, 80.0 to 7),
        )
        val graph800 = multiSetGraph(
            completedAtMillis = 800L,
            sourceId = "press-id",
            name = "Bench Press",
            sets = listOf(77.5 to 8, 80.0 to 6),
        )
        val graph700 = multiSetGraph(
            completedAtMillis = 700L,
            sourceId = "press-id",
            name = "Bench Press",
            sets = listOf(75.0 to 10),
        )
        val graph600 = multiSetGraph(
            completedAtMillis = 600L,
            sourceId = "press-id",
            name = "Bench Press",
            sets = listOf(72.5 to 10),
        )

        val history = PreviousSetResolver.resolveExerciseHistory(
            current,
            listOf(graph900, graph800, graph700, graph600),
        )

        val summary = history[current.exercises.single().id]
        assertNotNull(summary)
        val lastSession = summary!!.lastSession
        assertNotNull(lastSession)
        assertEquals(900L, lastSession!!.completedAtMillis)
        assertEquals(
            listOf(
                PreviousSetPerformance(80.0, 8),
                PreviousSetPerformance(82.5, 6),
                PreviousSetPerformance(80.0, 7),
            ),
            lastSession.sets,
        )
        // Capped at 3 recent sessions
        assertEquals(3, summary.recentSessions.size)
        assertEquals(listOf(900L, 800L, 700L), summary.recentSessions.map { it.completedAtMillis })
    }

    @Test
    fun resolveExerciseHistory_excludesSessionsAtOrAfterWorkoutStart() {
        val current = workout(1_000L, "press-id", "Bench Press")
        val futureGraph = multiSetGraph(
            completedAtMillis = 1_000L,
            sourceId = "press-id",
            name = "Bench Press",
            sets = listOf(100.0 to 1),
        )
        val pastGraph = multiSetGraph(
            completedAtMillis = 900L,
            sourceId = "press-id",
            name = "Bench Press",
            sets = listOf(80.0 to 8),
        )

        val history = PreviousSetResolver.resolveExerciseHistory(current, listOf(futureGraph, pastGraph))
        val summary = history[current.exercises.single().id]
        assertNotNull(summary)
        assertEquals(900L, summary!!.lastSession?.completedAtMillis)
        assertEquals(1, summary.recentSessions.size)
    }

    @Test
    fun evaluateProgression_evaluatesTrendsCorrectly() {
        val prev = PreviousSetPerformance(80.0, 8)

        // Improved weight with same or more reps
        assertEquals(ProgressionTrend.IMPROVED, PreviousSetResolver.evaluateProgression("82.5", "8", prev))
        assertEquals(ProgressionTrend.IMPROVED, PreviousSetResolver.evaluateProgression("82.5", "9", prev))

        // Same weight with more reps
        assertEquals(ProgressionTrend.IMPROVED, PreviousSetResolver.evaluateProgression("80.0", "9", prev))

        // Identical
        assertEquals(ProgressionTrend.NEUTRAL, PreviousSetResolver.evaluateProgression("80.0", "8", prev))

        // Trade-off: higher weight, fewer reps
        assertEquals(ProgressionTrend.NEUTRAL, PreviousSetResolver.evaluateProgression("82.5", "6", prev))

        // Regressed
        assertEquals(ProgressionTrend.REGRESSED, PreviousSetResolver.evaluateProgression("77.5", "8", prev))
        assertEquals(ProgressionTrend.REGRESSED, PreviousSetResolver.evaluateProgression("80.0", "7", prev))
        assertEquals(ProgressionTrend.REGRESSED, PreviousSetResolver.evaluateProgression("75.0", "6", prev))

        // Missing or incomplete
        assertEquals(ProgressionTrend.NONE, PreviousSetResolver.evaluateProgression(null, "8", prev))
        assertEquals(ProgressionTrend.NONE, PreviousSetResolver.evaluateProgression("80.0", null, prev))
        assertEquals(ProgressionTrend.NONE, PreviousSetResolver.evaluateProgression("", "8", prev))
        assertEquals(ProgressionTrend.NONE, PreviousSetResolver.evaluateProgression("80.0", "", prev))
        assertEquals(ProgressionTrend.NONE, PreviousSetResolver.evaluateProgression("80.0", "8", null))
        assertEquals(
            ProgressionTrend.NONE,
            PreviousSetResolver.evaluateProgression("80.0", "8", PreviousSetPerformance(null, null)),
        )
    }

    private fun workout(startedAtMillis: Long, sourceId: String?, name: String) = ActiveWorkout(
        id = "current",
        routineName = "Routine",
        startedAtMillis = startedAtMillis,
        exercises = listOf(
            ActiveExercise(
                id = "active-exercise",
                sourceRoutineExerciseId = sourceId,
                name = name,
                sets = listOf(WorkoutSet(id = "active-set-0"), WorkoutSet(id = "active-set-1")),
            ),
        ),
    )

    private fun multiSetGraph(
        completedAtMillis: Long,
        sourceId: String?,
        name: String,
        sets: List<Pair<Double?, Int?>>,
    ): WorkoutHistoryGraph {
        val sessionId = "session-$completedAtMillis"
        val exerciseId = "exercise-$completedAtMillis"
        return WorkoutHistoryGraph(
            session = WorkoutHistorySession(
                id = sessionId,
                sourceRoutineId = null,
                routineName = "Routine",
                startedAtMillis = completedAtMillis - 100L,
                completedAtMillis = completedAtMillis,
                durationMillis = 100L,
                note = "",
                completedSetCount = sets.size,
                totalVolumeKg = 0.0,
                isLegacySummaryOnly = false,
            ),
            exercises = listOf(
                WorkoutHistoryExerciseGraph(
                    exercise = WorkoutHistoryExercise(
                        id = exerciseId,
                        sessionId = sessionId,
                        sourceRoutineExerciseId = sourceId,
                        name = name,
                        note = "",
                        restSeconds = 60,
                        position = 0,
                    ),
                    sets = sets.mapIndexed { index, (weight, reps) ->
                        WorkoutHistorySet(
                            id = "set-$completedAtMillis-$index",
                            workoutExerciseId = exerciseId,
                            position = index,
                            weightKg = weight,
                            repetitions = reps,
                        )
                    },
                ),
            ),
            photos = emptyList(),
        )
    }

    private fun graph(
        completedAtMillis: Long,
        sourceId: String?,
        name: String,
        setPosition: Int,
        weight: Double?,
        reps: Int?,
    ): WorkoutHistoryGraph {
        val sessionId = "session-$completedAtMillis"
        val exerciseId = "exercise-$completedAtMillis"
        return WorkoutHistoryGraph(
            session = WorkoutHistorySession(
                id = sessionId,
                sourceRoutineId = null,
                routineName = "Routine",
                startedAtMillis = completedAtMillis - 100L,
                completedAtMillis = completedAtMillis,
                durationMillis = 100L,
                note = "",
                completedSetCount = 1,
                totalVolumeKg = 0.0,
                isLegacySummaryOnly = false,
            ),
            exercises = listOf(
                WorkoutHistoryExerciseGraph(
                    exercise = WorkoutHistoryExercise(
                        id = exerciseId,
                        sessionId = sessionId,
                        sourceRoutineExerciseId = sourceId,
                        name = name,
                        note = "",
                        restSeconds = 60,
                        position = 0,
                    ),
                    sets = listOf(
                        WorkoutHistorySet(
                            id = "set-$completedAtMillis",
                            workoutExerciseId = exerciseId,
                            position = setPosition,
                            weightKg = weight,
                            repetitions = reps,
                        ),
                    ),
                ),
            ),
            photos = emptyList(),
        )
    }
}