package com.example.service

import com.example.model.LogEntry
import com.example.model.LogType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ServiceState(
    val isRunning: Boolean = false,
    val startTimeMillis: Long = 0L,
    val pulsesCount: Int = 0,
    val targetPackage: String = "",
    val targetAppName: String = "",
    val isWakeLockHeld: Boolean = false
)

object ServiceEventBus {

    private val _serviceState = MutableStateFlow(ServiceState())
    val serviceState: StateFlow<ServiceState> = _serviceState.asStateFlow()

    private val _logs = MutableStateFlow<List<LogEntry>>(
        listOf(
            LogEntry(
                type = LogType.SYSTEM,
                message = "Sistema StayAwake inicializado y listo."
            )
        )
    )
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun updateState(transform: (ServiceState) -> ServiceState) {
        _serviceState.update(transform)
    }

    fun addLog(type: LogType, message: String) {
        val entry = LogEntry(type = type, message = message)
        _logs.update { current ->
            // Keep the last 100 entries
            (listOf(entry) + current).take(100)
        }
    }

    fun clearLogs() {
        _logs.value = listOf(
            LogEntry(
                type = LogType.SYSTEM,
                message = "Registro de actividad limpiado."
            )
        )
    }
}
