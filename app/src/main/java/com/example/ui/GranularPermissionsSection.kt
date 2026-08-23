package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.JarvisDeviceAdminReceiver
import com.example.ui.theme.CyanJarvis
import com.example.ui.theme.GreenSecure
import com.example.ui.theme.TextSlate

data class GranularPermissionItem(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val permissions: List<String>,
    val isSpecial: Boolean = false,
    val activeColor: Color = CyanJarvis
)

@Composable
fun GranularPermissionsSection(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var selectedPermissionToRequest by remember { mutableStateOf<GranularPermissionItem?>(null) }

    fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun isItemGranted(item: GranularPermissionItem): Boolean {
        if (item.id == "accessibility") {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: ""
            return enabledServices.contains(context.packageName)
        }
        if (item.id == "admin") {
            return JarvisDeviceAdminReceiver.isDeviceAdminActive(context)
        }
        return item.permissions.isNotEmpty() && item.permissions.all { isPermissionGranted(it) }
    }

    // List of system permissions
    val permissionItems = remember {
        listOf(
            GranularPermissionItem(
                id = "microphone",
                name = "Microphone Access",
                description = "Required for hands-free voice commands & wake-word trigger",
                icon = Icons.Default.Mic,
                permissions = listOf(Manifest.permission.RECORD_AUDIO),
                activeColor = CyanJarvis
            ),
            GranularPermissionItem(
                id = "contacts",
                name = "Contacts",
                description = "Allows calling and texting contacts by name",
                icon = Icons.Default.Contacts,
                permissions = listOf(Manifest.permission.READ_CONTACTS),
                activeColor = Color(0xFF3B82F6)
            ),
            GranularPermissionItem(
                id = "sms",
                name = "SMS & Texting",
                description = "Enables reading & sending hands-free SMS messages",
                icon = Icons.Default.Sms,
                permissions = listOf(Manifest.permission.SEND_SMS, Manifest.permission.READ_SMS),
                activeColor = Color(0xFF10B981)
            ),
            GranularPermissionItem(
                id = "location",
                name = "Location",
                description = "Enables location-aware search, weather & navigation assistance",
                icon = Icons.Default.LocationOn,
                permissions = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                activeColor = Color(0xFFF59E0B)
            ),
            GranularPermissionItem(
                id = "phone",
                name = "Phone & Calling",
                description = "Initiates direct phone calls on voice command",
                icon = Icons.Default.Phone,
                permissions = listOf(Manifest.permission.CALL_PHONE, Manifest.permission.READ_PHONE_STATE),
                activeColor = Color(0xFF8B5CF6)
            ),
            GranularPermissionItem(
                id = "camera",
                name = "Camera Access",
                description = "Enables camera vision analysis & snapshot features",
                icon = Icons.Default.CameraAlt,
                permissions = listOf(Manifest.permission.CAMERA),
                activeColor = Color(0xFFEC4899)
            ),
            GranularPermissionItem(
                id = "notifications",
                name = "Notifications",
                description = "Posts system alerts, reminders & status notifications",
                icon = Icons.Default.Notifications,
                permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    listOf(Manifest.permission.POST_NOTIFICATIONS)
                } else emptyList(),
                activeColor = Color(0xFF06B6D4)
            ),
            GranularPermissionItem(
                id = "accessibility",
                name = "Accessibility Service",
                description = "Screen reading & hands-free app UI automation",
                icon = Icons.Default.AccessibilityNew,
                permissions = emptyList(),
                isSpecial = true,
                activeColor = Color(0xFFF97316)
            ),
            GranularPermissionItem(
                id = "admin",
                name = "Device Administrator",
                description = "System management & lock screen control privileges",
                icon = Icons.Default.AdminPanelSettings,
                permissions = emptyList(),
                isSpecial = true,
                activeColor = GreenSecure
            )
        )
    }

    var permissionStates by remember {
        mutableStateOf(permissionItems.associate { it.id to isItemGranted(it) })
    }

    fun refreshStates() {
        permissionStates = permissionItems.associate { it.id to isItemGranted(it) }
    }

    // Permission launcher for runtime array
    val multiPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshStates()
    }

    // Refresh state when coming back from system settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refreshStates()
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "GRANULAR PERMISSION CONTROL",
                    color = CyanJarvis,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Toggle individual features and system privileges",
                    fontSize = 11.sp,
                    color = TextSlate
                )
            }

            TextButton(
                onClick = {
                    try {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(context, "Opening App Settings...", Toast.LENGTH_SHORT).show()
                    }
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text("App Settings", fontSize = 11.sp, color = CyanJarvis)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        permissionItems.forEach { item ->
            val isGranted = permissionStates[item.id] == true

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(14.dp),
                color = if (isGranted) item.activeColor.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
                border = BorderStroke(
                    1.dp,
                    if (isGranted) item.activeColor.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
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
                                    color = if (isGranted) item.activeColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.name,
                                tint = if (isGranted) item.activeColor else TextSlate,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = item.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isGranted) "Granted" else "Disabled",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isGranted) item.activeColor else TextSlate
                                )
                            }
                            Text(
                                text = item.description,
                                fontSize = 11.sp,
                                color = TextSlate,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Switch(
                        checked = isGranted,
                        onCheckedChange = { checked ->
                            if (checked) {
                                when (item.id) {
                                    "accessibility" -> {
                                        try {
                                            context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            })
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Open Accessibility Settings", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    "admin" -> {
                                        try {
                                            context.startActivity(JarvisDeviceAdminReceiver.getAddDeviceAdminIntent(context))
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Open Security Settings", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    else -> {
                                        if (item.permissions.isNotEmpty()) {
                                            multiPermissionLauncher.launch(item.permissions.toTypedArray())
                                        }
                                    }
                                }
                            } else {
                                // Direct user to OS system settings to revoke
                                try {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.fromParts("package", context.packageName, null)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(intent)
                                    Toast.makeText(context, "Toggle OFF permissions in System Settings", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Open System Settings", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = item.activeColor,
                            checkedTrackColor = item.activeColor.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    }
}
