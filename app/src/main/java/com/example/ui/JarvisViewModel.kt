package com.example.ui

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import android.accessibilityservice.AccessibilityService
import com.example.JarvisAccessibilityService
import com.example.network.FirebaseManager
import com.example.network.GeminiRepository
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch

data class JarvisSettings(
    val screenReaderMode: Boolean = false,
    val voiceSensitivity: Float = 0.5f,
    val microphoneRmsThreshold: Float = 6000f,
    val textInputMode: Boolean = true,
    val wakeWordEnabled: Boolean = false,
    val persistentBackgroundEnabled: Boolean = true,
    val isDarkMode: Boolean = true,
    val themeMode: String = "system", // "system", "dark", "light"
    val voicePlaybackSpeed: Float = 1.0f,
    val ttsEnabled: Boolean = true,
    val systemPrompt: String = "You are J.A.R.V.I.S., an advanced AI assistant. You have full multilingual capabilities with native support for English, Bangla (বাংলা), and all languages. Always respond in the exact language the user speaks or writes (if the user speaks Bangla, reply in natural, fluent Bangla; if English, reply in English). Provide concise, clear, and professional answers formatted as plain text suitable for a Text-to-Speech engine (no markdown asterisks, bolding, or lists).",
    val openRouterApiKey: String = "",
    val openRouterModel: String = "openai/gpt-4o-mini",
    val bubbleTheme: String = "arc_reactor",
    val language: String = "auto",
    val waveformStyle: String = "wave", // "wave", "bar", "line", "ripple"
    val waveformColorPalette: String = "cyan" // "cyan", "violet", "emerald", "amber", "monochrome"
)

class JarvisViewModel(application: Application) : AndroidViewModel(application) {
    private var speechRecognizer: JarvisSpeechRecognizer? = null

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms

    private val _partialSpeechText = MutableStateFlow("")
    val partialSpeechText: StateFlow<String> = _partialSpeechText

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    init {
        initSpeechRecognizer()
    }

    private var transcriptDebounceJob: kotlinx.coroutines.Job? = null
    private var pendingPartialBuffer: String = ""

    private fun initSpeechRecognizer() {
        speechRecognizer = JarvisSpeechRecognizer(
            context = getApplication(),
            onCommand = { command -> 
                flushPartialTranscript(immediate = true)
                processCommand(command, getApplication()) 
            },
            onStatusChange = { isListening ->
                if (isListening) {
                    startListening()
                } else {
                    flushSafetyPartialTranscript()
                    stopListening()
                }
            },
            onPartialResult = { partial ->
                pendingPartialBuffer = partial
                transcriptDebounceJob?.cancel()
                transcriptDebounceJob = viewModelScope.launch {
                    kotlinx.coroutines.delay(300L) // Intelligent 300ms debounce
                    _partialSpeechText.value = pendingPartialBuffer
                }
            },
            onRmsChangedCallback = { rms ->
                val normalized = ((rms + 2f) / 12f).coerceIn(0f, 1f)
                _audioRms.value = normalized
            },
            onErrorCallback = { error ->
                flushSafetyPartialTranscript()
                _errorMessage.value = error
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Toast.makeText(getApplication(), error, Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    private fun flushPartialTranscript(immediate: Boolean = false) {
        transcriptDebounceJob?.cancel()
        transcriptDebounceJob = null
        _partialSpeechText.value = ""
        pendingPartialBuffer = ""
    }

    private fun flushSafetyPartialTranscript() {
        transcriptDebounceJob?.cancel()
        transcriptDebounceJob = null
        if (pendingPartialBuffer.isNotBlank() && _partialSpeechText.value != pendingPartialBuffer) {
            _partialSpeechText.value = pendingPartialBuffer
        }
        pendingPartialBuffer = ""
    }

    fun startListeningVoice() {
        if (speechRecognizer == null) {
            initSpeechRecognizer()
        }
        speechRecognizer?.startListening(_settings.value.language)
    }

    fun stopListeningVoice() {
        speechRecognizer?.stopListening()
    }

    fun runSpeechDiagnostics(): SpeechDiagnosticReport? {
        return speechRecognizer?.runDiagnosticCheck()
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private val repository = GeminiRepository()

    private val database = com.example.data.AppDatabase.getDatabase(application)
    private val chatRepository = com.example.data.ChatRepository(database.chatDao())
    val chatHistory: StateFlow<List<com.example.data.ChatEntity>> = chatRepository.allChats
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow<JarvisUiState>(JarvisUiState.Idle)
    val uiState: StateFlow<JarvisUiState> = _uiState

    private val _userState = MutableStateFlow<FirebaseUser?>(null)
    val userState: StateFlow<FirebaseUser?> = _userState

    private val settingsPrefs = application.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(
        JarvisSettings(
            screenReaderMode = settingsPrefs.getBoolean("screen_reader_mode", false),
            voiceSensitivity = settingsPrefs.getFloat("voice_sensitivity", 0.5f),
            microphoneRmsThreshold = settingsPrefs.getFloat("rms_threshold", 6000f),
            textInputMode = settingsPrefs.getBoolean("text_input_mode", false),
            wakeWordEnabled = settingsPrefs.getBoolean("wake_word_enabled", false),
            persistentBackgroundEnabled = settingsPrefs.getBoolean("persistent_background_enabled", true),
            isDarkMode = settingsPrefs.getBoolean("dark_mode", true),
            themeMode = settingsPrefs.getString("theme_mode", "system") ?: "system",
            voicePlaybackSpeed = settingsPrefs.getFloat("voice_speed", 1.0f),
            ttsEnabled = settingsPrefs.getBoolean("tts_enabled", true),
            systemPrompt = settingsPrefs.getString("system_prompt", null)
                ?: "You are J.A.R.V.I.S., an advanced AI assistant. You have full multilingual capabilities with native support for English, Bangla (বাংলা), and all languages. Always respond in the exact language the user speaks or writes (if the user speaks Bangla, reply in natural, fluent Bangla; if English, reply in English). Provide concise, clear, and professional answers formatted as plain text suitable for a Text-to-Speech engine (no markdown asterisks, bolding, or lists).",
            openRouterApiKey = settingsPrefs.getString("openrouter_api_key", "") ?: "",
            openRouterModel = settingsPrefs.getString("openrouter_model", "openai/gpt-4o-mini") ?: "openai/gpt-4o-mini",
            bubbleTheme = settingsPrefs.getString("bubble_theme", "arc_reactor") ?: "arc_reactor",
            language = settingsPrefs.getString("selected_language", "auto") ?: "auto",
            waveformStyle = settingsPrefs.getString("waveform_style", "wave") ?: "wave",
            waveformColorPalette = settingsPrefs.getString("waveform_palette", "cyan") ?: "cyan"
        )
    )
    val settings: StateFlow<JarvisSettings> = _settings

    private val _apiUsageWarning = MutableStateFlow(false)
    val apiUsageWarning: StateFlow<Boolean> = _apiUsageWarning

    // Track rolling requests in the last 60 seconds
    private val requestTimestamps = mutableListOf<Long>()

    // Persistent API stats tracker
    private val _apiUsageStats = MutableStateFlow(
        ApiUsageStats(
            totalCallsMade = settingsPrefs.getInt("stats_total_calls", 0),
            monthlyQuotaLimit = settingsPrefs.getInt("stats_monthly_quota", 1000),
            currentCycleStartDate = settingsPrefs.getLong("stats_cycle_start", System.currentTimeMillis())
        )
    )
    val apiUsageStats: StateFlow<ApiUsageStats> = _apiUsageStats

    fun resetApiUsageStats() {
        val now = System.currentTimeMillis()
        settingsPrefs.edit()
            .putInt("stats_total_calls", 0)
            .putLong("stats_cycle_start", now)
            .apply()
        _apiUsageStats.value = ApiUsageStats(
            totalCallsMade = 0,
            monthlyQuotaLimit = _apiUsageStats.value.monthlyQuotaLimit,
            currentCycleStartDate = now
        )
    }

    private fun incrementApiCallCount() {
        val current = _apiUsageStats.value
        val newCalls = current.totalCallsMade + 1

        settingsPrefs.edit()
            .putInt("stats_total_calls", newCalls)
            .apply()

        _apiUsageStats.value = current.copy(
            totalCallsMade = newCalls
        )
    }

    fun updateBubbleTheme(themeId: String) {
        settingsPrefs.edit().putString("bubble_theme", themeId).apply()
        _settings.value = _settings.value.copy(bubbleTheme = themeId)
    }

    fun updateWaveformStyle(style: String) {
        settingsPrefs.edit().putString("waveform_style", style).apply()
        _settings.value = _settings.value.copy(waveformStyle = style)
    }

    fun updateWaveformPalette(palette: String) {
        settingsPrefs.edit().putString("waveform_palette", palette).apply()
        _settings.value = _settings.value.copy(waveformColorPalette = palette)
    }

    fun updateLanguage(languageCode: String) {
        settingsPrefs.edit().putString("selected_language", languageCode).apply()
        _settings.value = _settings.value.copy(language = languageCode)
    }

    fun updateSystemPrompt(newPrompt: String) {
        settingsPrefs.edit().putString("system_prompt", newPrompt).apply()
        _settings.value = _settings.value.copy(systemPrompt = newPrompt)
    }

    fun toggleDarkMode(enabled: Boolean) {
        val mode = if (enabled) "dark" else "light"
        updateThemeMode(mode)
    }

    fun updateThemeMode(mode: String) {
        settingsPrefs.edit().putString("theme_mode", mode).apply()
        val isDark = when (mode) {
            "dark" -> true
            "light" -> false
            else -> true
        }
        settingsPrefs.edit().putBoolean("dark_mode", isDark).apply()
        _settings.value = _settings.value.copy(themeMode = mode, isDarkMode = isDark)
    }

    fun updateVoicePlaybackSpeed(speed: Float) {
        settingsPrefs.edit().putFloat("voice_speed", speed).apply()
        _settings.value = _settings.value.copy(voicePlaybackSpeed = speed)
    }

    fun toggleScreenReader(enabled: Boolean) {
        settingsPrefs.edit().putBoolean("screen_reader_mode", enabled).apply()
        _settings.value = _settings.value.copy(screenReaderMode = enabled)
        JarvisAccessibilityService.toggleScreenReader(enabled)
    }

    fun updateSensitivity(value: Float) {
        val mappedRms = (18000f - (value.coerceIn(0.1f, 1.0f) * 12000f)).coerceIn(1000f, 15000f)
        settingsPrefs.edit()
            .putFloat("voice_sensitivity", value)
            .putFloat("rms_threshold", mappedRms)
            .apply()
        _settings.value = _settings.value.copy(
            voiceSensitivity = value,
            microphoneRmsThreshold = mappedRms
        )
    }

    fun updateRmsThreshold(value: Float) {
        val mappedSensitivity = ((18000f - value) / 12000f).coerceIn(0.1f, 1.0f)
        settingsPrefs.edit()
            .putFloat("rms_threshold", value)
            .putFloat("voice_sensitivity", mappedSensitivity)
            .apply()
        _settings.value = _settings.value.copy(
            microphoneRmsThreshold = value,
            voiceSensitivity = mappedSensitivity
        )
    }

    fun toggleTextInputMode(enabled: Boolean) {
        settingsPrefs.edit().putBoolean("text_input_mode", enabled).apply()
        _settings.value = _settings.value.copy(textInputMode = enabled)
    }

    fun toggleWakeWord(enabled: Boolean) {
        settingsPrefs.edit().putBoolean("wake_word_enabled", enabled).apply()
        _settings.value = _settings.value.copy(wakeWordEnabled = enabled)
    }

    fun togglePersistentBackground(enabled: Boolean) {
        settingsPrefs.edit().putBoolean("persistent_background_enabled", enabled).apply()
        _settings.value = _settings.value.copy(persistentBackgroundEnabled = enabled)
    }

    fun toggleTts(enabled: Boolean) {
        settingsPrefs.edit().putBoolean("tts_enabled", enabled).apply()
        _settings.value = _settings.value.copy(ttsEnabled = enabled)
    }

    fun updateOpenRouterApiKey(apiKey: String) {
        val trimmed = apiKey.trim()
        settingsPrefs.edit().putString("openrouter_api_key", trimmed).apply()
        _settings.value = _settings.value.copy(openRouterApiKey = trimmed)
    }

    fun updateOpenRouterModel(model: String) {
        val trimmed = model.trim()
        settingsPrefs.edit().putString("openrouter_model", trimmed).apply()
        _settings.value = _settings.value.copy(openRouterModel = trimmed)
    }

    fun testOpenRouterConnection(apiKey: String, model: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = repository.testApiKeyConnection(apiKey = apiKey, model = model)
            result.onSuccess { reply ->
                onResult(true, "Connection successful! Server responded: \"$reply\"")
            }.onFailure { error ->
                onResult(false, error.message ?: "Connection test failed.")
            }
        }
    }

    private val _latestGeneratedCode = MutableStateFlow("")
    val latestGeneratedCode: StateFlow<String> = _latestGeneratedCode.asStateFlow()

    private val _showCodeStudioModal = MutableStateFlow(false)
    val showCodeStudioModal: StateFlow<Boolean> = _showCodeStudioModal.asStateFlow()

    fun toggleCodeStudioModal(show: Boolean) {
        _showCodeStudioModal.value = show
    }

    fun processCommand(command: String, context: android.content.Context) {
        viewModelScope.launch {
            _uiState.value = JarvisUiState.Thinking
            chatRepository.insertChat(com.example.data.ChatEntity(role = "user", content = command))

            // 0. App Self-Modification Check
            val selfModResult = com.example.util.JarvisSelfModifierEngine.processSelfModificationCommand(
                command = command,
                viewModel = this@JarvisViewModel,
                context = context
            )

            val response = if (selfModResult.modified) {
                selfModResult.description
            } else {
                // 1. Fast Native AR Phone Controller Execution (English & Bangla)
                val phoneResult = com.example.util.JarvisPhoneController.executeCommand(context, command)

                if (phoneResult.handled) {
                    phoneResult.responseText
                } else {
                    val now = System.currentTimeMillis()
                    requestTimestamps.add(now)
                    requestTimestamps.removeAll { now - it > 60_000 }
                    _apiUsageWarning.value = requestTimestamps.size >= 12

                    incrementApiCallCount()
                    val contextInjection = com.example.util.ContextualAwarenessEngine.buildContextPromptInjection(
                        recentUserMessages = chatHistory.value.map { it.content }
                    )
                    val enrichedSystemPrompt = "${_settings.value.systemPrompt}\n\n$contextInjection"

                    repository.generateResponse(
                        prompt = command,
                        systemPrompt = enrichedSystemPrompt,
                        userApiKey = _settings.value.openRouterApiKey,
                        userModel = _settings.value.openRouterModel
                    )
                }
            }

            // Check if AI generated HTML / Web code
            if (response.contains("<!DOCTYPE html>", ignoreCase = true) || response.contains("<html", ignoreCase = true)) {
                _latestGeneratedCode.value = response
                _showCodeStudioModal.value = true
            }

            chatRepository.insertChat(com.example.data.ChatEntity(role = "jarvis", content = response))

            FirebaseManager.saveUserCommand(command, response)
            _uiState.value = JarvisUiState.Speaking(response)
        }
    }

    fun startListening() {
        _uiState.value = JarvisUiState.Listening
    }

    fun stopListening() {
        flushSafetyPartialTranscript()
        _uiState.value = JarvisUiState.Idle
        _audioRms.value = 0f
        _partialSpeechText.value = ""
    }
}

sealed class JarvisUiState {
    object Idle : JarvisUiState()
    object Listening : JarvisUiState()
    object Thinking : JarvisUiState()
    data class Speaking(val message: String) : JarvisUiState()
}
