package com.yifeng.commandcenter.program

import android.content.Context
import android.content.SharedPreferences
import com.yifeng.commandcenter.metrics.BodyMetricsRepository
import com.yifeng.commandcenter.metrics.GoogleSheetsUrlNormalizer
import com.yifeng.commandcenter.metrics.SheetUrlResult
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

sealed interface ProgramSheetSyncOutcome {
    data class Success(
        val weeks: List<ProgramSheetWeek>,
        val activeWeekNumber: Int,
        val syncedAtMillis: Long,
    ) : ProgramSheetSyncOutcome

    data class Failure(val message: String) : ProgramSheetSyncOutcome
}

class ProgramSheetRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadCachedState(): ProgramSheetState {
        val rawUrl = prefs.getString(KEY_SHEET_URL, "").orEmpty()
        val lastSynced = if (prefs.contains(KEY_LAST_SYNCED_AT)) {
            prefs.getLong(KEY_LAST_SYNCED_AT, 0L).takeIf { it > 0L }
        } else {
            null
        }
        val weeks = decodeWeeks(prefs.getString(KEY_CACHED_WEEKS, null))
        val savedActive = prefs.getInt(KEY_ACTIVE_WEEK, 1)
        val activeWeek = if (weeks.any { it.weekNumber == savedActive }) {
            savedActive
        } else {
            weeks.firstOrNull()?.weekNumber ?: 1
        }
        return ProgramSheetState(
            sheetUrl = rawUrl,
            weeks = weeks,
            activeWeekNumber = activeWeek,
            lastSyncedAtMillis = lastSynced,
            isSyncing = false,
            lastError = null,
        )
    }

    fun setActiveWeek(weekNumber: Int) {
        prefs.edit().putInt(KEY_ACTIVE_WEEK, weekNumber).apply()
    }

    fun clearConfiguration() {
        prefs.edit()
            .remove(KEY_SHEET_URL)
            .remove(KEY_CACHED_WEEKS)
            .remove(KEY_LAST_SYNCED_AT)
            .remove(KEY_ACTIVE_WEEK)
            .apply()
    }

    fun isStale(lastSyncedAtMillis: Long?, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (lastSyncedAtMillis == null) return true
        return (nowMillis - lastSyncedAtMillis) >= DEFAULT_STALE_AGE_MILLIS
    }

    suspend fun syncFromUrl(rawUrl: String): ProgramSheetSyncOutcome = withContext(Dispatchers.IO) {
        val normalized = when (val result = GoogleSheetsUrlNormalizer.normalizeToCsvUrl(rawUrl)) {
            is SheetUrlResult.Invalid -> return@withContext ProgramSheetSyncOutcome.Failure(result.reason)
            is SheetUrlResult.Valid -> result.csvUrl
        }

        val weeks = try {
            fetchBestProgramWeeks(rawUrl, normalized)
        } catch (e: SheetAccessException) {
            return@withContext ProgramSheetSyncOutcome.Failure(e.message ?: "Unable to read Google Sheet.")
        } catch (_: IOException) {
            return@withContext ProgramSheetSyncOutcome.Failure(
                "Could not reach Google Sheets. Check your internet connection and try again.",
            )
        } catch (e: Exception) {
            return@withContext ProgramSheetSyncOutcome.Failure(
                e.message ?: "Unexpected error while syncing Google Sheet.",
            )
        }

        if (weeks.isEmpty()) {
            return@withContext ProgramSheetSyncOutcome.Failure(
                "No valid program rows found. Make sure the sheet has columns like Settimana/Week, Giorno/Day, Esercizio/Exercise, Serie/Sets, Ripetizioni/Reps, Peso/Weight.",
            )
        }

        val savedActive = prefs.getInt(KEY_ACTIVE_WEEK, weeks.first().weekNumber)
        val resolvedActive = if (weeks.any { it.weekNumber == savedActive }) {
            savedActive
        } else {
            weeks.first().weekNumber
        }

        val now = System.currentTimeMillis()
        prefs.edit()
            .putString(KEY_SHEET_URL, rawUrl.trim())
            .putString(KEY_CACHED_WEEKS, encodeWeeks(weeks))
            .putInt(KEY_ACTIVE_WEEK, resolvedActive)
            .putLong(KEY_LAST_SYNCED_AT, now)
            .apply()

        ProgramSheetSyncOutcome.Success(
            weeks = weeks,
            activeWeekNumber = resolvedActive,
            syncedAtMillis = now,
        )
    }

    private fun fetchBestProgramWeeks(rawUrl: String, normalizedCsvUrl: String): List<ProgramSheetWeek> {
        val docId = BodyMetricsRepository.extractDocIdWithoutGid(rawUrl, normalizedCsvUrl)
        if (docId != null) {
            val discoveredGids = runCatching { discoverTabGids(docId) }.getOrDefault(emptyList())
            if (discoveredGids.size > 1) {
                var bestWeeks: List<ProgramSheetWeek> = emptyList()
                var bestScore = -1
                var lastAccessError: SheetAccessException? = null
                for (gid in discoveredGids.take(MAX_TABS_TO_SCAN)) {
                    val tabCsvUrl = "https://docs.google.com/spreadsheets/d/$docId/export?format=csv&gid=$gid"
                    try {
                        val parsed = ProgramSheetCsvParser.parse(downloadCsv(tabCsvUrl)).weeks
                        val score = scoreWeeks(parsed)
                        if (score > bestScore) {
                            bestScore = score
                            bestWeeks = parsed
                        }
                    } catch (e: SheetAccessException) {
                        lastAccessError = e
                    } catch (_: IOException) {
                        // Try remaining tabs
                    }
                }
                if (bestWeeks.isNotEmpty()) return bestWeeks
                if (lastAccessError != null) throw lastAccessError
            }
        }
        val csvText = downloadCsv(normalizedCsvUrl)
        return ProgramSheetCsvParser.parse(csvText).weeks
    }

    private fun discoverTabGids(docId: String): List<String> {
        val html = downloadText("https://docs.google.com/spreadsheets/d/$docId/htmlview")
        return BodyMetricsRepository.extractTabGidsFromHtmlView(html)
    }

    private fun scoreWeeks(weeks: List<ProgramSheetWeek>): Int {
        val exercises = weeks.sumOf { w -> w.days.sumOf { d -> d.exercises.size } }
        val weighted = weeks.sumOf { w -> w.days.sumOf { d -> d.exercises.count { it.weightKg.isNotBlank() } } }
        return exercises * 2 + weighted + weeks.size * 5
    }

    private class SheetAccessException(message: String) : IOException(message)

    private fun downloadText(initialUrl: String): String {
        var currentUrl = initialUrl
        var redirects = 0
        while (redirects <= MAX_REDIRECTS) {
            val connection = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("User-Agent", "HevyntAndroid/1.0")
            }
            try {
                val code = connection.responseCode
                if (code in 300..399) {
                    val location = connection.getHeaderField("Location") ?: return ""
                    currentUrl = URL(URL(currentUrl), location).toString()
                    redirects++
                    continue
                }
                if (code !in 200..299) return ""
                return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } finally {
                connection.disconnect()
            }
        }
        return ""
    }

    private fun downloadCsv(initialUrl: String): String {
        var currentUrl = initialUrl
        var redirects = 0
        while (redirects <= MAX_REDIRECTS) {
            val connection = (URL(currentUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                requestMethod = "GET"
                setRequestProperty("Accept", "text/csv, text/plain;q=0.9, */*;q=0.8")
                setRequestProperty("User-Agent", "HevyntAndroid/1.0")
            }
            try {
                val code = connection.responseCode
                if (code in 300..399) {
                    val location = connection.getHeaderField("Location")
                        ?: throw SheetAccessException("Redirect from Google Sheets had no destination.")
                    currentUrl = URL(URL(currentUrl), location).toString()
                    redirects++
                    continue
                }
                if (code == HttpURLConnection.HTTP_UNAUTHORIZED || code == HttpURLConnection.HTTP_FORBIDDEN) {
                    throw SheetAccessException(
                        "This Google Sheet is private. Set sharing to \"Anyone with the link can view\" or use File → Share → Publish to web (CSV).",
                    )
                }
                if (code == HttpURLConnection.HTTP_NOT_FOUND) {
                    throw SheetAccessException("Google Sheet not found (404). Double-check the URL you pasted.")
                }
                if (code !in 200..299) {
                    throw SheetAccessException("Google Sheets returned HTTP $code.")
                }
                val contentType = connection.contentType.orEmpty().lowercase()
                val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val trimmedStart = body.trimStart().take(64).lowercase()
                if (contentType.contains("text/html") ||
                    trimmedStart.startsWith("<!doctype html") ||
                    trimmedStart.startsWith("<html")
                ) {
                    throw SheetAccessException(
                        "Google returned a sign-in page instead of CSV. Share the sheet as \"Anyone with the link can view\" or publish it to the web as CSV.",
                    )
                }
                return body
            } finally {
                connection.disconnect()
            }
        }
        throw SheetAccessException("Too many redirects while downloading Google Sheet.")
    }

    companion object {
        private const val PREFS_NAME = "program_sheet_prefs"
        private const val KEY_SHEET_URL = "sheet_url"
        private const val KEY_CACHED_WEEKS = "cached_weeks_json"
        private const val KEY_LAST_SYNCED_AT = "last_synced_at_millis"
        private const val KEY_ACTIVE_WEEK = "active_week_number"
        private const val TIMEOUT_MS = 10_000
        private const val MAX_REDIRECTS = 5
        private const val MAX_TABS_TO_SCAN = 6
        const val DEFAULT_STALE_AGE_MILLIS = 5 * 60_000L

        internal fun encodeWeeks(weeks: List<ProgramSheetWeek>): String {
            val array = JSONArray()
            for (week in weeks) {
                val weekObj = JSONObject()
                weekObj.put("w", week.weekNumber)
                weekObj.put("l", week.label)
                weekObj.put("d", week.isDeload)
                val daysArray = JSONArray()
                for (day in week.days) {
                    val dayObj = JSONObject()
                    dayObj.put("n", day.dayName)
                    val exArray = JSONArray()
                    for (ex in day.exercises) {
                        val exObj = JSONObject()
                        exObj.put("n", ex.name)
                        exObj.put("s", ex.sets)
                        exObj.put("r", ex.reps)
                        exObj.put("kg", ex.weightKg)
                        exObj.put("rs", ex.restSeconds)
                        exObj.put("nt", ex.notes)
                        exArray.put(exObj)
                    }
                    dayObj.put("e", exArray)
                    daysArray.put(dayObj)
                }
                weekObj.put("days", daysArray)
                array.put(weekObj)
            }
            return array.toString()
        }

        internal fun decodeWeeks(json: String?): List<ProgramSheetWeek> {
            if (json.isNullOrBlank()) return emptyList()
            return runCatching {
                val array = JSONArray(json)
                buildList(array.length()) {
                    for (i in 0 until array.length()) {
                        val weekObj = array.optJSONObject(i) ?: continue
                        val weekNumber = weekObj.optInt("w", -1).takeIf { it > 0 } ?: continue
                        val label = weekObj.optString("l", "W$weekNumber")
                        val isDeload = weekObj.optBoolean("d", false)
                        val daysArray = weekObj.optJSONArray("days") ?: JSONArray()
                        val days = buildList(daysArray.length()) {
                            for (j in 0 until daysArray.length()) {
                                val dayObj = daysArray.optJSONObject(j) ?: continue
                                val dayName = dayObj.optString("n").takeIf { it.isNotBlank() } ?: continue
                                val exArray = dayObj.optJSONArray("e") ?: JSONArray()
                                val exercises = buildList(exArray.length()) {
                                    for (k in 0 until exArray.length()) {
                                        val exObj = exArray.optJSONObject(k) ?: continue
                                        val name = exObj.optString("n").takeIf { it.isNotBlank() } ?: continue
                                        add(
                                            ProgramSheetExercise(
                                                name = name,
                                                sets = exObj.optInt("s", 3).coerceIn(1, 20),
                                                reps = exObj.optString("r", ""),
                                                weightKg = exObj.optString("kg", ""),
                                                restSeconds = exObj.optInt("rs", 150).coerceIn(0, 1800),
                                                notes = exObj.optString("nt", ""),
                                            ),
                                        )
                                    }
                                }
                                if (exercises.isNotEmpty()) {
                                    add(ProgramSheetDay(dayName = dayName, exercises = exercises))
                                }
                            }
                        }
                        if (days.isNotEmpty()) {
                            add(ProgramSheetWeek(weekNumber, label, isDeload, days))
                        }
                    }
                }.sortedBy { it.weekNumber }
            }.getOrDefault(emptyList())
        }
    }
}
