package com.labix.navirom.player

import com.labix.navirom.data.model.NaviromTrack

internal fun queueTrackAt(queue: List<NaviromTrack>, index: Int): NaviromTrack? =
    queue.getOrNull(index)
