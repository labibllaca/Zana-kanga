package com.labix.navirom.player

object NightSleepTimerPolicy {
    fun isWithinNightWindow(hour: Int, minute: Int): Boolean {
        val minuteOfDay = hour * 60 + minute
        return minuteOfDay in 0 until 6 * 60 || minuteOfDay in 23 * 60 until 24 * 60
    }

    fun shouldStart(
        enabled: Boolean,
        wasPlaying: Boolean,
        isPlaying: Boolean,
        nightTimerActive: Boolean,
        hour: Int,
        minute: Int
    ): Boolean = enabled && !wasPlaying && isPlaying && !nightTimerActive &&
        isWithinNightWindow(hour, minute)
}
