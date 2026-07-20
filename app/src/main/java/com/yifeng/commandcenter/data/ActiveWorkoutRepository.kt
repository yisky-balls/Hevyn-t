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
        preferences.edit()
            .putString(KEY_WORKOUT, encode(workout).toString())
            .putLong(KEY_REVISION, System.nanoTime())
            .apply()
    }

    fun clear() {
        preferences.edit()
            .remove(KEY_WORKOUT)
            .remove(KEY_EXPANDED)
            .putLong(KEY_REVISION, System.nanoTime())
            .apply()
    }

    fun revision(): Long = preferences.getLong(KEY_REVISION, 0L)

    fun isExpanded(): Boolean = preferences.getBoolean(KEY_EXPANDED, true)

    fun setExpanded(expanded: Boolean) {
        preferences.edit().putBoolean(KEY_EXPANDED, expanded).apply()
    }

    fun completeSet(exerciseId: String, setId: String): SetCompletionResult? {
        val result = ActiveWorkoutOperations.completeSet(load() ?: return null, exerciseId, setId)
            ?: return null
        save(result.workout)
        return result
    }

    internal fun encode(workout: ActiveWorkout): JSONObject = JSONObject()
        .put("routineName", workout.routineName)
        .put("startedAtMillis", workout.startedAtMillis)
        .put("restSeconds", workout.restSeconds)
        .put("exercises", JSONArray().apply {
            workout.exercises.forEach { exercise ->
                put(JSONObject()
                    .put("id", exercise.id)
                    .put("name", exercise.name)
                    .put("notes", exercise.notes)
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
        return ActiveWorkout(
            routineName = json.getString("routineName"),
            startedAtMillis = json.getLong("startedAtMillis"),
            restSeconds = json.getInt("restSeconds"),
            exercises = buildList {
                for (exerciseIndex in 0 until exercises.length()) {
                    val exercise = exercises.getJSONObject(exerciseIndex)
                    val sets = exercise.getJSONArray("sets")
                    add(ActiveExercise(
                        id = exercise.getString("id"),
                        name = exercise.getString("name"),
                        notes = exercise.optString("notes"),
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
        const val PREFERENCES = "active_workout"
        const val KEY_WORKOUT = "workout"
        const val KEY_REVISION = "revision"
        const val KEY_EXPANDED = "expanded"
    }
}