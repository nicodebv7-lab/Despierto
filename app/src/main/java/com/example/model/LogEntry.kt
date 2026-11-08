package com.example.model

enum class LogType {
    INFO,
    PULSE,
    WAKELOCK,
    WARNING,
    SYSTEM
}

data class LogEntry(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val type: LogType,
    val message: String
)
