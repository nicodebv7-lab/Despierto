package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppRepository
import com.example.data.PreferencesManager
import com.example.model.AppItem
import com.example.model.KeepAliveConfig
import com.example.model.LogEntry
import com.example.service.KeepAliveService
import com.example.service.ServiceEventBus
import com.example.service.ServiceState
import com.example.util.PowerExemptionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppFilterType {
    ALL, USER, SYSTEM
}

data class MainUiState(
    val serviceState: ServiceState = ServiceState(),
    val installedApps: List<AppItem> = emptyList(),
    val filteredApps: List<AppItem> = emptyList(),
    val isLoadingApps: Boolean = true,
    val searchQuery: String = "",
    val filterType: AppFilterType = AppFilterType.ALL,
    val config: KeepAliveConfig = KeepAliveConfig(),
    val isBatteryExempt: Boolean = false,
    val totalWakeMinutes: Long = 0L,
    val totalHistoricalPulses: Int = 0
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val appRepo = AppRepository(application)
    private val prefsManager = PreferencesManager(application)

    private val _searchQuery = MutableStateFlow("")
    private val _filterType = MutableStateFlow(AppFilterType.ALL)
    private val _rawApps = MutableStateFlow<List<AppItem>>(emptyList())
    private val _isLoadingApps = MutableStateFlow(true)
    private val _config = MutableStateFlow(prefsManager.getConfig())
    private val _isBatteryExempt = MutableStateFlow(
        PowerExemptionHelper.isIgnoringBatteryOptimizations(application)
    )

    val serviceState: StateFlow<ServiceState> = ServiceEventBus.serviceState
    val logs: StateFlow<List<LogEntry>> = ServiceEventBus.logs

    private val _filteredApps = combine(_rawApps, _searchQuery, _filterType) { apps, query, filter ->
        apps.filter { app ->
            val matchesQuery = query.isBlank() ||
                app.appName.contains(query, ignoreCase = true) ||
                app.packageName.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                AppFilterType.ALL -> true
                AppFilterType.USER -> !app.isSystemApp
                AppFilterType.SYSTEM -> app.isSystemApp
            }

            matchesQuery && matchesFilter
        }
    }

    val uiState: StateFlow<MainUiState> = combine(
        serviceState,
        _filteredApps,
        _isLoadingApps,
        _config,
        _isBatteryExempt
    ) { serviceState, filtered, loading, config, exempt ->
        MainUiState(
            serviceState = serviceState,
            installedApps = _rawApps.value,
            filteredApps = filtered,
            isLoadingApps = loading,
            searchQuery = _searchQuery.value,
            filterType = _filterType.value,
            config = config,
            isBatteryExempt = exempt,
            totalWakeMinutes = prefsManager.getTotalWakeMinutes(),
            totalHistoricalPulses = prefsManager.getTotalPulses()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState()
    )

    init {
        loadInstalledApps()
        checkInitialTargetApp()
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoadingApps.value = true
            val apps = appRepo.getInstalledLaunchableApps()
            _rawApps.value = apps
            _isLoadingApps.value = false

            // If no target app was selected yet, default to first popular or user app
            val currentConfig = _config.value
            if (currentConfig.targetPackageName.isBlank() && apps.isNotEmpty()) {
                val defaultApp = apps.firstOrNull { !it.isSystemApp } ?: apps.first()
                selectTargetApp(defaultApp)
            }
        }
    }

    private fun checkInitialTargetApp() {
        val saved = prefsManager.getConfig()
        _config.value = saved
    }

    fun searchApps(query: String) {
        _searchQuery.value = query
    }

    fun setFilterType(filterType: AppFilterType) {
        _filterType.value = filterType
    }

    fun selectTargetApp(app: AppItem) {
        val updated = _config.value.copy(
            targetPackageName = app.packageName,
            targetAppName = app.appName
        )
        _config.value = updated
        prefsManager.saveTargetApp(app.packageName, app.appName)

        // If service is running, update the live service state
        if (serviceState.value.isRunning) {
            ServiceEventBus.updateState {
                it.copy(
                    targetPackage = app.packageName,
                    targetAppName = app.appName
                )
            }
        }
    }

    fun toggleService() {
        val context = getApplication<Application>()
        if (serviceState.value.isRunning) {
            KeepAliveService.stopService(context)
        } else {
            val targetPkg = _config.value.targetPackageName
            val targetName = _config.value.targetAppName
            if (targetPkg.isNotBlank()) {
                KeepAliveService.startService(context, targetPkg, targetName)
            }
        }
    }

    fun launchTargetApp() {
        val targetPkg = _config.value.targetPackageName
        if (targetPkg.isNotBlank()) {
            appRepo.launchApp(targetPkg)
        }
    }

    fun launchAppByPackage(packageName: String) {
        appRepo.launchApp(packageName)
    }

    fun refreshBatteryStatus() {
        _isBatteryExempt.value = PowerExemptionHelper.isIgnoringBatteryOptimizations(getApplication())
    }

    fun updateConfig(update: (KeepAliveConfig) -> KeepAliveConfig) {
        val newConfig = update(_config.value)
        _config.value = newConfig
        prefsManager.updateConfig(newConfig)
    }

    fun clearLogs() {
        ServiceEventBus.clearLogs()
    }

    fun getAppIcon(packageName: String) = appRepo.getAppIconBitmap(packageName)
}
