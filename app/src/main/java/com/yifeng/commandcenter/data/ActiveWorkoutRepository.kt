package com.yifeng.commandcenter.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.yifeng.commandcenter.widget.WorkoutWidgetUpdateDispatcher
import java.nio.charset.StandardCharsets
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject

class ActiveWorkoutRepository(context: Context) {
    private val appContext = context.applicationContext ?: context
    private val preferences = appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
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

    fun beginFinishing(workout: ActiveWorkout): Boolean =
        synchronized(WORKOUT_LOCK) {
            val session = loadSession() ?: return@synchronized false
            if (session.workout != workout || finishingWorkoutId() != null) {
                return@synchronized false
            }
            preferences.edit()
                .putString(KEY_FINISHING_WORKOUT_ID, workout.id)
                .commit()
        }

    fun cancelFinishing(workoutId: String) {
        synchronized(WORKOUT_LOCK) {
            if (finishingWorkoutId() == workoutId) {
                preferences.edit()
                    .remove(KEY_FINISHING_WORKOUT_ID)
                    .commit()
            }
        }
    }

    fun cancelFinishingIfUnchanged(workoutId: String, expectedMarker: String): Boolean =
        synchronized(WORKOUT_LOCK) {
            if (expectedMarker != workoutId || finishingWorkoutId() != expectedMarker) {
                return@synchronized false
            }
            preferences.edit()
                .remove(KEY_FINISHING_WORKOUT_ID)
                .commit()
        }

    fun pendingFinishWorkoutId(): String? =
        synchronized(WORKOUT_LOCK) { finishingWorkoutId() }

    fun clearFinished(workoutId: String): Boolean =
        synchronized(WORKOUT_LOCK) {
            val session = loadSession()
            if (session?.workout?.id != workoutId || finishingWorkoutId() != workoutId) {
                return@synchronized false
            }
            val cleared = preferences.edit()
                .remove(KEY_SESSION)
                .remove(KEY_EXPANDED)
                .remove(KEY_FINISHING_WORKOUT_ID)
                .commit()
            if (cleared) {
                synchronized(cacheLock) { updateCache(null, null) }
                WorkoutWidgetUpdateDispatcher.request(appContext)
            }
            cleared
        }

    fun clearStaleFinishingMarker() {
        synchronized(WORKOUT_LOCK) {
            if (loadSession() == null && finishingWorkoutId() != null) {
                preferences.edit()
                    .remove(KEY_FINISHING_WORKOUT_ID)
                    .commit()
            }
        }
    }

    fun save(session: ActiveWorkoutSession) {
        synchronized(WORKOUT_LOCK) { saveLocked(session) }
    }

    fun startIfIdle(session: ActiveWorkoutSession): Boolean =
        synchronized(WORKOUT_LOCK) {
            if (finishingWorkoutId() != null || preferences.contains(KEY_SESSION)) {
                return@synchronized false
            }
            saveLocked(session)
            true
        }

    fun update(transform: (ActiveWorkout) -> ActiveWorkout?): ActiveWorkout? =
        synchronized(WORKOUT_LOCK) {
            if (finishingWorkoutId() != null) return@synchronized null
            val session = loadSession() ?: return@synchronized null
            val updated = transform(session.workout) ?: return@synchronized null
            saveLocked(session.copy(workout = updated))
            updated
        }

    fun completeSet(exerciseId: String, setId: String): SetCompletionResult? =
        synchronized(WORKOUT_LOCK) {
            if (finishingWorkoutId() != null) return@synchronized null
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
                remove(KEY_FINISHING_WORKOUT_ID)
            }
            WorkoutWidgetUpdateDispatcher.request(appContext)
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
        WorkoutWidgetUpdateDispatcher.request(appContext)
    }

    private fun finishingWorkoutId(): String? =
        preferences.getString(KEY_FINISHING_WORKOUT_ID, null)

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
        .put("id", session.workout.id)
        .put("sourceRoutineId", session.workout.sourceRoutineId ?: JSONObject.NULL)
        .put("routineName", session.workout.routineName)
        .put("startedAtMillis", session.workout.startedAtMillis)
        .put("exercises", JSONArray().apply {
            session.workout.exercises.forEach { exercise ->
                put(JSONObject()
                    .put("id", exercise.id)
                                .put(
                                    "sourceRoutineExerciseId",
                                    exercise.sourceRoutineExerciseId ?: JSONObject.NULL,
                                )
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
        val generatedWorkoutId = UUID.nameUUIDFromBytes(
            json.toString().toByteArray(StandardCharsets.UTF_8),
        ).toString()
        return ActiveWorkout(
            id = json.optString("id").ifBlank { generatedWorkoutId },
            sourceRoutineId = json.opt("sourceRoutineId")
                .takeUnless { value -> value == null || value == JSONObject.NULL }
                ?.toString()
                ?.takeIf(String::isNotBlank),
            routineName = json.getString("routineName"),
            startedAtMillis = json.getLong("startedAtMillis"),
            exercises = buildList {
                for (exerciseIndex in 0 until exercises.length()) {
                    val exercise = exercises.getJSONObject(exerciseIndex)
                    val sets = exercise.getJSONArray("sets")
                    add(ActiveExercise(
                        id = exercise.getString("id"),
                        sourceRoutineExerciseId = if (exercise.has("sourceRoutineExerciseId")) {
                            exercise.opt("sourceRoutineExerciseId")
                                .takeUnless { value -> value == null || value == JSONObject.NULL }
                                ?.toString()
                                ?.takeIf(String::isNotBlank)
                        } else {
                            exercise.optString("id").takeIf(String::isNotBlank)
                        },
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
        const val KEY_FINISHING_WORKOUT_ID = "finishing_workout_id"
    }
}