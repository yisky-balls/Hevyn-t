package com.yifeng.commandcenter.program

import com.yifeng.commandcenter.data.formatWeight
import com.yifeng.commandcenter.metrics.BodyMetricsCsvParser
import java.util.Locale

data class ProgramSheetParseResult(
    val weeks: List<ProgramSheetWeek>,
    val skippedRows: Int,
)

object ProgramSheetCsvParser {
    private val WEEK_HEADERS = setOf(
        "week", "settimana", "sett", "sett.", "wk", "w", "blocco",
    )
    private val DAY_HEADERS = setOf(
        "day", "giorno", "routine", "session", "seduta", "workout", "scheda", "giorno / routine", "day / routine",
    )
    private val EXERCISE_HEADERS = setOf(
        "exercise", "esercizio", "lift", "movimento", "name", "nome",
    )
    private val SETS_HEADERS = setOf(
        "sets", "serie", "set",
    )
    private val REPS_HEADERS = setOf(
        "reps", "ripetizioni", "rep", "target reps", "rip", "rip.",
    )
    private val WEIGHT_HEADERS = setOf(
        "weight", "peso", "kg", "weight (kg)", "peso (kg)", "weight kg", "peso kg", "carico", "carico (kg)", "load",
    )
    private val REST_HEADERS = setOf(
        "rest", "recupero", "rest (s)", "recupero (s)", "rest seconds", "pausa",
    )
    private val NOTES_HEADERS = setOf(
        "notes", "note", "rpe", "rir", "rpe / note", "commenti", "comments",
    )

    private val FIRST_NUMBER_REGEX = Regex("(\\d+)")
    private val MM_SS_REGEX = Regex("^(\\d+):(\\d{1,2})$")
    private val MINUTES_REGEX = Regex("^(\\d+(?:[.,]\\d+)?)\\s*m(?:in)?$", RegexOption.IGNORE_CASE)

    fun parse(csvContent: String): ProgramSheetParseResult {
        val cleaned = csvContent.removePrefix("\uFEFF")
        val rawLines = cleaned.lineSequence().filter { it.isNotBlank() }.toList()
        if (rawLines.isEmpty()) return ProgramSheetParseResult(emptyList(), 0)

        val delimiter = detectDelimiter(rawLines.take(25))
        val rows = BodyMetricsCsvParser.parseCsvRows(cleaned, delimiter)
            .filter { row -> row.any { it.isNotBlank() } }
        if (rows.isEmpty()) return ProgramSheetParseResult(emptyList(), 0)

        val header = detectHeader(rows) ?: return ProgramSheetParseResult(emptyList(), rows.size)
        val dataRows = rows.drop(header.rowIndex + 1)

        val weekAccumulators = LinkedHashMap<Int, WeekAccumulator>()
        var lastWeekNumber: Int? = null
        var lastWeekLabel: String? = null
        var lastDayName: String? = null
        var skipped = 0

        for (row in dataRows) {
            if (matchHeaderRow(row, 0) != null) continue

            val rawExercise = row.getOrNull(header.exerciseCol)?.trim().orEmpty()
            if (rawExercise.isEmpty()) {
                skipped++
                continue
            }

            val rawWeek = header.weekCol.takeIf { it >= 0 }?.let { row.getOrNull(it)?.trim() }.orEmpty()
            val parsedWeekNumber = parseWeekNumber(rawWeek) ?: lastWeekNumber ?: 1
            val weekLabel = if (rawWeek.isNotEmpty()) {
                rawWeek
            } else {
                lastWeekLabel ?: "W$parsedWeekNumber"
            }
            lastWeekNumber = parsedWeekNumber
            lastWeekLabel = weekLabel

            val rawDay = header.dayCol.takeIf { it >= 0 }?.let { row.getOrNull(it)?.trim() }.orEmpty()
            val dayName = rawDay.ifEmpty { lastDayName ?: "Day 1" }
            lastDayName = dayName

            val rawSets = header.setsCol.takeIf { it >= 0 }?.let { row.getOrNull(it)?.trim() }.orEmpty()
            val sets = parseSets(rawSets) ?: 3

            val rawReps = header.repsCol.takeIf { it >= 0 }?.let { row.getOrNull(it)?.trim() }.orEmpty()
            val reps = cleanReps(rawReps)

            val rawWeight = header.weightCol.takeIf { it >= 0 }?.let { row.getOrNull(it)?.trim() }.orEmpty()
            val weightKg = parseWeightString(rawWeight)

            val rawRest = header.restCol.takeIf { it >= 0 }?.let { row.getOrNull(it)?.trim() }.orEmpty()
            val restSeconds = parseRestSeconds(rawRest) ?: 150

            val rawNotes = header.notesCol.takeIf { it >= 0 }?.let { row.getOrNull(it)?.trim() }.orEmpty()

            val isDeloadHint = containsDeloadKeyword(weekLabel) ||
                containsDeloadKeyword(dayName) ||
                containsDeloadKeyword(rawNotes)

            val weekAcc = weekAccumulators.getOrPut(parsedWeekNumber) {
                WeekAccumulator(
                    weekNumber = parsedWeekNumber,
                    label = formatWeekLabel(parsedWeekNumber, weekLabel),
                    isDeload = isDeloadHint,
                )
            }
            if (isDeloadHint) {
                weekAcc.isDeload = true
            }

            val dayExercises = weekAcc.days.getOrPut(dayName) { mutableListOf() }
            dayExercises.add(
                ProgramSheetExercise(
                    name = rawExercise,
                    sets = sets,
                    reps = reps,
                    weightKg = weightKg,
                    restSeconds = restSeconds,
                    notes = rawNotes,
                ),
            )
        }

        val weeks = weekAccumulators.values
            .sortedBy { it.weekNumber }
            .map { acc ->
                ProgramSheetWeek(
                    weekNumber = acc.weekNumber,
                    label = acc.label,
                    isDeload = acc.isDeload,
                    days = acc.days.map { (dayName, exercises) ->
                        ProgramSheetDay(dayName = dayName, exercises = exercises.toList())
                    },
                )
            }

        return ProgramSheetParseResult(weeks = weeks, skippedRows = skipped)
    }

    private class WeekAccumulator(
        val weekNumber: Int,
        val label: String,
        var isDeload: Boolean,
        val days: LinkedHashMap<String, MutableList<ProgramSheetExercise>> = LinkedHashMap(),
    )

    private data class HeaderMatch(
        val rowIndex: Int,
        val weekCol: Int,
        val dayCol: Int,
        val exerciseCol: Int,
        val setsCol: Int,
        val repsCol: Int,
        val weightCol: Int,
        val restCol: Int,
        val notesCol: Int,
    )

    private fun detectHeader(rows: List<List<String>>): HeaderMatch? {
        val limit = minOf(rows.size, 30)
        for (i in 0 until limit) {
            val match = matchHeaderRow(rows[i], i)
            if (match != null) return match
        }
        return null
    }

    private fun matchHeaderRow(row: List<String>, rowIndex: Int): HeaderMatch? {
        val cells = row.map(::normalizeHeader)
        val weekCol = cells.indexOfFirst { it in WEEK_HEADERS || it.startsWith("week") || it.startsWith("sett") }
        val dayCol = cells.indexOfFirst { it in DAY_HEADERS || it.startsWith("giorno") || it.startsWith("day") || it.startsWith("routine") }
        val exerciseCol = cells.indexOfFirst { it in EXERCISE_HEADERS || it.startsWith("eserciz") || it.startsWith("exercis") }
        val setsCol = cells.indexOfFirst { it in SETS_HEADERS }
        val repsCol = cells.indexOfFirst { it in REPS_HEADERS || it.startsWith("ripet") || it.startsWith("rep") }
        val weightCol = cells.indexOfFirst { it in WEIGHT_HEADERS || it.contains("peso") || it.contains("weight") || it.contains("carico") }
        val restCol = cells.indexOfFirst { it in REST_HEADERS || it.startsWith("recuper") || it.startsWith("rest") }
        val notesCol = cells.indexOfFirst { it in NOTES_HEADERS || it.startsWith("note") }

        if (exerciseCol < 0) return null
        val matchedCore = listOf(weekCol, dayCol, setsCol, repsCol, weightCol).count { it >= 0 }
        if (matchedCore < 2) return null

        return HeaderMatch(
            rowIndex = rowIndex,
            weekCol = weekCol,
            dayCol = dayCol,
            exerciseCol = exerciseCol,
            setsCol = setsCol,
            repsCol = repsCol,
            weightCol = weightCol,
            restCol = restCol,
            notesCol = notesCol,
        )
    }

    private fun normalizeHeader(cell: String): String =
        cell.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")

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

    internal fun parseWeekNumber(raw: String): Int? {
        if (raw.isBlank()) return null
        return FIRST_NUMBER_REGEX.find(raw)?.groupValues?.getOrNull(1)?.toIntOrNull()?.takeIf { it in 1..52 }
    }

    private fun formatWeekLabel(weekNumber: Int, rawLabel: String): String {
        val trimmed = rawLabel.trim()
        if (trimmed.isEmpty() || trimmed == weekNumber.toString()) {
            return "W$weekNumber"
        }
        return if (containsDeloadKeyword(trimmed)) "W$weekNumber · Scarico" else "W$weekNumber"
    }

    private fun containsDeloadKeyword(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        return lower.contains("scarico") || lower.contains("deload")
    }

    private fun parseSets(raw: String): Int? {
        if (raw.isBlank()) return null
        return FIRST_NUMBER_REGEX.find(raw)?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceIn(1, 20)
    }

    private fun cleanReps(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed == "-" || trimmed == "—") return ""
        return trimmed
    }

    internal fun parseWeightString(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed == "-" || trimmed == "—" || trimmed.startsWith("#")) return ""
        val cleaned = trimmed
            .lowercase(Locale.ROOT)
            .replace("kg", "")
            .replace("lbs", "")
            .replace("lb", "")
            .replace(" ", "")
            .trim()
        if (cleaned.isEmpty()) return ""
        val normalized = when {
            ',' in cleaned && '.' in cleaned -> {
                if (cleaned.lastIndexOf(',') > cleaned.lastIndexOf('.')) {
                    cleaned.replace(".", "").replace(',', '.')
                } else {
                    cleaned.replace(",", "")
                }
            }
            ',' in cleaned -> cleaned.replace(',', '.')
            else -> cleaned
        }
        val value = normalized.toDoubleOrNull()?.takeIf { it > 0.0 && it <= 1000.0 } ?: return ""
        return formatWeight(value)
    }

    internal fun parseRestSeconds(raw: String): Int? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        MM_SS_REGEX.matchEntire(trimmed)?.let { match ->
            val min = match.groupValues[1].toIntOrNull() ?: return null
            val sec = match.groupValues[2].toIntOrNull() ?: return null
            return (min * 60 + sec).coerceIn(0, 1800)
        }
        MINUTES_REGEX.matchEntire(trimmed)?.let { match ->
            val min = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
            return (min * 60).toInt().coerceIn(0, 1800)
        }
        val digits = trimmed.lowercase(Locale.ROOT).replace("s", "").trim()
        return digits.toIntOrNull()?.coerceIn(0, 1800)
    }
}
