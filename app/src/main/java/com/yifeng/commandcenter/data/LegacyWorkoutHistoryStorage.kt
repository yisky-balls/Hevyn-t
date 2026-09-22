package com.yifeng.commandcenter.data

import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

class LegacyWorkoutHistoryStorage(
    private val legacyPreferences: SharedPreferences?,
) {
    /** Legacy history is read-only input for the independent history-v2 transactional import. */
    fun loadWorkoutHistory(): List<LastWorkoutSummary> {
        val preferences = legacyPreferences ?: return emptyList()
        preferences.getString(KEY_WORKOUT_HISTORY, null)?.let { raw ->
            return runCatching { decodeWorkoutHistory(JSONArray(raw)) }.getOrDefault(emptyList())
        }
        return loadLegacyLastWorkout(preferences)?.let(::listOf).orEmpty()
    }

    fun saveLastWorkout(summary: LastWorkoutSummary) {
        val preferences = checkNotNull(legacyPreferences) {
            "Legacy history preferences are unavailable in this repository."
        }
        val history = loadWorkoutHistory() + summary
        val array = JSONArray().apply {
            history.forEach { item ->
                put(
                    JSONObject()
                        .put("name", item.name)
                        .put("completedAtMillis", item.completedAtMillis)
                        .put("completedSets", item.completedSets)
                        .put("volumeKg", item.volumeKg),
                )
            }
        }
        preferences.edit {
            putString(KEY_WORKOUT_HISTORY, array.toString())
            remove(KEY_LAST_NAME)
            remove(KEY_LAST_DATE)
            remove(KEY_LAST_SETS)
            remove(KEY_LAST_VOLUME)
        }
    }

    private fun loadLegacyLastWorkout(preferences: SharedPreferences): LastWorkoutSummary? {
        val name = preferences.getString(KEY_LAST_NAME, null) ?: return null
        return LastWorkoutSummary(
            name = name,
            completedAtMillis = preferences.getLong(KEY_LAST_DATE, 0L),
            completedSets = preferences.getInt(KEY_LAST_SETS, 0),
            volumeKg = when (val value = preferences.all[KEY_LAST_VOLUME]) {
                is Float -> value.toDouble()
                is Int -> value.toDouble()
                is Long -> value.toDouble()
                else -> 0.0
            },
        )
    }

    private fun decodeWorkoutHistory(array: JSONArray): List<LastWorkoutSummary> = buildList {
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            add(
                LastWorkoutSummary(
                    name = item.getString("name"),
                    completedAtMillis = item.getLong("completedAtMillis"),
                    completedSets = item.optInt("completedSets"),
                    volumeKg = item.optDouble("volumeKg"),
                ),
            )
        }
    }

    internal companion object {
        const val PREFERENCES = "lift_programs"
        private const val KEY_LAST_NAME = "last_name"
        private const val KEY_LAST_DATE = "last_date"
        private const val KEY_LAST_SETS = "last_sets"
        private const val KEY_LAST_VOLUME = "last_volume"
        private const val KEY_WORKOUT_HISTORY = "workout_history"
    }
}
