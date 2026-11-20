package com.yifeng.commandcenter.timer

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * SharedPreferences wrapper for rest-timer behaviour settings.
 */
object RestTimerPreferences {

    const val PREFERENCES = "rest_timer_settings"
    const val KEY_SOFT_LOCK_ON_COMPLETE = "soft_lock_on_complete"

    fun isSoftLockOnCompleteEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_SOFT_LOCK_ON_COMPLETE, false)

    fun setSoftLockOnCompleteEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit {
            putBoolean(KEY_SOFT_LOCK_ON_COMPLETE, enabled)
        }
    }

    fun registerListener(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs(context).registerOnSharedPreferenceChangeListener(listener)
    }

    fun unregisterListener(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        prefs(context).unregisterOnSharedPreferenceChangeListener(listener)
    }

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
}
