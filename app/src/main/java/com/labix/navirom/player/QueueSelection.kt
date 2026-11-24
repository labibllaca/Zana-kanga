package com.labix.navirom.player

import com.labix.navirom.data.model.NaviromTrack

internal data class PlaybackStart(
    val queue: List<NaviromTrack>,
    val index: Int
)

internal fun queueTrackAt(queue: List<NaviromTrack>, index: Int): NaviromTrack? =
    queue.getOrNull(index)

/**
 * Builds the playback queue and the index that must start playing.
 *
 * [startIndex] points into [tracks] as the caller knows it (for example the folder
 * list the user is looking at). The returned index always points into the returned
 * queue, so a track the user tapped starts even when shuffle reorders the queue.
 */
internal fun resolvePlaybackStart(
    tracks: List<NaviromTrack>,
    startIndex: Int,
    shuffle: Boolean
): PlaybackStart {
    if (tracks.isEmpty()) return PlaybackStart(emptyList(), 0)
    val safeStart = startIndex.coerceIn(0, tracks.lastIndex)
    if (!shuffle) return PlaybackStart(tracks, safeStart)

    val requestedTrackId = tracks[safeStart].id
    val queue = tracks.shuffled()
    val index = queue.indexOfFirst { it.id == requestedTrackId }.coerceAtLeast(0)
    return PlaybackStart(queue, index)
}
