package com.labix.navirom.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.labix.navirom.ui.AppLanguage
import com.labix.navirom.ui.NaviromStrings
import com.labix.navirom.ui.NaviromTab

@Composable
fun ResponsivePrimaryNavigation(
    selectedTab: NaviromTab,
    onTabSelected: (NaviromTab) -> Unit,
    modifier: Modifier = Modifier,
    appLanguage: AppLanguage = AppLanguage.ENGLISH
) {
    NavigationRail(modifier = modifier) {
        NaviromTab.entries.forEach { tab ->
            val labelKey = when (tab) {
                NaviromTab.LIBRARY -> "tab_library"
                NaviromTab.PLAYLISTS -> "tab_playlists"
                NaviromTab.FOLDERS -> "tab_folders"
                NaviromTab.SEARCH -> "tab_search"
                NaviromTab.SETTINGS -> "tab_settings"
            }
            val label = NaviromStrings.get(labelKey, appLanguage)
            val icon = when (tab) {
                NaviromTab.LIBRARY -> Icons.Filled.LibraryMusic
                NaviromTab.PLAYLISTS -> Icons.Filled.PlaylistPlay
                NaviromTab.FOLDERS -> Icons.Filled.Folder
                NaviromTab.SEARCH -> Icons.Filled.Search
                NaviromTab.SETTINGS -> Icons.Filled.Settings
            }

            NavigationRailItem(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                icon = { Icon(imageVector = icon, contentDescription = label) },
                label = { Text(label) },
                alwaysShowLabel = true
            )
        }
    }
}
