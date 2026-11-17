package com.labix.navirom.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat

/** Fires when the exact alarm goes off and wakes the playback service. */
class WakeAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ACTION_FIRE_WAKE_ALARM) return
        startAlarmPlayback(context)
    }

    companion object {
        const val ACTION_FIRE_WAKE_ALARM = "com.labix.navirom.ACTION_FIRE_WAKE_ALARM"

        fun startAlarmPlayback(context: Context) {
            val preferences = WakeAlarmPreferences(
                context.getSharedPreferences(WakeAlarmPreferences.PREFERENCES_NAME, Context.MODE_PRIVATE)
            )
            preferences.disarm()
            val serviceIntent = Intent(context, NaviromPlaybackService::class.java)
                .setAction(NaviromPlaybackService.ACTION_WAKE_ALARM)
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }
}
