package com.labix.navirom.ui.util

fun formatMs(ms: Long): String = "%d:%02d".format(ms / 60000, ms / 1000 % 60)
