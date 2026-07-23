package com.yifeng.commandcenter.timer

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.yifeng.commandcenter.MainActivity
import com.yifeng.commandcenter.R
import com.yifeng.commandcenter.data.ActiveWorkout
import com.yifeng.commandcenter.data.ActiveWorkoutRepository
import com.yifeng.commandcenter.data.NextSetTarget
import com.yifeng.commandcenter.data.WorkoutProgress

data class RestTimerState(
    val endMillis: Long,
    val durationSeconds: Int,
    val completedExerciseId: String,
    val completedSetId: String,
    val exerciseName: String,
    val nextTarget: NextSetTarget?,
)

object RestTimerManager {
    const val ACTION_SKIP = "com.yifeng.commandcenter.timer.SKIP"
    const val ACTION_SUBTRACT = "com.yifeng.commandcenter.timer.SUBTRACT"
    const val ACTION_ADD = "com.yifeng.commandcenter.timer.ADD"
    const val ACTION_COMPLETE = "com.yifeng.commandcenter.timer.COMPLETE"
    const val ACTION_COMPLETE_SET = "com.yifeng.commandcenter.timer.COMPLETE_SET"
    const val EXTRA_EXERCISE_ID = "exercise_id"
    const val EXTRA_SET_ID = "set_id"
    const val EXTRA_EXPECTED_END_MILLIS = "expected_end_millis"

    private const val PREFERENCES = "rest_timer"
    private const val KEY_END = "end"
    private const val KEY_DURATION = "duration"
    private const val KEY_EXERCISE = "exercise"
    private const val KEY_COMPLETED_EXERCISE_ID = "completed_exercise_id"
    private const val KEY_COMPLETED_SET_ID = "completed_set_id"
    private const val KEY_NEXT_EXERCISE_ID = "next_exercise_id"
    private const val KEY_NEXT_SET_ID = "next_set_id"
    private const val KEY_NEXT_EXERCISE_INDEX = "next_exercise_index"
    private const val KEY_NEXT_SET_INDEX = "next_set_index"
    private const val KEY_NEXT_EXERCISE_NAME = "next_exercise_name"
    private const val KEY_NEXT_WEIGHT = "next_weight"
    private const val KEY_NEXT_REPS = "next_reps"
    private const val CHANNEL_ACTIVE = "active_rest_timer"
    private const val CHANNEL_COMPLETE = "completed_rest_timer"
    private const val ACTIVE_NOTIFICATION_ID = 41
    private const val COMPLETE_NOTIFICATION_ID = 42
    private const val ACTIVE_SET_NOTIFICATION_ID = 43

    fun start(
        context: Context,
        durationSeconds: Int,
        completedExerciseId: String,
        completedSetId: String,
        exerciseName: String,
    ) {
        NotificationManagerCompat.from(context).cancel(ACTIVE_SET_NOTIFICATION_ID)
        val endMillis = System.currentTimeMillis() + durationSeconds * 1_000L
        val state = RestTimerState(
            endMillis = endMillis,
            durationSeconds = durationSeconds,
            completedExerciseId = completedExerciseId,
            completedSetId = completedSetId,
            exerciseName = exerciseName,
            nextTarget = null,
        )
        saveState(context, state)
        scheduleCompletion(context, endMillis)
        showActiveNotification(context)
    }

    fun adjust(context: Context, deltaSeconds: Int) {
        val current = readStoredState(context) ?: return
        val now = System.currentTimeMillis()
        if (current.endMillis <= now) {
            complete(context)
            return
        }
        val adjustedEnd = TimerMath.adjustedRestEndTime(current.endMillis, now, deltaSeconds)
        if (adjustedEnd <= now) {
            complete(context)
            return
        }
        val adjustedDuration = (current.durationSeconds + deltaSeconds).coerceAtLeast(1)
        saveState(context, current.copy(endMillis = adjustedEnd, durationSeconds = adjustedDuration))
        scheduleCompletion(context, adjustedEnd)
        showActiveNotification(context)
    }

    fun skip(context: Context) {
        val state = readStoredState(context)
        val target = state?.let { resolveTarget(context, it) }
        clearState(context)
        cancelCompletion(context)
        NotificationManagerCompat.from(context).cancel(ACTIVE_NOTIFICATION_ID)
        NotificationManagerCompat.from(context).cancel(COMPLETE_NOTIFICATION_ID)
        target?.let { showActiveSetNotification(context, it) }
    }

    fun cancel(context: Context) {
        clearState(context)
        cancelCompletion(context)
        NotificationManagerCompat.from(context).cancel(ACTIVE_NOTIFICATION_ID)
        NotificationManagerCompat.from(context).cancel(COMPLETE_NOTIFICATION_ID)
        NotificationManagerCompat.from(context).cancel(ACTIVE_SET_NOTIFICATION_ID)
    }

    fun reconcileRemovedSet(context: Context, setId: String) {
        NotificationManagerCompat.from(context).cancel(ACTIVE_SET_NOTIFICATION_ID)
        val state = readStoredState(context) ?: return
        if (state.completedSetId == setId) {
            state.copy(completedSetId = "").also { saveState(context, it) }
        }
        refresh(context)
    }

    fun completeSet(context: Context, exerciseId: String?, setId: String?) {
        if (exerciseId.isNullOrBlank() || setId.isNullOrBlank()) return
        val repository = ActiveWorkoutRepository(context)
        val result = repository.completeSet(exerciseId, setId) ?: return
        NotificationManagerCompat.from(context).cancel(ACTIVE_SET_NOTIFICATION_ID)
        if (result.nextTarget == null) {
            clearState(context)
            cancelCompletion(context)
            NotificationManagerCompat.from(context).cancel(ACTIVE_NOTIFICATION_ID)
            showWorkoutCompleteNotification(context)
            return
        }
        start(
            context = context,
            durationSeconds = result.restSeconds,
            completedExerciseId = result.completedExerciseId,
            completedSetId = result.completedSetId,
            exerciseName = result.completedExerciseName,
        )
    }

    @SuppressLint("MissingPermission")
    fun complete(context: Context, expectedEndMillis: Long? = null) {
        val state = readStoredState(context) ?: return
        if (expectedEndMillis != null && state.endMillis != expectedEndMillis) return
        if (System.currentTimeMillis() < state.endMillis) return
        val target = resolveTarget(context, state)
        clearState(context)
        cancelCompletion(context)
        NotificationManagerCompat.from(context).cancel(ACTIVE_NOTIFICATION_ID)
        NotificationManagerCompat.from(context).cancel(COMPLETE_NOTIFICATION_ID)
        if (target != null) showActiveSetNotification(context, target, restComplete = true)
        else showWorkoutCompleteNotification(context)
    }

    fun refresh(context: Context) {
        val state = readStoredState(context) ?: return
        if (state.endMillis <= System.currentTimeMillis()) complete(context)
        else showActiveNotification(context)
    }

    fun getState(context: Context): RestTimerState? {
        val state = readStoredState(context) ?: return null
        if (state.endMillis <= System.currentTimeMillis()) {
            complete(context)
            return null
        }
        return state.copy(nextTarget = resolveTarget(context, state))
    }

    fun resolveState(state: RestTimerState, workout: ActiveWorkout): RestTimerState =
        state.copy(nextTarget = resolveTarget(workout, state))

    fun registerStateListener(context: Context, onChanged: (RestTimerState?) -> Unit): () -> Unit {
        val appContext = context.applicationContext
        val preferences = appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == KEY_END) onChanged(getState(appContext))
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        return { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    private fun readStoredState(context: Context): RestTimerState? {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val end = preferences.getLong(KEY_END, 0L)
        if (end == 0L) return null
        return RestTimerState(
            endMillis = end,
            durationSeconds = preferences.getInt(KEY_DURATION, 180),
            completedExerciseId = preferences.getString(KEY_COMPLETED_EXERCISE_ID, "").orEmpty(),
            completedSetId = preferences.getString(KEY_COMPLETED_SET_ID, "").orEmpty(),
            exerciseName = preferences.getString(KEY_EXERCISE, "exercise") ?: "exercise",
            nextTarget = readTarget(preferences),
        )
    }

    @SuppressLint("MissingPermission")
    private fun showActiveNotification(context: Context) {
        val storedState = readStoredState(context) ?: return
        val now = System.currentTimeMillis()
        if (storedState.endMillis <= now) {
            complete(context)
            return
        }
        val state = storedState.copy(nextTarget = resolveTarget(context, storedState))
        if (!canNotify(context)) return
        createChannels(context)
        val launchIntent = PendingIntent.getActivity(
            context,
            10,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ACTIVE)
            .setSmallIcon(R.drawable.ic_timer_notification)
            .setContentTitle(state.exerciseName)
            .setContentText(
                state.nextTarget?.let { "Next: ${it.exerciseName} • ${targetDescription(it)}" }
                    ?: "Final rest"
            )
            .setContentIntent(launchIntent)
            .setWhen(state.endMillis)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setTimeoutAfter((state.endMillis - now).coerceAtLeast(1L))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(action(context, ACTION_SKIP, 11, "Skip"))
            .addAction(action(context, ACTION_SUBTRACT, 12, "-15s"))
            .addAction(action(context, ACTION_ADD, 13, "+15s"))
            .build()
        NotificationManagerCompat.from(context).notify(ACTIVE_NOTIFICATION_ID, notification)
    }

    private fun action(
        context: Context,
        action: String,
        requestCode: Int,
        label: String,
    ): NotificationCompat.Action {
        val intent = Intent(context, RestTimerReceiver::class.java).setAction(action)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Action(R.drawable.ic_timer_notification, label, pendingIntent)
    }

    private fun saveState(context: Context, state: RestTimerState) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit {
            putLong(KEY_END, state.endMillis)
            putInt(KEY_DURATION, state.durationSeconds)
            putString(KEY_COMPLETED_EXERCISE_ID, state.completedExerciseId)
            putString(KEY_COMPLETED_SET_ID, state.completedSetId)
            putString(KEY_EXERCISE, state.exerciseName)
            writeTarget(this, null)
        }
    }

    private fun clearState(context: Context) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit {
            remove(KEY_END)
            remove(KEY_DURATION)
            remove(KEY_COMPLETED_EXERCISE_ID)
            remove(KEY_COMPLETED_SET_ID)
            remove(KEY_EXERCISE)
            remove(KEY_NEXT_EXERCISE_ID)
            remove(KEY_NEXT_SET_ID)
            remove(KEY_NEXT_EXERCISE_INDEX)
            remove(KEY_NEXT_SET_INDEX)
            remove(KEY_NEXT_EXERCISE_NAME)
            remove(KEY_NEXT_WEIGHT)
            remove(KEY_NEXT_REPS)
        }
    }

    @SuppressLint("MissingPermission")
    private fun showActiveSetNotification(
        context: Context,
        target: NextSetTarget,
        restComplete: Boolean = false,
    ) {
        if (!canNotify(context)) return
        createChannels(context)
        val launchIntent = PendingIntent.getActivity(
            context,
            30,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val completeIntent = Intent(context, RestTimerReceiver::class.java)
            .setAction(ACTION_COMPLETE_SET)
            .putExtra(EXTRA_EXERCISE_ID, target.exerciseId)
            .putExtra(EXTRA_SET_ID, target.setId)
        val completePendingIntent = PendingIntent.getBroadcast(
            context,
            target.setId.hashCode(),
            completeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(
            context,
            if (restComplete) CHANNEL_COMPLETE else CHANNEL_ACTIVE,
        )
            .setSmallIcon(R.drawable.ic_timer_notification)
            .setContentTitle(if (restComplete) "Rest complete — ${target.exerciseName}" else target.exerciseName)
            .setContentText("${targetDescription(target)} • Complete when finished")
            .setContentIntent(launchIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(
                NotificationCompat.Action(
                    R.drawable.ic_timer_notification,
                    "Complete set",
                    completePendingIntent,
                )
            )
            .build()
        NotificationManagerCompat.from(context).notify(ACTIVE_SET_NOTIFICATION_ID, notification)
    }

    @SuppressLint("MissingPermission")
    private fun showWorkoutCompleteNotification(context: Context) {
        if (!canNotify(context)) return
        createChannels(context)
        val launchIntent = PendingIntent.getActivity(
            context,
            40,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_COMPLETE)
            .setSmallIcon(R.drawable.ic_timer_notification)
            .setContentTitle("Workout sets complete")
            .setContentText("All sets in the active workout are complete.")
            .setContentIntent(launchIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        NotificationManagerCompat.from(context).notify(COMPLETE_NOTIFICATION_ID, notification)
    }

    private fun resolveTarget(context: Context, state: RestTimerState): NextSetTarget? {
        val workout = ActiveWorkoutRepository(context).load() ?: return null
        return resolveTarget(workout, state)
    }

    private fun resolveTarget(workout: ActiveWorkout, state: RestTimerState): NextSetTarget? {
        if (state.completedExerciseId.isNotBlank()) {
            return WorkoutProgress.nextSetAfter(
                workout = workout,
                completedExerciseId = state.completedExerciseId,
                completedSetId = state.completedSetId,
            )
        }
        val legacyTarget = state.nextTarget
        if (legacyTarget != null) {
            val exercise = workout.exercises.firstOrNull { it.id == legacyTarget.exerciseId }
            val set = exercise?.sets?.firstOrNull { it.id == legacyTarget.setId }
            if (exercise != null && set != null && !set.completed) {
                return WorkoutProgress.firstUnfinishedSet(
                    workout.copy(exercises = listOf(exercise) + workout.exercises.filterNot { it.id == exercise.id })
                )
            }
        }
        return WorkoutProgress.firstUnfinishedSet(workout)
    }

    private fun writeTarget(editor: SharedPreferences.Editor, target: NextSetTarget?) {
        if (target == null) {
            editor
                .remove(KEY_NEXT_EXERCISE_ID)
                .remove(KEY_NEXT_SET_ID)
                .remove(KEY_NEXT_EXERCISE_INDEX)
                .remove(KEY_NEXT_SET_INDEX)
                .remove(KEY_NEXT_EXERCISE_NAME)
                .remove(KEY_NEXT_WEIGHT)
                .remove(KEY_NEXT_REPS)
            return
        }
        editor
            .putString(KEY_NEXT_EXERCISE_ID, target.exerciseId)
            .putString(KEY_NEXT_SET_ID, target.setId)
            .putInt(KEY_NEXT_EXERCISE_INDEX, target.exerciseIndex)
            .putInt(KEY_NEXT_SET_INDEX, target.setIndex)
            .putString(KEY_NEXT_EXERCISE_NAME, target.exerciseName)
            .putString(KEY_NEXT_WEIGHT, target.weight)
            .putString(KEY_NEXT_REPS, target.reps)
    }

    private fun readTarget(preferences: SharedPreferences): NextSetTarget? {
        val exerciseId = preferences.getString(KEY_NEXT_EXERCISE_ID, null) ?: return null
        val setId = preferences.getString(KEY_NEXT_SET_ID, null) ?: return null
        return NextSetTarget(
            exerciseId = exerciseId,
            setId = setId,
            exerciseIndex = preferences.getInt(KEY_NEXT_EXERCISE_INDEX, 0),
            setIndex = preferences.getInt(KEY_NEXT_SET_INDEX, 0),
            exerciseName = preferences.getString(KEY_NEXT_EXERCISE_NAME, "Exercise") ?: "Exercise",
            weight = preferences.getString(KEY_NEXT_WEIGHT, "") ?: "",
            reps = preferences.getString(KEY_NEXT_REPS, "") ?: "",
        )
    }

    private fun targetDescription(target: NextSetTarget): String {
        val load = when {
            target.weight.isNotBlank() && target.reps.isNotBlank() ->
                " • ${target.weight} kg × ${target.reps} reps"
            target.reps.isNotBlank() -> " • ${target.reps} reps"
            else -> ""
        }
        return "Set ${target.setNumber}$load"
    }

    private fun scheduleCompletion(context: Context, endMillis: Long) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        alarmManager.cancel(completionIntent(context))
        val canScheduleExactly = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()
        if (canScheduleExactly) {
            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    endMillis,
                    completionIntent(context),
                )
                return
            } catch (_: SecurityException) {
                // The special access can be revoked between the permission check and this call.
            }
        }
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            endMillis,
            completionIntent(context),
        )
    }

    private fun cancelCompletion(context: Context) {
        context.getSystemService(AlarmManager::class.java).cancel(completionIntent(context))
    }

    private fun completionIntent(context: Context): PendingIntent {
        val endMillis = readStoredState(context)?.endMillis ?: 0L
        val intent = Intent(context, RestTimerReceiver::class.java)
            .setAction(ACTION_COMPLETE)
            .putExtra(EXTRA_EXPECTED_END_MILLIS, endMillis)
        return PendingIntent.getBroadcast(
            context,
            14,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ACTIVE, "Active rest timer", NotificationManager.IMPORTANCE_LOW)
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_COMPLETE, "Rest complete", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    private fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
}