package com.labix

import com.labix.navirom.player.PlaybackAwareCountdown
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackAwareCountdownTest {
    @Test
    fun pausedPlaybackDoesNotAdvanceCountdown() = runTest {
        val isPlaying = MutableStateFlow(false)
        var remainingSeconds = 2
        var finished = false

        backgroundScope.launch {
            PlaybackAwareCountdown().run(
                initialSeconds = 2,
                isPlaying = isPlaying,
                onTick = { remainingSeconds = it },
                onFinished = { finished = true }
            )
        }
        runCurrent()
        advanceTimeBy(5_000)
        runCurrent()

        assertEquals(2, remainingSeconds)
        assertFalse(finished)
    }

    @Test
    fun resumeContinuesRemainingPlaybackTimeAndFinishesOnce() = runTest {
        val isPlaying = MutableStateFlow(true)
        val ticks = mutableListOf<Int>()
        var finishCount = 0

        backgroundScope.launch {
            PlaybackAwareCountdown().run(
                initialSeconds = 2,
                isPlaying = isPlaying,
                onTick = { ticks += it },
                onFinished = { finishCount++ }
            )
        }
        runCurrent()
        advanceTimeBy(1_000)
        runCurrent()
        isPlaying.value = false
        runCurrent()
        advanceTimeBy(3_000)
        runCurrent()

        assertEquals(listOf(1), ticks)
        assertEquals(0, finishCount)

        isPlaying.value = true
        runCurrent()
        advanceTimeBy(1_000)
        runCurrent()

        assertEquals(listOf(1, 0), ticks)
        assertEquals(1, finishCount)
    }
}
