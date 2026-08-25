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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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