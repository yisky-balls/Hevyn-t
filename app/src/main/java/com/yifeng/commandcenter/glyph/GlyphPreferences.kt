package com.yifeng.commandcenter.glyph

import android.content.Context
import android.content.SharedPreferences

/**
 * Thin SharedPreferences wrapper for Glyph-related user settings.
 *
 * Kept minimal for the initial implementation. Two toggles:
 * - **glyphTimerEnabled** – show rest timer countdown on the Glyph strip
 * - **glyphPulseOnComplete** – pulse all zones when the timer finishes
 */
object GlyphPreferences {

    const val PREFERENCES = "glyph_settings"
    const val KEY_TIMER_ENABLED = "glyph_timer_enabled"
    const val KEY_PULSE_ON_COMPLETE = "glyph_pulse_on_complete"

    fun isTimerEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_TIMER_ENABLED, true)

    fun setTimerEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_TIMER_ENABLED, enabled).apply()
    }

    fun isPulseOnComplete(context: Context): Boolean =
        prefs(context).getBoolean(KEY_PULSE_ON_COMPLETE, true)

    fun setPulseOnComplete(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_PULSE_ON_COMPLETE, enabled).apply()
    }

    fun registerListener(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs(context).registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs(context).unregisterOnSharedPreferenceChangeListener(listener)
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
}
