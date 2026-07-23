package com.yifeng.commandcenter.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

class ActiveWorkoutRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val cacheLock = Any()
    private var cacheInitialized = false
    private var cachedRawSession: String? = null
    private var cachedSession: ActiveWorkoutSession? = null

    fun loadSession(): ActiveWorkoutSession? {
        val raw = preferences.getString(KEY_SESSION, null)
        return synchronized(cacheLock) {
            if (cacheInitialized && raw == cachedRawSession) return@synchronized cachedSession
            val decoded = raw?.let { runCatching { decodeSession(JSONObject(it)) }.getOrNull() }
            updateCache(raw, decoded)
            decoded
        }
    }

    fun load(): ActiveWorkout? = loadSession()?.workout

    fun save(session: ActiveWorkoutSession) {
        synchronized(WORKOUT_LOCK) { saveLocked(session) }
    }

    fun update(transform: (ActiveWorkout) -> ActiveWorkout?): ActiveWorkout? =
        synchronized(WORKOUT_LOCK) {
            val session = loadSession() ?: return@synchronized null
            val updated = transform(session.workout) ?: return@synchronized null
            saveLocked(session.copy(workout = updated))
            updated
        }

    fun completeSet(exerciseId: String, setId: String): SetCompletionResult? =
        synchronized(WORKOUT_LOCK) {
            val session = loadSession() ?: return@synchronized null
            val result = ActiveWorkoutOperations.completeSet(session.workout, exerciseId, setId)
                ?: return@synchronized null
            saveLocked(session.copy(workout = result.workout))
            result
        }

    fun clear() {
        synchronized(WORKOUT_LOCK) {
            synchronized(cacheLock) { updateCache(null, null) }
            preferences.edit {
                remove(KEY_SESSION)
                remove(KEY_EXPANDED)
            }
        }
    }

    fun isExpanded(): Boolean = preferences.getBoolean(KEY_EXPANDED, true)

    fun setExpanded(expanded: Boolean) {
        preferences.edit { putBoolean(KEY_EXPANDED, expanded) }
    }

    fun registerSessionListener(onChanged: (ActiveWorkoutSession?) -> Unit): () -> Unit {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_SESSION) onChanged(loadSession())
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        return { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    private fun saveLocked(session: ActiveWorkoutSession) {
        val raw = encode(session).toString()
        synchronized(cacheLock) { updateCache(raw, session) }
        preferences.edit { putString(KEY_SESSION, raw) }
    }

    private fun updateCache(raw: String?, session: ActiveWorkoutSession?) {
        cachedRawSession = raw
        cachedSession = session
        cacheInitialized = true
    }

    internal fun encode(session: ActiveWorkoutSession): JSONObject = JSONObject()
        .put("routineBuildTarget", session.routineBuildTarget?.let { target ->
            JSONObject()
                .put("folderId", target.folderId)
                .put("routineId", target.routineId ?: JSONObject.NULL)
        } ?: JSONObject.NULL)
        .put("routineName", session.workout.routineName)
        .put("startedAtMillis", session.workout.startedAtMillis)
        .put("exercises", JSONArray().apply {
            session.workout.exercises.forEach { exercise ->
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

    internal fun decodeSession(json: JSONObject): ActiveWorkoutSession {
        val target = json.optJSONObject("routineBuildTarget")?.let {
            RoutineBuildTarget(
                folderId = it.getString("folderId"),
                routineId = it.opt("routineId")
                    .takeUnless { value -> value == null || value == JSONObject.NULL }
                    ?.toString()
                    ?.takeIf(String::isNotBlank),
            )
        }
        return ActiveWorkoutSession(
            workout = decodeWorkout(json),
            routineBuildTarget = target,
        )
    }

    private fun decodeWorkout(json: JSONObject): ActiveWorkout {
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
        const val KEY_SESSION = "workout"
        const val KEY_EXPANDED = "expanded"
    }
}