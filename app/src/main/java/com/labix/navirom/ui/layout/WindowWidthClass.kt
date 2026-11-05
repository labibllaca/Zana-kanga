package com.labix.navirom.ui.layout

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowWidthClass {
    COMPACT,
    MEDIUM,
    EXPANDED
}

fun windowWidthClass(widthDp: Dp): WindowWidthClass = when {
    widthDp < 600.dp -> WindowWidthClass.COMPACT
    widthDp < 840.dp -> WindowWidthClass.MEDIUM
    else -> WindowWidthClass.EXPANDED
}
