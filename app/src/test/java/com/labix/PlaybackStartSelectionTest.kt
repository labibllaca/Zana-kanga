package com.labix

import com.labix.navirom.data.model.NaviromTrack
import com.labix.navirom.player.resolvePlaybackStart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackStartSelectionTest {

    private fun tracks(count: Int) = (0 until count).map { index ->
        NaviromTrack(id = "track_$index", title = "Track $index")
    }

    @Test
    fun tappingATrackWhileShuffleIsOnStartsThatTrack() {
        val folder = tracks(40)
        val clickedIndex = 17

        repeat(50) {
            val start = resolvePlaybackStart(folder, clickedIndex, shuffle = true)

            assertEquals(
                "Tapping '${folder[clickedIndex].title}' must play that track, not another one",
                folder[clickedIndex].id,
                start.queue[start.index].id
            )
            assertEquals(folder.size, start.queue.size)
            assertEquals(folder.map { it.id }.toSet(), start.queue.map { it.id }.toSet())
        }
    }

    @Test
    fun tappingATrackWithShuffleOffKeepsFolderOrderAndStartsThatTrack() {
        val folder = tracks(10)

        val start = resolvePlaybackStart(folder, 4, shuffle = false)

        assertEquals(folder.map { it.id }, start.queue.map { it.id })
        assertEquals(4, start.index)
        assertEquals(folder[4].id, start.queue[start.index].id)
    }

    @Test
    fun outOfBoundsStartIndexStillPlaysAValidTrack() {
        val folder = tracks(3)

        assertEquals(2, resolvePlaybackStart(folder, 99, shuffle = false).index)
        assertEquals(0, resolvePlaybackStart(folder, -4, shuffle = false).index)

        val shuffledStart = resolvePlaybackStart(folder, 99, shuffle = true)
        assertTrue(shuffledStart.index in shuffledStart.queue.indices)
        assertEquals(folder.last().id, shuffledStart.queue[shuffledStart.index].id)
    }

    @Test
    fun emptyTrackListProducesAnEmptyQueue() {
        val start = resolvePlaybackStart(emptyList(), 0, shuffle = true)

        assertTrue(start.queue.isEmpty())
        assertEquals(0, start.index)
    }
}
