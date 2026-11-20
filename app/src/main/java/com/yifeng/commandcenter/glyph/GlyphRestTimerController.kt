package com.yifeng.commandcenter.glyph

import android.content.ComponentName
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.nothing.ketchum.Glyph
import com.nothing.ketchum.GlyphException
import com.nothing.ketchum.GlyphFrame
import com.nothing.ketchum.GlyphManager
import com.yifeng.commandcenter.timer.RestTimerState

/**
 * Controls the Nothing Phone (4a) rear Glyph LED strip as a rest-timer
 * visual progression/filling bar.
 *
 * ## Architecture
 *
 * This controller is a **consumer** of the existing rest timer — it does NOT
 * create a second timer. The single source of truth remains [RestTimerState]
 * with its `endMillis` and exercise-specific `durationSeconds`.
 *
 * ## Phone (4a) Glyph layout & Physical Fill Order
 *
 * On the Nothing Phone (4a), the dedicated red recording LED square is
 * physically located at the **base (bottom)** of the vertical Glyph Bar.
 *
 * In Nothing's Phone (4a) SDK layout:
 * - A_6 is at the bottom (closest to the red LED).
 * - A_1 is at the top (furthest from the red LED).
 *
 * The bar begins **completely empty (0%)** and fills outwards from the
 * red recording LED toward the top of the phone:
 *
 * 1. A6 (index 5) — closest to the red indicator
 * 2. A5 (index 4)
 * 3. A4 (index 3)
 * 4. A3 (index 2)
 * 5. A2 (index 1)
 * 6. A1 (index 0) — furthest from the red indicator
 *
 * ## Six-Segment Progression
 *
 * | Elapsed % | Lit segments | Lit Zones                           |
 * |-----------|-------------|-------------------------------------|
 * | 0         | 0           | all off (START: completely empty)   |
 * | 1–16      | 1           | A6                                  |
 * | 17–33     | 2           | A6+A5                               |
 * | 34–50     | 3           | A6+A5+A4                            |
 * | 51–66     | 4           | A6+A5+A4+A3                         |
 * | 67–83     | 5           | A6+A5+A4+A3+A2                      |
 * | 84–100    | 6           | A6+A5+A4+A3+A2+A1 (DONE: full)      |
 *
 * SDK commands are ONLY sent when the segment count actually changes.
 */
class GlyphRestTimerController(private val context: Context) {

    companion object {
        private const val TAG = "GlyphTimer"

        /** Number of zones on the Phone (4a) Glyph strip. */
        const val ZONE_COUNT = 6

        /**
         * The explicit fill order starting from the segment physically closest
         * to the red LED indicator at the base of the Glyph Bar (A6) and
         * proceeding outward to the top (A1).
         */
        val FILL_ORDER = listOf(
            Glyph.Code_25111.A_6, // 0: bottom, closest to red recording LED
            Glyph.Code_25111.A_5, // 1
            Glyph.Code_25111.A_4, // 2
            Glyph.Code_25111.A_3, // 3
            Glyph.Code_25111.A_2, // 4
            Glyph.Code_25111.A_1, // 5: top, furthest from red recording LED
        )

        /** Duration of the completion pulse animation in milliseconds. */
        private const val PULSE_PERIOD_MS = 500

        /** Number of breathing cycles for the completion pulse. */
        private const val PULSE_CYCLES = 2

        /** Interval between pulse cycles in milliseconds. */
        private const val PULSE_INTERVAL_MS = 100

        /**
         * Maps an elapsed timer progress percentage (0–100) to the number of
         * illuminated segments (0–6).
         *
         * - 0% elapsed: 0 segments (empty at start)
         * - 1–16% elapsed: 1 segment (A6)
         * - 17–33% elapsed: 2 segments (A6+A5)
         * - 34–50% elapsed: 3 segments (A6+A5+A4)
         * - 51–66% elapsed: 4 segments (A6+A5+A4+A3)
         * - 67–83% elapsed: 5 segments (A6+A5+A4+A3+A2)
         * - 84–100% elapsed: 6 segments (A6+A5+A4+A3+A2+A1, full)
         */
        fun segmentCount(elapsedPercent: Int): Int = when {
            elapsedPercent >= 84 -> 6
            elapsedPercent >= 67 -> 5
            elapsedPercent >= 51 -> 4
            elapsedPercent >= 34 -> 3
            elapsedPercent >= 17 -> 2
            elapsedPercent >= 1  -> 1
            else                 -> 0
        }

        /**
         * Calculates the elapsed progress percentage (0–100) from remaining
         * time and total duration.
         *
         * 0% = timer just started (elapsed == 0)
         * 100% = timer finished (remaining == 0)
         */
        fun elapsedPercent(remainingMs: Long, totalMs: Long): Int {
            if (totalMs <= 0L) return 0
            val elapsedMs = (totalMs - remainingMs).coerceIn(0L, totalMs)
            return ((elapsedMs.toFloat() / totalMs.toFloat()) * 100f)
                .toInt()
                .coerceIn(0, 100)
        }
    }

    private var glyphManager: GlyphManager? = null
    @Volatile private var sessionOpen = false
    private var lastSegmentCount = -1
    @Volatile private var currentTimerState: RestTimerState? = null
    private val handler = Handler(Looper.getMainLooper())
    private var pulseOffRunnable: Runnable? = null

    /**
     * Initialise the Glyph SDK and open a session.
     * Safe to call multiple times; idempotent.
     */
    fun init() {
        if (glyphManager != null) return
        try {
            val gm = GlyphManager.getInstance(context.applicationContext)
            glyphManager = gm
            gm.init(object : GlyphManager.Callback {
                override fun onServiceConnected(componentName: ComponentName?) {
                    Log.d(TAG, "Glyph service connected")
                    try {
                        gm.register(Glyph.DEVICE_25111)
                        gm.openSession()
                        sessionOpen = true
                        Log.d(TAG, "Glyph session opened successfully")
                        // If a timer was started before connection finished, render it immediately
                        currentTimerState?.let { state ->
                            handler.post { renderTimer(state) }
                        }
                    } catch (e: GlyphException) {
                        Log.w(TAG, "Failed to open Glyph session: ${e.message}")
                    } catch (t: Throwable) {
                        Log.w(TAG, "Unexpected error opening Glyph session", t)
                    }
                }

                override fun onServiceDisconnected(componentName: ComponentName?) {
                    Log.d(TAG, "Glyph service disconnected")
                    sessionOpen = false
                }
            })
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to initialise GlyphManager", t)
            glyphManager = null
        }
    }

    /**
     * Update the Glyph strip to reflect the current timer state.
     *
     * @param state The current rest timer state, or `null` if no timer is active.
     */
    fun updateTimer(state: RestTimerState?) {
        cancelPendingPulseOff()
        currentTimerState = state
        if (!sessionOpen) return
        renderTimer(state)
    }

    /**
     * Renders the timer state to the Glyph strip.
     * Only communicates with the SDK when the lit segment count changes.
     */
    private fun renderTimer(state: RestTimerState?) {
        if (state == null) {
            if (lastSegmentCount != 0) {
                turnOff()
                lastSegmentCount = 0
            }
            return
        }

        val now = System.currentTimeMillis()
        val remainingMs = (state.endMillis - now).coerceAtLeast(0L)
        val totalMs = (state.durationSeconds * 1_000L).coerceAtLeast(1L)
        val percent = elapsedPercent(remainingMs, totalMs)
        val segments = segmentCount(percent)

        if (segments == lastSegmentCount) return // No change — skip SDK call

        lastSegmentCount = segments

        if (segments > 0) {
            showSegments(segments)
        } else {
            turnOff()
        }
    }

    /**
     * Called when the rest timer reaches zero.
     *
     * @param pulseEnabled Whether the pulse animation should play.
     */
    fun onTimerComplete(pulseEnabled: Boolean) {
        cancelPendingPulseOff()
        currentTimerState = null
        lastSegmentCount = 0
        if (!sessionOpen) return

        if (pulseEnabled) {
            pulseAllZones()
        } else {
            turnOff()
        }
    }

    /**
     * Shut down the Glyph SDK session.
     */
    fun shutdown() {
        cancelPendingPulseOff()
        currentTimerState = null
        lastSegmentCount = -1
        val gm = glyphManager ?: return
        try {
            if (sessionOpen) {
                gm.turnOff()
                gm.closeSession()
                sessionOpen = false
            }
        } catch (e: GlyphException) {
            Log.w(TAG, "Error closing Glyph session: ${e.message}")
        } catch (t: Throwable) {
            Log.w(TAG, "Unexpected error closing Glyph session", t)
        }
        try {
            gm.unInit()
        } catch (t: Throwable) {
            Log.w(TAG, "Error uninitialising GlyphManager", t)
        }
        glyphManager = null
    }

    // ---- Private helpers ------------------------------------------------

    /**
     * Illuminate the first [count] zones according to [FILL_ORDER]
     * (starting closest to the red indicator: A6, then outward to A1).
     */
    private fun showSegments(count: Int) {
        val gm = glyphManager ?: return
        try {
            if (count <= 0) {
                turnOff()
                return
            }
            val builder = gm.glyphFrameBuilder ?: return
            for (i in 0 until count.coerceAtMost(ZONE_COUNT)) {
                builder.buildChannel(FILL_ORDER[i])
            }
            val frame = builder.build()
            gm.toggle(frame)
            Log.d(TAG, "Glyph showing $count segment(s) in fill order: ${FILL_ORDER.take(count)}")
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to update Glyph segments", t)
        }
    }

    /**
     * Play a brief pulse animation across all six zones, then turn off.
     */
    private fun pulseAllZones() {
        val gm = glyphManager ?: return
        try {
            val builder = gm.glyphFrameBuilder ?: return
            for (channel in FILL_ORDER) {
                builder.buildChannel(channel)
            }
            builder.buildPeriod(PULSE_PERIOD_MS)
            builder.buildCycles(PULSE_CYCLES)
            builder.buildInterval(PULSE_INTERVAL_MS)
            val frame = builder.build()
            gm.animate(frame)
            Log.d(TAG, "Glyph completion pulse started")

            val animDuration = PULSE_CYCLES.toLong() * (PULSE_PERIOD_MS + PULSE_INTERVAL_MS) + 200L
            cancelPendingPulseOff()
            val runnable = Runnable {
                pulseOffRunnable = null
                if (currentTimerState == null) {
                    turnOff()
                }
            }
            pulseOffRunnable = runnable
            handler.postDelayed(runnable, animDuration)
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to play Glyph completion pulse", t)
        }
    }

    private fun cancelPendingPulseOff() {
        pulseOffRunnable?.let { handler.removeCallbacks(it) }
        pulseOffRunnable = null
    }

    /** Turn off all Glyph zones. */
    fun turnOff() {
        try {
            glyphManager?.turnOff()
            Log.d(TAG, "Glyph turned off")
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to turn off Glyph", t)
        }
    }
}
