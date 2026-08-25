package com.yifeng.commandcenter.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.yifeng.commandcenter.MainActivity
import com.yifeng.commandcenter.R
import com.yifeng.commandcenter.data.ActiveWorkoutRepository
import com.yifeng.commandcenter.history.PreviousSetResolver
import com.yifeng.commandcenter.history.WorkoutHistoryRepository
import com.yifeng.commandcenter.timer.RestTimerManager
import com.yifeng.commandcenter.timer.RestTimerReceiver
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

private const val RECEIVER_TIMEOUT_MILLIS = 9_000L

private fun BroadcastReceiver.launchAsync(
    context: Context,
    block: suspend (Context) -> Unit,
) {
    val appContext = context.applicationContext ?: context
    val pendingResult = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        try {
            withTimeout(RECEIVER_TIMEOUT_MILLIS) {
                block(appContext)
            }
        } catch (_: Exception) {
            // Broadcast work is best-effort and bounded by the receiver lifetime.
        } finally {
            pendingResult.finish()
        }
    }
}

abstract class BaseWorkoutWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        launchAsync(context) { appContext ->
            WorkoutWidgetUpdateDispatcher.refreshNow(appContext)
        }
    }
}

class CompactWorkoutWidgetProvider : BaseWorkoutWidgetProvider()

class MainWorkoutWidgetProvider : BaseWorkoutWidgetProvider()

class WorkoutWidgetUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_REFRESH) return
        val generation = intent.getLongExtra(EXTRA_GENERATION, 0L)
        if (generation <= 0L) return
        launchAsync(context) { appContext ->
            WorkoutWidgetUpdateDispatcher.refreshRequested(appContext, generation)
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.yifeng.commandcenter.widget.REFRESH"
        const val EXTRA_GENERATION = "refresh_generation"
    }
}

object WorkoutWidgetUpdateDispatcher {
    private const val COALESCE_MILLIS = 250L
    private val latestGeneration = AtomicLong(0L)
    private val refreshMutex = Mutex()

    fun request(context: Context) {
        val appContext = context.applicationContext ?: context
        try {
            val manager = AppWidgetManager.getInstance(appContext)
            val hasWidgets = manager.getAppWidgetIds(
                ComponentName(appContext, CompactWorkoutWidgetProvider::class.java),
            ).isNotEmpty() || manager.getAppWidgetIds(
                ComponentName(appContext, MainWorkoutWidgetProvider::class.java),
            ).isNotEmpty()
            if (!hasWidgets) return
            val generation = nextGeneration()
            appContext.sendBroadcast(
                Intent(appContext, WorkoutWidgetUpdateReceiver::class.java)
                    .setAction(WorkoutWidgetUpdateReceiver.ACTION_REFRESH)
                    .putExtra(WorkoutWidgetUpdateReceiver.EXTRA_GENERATION, generation),
            )
        } catch (_: Exception) {
            // State mutations remain successful even if the launcher cannot be notified.
        }
    }

    internal suspend fun refreshRequested(context: Context, generation: Long) {
        val appContext = context.applicationContext ?: context
        recordGeneration(generation)
        delay(COALESCE_MILLIS)
        refreshMutex.withLock {
            if (generation != latestGeneration.get()) return
            WorkoutWidgetUpdater.updateAll(appContext)
        }
    }

    internal suspend fun refreshNow(context: Context) {
        val appContext = context.applicationContext ?: context
        refreshMutex.withLock {
            WorkoutWidgetUpdater.updateAll(appContext)
        }
    }

    private fun nextGeneration(): Long {
        while (true) {
            val current = latestGeneration.get()
            val candidate = maxOf(SystemClock.elapsedRealtimeNanos(), current + 1L)
            if (latestGeneration.compareAndSet(current, candidate)) return candidate
        }
    }

    private fun recordGeneration(generation: Long) {
        while (true) {
            val current = latestGeneration.get()
            if (generation <= current || latestGeneration.compareAndSet(current, generation)) return
        }
    }
}

internal object WorkoutWidgetUpdater {
    suspend fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        val compactIds = manager.getAppWidgetIds(
            ComponentName(context, CompactWorkoutWidgetProvider::class.java),
        )
        val mainIds = manager.getAppWidgetIds(
            ComponentName(context, MainWorkoutWidgetProvider::class.java),
        )
        if (compactIds.isEmpty() && mainIds.isEmpty()) return
        val state = WorkoutWidgetStateLoader.load(context)
        compactIds.forEach { manager.updateAppWidget(it, compact(context, state)) }
        mainIds.forEach { manager.updateAppWidget(it, main(context, state)) }
    }

    private fun compact(context: Context, state: WorkoutWidgetState): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_workout_compact).apply {
            bindRoot(context)
            setVisible(R.id.widget_compact_idle, state is WorkoutWidgetState.Idle)
            setVisible(R.id.widget_compact_active, state is WorkoutWidgetState.Active)
            setVisible(R.id.widget_compact_rest, state is WorkoutWidgetState.Rest)
            when (state) {
                is WorkoutWidgetState.Idle -> {
                    setTextViewText(
                        R.id.widget_compact_idle_title,
                        state.lastWorkout?.session?.routineName ?: "NO WORKOUT YET",
                    )
                    setTextViewText(R.id.widget_compact_idle_action, "OPEN WORKOUT")
                    setOnClickPendingIntent(R.id.widget_compact_idle_action, launchApp(context))
                }
                is WorkoutWidgetState.Active -> {
                    val target = state.target
                    setTextViewText(
                        R.id.widget_compact_active_exercise,
                        target?.exerciseName ?: state.workout.routineName,
                    )
                    setTextViewText(
                        R.id.widget_compact_active_load,
                        target?.let(::loadDescription) ?: "ALL SETS COMPLETE",
                    )
                    if (target == null) {
                        setViewVisibility(R.id.widget_compact_complete, View.GONE)
                    } else {
                        setViewVisibility(R.id.widget_compact_complete, View.VISIBLE)
                        setOnClickPendingIntent(
                            R.id.widget_compact_complete,
                            completeSet(context, target.exerciseId, target.setId),
                        )
                    }
                }
                is WorkoutWidgetState.Rest -> bindCompactRest(context, state.timer)
            }
        }

    private fun main(context: Context, state: WorkoutWidgetState): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_workout_main).apply {
            bindRoot(context)
            setVisible(R.id.widget_main_idle, state is WorkoutWidgetState.Idle)
            setVisible(R.id.widget_main_active, state is WorkoutWidgetState.Active)
            setVisible(R.id.widget_main_rest, state is WorkoutWidgetState.Rest)
            when (state) {
                is WorkoutWidgetState.Idle -> {
                    val graph = state.lastWorkout
                    setTextViewText(
                        R.id.widget_main_idle_last,
                        graph?.session?.routineName?.let { "Last: $it" } ?: "No completed workout yet",
                    )
                    setTextViewText(
                        R.id.widget_main_idle_count,
                        graph?.exercises?.size?.let { "$it exercise${if (it == 1) "" else "s"}" }
                            ?: "Start in the app to create local history",
                    )
                    setTextViewText(R.id.widget_main_idle_action, "OPEN WORKOUT")
                    setOnClickPendingIntent(R.id.widget_main_idle_action, launchApp(context))
                }
                is WorkoutWidgetState.Active -> {
                    val target = state.target
                    setTextViewText(R.id.widget_main_active_routine, state.workout.routineName)
                    setTextViewText(
                        R.id.widget_main_active_progress,
                        target?.let { "${it.exerciseIndex + 1} / ${state.workout.exercises.size}" } ?: "DONE",
                    )
                    setTextViewText(
                        R.id.widget_main_active_exercise,
                        target?.exerciseName ?: "ALL SETS COMPLETE",
                    )
                    setTextViewText(
                        R.id.widget_main_active_set,
                        target?.let { "Set ${it.setNumber}" } ?: "Finish in the app",
                    )
                    setTextViewText(
                        R.id.widget_main_active_load,
                        target?.let(::loadDescription) ?: "Workout ready to finish",
                    )
                    setTextViewText(
                        R.id.widget_main_active_previous,
                        "Previous: ${PreviousSetResolver.format(state.previous)}",
                    )
                    if (target == null) {
                        setViewVisibility(R.id.widget_main_complete, View.GONE)
                    } else {
                        setViewVisibility(R.id.widget_main_complete, View.VISIBLE)
                        setOnClickPendingIntent(
                            R.id.widget_main_complete,
                            completeSet(context, target.exerciseId, target.setId),
                        )
                    }
                }
                is WorkoutWidgetState.Rest -> bindMainRest(context, state.timer)
            }
        }

    private fun RemoteViews.bindCompactRest(context: Context, timer: com.yifeng.commandcenter.timer.RestTimerState) {
        bindChronometer(R.id.widget_compact_rest_timer, timer.endMillis)
        bindTimerActions(
            context,
            subtractViewId = R.id.widget_compact_subtract,
            skipViewId = R.id.widget_compact_skip,
            addViewId = R.id.widget_compact_add,
        )
    }

    private fun RemoteViews.bindMainRest(context: Context, timer: com.yifeng.commandcenter.timer.RestTimerState) {
        bindChronometer(R.id.widget_main_rest_timer, timer.endMillis)
        val next = timer.nextTarget
        setTextViewText(
            R.id.widget_main_rest_next,
            next?.let { "${it.exerciseName} · Set ${it.setNumber}" } ?: "Final rest",
        )
        setTextViewText(
            R.id.widget_main_rest_load,
            next?.let(::loadDescription) ?: "Finish workout in the app",
        )
        bindTimerActions(
            context,
            subtractViewId = R.id.widget_main_subtract,
            skipViewId = R.id.widget_main_skip,
            addViewId = R.id.widget_main_add,
        )
    }

    private fun RemoteViews.bindRoot(context: Context) {
        setOnClickPendingIntent(R.id.widget_root, launchApp(context))
    }

    private fun RemoteViews.bindChronometer(viewId: Int, endMillis: Long) {
        val elapsedEnd = SystemClock.elapsedRealtime() +
            (endMillis - System.currentTimeMillis()).coerceAtLeast(0L)
        setChronometer(viewId, elapsedEnd, null, true)
        setChronometerCountDown(viewId, true)
    }

    private fun RemoteViews.bindTimerActions(
        context: Context,
        subtractViewId: Int,
        skipViewId: Int,
        addViewId: Int,
    ) {
        setOnClickPendingIntent(
            subtractViewId,
            timerAction(context, RestTimerManager.ACTION_SUBTRACT, 710),
        )
        setOnClickPendingIntent(
            skipViewId,
            timerAction(context, RestTimerManager.ACTION_SKIP, 711),
        )
        setOnClickPendingIntent(
            addViewId,
            timerAction(context, RestTimerManager.ACTION_ADD, 712),
        )
    }

    private fun RemoteViews.setVisible(viewId: Int, visible: Boolean) {
        setViewVisibility(viewId, if (visible) View.VISIBLE else View.GONE)
    }

    private fun loadDescription(target: com.yifeng.commandcenter.data.NextSetTarget): String = when {
        target.weight.isNotBlank() && target.reps.isNotBlank() ->
            "${target.weight} kg × ${target.reps}"
        target.reps.isNotBlank() -> "${target.reps} reps"
        target.weight.isNotBlank() -> "${target.weight} kg"
        else -> "ENTER LOAD IN APP"
    }

    private fun launchApp(context: Context): PendingIntent = PendingIntent.getActivity(
        context,
        700,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun completeSet(
        context: Context,
        exerciseId: String,
        setId: String,
    ): PendingIntent = PendingIntent.getBroadcast(
        context,
        setId.hashCode(),
        Intent(context, RestTimerReceiver::class.java)
            .setAction(RestTimerManager.ACTION_COMPLETE_SET)
            .setData(
                Uri.Builder()
                    .scheme(context.packageName)
                    .authority("complete-set")
                    .appendPath(exerciseId)
                    .appendPath(setId)
                    .build(),
            )
            .putExtra(RestTimerManager.EXTRA_EXERCISE_ID, exerciseId)
            .putExtra(RestTimerManager.EXTRA_SET_ID, setId),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun timerAction(context: Context, action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, RestTimerReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}