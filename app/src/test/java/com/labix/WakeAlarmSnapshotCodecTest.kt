package com.labix

import com.labix.navirom.data.model.NaviromTrack
import com.labix.navirom.player.WakeAlarmSnapshot
import com.labix.navirom.player.WakeAlarmSnapshotCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WakeAlarmSnapshotCodecTest {
    private val track = NaviromTrack(
        id = "t1",
        title = "Song",
        artist = "Artist",
        album = "Album",
        durationSeconds = 200,
        streamUrl = "https://example.test/stream/1"
    )

    @Test
    fun round_trips_track_queue_and_position() {
        val snapshot = WakeAlarmSnapshot(track, listOf(track, track.copy(id = "t2")), 42_000L)
        assertEquals(snapshot, WakeAlarmSnapshotCodec.decode(WakeAlarmSnapshotCodec.encode(snapshot)))
    }

    @Test
    fun broken_or_missing_json_decodes_to_null() {
        assertNull(WakeAlarmSnapshotCodec.decode("{not json"))
        assertNull(WakeAlarmSnapshotCodec.decode(null))
        assertNull(WakeAlarmSnapshotCodec.decode(""))
    }
}
