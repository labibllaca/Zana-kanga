package com.labix

import com.labix.navirom.player.NightSleepTimerPolicy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NightSleepTimerPolicyTest {
    @Test
    fun nightWindowIncludes2300AndExcludes0600() {
        assertFalse(NightSleepTimerPolicy.isWithinNightWindow(22, 59))
        assertTrue(NightSleepTimerPolicy.isWithinNightWindow(23, 0))
        assertTrue(NightSleepTimerPolicy.isWithinNightWindow(5, 59))
        assertFalse(NightSleepTimerPolicy.isWithinNightWindow(6, 0))
    }

    @Test
    fun startsOnlyForEnabledPlaybackTransitionInsideNightWindow() {
        assertTrue(NightSleepTimerPolicy.shouldStart(true, false, true, false, 23, 0))
        assertFalse(NightSleepTimerPolicy.shouldStart(false, false, true, false, 23, 0))
        assertFalse(NightSleepTimerPolicy.shouldStart(true, true, true, false, 23, 0))
        assertFalse(NightSleepTimerPolicy.shouldStart(true, false, true, false, 22, 59))
        assertFalse(NightSleepTimerPolicy.shouldStart(true, false, true, false, 6, 0))
    }

    @Test
    fun activeNightTimerDoesNotRestartOnPlaybackResume() {
        assertFalse(NightSleepTimerPolicy.shouldStart(true, false, true, true, 23, 30))
    }
}
