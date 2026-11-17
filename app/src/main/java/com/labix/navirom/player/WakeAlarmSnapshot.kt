package com.labix.navirom.player

import com.labix.navirom.data.model.NaviromTrack
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/** The track, queue and position the wake alarm resumes from. */
data class WakeAlarmSnapshot(
    val track: NaviromTrack,
    val queue: List<NaviromTrack>,
    val positionMs: Long
)

object WakeAlarmSnapshotCodec {
    private val moshi: Moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    private val trackListType = Types.newParameterizedType(List::class.java, NaviromTrack::class.java)
    private val adapter = moshi.adapter(WakeAlarmSnapshot::class.java)
    private val queueAdapter = moshi.adapter<List<NaviromTrack>>(trackListType)

    fun encode(snapshot: WakeAlarmSnapshot): String = adapter.toJson(snapshot)

    fun decode(json: String?): WakeAlarmSnapshot? {
        if (json.isNullOrBlank()) return null
        return try {
            adapter.fromJson(json)
        } catch (_: Throwable) {
            null
        }
    }

    /** Queue-only payloads are stored separately so a broken queue cannot kill the track. */
    fun encodeQueue(queue: List<NaviromTrack>): String = queueAdapter.toJson(queue)

    fun decodeQueue(json: String?): List<NaviromTrack> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            queueAdapter.fromJson(json) ?: emptyList()
        } catch (_: Throwable) {
            emptyList()
        }
    }
}
