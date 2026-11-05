package com.labix

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.labix.navirom.ui.layout.usesWidePlayerLayout
import com.labix.navirom.ui.layout.WindowWidthClass
import com.labix.navirom.ui.layout.responsiveContentMaxWidth
import com.labix.navirom.ui.layout.responsiveSettingsMaxWidth
import com.labix.navirom.ui.layout.responsiveStatsMaxWidth
import org.junit.Assert.assertEquals
import org.junit.Test

class ResponsiveContentFrameTest {
    @Test
    fun contentWidthIsBoundedOnlyForMediumAndExpandedWindows() {
        assertEquals(Dp.Infinity, responsiveContentMaxWidth(WindowWidthClass.COMPACT))
        assertEquals(960.dp, responsiveContentMaxWidth(WindowWidthClass.MEDIUM))
        assertEquals(1280.dp, responsiveContentMaxWidth(WindowWidthClass.EXPANDED))
        assertEquals(Dp.Infinity, responsiveSettingsMaxWidth(WindowWidthClass.COMPACT))
        assertEquals(960.dp, responsiveSettingsMaxWidth(WindowWidthClass.MEDIUM))
        assertEquals(960.dp, responsiveSettingsMaxWidth(WindowWidthClass.EXPANDED))
        assertEquals(Dp.Infinity, responsiveStatsMaxWidth(WindowWidthClass.COMPACT))
        assertEquals(960.dp, responsiveStatsMaxWidth(WindowWidthClass.MEDIUM))
        assertEquals(1120.dp, responsiveStatsMaxWidth(WindowWidthClass.EXPANDED))
    }

    @Test
    fun playerUsesWideLayoutForExpandedWindowsOrShortMediumLandscape() {
        assertEquals(false, usesWidePlayerLayout(599.dp, 900.dp))
        assertEquals(true, usesWidePlayerLayout(600.dp, 400.dp))
        assertEquals(false, usesWidePlayerLayout(800.dp, 601.dp))
        assertEquals(true, usesWidePlayerLayout(840.dp, 900.dp))
    }
}
