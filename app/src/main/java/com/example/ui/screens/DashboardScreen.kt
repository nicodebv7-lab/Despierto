package com.example.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MainUiState
import com.example.ui.MainViewModel
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.DangerCoral
import com.example.ui.theme.DeepMidnight
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardHover
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import com.example.util.PowerExemptionHelper
import kotlinx.coroutines.delay

@Composable
fun DashboardScreen(
    uiState: MainUiState,
    viewModel: MainViewModel,
    onNavigateToApps: () -> Unit,
    onNavigateToPower: () -> Unit
) {
    val context = LocalContext.current
    val isRunning = uiState.serviceState.isRunning

    // Chronometer for elapsed active time
    var elapsedSeconds by remember { mutableLongStateOf(0L) }
    LaunchedEffect(isRunning, uiState.serviceState.startTimeMillis) {
        if (isRunning && uiState.serviceState.startTimeMillis > 0) {
            while (true) {
                elapsedSeconds = (System.currentTimeMillis() - uiState.serviceState.startTimeMillis) / 1000L
                delay(1000L)
            }
        } else {
            elapsedSeconds = 0L
        }
    }

    // Glowing animation for active shield
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            HeaderStatusCard(
                isRunning = isRunning,
                pulseScale = if (isRunning) pulseScale else 1f,
                targetAppName = uiState.config.targetAppName.ifBlank { "Ninguna app seleccionada" },
                onToggleService = { viewModel.toggleService() }
            )
        }

        // Battery optimization warning banner if not exempt
        if (!uiState.isBatteryExempt) {
            item {
                BatteryWarningCard(
                    onRequestExemption = {
                        try {
                            val intent = PowerExemptionHelper.createRequestExemptionIntent(context)
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            try {
                                val intent = PowerExemptionHelper.createIgnoreBatterySettingsIntent()
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                        }
                    },
                    onLearnMore = onNavigateToPower
                )
            }
        }

        // Target App Hero Card
        item {
            TargetAppHeroCard(
                targetAppName = uiState.config.targetAppName,
                targetPackageName = uiState.config.targetPackageName,
                targetIcon = if (uiState.config.targetPackageName.isNotBlank()) {
                    viewModel.getAppIcon(uiState.config.targetPackageName)
                } else null,
                isRunning = isRunning,
                onOpenApp = { viewModel.launchTargetApp() },
                onChangeApp = onNavigateToApps
            )
        }

        // Live Telemetry Grid
        item {
            Text(
                text = "TELEMETRÍA EN TIEMPO REAL",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = CyberCyan
                ),
                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 4.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TelemetryCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Timer,
                        iconTint = CyberCyan,
                        title = "Tiempo Activo",
                        value = if (isRunning) formatElapsedTime(elapsedSeconds) else "00:00:00",
                        subtitle = if (isRunning) "En ejecución" else "En pausa"
                    )
                    TelemetryCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.ElectricBolt,
                        iconTint = CyberEmerald,
                        title = "Pulsos de CPU",
                        value = if (isRunning) "${uiState.serviceState.pulsesCount}" else "0",
                        subtitle = "Cada ${uiState.config.heartbeatIntervalSeconds}s"
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TelemetryCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Lock,
                        iconTint = if (uiState.serviceState.isWakeLockHeld) CyberEmerald else TextMuted,
                        title = "CPU Pantalla Bloqueada",
                        value = if (uiState.serviceState.isWakeLockHeld) "WakeLock ACTIVO" else "Inactivo",
                        subtitle = "Protege contra Doze"
                    )
                    TelemetryCard(
                        modifier = Modifier.weight(1f),
                        icon = if (uiState.isBatteryExempt) Icons.Default.CheckCircle else Icons.Default.BatteryAlert,
                        iconTint = if (uiState.isBatteryExempt) CyberEmerald else WarningAmber,
                        title = "Ahorro Batería",
                        value = if (uiState.isBatteryExempt) "Sin Restricción" else "Optimizado",
                        subtitle = if (uiState.isBatteryExempt) "Configuración Óptima" else "Toca para ajustar",
                        onClick = onNavigateToPower
                    )
                }
            }
        }

        // Quick Explanatory Feature Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "¿Cómo mantiene despierta tu aplicación?",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "1. Mantiene un WakeLock de CPU para que el procesador no se apague al bloquear la pantalla.\n" +
                               "2. Envía pulsos periódicos de actividad en primer plano que evitan que Android cierre sockets de red o congele la app.\n" +
                               "3. El acceso rápido en la barra de notificaciones te permite reabrir tu app con un solo toque.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun HeaderStatusCard(
    isRunning: Boolean,
    pulseScale: Float,
    targetAppName: String,
    onToggleService: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isRunning) CyberEmerald else SurfaceBorder,
        animationSpec = tween(500),
        label = "border_color"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, RoundedCornerShape(24.dp)),
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning) Color(0xFF0F1E28) else SurfaceCard
        ),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = if (isRunning) {
                                listOf(CyberEmerald.copy(alpha = 0.35f), Color.Transparent)
                            } else {
                                listOf(SurfaceCardHover, Color.Transparent)
                            }
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(if (isRunning) CyberEmerald else SurfaceCardHover)
                        .border(
                            1.5.dp,
                            if (isRunning) CyberCyan else SurfaceBorder,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Shield else Icons.Default.PowerSettingsNew,
                        contentDescription = "Estado del servicio",
                        tint = if (isRunning) DeepMidnight else TextSecondary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = if (isRunning) "MODO GUARDIÁN ACTIVO" else "MODO GUARDIÁN EN ESPERA",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.2.sp
                ),
                color = if (isRunning) CyberEmerald else TextSecondary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = if (isRunning) {
                    "Protegiendo '$targetAppName' contra suspensión y bloqueo"
                } else {
                    "Activa el interruptor para mantener tu app activa en segundo plano"
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                modifier = Modifier.padding(horizontal = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceDark)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (isRunning) "Servicio Funcionando" else "Servicio Detenido",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary
                    )
                    Text(
                        text = if (isRunning) "CPU despierto con pantalla bloqueada" else "Toca para iniciar protección",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }

                Switch(
                    checked = isRunning,
                    onCheckedChange = { onToggleService() },
                    modifier = Modifier.testTag("service_toggle_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DeepMidnight,
                        checkedTrackColor = CyberEmerald,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = SurfaceBorder
                    )
                )
            }
        }
    }
}

@Composable
fun TargetAppHeroCard(
    targetAppName: String,
    targetPackageName: String,
    targetIcon: Bitmap?,
    isRunning: Boolean,
    onOpenApp: () -> Unit,
    onChangeApp: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "APLICACIÓN OBJETIVO",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = CyberCyan
                    )
                )

                Text(
                    text = "Cambiar",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = ElectricBlue
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onChangeApp() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("change_app_button")
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceDark)
                        .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (targetIcon != null) {
                        Image(
                            bitmap = targetIcon.asImageBitmap(),
                            contentDescription = targetAppName,
                            modifier = Modifier.size(42.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = CyberCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = targetAppName.ifBlank { "Ninguna app seleccionada" },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = targetPackageName.ifBlank { "Toca 'Cambiar' para elegir una app" },
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenApp,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("launch_target_button"),
                    enabled = targetPackageName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CyberCyan,
                        contentColor = DeepMidnight
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Abrir App Ahora",
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onChangeApp,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TelemetryCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary,
                    maxLines = 1
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                ),
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                maxLines = 1
            )
        }
    }
}

@Composable
fun BatteryWarningCard(
    onRequestExemption: () -> Unit,
    onLearnMore: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF281C08)),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = WarningAmber,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Ahorro de batería activo",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = WarningAmber
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Android puede detener StayAwake al apagar la pantalla salvo que concedas la exención de optimización de energía.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFFDE68A)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onRequestExemption,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = WarningAmber,
                        contentColor = DeepMidnight
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("request_exemption_button")
                ) {
                    Text(
                        text = "Conceder Exención",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                FilledTonalButton(
                    onClick = onLearnMore,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Guía de ajustes", fontSize = 13.sp)
                }
            }
        }
    }
}

fun formatElapsedTime(seconds: Long): String {
    val hrs = seconds / 3600
    val mins = (seconds % 3600) / 60
    val secs = seconds % 60
    return String.format("%02d:%02d:%02d", hrs, mins, secs)
}
