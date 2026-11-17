package com.labix.navirom.player

import com.labix.navirom.data.model.SleepTimerOptions

internal object SleepTimerCompletion {
    suspend fun run(
        options: SleepTimerOptions,
        pausePlayback: () -> Unit,
        clearTimer: () -> Unit,
        executeActions: suspend (SleepTimerOptions) -> Unit,
        onExpirySnapshot: () -> Unit = {}
    ) {
        onExpirySnapshot()
        pausePlayback()
        clearTimer()
        executeActions(options)
    }
}
