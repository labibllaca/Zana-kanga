package com.labix

import com.labix.navirom.player.WakeAlarmTime
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeAlarmTriggerDueTest {

    @Test
    fun trigger_is_not_due_before_the_armed_instant() {
        assertFalse(WakeAlarmTime.isTriggerDue(nowMs = 1_000L, triggerAtMs = 2_000L))
    }

    @Test
    fun trigger_is_due_at_and_after_the_armed_instant() {
        assertTrue(WakeAlarmTime.isTriggerDue(nowMs = 2_000L, triggerAtMs = 2_000L))
        assertTrue(WakeAlarmTime.isTriggerDue(nowMs = 5_000L, triggerAtMs = 2_000L))
    }

    @Test
    fun missing_trigger_stamp_is_never_due() {
        assertFalse(WakeAlarmTime.isTriggerDue(nowMs = 5_000L, triggerAtMs = 0L))
    }
}
