package com.yifeng.commandcenter

import com.yifeng.commandcenter.history.ExerciseProgressCalculator
import com.yifeng.commandcenter.history.ExerciseProgressMetric
import com.yifeng.commandcenter.history.ExerciseProgressPeriod
import com.yifeng.commandcenter.history.HistoricalExerciseSetRow
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class ExerciseProgressCalculatorTest {

    private fun epochMillis(date: LocalDate): Long =
        date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    @Test
    fun groupsNormalizedExerciseNamesAndSortsBySessionCount() {
        val d1 = LocalDate.of(2026, 10, 1)
        val d2 = LocalDate.of(2026, 10, 8)
        val d3 = LocalDate.of(2026, 10, 15)

        val rows = listOf(
            HistoricalExerciseSetRow("ex1", null, "bench press ", epochMillis(d1), 0, 75.0, 8),
            HistoricalExerciseSetRow("ex2", null, "Bench Press", epochMillis(d2), 0, 80.0, 6),
            HistoricalExerciseSetRow("ex3", null, "Squat", epochMillis(d3), 0, 100.0, 5),
        )

        val options = ExerciseProgressCalculator.buildOptions(rows)

        assertEquals(2, options.size)
        assertEquals("benchpress", options[0].normalizedKey)
        assertEquals("Bench Press", options[0].displayName)
        assertEquals(2, options[0].sessionCount)
        assertEquals(80.0, options[0].bestWeightKg!!, 0.001)
        assertEquals("squat", options[1].normalizedKey)
        assertEquals(1, options[1].sessionCount)
    }

    @Test
    fun computesTopWeightEstimated1RmVolumeAndAllTimePr() {
        val today = LocalDate.of(2026, 11, 20)
        val oldPrDay = today.minusDays(60)
        val day1 = today.minusDays(14)
        val day2 = today.minusDays(3)

        val rows = listOf(
            // Old all-time PR session (outside 30D window): 90 kg x 1 -> 1RM = 90.0, vol = 90
            HistoricalExerciseSetRow("s0", null, "Bench Press", epochMillis(oldPrDay), 0, 90.0, 1),
            // Session 1 in 30D: 75x8 (600) + 80x5 (400) -> top=80, vol=1000, 1RM=max(75*(1+8/30)=95, 80*(1+5/30)=93.33)=95
            HistoricalExerciseSetRow("s1", null, "Bench Press", epochMillis(day1), 0, 75.0, 8),
            HistoricalExerciseSetRow("s1", null, "Bench Press", epochMillis(day1), 1, 80.0, 5),
            // Session 2 in 30D: 82.5x6 -> top=82.5, vol=495, 1RM=82.5*(1+6/30)=99.0
            HistoricalExerciseSetRow("s2", null, "Bench Press", epochMillis(day2), 0, 82.5, 6),
        )

        val topSummary = ExerciseProgressCalculator.summarize(
            rows = rows,
            exerciseKey = "benchpress",
            metric = ExerciseProgressMetric.TOP_WEIGHT,
            period = ExerciseProgressPeriod.DAYS_30,
            todayEpochDay = today.toEpochDay(),
            zoneId = ZoneOffset.UTC,
        )
        assertNotNull(topSummary)
        assertEquals(2, topSummary!!.points.size)
        assertEquals(82.5, topSummary.latestValue!!, 0.001)
        assertEquals(2.5, topSummary.deltaValue!!, 0.001)
        assertEquals(82.5, topSummary.periodBest!!, 0.001)
        assertEquals(90.0, topSummary.allTimePr!!, 0.001)

        val oneRmSummary = ExerciseProgressCalculator.summarize(
            rows = rows,
            exerciseKey = "benchpress",
            metric = ExerciseProgressMetric.ESTIMATED_1RM,
            period = ExerciseProgressPeriod.DAYS_30,
            todayEpochDay = today.toEpochDay(),
            zoneId = ZoneOffset.UTC,
        )
        assertEquals(99.0, oneRmSummary!!.latestValue!!, 0.01)
        assertEquals(4.0, oneRmSummary.deltaValue!!, 0.01)
        assertEquals(99.0, oneRmSummary.allTimePr!!, 0.01)

        val volSummary = ExerciseProgressCalculator.summarize(
            rows = rows,
            exerciseKey = "benchpress",
            metric = ExerciseProgressMetric.SESSION_VOLUME,
            period = ExerciseProgressPeriod.DAYS_30,
            todayEpochDay = today.toEpochDay(),
            zoneId = ZoneOffset.UTC,
        )
        assertEquals(495.0, volSummary!!.latestValue!!, 0.001)
        assertEquals(-505.0, volSummary.deltaValue!!, 0.001)
        assertEquals(1000.0, volSummary.periodBest!!, 0.001)
    }
}
