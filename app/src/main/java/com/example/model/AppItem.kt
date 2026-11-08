package com.example.model

data class AppItem(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean = false,
    val versionName: String = ""
)
