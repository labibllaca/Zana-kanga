package com.labix.navirom.player

import java.util.Calendar
import java.util.TimeZone

/** Wall-clock helpers for the one-shot wake alarm. */
object WakeAlarmTime {

    /** Next occurrence of [hour]:[minute] strictly after [nowMs] in [zone]. */
    fun nextTriggerAt(
        nowMs: Long,
        hour: Int,
        minute: Int,
        zone: TimeZone = TimeZone.getDefault()
    ): Long {
        val candidate = calendarAt(nowMs, hour, minute, zone)
        if (candidate.timeInMillis <= nowMs) {
            candidate.add(Calendar.DAY_OF_YEAR, 1)
            // Re-apply the wall clock time in case the day shift crossed a DST boundary.
            candidate.set(Calendar.HOUR_OF_DAY, hour)
            candidate.set(Calendar.MINUTE, minute)
        }
        return candidate.timeInMillis
    }

    /** True when today's [hour]:[minute] has been reached and is not in the future. */
    fun isDue(
        nowMs: Long,
        hour: Int,
        minute: Int,
        zone: TimeZone = TimeZone.getDefault()
    ): Boolean = calendarAt(nowMs, hour, minute, zone).timeInMillis <= nowMs

    /**
     * True only once the armed instant (persisted when the alarm was scheduled) is
     * reached. A missing stamp is never due, so a reboot cannot fire an unarmed alarm.
     */
    fun isTriggerDue(nowMs: Long, triggerAtMs: Long): Boolean =
        triggerAtMs > 0L && nowMs >= triggerAtMs

    private fun calendarAt(nowMs: Long, hour: Int, minute: Int, zone: TimeZone): Calendar =
        Calendar.getInstance(zone).apply {
            timeInMillis = nowMs
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
}
