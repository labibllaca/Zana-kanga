package com.labix.navirom.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Keeps the wake alarm alive across reboots, time changes and app updates.
 * A time that already passed while the device was off fires immediately.
 */
class WakeAlarmBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                val preferences = WakeAlarmPreferences(
                    context.getSharedPreferences(WakeAlarmPreferences.PREFERENCES_NAME, Context.MODE_PRIVATE)
                )
                if (!preferences.enabled) return

                if (WakeAlarmTime.isDue(System.currentTimeMillis(), preferences.hour, preferences.minute)) {
                    WakeAlarmReceiver.startAlarmPlayback(context)
                } else {
                    WakeAlarmScheduler(context).rearmIfPending()
                }
            }
        }
    }
}
