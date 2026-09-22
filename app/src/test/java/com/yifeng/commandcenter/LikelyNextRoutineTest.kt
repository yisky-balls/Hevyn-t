package com.yifeng.commandcenter

import com.yifeng.commandcenter.data.LikelyNextRoutinePredictor
import com.yifeng.commandcenter.data.LikelyNextRoutineResolver
import com.yifeng.commandcenter.data.RoutineFolderSummary
import com.yifeng.commandcenter.data.RoutineRecommendationReason
import com.yifeng.commandcenter.data.RoutineSummary
import com.yifeng.commandcenter.history.WorkoutHistorySession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LikelyNextRoutineTest {
    @Test
    fun `A B C A B predicts C from observed transition`() {
        val result = LikelyNextRoutinePredictor.predict(
            folders = folder("A", "B", "C"),
            history = history("A", "B", "C", "A", "B"),
        )

        assertEquals("C", result?.routine?.id)
        assertEquals(RoutineRecommendationReason.ObservedTransition, result?.reason)
    }

    @Test
    fun `strongest repeated transition wins over a less common transition`() {
        val result = LikelyNextRoutinePredictor.predict(
            folders = folder("A", "B", "C", "D"),
            history = history("A", "B", "C", "B", "D", "B", "C", "B"),
        )

        assertEquals("C", result?.routine?.id)
    }

    @Test
    fun `insufficient history does not invent certainty among several untouched routines`() {
        assertNull(
            LikelyNextRoutinePredictor.predict(
                folders = folder("A", "B", "C"),
                history = emptyList(),
            ),
        )
    }

    @Test
    fun `unique routine not performed recently is a deterministic fallback`() {
        val result = LikelyNextRoutinePredictor.predict(
            folders = folder("A", "B", "C"),
            history = history("A", "B"),
        )

        assertEquals("C", result?.routine?.id)
        assertEquals(RoutineRecommendationReason.LeastRecentlyPerformed, result?.reason)
    }

    @Test
    fun `deleted and legacy routines are not prediction targets`() {
        val result = LikelyNextRoutinePredictor.predict(
            folders = folder("A", "B"),
            history = listOf(
                session("deleted", 1L),
                session("A", 2L),
                session("deleted", 3L),
                session("B", 4L),
            ),
        )

        assertEquals("A", result?.routine?.id)
    }

    @Test
    fun `shared resolver returns recommendation with attached last performed metadata`() {
        val resolved = LikelyNextRoutineResolver.resolve(
            folders = folder("A", "B", "C"),
            history = history("A", "B"),
        )

        assertEquals("C", resolved.recommendation?.routine?.id)
        assertEquals(1_000L, resolved.folders.single().routines.first { it.id == "A" }.lastPerformedAtMillis)
        assertEquals(2_000L, resolved.folders.single().routines.first { it.id == "B" }.lastPerformedAtMillis)
    }

    @Test
    fun `last performed metadata is attached independently per routine`() {
        val attached = LikelyNextRoutinePredictor.attachLastPerformed(
            folders = folder("A", "B"),
            history = history("A", "B", "A"),
        ).single().routines.associateBy(RoutineSummary::id)

        assertEquals(3_000L, attached["A"]?.lastPerformedAtMillis)
        assertEquals(2_000L, attached["B"]?.lastPerformedAtMillis)
    }

    private fun folder(vararg ids: String) = listOf(
        RoutineFolderSummary(
            id = "folder",
            name = "Folder",
            routines = ids.map { id ->
                RoutineSummary(id, "folder", id, exerciseCount = 3)
            },
        ),
    )

    private fun history(vararg routineIds: String) = routineIds.mapIndexed { index, id ->
        session(id, (index + 1) * 1_000L)
    }

    private fun session(routineId: String, completedAt: Long) = WorkoutHistorySession(
        id = "session-$routineId-$completedAt",
        sourceRoutineId = routineId,
        routineName = routineId,
        startedAtMillis = completedAt - 500L,
        completedAtMillis = completedAt,
        durationMillis = 500L,
        note = "",
        completedSetCount = 1,
        totalVolumeKg = 1.0,
        isLegacySummaryOnly = false,
    )
}