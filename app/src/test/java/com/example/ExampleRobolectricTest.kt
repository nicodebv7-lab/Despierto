package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.PreferencesManager
import com.example.model.KeepAliveConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("StayAwake", appName)
    }

    @Test
    fun `save and retrieve config from preferences`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = PreferencesManager(context)

        prefs.saveTargetApp("com.target.music", "Music Player")
        val config = prefs.getConfig()

        assertEquals("com.target.music", config.targetPackageName)
        assertEquals("Music Player", config.targetAppName)
        assertTrue(config.keepCpuAwake)
    }
}
