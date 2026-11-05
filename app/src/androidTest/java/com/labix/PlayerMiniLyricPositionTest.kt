package com.labix

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.labix.navirom.data.lyrics.LyricsData
import com.labix.navirom.data.model.DownloadStatus
import com.labix.navirom.data.model.NaviromTrack
import com.labix.navirom.data.model.PlaybackState
import com.labix.navirom.ui.components.FullPlayerModal
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlayerMiniLyricPositionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun miniLyricIsCenteredBetweenPlaybackControlsAndBottomActions() {
        composeRule.setContent {
            MaterialTheme {
                FullPlayerModal(
                    playbackState = PlaybackState(currentTrack = NaviromTrack(id = "track", title = "Title")),
                    isFavorite = false,
                    downloadStatus = DownloadStatus.NOT_DOWNLOADED,
                    lyricsData = LyricsData(plainLyrics = "Centered sample lyric"),
                    onDismiss = {}, onTogglePlayPause = {}, onNext = {}, onPrevious = {},
                    onSeekTo = {}, onToggleShuffle = {}, onCycleRepeat = {}, onToggleFavorite = {},
                    onDownloadTrack = {}, onOpenQueue = {}, onSetSpeed = {}, onSetSleepTimer = { _, _ -> }
                )
            }
        }
        composeRule.waitForIdle()

        val lyric = composeRule.onNodeWithText("Centered sample lyric").fetchSemanticsNode().boundsInRoot
        val controls = composeRule.onNodeWithContentDescription("Repeat").fetchSemanticsNode().boundsInRoot
        val bottomActions = composeRule.onNodeWithContentDescription("Lyrics").fetchSemanticsNode().boundsInRoot
        val midpoint = (controls.center.y + bottomActions.center.y) / 2f

        assertTrue("Mini lyric should sit between controls and bottom actions", kotlin.math.abs(lyric.center.y - midpoint) < 32f)
    }
}
