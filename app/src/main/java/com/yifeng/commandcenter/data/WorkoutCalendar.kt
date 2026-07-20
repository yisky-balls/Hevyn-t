package com.yifeng.commandcenter.data

import java.util.Calendar
import java.util.TimeZone

data class WorkoutMonth(
    val year: Int,
    val month: Int,
    val firstDayOffset: Int,
    val daysInMonth: Int,
    val workoutDays: Set<Int>,
    val today: Int?,
) {
    val weekCount: Int get() = (firstDayOffset + daysInMonth + 6) / 7
}

object WorkoutCalendar {
    fun month(
        nowMillis: Long,
        completedAtMillis: List<Long>,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): WorkoutMonth {
        val now = Calendar.getInstance(timeZone).apply { timeInMillis = nowMillis }
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH)
        val first = Calendar.getInstance(timeZone).apply {
            clear()
            set(year, month, 1)
        }
        val workoutDays = completedAtMillis.mapNotNullTo(mutableSetOf()) { millis ->
            val date = Calendar.getInstance(timeZone).apply { timeInMillis = millis }
            date.get(Calendar.DAY_OF_MONTH).takeIf {
                date.get(Calendar.YEAR) == year && date.get(Calendar.MONTH) == month
            }
        }
        return WorkoutMonth(
            year = year,
            month = month,
            firstDayOffset = first.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY,
            daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH),
            workoutDays = workoutDays,
            today = now.get(Calendar.DAY_OF_MONTH),
        )
    }
}