package com.labix

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.performClick
import com.labix.navirom.data.lyrics.LyricsData
import com.labix.navirom.data.model.DownloadStatus
import com.labix.navirom.data.model.NaviromTrack
import com.labix.navirom.data.model.PlaybackState
import com.labix.navirom.ui.AppLanguage
import com.labix.navirom.ui.NaviromStrings
import com.labix.navirom.ui.components.FullPlayerModal
import org.junit.Rule
import org.junit.Test

class FullPlayerLyricsLocalizationTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var activeLanguage: MutableState<AppLanguage>

    @Test
    fun loadingIndicationUsesSelectedLanguage() {
        showLyrics(LyricsData(trackId = "track", isLoading = true))
        listOf(AppLanguage.ENGLISH, AppLanguage.ALBANIAN, AppLanguage.GERMAN).forEach { language ->
            composeRule.runOnIdle { activeLanguage.value = language }
            composeRule.waitForIdle()
            composeRule.onNodeWithText(NaviromStrings.get("lyrics_loading", language)).assertIsDisplayed()
        }
    }

    @Test
    fun noLyricsMessageUsesSelectedLanguage() {
        showLyrics(LyricsData(trackId = "track"))
        listOf(AppLanguage.ENGLISH, AppLanguage.ALBANIAN, AppLanguage.GERMAN).forEach { language ->
            composeRule.runOnIdle { activeLanguage.value = language }
            composeRule.waitForIdle()
            composeRule.onNodeWithText(NaviromStrings.get("lyrics_not_found", language)).assertIsDisplayed()
            if (language != AppLanguage.ENGLISH) {
                composeRule.onAllNodesWithText("Lyrics not available").assertCountEquals(0)
            }
        }
    }

    private fun showLyrics(lyricsData: LyricsData) {
        activeLanguage = mutableStateOf(AppLanguage.ENGLISH)
        composeRule.setContent {
            MaterialTheme {
                FullPlayerModal(
                    playbackState = PlaybackState(currentTrack = NaviromTrack(id = "track", title = "Title")),
                    isFavorite = false,
                    downloadStatus = DownloadStatus.NOT_DOWNLOADED,
                    lyricsData = lyricsData,
                    appLanguage = activeLanguage.value,
                    onDismiss = {}, onTogglePlayPause = {}, onNext = {}, onPrevious = {},
                    onSeekTo = {}, onToggleShuffle = {}, onCycleRepeat = {}, onToggleFavorite = {},
                    onDownloadTrack = {}, onOpenQueue = {}, onSetSpeed = {}, onSetSleepTimer = { _, _ -> }
                )
            }
        }
        composeRule.onNodeWithContentDescription("Lyrics").performClick()
        composeRule.waitForIdle()
    }
}
