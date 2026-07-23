package com.yifeng.commandcenter

import com.yifeng.commandcenter.timer.TimerMath
import org.junit.Assert.assertEquals
import org.junit.Test

class TimerMathTest {
    @Test
    fun `add action moves end time forward by fifteen seconds`() {
        assertEquals(115_000L, TimerMath.adjustedEndTime(100_000L, 15))
    }

    @Test
    fun `subtract action moves end time backward by fifteen seconds`() {
        assertEquals(85_000L, TimerMath.adjustedEndTime(100_000L, -15))
    }

    @Test
    fun `remaining time never becomes negative`() {
        assertEquals(0, TimerMath.remainingSeconds(endMillis = 1_000L, nowMillis = 2_000L))
    }

    @Test
    fun `remaining time is returned in whole seconds`() {
        assertEquals(14, TimerMath.remainingSeconds(endMillis = 15_900L, nowMillis = 1_000L))
    }

    @Test
    fun `adding time after expiry calculates from now`() {
        assertEquals(
            35_000L,
            TimerMath.adjustedRestEndTime(
                currentEndMillis = 10_000L,
                nowMillis = 20_000L,
                deltaSeconds = 15,
            ),
        )
    }

    @Test
    fun `subtracting time after expiry remains expired`() {
        assertEquals(
            5_000L,
            TimerMath.adjustedRestEndTime(
                currentEndMillis = 10_000L,
                nowMillis = 20_000L,
                deltaSeconds = -15,
            ),
        )
    }
}