package com.yifeng.commandcenter

import com.yifeng.commandcenter.metrics.BodyMetricEntry
import com.yifeng.commandcenter.metrics.BodyMetricsCsvParser
import com.yifeng.commandcenter.metrics.BodyMetricsRepository
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BodyMetricsCsvParserTest {

    @Test
    fun parsesItalianHeadersWithQuotedCommaDecimalsAndEuDates() {
        val csv = """
            Data,Peso (kg),Calorie
            01/11/2026,"76,4",2350
            05/11/2026,"75,9","2.420"
            18/11/2026,"75,2",2200 kcal
        """.trimIndent()

        val result = BodyMetricsCsvParser.parse(csv, fallbackYear = 2026)

        assertEquals(3, result.entries.size)
        assertEquals(LocalDate.of(2026, 11, 1).toEpochDay(), result.entries[0].dateEpochDay)
        assertEquals(76.4, result.entries[0].weightKg!!, 0.001)
        assertEquals(2350, result.entries[0].caloriesKcal)

        assertEquals(LocalDate.of(2026, 11, 5).toEpochDay(), result.entries[1].dateEpochDay)
        assertEquals(75.9, result.entries[1].weightKg!!, 0.001)
        assertEquals(2420, result.entries[1].caloriesKcal)

        assertEquals(LocalDate.of(2026, 11, 18).toEpochDay(), result.entries[2].dateEpochDay)
        assertEquals(75.2, result.entries[2].weightKg!!, 0.001)
        assertEquals(2200, result.entries[2].caloriesKcal)
    }

    @Test
    fun parsesSemicolonDelimitedAndPartialRowsAndSortsChronologically() {
        val csv = """
            Date;Weight;Calories
            2026-11-15;74.8;
            2026-11-10;;2500
            2026-11-12;75.1;2300
            ;;
            invalid_date;74.0;2100
        """.trimIndent()

        val result = BodyMetricsCsvParser.parse(csv, fallbackYear = 2026)

        assertEquals(3, result.entries.size)
        assertEquals(LocalDate.of(2026, 11, 10).toEpochDay(), result.entries[0].dateEpochDay)
        assertNull(result.entries[0].weightKg)
        assertEquals(2500, result.entries[0].caloriesKcal)

        assertEquals(LocalDate.of(2026, 11, 12).toEpochDay(), result.entries[1].dateEpochDay)
        assertEquals(75.1, result.entries[1].weightKg!!, 0.001)
        assertEquals(2300, result.entries[1].caloriesKcal)

        assertEquals(LocalDate.of(2026, 11, 15).toEpochDay(), result.entries[2].dateEpochDay)
        assertEquals(74.8, result.entries[2].weightKg!!, 0.001)
        assertNull(result.entries[2].caloriesKcal)
    }

    @Test
    fun mergesDuplicateDatesKeepingLatestNonNullValues() {
        val csv = """
            Data,Peso,Kcal
            2026-11-18,75.0,
            2026-11-18,,2400
            2026-11-18,74.8,
        """.trimIndent()

        val result = BodyMetricsCsvParser.parse(csv, fallbackYear = 2026)

        assertEquals(1, result.entries.size)
        assertEquals(74.8, result.entries[0].weightKg!!, 0.001)
        assertEquals(2400, result.entries[0].caloriesKcal)
    }

    @Test
    fun roundTripsCachedEntriesJson() {
        val entries = listOf(
            BodyMetricEntry(LocalDate.of(2026, 11, 1).toEpochDay(), 76.2, 2350),
            BodyMetricEntry(LocalDate.of(2026, 11, 4).toEpochDay(), 75.8, null),
            BodyMetricEntry(LocalDate.of(2026, 11, 7).toEpochDay(), null, 2410),
        )
        val encoded = BodyMetricsRepository.encodeEntries(entries)
        val decoded = BodyMetricsRepository.decodeEntries(encoded)
        assertEquals(entries, decoded)
    }

    @Test
    fun parsesDashboardWithMultipleHeaderSections() {
        val csv = """
            ,HEALTH & WEIGHT TRACKING DASHBOARD,,,,,Metric,Value,Notes / Unit
            ,Date,Weight (kg),Total Calories,What I Ate / Notes,Save Entry,,,
            ,2026-11-19,,,,FALSE,,,
            ,RECENT ACTIVITY FEED (LAST 14 LOGGED ENTRIES),,,,,,,
            ,Date,Weight (kg),7-Day Avg,Calories,What I Ate / Notes,,,
            ,2026-11-19,,,"2,310","Colazione e cena",,,
            ,2026-11-14,63.80 kg,,,,,,
        """.trimIndent()

        val result = BodyMetricsCsvParser.parse(csv, fallbackYear = 2026)

        assertEquals(2, result.entries.size)
        assertEquals(63.8, result.entries[0].weightKg!!, 0.001)
        assertNull(result.entries[0].caloriesKcal)
        assertNull(result.entries[1].weightKg)
        assertEquals(2310, result.entries[1].caloriesKcal)
    }

    @Test
    fun parsesWeightTrackingSheetWithMixedDateFormatsAndCalories() {
        val csv = """
            Date,Weight (kg),7-Day Avg Weight,Total Calories,What I Ate / Notes
            2026-09-16,63.00,63.00,"2,265","Greek yogurt"
            2026-09-18,60.65,61.50,"2,465","snack"
            28/10/2026,61.20,,,
            14/11/2026,63.80,,,
            2026-11-19,,,"2,310","Colazione"
        """.trimIndent()

        val result = BodyMetricsCsvParser.parse(csv, fallbackYear = 2026)

        assertEquals(5, result.entries.size)
        assertEquals(2265, result.entries[0].caloriesKcal)
        assertEquals(2465, result.entries[1].caloriesKcal)
        assertEquals(LocalDate.of(2026, 10, 28).toEpochDay(), result.entries[2].dateEpochDay)
        assertEquals(61.2, result.entries[2].weightKg!!, 0.001)
        assertEquals(LocalDate.of(2026, 11, 14).toEpochDay(), result.entries[3].dateEpochDay)
        assertEquals(63.8, result.entries[3].weightKg!!, 0.001)
        assertEquals(2310, result.entries[4].caloriesKcal)
    }

    @Test
    fun extractsMultipleTabGidsFromHtmlView() {
        val html = """
            items.push({name: "Dashboard", pageUrl: "https:\/\/docs.google.com\/spreadsheets\/d\/abc\/htmlview\/sheet?headers\x3dtrue&gid=1950319252", gid: "1950319252",initialSheet: ("1950319252" == gid)})
            items.push({name: "weight tracking", pageUrl: "https:\/\/docs.google.com\/spreadsheets\/d\/abc\/htmlview\/sheet?headers\x3dtrue&gid=272414793", gid: "272414793",initialSheet: ("272414793" == gid)})
        """.trimIndent()

        val gids = BodyMetricsRepository.extractTabGidsFromHtmlView(html)
        assertEquals(listOf("1950319252", "272414793"), gids)
    }
}
