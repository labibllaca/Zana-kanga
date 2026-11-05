package com.labix

import com.labix.navirom.data.model.NaviromTrack
import com.labix.navirom.player.queueTrackAt
import org.junit.Assert.assertEquals
import org.junit.Test

class QueueSelectionTest {
    @Test
    fun selectingQueueIndexReturnsTheTrackAtThatDisplayedPosition() {
        val queue = listOf(
            NaviromTrack(id = "one", title = "First"),
            NaviromTrack(id = "two", title = "Second"),
            NaviromTrack(id = "three", title = "Third")
        )

        assertEquals("two", queueTrackAt(queue, 1)?.id)
        assertEquals(listOf("one", "two", "three"), queue.map { it.id })
    }
}
