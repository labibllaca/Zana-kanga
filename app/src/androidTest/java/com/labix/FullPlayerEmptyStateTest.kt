package com.labix

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.labix.navirom.data.model.DownloadStatus
import com.labix.navirom.data.model.PlaybackState
import com.labix.navirom.ui.components.FullPlayerModal
import org.junit.Rule
import org.junit.Test

class FullPlayerEmptyStateTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun playerShowsHelpfulStateWhenNothingIsPlaying() {
        composeRule.setContent {
            MaterialTheme {
                FullPlayerModal(
                    playbackState = PlaybackState(),
                    isFavorite = false,
                    downloadStatus = DownloadStatus.NOT_DOWNLOADED,
                    onDismiss = {},
                    onTogglePlayPause = {},
                    onNext = {},
                    onPrevious = {},
                    onSeekTo = {},
                    onToggleShuffle = {},
                    onCycleRepeat = {},
                    onToggleFavorite = {},
                    onDownloadTrack = {},
                    onOpenQueue = {},
                    onSetSpeed = {},
                    onSetSleepTimer = { _, _ -> }
                )
            }
        }

        composeRule.onNodeWithText("Nothing playing").assertIsDisplayed()
        composeRule.onNodeWithText("Choose a song from your library to start listening").assertIsDisplayed()
    }
}
