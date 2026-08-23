package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.JarvisDeviceAdminReceiver
import com.example.ui.theme.CyanJarvis
import com.example.ui.theme.GreenSecure
import com.example.ui.theme.TextSlate

@Composable
fun DeviceAdminAndBackgroundSection(
    persistentBackgroundEnabled: Boolean,
    onTogglePersistentBackground: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isAdminActive by remember { mutableStateOf(JarvisDeviceAdminReceiver.isDeviceAdminActive(context)) }

    fun checkBatteryOptimized(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        return powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    var isBatteryIgnoringOptimizations by remember { mutableStateOf(checkBatteryOptimized()) }

    // Refresh status on lifecycle ON_RESUME
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAdminActive = JarvisDeviceAdminReceiver.isDeviceAdminActive(context)
                isBatteryIgnoringOptimizations = checkBatteryOptimized()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Text(
            text = "SYSTEM ADMIN & 24/7 BACKGROUND EXECUTION",
            color = CyanJarvis,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            fontFamily = FontFamily.Monospace
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 1. Device Administrator Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = if (isAdminActive) GreenSecure.copy(alpha = 0.08f) else Color(0xFFF59E0B).copy(alpha = 0.08f),
            border = BorderStroke(
                1.dp,
                if (isAdminActive) GreenSecure.copy(alpha = 0.4f) else Color(0xFFF59E0B).copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = if (isAdminActive) GreenSecure.copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AdminPanelSettings,
                                contentDescription = "Device Admin",
                                tint = if (isAdminActive) GreenSecure else Color(0xFFF59E0B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Device Administrator",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isAdminActive) "System Privilege Active" else "Privileges Not Granted",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isAdminActive) GreenSecure else Color(0xFFF59E0B)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            try {
                                if (isAdminActive) {
                                    val intent = Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                } else {
                                    val intent = JarvisDeviceAdminReceiver.getAddDeviceAdminIntent(context)
                                    context.startActivity(intent)
                                }
                            } catch (e: Exception) {
                                Toast.makeText(context, "Opening security settings...", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAdminActive) GreenSecure else CyanJarvis
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isAdminActive) "Active" else "Enable Admin",
                            color = Color(0xFF03070E),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Grants J.A.R.V.I.S. Android Device Administrator rights for lock screen control, keyguard security management, and automated background execution.",
                    fontSize = 12.sp,
                    color = TextSlate,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Battery Optimization (Ignore Battery Optimizations) Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = if (isBatteryIgnoringOptimizations) GreenSecure.copy(alpha = 0.08f) else Color(0xFF3B82F6).copy(alpha = 0.08f),
            border = BorderStroke(
                1.dp,
                if (isBatteryIgnoringOptimizations) GreenSecure.copy(alpha = 0.4f) else Color(0xFF3B82F6).copy(alpha = 0.4f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = if (isBatteryIgnoringOptimizations) GreenSecure.copy(alpha = 0.2f) else Color(0xFF3B82F6).copy(alpha = 0.2f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BatteryChargingFull,
                                contentDescription = "Battery Optimization",
                                tint = if (isBatteryIgnoringOptimizations) GreenSecure else Color(0xFF3B82F6),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Battery Optimization",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isBatteryIgnoringOptimizations) "Unrestricted 24/7 Run" else "Optimized (OS May Sleep Service)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isBatteryIgnoringOptimizations) GreenSecure else Color(0xFF3B82F6)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                try {
                                    val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(fallbackIntent)
                                } catch (ex: Exception) {
                                    Toast.makeText(context, "Open battery settings manually", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (isBatteryIgnoringOptimizations) GreenSecure else Color(0xFF3B82F6)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = if (isBatteryIgnoringOptimizations) "Unrestricted" else "Request 24/7",
                            color = if (isBatteryIgnoringOptimizations) GreenSecure else Color(0xFF3B82F6),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Bypasses Android Doze mode so J.A.R.V.I.S. remains listening for voice triggers 24 hours a day without background hibernation.",
                    fontSize = 12.sp,
                    color = TextSlate,
                    lineHeight = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Persistent Service & Auto-Start on Boot Toggle
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, CyanJarvis.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = CyanJarvis.copy(alpha = 0.15f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Persistent Background Run",
                            tint = CyanJarvis,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Auto-Start & Keep-Alive",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Auto-starts on reboot & re-launches if app is cleared",
                            fontSize = 11.sp,
                            color = TextSlate,
                            lineHeight = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Switch(
                    checked = persistentBackgroundEnabled,
                    onCheckedChange = onTogglePersistentBackground,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = CyanJarvis,
                        checkedTrackColor = CyanJarvis.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}
