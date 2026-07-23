package com.yifeng.commandcenter

import com.yifeng.commandcenter.ui.sanitizeWorkoutNumber
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutInputTest {
    @Test
    fun `weight input keeps one decimal separator`() {
        assertEquals("82.55", sanitizeWorkoutNumber("82..5x5", allowDecimal = true))
        assertEquals(".5", sanitizeWorkoutNumber(".5", allowDecimal = true))
        assertEquals("82.5", sanitizeWorkoutNumber("82,5", allowDecimal = true))
    }

    @Test
    fun `repetition input remains integer only`() {
        assertEquals("825", sanitizeWorkoutNumber("8.2x5", allowDecimal = false))
    }

    @Test
    fun `numeric input respects its length limit`() {
        assertEquals("123.45", sanitizeWorkoutNumber("123.4567", allowDecimal = true))
        assertEquals("123456", sanitizeWorkoutNumber("1234567", allowDecimal = false))
    }
}