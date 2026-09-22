package com.yifeng.commandcenter.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.yifeng.commandcenter.MainActivity
import com.yifeng.commandcenter.R
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
            setVisible(R.id.widget_compact_idle, state is WorkoutWidgetState.Idle)
            setVisible(R.id.widget_compact_active, state is WorkoutWidgetState.Active)
            when (state) {
                is WorkoutWidgetState.Idle -> {
                    val routine = state.recommendation?.routine
                    setTextViewText(R.id.widget_compact_idle_title, routine?.name ?: "CHOOSE A ROUTINE")
                    setTextViewText(
                        R.id.widget_compact_idle_metadata,
                        routine?.let(::routineMetadata) ?: "Open Hevynt to get started",
                    )
                    val action = openWorkout(context)
                    setTextViewText(R.id.widget_compact_idle_action, "OPEN")
                    setOnClickPendingIntent(R.id.widget_compact_idle_action, action)
                    setOnClickPendingIntent(R.id.widget_root, action)
                }
                is WorkoutWidgetState.Active -> {
                    setTextViewText(R.id.widget_compact_active_routine, state.workout.routineName)
                    setTextViewText(
                        R.id.widget_compact_active_progress,
                        "${state.completedSets} / ${state.totalSets} sets",
                    )
                    bindElapsedChronometer(R.id.widget_compact_active_timer, state.workout.startedAtMillis)
                    val action = continueWorkout(context)
                    setOnClickPendingIntent(R.id.widget_compact_continue, action)
                    setOnClickPendingIntent(R.id.widget_root, action)
                }
            }
        }

    private fun main(context: Context, state: WorkoutWidgetState): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_workout_main).apply {
            setVisible(R.id.widget_main_idle, state is WorkoutWidgetState.Idle)
            setVisible(R.id.widget_main_active, state is WorkoutWidgetState.Active)
            when (state) {
                is WorkoutWidgetState.Idle -> {
                    val routine = state.recommendation?.routine
                    setTextViewText(R.id.widget_main_idle_routine, routine?.name ?: "CHOOSE A ROUTINE")
                    setTextViewText(
                        R.id.widget_main_idle_metadata,
                        routine?.let(::routineMetadata) ?: "Open Hevynt to select a workout",
                    )
                    val action = openWorkout(context)
                    setTextViewText(R.id.widget_main_idle_action, "OPEN")
                    setOnClickPendingIntent(R.id.widget_main_idle_action, action)
                    setOnClickPendingIntent(R.id.widget_root, action)
                }
                is WorkoutWidgetState.Active -> {
                    setTextViewText(R.id.widget_main_active_routine, state.workout.routineName)
                    setTextViewText(
                        R.id.widget_main_active_progress,
                        "${state.completedSets} / ${state.totalSets} sets",
                    )
                    bindElapsedChronometer(R.id.widget_main_active_timer, state.workout.startedAtMillis)
                    val action = continueWorkout(context)
                    setOnClickPendingIntent(R.id.widget_main_continue, action)
                    setOnClickPendingIntent(R.id.widget_root, action)
                }
            }
        }

    private fun RemoteViews.bindElapsedChronometer(viewId: Int, startedAtMillis: Long) {
        val elapsed = (System.currentTimeMillis() - startedAtMillis).coerceAtLeast(0L)
        setChronometer(viewId, SystemClock.elapsedRealtime() - elapsed, null, true)
        setChronometerCountDown(viewId, false)
    }

    private fun routineMetadata(routine: com.yifeng.commandcenter.data.RoutineSummary): String {
        val count = routine.exerciseCount
        val age = routine.lastPerformedAtMillis?.let { completedAt ->
            val days = ((System.currentTimeMillis() - completedAt).coerceAtLeast(0L) / 86_400_000L)
            "last ${days}d ago"
        } ?: "not performed yet"
        return "$count exercise${if (count == 1) "" else "s"} · $age"
    }

    private fun openWorkout(context: Context): PendingIntent = activityIntent(
        context = context,
        requestCode = 703,
        intent = Intent(context, MainActivity::class.java)
            .setAction(MainActivity.ACTION_OPEN_WORKOUT),
    )

    private fun continueWorkout(context: Context): PendingIntent = activityIntent(
        context = context,
        requestCode = 702,
        intent = Intent(context, MainActivity::class.java)
            .setAction(MainActivity.ACTION_CONTINUE_WORKOUT),
    )

    private fun activityIntent(
        context: Context,
        requestCode: Int,
        intent: Intent,
    ): PendingIntent = PendingIntent.getActivity(
        context,
        requestCode,
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun RemoteViews.setVisible(viewId: Int, visible: Boolean) {
        setViewVisibility(viewId, if (visible) View.VISIBLE else View.GONE)
    }
}
