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
import com.yifeng.commandcenter.MainActivity
import com.yifeng.commandcenter.R
import com.yifeng.commandcenter.data.ActiveWorkoutRepository
import com.yifeng.commandcenter.data.NextSetTarget

data class RestTimerState(
    val endMillis: Long,
    val durationSeconds: Int,
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

    private const val PREFERENCES = "rest_timer"
    private const val KEY_END = "end"
    private const val KEY_DURATION = "duration"
    private const val KEY_EXERCISE = "exercise"
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
        exerciseName: String,
        nextTarget: NextSetTarget?,
    ) {
        NotificationManagerCompat.from(context).cancel(ACTIVE_SET_NOTIFICATION_ID)
        val endMillis = System.currentTimeMillis() + durationSeconds * 1_000L
        saveState(context, RestTimerState(endMillis, durationSeconds, exerciseName, nextTarget))
        scheduleCompletion(context, endMillis)
        showActiveNotification(context)
    }

    fun adjust(context: Context, deltaSeconds: Int) {
        val current = getState(context) ?: return
        val now = System.currentTimeMillis()
        val adjustedEnd = TimerMath.adjustedRestEndTime(current.endMillis, now, deltaSeconds)
        if (adjustedEnd <= System.currentTimeMillis()) {
            complete(context)
            return
        }
        val adjustedDuration = (current.durationSeconds + deltaSeconds).coerceAtLeast(1)
        saveState(context, current.copy(endMillis = adjustedEnd, durationSeconds = adjustedDuration))
        scheduleCompletion(context, adjustedEnd)
        showActiveNotification(context)
    }

    fun skip(context: Context) {
        val state = getState(context)
        clearState(context)
        cancelCompletion(context)
        NotificationManagerCompat.from(context).cancel(ACTIVE_NOTIFICATION_ID)
        NotificationManagerCompat.from(context).cancel(COMPLETE_NOTIFICATION_ID)
        state?.nextTarget?.let { showActiveSetNotification(context, it) }
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
        val state = getState(context) ?: return
        if (state.nextTarget?.setId == setId) cancel(context)
    }

    fun completeSet(context: Context, exerciseId: String?, setId: String?) {
        if (exerciseId.isNullOrBlank() || setId.isNullOrBlank()) return
        val repository = ActiveWorkoutRepository(context)
        val result = repository.completeSet(exerciseId, setId) ?: return
        NotificationManagerCompat.from(context).cancel(ACTIVE_SET_NOTIFICATION_ID)
        start(
            context = context,
            durationSeconds = result.workout.restSeconds,
            exerciseName = result.completedExerciseName,
            nextTarget = result.nextTarget,
        )
    }

    @SuppressLint("MissingPermission")
    fun complete(context: Context) {
        val state = getState(context) ?: return
        if (!canNotify(context)) return
        createChannels(context)
        val launchIntent = PendingIntent.getActivity(
            context,
            20,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_COMPLETE)
            .setSmallIcon(R.drawable.ic_timer_notification)
            .setContentTitle("Rest complete — press Skip")
            .setContentText(
                state.nextTarget?.let { "${it.exerciseName} • ${targetDescription(it)}" }
                    ?: "Workout complete"
            )
            .setContentIntent(launchIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(action(context, ACTION_SKIP, 11, "Skip"))
            .addAction(action(context, ACTION_SUBTRACT, 12, "-15s"))
            .addAction(action(context, ACTION_ADD, 13, "+15s"))
            .build()
        NotificationManagerCompat.from(context).cancel(ACTIVE_NOTIFICATION_ID)
        NotificationManagerCompat.from(context).notify(COMPLETE_NOTIFICATION_ID, notification)
    }

    fun refresh(context: Context) {
        if (getState(context) != null) showActiveNotification(context)
    }

    fun getState(context: Context): RestTimerState? {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val end = preferences.getLong(KEY_END, 0L)
        if (end == 0L) return null
        return RestTimerState(
            endMillis = end,
            durationSeconds = preferences.getInt(KEY_DURATION, 180),
            exerciseName = preferences.getString(KEY_EXERCISE, "exercise") ?: "exercise",
            nextTarget = readTarget(preferences),
        )
    }

    @SuppressLint("MissingPermission")
    private fun showActiveNotification(context: Context) {
        val state = getState(context) ?: return
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
        val editor = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit()
            .putLong(KEY_END, state.endMillis)
            .putInt(KEY_DURATION, state.durationSeconds)
            .putString(KEY_EXERCISE, state.exerciseName)
        writeTarget(editor, state.nextTarget)
        editor.apply()
    }

    private fun clearState(context: Context) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit()
            .remove(KEY_END)
            .remove(KEY_DURATION)
            .remove(KEY_EXERCISE)
            .apply()
    }

    @SuppressLint("MissingPermission")
    private fun showActiveSetNotification(context: Context, target: NextSetTarget) {
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
        val notification = NotificationCompat.Builder(context, CHANNEL_ACTIVE)
            .setSmallIcon(R.drawable.ic_timer_notification)
            .setContentTitle(target.exerciseName)
            .setContentText(targetDescription(target))
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
        val intent = Intent(context, RestTimerReceiver::class.java).setAction(ACTION_COMPLETE)
        return PendingIntent.getBroadcast(
            context,
            14,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
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