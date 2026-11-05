package com.labix

import com.labix.navirom.data.model.SleepTimerOptions
import com.labix.navirom.player.SleepTimerCompletion
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class SleepTimerCompletionTest {
    @Test
    fun pausesClearsThenExecutesActionsWithTheSelectedOptions() = runTest {
        val selectedOptions = SleepTimerOptions(
            disableBluetooth = true,
            disableWifi = false,
            disableMobileData = true,
            disableGps = true
        )
        val events = mutableListOf<String>()
        var executedOptions: SleepTimerOptions? = null

        SleepTimerCompletion.run(
            options = selectedOptions,
            pausePlayback = { events += "pause" },
            clearTimer = { events += "clear" },
            executeActions = {
                executedOptions = it
                events += "actions"
            }
        )

        assertEquals(listOf("pause", "clear", "actions"), events)
        assertEquals(selectedOptions, executedOptions)
    }
}
