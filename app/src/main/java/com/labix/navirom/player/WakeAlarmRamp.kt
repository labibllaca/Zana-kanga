package com.labix.navirom.player

/** Volume curve of the wake alarm: quiet start, one step every 15 seconds. */
object WakeAlarmRamp {
    const val START_LEVEL: Float = 0.05f
    const val STEP: Float = 0.05f
    const val TICK_MS: Long = 15_000L

    fun levelForTick(tick: Int): Float =
        (START_LEVEL + STEP * tick.coerceAtLeast(0)).coerceAtMost(1f)
}
