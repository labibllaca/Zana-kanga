package com.labix

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.labix.navirom.data.model.SleepTimerOptions
import com.labix.navirom.data.local.SleepTimerOptionsPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SleepTimerOptionsPersistenceTest {
    @Test
    fun gpsDefaultsOffAndAllShutdownChoicesRoundTrip() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = context.getSharedPreferences("sleep_timer_options_test", 0)
        preferences.edit().clear().commit()

        try {
            assertFalse(SleepTimerOptionsPreferences.load(preferences).disableGps)

            val selectedOptions = SleepTimerOptions(
                disableBluetooth = true,
                disableWifi = true,
                disableMobileData = true,
                disableGps = true
            )
            SleepTimerOptionsPreferences.save(preferences, selectedOptions)

            assertEquals(selectedOptions, SleepTimerOptionsPreferences.load(preferences))
        } finally {
            preferences.edit().clear().commit()
        }
    }
}
