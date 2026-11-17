package com.labix

import com.labix.navirom.player.WakeAlarmTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.util.TimeZone

class WakeAlarmTimeTest {
    private val zone = TimeZone.getTimeZone("Europe/Berlin")

    private fun millisOf(dateTime: LocalDateTime): Long =
        dateTime.atZone(zone.toZoneId()).toInstant().toEpochMilli()

    @Test
    fun next_trigger_rolls_to_tomorrow_when_todays_time_already_passed() {
        val now = millisOf(LocalDateTime.of(2026, 11, 17, 22, 0))
        val next = WakeAlarmTime.nextTriggerAt(now, 7, 0, zone)
        assertEquals(millisOf(LocalDateTime.of(2026, 11, 18, 7, 0)), next)
    }

    @Test
    fun next_trigger_uses_today_when_todays_time_is_still_ahead() {
        val now = millisOf(LocalDateTime.of(2026, 11, 17, 5, 30))
        val next = WakeAlarmTime.nextTriggerAt(now, 7, 0, zone)
        assertEquals(millisOf(LocalDateTime.of(2026, 11, 17, 7, 0)), next)
    }

    @Test
    fun is_due_is_true_from_the_armed_minute_until_midnight() {
        val justAfter = millisOf(LocalDateTime.of(2026, 11, 18, 7, 0, 30))
        assertTrue(WakeAlarmTime.isDue(justAfter, 7, 0, zone))
        assertFalse(WakeAlarmTime.isDue(justAfter, 7, 5, zone))
    }

    @Test
    fun is_due_is_false_before_the_armed_time() {
        val before = millisOf(LocalDateTime.of(2026, 11, 18, 6, 59))
        assertFalse(WakeAlarmTime.isDue(before, 7, 0, zone))
    }
}
