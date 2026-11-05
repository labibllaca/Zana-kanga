package com.labix

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.labix.navirom.update.UpdateManager
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AutoUpdateDefaultTest {
    @Test
    fun autoCheckDefaultsOffWhenNoPreferenceHasBeenSaved() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = context.getSharedPreferences("navirom_update_prefs", 0)
        val existing = preferences.all
        preferences.edit().remove("auto_check_updates").commit()

        try {
            assertFalse(UpdateManager(context).autoCheckEnabled.value)
        } finally {
            val editor = preferences.edit().remove("auto_check_updates")
            existing["auto_check_updates"]?.let { editor.putBoolean("auto_check_updates", it as Boolean) }
            editor.commit()
        }
    }
}
