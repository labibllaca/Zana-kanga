package com.labix.navirom.player

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first

class PlaybackAwareCountdown {
    suspend fun run(
        initialSeconds: Int,
        isPlaying: StateFlow<Boolean>,
        onTick: (Int) -> Unit,
        onFinished: () -> Unit
    ) {
        var remainingSeconds = initialSeconds.coerceAtLeast(0)
        while (remainingSeconds > 0) {
            if (!isPlaying.value) isPlaying.first { it }
            delay(1_000L)
            if (isPlaying.value) {
                remainingSeconds--
                onTick(remainingSeconds)
            }
        }
        onFinished()
    }
}
