package com.example

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import com.google.firebase.FirebaseApp
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.BuildConfig
import com.example.data.OpenRouterPresets
import com.example.ui.JarvisSettings
import com.example.ui.JarvisUiState
import com.example.ui.JarvisViewModel
import com.example.ui.theme.*

import android.provider.Settings
import android.text.TextUtils

class MainActivity : ComponentActivity() {
    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = ComponentName(this, JarvisAccessibilityService::class.java)
        val enabledServicesSetting = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServicesSetting)
        while (colonSplitter.hasNext()) {
            val componentNameString = colonSplitter.next()
            val enabledService = ComponentName.unflattenFromString(componentNameString)
            if (enabledService != null && enabledService == expectedComponentName) {
                return true
            }
        }
        return false
    }

    private fun configureScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureScreenFlags()
        try {
            FirebaseApp.initializeApp(this)
        } catch (e: Exception) {
            android.util.Log.e("Jarvis", "Firebase initialization failed: ${e.message}")
        }
        enableEdgeToEdge()
        setContent {
            val isAccessEnabled = remember { mutableStateOf(isAccessibilityServiceEnabled()) }
            // Refresh on resume
            DisposableEffect(Unit) {
                onDispose { }
            }
            JarvisScreen(isAccessEnabled = isAccessEnabled.value)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        configureScreenFlags()
    }
}

@Composable
fun JarvisScreen(isAccessEnabled: Boolean, viewModel: JarvisViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val userState by viewModel.userState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val apiUsageWarning by viewModel.apiUsageWarning.collectAsState()
    val chatHistory by viewModel.chatHistory.collectAsState()
    val audioRms by viewModel.audioRms.collectAsState()
    val partialSpeechText by viewModel.partialSpeechText.collectAsState()
    val apiUsageStats by viewModel.apiUsageStats.collectAsState()
    val context = LocalContext.current
    
    val voiceManager = remember { com.example.ui.JarvisVoiceManager(context.applicationContext) }
    
    DisposableEffect(Unit) {
        onDispose { 
            voiceManager.shutdown()
        }
    }
    
    LaunchedEffect(uiState, settings.ttsEnabled, settings.language) {
        if (uiState is com.example.ui.JarvisUiState.Speaking) {
            if (settings.ttsEnabled) {
                voiceManager.speak((uiState as com.example.ui.JarvisUiState.Speaking).message, settings.language)
            } else {
                voiceManager.stop()
            }
        }
    }
    
    LaunchedEffect(settings.voicePlaybackSpeed) {
        voiceManager.setSpeechRate(settings.voicePlaybackSpeed)
    }

    var showSettings by remember { mutableStateOf(false) }

    val voicePermissionState = com.example.ui.rememberVoicePermissionState(
        onPermissionGranted = {
            if (settings.wakeWordEnabled) {
                val intent = Intent(context, WakeWordService::class.java)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("Jarvis", "Failed to start wake word service", e)
                }
            }
        }
    )

    if (voicePermissionState.showRationaleDialog) {
        com.example.ui.VoicePermissionDialog(
            state = voicePermissionState,
            onDismiss = { voicePermissionState.showRationaleDialog = false }
        )
    }

    val speechBroadcastReceiver = remember {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    "com.example.ACTION_WAKE_WORD_DETECTED" -> {
                        viewModel.startListeningVoice()
                    }
                    SpeechRecognitionService.ACTION_SPEECH_RESULT -> {
                        val command = intent.getStringExtra(SpeechRecognitionService.EXTRA_COMMAND)
                            ?: intent.getStringExtra(SpeechRecognitionService.EXTRA_SPEECH_RESULT)
                        if (!command.isNullOrBlank() && context != null) {
                            viewModel.processCommand(command, context)
                        }
                    }
                    SpeechRecognitionService.ACTION_SPEECH_STATE_CHANGED -> {
                        val isListening = intent.getBooleanExtra(SpeechRecognitionService.EXTRA_IS_LISTENING, false)
                        if (isListening) {
                            viewModel.startListening()
                        } else {
                            viewModel.stopListening()
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        val filter = IntentFilter().apply {
            addAction("com.example.ACTION_WAKE_WORD_DETECTED")
            addAction(SpeechRecognitionService.ACTION_SPEECH_RESULT)
            addAction(SpeechRecognitionService.ACTION_SPEECH_STATE_CHANGED)
            addAction(SpeechRecognitionService.ACTION_SPEECH_PARTIAL_RESULT)
            addAction(SpeechRecognitionService.ACTION_SPEECH_ERROR)
        }
        ContextCompat.registerReceiver(
            context,
            speechBroadcastReceiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose {
            context.unregisterReceiver(speechBroadcastReceiver)
        }
    }

    LaunchedEffect(settings.wakeWordEnabled, voicePermissionState.hasRecordAudioPermission) {
        val intent = Intent(context, WakeWordService::class.java)
        if (settings.wakeWordEnabled && voicePermissionState.hasRecordAudioPermission) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                android.util.Log.e("Jarvis", "Failed to start wake word service", e)
            }
        } else {
            context.stopService(intent)
        }
    }

    val isApiKeyConfigured = settings.openRouterApiKey.isNotBlank() ||
        (BuildConfig.OPENROUTER_API_KEY.isNotBlank() && BuildConfig.OPENROUTER_API_KEY != "MY_OPENROUTER_API_KEY")

    val latestGeneratedCode by viewModel.latestGeneratedCode.collectAsState()
    val showCodeStudioModal by viewModel.showCodeStudioModal.collectAsState()

    if (showCodeStudioModal) {
        com.example.ui.JarvisCodeStudioModal(
            onDismiss = { viewModel.toggleCodeStudioModal(false) },
            onGenerateCodePrompt = { prompt ->
                viewModel.processCommand(prompt, context)
            },
            currentGeneratedCode = latestGeneratedCode,
            isAiGenerating = uiState is JarvisUiState.Thinking
        )
    }

    if (showSettings) {
        JarvisSettingsDialog(
            settings = settings,
            onDismiss = { showSettings = false },
            onOpenRouterApiKeyChange = { viewModel.updateOpenRouterApiKey(it) },
            onOpenRouterModelChange = { viewModel.updateOpenRouterModel(it) },
            onTestConnection = { key, model, callback ->
                viewModel.testOpenRouterConnection(key, model, callback)
            },
            onBubbleThemeChange = { viewModel.updateBubbleTheme(it) },
            onLanguageChange = { viewModel.updateLanguage(it) },
            onToggleScreenReader = { viewModel.toggleScreenReader(it) },
            onSensitivityChange = { viewModel.updateSensitivity(it) },
            onRmsThresholdChange = { viewModel.updateRmsThreshold(it) },
            onToggleTextInput = { viewModel.toggleTextInputMode(it) },
            onToggleWakeWord = { viewModel.toggleWakeWord(it) },
            onToggleDarkMode = { viewModel.toggleDarkMode(it) },
            onThemeModeChange = { viewModel.updateThemeMode(it) },
            onPlaybackSpeedChange = { viewModel.updateVoicePlaybackSpeed(it) },
            onToggleTts = { viewModel.toggleTts(it) },
            onTogglePersistentBackground = { viewModel.togglePersistentBackground(it) },
            onWaveformStyleChange = { viewModel.updateWaveformStyle(it) },
            onWaveformPaletteChange = { viewModel.updateWaveformPalette(it) },
            onSystemPromptChange = { viewModel.updateSystemPrompt(it) }
        )
    }

    val currentBubbleTheme = com.example.ui.theme.JarvisBubbleTheme.fromId(settings.bubbleTheme)
    val isSystemInDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isDarkTheme = when (settings.themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDark
    }

    MyApplicationTheme(darkTheme = isDarkTheme) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = { JarvisHeader(apiUsageWarning = apiUsageWarning, onSettingsClick = { showSettings = true }) },
            bottomBar = { JarvisFooter() }
        ) { innerPadding ->
            val bgColor = MaterialTheme.colorScheme.background
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(color = bgColor)
                    }
            ) {
                // Animated Pulsing Futuristic Bubble Themes
                com.example.ui.AnimatedBubbleBackground(
                    theme = currentBubbleTheme,
                    isListening = uiState is JarvisUiState.Listening,
                    audioRms = audioRms
                )
                
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Quick Action & Status Chips (Themes, Language, System Access)
                    QuickActionBar(
                        currentTheme = currentBubbleTheme,
                        currentLanguage = settings.language,
                        isAccessEnabled = isAccessEnabled,
                        onThemeClick = { showSettings = true },
                        onCodeStudioClick = { viewModel.toggleCodeStudioModal(true) },
                        onLanguageClick = {
                            val nextLang = when (settings.language) {
                                "auto" -> "bn-BD"
                                "bn-BD" -> "en-US"
                                "en-US" -> "hi-IN"
                                else -> "auto"
                            }
                            viewModel.updateLanguage(nextLang)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    )

                    if (!isApiKeyConfigured) {
                        Spacer(modifier = Modifier.height(4.dp))
                        com.example.ui.ApiKeySetupBanner(
                            onSetupClick = { showSettings = true },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    if (!voicePermissionState.hasRecordAudioPermission || !voicePermissionState.isNetworkOnline) {
                        Spacer(modifier = Modifier.height(4.dp))
                        com.example.ui.VoicePermissionBanner(
                            permissionState = voicePermissionState,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    
                    val proactiveSuggestions = remember(chatHistory) {
                        com.example.util.ContextualAwarenessEngine.generateProactiveSuggestions(
                            context = context,
                            recentUserMessages = chatHistory.map { it.content }
                        )
                    }

                    if (proactiveSuggestions.isNotEmpty()) {
                        com.example.ui.ProactiveSuggestionsBanner(
                            suggestions = proactiveSuggestions,
                            onSuggestionClicked = { suggestion ->
                                viewModel.processCommand(suggestion.actionPrompt, context)
                            },
                            onDismiss = {},
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        com.example.ui.JarvisChatList(
                            chatHistory = chatHistory,
                            isThinking = uiState is JarvisUiState.Thinking,
                            theme = currentBubbleTheme
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    if (uiState is JarvisUiState.Listening) {
                        com.example.ui.RealtimeListeningWaveform(
                            audioRms = audioRms,
                            partialText = partialSpeechText,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    } else if (uiState is JarvisUiState.Speaking || uiState is JarvisUiState.Thinking) {
                        com.example.ui.GeminiStreamingAudioWaveform(
                            audioRms = if (audioRms > 0.05f) audioRms else 0.45f,
                            statusMessage = if (uiState is JarvisUiState.Speaking) "GEMINI LIVE VOICE STREAM ACTIVE" else "FORMULATING RESPONSE...",
                            isAiStreaming = true,
                            waveformStyle = settings.waveformStyle,
                            waveformPalette = settings.waveformColorPalette,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                
                JarvisControls(
                    uiState = uiState,
                    audioRms = audioRms,
                    isAccessEnabled = isAccessEnabled,
                    settings = settings,
                    apiUsageStats = apiUsageStats,
                    onResetStats = { viewModel.resetApiUsageStats() },
                    onToggleWakeWord = {
                        if (!voicePermissionState.hasRecordAudioPermission) {
                            voicePermissionState.requestPermission()
                        }
                        viewModel.toggleWakeWord(!settings.wakeWordEnabled)
                    },
                    onCommand = { viewModel.processCommand(it, context) },
                    onMicClick = {
                        if (voicePermissionState.hasRecordAudioPermission) {
                            if (uiState is JarvisUiState.Listening) {
                                viewModel.stopListeningVoice()
                            } else {
                                viewModel.startListeningVoice()
                            }
                        } else {
                            voicePermissionState.requestPermission()
                        }
                    }
                )
            }
            }
        }
    }
}

@Composable
fun JarvisHeader(apiUsageWarning: Boolean, onSettingsClick: () -> Unit) {
    val outlineColor = MaterialTheme.colorScheme.outline
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(top = 40.dp, start = 20.dp, end = 20.dp, bottom = 16.dp)
            .drawBehind {
                drawLine(
                    color = outlineColor,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.dp.toPx()
                )
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            if (apiUsageWarning) {
                Text(
                    text = "API QUOTA WARNING",
                    color = Color(0xFFFF5555),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            } else {
                Text(
                    text = "SYSTEM ACTIVE",
                    color = CyanJarvis,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }
            Text(
                text = "J.A.R.V.I.S. v4.0.2",
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Light
            )
        }
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = TextSlate
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            
            Column(horizontalAlignment = Alignment.End) {
                Text(text = "SYNC STATUS", color = TextSlate, fontSize = 9.sp)
                Text(text = "SECURE", color = GreenSecure, fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .border(1.dp, outlineColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(CyanJarvis, CircleShape)
                )
            }
        }
    }
}

@Composable
fun JarvisVisualizer(
    uiState: JarvisUiState,
    audioRms: Float = 0f,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    com.example.ui.CircularPulsingIndicator(
        isListening = uiState is JarvisUiState.Listening,
        isProcessing = uiState is JarvisUiState.Thinking,
        isSpeaking = uiState is JarvisUiState.Speaking,
        audioRms = audioRms,
        indicatorSize = 180.dp,
        showStatusText = true,
        onClick = onClick,
        modifier = modifier
    )
}

@Composable
fun WaveIndicator() {
    val infiniteTransition = rememberInfiniteTransition()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.padding(top = 8.dp)
    ) {
        repeat(5) { index ->
            val height by infiniteTransition.animateFloat(
                initialValue = 8f,
                targetValue = 24f,
                animationSpec = infiniteRepeatable(
                    animation = tween(250 + index * 100, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(height.dp)
                    .background(CyanJarvis, RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
fun JarvisStatusText(uiState: JarvisUiState) {
    val text = when(uiState) {
        is JarvisUiState.Speaking -> uiState.message
        is JarvisUiState.Thinking -> "I'm analyzing your current environment. Accessibility services are fully synchronized..."
        is JarvisUiState.Listening -> "Awaiting command..."
        else -> "Jarvis AI ready. Tap to activate neural link."
    }
    
    Text(
        text = "\"$text\"",
        color = TextSlate,
        fontSize = 14.sp,
        fontStyle = FontStyle.Italic,
        lineHeight = 22.sp,
        modifier = Modifier.padding(horizontal = 40.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}

@Composable
fun JarvisControls(
    uiState: JarvisUiState,
    audioRms: Float,
    isAccessEnabled: Boolean,
    settings: JarvisSettings,
    apiUsageStats: com.example.ui.ApiUsageStats,
    onResetStats: () -> Unit,
    onToggleWakeWord: () -> Unit,
    onCommand: (String) -> Unit,
    onMicClick: () -> Unit
) {
    val context = LocalContext.current
    var inputText by remember { mutableStateOf("") }
    val isListening = uiState is JarvisUiState.Listening
    
    Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp)) {
        // Status Row: Accessibility & Wake Word Hands-Free Mode
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusCard(
                title = "ACCESSIBILITY",
                value = if (isAccessEnabled) "Full Access" else "Disabled",
                statusColor = if (isAccessEnabled) GreenSecure else Color.Red,
                modifier = Modifier
                    .weight(1f)
                    .clickable {
                        if (!isAccessEnabled) {
                            context.startActivity(android.content.Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS))
                        }
                    }
            )
            StatusCard(
                title = "WAKE WORD (HANDS-FREE)",
                value = if (settings.wakeWordEnabled) "Active ('Hey Jarvis')" else "Off (Tap to enable)",
                statusColor = if (settings.wakeWordEnabled) GreenSecure else TextSlate,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onToggleWakeWord() }
            )
        }
        
        Spacer(modifier = Modifier.height(10.dp))
        
        com.example.ui.ApiUsageStatsPanel(
            stats = apiUsageStats,
            onResetStats = onResetStats
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        if (settings.textInputMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                    .border(
                        width = 1.dp,
                        color = if (isListening) Color.Red.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .padding(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Manual Voice Activation / Mic Toggle Button
                IconButton(
                    onClick = onMicClick,
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = if (isListening) Color.Red.copy(alpha = 0.85f) else CyanJarvis.copy(alpha = 0.15f),
                            shape = CircleShape
                        )
                        .border(
                            width = 1.dp,
                            color = if (isListening) Color.Red else CyanJarvis.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                ) {
                    if (isListening) {
                        com.example.ui.ResponsiveMicWaveIndicator(audioRms = audioRms)
                    } else {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Manual Voice Activation",
                            tint = CyanJarvis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    placeholder = { 
                        Text(
                            text = if (isListening) "Listening for voice..." else "Enter command or speak...", 
                            color = TextSlate, 
                            fontSize = 14.sp
                        ) 
                    },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedTextColor = MaterialTheme.colorScheme.onBackground,
                        unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                    ),
                    singleLine = true
                )

                IconButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            onCommand(inputText)
                            inputText = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(CyanJarvis, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = MaterialTheme.colorScheme.background
                    )
                }
            }
        } else {
            // Voice-Only Interaction HUD Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                    .border(
                        width = 1.dp,
                        color = if (isListening) Color.Red.copy(alpha = 0.8f) else CyanJarvis.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clickable { onMicClick() }
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = if (isListening) Color.Red.copy(alpha = 0.8f) else CyanJarvis.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                            .border(
                                1.dp,
                                if (isListening) Color.Red else CyanJarvis,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isListening) {
                            com.example.ui.ResponsiveMicWaveIndicator(audioRms = audioRms)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Activate Voice",
                                tint = CyanJarvis
                            )
                        }
                    }
                    Column {
                        Text(
                            text = if (isListening) "REAL-TIME LISTENING ACTIVE" else "MANUAL VOICE ACTIVATION",
                            color = if (isListening) Color.Red else CyanJarvis,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = if (isListening) "Tap to finish listening" else "Tap to speak command hands-free",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Light
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QuickActionBar(
    currentTheme: com.example.ui.theme.JarvisBubbleTheme,
    currentLanguage: String,
    isAccessEnabled: Boolean,
    onThemeClick: () -> Unit,
    onCodeStudioClick: () -> Unit = {},
    onLanguageClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val langLabel = when {
        currentLanguage.startsWith("bn", ignoreCase = true) -> "🇧🇩 বাংলা (Bangla)"
        currentLanguage.startsWith("en", ignoreCase = true) -> "🇺🇸 English"
        currentLanguage.startsWith("hi", ignoreCase = true) -> "🇮🇳 हिन्दी"
        currentLanguage.startsWith("es", ignoreCase = true) -> "🇪🇸 Español"
        currentLanguage.startsWith("ar", ignoreCase = true) -> "🇸🇦 العربية"
        else -> "🌐 Auto / All"
    }

    Row(
        modifier = modifier
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // AI Code Studio Chip
        Surface(
            onClick = onCodeStudioClick,
            shape = RoundedCornerShape(20.dp),
            color = CyanJarvis.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, CyanJarvis.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Science,
                    contentDescription = "Code Studio",
                    tint = CyanJarvis,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "💻 AI Code Studio",
                    fontSize = 11.sp,
                    color = CyanJarvis,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Theme Chip
        Surface(
            onClick = onThemeClick,
            shape = RoundedCornerShape(20.dp),
            color = currentTheme.primaryColor.copy(alpha = 0.12f),
            border = BorderStroke(1.dp, currentTheme.primaryColor.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(currentTheme.primaryColor, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Theme: ${currentTheme.title}",
                    fontSize = 11.sp,
                    color = currentTheme.primaryColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Language Chip
        Surface(
            onClick = onLanguageClick,
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, CyanJarvis.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = langLabel,
                    fontSize = 11.sp,
                    color = CyanJarvis,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Accessibility / Fast Phone Access Chip
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (isAccessEnabled) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f),
            border = BorderStroke(1.dp, if (isAccessEnabled) Color(0xFF10B981).copy(alpha = 0.4f) else Color(0xFFF59E0B).copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(if (isAccessEnabled) Color(0xFF10B981) else Color(0xFFF59E0B), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isAccessEnabled) "Fast Phone Control: Active" else "Phone Control: Basic",
                    fontSize = 11.sp,
                    color = if (isAccessEnabled) Color(0xFF10B981) else Color(0xFFF59E0B),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Device Admin Chip
        val context = LocalContext.current
        var isAdminActive by remember { mutableStateOf(JarvisDeviceAdminReceiver.isDeviceAdminActive(context)) }
        Surface(
            onClick = {
                try {
                    val intent = JarvisDeviceAdminReceiver.getAddDeviceAdminIntent(context)
                    context.startActivity(intent)
                } catch (e: Exception) {
                    // Ignore
                }
            },
            shape = RoundedCornerShape(20.dp),
            color = if (isAdminActive) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f),
            border = BorderStroke(1.dp, if (isAdminActive) Color(0xFF10B981).copy(alpha = 0.4f) else Color(0xFFF59E0B).copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(if (isAdminActive) Color(0xFF10B981) else Color(0xFFF59E0B), CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isAdminActive) "Device Admin: Active" else "Device Admin: Enable",
                    fontSize = 11.sp,
                    color = if (isAdminActive) Color(0xFF10B981) else Color(0xFFF59E0B),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JarvisSettingsDialog(
    settings: JarvisSettings,
    onDismiss: () -> Unit,
    onOpenRouterApiKeyChange: (String) -> Unit,
    onOpenRouterModelChange: (String) -> Unit,
    onTestConnection: ((String, String, (Boolean, String) -> Unit) -> Unit)? = null,
    onBubbleThemeChange: (String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onToggleScreenReader: (Boolean) -> Unit,
    onSensitivityChange: (Float) -> Unit,
    onRmsThresholdChange: (Float) -> Unit = {},
    onToggleTextInput: (Boolean) -> Unit,
    onToggleWakeWord: (Boolean) -> Unit,
    onToggleDarkMode: (Boolean) -> Unit,
    onThemeModeChange: (String) -> Unit = {},
    onPlaybackSpeedChange: (Float) -> Unit,
    onToggleTts: (Boolean) -> Unit,
    onTogglePersistentBackground: (Boolean) -> Unit = {},
    onWaveformStyleChange: (String) -> Unit = {},
    onWaveformPaletteChange: (String) -> Unit = {},
    onSystemPromptChange: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = MaterialTheme.colorScheme.background.copy(alpha = 0.5f)
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            Text("ADVANCED CONFIGURATION", color = CyanJarvis, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            
            Spacer(modifier = Modifier.height(16.dp))

            // Waveform Visualizer Style & Color Palette Customization
            com.example.ui.WaveformCustomizationSection(
                currentStyle = settings.waveformStyle,
                currentPalette = settings.waveformColorPalette,
                onStyleSelected = onWaveformStyleChange,
                onPaletteSelected = onWaveformPaletteChange
            )

            Spacer(modifier = Modifier.height(24.dp))

            // System Admin & 24/7 Background Execution Controls
            com.example.ui.DeviceAdminAndBackgroundSection(
                persistentBackgroundEnabled = settings.persistentBackgroundEnabled,
                onTogglePersistentBackground = onTogglePersistentBackground
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Granular Permission Control Section (Microphone, Contacts, SMS, Location, Camera, Phone, etc.)
            com.example.ui.GranularPermissionsSection()

            Spacer(modifier = Modifier.height(24.dp))

            // 1. Animated Bubble Theme Selector
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Animated Background Theme",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Select animated pulsing energy hologram & bubble style",
                    color = TextSlate,
                    fontSize = 12.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    com.example.ui.theme.JarvisBubbleTheme.entries.forEach { theme ->
                        val isSelected = settings.bubbleTheme.equals(theme.id, ignoreCase = true)
                        Surface(
                            onClick = { onBubbleThemeChange(theme.id) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) theme.primaryColor.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) theme.primaryColor else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .background(
                                            brush = Brush.radialGradient(listOf(theme.glowColor, theme.primaryColor)),
                                            shape = CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = theme.title,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) theme.primaryColor else MaterialTheme.colorScheme.onBackground
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Language & Voice Recognition Mode (Bangla & English Support)
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Language & Voice Recognition",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Full voice recognition & TTS response in Bangla and English",
                    color = TextSlate,
                    fontSize = 12.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))

                val languages = listOf(
                    "auto" to "🌐 Auto Detect",
                    "bn-BD" to "🇧🇩 বাংলা (Bangla)",
                    "en-US" to "🇺🇸 English (US)",
                    "en-GB" to "🇬🇧 English (UK)",
                    "hi-IN" to "🇮🇳 हिन्दी (Hindi)",
                    "ar" to "🇸🇦 العربية (Arabic)",
                    "es-ES" to "🇪🇸 Español"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    languages.forEach { (code, label) ->
                        val isSelected = settings.language.equals(code, ignoreCase = true)
                        SuggestionChip(
                            onClick = { onLanguageChange(code) },
                            label = {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) CyanJarvis else MaterialTheme.colorScheme.onBackground
                                )
                            },
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = if (isSelected) CyanJarvis else MaterialTheme.colorScheme.outline
                            ),
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (isSelected) CyanJarvis.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // OpenRouter API Key & Model Configuration Section
            com.example.ui.OpenRouterApiConfigSection(
                currentApiKey = settings.openRouterApiKey,
                currentModel = settings.openRouterModel,
                onApiKeyChange = onOpenRouterApiKeyChange,
                onModelChange = onOpenRouterModelChange,
                onTestConnection = onTestConnection
            )

            Spacer(modifier = Modifier.height(24.dp))
            
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "App Theme & Appearance",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Sync with system appearance settings or lock to dark/light theme",
                    color = TextSlate,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val themeOptions = listOf(
                        Triple("system", "System", Icons.Default.SettingsSuggest),
                        Triple("dark", "Dark", Icons.Default.DarkMode),
                        Triple("light", "Light", Icons.Default.LightMode)
                    )

                    themeOptions.forEach { (modeKey, label, icon) ->
                        val isSelected = settings.themeMode == modeKey
                        Surface(
                            onClick = { onThemeModeChange(modeKey) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) CyanJarvis.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, if (isSelected) CyanJarvis else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = label,
                                    tint = if (isSelected) CyanJarvis else TextSlate,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) CyanJarvis else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Screen Reader Mode", color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)
                    Text("Reads screen text aloud (Requires Accessibility)", color = TextSlate, fontSize = 12.sp)
                }
                Switch(
                    checked = settings.screenReaderMode,
                    onCheckedChange = onToggleScreenReader,
                    colors = SwitchDefaults.colors(checkedThumbColor = CyanJarvis, checkedTrackColor = CyanJarvis.copy(alpha = 0.5f))
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Microphone RMS Trigger Threshold",
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Calibrate minimum speech amplitude (RMS) for 'Hey Jarvis' wake-word detection",
                            color = TextSlate,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = CyanJarvis.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, CyanJarvis.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "${settings.microphoneRmsThreshold.toInt()} RMS",
                            color = CyanJarvis,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Slider(
                    value = settings.microphoneRmsThreshold,
                    onValueChange = onRmsThresholdChange,
                    valueRange = 1000f..15000f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyanJarvis,
                        activeTrackColor = CyanJarvis,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("1000 (Whisper / Quiet)", color = TextSlate, fontSize = 10.sp)
                    Text("6000 (Normal)", color = TextSlate, fontSize = 10.sp)
                    Text("15000 (Loud / Noisy Room)", color = TextSlate, fontSize = 10.sp)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Voice Command Sensitivity", color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)
                Slider(
                    value = settings.voiceSensitivity,
                    onValueChange = onSensitivityChange,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyanJarvis,
                        activeTrackColor = CyanJarvis,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Voice Playback Speed", color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)
                Text("Adjust the speaking rate of synthesized responses", color = TextSlate, fontSize = 12.sp)
                Slider(
                    value = settings.voicePlaybackSpeed,
                    onValueChange = onPlaybackSpeedChange,
                    valueRange = 0.5f..2.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = CyanJarvis,
                        activeTrackColor = CyanJarvis,
                        inactiveTrackColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Alternative Input Mode", color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)
                    Text("Use text input instead of voice commands", color = TextSlate, fontSize = 12.sp)
                }
                Switch(
                    checked = settings.textInputMode,
                    onCheckedChange = onToggleTextInput,
                    colors = SwitchDefaults.colors(checkedThumbColor = CyanJarvis, checkedTrackColor = CyanJarvis.copy(alpha = 0.5f))
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Hands-Free Wake Word", color = MaterialTheme.colorScheme.onBackground, fontSize = 16.sp)
                    Text("Always listening for 'Hey Jarvis'", color = TextSlate, fontSize = 12.sp)
                }
                Switch(
                    checked = settings.wakeWordEnabled,
                    onCheckedChange = onToggleWakeWord,
                    colors = SwitchDefaults.colors(checkedThumbColor = CyanJarvis, checkedTrackColor = CyanJarvis.copy(alpha = 0.5f))
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Custom AI Persona / System Prompt Section
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "AI Persona & Multilingual System Prompt",
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Define custom role, instructions, and personality for AI responses in Bangla and English",
                    color = TextSlate,
                    fontSize = 12.sp
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Persona Presets Chips
                val presetPersonas = listOf(
                    "J.A.R.V.I.S. (Multilingual)" to "You are J.A.R.V.I.S., an advanced AI assistant. You have full multilingual capabilities with native support for English, Bangla (বাংলা), and all languages. Always respond in the exact language the user speaks or writes (if the user speaks Bangla, reply in natural, fluent Bangla; if English, reply in English). Provide concise, clear, and professional answers formatted as plain text suitable for a Text-to-Speech engine (no markdown asterisks, bolding, or lists).",
                    "বাংলা সহকারী (Bangla Assistant)" to "আপনি জার্ভিস, একটি অত্যন্ত দক্ষ এবং বিনয়ী এআই সহকারী। ব্যবহারকারী বাংলায় কথা বললে সবসময় বিশুদ্ধ, সাবলীল এবং আন্তরিক বাংলায় উত্তর দিন। ভয়েস স্পিচের জন্য উত্তর ছোট ও স্পষ্ট রাখুন।",
                    "Concise Butler" to "You are an impeccably polite and ultra-concise British butler assistant. Respond in 1-2 succinct, refined sentences suitable for speech.",
                    "Sarcastic Sci-Fi" to "You are a witty, mildly sarcastic AI companion with dry humor, but still helpful and accurate. Keep responses punchy and speech-friendly.",
                    "Tech Specialist" to "You are a senior tech specialist and engineer. Provide accurate, analytically sharp, direct answers without fluff."
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presetPersonas.forEach { (name, prompt) ->
                        val isSelected = settings.systemPrompt.trim() == prompt.trim()
                        SuggestionChip(
                            onClick = { onSystemPromptChange(prompt) },
                            label = { 
                                Text(
                                    text = name,
                                    fontSize = 11.sp,
                                    color = if (isSelected) CyanJarvis else MaterialTheme.colorScheme.onBackground
                                )
                            },
                            border = SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = if (isSelected) CyanJarvis else MaterialTheme.colorScheme.outline
                            ),
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (isSelected) CyanJarvis.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                var promptText by remember(settings.systemPrompt) { mutableStateOf(settings.systemPrompt) }
                
                OutlinedTextField(
                    value = promptText,
                    onValueChange = {
                        promptText = it
                        onSystemPromptChange(it)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 160.dp),
                    placeholder = {
                        Text("Enter system prompt or custom persona instructions...", color = TextSlate, fontSize = 13.sp)
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyanJarvis,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${promptText.length} characters",
                        color = TextSlate,
                        fontSize = 11.sp
                    )
                    TextButton(
                        onClick = {
                            val defaultPrompt = "You are J.A.R.V.I.S., an advanced AI assistant. You have full multilingual capabilities with native support for English, Bangla (বাংলা), and all languages. Always respond in the exact language the user speaks or writes (if the user speaks Bangla, reply in natural, fluent Bangla; if English, reply in English). Provide concise, clear, and professional answers formatted as plain text suitable for a Text-to-Speech engine (no markdown asterisks, bolding, or lists)."
                            promptText = defaultPrompt
                            onSystemPromptChange(defaultPrompt)
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Reset to Default", color = CyanJarvis, fontSize = 12.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun StatusCard(title: String, value: String, statusColor: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Text(text = title, color = TextSlate, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = value, color = MaterialTheme.colorScheme.onBackground, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Box(modifier = Modifier
                .size(8.dp)
                .background(statusColor, CircleShape))
        }
    }
}

@Composable
fun JarvisFooter() {
    val outlineColor = MaterialTheme.colorScheme.outline
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawLine(
                    color = outlineColor,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 1.dp.toPx()
                )
            }
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "CPU: 14%", color = Color(0xFF475569), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier
                .size(4.dp)
                .background(CyanJarvis, CircleShape))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "ENCRYPTED VOICE LINK", color = Color(0xFF475569), fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Text(text = "MEM: 2.4GB", color = Color(0xFF475569), fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun SmallWaveIndicator() {
    val infiniteTransition = rememberInfiniteTransition()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        repeat(3) { index ->
            val height by infiniteTransition.animateFloat(
                initialValue = 4f,
                targetValue = 16f,
                animationSpec = infiniteRepeatable(
                    animation = tween(250 + index * 100, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(height.dp)
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(1.5.dp))
            )
        }
    }
}
