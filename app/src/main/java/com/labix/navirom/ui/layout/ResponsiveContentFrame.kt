package com.labix.navirom.ui.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun responsiveContentMaxWidth(widthClass: WindowWidthClass): Dp = when (widthClass) {
    WindowWidthClass.COMPACT -> Dp.Infinity
    WindowWidthClass.MEDIUM -> 960.dp
    WindowWidthClass.EXPANDED -> 1280.dp
}

fun responsiveSettingsMaxWidth(widthClass: WindowWidthClass): Dp = when (widthClass) {
    WindowWidthClass.COMPACT -> Dp.Infinity
    WindowWidthClass.MEDIUM, WindowWidthClass.EXPANDED -> 960.dp
}

fun responsiveStatsMaxWidth(widthClass: WindowWidthClass): Dp = when (widthClass) {
    WindowWidthClass.COMPACT -> Dp.Infinity
    WindowWidthClass.MEDIUM -> 960.dp
    WindowWidthClass.EXPANDED -> 1120.dp
}

fun usesWidePlayerLayout(width: Dp, height: Dp): Boolean =
    width >= 840.dp || (width >= 600.dp && width > height && height <= 600.dp)

@Composable
fun ResponsiveContentFrame(
    widthClass: WindowWidthClass,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = responsiveContentMaxWidth(widthClass))
                .fillMaxSize(),
            content = content
        )
    }
}
