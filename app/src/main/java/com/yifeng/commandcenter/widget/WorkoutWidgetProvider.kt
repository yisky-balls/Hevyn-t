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
import com.yifeng.commandcenter.data.ActiveWorkoutOperations
import com.yifeng.commandcenter.data.ActiveWorkoutRepository
import com.yifeng.commandcenter.data.ActiveWorkoutSession
import com.yifeng.commandcenter.data.LikelyNextRoutineResolver
import com.yifeng.commandcenter.data.RoutineRepository
import com.yifeng.commandcenter.data.RoutineWorkoutMapper
import com.yifeng.commandcenter.data.WorkoutProgress
import com.yifeng.commandcenter.glyph.GlyphDeviceSupport
import com.yifeng.commandcenter.glyph.GlyphPreferences
import com.yifeng.commandcenter.history.WorkoutHistoryRepository
import com.yifeng.commandcenter.timer.RestTimerManager
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
private const val TAG = "HevyntWidget"

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
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Error in launchAsync for receiver", e)
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
        android.util.Log.i(TAG, "onUpdate called for ${javaClass.simpleName} ids=${appWidgetIds.joinToString()}")
        launchAsync(context) { appContext ->
            WorkoutWidgetUpdateDispatcher.refreshNow(appContext)
        }
    }
}

class CompactWorkoutWidgetProvider : BaseWorkoutWidgetProvider()

class MainWorkoutWidgetProvider : BaseWorkoutWidgetProvider()

class WorkoutWidgetUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        android.util.Log.i(TAG, "WorkoutWidgetUpdateReceiver onReceive action=${intent.action}")
        when (intent.action) {
            ACTION_REFRESH -> {
                val generation = intent.getLongExtra(EXTRA_GENERATION, 0L)
                if (generation <= 0L) return
                launchAsync(context) { appContext ->
                    WorkoutWidgetUpdateDispatcher.refreshRequested(appContext, generation)
                }
            }
            ACTION_TOGGLE_GLYPH -> {
                val current = GlyphPreferences.isTimerEnabled(context)
                GlyphPreferences.setTimerEnabled(context, !current)
                launchAsync(context) { appContext ->
                    WorkoutWidgetUpdateDispatcher.refreshNow(appContext)
                }
            }
            ACTION_START_NEXT_WORKOUT -> {
                launchAsync(context) { appContext ->
                    val routineRepo = RoutineRepository(appContext)
                    routineRepo.initialize()
                    val historyRepo = WorkoutHistoryRepository(appContext)
                    val history = historyRepo.sessions()
                    val recommendation = LikelyNextRoutineResolver.resolve(
                        folders = routineRepo.summaries(),
                        history = history,
                    ).recommendation
                    val routineSummary = recommendation?.routine
                        ?: routineRepo.summaries().flatMap { it.routines }.firstOrNull()

                    android.util.Log.i(TAG, "ACTION_START_NEXT_WORKOUT resolved routine=${routineSummary?.name} id=${routineSummary?.id}")

                    if (routineSummary != null) {
                        val fullRoutine = routineRepo.routine(routineSummary.id)
                        if (fullRoutine != null) {
                            var workout = RoutineWorkoutMapper.fromRoutine(fullRoutine)
                            val previousSets = historyRepo.previousSets(workout)
                            val filledExercises = workout.exercises.map { exercise ->
                                exercise.copy(
                                    sets = exercise.sets.map { set ->
                                        previousSets[set.id]?.let { prev ->
                                            ActiveWorkoutOperations.copyPreviousSet(set, prev)
                                        } ?: set
                                    },
                                )
                            }
                            workout = workout.copy(exercises = filledExercises)
                            val started = ActiveWorkoutRepository(appContext).startIfIdle(ActiveWorkoutSession(workout))
                            android.util.Log.i(TAG, "ActiveWorkoutRepository.startIfIdle result=$started")
                            WorkoutWidgetUpdateDispatcher.refreshNow(appContext)
                        } else {
                            android.util.Log.e(TAG, "fullRoutine was null for routine id=${routineSummary.id}")
                        }
                    } else {
                        android.util.Log.e(TAG, "No routineSummary available to start!")
                    }
                }
            }
            ACTION_COMPLETE_SET -> {
                launchAsync(context) { appContext ->
                    val repository = ActiveWorkoutRepository(appContext)
                    val workout = repository.load()
                    if (workout != null) {
                        val target = WorkoutProgress.firstUnfinishedSet(workout)
                        if (target != null) {
                            RestTimerManager.completeSet(appContext, target.exerciseId, target.setId)
                            WorkoutWidgetUpdateDispatcher.refreshNow(appContext)
                        }
                    }
                }
            }
            ACTION_ADJUST_WEIGHT -> {
                val delta = intent.getDoubleExtra(EXTRA_DELTA_KG, 0.0)
                if (delta != 0.0) {
                    launchAsync(context) { appContext ->
                        val repository = ActiveWorkoutRepository(appContext)
                        val workout = repository.load()
                        if (workout != null) {
                            val target = WorkoutProgress.firstUnfinishedSet(workout)
                            if (target != null) {
                                val updated = ActiveWorkoutOperations.updateSet(
                                    workout,
                                    target.exerciseId,
                                    target.setId,
                                ) { set ->
                                    ActiveWorkoutOperations.adjustWeight(set, delta)
                                }
                                if (updated != null) {
                                    repository.save(ActiveWorkoutSession(updated))
                                    WorkoutWidgetUpdateDispatcher.refreshNow(appContext)
                                }
                            }
                        }
                    }
                }
            }
            ACTION_ADJUST_REPS -> {
                val delta = intent.getIntExtra(EXTRA_DELTA_REPS, 0)
                if (delta != 0) {
                    launchAsync(context) { appContext ->
                        val repository = ActiveWorkoutRepository(appContext)
                        val workout = repository.load()
                        if (workout != null) {
                            val target = WorkoutProgress.firstUnfinishedSet(workout)
                            if (target != null) {
                                val updated = ActiveWorkoutOperations.updateSet(
                                    workout,
                                    target.exerciseId,
                                    target.setId,
                                ) { set ->
                                    ActiveWorkoutOperations.adjustReps(set, delta)
                                }
                                if (updated != null) {
                                    repository.save(ActiveWorkoutSession(updated))
                                    WorkoutWidgetUpdateDispatcher.refreshNow(appContext)
                                }
                            }
                        }
                    }
                }
            }
            ACTION_SKIP_REST -> {
                launchAsync(context) { appContext ->
                    RestTimerManager.skip(appContext)
                    WorkoutWidgetUpdateDispatcher.refreshNow(appContext)
                }
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.yifeng.commandcenter.widget.REFRESH"
        const val ACTION_TOGGLE_GLYPH = "com.yifeng.commandcenter.widget.TOGGLE_GLYPH"
        const val ACTION_START_NEXT_WORKOUT = "com.yifeng.commandcenter.widget.START_NEXT_WORKOUT"
        const val ACTION_COMPLETE_SET = "com.yifeng.commandcenter.widget.COMPLETE_SET"
        const val ACTION_ADJUST_WEIGHT = "com.yifeng.commandcenter.widget.ADJUST_WEIGHT"
        const val ACTION_ADJUST_REPS = "com.yifeng.commandcenter.widget.ADJUST_REPS"
        const val ACTION_SKIP_REST = "com.yifeng.commandcenter.widget.SKIP_REST"

        const val EXTRA_GENERATION = "refresh_generation"
        const val EXTRA_DELTA_KG = "delta_kg"
        const val EXTRA_DELTA_REPS = "delta_reps"
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

internal fun cleanRoutineName(name: String): String =
    name.replace(Regex("^(MON|TUE|WED|THU|FRI|SAT|SUN)\\s*[-—–]\\s*", RegexOption.IGNORE_CASE), "").trim()

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
                    val cleanName = cleanRoutineName(state.nextRoutineName).uppercase()
                    val titleBitmap = WidgetDotMatrixRenderer.renderText(
                        context = context,
                        text = cleanName,
                        textSizeSp = 22f,
                    )
                    setImageViewBitmap(R.id.widget_compact_idle_title_img, titleBitmap)
                    setViewVisibility(R.id.widget_compact_idle_title_img, View.VISIBLE)
                    setViewVisibility(R.id.widget_compact_idle_title, View.GONE)
                    setTextViewText(R.id.widget_compact_idle_title, cleanName)
                    setTextViewText(
                        R.id.widget_compact_idle_metadata,
                        "Next · ${state.exerciseCount} exercises",
                    )
                    val startAction = startNextWorkout(context)
                    setOnClickPendingIntent(R.id.widget_compact_idle_play, startAction)
                    setOnClickPendingIntent(R.id.widget_compact_idle_info, openWorkout(context))
                }
                is WorkoutWidgetState.Active -> {
                    setTextViewText(R.id.widget_compact_active_routine, state.currentExerciseName)
                    setTextViewText(
                        R.id.widget_compact_active_progress,
                        "Set ${state.currentSetNumber} of ${state.totalSetsInExercise} · ",
                    )
                    bindElapsedChronometer(R.id.widget_compact_active_timer, state.workout.startedAtMillis)
                    if (state.isRestActive) {
                        setTextViewText(R.id.widget_compact_continue, "Skip rest")
                        setOnClickPendingIntent(R.id.widget_compact_continue, skipRest(context))
                    } else {
                        setTextViewText(R.id.widget_compact_continue, "Complete set")
                        setOnClickPendingIntent(R.id.widget_compact_continue, completeSet(context))
                    }
                    setOnClickPendingIntent(R.id.widget_compact_active_info, continueWorkout(context))
                }
            }
        }

    private fun main(context: Context, state: WorkoutWidgetState): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_workout_main).apply {
            setVisible(R.id.widget_main_idle, state is WorkoutWidgetState.Idle)
            setVisible(R.id.widget_main_active, state is WorkoutWidgetState.Active)

            when (state) {
                is WorkoutWidgetState.Idle -> {
                    val cleanName = cleanRoutineName(state.nextRoutineName).uppercase()
                    val titleBitmap = WidgetDotMatrixRenderer.renderText(
                        context = context,
                        text = cleanName,
                        textSizeSp = 26f,
                    )
                    setImageViewBitmap(R.id.widget_main_idle_title_img, titleBitmap)
                    setViewVisibility(R.id.widget_main_idle_title_img, View.VISIBLE)
                    setViewVisibility(R.id.widget_main_idle_routine, View.GONE)
                    setTextViewText(R.id.widget_main_idle_routine, cleanName)
                    setTextViewText(
                        R.id.widget_main_idle_metadata,
                        "Next session · ${state.exerciseCount} exercises",
                    )

                    // 4x3 preview exercises list to fill the 4x3 height cleanly
                    val p1 = state.previewExercises.getOrNull(0) ?: ""
                    val p2 = state.previewExercises.getOrNull(1) ?: ""
                    val p3 = state.previewExercises.getOrNull(2) ?: ""
                    setTextViewText(R.id.widget_main_idle_preview_1, p1)
                    setTextViewText(R.id.widget_main_idle_preview_2, p2)
                    setTextViewText(R.id.widget_main_idle_preview_3, p3)
                    setViewVisibility(R.id.widget_main_idle_preview_1, if (p1.isNotBlank()) View.VISIBLE else View.GONE)
                    setViewVisibility(R.id.widget_main_idle_preview_2, if (p2.isNotBlank()) View.VISIBLE else View.GONE)
                    setViewVisibility(R.id.widget_main_idle_preview_3, if (p3.isNotBlank()) View.VISIBLE else View.GONE)

                    val startAction = startNextWorkout(context)
                    setOnClickPendingIntent(R.id.widget_main_idle_start_btn, startAction)
                    setOnClickPendingIntent(R.id.widget_main_idle_header, openWorkout(context))
                }
                is WorkoutWidgetState.Active -> {
                    setTextViewText(R.id.widget_main_active_exercise, state.currentExerciseName)
                    setTextViewText(
                        R.id.widget_main_active_set_info,
                        "Set ${state.currentSetNumber} of ${state.totalSetsInExercise}",
                    )
                    val cleanRoutine = cleanRoutineName(state.workout.routineName)
                    setTextViewText(R.id.widget_main_active_routine, "$cleanRoutine · ")
                    bindElapsedChronometer(R.id.widget_main_active_timer, state.workout.startedAtMillis)

                    if (state.isRestActive && state.restTimerEndMillis != null) {
                        bindCountdownChronometer(R.id.widget_main_rest_chronometer, state.restTimerEndMillis)
                        val remaining = ((state.restTimerEndMillis - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
                        val duration = state.restTimerDurationSeconds ?: 1
                        val progressPercent = ((remaining.toFloat() / duration.toFloat()) * 100).toInt().coerceIn(0, 100)
                        setProgressBar(R.id.widget_main_rest_progress, 100, progressPercent, false)
                        setTextViewText(R.id.widget_main_rest_label, "Rest")
                    } else {
                        setChronometer(R.id.widget_main_rest_chronometer, SystemClock.elapsedRealtime(), null, false)
                        setProgressBar(R.id.widget_main_rest_progress, 100, 100, false)
                        setTextViewText(R.id.widget_main_rest_label, "Set ready")
                    }

                    setTextViewText(R.id.widget_weight_value, state.weightKg)
                    setTextViewText(R.id.widget_reps_value, state.reps)

                    setOnClickPendingIntent(R.id.widget_btn_weight_minus, adjustWeight(context, -2.5, 715))
                    setOnClickPendingIntent(R.id.widget_btn_weight_plus, adjustWeight(context, +2.5, 716))
                    setOnClickPendingIntent(R.id.widget_btn_reps_minus, adjustReps(context, -1, 717))
                    setOnClickPendingIntent(R.id.widget_btn_reps_plus, adjustReps(context, +1, 718))

                    setOnClickPendingIntent(R.id.widget_btn_skip_rest, skipRest(context))
                    setOnClickPendingIntent(R.id.widget_btn_complete_set, completeSet(context))
                    setOnClickPendingIntent(R.id.widget_main_active_header, continueWorkout(context))
                }
            }
        }

    private fun RemoteViews.bindElapsedChronometer(viewId: Int, startedAtMillis: Long) {
        val elapsed = (System.currentTimeMillis() - startedAtMillis).coerceAtLeast(0L)
        setChronometer(viewId, SystemClock.elapsedRealtime() - elapsed, null, true)
        setChronometerCountDown(viewId, false)
    }

    private fun RemoteViews.bindCountdownChronometer(viewId: Int, endMillis: Long) {
        val remainingMillis = endMillis - System.currentTimeMillis()
        val base = SystemClock.elapsedRealtime() + remainingMillis
        setChronometer(viewId, base, null, true)
        setChronometerCountDown(viewId, true)
    }

    private fun adjustWeight(context: Context, deltaKg: Double, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, WorkoutWidgetUpdateReceiver::class.java)
                .setAction(WorkoutWidgetUpdateReceiver.ACTION_ADJUST_WEIGHT)
                .putExtra(WorkoutWidgetUpdateReceiver.EXTRA_DELTA_KG, deltaKg),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun adjustReps(context: Context, deltaReps: Int, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, WorkoutWidgetUpdateReceiver::class.java)
                .setAction(WorkoutWidgetUpdateReceiver.ACTION_ADJUST_REPS)
                .putExtra(WorkoutWidgetUpdateReceiver.EXTRA_DELTA_REPS, deltaReps),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun completeSet(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            720,
            Intent(context, WorkoutWidgetUpdateReceiver::class.java)
                .setAction(WorkoutWidgetUpdateReceiver.ACTION_COMPLETE_SET),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun skipRest(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            721,
            Intent(context, WorkoutWidgetUpdateReceiver::class.java)
                .setAction(WorkoutWidgetUpdateReceiver.ACTION_SKIP_REST),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun startNextWorkout(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            722,
            Intent(context, WorkoutWidgetUpdateReceiver::class.java)
                .setAction(WorkoutWidgetUpdateReceiver.ACTION_START_NEXT_WORKOUT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

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
