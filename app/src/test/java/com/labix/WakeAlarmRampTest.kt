package com.labix

import com.labix.navirom.player.WakeAlarmRamp
import org.junit.Assert.assertEquals
import org.junit.Test

class WakeAlarmRampTest {
    @Test
    fun first_tick_starts_at_five_percent() {
        assertEquals(0.05f, WakeAlarmRamp.levelForTick(0), 0.0001f)
    }

    @Test
    fun each_tick_adds_five_percent() {
        assertEquals(0.10f, WakeAlarmRamp.levelForTick(1), 0.0001f)
        assertEquals(0.55f, WakeAlarmRamp.levelForTick(10), 0.0001f)
    }

    @Test
    fun level_never_exceeds_full_volume() {
        assertEquals(1.0f, WakeAlarmRamp.levelForTick(400), 0.0001f)
    }

    @Test
    fun tick_length_is_fifteen_seconds() {
        assertEquals(15_000L, WakeAlarmRamp.TICK_MS)
    }
}
