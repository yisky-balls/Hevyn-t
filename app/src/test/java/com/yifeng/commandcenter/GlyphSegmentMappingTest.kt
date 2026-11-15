package com.yifeng.commandcenter

import com.nothing.ketchum.Glyph
import com.yifeng.commandcenter.glyph.GlyphRestTimerController
import com.yifeng.commandcenter.glyph.GlyphRestTimerController.Companion.elapsedPercent
import com.yifeng.commandcenter.glyph.GlyphRestTimerController.Companion.segmentCount
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests verifying that:
 * 1. The visual behavior starts completely empty (0 segments) and fills to full (6 segments).
 * 2. The fill order begins with A6 (physically closest to the red indicator) and ends at A1.
 * 3. The full exercise rest duration is proportionally represented across all six segments.
 */
class GlyphSegmentMappingTest {

    // ---- Fill order tests -----------------------------------------------

    @Test
    fun `fill order starts at A6 closest to red LED and ends at A1`() {
        val expected = listOf(
            Glyph.Code_25111.A_6, // closest to red recording LED at base
            Glyph.Code_25111.A_5,
            Glyph.Code_25111.A_4,
            Glyph.Code_25111.A_3,
            Glyph.Code_25111.A_2,
            Glyph.Code_25111.A_1, // furthest from red recording LED at top
        )
        assertEquals(expected, GlyphRestTimerController.FILL_ORDER)
    }

    // ---- segmentCount thresholds ----------------------------------------

    @Test
    fun `segmentCount at 0 percent elapsed is 0 segments (empty start)`() {
        assertEquals(0, segmentCount(0))
    }

    @Test
    fun `segmentCount at 1 percent elapsed is 1 segment`() {
        assertEquals(1, segmentCount(1))
    }

    @Test
    fun `segmentCount at 16 percent elapsed is 1 segment`() {
        assertEquals(1, segmentCount(16))
    }

    @Test
    fun `segmentCount at 17 percent elapsed is 2 segments`() {
        assertEquals(2, segmentCount(17))
    }

    @Test
    fun `segmentCount at 33 percent elapsed is 2 segments`() {
        assertEquals(2, segmentCount(33))
    }

    @Test
    fun `segmentCount at 34 percent elapsed is 3 segments`() {
        assertEquals(3, segmentCount(34))
    }

    @Test
    fun `segmentCount at 50 percent elapsed is 3 segments`() {
        assertEquals(3, segmentCount(50))
    }

    @Test
    fun `segmentCount at 51 percent elapsed is 4 segments`() {
        assertEquals(4, segmentCount(51))
    }

    @Test
    fun `segmentCount at 66 percent elapsed is 4 segments`() {
        assertEquals(4, segmentCount(66))
    }

    @Test
    fun `segmentCount at 67 percent elapsed is 5 segments`() {
        assertEquals(5, segmentCount(67))
    }

    @Test
    fun `segmentCount at 83 percent elapsed is 5 segments`() {
        assertEquals(5, segmentCount(83))
    }

    @Test
    fun `segmentCount at 84 percent elapsed is 6 segments`() {
        assertEquals(6, segmentCount(84))
    }

    @Test
    fun `segmentCount at 100 percent elapsed is 6 segments (full)`() {
        assertEquals(6, segmentCount(100))
    }

    @Test
    fun `segmentCount clamps negative values to 0`() {
        assertEquals(0, segmentCount(-10))
    }

    @Test
    fun `segmentCount clamps values over 100 to 6`() {
        assertEquals(6, segmentCount(150))
    }

    // ---- elapsedPercent tests -------------------------------------------

    @Test
    fun `elapsedPercent is 0 when timer just started`() {
        assertEquals(0, elapsedPercent(remainingMs = 180_000L, totalMs = 180_000L))
    }

    @Test
    fun `elapsedPercent is 100 when timer is finished`() {
        assertEquals(100, elapsedPercent(remainingMs = 0L, totalMs = 180_000L))
    }

    @Test
    fun `elapsedPercent is 50 when timer is halfway`() {
        assertEquals(50, elapsedPercent(remainingMs = 90_000L, totalMs = 180_000L))
    }

    @Test
    fun `elapsedPercent is 0 for zero or negative total duration`() {
        assertEquals(0, elapsedPercent(remainingMs = 10_000L, totalMs = 0L))
        assertEquals(0, elapsedPercent(remainingMs = 10_000L, totalMs = -10L))
    }

    @Test
    fun `elapsedPercent clamps to 100 if remaining is negative`() {
        assertEquals(100, elapsedPercent(remainingMs = -5_000L, totalMs = 60_000L))
    }

    @Test
    fun `elapsedPercent clamps to 0 if remaining exceeds total`() {
        assertEquals(0, elapsedPercent(remainingMs = 75_000L, totalMs = 60_000L))
    }

    // ---- Exercise timer progression tests --------------------------------

    @Test
    fun `3-minute rest timer fills proportionally across the entire 180 seconds`() {
        val totalMs = 180_000L

        // START: 3:00 remaining -> 0 segments (empty)
        assertEquals(0, segmentCount(elapsedPercent(remainingMs = 180_000L, totalMs = totalMs)))

        // 2:30 remaining: 30s elapsed (16.6%) -> 1 segment
        assertEquals(1, segmentCount(elapsedPercent(remainingMs = 150_000L, totalMs = totalMs)))

        // 2:00 remaining: 60s elapsed (33.3%) -> 2 segments
        assertEquals(2, segmentCount(elapsedPercent(remainingMs = 120_000L, totalMs = totalMs)))

        // 1:30 remaining: 90s elapsed (50.0%) -> 3 segments
        assertEquals(3, segmentCount(elapsedPercent(remainingMs = 90_000L, totalMs = totalMs)))

        // 1:00 remaining: 120s elapsed (66.6%) -> 4 segments
        assertEquals(4, segmentCount(elapsedPercent(remainingMs = 60_000L, totalMs = totalMs)))

        // 0:30 remaining: 150s elapsed (83.3%) -> 5 segments
        assertEquals(5, segmentCount(elapsedPercent(remainingMs = 30_000L, totalMs = totalMs)))

        // 0:00 remaining: 180s elapsed (100%) -> 6 segments (full)
        assertEquals(6, segmentCount(elapsedPercent(remainingMs = 0L, totalMs = totalMs)))
    }

    @Test
    fun `2-minute rest timer fills proportionally across 120 seconds`() {
        assertFillProgression(durationSeconds = 120)
    }

    @Test
    fun `1-minute-30-second rest timer fills proportionally across 90 seconds`() {
        assertFillProgression(durationSeconds = 90)
    }

    @Test
    fun `1-minute rest timer fills proportionally across 60 seconds`() {
        assertFillProgression(durationSeconds = 60)
    }

    @Test
    fun `45-second rest timer fills proportionally across 45 seconds`() {
        assertFillProgression(durationSeconds = 45)
    }

    @Test
    fun `30-second rest timer fills proportionally across 30 seconds`() {
        assertFillProgression(durationSeconds = 30)
    }

    private fun assertFillProgression(durationSeconds: Int) {
        val totalMs = durationSeconds * 1_000L
        val transitions = mutableListOf<Pair<Int, Int>>() // (second, segments)
        var lastSegments = -1

        for (second in 0..durationSeconds) {
            val remainingMs = (durationSeconds - second) * 1_000L
            val percent = elapsedPercent(remainingMs, totalMs)
            val segments = segmentCount(percent)
            if (segments != lastSegments) {
                transitions.add(second to segments)
                lastSegments = segments
            }
        }

        // Must start completely empty (0 segments)
        assertEquals("Should start empty (0 segments)", 0, transitions.first().second)
        // Must end completely full (6 segments)
        assertEquals("Should end full (6 segments)", 6, transitions.last().second)

        // Must monotonically increase from 0 to 6
        for (i in 1 until transitions.size) {
            assertTrue(
                "Segments should increase: ${transitions[i - 1]} -> ${transitions[i]}",
                transitions[i].second > transitions[i - 1].second,
            )
        }
    }
}
