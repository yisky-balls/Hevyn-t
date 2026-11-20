package com.yifeng.commandcenter

import com.yifeng.commandcenter.metrics.BodyMetricEntry
import com.yifeng.commandcenter.metrics.BodyMetricsCalculator
import com.yifeng.commandcenter.metrics.BodyMetricsPeriod
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class BodyMetricsCalculatorTest {

    @Test
    fun filtersByPeriodAndComputesStatsAndMovingAverage() {
        val today = LocalDate.of(2026, 11, 20)
        val entries = listOf(
            BodyMetricEntry(today.minusDays(45).toEpochDay(), 78.0, 2600),
            BodyMetricEntry(today.minusDays(10).toEpochDay(), 76.0, 2400),
            BodyMetricEntry(today.minusDays(5).toEpochDay(), 75.4, 2300),
            BodyMetricEntry(today.minusDays(2).toEpochDay(), 75.0, 2200),
            BodyMetricEntry(today.toEpochDay(), 74.6, 2100),
        )

        val summary7d = BodyMetricsCalculator.summarize(
            entries = entries,
            period = BodyMetricsPeriod.DAYS_7,
            todayEpochDay = today.toEpochDay(),
        )
        assertEquals(3, summary7d.weightPoints.size)
        assertEquals(74.6, summary7d.latestWeightKg!!, 0.001)
        assertEquals(-0.8, summary7d.weightDeltaKg!!, 0.001)
        assertEquals(2200, summary7d.avgCalories)
        assertEquals(2100, summary7d.minCalories)
        assertEquals(2300, summary7d.maxCalories)
        // Moving avg on day -5 includes day -10 (within 6-day window: -11..-5): (76.0 + 75.4) / 2 = 75.7
        assertEquals(3, summary7d.weightMovingAvgPoints.size)
        assertEquals(75.7, summary7d.weightMovingAvgPoints.first().weightKg, 0.001)

        val summaryAll = BodyMetricsCalculator.summarize(
            entries = entries,
            period = BodyMetricsPeriod.ALL,
            todayEpochDay = today.toEpochDay(),
        )
        assertEquals(5, summaryAll.weightPoints.size)
        assertEquals(-3.4, summaryAll.weightDeltaKg!!, 0.001)
    }
}
