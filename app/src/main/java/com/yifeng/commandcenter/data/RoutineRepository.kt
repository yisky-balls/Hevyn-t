package com.yifeng.commandcenter.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class RoutineRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun loadFolders(): List<RoutineFolder> {
        val savedFolders = preferences.getString(KEY_FOLDERS, null)
        if (savedFolders != null) {
            return runCatching { decodeFolders(savedFolders) }.getOrElse { defaultFolders() }
        }

        val legacyRoutines = preferences.getString(KEY_ROUTINES, null)?.let { raw ->
            runCatching { decodeRoutines(JSONArray(raw)) }.getOrNull()
        }
        val migrated = if (legacyRoutines != null) {
            listOf(
                RoutineFolder(name = "2b2d1s"),
                RoutineFolder(name = "PPL UL", routines = legacyRoutines),
            )
        } else {
            defaultFolders()
        }
        saveFolders(migrated)
        return migrated
    }

    fun saveFolders(folders: List<RoutineFolder>) {
        val array = JSONArray()
        folders.forEach { folder ->
            array.put(
                JSONObject()
                    .put("id", folder.id)
                    .put("name", folder.name)
                    .put("routines", encodeRoutines(folder.routines))
            )
        }
        preferences.edit()
            .putString(KEY_FOLDERS, array.toString())
            .remove(KEY_ROUTINES)
            .apply()
    }

    private fun decodeFolders(raw: String): List<RoutineFolder> {
        val array = JSONArray(raw)
        return runCatching {
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.getJSONObject(index)
                    add(
                        RoutineFolder(
                            id = item.getString("id"),
                            name = item.getString("name"),
                            routines = decodeRoutines(item.getJSONArray("routines")),
                        )
                    )
                }
            }
        }.getOrElse { throw it }
    }

    private fun encodeRoutines(routines: List<Routine>): JSONArray {
        val array = JSONArray()
        routines.forEach { routine ->
            array.put(
                JSONObject()
                    .put("id", routine.id)
                    .put("name", routine.name)
                    .put("restSeconds", routine.restSeconds)
                    .put("exercises", JSONArray(routine.exercises))
            )
        }
        return array
    }

    private fun decodeRoutines(array: JSONArray): List<Routine> = buildList {
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            val exerciseArray = item.getJSONArray("exercises")
            add(
                Routine(
                    id = item.getString("id"),
                    name = item.getString("name"),
                    exercises = buildList {
                        for (exerciseIndex in 0 until exerciseArray.length()) {
                            add(exerciseArray.getString(exerciseIndex))
                        }
                    },
                    restSeconds = item.optInt("restSeconds", 180),
                )
            )
        }
    }

    fun loadWorkoutHistory(): List<LastWorkoutSummary> {
        preferences.getString(KEY_WORKOUT_HISTORY, null)?.let { raw ->
            return runCatching { decodeWorkoutHistory(JSONArray(raw)) }.getOrDefault(emptyList())
        }
        val legacy = loadLegacyLastWorkout() ?: return emptyList()
        saveWorkoutHistory(listOf(legacy))
        return listOf(legacy)
    }

    fun loadLastWorkout(): LastWorkoutSummary? =
        loadWorkoutHistory().maxByOrNull { it.completedAtMillis }

    private fun loadLegacyLastWorkout(): LastWorkoutSummary? {
        val name = preferences.getString(KEY_LAST_NAME, null) ?: return null
        return LastWorkoutSummary(
            name = name,
            completedAtMillis = preferences.getLong(KEY_LAST_DATE, 0L),
            completedSets = preferences.getInt(KEY_LAST_SETS, 0),
            volumeKg = preferences.getInt(KEY_LAST_VOLUME, 0),
        )
    }

    fun saveLastWorkout(summary: LastWorkoutSummary) {
        saveWorkoutHistory(loadWorkoutHistory() + summary)
    }

    private fun saveWorkoutHistory(history: List<LastWorkoutSummary>) {
        val array = JSONArray().apply {
            history.forEach { summary ->
                put(JSONObject()
                    .put("name", summary.name)
                    .put("completedAtMillis", summary.completedAtMillis)
                    .put("completedSets", summary.completedSets)
                    .put("volumeKg", summary.volumeKg))
            }
        }
        preferences.edit()
            .putString(KEY_WORKOUT_HISTORY, array.toString())
            .remove(KEY_LAST_NAME)
            .remove(KEY_LAST_DATE)
            .remove(KEY_LAST_SETS)
            .remove(KEY_LAST_VOLUME)
            .apply()
    }

    private fun decodeWorkoutHistory(array: JSONArray): List<LastWorkoutSummary> = buildList {
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            add(LastWorkoutSummary(
                name = item.getString("name"),
                completedAtMillis = item.getLong("completedAtMillis"),
                completedSets = item.optInt("completedSets"),
                volumeKg = item.optInt("volumeKg"),
            ))
        }
    }

    private fun defaultFolders() = listOf(
        RoutineFolder(name = "2b2d1s"),
        RoutineFolder(
            name = "PPL UL",
            routines = listOf(
                Routine(
                    name = "Upper",
                    exercises = listOf(
                        "Bench Press - Wide Grip (Barbell)",
                        "Incline Bench Press (Dumbbell)",
                        "Lat Pulldown (Cable)",
                        "Seated Shoulder Press",
                    ),
                ),
                Routine(
                    name = "Leg",
                    exercises = listOf(
                        "Squat (Barbell)",
                        "Leg Extension (Machine)",
                        "Lying Leg Curl (Machine)",
                        "Cable Crunch",
                    ),
                    restSeconds = 150,
                ),
            ),
        ),
    )

    companion object {
        private const val PREFERENCES = "lift_programs"
        private const val KEY_FOLDERS = "routine_folders"
        private const val KEY_ROUTINES = "routines"
        private const val KEY_LAST_NAME = "last_name"
        private const val KEY_LAST_DATE = "last_date"
        private const val KEY_LAST_SETS = "last_sets"
        private const val KEY_LAST_VOLUME = "last_volume"
        private const val KEY_WORKOUT_HISTORY = "workout_history"
    }
}