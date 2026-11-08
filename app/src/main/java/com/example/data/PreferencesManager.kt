package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.KeepAliveConfig

class PreferencesManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getConfig(): KeepAliveConfig {
        return KeepAliveConfig(
            targetPackageName = prefs.getString(KEY_TARGET_PACKAGE, "") ?: "",
            targetAppName = prefs.getString(KEY_TARGET_APP_NAME, "") ?: "",
            keepCpuAwake = prefs.getBoolean(KEY_KEEP_CPU_AWAKE, true),
            heartbeatIntervalSeconds = prefs.getInt(KEY_HEARTBEAT_INTERVAL, 30),
            preventScreenSleep = prefs.getBoolean(KEY_PREVENT_SCREEN_SLEEP, false),
            soundAlertOnKill = prefs.getBoolean(KEY_SOUND_ALERT_ON_KILL, false),
            autoStartOnBoot = prefs.getBoolean(KEY_AUTO_START_BOOT, false)
        )
    }

    fun saveTargetApp(packageName: String, appName: String) {
        prefs.edit()
            .putString(KEY_TARGET_PACKAGE, packageName)
            .putString(KEY_TARGET_APP_NAME, appName)
            .apply()
    }

    fun updateConfig(config: KeepAliveConfig) {
        prefs.edit()
            .putString(KEY_TARGET_PACKAGE, config.targetPackageName)
            .putString(KEY_TARGET_APP_NAME, config.targetAppName)
            .putBoolean(KEY_KEEP_CPU_AWAKE, config.keepCpuAwake)
            .putInt(KEY_HEARTBEAT_INTERVAL, config.heartbeatIntervalSeconds)
            .putBoolean(KEY_PREVENT_SCREEN_SLEEP, config.preventScreenSleep)
            .putBoolean(KEY_SOUND_ALERT_ON_KILL, config.soundAlertOnKill)
            .putBoolean(KEY_AUTO_START_BOOT, config.autoStartOnBoot)
            .apply()
    }

    fun addTotalWakeMinutes(minutes: Long) {
        val current = prefs.getLong(KEY_TOTAL_WAKE_MINUTES, 0L)
        prefs.edit().putLong(KEY_TOTAL_WAKE_MINUTES, current + minutes).apply()
    }

    fun getTotalWakeMinutes(): Long = prefs.getLong(KEY_TOTAL_WAKE_MINUTES, 0L)

    fun addTotalPulses(count: Int) {
        val current = prefs.getInt(KEY_TOTAL_PULSES, 0)
        prefs.edit().putInt(KEY_TOTAL_PULSES, current + count).apply()
    }

    fun getTotalPulses(): Int = prefs.getInt(KEY_TOTAL_PULSES, 0)

    companion object {
        private const val PREFS_NAME = "stayawake_prefs"
        private const val KEY_TARGET_PACKAGE = "target_package"
        private const val KEY_TARGET_APP_NAME = "target_app_name"
        private const val KEY_KEEP_CPU_AWAKE = "keep_cpu_awake"
        private const val KEY_HEARTBEAT_INTERVAL = "heartbeat_interval"
        private const val KEY_PREVENT_SCREEN_SLEEP = "prevent_screen_sleep"
        private const val KEY_SOUND_ALERT_ON_KILL = "sound_alert_on_kill"
        private const val KEY_AUTO_START_BOOT = "auto_start_boot"
        private const val KEY_TOTAL_WAKE_MINUTES = "total_wake_minutes"
        private const val KEY_TOTAL_PULSES = "total_pulses"
    }
}
