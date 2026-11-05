package com.labix

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.labix.navirom.ui.AppLanguage
import com.labix.navirom.ui.NaviromStrings
import com.labix.navirom.ui.NaviromTab
import com.labix.navirom.ui.components.ResponsivePrimaryNavigation
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ResponsivePrimaryNavigationTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun expandedRailShowsAllPrimaryDestinations() {
        composeRule.setContent {
            MaterialTheme {
                ResponsivePrimaryNavigation(
                    selectedTab = NaviromTab.LIBRARY,
                    onTabSelected = {}
                )
            }
        }

        listOf("tab_library", "tab_playlists", "tab_folders", "tab_search", "tab_settings")
            .forEach { key ->
                composeRule.onNodeWithText(NaviromStrings.get(key, AppLanguage.ENGLISH)).assertExists()
            }
        composeRule.onNodeWithText(NaviromStrings.get("tab_library", AppLanguage.ENGLISH)).assertIsSelected()
    }

    @Test
    fun selectingDestinationCallsCallback() {
        var selectedTab: NaviromTab? = null
        composeRule.setContent {
            MaterialTheme {
                ResponsivePrimaryNavigation(
                    selectedTab = NaviromTab.LIBRARY,
                    onTabSelected = { selectedTab = it }
                )
            }
        }

        composeRule.onNodeWithText(NaviromStrings.get("tab_playlists", AppLanguage.ENGLISH)).performClick()

        composeRule.runOnIdle { assertEquals(NaviromTab.PLAYLISTS, selectedTab) }
    }
}
