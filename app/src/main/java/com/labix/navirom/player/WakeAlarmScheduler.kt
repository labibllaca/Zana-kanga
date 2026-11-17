package com.labix.navirom.player

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.labix.MainActivity

/**
 * Owns the single wake alarm. Exact when the platform allows it, otherwise an
 * inexact-but-Doze-tolerant fallback so the alarm still exists.
 */
class WakeAlarmScheduler(context: Context) {

    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val preferences = WakeAlarmPreferences(
        appContext.getSharedPreferences(WakeAlarmPreferences.PREFERENCES_NAME, Context.MODE_PRIVATE)
    )

    private val fireIntent: PendingIntent
        get() = PendingIntent.getBroadcast(
            appContext,
            REQUEST_CODE,
            Intent(appContext, WakeAlarmReceiver::class.java)
                .setAction(WakeAlarmReceiver.ACTION_FIRE_WAKE_ALARM),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun canScheduleExact(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

    /** Saves the wall-clock time and (re)schedules the next occurrence. */
    fun schedule(hour: Int, minute: Int) {
        preferences.hour = hour
        preferences.minute = minute
        preferences.enabled = true
        arm(hour, minute)
    }

    fun cancel() {
        preferences.enabled = false
        alarmManager.cancel(fireIntent)
    }

    /** Re-arms the saved time after boot or a clock change. */
    fun rearmIfPending() {
        if (!preferences.enabled) return
        arm(preferences.hour, preferences.minute)
    }

    private fun arm(hour: Int, minute: Int) {
        val triggerAt = WakeAlarmTime.nextTriggerAt(System.currentTimeMillis(), hour, minute)
        preferences.triggerAtMs = triggerAt
        val operation = fireIntent
        if (canScheduleExact()) {
            alarmManager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAt, showIntent()), operation)
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, operation)
            }
        }
    }

    private fun showIntent(): PendingIntent = PendingIntent.getActivity(
        appContext,
        REQUEST_CODE,
        Intent(appContext, MainActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    companion object {
        private const val REQUEST_CODE = 7301
    }
}
