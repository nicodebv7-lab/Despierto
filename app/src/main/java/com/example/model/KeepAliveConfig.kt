package com.example.model

data class KeepAliveConfig(
    val targetPackageName: String = "",
    val targetAppName: String = "",
    val keepCpuAwake: Boolean = true,
    val heartbeatIntervalSeconds: Int = 30,
    val preventScreenSleep: Boolean = false,
    val soundAlertOnKill: Boolean = false,
    val autoStartOnBoot: Boolean = false
)
