package com.yifeng.commandcenter

import com.yifeng.commandcenter.data.ActiveWorkoutSession
import com.yifeng.commandcenter.data.RepeatLastWorkout
import com.yifeng.commandcenter.data.RepeatLastWorkoutResult
import com.yifeng.commandcenter.data.RepeatedWorkoutMapper
import com.yifeng.commandcenter.history.WorkoutHistoryExercise
import com.yifeng.commandcenter.history.WorkoutHistoryExerciseGraph
import com.yifeng.commandcenter.history.WorkoutHistoryGraph
import com.yifeng.commandcenter.history.WorkoutHistorySession
import com.yifeng.commandcenter.history.WorkoutHistorySet
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Test

class RepeatLastWorkoutTest {
    @Test
    fun `mapper preserves ordered values and configuration with fresh mutable state`() {
        val graph = historyGraph()
        val ids = ArrayDeque(listOf("workout-new", "exercise-new", "set-new-0", "set-new-1"))

        val workout = RepeatedWorkoutMapper.fromHistory(graph, 99_000L) { ids.removeFirst() }

        assertEquals("workout-new", workout.id)
        assertNotEquals(graph.session.id, workout.id)
        assertEquals("routine", workout.sourceRoutineId)
        assertEquals("Upper A", workout.routineName)
        assertEquals(99_000L, workout.startedAtMillis)
        assertEquals(listOf("Incline DB Press"), workout.exercises.map { it.name })
        assertEquals("source-press", workout.exercises.single().sourceRoutineExerciseId)
        assertEquals("Controlled eccentric", workout.exercises.single().notes)
        assertEquals(135, workout.exercises.single().restSeconds)
        assertEquals(listOf("30", "32.5"), workout.exercises.single().sets.map { it.weight })
        assertEquals(listOf("10", "8"), workout.exercises.single().sets.map { it.reps })
        assertEquals(listOf("set-new-0", "set-new-1"), workout.exercises.single().sets.map { it.id })
        assertFalse(workout.exercises.single().sets.any { it.completed })

        val edited = workout.copy(routineName = "Edited")
        assertEquals("Upper A", graph.session.routineName)
        assertEquals("Edited", edited.routineName)
    }

    @Test
    fun `use case handles no history without trying to persist`() = runBlocking {
        var starts = 0
        val result = RepeatLastWorkout(
            latestGraph = { null },
            startIfIdle = { starts++; true },
        )()

        assertSame(RepeatLastWorkoutResult.NoHistory, result)
        assertEquals(0, starts)
    }

    @Test
    fun `use case reports active workout race without replacing it`() = runBlocking {
        var attempted: ActiveWorkoutSession? = null
        val result = RepeatLastWorkout(
            latestGraph = { historyGraph() },
            startIfIdle = { attempted = it; false },
            nowMillis = { 123L },
            nextId = { "fresh-${System.nanoTime()}" },
        )()

        assertSame(RepeatLastWorkoutResult.ActiveWorkoutExists, result)
        assertEquals(123L, attempted?.workout?.startedAtMillis)
    }

    private fun historyGraph() = WorkoutHistoryGraph(
        session = WorkoutHistorySession(
            id = "historic-session",
            sourceRoutineId = "routine",
            routineName = "Upper A",
            startedAtMillis = 1_000L,
            completedAtMillis = 2_000L,
            durationMillis = 1_000L,
            note = "",
            completedSetCount = 2,
            totalVolumeKg = 560.0,
            isLegacySummaryOnly = false,
        ),
        exercises = listOf(
            WorkoutHistoryExerciseGraph(
                exercise = WorkoutHistoryExercise(
                    id = "historic-press",
                    sessionId = "historic-session",
                    sourceRoutineExerciseId = "source-press",
                    name = "Incline DB Press",
                    note = "Controlled eccentric",
                    restSeconds = 135,
                    position = 2,
                ),
                sets = listOf(
                    WorkoutHistorySet("historic-set-1", "historic-press", 1, 32.5, 8),
                    WorkoutHistorySet("historic-set-0", "historic-press", 0, 30.0, 10),
                ),
            ),
        ),
        photos = emptyList(),
    )
}