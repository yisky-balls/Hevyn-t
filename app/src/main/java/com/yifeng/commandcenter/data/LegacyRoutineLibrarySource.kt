package com.yifeng.commandcenter.data

import android.content.Context
import android.content.SharedPreferences
import java.nio.charset.StandardCharsets
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

sealed interface LegacyRoutineLibraryResult {
    data class Ready(val folders: List<RoutineFolder>) : LegacyRoutineLibraryResult
    data class Corrupt(val cause: Throwable) : LegacyRoutineLibraryResult
}

fun interface LegacyRoutineLibrarySource {
    fun read(): LegacyRoutineLibraryResult
}

internal class SharedPreferencesRoutineLibrarySource(
    private val preferences: SharedPreferences,
) : LegacyRoutineLibrarySource {
    constructor(context: Context) : this(
        (context.applicationContext ?: context)
            .getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE),
    )

    override fun read(): LegacyRoutineLibraryResult {
        preferences.getString(KEY_FOLDERS, null)?.let { raw ->
            return decodeResult { decodeFolders(raw) }
        }
        preferences.getString(KEY_ROUTINES, null)?.let { raw ->
            return decodeResult {
                listOf(
                    RoutineFolder(id = stableId("folder:2b2d1s"), name = "2b2d1s"),
                    RoutineFolder(
                        id = stableId("folder:ppl-ul"),
                        name = "PPL UL",
                        routines = decodeRoutines(JSONArray(raw)),
                    ),
                )
            }
        }
        return LegacyRoutineLibraryResult.Ready(defaultRoutineFolders())
    }

    private fun decodeResult(block: () -> List<RoutineFolder>): LegacyRoutineLibraryResult =
        try {
            LegacyRoutineLibraryResult.Ready(block())
        } catch (error: Throwable) {
            LegacyRoutineLibraryResult.Corrupt(error)
        }

    private fun decodeFolders(raw: String): List<RoutineFolder> {
        val array = JSONArray(raw)
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.getJSONObject(index)
                add(
                    RoutineFolder(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        routines = decodeRoutines(item.getJSONArray("routines")),
                    ),
                )
            }
        }
    }

    private fun decodeRoutines(array: JSONArray): List<Routine> = buildList {
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            val exerciseArray = item.getJSONArray("exercises")
            val legacyRestSeconds = item.optInt("restSeconds", 180)
            add(
                Routine(
                    id = item.getString("id"),
                    name = item.getString("name"),
                    exercises = buildList {
                        for (exerciseIndex in 0 until exerciseArray.length()) {
                            val rawExercise = exerciseArray.get(exerciseIndex)
                            add(
                                if (rawExercise is JSONObject) {
                                    RoutineExercise(
                                        id = rawExercise.optString("id").ifBlank {
                                            stableId("legacy-exercise:$index:$exerciseIndex:${rawExercise}")
                                        },
                                        name = rawExercise.optString("name"),
                                        notes = rawExercise.optString("notes"),
                                        restSeconds = rawExercise.optInt("restSeconds", legacyRestSeconds),
                                        sets = rawExercise.optJSONArray("sets")
                                            ?.let(::decodeSets)
                                            ?: defaultSets(),
                                    )
                                } else {
                                    RoutineExercise(
                                        id = stableId("legacy-exercise:$index:$exerciseIndex:$rawExercise"),
                                        name = rawExercise.toString(),
                                        restSeconds = legacyRestSeconds,
                                    )
                                },
                            )
                        }
                    },
                ),
            )
        }
    }

    private fun decodeSets(array: JSONArray): List<WorkoutSet> = buildList {
        for (index in 0 until array.length()) {
            val set = array.getJSONObject(index)
            add(
                WorkoutSet(
                    id = set.getString("id"),
                    weight = set.optString("weight"),
                    reps = set.optString("reps"),
                    completed = false,
                ),
            )
        }
    }

    private companion object {
        const val PREFERENCES = "lift_programs"
        const val KEY_FOLDERS = "routine_folders"
        const val KEY_ROUTINES = "routines"
    }
}

internal fun defaultRoutineFolders(): List<RoutineFolder> = listOf(
    RoutineFolder(id = stableId("folder:2b2d1s"), name = "2b2d1s"),
    RoutineFolder(
        id = stableId("folder:ppl-ul"),
        name = "PPL UL",
        routines = listOf(
            Routine(
                id = stableId("routine:upper"),
                name = "Upper",
                exercises = listOf(
                    defaultRoutineExercise("Bench Press - Wide Grip (Barbell)"),
                    defaultRoutineExercise("Incline Bench Press (Dumbbell)"),
                    defaultRoutineExercise("Lat Pulldown (Cable)"),
                    defaultRoutineExercise("Seated Shoulder Press"),
                ),
            ),
            Routine(
                id = stableId("routine:leg"),
                name = "Leg",
                exercises = listOf(
                    defaultRoutineExercise("Squat (Barbell)", 150),
                    defaultRoutineExercise("Leg Extension (Machine)", 150),
                    defaultRoutineExercise("Lying Leg Curl (Machine)", 150),
                    defaultRoutineExercise("Cable Crunch", 150),
                ),
            ),
        ),
    ),
)

private fun defaultRoutineExercise(name: String, restSeconds: Int = 180) = RoutineExercise(
    id = stableId("exercise:$name"),
    name = name,
    restSeconds = restSeconds,
    sets = List(3) { position -> WorkoutSet(id = stableId("set:$name:$position")) },
)

private fun stableId(value: String): String = UUID.nameUUIDFromBytes(
    "hevynt-routine-library:$value".toByteArray(StandardCharsets.UTF_8),
).toString()