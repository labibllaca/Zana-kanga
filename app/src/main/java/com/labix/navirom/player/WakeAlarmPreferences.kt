package com.labix.navirom.player

import android.content.SharedPreferences

/** Persists the armed wake alarm and the playback snapshot it resumes from. */
class WakeAlarmPreferences(private val preferences: SharedPreferences) {

    var enabled: Boolean
        get() = preferences.getBoolean(KEY_ENABLED, false)
        set(value) = preferences.edit().putBoolean(KEY_ENABLED, value).apply()

    var hour: Int
        get() = preferences.getInt(KEY_HOUR, DEFAULT_HOUR)
        set(value) = preferences.edit().putInt(KEY_HOUR, value).apply()

    var minute: Int
        get() = preferences.getInt(KEY_MINUTE, DEFAULT_MINUTE)
        set(value) = preferences.edit().putInt(KEY_MINUTE, value).apply()

    fun saveSnapshot(snapshot: WakeAlarmSnapshot) {
        preferences.edit()
            .putString(KEY_SNAPSHOT, WakeAlarmSnapshotCodec.encode(snapshot))
            .apply()
    }

    fun loadSnapshot(): WakeAlarmSnapshot? =
        WakeAlarmSnapshotCodec.decode(preferences.getString(KEY_SNAPSHOT, null))

    fun clearSnapshot() {
        preferences.edit().remove(KEY_SNAPSHOT).apply()
    }

    fun disarm() {
        preferences.edit().putBoolean(KEY_ENABLED, false).apply()
    }

    companion object {
        const val PREFERENCES_NAME = "navirom_wake_alarm"
        private const val KEY_ENABLED = "wake_alarm_enabled"
        private const val KEY_HOUR = "wake_alarm_hour"
        private const val KEY_MINUTE = "wake_alarm_minute"
        private const val KEY_SNAPSHOT = "wake_alarm_snapshot"
        private const val DEFAULT_HOUR = 7
        private const val DEFAULT_MINUTE = 0
    }
}
