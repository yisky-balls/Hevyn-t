package com.yifeng.commandcenter.data

import com.yifeng.commandcenter.history.MonthlyWorkoutAggregate
import com.yifeng.commandcenter.history.WorkoutHistorySession
import java.util.Calendar
import java.util.TimeZone

data class WorkoutDateRange(
    val startMillisInclusive: Long,
    val endMillisExclusive: Long,
) {
    operator fun contains(millis: Long): Boolean =
        millis >= startMillisInclusive && millis < endMillisExclusive
}

data class WorkoutMonth(
    val year: Int,
    val month: Int,
    val firstDayOffset: Int,
    val daysInMonth: Int,
    val workoutCountsByDay: Map<Int, Int>,
    val today: Int?,
    val range: WorkoutDateRange,
) {
    val weekCount: Int get() = (firstDayOffset + daysInMonth + 6) / 7
    val workoutDays: Set<Int> get() = workoutCountsByDay.keys
}

object WorkoutCalendar {
    fun month(
        nowMillis: Long,
        completedAtMillis: List<Long>,
        timeZone: TimeZone = TimeZone.getDefault(),
        todayMillis: Long = nowMillis,
    ): WorkoutMonth {
        val now = Calendar.getInstance(timeZone).apply { timeInMillis = nowMillis }
        val year = now.get(Calendar.YEAR)
        val month = now.get(Calendar.MONTH)
        val first = Calendar.getInstance(timeZone).apply {
            clear()
            set(year, month, 1)
        }
        val workoutCountsByDay = completedAtMillis.mapNotNull { millis ->
            val date = Calendar.getInstance(timeZone).apply { timeInMillis = millis }
            date.get(Calendar.DAY_OF_MONTH).takeIf {
                date.get(Calendar.YEAR) == year && date.get(Calendar.MONTH) == month
            }
        }.groupingBy { it }.eachCount()
        return WorkoutMonth(
            year = year,
            month = month,
            firstDayOffset = (first.get(Calendar.DAY_OF_WEEK) + 5) % 7,
            daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH),
            workoutCountsByDay = workoutCountsByDay,
            today = dayInMonth(todayMillis, year, month, timeZone),
            range = monthRange(year, month, timeZone),
        )
    }

    fun monthRange(
        year: Int,
        month: Int,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): WorkoutDateRange {
        val start = Calendar.getInstance(timeZone).apply {
            clear()
            set(year, month, 1)
        }
        val end = (start.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
        return WorkoutDateRange(
            startMillisInclusive = start.timeInMillis,
            endMillisExclusive = end.timeInMillis,
        )
    }

    fun dayRange(
        year: Int,
        month: Int,
        day: Int,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): WorkoutDateRange {
        val start = Calendar.getInstance(timeZone).apply {
            clear()
            set(year, month, day)
        }
        val end = (start.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
        return WorkoutDateRange(
            startMillisInclusive = start.timeInMillis,
            endMillisExclusive = end.timeInMillis,
        )
    }

    fun aggregate(
        sessions: List<WorkoutHistorySession>,
        range: WorkoutDateRange,
    ): MonthlyWorkoutAggregate {
        val matchingSessions = sessions.filter { it.completedAtMillis in range }
        return MonthlyWorkoutAggregate(
            workoutCount = matchingSessions.size,
            completedSetCount = matchingSessions.sumOf { it.completedSetCount },
            totalVolumeKg = matchingSessions.sumOf { it.totalVolumeKg },
        )
    }

    private fun dayInMonth(
        millis: Long,
        year: Int,
        month: Int,
        timeZone: TimeZone,
    ): Int? {
        val date = Calendar.getInstance(timeZone).apply { timeInMillis = millis }
        return date.get(Calendar.DAY_OF_MONTH).takeIf {
            date.get(Calendar.YEAR) == year && date.get(Calendar.MONTH) == month
        }
    }
}