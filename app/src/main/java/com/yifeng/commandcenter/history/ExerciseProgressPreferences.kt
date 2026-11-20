package com.yifeng.commandcenter.history

import android.content.Context
import android.content.SharedPreferences

class ExerciseProgressPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSelectedExerciseKey(): String? =
        prefs.getString(KEY_SELECTED_EXERCISE, null)?.takeIf { it.isNotBlank() }

    fun setSelectedExerciseKey(normalizedKey: String) {
        prefs.edit().putString(KEY_SELECTED_EXERCISE, normalizedKey).apply()
    }

    fun getSelectedMetric(): ExerciseProgressMetric {
        val raw = prefs.getString(KEY_SELECTED_METRIC, null)
        return ExerciseProgressMetric.entries.firstOrNull { it.name == raw }
            ?: ExerciseProgressMetric.TOP_WEIGHT
    }

    fun setSelectedMetric(metric: ExerciseProgressMetric) {
        prefs.edit().putString(KEY_SELECTED_METRIC, metric.name).apply()
    }

    private companion object {
        const val PREFS_NAME = "exercise_progress_prefs"
        const val KEY_SELECTED_EXERCISE = "selected_exercise_key"
        const val KEY_SELECTED_METRIC = "selected_metric"
    }
}
