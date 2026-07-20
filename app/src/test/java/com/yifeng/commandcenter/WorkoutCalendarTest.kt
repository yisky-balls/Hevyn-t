package com.yifeng.commandcenter

import com.yifeng.commandcenter.data.WorkoutCalendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class WorkoutCalendarTest {
    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun july2026_hasCorrectOffsetAndDays() {
        val month = WorkoutCalendar.month(date(2026, Calendar.JULY, 21), emptyList(), utc)

        assertEquals(3, month.firstDayOffset)
        assertEquals(31, month.daysInMonth)
        assertEquals(5, month.weekCount)
    }

    @Test
    fun leapYearFebruary_has29Days() {
        val month = WorkoutCalendar.month(date(2024, Calendar.FEBRUARY, 1), emptyList(), utc)
        assertEquals(29, month.daysInMonth)
    }

    @Test
    fun workoutDays_includeOnlyCurrentMonthAndDeduplicate() {
        val month = WorkoutCalendar.month(
            nowMillis = date(2026, Calendar.JULY, 21),
            completedAtMillis = listOf(
                date(2026, Calendar.JULY, 4),
                date(2026, Calendar.JULY, 4, 18),
                date(2026, Calendar.JUNE, 4),
            ),
            timeZone = utc,
        )

        assertEquals(setOf(4), month.workoutDays)
        assertTrue(4 in month.workoutDays)
    }

    private fun date(year: Int, month: Int, day: Int, hour: Int = 12): Long =
        Calendar.getInstance(utc).apply {
            clear()
            set(year, month, day, hour, 0, 0)
        }.timeInMillis
}