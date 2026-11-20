package com.yifeng.commandcenter

import com.yifeng.commandcenter.data.WorkoutCalendar
import com.yifeng.commandcenter.history.WorkoutHistorySession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class WorkoutCalendarTest {
    private val utc = TimeZone.getTimeZone("UTC")

    @Test
    fun july2026_hasMondayFirstOffsetAndCorrectDays() {
        val month = WorkoutCalendar.month(date(2026, Calendar.JULY, 21), emptyList(), utc)

        assertEquals(2, month.firstDayOffset)
        assertEquals(31, month.daysInMonth)
        assertEquals(5, month.weekCount)
    }

    @Test
    fun mondayAndSundayMonthStarts_useOffsetsZeroAndSix() {
        val mondayStart = WorkoutCalendar.month(date(2026, Calendar.JUNE, 15), emptyList(), utc)
        val sundayStart = WorkoutCalendar.month(date(2026, Calendar.NOVEMBER, 15), emptyList(), utc)

        assertEquals(0, mondayStart.firstDayOffset)
        assertEquals(6, sundayStart.firstDayOffset)
    }

    @Test
    fun leapYearFebruary_has29Days() {
        val month = WorkoutCalendar.month(date(2024, Calendar.FEBRUARY, 1), emptyList(), utc)
        assertEquals(29, month.daysInMonth)
    }

    @Test
    fun workoutCounts_includeOnlyCurrentMonthAndCountSameDaySessions() {
        val month = WorkoutCalendar.month(
            nowMillis = date(2026, Calendar.JULY, 21),
            completedAtMillis = listOf(
                date(2026, Calendar.JULY, 4),
                date(2026, Calendar.JULY, 4, 18),
                date(2026, Calendar.JUNE, 4),
            ),
            timeZone = utc,
        )

        assertEquals(mapOf(4 to 2), month.workoutCountsByDay)
        assertEquals(setOf(4), month.workoutDays)
        assertTrue(4 in month.workoutDays)
    }

    @Test
    fun viewedMonth_marksTodayOnlyWhenTodayIsInsideIt() {
        val july = WorkoutCalendar.month(
            nowMillis = date(2026, Calendar.JULY, 1),
            completedAtMillis = emptyList(),
            timeZone = utc,
            todayMillis = date(2026, Calendar.AUGUST, 3),
        )
        val august = WorkoutCalendar.month(
            nowMillis = date(2026, Calendar.AUGUST, 1),
            completedAtMillis = emptyList(),
            timeZone = utc,
            todayMillis = date(2026, Calendar.AUGUST, 3),
        )

        assertNull(july.today)
        assertEquals(3, august.today)
    }

    @Test
    fun monthAndDayRanges_areStartInclusiveAndEndExclusive() {
        val monthRange = WorkoutCalendar.monthRange(2026, Calendar.JULY, utc)
        val dayRange = WorkoutCalendar.dayRange(2026, Calendar.JULY, 4, utc)

        assertEquals(date(2026, Calendar.JULY, 1, 0), monthRange.startMillisInclusive)
        assertEquals(date(2026, Calendar.AUGUST, 1, 0), monthRange.endMillisExclusive)
        assertTrue(date(2026, Calendar.JULY, 4, 0) in dayRange)
        assertTrue(date(2026, Calendar.JULY, 4, 23) in dayRange)
        assertTrue(date(2026, Calendar.JULY, 5, 0) !in dayRange)
    }

    @Test
    fun monthlyAggregate_includesOnlySessionsInsideRange() {
        val range = WorkoutCalendar.monthRange(2026, Calendar.JULY, utc)
        val aggregate = WorkoutCalendar.aggregate(
            sessions = listOf(
                session("july-1", date(2026, Calendar.JULY, 1, 0), 3, 120.5),
                session("july-31", date(2026, Calendar.JULY, 31, 23), 4, 240.25),
                session("august-1", date(2026, Calendar.AUGUST, 1, 0), 8, 999.0),
            ),
            range = range,
        )

        assertEquals(2, aggregate.workoutCount)
        assertEquals(7, aggregate.completedSetCount)
        assertEquals(360.75, aggregate.totalVolumeKg, 0.0)
    }

    @Test
    fun currentWeek_returns7DaysFromMondayToSundayWithWorkoutsAndToday() {
        val wednesday = date(2026, Calendar.NOVEMBER, 11) // Wed Nov 11, 2026
        val completed = listOf(
            date(2026, Calendar.NOVEMBER, 9),  // Monday
            date(2026, Calendar.NOVEMBER, 11), // Wednesday
            date(2026, Calendar.NOVEMBER, 13), // Friday
        )
        val week = WorkoutCalendar.currentWeek(wednesday, completed, utc)

        assertEquals(7, week.size)
        assertEquals(listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"), week.map { it.dayOfWeekName })
        assertEquals(listOf(9, 10, 11, 12, 13, 14, 15), week.map { it.dayOfMonth })
        assertTrue(week[0].hasWorkout) // Mon 9
        assertTrue(!week[1].hasWorkout) // Tue 10
        assertTrue(week[2].hasWorkout) // Wed 11
        assertTrue(week[2].isToday) // Wed 11 is today
        assertTrue(!week[3].hasWorkout) // Thu 12
        assertTrue(week[4].hasWorkout) // Fri 13
        assertTrue(!week[5].isToday)
    }

    @Test
    fun formatVolumeTonnage_formatsTonnesAndKgCorrectly() {
        assertEquals("28.9t", WorkoutCalendar.formatVolumeTonnage(28900.0))
        assertEquals("1.0t", WorkoutCalendar.formatVolumeTonnage(1000.0))
        assertEquals("400 kg", WorkoutCalendar.formatVolumeTonnage(400.0))
    }

    @Test
    fun daysBetweenLocalDates_distinguishesYesterdayNightFromTodayMorning() {
        val yesterdayNight = date(2026, Calendar.NOVEMBER, 10, hour = 22)
        val todayMorning = date(2026, Calendar.NOVEMBER, 11, hour = 8)
        val sameDayLater = date(2026, Calendar.NOVEMBER, 10, hour = 23)

        assertEquals(1, WorkoutCalendar.daysBetweenLocalDates(yesterdayNight, todayMorning, utc))
        assertEquals(0, WorkoutCalendar.daysBetweenLocalDates(yesterdayNight, sameDayLater, utc))
    }

    @Test
    fun dateRanges_distinguishSingleDayFromMultiDayRanges() {
        val dayRange = WorkoutCalendar.dayRange(2026, Calendar.NOVEMBER, 11, utc)
        val monthRange = WorkoutCalendar.monthRange(2026, Calendar.NOVEMBER, utc)

        assertTrue(!dayRange.isMultiDay)
        assertTrue(monthRange.isMultiDay)
    }

    private fun session(
        id: String,
        completedAtMillis: Long,
        completedSetCount: Int,
        totalVolumeKg: Double,
    ) = WorkoutHistorySession(
        id = id,
        sourceRoutineId = null,
        routineName = "Routine",
        startedAtMillis = completedAtMillis - 1_000L,
        completedAtMillis = completedAtMillis,
        durationMillis = 1_000L,
        note = "",
        completedSetCount = completedSetCount,
        totalVolumeKg = totalVolumeKg,
        isLegacySummaryOnly = false,
    )

    private fun date(year: Int, month: Int, day: Int, hour: Int = 12): Long =
        Calendar.getInstance(utc).apply {
            clear()
            set(year, month, day, hour, 0, 0)
        }.timeInMillis
}