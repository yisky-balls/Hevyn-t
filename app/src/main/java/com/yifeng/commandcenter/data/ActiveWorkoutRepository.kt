package com.yifeng.commandcenter.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class ActiveWorkoutRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun load(): ActiveWorkout? = preferences.getString(KEY_WORKOUT, null)?.let { raw ->
        runCatching { decode(JSONObject(raw)) }.getOrNull()
    }

    fun save(workout: ActiveWorkout) {
        synchronized(WORKOUT_LOCK) { saveLocked(workout) }
    }

    fun update(transform: (ActiveWorkout) -> ActiveWorkout?): ActiveWorkout? =
        synchronized(WORKOUT_LOCK) {
            val updated = transform(load() ?: return@synchronized null) ?: return@synchronized null
            saveLocked(updated)
            updated
        }

    fun clear() {
        synchronized(WORKOUT_LOCK) {
            preferences.edit()
                .remove(KEY_WORKOUT)
                .remove(KEY_EXPANDED)
                .putLong(KEY_REVISION, System.nanoTime())
                .apply()
        }
    }

    fun revision(): Long = preferences.getLong(KEY_REVISION, 0L)

    fun isExpanded(): Boolean = preferences.getBoolean(KEY_EXPANDED, true)

    fun setExpanded(expanded: Boolean) {
        preferences.edit().putBoolean(KEY_EXPANDED, expanded).apply()
    }

    fun completeSet(exerciseId: String, setId: String): SetCompletionResult? {
        return synchronized(WORKOUT_LOCK) {
            val result = ActiveWorkoutOperations.completeSet(load() ?: return@synchronized null, exerciseId, setId)
                ?: return@synchronized null
            saveLocked(result.workout)
            result
        }
    }

    private fun saveLocked(workout: ActiveWorkout) {
        preferences.edit()
            .putString(KEY_WORKOUT, encode(workout).toString())
            .putLong(KEY_REVISION, System.nanoTime())
            .apply()
    }

    internal fun encode(workout: ActiveWorkout): JSONObject = JSONObject()
        .put("routineName", workout.routineName)
        .put("startedAtMillis", workout.startedAtMillis)
        .put("exercises", JSONArray().apply {
            workout.exercises.forEach { exercise ->
                put(JSONObject()
                    .put("id", exercise.id)
                    .put("name", exercise.name)
                    .put("notes", exercise.notes)
                     .put("restSeconds", exercise.restSeconds)
                    .put("sets", JSONArray().apply {
                        exercise.sets.forEach { set ->
                            put(JSONObject()
                                .put("id", set.id)
                                .put("weight", set.weight)
                                .put("reps", set.reps)
                                .put("completed", set.completed))
                        }
                    }))
            }
        })

    internal fun decode(json: JSONObject): ActiveWorkout {
        val exercises = json.getJSONArray("exercises")
        val legacyRestSeconds = json.optInt("restSeconds", 180)
        return ActiveWorkout(
            routineName = json.getString("routineName"),
            startedAtMillis = json.getLong("startedAtMillis"),
            exercises = buildList {
                for (exerciseIndex in 0 until exercises.length()) {
                    val exercise = exercises.getJSONObject(exerciseIndex)
                    val sets = exercise.getJSONArray("sets")
                    add(ActiveExercise(
                        id = exercise.getString("id"),
                        name = exercise.getString("name"),
                        notes = exercise.optString("notes"),
                        restSeconds = exercise.optInt("restSeconds", legacyRestSeconds),
                        sets = buildList {
                            for (setIndex in 0 until sets.length()) {
                                val set = sets.getJSONObject(setIndex)
                                add(WorkoutSet(
                                    id = set.getString("id"),
                                    weight = set.optString("weight"),
                                    reps = set.optString("reps"),
                                    completed = set.optBoolean("completed"),
                                ))
                            }
                        },
                    ))
                }
            },
        )
    }

    private companion object {
        val WORKOUT_LOCK = Any()
        const val PREFERENCES = "active_workout"
        const val KEY_WORKOUT = "workout"
        const val KEY_REVISION = "revision"
        const val KEY_EXPANDED = "expanded"
    }
}