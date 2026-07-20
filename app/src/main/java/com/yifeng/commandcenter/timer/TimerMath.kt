package com.yifeng.commandcenter.timer

object TimerMath {
    fun adjustedEndTime(currentEndMillis: Long, deltaSeconds: Int): Long =
        currentEndMillis + deltaSeconds * 1_000L

    fun remainingSeconds(endMillis: Long, nowMillis: Long): Int =
        ((endMillis - nowMillis).coerceAtLeast(0L) / 1_000L).toInt()

    fun adjustedRestEndTime(currentEndMillis: Long, nowMillis: Long, deltaSeconds: Int): Long =
        currentEndMillis.coerceAtLeast(nowMillis) + deltaSeconds * 1_000L
}