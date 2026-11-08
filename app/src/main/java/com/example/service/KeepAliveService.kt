package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.StayAwakeApp
import com.example.data.AppRepository
import com.example.data.PreferencesManager
import com.example.model.LogType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class KeepAliveService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private var heartbeatJob: Job? = null

    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var prefsManager: PreferencesManager
    private lateinit var appRepo: AppRepository

    private var startTimeMillis: Long = 0L
    private var pulsesCount: Int = 0
    private var targetPackage: String = ""
    private var targetAppName: String = ""
    private var intervalSeconds: Int = 30
    private var keepCpuAwake: Boolean = true

    override fun onCreate() {
        super.onCreate()
        prefsManager = PreferencesManager(this)
        appRepo = AppRepository(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_STOP -> {
                stopGuardianService()
                return START_NOT_STICKY
            }
            ACTION_RELAUNCH_TARGET -> {
                if (targetPackage.isNotEmpty()) {
                    appRepo.launchApp(targetPackage)
                }
                return START_STICKY
            }
            ACTION_START -> {
                startGuardianService(intent)
            }
        }

        return START_STICKY
    }

    @SuppressLint("WakelockTimeout")
    private fun startGuardianService(intent: Intent?) {
        val config = prefsManager.getConfig()
        targetPackage = intent?.getStringExtra(EXTRA_TARGET_PACKAGE)
            ?: config.targetPackageName.ifEmpty { "com.example.none" }
        targetAppName = intent?.getStringExtra(EXTRA_TARGET_APP_NAME)
            ?: config.targetAppName.ifEmpty { "App Seleccionada" }
        intervalSeconds = config.heartbeatIntervalSeconds.coerceAtLeast(10)
        keepCpuAwake = config.keepCpuAwake

        startTimeMillis = System.currentTimeMillis()
        pulsesCount = 0

        // 1. Acquire WakeLock if enabled
        if (keepCpuAwake) {
            try {
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "StayAwake:GuardianCpuWakeLock"
                ).apply {
                    setReferenceCounted(false)
                    acquire()
                }
                ServiceEventBus.addLog(
                    LogType.WAKELOCK,
                    "WakeLock CPU adquirido. El procesador permanecerá activo con la pantalla bloqueada."
                )
            } catch (e: Exception) {
                ServiceEventBus.addLog(
                    LogType.WARNING,
                    "No se pudo adquirir WakeLock: ${e.message}"
                )
            }
        }

        // 2. Start Foreground Notification
        val notification = buildNotification("Iniciando protección de $targetAppName...", pulsesCount)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                StayAwakeApp.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(StayAwakeApp.NOTIFICATION_ID, notification)
        }

        // 3. Update EventBus state
        ServiceEventBus.updateState {
            it.copy(
                isRunning = true,
                startTimeMillis = startTimeMillis,
                pulsesCount = 0,
                targetPackage = targetPackage,
                targetAppName = targetAppName,
                isWakeLockHeld = wakeLock?.isHeld == true
            )
        }

        ServiceEventBus.addLog(
            LogType.SYSTEM,
            "Modo Guardián ACTIVO protegiendo '$targetAppName' (Intervalo: ${intervalSeconds}s)."
        )

        // 4. Start Heartbeat pulse loop
        startHeartbeatLoop()
    }

    private fun startHeartbeatLoop() {
        heartbeatJob?.cancel()
        heartbeatJob = serviceScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(intervalSeconds * 1000L)
                pulsesCount++

                ServiceEventBus.updateState {
                    it.copy(
                        pulsesCount = pulsesCount,
                        isWakeLockHeld = wakeLock?.isHeld == true
                    )
                }

                // Periodic event logging every few pulses to not flood the logs
                if (pulsesCount == 1 || pulsesCount % 5 == 0) {
                    ServiceEventBus.addLog(
                        LogType.PULSE,
                        "Pulso #$pulsesCount: CPU activo en segundo plano. Protección activa."
                    )
                }

                // Update notification text every 2 pulses
                if (pulsesCount % 2 == 0) {
                    val updatedNotification = buildNotification(
                        "Protección activa: CPU despierto ($pulsesCount pulsos)",
                        pulsesCount
                    )
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
                    manager.notify(StayAwakeApp.NOTIFICATION_ID, updatedNotification)
                }
            }
        }
    }

    private fun buildNotification(statusText: String, pulses: Int): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, KeepAliveService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, StayAwakeApp.CHANNEL_ID)
            .setContentTitle("🛡️ StayAwake: $targetAppName")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Detener",
                stopPendingIntent
            )

        // If target package is valid, add launch shortcut action
        if (targetPackage.isNotBlank() && targetPackage != "com.example.none") {
            val launchIntent = packageManager.getLaunchIntentForPackage(targetPackage)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                val launchPendingIntent = PendingIntent.getActivity(
                    this,
                    2,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                builder.addAction(
                    android.R.drawable.ic_media_play,
                    "Abrir App",
                    launchPendingIntent
                )
            }
        }

        return builder.build()
    }

    private fun stopGuardianService() {
        heartbeatJob?.cancel()
        heartbeatJob = null

        val durationMillis = if (startTimeMillis > 0) System.currentTimeMillis() - startTimeMillis else 0L
        val durationMinutes = durationMillis / 60000L

        prefsManager.addTotalWakeMinutes(durationMinutes)
        prefsManager.addTotalPulses(pulsesCount)

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                ServiceEventBus.addLog(LogType.WAKELOCK, "WakeLock CPU liberado con éxito.")
            }
        } catch (_: Exception) {}
        wakeLock = null

        ServiceEventBus.updateState {
            it.copy(
                isRunning = false,
                isWakeLockHeld = false
            )
        }

        ServiceEventBus.addLog(
            LogType.SYSTEM,
            "Modo Guardián detenido. Duración: ${durationMinutes} min, Pulsos: $pulsesCount."
        )

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopGuardianService()
        serviceJob.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.example.stayawake.ACTION_START"
        const val ACTION_STOP = "com.example.stayawake.ACTION_STOP"
        const val ACTION_RELAUNCH_TARGET = "com.example.stayawake.ACTION_RELAUNCH_TARGET"

        const val EXTRA_TARGET_PACKAGE = "extra_target_package"
        const val EXTRA_TARGET_APP_NAME = "extra_target_app_name"

        fun startService(context: Context, packageName: String, appName: String) {
            val intent = Intent(context, KeepAliveService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_TARGET_PACKAGE, packageName)
                putExtra(EXTRA_TARGET_APP_NAME, appName)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, KeepAliveService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
