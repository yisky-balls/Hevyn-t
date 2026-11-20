package com.yifeng.commandcenter.metrics

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField
import java.util.Locale
import kotlin.math.roundToInt

data class BodyMetricsParseResult(
    val entries: List<BodyMetricEntry>,
    val skippedRows: Int,
)

object BodyMetricsCsvParser {
    private val DATE_HEADERS = setOf(
        "date", "data", "giorno", "day", "timestamp", "quando", "dt",
    )
    private val WEIGHT_HEADERS = setOf(
        "weight", "peso", "kg", "weight kg", "weight (kg)", "peso kg", "peso (kg)",
        "bodyweight", "body weight", "bw", "peso corporeo",
    )
    private val CALORIES_HEADERS = setOf(
        "calories", "calorie", "kcal", "cal", "energy", "energia", "intake",
        "calorie (kcal)", "calories (kcal)", "daily calories", "kcal/giorno",
    )

    private val ISO_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val DMY_SLASH = DateTimeFormatter.ofPattern("d/M/yyyy", Locale.US)
    private val DMY_DASH = DateTimeFormatter.ofPattern("d-M-yyyy", Locale.US)
    private val DMY_DOT = DateTimeFormatter.ofPattern("d.M.yyyy", Locale.US)
    private val MDY_SLASH = DateTimeFormatter.ofPattern("M/d/yyyy", Locale.US)
    private val MDY_DASH = DateTimeFormatter.ofPattern("M-d-yyyy", Locale.US)
    private val DMY_SHORT_YEAR_SLASH = DateTimeFormatter.ofPattern("d/M/yy", Locale.US)
    private val MDY_SHORT_YEAR_SLASH = DateTimeFormatter.ofPattern("M/d/yy", Locale.US)
    private val TEXT_DATE_IT = DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern("d MMM yyyy")
        .toFormatter(Locale.ITALIAN)
    private val TEXT_DATE_EN = DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern("d MMM yyyy")
        .toFormatter(Locale.ENGLISH)
    private val TEXT_DATE_EN_ALT = DateTimeFormatterBuilder()
        .parseCaseInsensitive()
        .appendPattern("MMM d, yyyy")
        .toFormatter(Locale.ENGLISH)

    private val NUMERIC_SLASHDASH_REGEX = Regex("^(\\d{1,2})[/\\-.](\\d{1,2})[/\\-.](\\d{2,4})$")
    private val DAY_MONTH_ONLY_REGEX = Regex("^(\\d{1,2})[/\\-.](\\d{1,2})$")

    fun parse(csvContent: String, fallbackYear: Int = LocalDate.now().year): BodyMetricsParseResult {
        val cleaned = csvContent.removePrefix("\uFEFF")
        val rawLines = cleaned.lineSequence().filter { it.isNotBlank() }.toList()
        if (rawLines.isEmpty()) return BodyMetricsParseResult(emptyList(), 0)

        val delimiter = detectDelimiter(rawLines.take(10))
        val rows = parseCsvRows(cleaned, delimiter).filter { row -> row.any { it.isNotBlank() } }
        if (rows.isEmpty()) return BodyMetricsParseResult(emptyList(), 0)

        val initialHeader = detectHeaders(rows)
        val dataRows = if (initialHeader != null) rows.drop(initialHeader.headerRowIndex + 1) else rows
        var dateCol = initialHeader?.dateCol ?: 0
        var weightCol = initialHeader?.weightCol ?: 1
        var caloriesCol = initialHeader?.caloriesCol ?: 2

        val preferMonthFirst = detectMonthFirstPreference(dataRows.mapNotNull { it.getOrNull(dateCol) })

        val byDate = LinkedHashMap<Long, BodyMetricEntry>()
        var skipped = 0

        for (row in dataRows) {
            val inlineHeader = matchHeaderRow(row)
            if (inlineHeader != null) {
                dateCol = inlineHeader.dateCol
                weightCol = inlineHeader.weightCol
                caloriesCol = inlineHeader.caloriesCol
                continue
            }

            val rawDate = row.getOrNull(dateCol)?.trim().orEmpty()
            val date = parseDate(rawDate, preferMonthFirst, fallbackYear)
            if (date == null) {
                skipped++
                continue
            }

            val weight = row.getOrNull(weightCol)?.let(::parseWeightKg)
            val calories = row.getOrNull(caloriesCol)?.let(::parseCalories)
            if (weight == null && calories == null) {
                skipped++
                continue
            }

            val epochDay = date.toEpochDay()
            val existing = byDate[epochDay]
            byDate[epochDay] = BodyMetricEntry(
                dateEpochDay = epochDay,
                weightKg = weight ?: existing?.weightKg,
                caloriesKcal = calories ?: existing?.caloriesKcal,
            )
        }

        val sorted = byDate.values.sortedBy { it.dateEpochDay }
        return BodyMetricsParseResult(entries = sorted, skippedRows = skipped)
    }

    private data class HeaderMatch(
        val headerRowIndex: Int,
        val dateCol: Int,
        val weightCol: Int,
        val caloriesCol: Int,
    )

    private fun detectHeaders(rows: List<List<String>>): HeaderMatch? {
        val limit = minOf(rows.size, 5)
        for (rowIdx in 0 until limit) {
            val match = matchHeaderRow(rows[rowIdx], rowIdx)
            if (match != null) return match
        }
        return null
    }

    private fun matchHeaderRow(row: List<String>, rowIdx: Int = 0): HeaderMatch? {
        val cells = row.map { normalizeHeader(it) }
        val dateIdx = cells.indexOfFirst { isDateHeader(it) }
        val weightIdx = cells.indexOfFirst { isWeightHeader(it) }
        val caloriesIdx = cells.indexOfFirst { isCaloriesHeader(it) }
        val matches = listOf(dateIdx, weightIdx, caloriesIdx).count { it >= 0 }
        return if (matches >= 2) {
            HeaderMatch(
                headerRowIndex = rowIdx,
                dateCol = if (dateIdx >= 0) dateIdx else 0,
                weightCol = if (weightIdx >= 0) weightIdx else -1,
                caloriesCol = if (caloriesIdx >= 0) caloriesIdx else -1,
            )
        } else {
            null
        }
    }

    private fun normalizeHeader(cell: String): String =
        cell.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")

    private fun isDateHeader(h: String): Boolean =
        h in DATE_HEADERS || h.startsWith("date") || h.startsWith("data") || h.contains("giorno")

    private fun isWeightHeader(h: String): Boolean {
        if (h.contains("avg") || h.contains("average") || h.contains("media")) return false
        return h in WEIGHT_HEADERS || h.contains("weight") || h.contains("peso")
    }

    private fun isCaloriesHeader(h: String): Boolean {
        if (h.contains("avg") || h.contains("average") || h.contains("media")) return false
        return h in CALORIES_HEADERS || h.contains("calori") || h.contains("kcal")
    }

    private fun detectDelimiter(sampleLines: List<String>): Char {
        var commas = 0
        var semicolons = 0
        var tabs = 0
        for (line in sampleLines) {
            var inQuotes = false
            for (c in line) {
                when {
                    c == '"' -> inQuotes = !inQuotes
                    !inQuotes && c == ',' -> commas++
                    !inQuotes && c == ';' -> semicolons++
                    !inQuotes && c == '\t' -> tabs++
                }
            }
        }
        return when {
            semicolons > commas && semicolons >= tabs -> ';'
            tabs > commas && tabs > semicolons -> '\t'
            else -> ','
        }
    }

    internal fun parseCsvRows(input: String, delimiter: Char): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val currentRow = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false
        var i = 0
        val n = input.length

        while (i < n) {
            val ch = input[i]
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < n && input[i + 1] == '"') {
                        currentField.append('"')
                        i += 2
                        continue
                    } else {
                        inQuotes = false
                    }
                } else {
                    currentField.append(ch)
                }
            } else {
                when (ch) {
                    '"' -> inQuotes = true
                    delimiter -> {
                        currentRow.add(currentField.toString().trim())
                        currentField.setLength(0)
                    }
                    '\r' -> {
                        currentRow.add(currentField.toString().trim())
                        currentField.setLength(0)
                        rows.add(currentRow.toList())
                        currentRow.clear()
                        if (i + 1 < n && input[i + 1] == '\n') i++
                    }
                    '\n' -> {
                        currentRow.add(currentField.toString().trim())
                        currentField.setLength(0)
                        rows.add(currentRow.toList())
                        currentRow.clear()
                    }
                    else -> currentField.append(ch)
                }
            }
            i++
        }

        if (currentField.isNotEmpty() || currentRow.isNotEmpty()) {
            currentRow.add(currentField.toString().trim())
            rows.add(currentRow.toList())
        }
        return rows
    }

    private fun detectMonthFirstPreference(rawDates: List<String>): Boolean {
        var dmyEvidence = 0
        var mdyEvidence = 0
        for (raw in rawDates) {
            val cleaned = stripTimePortion(raw.trim())
            val match = NUMERIC_SLASHDASH_REGEX.matchEntire(cleaned) ?: continue
            val first = match.groupValues[1].toIntOrNull() ?: continue
            val second = match.groupValues[2].toIntOrNull() ?: continue
            if (first > 12 && second in 1..12) dmyEvidence++
            if (second > 12 && first in 1..12) mdyEvidence++
        }
        return mdyEvidence > dmyEvidence
    }

    private fun stripTimePortion(raw: String): String {
        val tIndex = raw.indexOf('T')
        if (tIndex > 0 && raw.length >= 10 && raw[4] == '-') {
            return raw.substring(0, tIndex)
        }
        val spaceIndex = raw.indexOf(' ')
        if (spaceIndex > 0) {
            val after = raw.substring(spaceIndex + 1).trim()
            if (after.contains(':')) {
                return raw.substring(0, spaceIndex)
            }
        }
        return raw
    }

    internal fun parseDate(raw: String, preferMonthFirst: Boolean, fallbackYear: Int): LocalDate? {
        val cleaned = stripTimePortion(raw.trim())
        if (cleaned.isEmpty()) return null

        // ISO yyyy-MM-dd (or yyyy/MM/dd)
        if (cleaned.length >= 8 && cleaned[4] in charArrayOf('-', '/')) {
            val normalized = cleaned.replace('/', '-')
            runCatching { LocalDate.parse(normalized, ISO_FORMATTER) }.getOrNull()?.let { return it }
        }

        val slashDash = NUMERIC_SLASHDASH_REGEX.matchEntire(cleaned)
        if (slashDash != null) {
            val formatters = if (preferMonthFirst) {
                listOf(MDY_SLASH, MDY_DASH, MDY_SHORT_YEAR_SLASH, DMY_SLASH, DMY_DASH, DMY_DOT, DMY_SHORT_YEAR_SLASH)
            } else {
                listOf(DMY_SLASH, DMY_DASH, DMY_DOT, DMY_SHORT_YEAR_SLASH, MDY_SLASH, MDY_DASH, MDY_SHORT_YEAR_SLASH)
            }
            for (formatter in formatters) {
                runCatching { LocalDate.parse(cleaned, formatter) }.getOrNull()?.let { return it }
            }
        }

        val dayMonthOnly = DAY_MONTH_ONLY_REGEX.matchEntire(cleaned)
        if (dayMonthOnly != null) {
            val a = dayMonthOnly.groupValues[1].toIntOrNull()
            val b = dayMonthOnly.groupValues[2].toIntOrNull()
            if (a != null && b != null) {
                val (day, month) = if (preferMonthFirst && a in 1..12 && b in 1..31) b to a else a to b
                runCatching { LocalDate.of(fallbackYear, month, day) }.getOrNull()?.let { return it }
            }
        }

        for (formatter in listOf(TEXT_DATE_IT, TEXT_DATE_EN, TEXT_DATE_EN_ALT)) {
            runCatching { LocalDate.parse(cleaned, formatter) }.getOrNull()?.let { return it }
        }

        // Day + month name without year (e.g. "18 Nov", "18 nov")
        val noYearIt = DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("d MMM")
            .parseDefaulting(ChronoField.YEAR, fallbackYear.toLong())
            .toFormatter(Locale.ITALIAN)
        val noYearEn = DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendPattern("d MMM")
            .parseDefaulting(ChronoField.YEAR, fallbackYear.toLong())
            .toFormatter(Locale.ENGLISH)
        for (formatter in listOf(noYearIt, noYearEn)) {
            runCatching { LocalDate.parse(cleaned, formatter) }.getOrNull()?.let { return it }
        }

        return null
    }

    internal fun parseWeightKg(raw: String): Double? {
        val cleaned = raw.trim()
            .lowercase(Locale.ROOT)
            .replace("kg", "")
            .replace("lbs", "")
            .replace("lb", "")
            .replace(" ", "")
            .trim()
        val value = parseFlexibleDouble(cleaned) ?: return null
        return value.takeIf { it in 20.0..350.0 }
    }

    internal fun parseCalories(raw: String): Int? {
        val cleaned = raw.trim()
            .lowercase(Locale.ROOT)
            .replace("kcal", "")
            .replace("cal", "")
            .replace(" ", "")
            .trim()
        if (cleaned.isEmpty()) return null

        // Handle thousands separator like "2.450" or "2,450" when 3 digits follow separator
        val thousandsMatch = Regex("^(\\d{1,2})[.,](\\d{3})$").matchEntire(cleaned)
        val value = if (thousandsMatch != null) {
            (thousandsMatch.groupValues[1] + thousandsMatch.groupValues[2]).toDoubleOrNull()
        } else {
            parseFlexibleDouble(cleaned)
        } ?: return null

        val rounded = value.roundToInt()
        return rounded.takeIf { it in 100..15000 }
    }

    private fun parseFlexibleDouble(cleaned: String): Double? {
        if (cleaned.isEmpty()) return null
        val hasComma = ',' in cleaned
        val hasDot = '.' in cleaned
        val normalized = when {
            hasComma && hasDot -> {
                if (cleaned.lastIndexOf(',') > cleaned.lastIndexOf('.')) {
                    cleaned.replace(".", "").replace(',', '.')
                } else {
                    cleaned.replace(",", "")
                }
            }
            hasComma -> cleaned.replace(',', '.')
            else -> cleaned
        }
        return normalized.toDoubleOrNull()
    }
}

object BodyMetricsCalculator {
    fun summarize(
        entries: List<BodyMetricEntry>,
        period: BodyMetricsPeriod,
        todayEpochDay: Long = LocalDate.now().toEpochDay(),
    ): BodyMetricsSummary {
        val sorted = entries.sortedBy { it.dateEpochDay }
        val cutoffEpochDay = period.days?.let { todayEpochDay - (it - 1L) }
        val inPeriod = if (cutoffEpochDay != null) {
            sorted.filter { it.dateEpochDay >= cutoffEpochDay }
        } else {
            sorted
        }

        val weightPoints = inPeriod.mapNotNull { entry ->
            entry.weightKg?.let { WeightPoint(entry.dateEpochDay, it) }
        }
        val caloriesPoints = inPeriod.mapNotNull { entry ->
            entry.caloriesKcal?.let { CaloriesPoint(entry.dateEpochDay, it) }
        }

        val allWeightPoints = sorted.mapNotNull { entry ->
            entry.weightKg?.let { WeightPoint(entry.dateEpochDay, it) }
        }
        val movingAvgPoints = computeRolling7DayAverage(allWeightPoints, cutoffEpochDay)

        val latestWeight = weightPoints.lastOrNull()
        val firstWeight = weightPoints.firstOrNull()
        val weightDelta = if (weightPoints.size >= 2 && latestWeight != null && firstWeight != null) {
            latestWeight.weightKg - firstWeight.weightKg
        } else {
            null
        }

        val latestCal = caloriesPoints.lastOrNull()
        val avgCal = if (caloriesPoints.isNotEmpty()) {
            caloriesPoints.map { it.caloriesKcal }.average().roundToInt()
        } else {
            null
        }

        return BodyMetricsSummary(
            period = period,
            weightPoints = weightPoints,
            weightMovingAvgPoints = movingAvgPoints,
            caloriesPoints = caloriesPoints,
            latestWeightKg = latestWeight?.weightKg,
            latestWeightDateEpochDay = latestWeight?.dateEpochDay,
            weightDeltaKg = weightDelta,
            minWeightKg = weightPoints.minOfOrNull { it.weightKg },
            maxWeightKg = weightPoints.maxOfOrNull { it.weightKg },
            avgWeightKg = weightPoints.takeIf { it.isNotEmpty() }?.map { it.weightKg }?.average(),
            latestCalories = latestCal?.caloriesKcal,
            latestCaloriesDateEpochDay = latestCal?.dateEpochDay,
            avgCalories = avgCal,
            minCalories = caloriesPoints.minOfOrNull { it.caloriesKcal },
            maxCalories = caloriesPoints.maxOfOrNull { it.caloriesKcal },
        )
    }

    private fun computeRolling7DayAverage(
        allWeightPoints: List<WeightPoint>,
        cutoffEpochDay: Long?,
    ): List<WeightPoint> {
        if (allWeightPoints.size < 2) return emptyList()
        return allWeightPoints.mapNotNull { point ->
            if (cutoffEpochDay != null && point.dateEpochDay < cutoffEpochDay) return@mapNotNull null
            val windowStart = point.dateEpochDay - 6L
            val window = allWeightPoints.filter { it.dateEpochDay in windowStart..point.dateEpochDay }
            if (window.isEmpty()) null
            else WeightPoint(point.dateEpochDay, window.map { it.weightKg }.average())
        }
    }
}
