package com.yifeng.commandcenter.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.SizeF
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
import com.yifeng.commandcenter.history.WorkoutHistoryRepository
import com.yifeng.commandcenter.timer.RestTimerManager
import java.util.Locale
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

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        launchAsync(context) { appContext ->
            WorkoutWidgetUpdateDispatcher.refreshNow(appContext)
        }
    }
}

class CompactWorkoutWidgetProvider : BaseWorkoutWidgetProvider()

class MainWorkoutWidgetProvider : BaseWorkoutWidgetProvider()

class FullWorkoutWidgetProvider : BaseWorkoutWidgetProvider()

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
            ACTION_START_NEXT_WORKOUT -> {
                launchAsync(context) { appContext ->
                    val routineRepo = RoutineRepository(appContext)
                    routineRepo.initialize()
                    val historyRepo = WorkoutHistoryRepository(appContext)
                    val history = historyRepo.sessions()
                    val summaries = routineRepo.summaries()
                    val recommendation = LikelyNextRoutineResolver.resolve(
                        folders = summaries,
                        history = history,
                    ).recommendation
                    val allRoutines = summaries.flatMap { it.routines }
                    val routineSummary = recommendation?.routine
                        ?: allRoutines.firstOrNull { it.exerciseCount > 0 }
                        ?: allRoutines.firstOrNull()

                    android.util.Log.i(TAG, "ACTION_START_NEXT_WORKOUT resolved routine=${routineSummary?.name} id=${routineSummary?.id}")

                    if (routineSummary != null && routineSummary.exerciseCount > 0) {
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
                        android.util.Log.e(TAG, "No non-empty routineSummary available to start!")
                    }
                }
            }
            ACTION_COMPLETE_SET -> {
                launchAsync(context) { appContext ->
                    val repository = ActiveWorkoutRepository(appContext)
                    val workout = repository.loadSession()
                        ?.takeIf { it.routineBuildTarget == null }
                        ?.workout
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
                        val updated = repository.updateLiveWorkout { workout ->
                            val target = WorkoutProgress.firstUnfinishedSet(workout) ?: return@updateLiveWorkout null
                            ActiveWorkoutOperations.updateSet(
                                workout,
                                target.exerciseId,
                                target.setId,
                            ) { set ->
                                ActiveWorkoutOperations.adjustWeight(set, delta)
                            }
                        }
                        if (updated != null) {
                            WorkoutWidgetUpdateDispatcher.refreshNow(appContext)
                        }
                    }
                }
            }
            ACTION_ADJUST_REPS -> {
                val delta = intent.getIntExtra(EXTRA_DELTA_REPS, 0)
                if (delta != 0) {
                    launchAsync(context) { appContext ->
                        val repository = ActiveWorkoutRepository(appContext)
                        val updated = repository.updateLiveWorkout { workout ->
                            val target = WorkoutProgress.firstUnfinishedSet(workout) ?: return@updateLiveWorkout null
                            ActiveWorkoutOperations.updateSet(
                                workout,
                                target.exerciseId,
                                target.setId,
                            ) { set ->
                                ActiveWorkoutOperations.adjustReps(set, delta)
                            }
                        }
                        if (updated != null) {
                            WorkoutWidgetUpdateDispatcher.refreshNow(appContext)
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
            ACTION_ADD_REST_30 -> {
                launchAsync(context) { appContext ->
                    RestTimerManager.addRestSeconds(appContext, 30)
                    WorkoutWidgetUpdateDispatcher.refreshNow(appContext)
                }
            }
        }
    }

    companion object {
        const val ACTION_REFRESH = "com.yifeng.commandcenter.widget.REFRESH"
        const val ACTION_START_NEXT_WORKOUT = "com.yifeng.commandcenter.widget.START_NEXT_WORKOUT"
        const val ACTION_COMPLETE_SET = "com.yifeng.commandcenter.widget.COMPLETE_SET"
        const val ACTION_ADJUST_WEIGHT = "com.yifeng.commandcenter.widget.ADJUST_WEIGHT"
        const val ACTION_ADJUST_REPS = "com.yifeng.commandcenter.widget.ADJUST_REPS"
        const val ACTION_SKIP_REST = "com.yifeng.commandcenter.widget.SKIP_REST"
        const val ACTION_ADD_REST_30 = "com.yifeng.commandcenter.widget.ADD_REST_30"

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
            ).isNotEmpty() || manager.getAppWidgetIds(
                ComponentName(appContext, FullWorkoutWidgetProvider::class.java),
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
    com.yifeng.commandcenter.data.cleanRoutineName(name)

internal object WorkoutWidgetCountdownTicker {
    private var job: kotlinx.coroutines.Job? = null
    @Volatile
    private var targetEndMillis: Long = 0L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Synchronized
    fun start(context: Context, endMillis: Long) {
        targetEndMillis = endMillis
        if (job?.isActive == true) return
        val appContext = context.applicationContext ?: context
        job = scope.launch {
            while (System.currentTimeMillis() < targetEndMillis) {
                val remaining = targetEndMillis - System.currentTimeMillis()
                if (remaining <= 0) break
                val delayMillis = (remaining % 1000L).let { if (it == 0L) 1000L else it }
                delay(delayMillis.coerceIn(200L, 1000L))
                WorkoutWidgetUpdater.updateAll(appContext)
            }
            WorkoutWidgetUpdater.updateAll(appContext)
        }
    }

    @Synchronized
    fun stop() {
        targetEndMillis = 0L
        job?.cancel()
        job = null
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
        val fullIds = manager.getAppWidgetIds(
            ComponentName(context, FullWorkoutWidgetProvider::class.java),
        )
        if (compactIds.isEmpty() && mainIds.isEmpty() && fullIds.isEmpty()) return
        val state = WorkoutWidgetStateLoader.load(context)

        // Manage ticker lifecycle for active rest countdowns
        if (state is WorkoutWidgetState.Active && state.isRestActive && state.restTimerEndMillis != null) {
            WorkoutWidgetCountdownTicker.start(context, state.restTimerEndMillis)
        } else {
            WorkoutWidgetCountdownTicker.stop()
        }

        val compactViews = compact(context, state)
        val mainCompactViews = mainCompact(context, state)
        val mainFullViews = mainFull(context, state)

        compactIds.forEach { manager.updateAppWidget(it, compactViews) }
        fullIds.forEach { manager.updateAppWidget(it, mainFullViews) }

        if (mainIds.isNotEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val responsiveViews = RemoteViews(
                    mapOf(
                        SizeF(200f, 100f) to mainCompactViews,
                        SizeF(200f, 210f) to mainFullViews,
                    ),
                )
                mainIds.forEach { manager.updateAppWidget(it, responsiveViews) }
            } else {
                mainIds.forEach { id ->
                    val options = manager.getAppWidgetOptions(id)
                    val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0)
                    val views = if (minHeight >= 210) mainFullViews else mainCompactViews
                    manager.updateAppWidget(id, views)
                }
            }
        }
    }

    private fun compact(context: Context, state: WorkoutWidgetState): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_workout_compact).apply {
            setVisible(R.id.widget_compact_idle, state is WorkoutWidgetState.Idle)
            setVisible(R.id.widget_compact_active, state is WorkoutWidgetState.Active)
            when (state) {
                is WorkoutWidgetState.Idle -> {
                    setOnClickPendingIntent(R.id.widget_compact_idle_play, startNextWorkout(context))
                    setOnClickPendingIntent(R.id.widget_compact_idle_info, openWorkout(context))
                }
                is WorkoutWidgetState.Active -> {
                    setTextViewText(R.id.widget_compact_active_exercise, state.currentExerciseName)
                    setTextViewText(
                        R.id.widget_compact_active_set_info,
                        "Set ${state.currentSetNumber} of ${state.totalSetsInExercise}",
                    )
                    val timerBitmap = if (state.isRestActive && state.restTimerEndMillis != null) {
                        val remaining = ((state.restTimerEndMillis - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
                        val mm = remaining / 60
                        val ss = remaining % 60
                        WidgetDotMatrixRenderer.renderText(
                            context = context,
                            text = String.format(Locale.US, "%02d:%02d", mm, ss),
                            textSizeSp = 22f,
                            textColor = 0xFF7C8CF8.toInt(),
                        )
                    } else {
                        WidgetDotMatrixRenderer.renderText(
                            context = context,
                            text = "READY",
                            textSizeSp = 20f,
                            textColor = 0xFFFFFFFF.toInt(),
                        )
                    }
                    setImageViewBitmap(R.id.widget_compact_active_timer_img, timerBitmap)
                    setOnClickPendingIntent(R.id.widget_compact_active_complete, completeSet(context))
                    setOnClickPendingIntent(R.id.widget_compact_active_info, continueWorkout(context))
                }
            }
        }

    private fun mainCompact(context: Context, state: WorkoutWidgetState): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_workout_main_compact).apply {
            setVisible(R.id.widget_main_compact_idle, state is WorkoutWidgetState.Idle)
            setVisible(R.id.widget_main_compact_active, state is WorkoutWidgetState.Active)

            when (state) {
                is WorkoutWidgetState.Idle -> {
                    setOnClickPendingIntent(R.id.widget_main_compact_idle_start_btn, startNextWorkout(context))
                    setOnClickPendingIntent(R.id.widget_main_compact_idle_scene, openWorkout(context))
                }
                is WorkoutWidgetState.Active -> {
                    setTextViewText(R.id.widget_main_compact_active_exercise, state.currentExerciseName)
                    setTextViewText(
                        R.id.widget_main_compact_active_set_info,
                        state.plannedSetSummary,
                    )

                    if (state.isRestActive && state.restTimerEndMillis != null) {
                        val remaining = ((state.restTimerEndMillis - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
                        val mm = remaining / 60
                        val ss = remaining % 60
                        val timerBitmap = WidgetDotMatrixRenderer.renderText(
                            context = context,
                            text = String.format(Locale.US, "%02d:%02d", mm, ss),
                            textSizeSp = 26f,
                            textColor = 0xFF7C8CF8.toInt(),
                        )
                        setImageViewBitmap(R.id.widget_main_compact_active_timer_img, timerBitmap)

                        val duration = state.restTimerDurationSeconds ?: 1
                        val progressPercent = ((remaining.toFloat() / duration.toFloat()) * 100).toInt().coerceIn(0, 100)
                        setProgressBar(R.id.widget_main_compact_active_progress, 100, progressPercent, false)

                        setTextViewText(R.id.widget_main_compact_active_btn_left, "Skip rest")
                        setOnClickPendingIntent(R.id.widget_main_compact_active_btn_left, skipRest(context))
                    } else {
                        val readyBitmap = WidgetDotMatrixRenderer.renderText(
                            context = context,
                            text = "READY",
                            textSizeSp = 22f,
                            textColor = 0xFFFFFFFF.toInt(),
                        )
                        setImageViewBitmap(R.id.widget_main_compact_active_timer_img, readyBitmap)
                        setProgressBar(R.id.widget_main_compact_active_progress, 100, 100, false)

                        setTextViewText(R.id.widget_main_compact_active_btn_left, "+30 s")
                        setOnClickPendingIntent(R.id.widget_main_compact_active_btn_left, addRest30(context))
                    }

                    setTextViewText(R.id.widget_main_compact_active_btn_right, "Complete set")
                    setOnClickPendingIntent(R.id.widget_main_compact_active_btn_right, completeSet(context))
                    setOnClickPendingIntent(R.id.widget_main_compact_active_header, continueWorkout(context))
                }
            }
        }

    private fun mainFull(context: Context, state: WorkoutWidgetState): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_workout_main_full).apply {
            setVisible(R.id.widget_main_full_idle, state is WorkoutWidgetState.Idle)
            setVisible(R.id.widget_main_full_active, state is WorkoutWidgetState.Active)

            when (state) {
                is WorkoutWidgetState.Idle -> {
                    setOnClickPendingIntent(R.id.widget_main_full_idle_start_btn, startNextWorkout(context))
                    setOnClickPendingIntent(R.id.widget_main_full_idle_scene, openWorkout(context))
                }
                is WorkoutWidgetState.Active -> {
                    setTextViewText(R.id.widget_main_full_active_exercise, state.currentExerciseName)
                    val cleanRoutine = cleanRoutineName(state.workout.routineName)
                    val elapsedSec = ((System.currentTimeMillis() - state.workout.startedAtMillis) / 1000).coerceAtLeast(0)
                    val em = elapsedSec / 60
                    val es = elapsedSec % 60
                    setTextViewText(R.id.widget_main_full_active_routine, "$cleanRoutine · " + String.format(Locale.US, "%02d:%02d", em, es))

                    setTextViewText(
                        R.id.widget_main_full_active_set_info,
                        "Set ${state.currentSetNumber} of ${state.totalSetsInExercise}",
                    )

                    if (state.isRestActive && state.restTimerEndMillis != null) {
                        val remaining = ((state.restTimerEndMillis - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
                        val mm = remaining / 60
                        val ss = remaining % 60
                        val timerBitmap = WidgetDotMatrixRenderer.renderText(
                            context = context,
                            text = String.format(Locale.US, "%02d:%02d", mm, ss),
                            textSizeSp = 36f,
                            textColor = 0xFF7C8CF8.toInt(),
                        )
                        setImageViewBitmap(R.id.widget_main_full_active_timer_img, timerBitmap)
                        setTextViewText(R.id.widget_main_full_active_rest_label, "Rest")

                        val duration = state.restTimerDurationSeconds ?: 1
                        val progressPercent = ((remaining.toFloat() / duration.toFloat()) * 100).toInt().coerceIn(0, 100)
                        setProgressBar(R.id.widget_main_full_active_progress, 100, progressPercent, false)

                        setTextViewText(R.id.widget_main_full_btn_left, "Skip rest")
                        setOnClickPendingIntent(R.id.widget_main_full_btn_left, skipRest(context))
                    } else {
                        val readyBitmap = WidgetDotMatrixRenderer.renderText(
                            context = context,
                            text = "READY",
                            textSizeSp = 30f,
                            textColor = 0xFFFFFFFF.toInt(),
                        )
                        setImageViewBitmap(R.id.widget_main_full_active_timer_img, readyBitmap)
                        setTextViewText(R.id.widget_main_full_active_rest_label, "")

                        setProgressBar(R.id.widget_main_full_active_progress, 100, 100, false)

                        setTextViewText(R.id.widget_main_full_btn_left, "+30 s")
                        setOnClickPendingIntent(R.id.widget_main_full_btn_left, addRest30(context))
                    }

                    setTextViewText(R.id.widget_weight_value, state.weightKg)
                    setTextViewText(R.id.widget_reps_value, state.reps)

                    setOnClickPendingIntent(R.id.widget_btn_weight_minus, adjustWeight(context, -2.5, 715))
                    setOnClickPendingIntent(R.id.widget_btn_weight_plus, adjustWeight(context, +2.5, 716))
                    setOnClickPendingIntent(R.id.widget_btn_reps_minus, adjustReps(context, -1, 717))
                    setOnClickPendingIntent(R.id.widget_btn_reps_plus, adjustReps(context, +1, 718))

                    setTextViewText(R.id.widget_main_full_btn_right, "Complete set")
                    setOnClickPendingIntent(R.id.widget_main_full_btn_right, completeSet(context))
                    setOnClickPendingIntent(R.id.widget_main_full_active_header, continueWorkout(context))
                }
            }
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

    private fun addRest30(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            724,
            Intent(context, WorkoutWidgetUpdateReceiver::class.java)
                .setAction(WorkoutWidgetUpdateReceiver.ACTION_ADD_REST_30),
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
