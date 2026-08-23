package com.example.ui

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.CyanJarvis
import com.example.ui.theme.GreenSecure
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextSlate

/**
 * State holder for Voice Interaction Permissions (Microphone & Internet / Network).
 */
@Stable
class VoicePermissionState(
    val context: Context,
    hasRecordAudio: Boolean,
    hasInternet: Boolean,
    isNetworkOnline: Boolean,
    shouldShowRationale: Boolean,
    private val onRequestAudio: () -> Unit
) {
    var hasRecordAudioPermission by mutableStateOf(hasRecordAudio)
        internal set

    var hasInternetPermission by mutableStateOf(hasInternet)
        internal set

    var isNetworkOnline by mutableStateOf(isNetworkOnline)
        internal set

    var shouldShowRationale by mutableStateOf(shouldShowRationale)
        internal set

    var isPermanentlyDenied by mutableStateOf(false)
        internal set

    var showRationaleDialog by mutableStateOf(false)

    /**
     * Whether all necessary conditions (Microphone granted + Internet granted) are fulfilled.
     */
    val allGranted: Boolean
        get() = hasRecordAudioPermission && hasInternetPermission

    /**
     * Requests microphone permission directly or shows the rationale dialog if needed.
     */
    fun requestPermission(forceDirectRequest: Boolean = false) {
        if (hasRecordAudioPermission) return

        if (!forceDirectRequest && shouldShowRationale) {
            showRationaleDialog = true
        } else {
            onRequestAudio()
        }
    }

    /**
     * Opens Android System Application Settings page so the user can manually enable permissions.
     */
    fun openAppSettings() {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            android.util.Log.e("PermissionHandler", "Failed to open app settings", e)
        }
    }
}

/**
 * Remember and observe voice permissions (Microphone & Internet) and network status.
 * Automatically refreshes state when app resumes from background or Android Settings.
 */
@Composable
fun rememberVoicePermissionState(
    onPermissionGranted: () -> Unit = {},
    onPermissionDenied: () -> Unit = {}
): VoicePermissionState {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isInspection = LocalInspectionMode.current

    val activity = remember(context) {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is Activity) return@remember ctx
            ctx = ctx.baseContext
        }
        null
    }

    fun checkAudioGranted(): Boolean {
        return if (isInspection) true else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun checkInternetGranted(): Boolean {
        return if (isInspection) true else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.INTERNET
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun checkNetworkOnline(): Boolean {
        if (isInspection) return true
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            val network = cm?.activeNetwork ?: return false
            val capabilities = cm.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            true
        }
    }

    fun shouldShowAudioRationale(): Boolean {
        if (isInspection || activity == null) return false
        return ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.RECORD_AUDIO
        )
    }

    var hasAudioState by remember { mutableStateOf(checkAudioGranted()) }
    var hasInternetState by remember { mutableStateOf(checkInternetGranted()) }
    var isOnlineState by remember { mutableStateOf(checkNetworkOnline()) }
    var shouldShowRationaleState by remember { mutableStateOf(shouldShowAudioRationale()) }
    var permissionRequestedOnce by remember { mutableStateOf(false) }

    var requestAudioLauncherTrigger by remember { mutableStateOf<(() -> Unit)?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasAudioState = isGranted
        shouldShowRationaleState = shouldShowAudioRationale()
        if (isGranted) {
            onPermissionGranted()
        } else {
            permissionRequestedOnce = true
            onPermissionDenied()
        }
    }

    LaunchedEffect(permissionLauncher) {
        requestAudioLauncherTrigger = {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val state = remember(context) {
        VoicePermissionState(
            context = context,
            hasRecordAudio = hasAudioState,
            hasInternet = hasInternetState,
            isNetworkOnline = isOnlineState,
            shouldShowRationale = shouldShowRationaleState,
            onRequestAudio = { requestAudioLauncherTrigger?.invoke() }
        )
    }

    // Keep state values in sync
    SideEffect {
        state.hasRecordAudioPermission = hasAudioState
        state.hasInternetPermission = hasInternetState
        state.isNetworkOnline = isOnlineState
        state.shouldShowRationale = shouldShowRationaleState
        state.isPermanentlyDenied = permissionRequestedOnce && !hasAudioState && !shouldShowRationaleState
    }

    // Refresh on App Resume (e.g. user toggles permission in Settings and returns)
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAudioState = checkAudioGranted()
                hasInternetState = checkInternetGranted()
                isOnlineState = checkNetworkOnline()
                shouldShowRationaleState = shouldShowAudioRationale()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        // Monitor Network Connectivity
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isOnlineState = true
            }
            override fun onLost(network: Network) {
                isOnlineState = checkNetworkOnline()
            }
        }

        try {
            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            connectivityManager?.registerNetworkCallback(request, networkCallback)
        } catch (e: Exception) {
            // Ignore if callback registration fails
        }

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                connectivityManager?.unregisterNetworkCallback(networkCallback)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    return state
}

/**
 * Modern J.A.R.V.I.S. Styled Permission Rationale & Settings Dialog.
 */
@Composable
fun VoicePermissionDialog(
    state: VoicePermissionState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("voice_permission_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.5.dp, CyanJarvis.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon Header with Glow Badge
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(CyanJarvis.copy(alpha = 0.25f), Color.Transparent)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Security & Permissions",
                        tint = CyanJarvis,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "VOICE PROTOCOL ACCESS",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp,
                    color = CyanJarvis,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "J.A.R.V.I.S. requires hardware & network permissions to process voice interaction and cloud AI queries.",
                    fontSize = 13.sp,
                    color = TextSlate,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
                )

                // Permissions Breakdown Cards
                PermissionStatusCard(
                    title = "Microphone Access",
                    description = "Needed for voice commands, wake-word detection, and real-time speech synthesis.",
                    isGranted = state.hasRecordAudioPermission,
                    icon = if (state.hasRecordAudioPermission) Icons.Default.Mic else Icons.Default.MicOff,
                    activeColor = CyanJarvis
                )

                Spacer(modifier = Modifier.height(12.dp))

                PermissionStatusCard(
                    title = "Internet & Neural Cloud",
                    description = "Required to connect to Gemini AI, OpenRouter API models, and search services.",
                    isGranted = state.hasInternetPermission && state.isNetworkOnline,
                    icon = if (state.isNetworkOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                    statusText = if (!state.isNetworkOnline) "Offline" else "Connected",
                    activeColor = GreenSecure
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("permission_dismiss_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text("Dismiss", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    }

                    if (state.isPermanentlyDenied) {
                        Button(
                            onClick = {
                                state.openAppSettings()
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                                .testTag("permission_settings_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanJarvis)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInNew,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFF03070E)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Settings",
                                color = Color(0xFF03070E),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                state.requestPermission(forceDirectRequest = true)
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(1.3f)
                                .height(48.dp)
                                .testTag("permission_grant_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = CyanJarvis)
                        ) {
                            Text(
                                text = "Grant Access",
                                color = Color(0xFF03070E),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionStatusCard(
    title: String,
    description: String,
    isGranted: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    statusText: String = if (isGranted) "Granted" else "Required",
    activeColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (isGranted) activeColor.copy(alpha = 0.08f) else Color(0xFFFF5555).copy(alpha = 0.08f),
        border = BorderStroke(
            1.dp,
            if (isGranted) activeColor.copy(alpha = 0.35f) else Color(0xFFFF5555).copy(alpha = 0.35f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(
                        color = if (isGranted) activeColor.copy(alpha = 0.15f) else Color(0xFFFF5555).copy(alpha = 0.15f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (isGranted) activeColor else Color(0xFFFF5555),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isGranted) activeColor else Color(0xFFFF5555)
                    )
                }
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = TextSlate,
                    lineHeight = 14.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

/**
 * Standalone banner component for when permissions are missing.
 */
@Composable
fun VoicePermissionBanner(
    permissionState: VoicePermissionState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !permissionState.hasRecordAudioPermission || !permissionState.isNetworkOnline,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("voice_permission_banner"),
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF1E1408),
            border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = Color(0xFFFBBF24),
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (!permissionState.hasRecordAudioPermission) "Microphone Access Required" else "Internet Offline",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFBBF24)
                    )
                    Text(
                        text = if (!permissionState.hasRecordAudioPermission) "Tap to enable voice recognition" else "Connect to network for AI responses",
                        fontSize = 10.sp,
                        color = Color(0xFFD1D5DB)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                TextButton(
                    onClick = {
                        if (!permissionState.hasRecordAudioPermission) {
                            permissionState.requestPermission()
                        } else {
                            permissionState.openAppSettings()
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(
                        text = if (!permissionState.hasRecordAudioPermission) "Enable" else "Check",
                        color = Color(0xFFFBBF24),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
