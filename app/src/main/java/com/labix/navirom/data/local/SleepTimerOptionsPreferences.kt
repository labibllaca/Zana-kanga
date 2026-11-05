package com.labix.navirom.data.local

import android.content.SharedPreferences
import com.labix.navirom.data.model.SleepTimerOptions

object SleepTimerOptionsPreferences {
    fun load(preferences: SharedPreferences): SleepTimerOptions = SleepTimerOptions(
        disableBluetooth = preferences.getBoolean("sleep_timer_disable_bt", false),
        disableWifi = preferences.getBoolean("sleep_timer_disable_wifi", false),
        disableMobileData = preferences.getBoolean("sleep_timer_disable_mobile_data", false),
        disableGps = preferences.getBoolean("sleep_timer_disable_gps", false)
    )

    fun save(preferences: SharedPreferences, options: SleepTimerOptions) {
        preferences.edit()
            .putBoolean("sleep_timer_disable_bt", options.disableBluetooth)
            .putBoolean("sleep_timer_disable_wifi", options.disableWifi)
            .putBoolean("sleep_timer_disable_mobile_data", options.disableMobileData)
            .putBoolean("sleep_timer_disable_gps", options.disableGps)
            .apply()
    }
}
