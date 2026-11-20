package com.yifeng.commandcenter.metrics

import android.content.Context
import android.content.SharedPreferences
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

sealed interface BodyMetricsSyncOutcome {
    data class Success(
        val entries: List<BodyMetricEntry>,
        val syncedAtMillis: Long,
    ) : BodyMetricsSyncOutcome

    data class Failure(val message: String) : BodyMetricsSyncOutcome
}

class BodyMetricsRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadCachedState(): BodyMetricsSyncState {
        val rawUrl = prefs.getString(KEY_SHEET_URL, "").orEmpty()
        val lastSynced = if (prefs.contains(KEY_LAST_SYNCED_AT)) {
            prefs.getLong(KEY_LAST_SYNCED_AT, 0L).takeIf { it > 0L }
        } else {
            null
        }
        val entries = decodeEntries(prefs.getString(KEY_CACHED_ENTRIES, null))
        return BodyMetricsSyncState(
            sheetUrl = rawUrl,
            entries = entries,
            lastSyncedAtMillis = lastSynced,
            isSyncing = false,
            lastError = null,
        )
    }

    fun saveSheetUrl(rawUrl: String) {
        prefs.edit().putString(KEY_SHEET_URL, rawUrl.trim()).apply()
    }

    fun clearConfiguration() {
        prefs.edit()
            .remove(KEY_SHEET_URL)
            .remove(KEY_CACHED_ENTRIES)
            .remove(KEY_LAST_SYNCED_AT)
            .apply()
    }

    fun isStale(lastSyncedAtMillis: Long?, nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (lastSyncedAtMillis == null) return true
        return (nowMillis - lastSyncedAtMillis) >= DEFAULT_STALE_AGE_MILLIS
    }

    suspend fun syncFromUrl(rawUrl: String): BodyMetricsSyncOutcome = withContext(Dispatchers.IO) {
        val normalized = when (val result = GoogleSheetsUrlNormalizer.normalizeToCsvUrl(rawUrl)) {
            is SheetUrlResult.Invalid -> return@withContext BodyMetricsSyncOutcome.Failure(result.reason)
            is SheetUrlResult.Valid -> result.csvUrl
        }

        val entries = try {
            fetchBestEntries(rawUrl, normalized)
        } catch (e: SheetAccessException) {
            return@withContext BodyMetricsSyncOutcome.Failure(e.message ?: "Unable to read Google Sheet.")
        } catch (e: IOException) {
            return@withContext BodyMetricsSyncOutcome.Failure(
                "Could not reach Google Sheets. Check your internet connection and try again.",
            )
        } catch (e: Exception) {
            return@withContext BodyMetricsSyncOutcome.Failure(
                e.message ?: "Unexpected error while syncing Google Sheet.",
            )
        }

        if (entries.isEmpty()) {
            return@withContext BodyMetricsSyncOutcome.Failure(
                "No valid weight or calorie rows found. Make sure the sheet has Date, Weight, and Calories columns.",
            )
        }

        val now = System.currentTimeMillis()
        prefs.edit()
            .putString(KEY_SHEET_URL, rawUrl.trim())
            .putString(KEY_CACHED_ENTRIES, encodeEntries(entries))
            .putLong(KEY_LAST_SYNCED_AT, now)
            .apply()

        BodyMetricsSyncOutcome.Success(entries = entries, syncedAtMillis = now)
    }

    private fun fetchBestEntries(rawUrl: String, normalizedCsvUrl: String): List<BodyMetricEntry> {
        val docId = extractDocIdWithoutGid(rawUrl, normalizedCsvUrl)
        if (docId != null) {
            val discoveredGids = runCatching { discoverTabGids(docId) }.getOrDefault(emptyList())
            if (discoveredGids.size > 1) {
                var bestEntries: List<BodyMetricEntry> = emptyList()
                var bestScore = -1
                var lastAccessError: SheetAccessException? = null
                for (gid in discoveredGids.take(MAX_TABS_TO_SCAN)) {
                    val tabCsvUrl = "https://docs.google.com/spreadsheets/d/$docId/export?format=csv&gid=$gid"
                    try {
                        val parsed = BodyMetricsCsvParser.parse(downloadCsv(tabCsvUrl)).entries
                        val score = scoreEntries(parsed)
                        if (score > bestScore) {
                            bestScore = score
                            bestEntries = parsed
                        }
                    } catch (e: SheetAccessException) {
                        lastAccessError = e
                    } catch (_: IOException) {
                        // Try remaining tabs
                    }
                }
                if (bestEntries.isNotEmpty()) return bestEntries
                if (lastAccessError != null) throw lastAccessError
            }
        }
        val csvText = downloadCsv(normalizedCsvUrl)
        return BodyMetricsCsvParser.parse(csvText).entries
    }

    private fun discoverTabGids(docId: String): List<String> {
        val html = downloadText("https://docs.google.com/spreadsheets/d/$docId/htmlview")
        return extractTabGidsFromHtmlView(html)
    }

    private fun scoreEntries(entries: List<BodyMetricEntry>): Int {
        val weightCount = entries.count { it.weightKg != null }
        val caloriesCount = entries.count { it.caloriesKcal != null }
        val bothBonus = if (weightCount > 0 && caloriesCount > 0) 10 else 0
        return entries.size * 2 + weightCount + caloriesCount + bothBonus
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
                    throw SheetAccessException(
                        "Google Sheet not found (404). Double-check the URL you pasted.",
                    )
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
        private const val PREFS_NAME = "body_metrics_prefs"
        private const val KEY_SHEET_URL = "sheet_url"
        private const val KEY_CACHED_ENTRIES = "cached_entries_json"
        private const val KEY_LAST_SYNCED_AT = "last_synced_at_millis"
        private const val TIMEOUT_MS = 10_000
        private const val MAX_REDIRECTS = 5
        private const val MAX_TABS_TO_SCAN = 6
        const val DEFAULT_STALE_AGE_MILLIS = 5 * 60_000L

        private val DOC_ID_NO_GID_REGEX = Regex("/spreadsheets/d/([a-zA-Z0-9-_]+)/export\\?format=csv$")
        private val HTMLVIEW_TAB_GID_REGEX = Regex("""items\.push\(\{[^}]*gid:\s*"(\d+)"""")

        internal fun extractDocIdWithoutGid(rawUrl: String, normalizedCsvUrl: String): String? {
            if (rawUrl.contains("gid=", ignoreCase = true)) return null
            return DOC_ID_NO_GID_REGEX.find(normalizedCsvUrl)?.groupValues?.getOrNull(1)
        }

        internal fun extractTabGidsFromHtmlView(html: String): List<String> =
            HTMLVIEW_TAB_GID_REGEX.findAll(html)
                .map { it.groupValues[1] }
                .distinct()
                .toList()

        internal fun encodeEntries(entries: List<BodyMetricEntry>): String {
            val array = JSONArray()
            for (entry in entries) {
                val obj = JSONObject()
                obj.put("d", entry.dateEpochDay)
                if (entry.weightKg != null) obj.put("w", entry.weightKg)
                if (entry.caloriesKcal != null) obj.put("c", entry.caloriesKcal)
                array.put(obj)
            }
            return array.toString()
        }

        internal fun decodeEntries(json: String?): List<BodyMetricEntry> {
            if (json.isNullOrBlank()) return emptyList()
            return runCatching {
                val array = JSONArray(json)
                buildList(array.length()) {
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i) ?: continue
                        if (!obj.has("d")) continue
                        val epochDay = obj.getLong("d")
                        val weight = if (obj.has("w") && !obj.isNull("w")) obj.getDouble("w") else null
                        val calories = if (obj.has("c") && !obj.isNull("c")) obj.getInt("c") else null
                        if (weight != null || calories != null) {
                            add(BodyMetricEntry(epochDay, weight, calories))
                        }
                    }
                }.sortedBy { it.dateEpochDay }
            }.getOrDefault(emptyList())
        }
    }
}
