package com.yifeng.commandcenter

import com.yifeng.commandcenter.timer.NotificationText
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationTextTest {
    @Test
    fun `preserves legitimate unicode and punctuation`() {
        assertEquals(
            "Développé d’Arnold — Straße 👨‍👩‍👧 🏋️",
            NotificationText.plain("Développé d’Arnold — Straße 👨‍👩‍👧 🏋️"),
        )
    }

    @Test
    fun `flattens layout separators and removes control characters`() {
        assertEquals(
            "Incline DB Press Set A",
            NotificationText.plain("Incline\tDB\nPress\u0000\u2028Set A"),
        )
        assertEquals("Exercise", NotificationText.plain("\n\r\u0000"))
    }

    @Test
    fun `removes directional and zero width layout controls and malformed surrogates`() {
        assertEquals(
            "Bench Press",
            NotificationText.plain("\uD800Bench\u202EPress\u200B\u2060\uDC00"),
        )
        assertEquals("Exercise", NotificationText.plain("\u202E\u200B\u2060\uFEFF"))
    }
}