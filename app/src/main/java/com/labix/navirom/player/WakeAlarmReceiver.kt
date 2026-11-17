package com.labix.navirom.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.labix.MainActivity

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
            try {
                ContextCompat.startForegroundService(context, serviceIntent)
            } catch (e: Throwable) {
                // Android 15+ blocks background FGS starts (boot broadcast, inexact alarm).
                // Opening the app makes it foreground, which allows the service start.
                try {
                    context.startActivity(
                        Intent(context, MainActivity::class.java).apply {
                            action = ACTION_START_WAKE_ALARM
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        }
                    )
                } catch (_: Throwable) {}
            }
        }

        const val ACTION_START_WAKE_ALARM = "com.labix.navirom.ACTION_START_WAKE_ALARM"
    }
}
