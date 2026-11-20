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

    /** True when the range spans more than a single local day (e.g. a month filter). */
    val isMultiDay: Boolean
        get() = (endMillisExclusive - startMillisInclusive) > 36L * 60L * 60L * 1000L
}

data class WorkoutWeekDay(
    val dayOfWeekName: String,
    val dayOfMonth: Int,
    val isToday: Boolean,
    val hasWorkout: Boolean,
    val timestampMillis: Long,
    val range: WorkoutDateRange = WorkoutDateRange(timestampMillis, timestampMillis + 86_400_000L),
)

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

    fun currentWeek(
        nowMillis: Long = System.currentTimeMillis(),
        completedAtMillis: List<Long>,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): List<WorkoutWeekDay> {
        val now = Calendar.getInstance(timeZone).apply { timeInMillis = nowMillis }
        val todayYear = now.get(Calendar.YEAR)
        val todayDayOfYear = now.get(Calendar.DAY_OF_YEAR)

        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = nowMillis
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            val dow = get(Calendar.DAY_OF_WEEK)
            val daysFromMonday = (dow - Calendar.MONDAY + 7) % 7
            add(Calendar.DAY_OF_MONTH, -daysFromMonday)
        }

        val completedDates = completedAtMillis.map { millis ->
            val c = Calendar.getInstance(timeZone).apply { timeInMillis = millis }
            c.get(Calendar.YEAR) to c.get(Calendar.DAY_OF_YEAR)
        }.toSet()

        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        return (0..6).map { i ->
            val dayCal = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, i) }
            val nextDayCal = (dayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
            val y = dayCal.get(Calendar.YEAR)
            val doy = dayCal.get(Calendar.DAY_OF_YEAR)
            val isToday = y == todayYear && doy == todayDayOfYear
            val hasWorkout = (y to doy) in completedDates
            WorkoutWeekDay(
                dayOfWeekName = dayNames[i],
                dayOfMonth = dayCal.get(Calendar.DAY_OF_MONTH),
                isToday = isToday,
                hasWorkout = hasWorkout,
                timestampMillis = dayCal.timeInMillis,
                range = WorkoutDateRange(dayCal.timeInMillis, nextDayCal.timeInMillis),
            )
        }
    }

    /**
     * Computes whole local calendar days between [earlierMillis] and [laterMillis], avoiding
     * 24-hour division bugs across midnight or DST boundaries.
     */
    fun daysBetweenLocalDates(
        earlierMillis: Long,
        laterMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault(),
    ): Int {
        val earlier = Calendar.getInstance(timeZone).apply {
            timeInMillis = earlierMillis
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val later = Calendar.getInstance(timeZone).apply {
            timeInMillis = laterMillis
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return Math.round((later.timeInMillis - earlier.timeInMillis).toDouble() / 86_400_000.0)
            .toInt()
            .coerceAtLeast(0)
    }

    fun formatVolumeTonnage(volumeKg: Double): String {
        return if (volumeKg >= 1000.0) {
            val tonnes = volumeKg / 1000.0
            String.format(java.util.Locale.US, "%.1ft", tonnes)
        } else {
            "${formatWeight(volumeKg)} kg"
        }
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
