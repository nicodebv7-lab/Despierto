package com.example.util

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

data class ManufacturerGuide(
    val brandName: String,
    val title: String,
    val steps: List<String>,
    val tips: String,
    val autostartIntent: Intent? = null
)

object PowerExemptionHelper {

    fun isIgnoringBatteryOptimizations(context: Context, packageName: String = context.packageName): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return false
        return powerManager.isIgnoringBatteryOptimizations(packageName)
    }

    @SuppressLint("BatteryLife")
    fun createRequestExemptionIntent(context: Context): Intent {
        return Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun createIgnoreBatterySettingsIntent(): Intent {
        return Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun createAppDetailsIntent(packageName: String): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun getDetectedBrand(): String {
        return Build.MANUFACTURER.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    fun getManufacturerGuide(context: Context, targetAppName: String = "tu app"): ManufacturerGuide {
        val manufacturer = Build.MANUFACTURER.lowercase()

        return when {
            manufacturer.contains("samsung") -> ManufacturerGuide(
                brandName = "Samsung (One UI)",
                title = "Configuración para evitar cierre en Samsung",
                steps = listOf(
                    "1. Ve a Ajustes > Cuidado del dispositivo > Batería.",
                    "2. Pulsa en 'Límites de uso en segundo plano'.",
                    "3. Entra a 'Apps que nunca se suspenden' y añade tanto StayAwake como $targetAppName.",
                    "4. En la pantalla de aplicaciones recientes (multitarea), toca el icono de $targetAppName y selecciona 'Bloquear esta aplicación' (icono de candado)."
                ),
                tips = "One UI suspende agresivamente las aplicaciones si no se agregan a 'Nunca suspender'."
            )

            manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> ManufacturerGuide(
                brandName = "Xiaomi / Redmi / POCO (HyperOS / MIUI)",
                title = "Configuración para evitar cierre en MIUI / HyperOS",
                steps = listOf(
                    "1. Abre la app 'Seguridad' > Administrar aplicaciones.",
                    "2. Busca tanto StayAwake como $targetAppName.",
                    "3. Activa 'Inicio automático' (Autostart).",
                    "4. En 'Ahorro de batería', cambia la opción a 'Sin restricciones'.",
                    "5. Abre apps recientes, mantén presionada la tarjeta de $targetAppName y pulsa el candado."
                ),
                tips = "HyperOS y MIUI detienen procesos en segundo plano a los 10 minutos si no tienen 'Sin restricciones' y 'Inicio automático'.",
                autostartIntent = Intent().apply {
                    component = ComponentName(
                        "com.miui.securitycenter",
                        "com.miui.permcenter.autostart.AutoStartManagementActivity"
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )

            manufacturer.contains("huawei") || manufacturer.contains("honor") -> ManufacturerGuide(
                brandName = "Huawei / Honor (EMUI / MagicOS)",
                title = "Configuración para EMUI / MagicOS",
                steps = listOf(
                    "1. Ve a Ajustes > Batería > Inicio de aplicaciones.",
                    "2. Busca StayAwake y $targetAppName.",
                    "3. Cambia de 'Gestionar automáticamente' a 'Gestionar manualmente'.",
                    "4. Asegúrate de activar los 3 interruptores: 'Inicio automático', 'Inicio secundario' y 'Ejecutar en segundo plano'."
                ),
                tips = "EMUI detiene los servicios en segundo plano agresivamente cuando la pantalla se apaga."
            )

            manufacturer.contains("oneplus") || manufacturer.contains("oppo") || manufacturer.contains("realme") -> ManufacturerGuide(
                brandName = "OnePlus / OPPO / Realme (ColorOS / OxygenOS)",
                title = "Configuración para ColorOS y OxygenOS",
                steps = listOf(
                    "1. Ve a Ajustes > Batería > Más ajustes > Optimizar uso de batería.",
                    "2. Selecciona StayAwake y $targetAppName y elige 'No optimizar'.",
                    "3. Ve a Ajustes > Aplicaciones > Gestión de aplicaciones > $targetAppName.",
                    "4. Entra a 'Uso de batería' y activa 'Permitir actividad en segundo plano' y 'Permitir inicio automático'."
                ),
                tips = "OxygenOS y ColorOS congelan los sockets de red al apagar la pantalla salvo en modo sin restricciones."
            )

            manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> ManufacturerGuide(
                brandName = "vivo / iQOO (Funtouch OS)",
                title = "Configuración para Funtouch OS",
                steps = listOf(
                    "1. Ve a Ajustes > Batería > Alto consumo de energía en segundo plano.",
                    "2. Permite la ejecución de StayAwake y $targetAppName.",
                    "3. En Administrador de aplicaciones, activa el Inicio automático."
                ),
                tips = "Funtouch OS corta procesos con alto consumo si no se agrega a la lista blanca."
            )

            else -> ManufacturerGuide(
                brandName = "Android Estándar / ${getDetectedBrand()}",
                title = "Configuración recomendada del sistema",
                steps = listOf(
                    "1. Concede la exención de optimización de batería a StayAwake desde el botón de la app.",
                    "2. Abre la información de $targetAppName (Ajustes > Apps > $targetAppName).",
                    "3. En 'Batería', selecciona 'Sin restricciones' (Unrestricted).",
                    "4. Desactiva 'Pausar actividad de apps si no se usan'.",
                    "5. Mantén activo el Modo Guardián de StayAwake antes de bloquear el teléfono."
                ),
                tips = "El WakeLock de StayAwake mantendrá la CPU despierta y evitará que el modo Doze de Android congele tu app con la pantalla bloqueada."
            )
        }
    }
}
