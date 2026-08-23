package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * SpeechRecognitionService using the Android SpeechRecognizer API to process voice input
 * for the Jarvis AI assistant.
 *
 * Supports foreground background operation, localized speech intents (Bangla, English, etc.),
 * Broadcast communication, and direct Service Binding with Kotlin StateFlow streams.
 */
class SpeechRecognitionService : Service(), RecognitionListener {

    enum class RecognitionState {
        IDLE,
        PREPARING,
        LISTENING,
        PROCESSING,
        ERROR
    }

    interface SpeechCallback {
        fun onSpeechRecognized(text: String, confidence: Float)
        fun onPartialSpeech(partialText: String)
        fun onStateChanged(state: RecognitionState)
        fun onRmsChanged(rmsDb: Float)
        fun onError(errorCode: Int, errorMessage: String)
    }

    inner class SpeechRecognitionBinder : Binder() {
        fun getService(): SpeechRecognitionService = this@SpeechRecognitionService

        fun startListening(language: String = "auto") {
            this@SpeechRecognitionService.startRecognition(language)
        }

        fun stopListening() {
            this@SpeechRecognitionService.stopRecognition()
        }

        fun cancelListening() {
            this@SpeechRecognitionService.cancelRecognition()
        }

        fun registerCallback(callback: SpeechCallback) {
            callbacks.add(callback)
        }

        fun unregisterCallback(callback: SpeechCallback) {
            callbacks.remove(callback)
        }

        val state: StateFlow<RecognitionState> get() = _state.asStateFlow()
        val partialText: StateFlow<String> get() = _partialText.asStateFlow()
        val lastRecognizedText: StateFlow<String?> get() = _lastRecognizedText.asStateFlow()
        val audioRms: StateFlow<Float> get() = _audioRms.asStateFlow()
    }

    private val binder = SpeechRecognitionBinder()
    private val callbacks = mutableSetOf<SpeechCallback>()

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow(RecognitionState.IDLE)
    val state: StateFlow<RecognitionState> = _state.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private val _lastRecognizedText = MutableStateFlow<String?>(null)
    val lastRecognizedText: StateFlow<String?> = _lastRecognizedText.asStateFlow()

    private val _audioRms = MutableStateFlow(0f)
    val audioRms: StateFlow<Float> = _audioRms.asStateFlow()

    private var currentLanguage: String = "auto"
    private var isBoundOrStartedAsForeground: Boolean = false

    companion object {
        private const val TAG = "SpeechRecognitionSvc"
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "JarvisSpeechRecognitionChannel"

        // Actions
        const val ACTION_START_LISTENING = "com.example.action.START_SPEECH_RECOGNITION"
        const val ACTION_STOP_LISTENING = "com.example.action.STOP_SPEECH_RECOGNITION"
        const val ACTION_CANCEL_LISTENING = "com.example.action.CANCEL_SPEECH_RECOGNITION"

        // Broadcast Results
        const val ACTION_SPEECH_RESULT = "com.example.action.SPEECH_RESULT"
        const val ACTION_SPEECH_PARTIAL_RESULT = "com.example.action.SPEECH_PARTIAL_RESULT"
        const val ACTION_SPEECH_STATE_CHANGED = "com.example.action.SPEECH_STATE_CHANGED"
        const val ACTION_SPEECH_ERROR = "com.example.action.SPEECH_ERROR"
        const val ACTION_SPEECH_RMS_CHANGED = "com.example.action.SPEECH_RMS_CHANGED"

        // Extras
        const val EXTRA_LANGUAGE = "extra_language"
        const val EXTRA_SPEECH_RESULT = "extra_speech_result"
        const val EXTRA_COMMAND = "extra_command"
        const val EXTRA_CONFIDENCE = "extra_confidence"
        const val EXTRA_PARTIAL_RESULT = "extra_partial_result"
        const val EXTRA_STATE = "extra_state"
        const val EXTRA_IS_LISTENING = "extra_is_listening"
        const val EXTRA_RMS_DB = "extra_rms_db"
        const val EXTRA_ERROR_CODE = "extra_error_code"
        const val EXTRA_ERROR_MESSAGE = "extra_error_message"

        /**
         * Helper to start listening via Intent
         */
        fun startListening(context: Context, language: String = "auto") {
            val intent = Intent(context, SpeechRecognitionService::class.java).apply {
                action = ACTION_START_LISTENING
                putExtra(EXTRA_LANGUAGE, language)
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start SpeechRecognitionService", e)
            }
        }

        /**
         * Helper to stop listening via Intent
         */
        fun stopListening(context: Context) {
            val intent = Intent(context, SpeechRecognitionService::class.java).apply {
                action = ACTION_STOP_LISTENING
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send stop to SpeechRecognitionService", e)
            }
        }

        /**
         * Helper to cancel listening via Intent
         */
        fun cancel(context: Context) {
            val intent = Intent(context, SpeechRecognitionService::class.java).apply {
                action = ACTION_CANCEL_LISTENING
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send cancel to SpeechRecognitionService", e)
            }
        }

        fun getErrorMessage(errorCode: Int): String {
            return when (errorCode) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Check microphone."
                SpeechRecognizer.ERROR_CLIENT -> "Client side recognition error."
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                SpeechRecognizer.ERROR_NETWORK -> "Network connection error for speech recognition."
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timed out during speech recognition."
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech recognized. Please speak again."
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy. Resetting..."
                SpeechRecognizer.ERROR_SERVER -> "Speech recognition server error."
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard. Timeout reached."
                SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> "Selected language not supported on this device."
                SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> "Language pack unavailable."
                SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> "Speech recognition server disconnected."
                SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> "Too many requests. Please try again later."
                else -> "Speech recognition error code: $errorCode"
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        ensureMainThreadSpeechRecognizer()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val lang = intent?.getStringExtra(EXTRA_LANGUAGE) ?: readSavedLanguage()

        when (action) {
            ACTION_START_LISTENING -> {
                promoteToForeground("Listening for voice input...")
                startRecognition(lang)
            }
            ACTION_STOP_LISTENING -> {
                stopRecognition()
            }
            ACTION_CANCEL_LISTENING -> {
                cancelRecognition()
                stopForegroundIfActive()
                stopSelf()
            }
            else -> {
                if (intent != null) {
                    promoteToForeground("J.A.R.V.I.S. Speech Recognition")
                    startRecognition(lang)
                }
            }
        }

        return START_NOT_STICKY
    }

    private fun readSavedLanguage(): String {
        return try {
            val prefs = getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
            prefs.getString("selected_language", "auto") ?: "auto"
        } catch (e: Exception) {
            "auto"
        }
    }

    private fun ensureMainThreadSpeechRecognizer() {
        mainHandler.post {
            if (speechRecognizer == null) {
                if (SpeechRecognizer.isRecognitionAvailable(this)) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                        setRecognitionListener(this@SpeechRecognitionService)
                    }
                    Log.d(TAG, "SpeechRecognizer initialized successfully")
                } else {
                    Log.w(TAG, "SpeechRecognizer is NOT available on this device")
                }
            }
        }
    }

    fun startRecognition(language: String = "auto") {
        currentLanguage = language

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Cannot start speech recognition: RECORD_AUDIO permission missing")
            val errorMsg = "Microphone permission not granted"
            broadcastError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS, errorMsg)
            updateState(RecognitionState.ERROR)
            return
        }

        mainHandler.post {
            try {
                if (speechRecognizer == null) {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                        setRecognitionListener(this@SpeechRecognitionService)
                    }
                }

                _partialText.value = ""
                updateState(RecognitionState.PREPARING)

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, packageName)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)

                    val resolvedLocale = when {
                        language.equals("bn-BD", ignoreCase = true) || language.equals("bn", ignoreCase = true) -> "bn-BD"
                        language.equals("en-US", ignoreCase = true) || language.equals("en", ignoreCase = true) -> "en-US"
                        language.equals("en-GB", ignoreCase = true) -> "en-GB"
                        language.equals("hi-IN", ignoreCase = true) || language.equals("hi", ignoreCase = true) -> "hi-IN"
                        language.equals("es-ES", ignoreCase = true) || language.equals("es", ignoreCase = true) -> "es-ES"
                        language.equals("ar", ignoreCase = true) || language.startsWith("ar-", ignoreCase = true) -> "ar-SA"
                        else -> Locale.getDefault().toLanguageTag().ifBlank { "en-US" }
                    }

                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, resolvedLocale)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, resolvedLocale)
                }

                speechRecognizer?.startListening(intent)
                Log.d(TAG, "Started listening with locale: $language")
            } catch (e: Exception) {
                Log.e(TAG, "Error invoking startListening on SpeechRecognizer", e)
                broadcastError(SpeechRecognizer.ERROR_CLIENT, "Failed to start speech recognition: ${e.message}")
                updateState(RecognitionState.ERROR)
            }
        }
    }

    fun stopRecognition() {
        mainHandler.post {
            try {
                updateState(RecognitionState.PROCESSING)
                speechRecognizer?.stopListening()
                updateNotificationContent("Processing speech...")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping SpeechRecognizer", e)
            }
        }
    }

    fun cancelRecognition() {
        mainHandler.post {
            try {
                speechRecognizer?.cancel()
                updateState(RecognitionState.IDLE)
                _partialText.value = ""
                _audioRms.value = 0f
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling SpeechRecognizer", e)
            }
        }
    }

    private fun updateState(newState: RecognitionState) {
        _state.value = newState
        val isListening = newState == RecognitionState.LISTENING || newState == RecognitionState.PREPARING

        // Notify callbacks
        callbacks.forEach { it.onStateChanged(newState) }

        // Broadcast state change
        val intent = Intent(ACTION_SPEECH_STATE_CHANGED).apply {
            setPackage(packageName)
            putExtra(EXTRA_STATE, newState.name)
            putExtra(EXTRA_IS_LISTENING, isListening)
        }
        sendBroadcast(intent)
    }

    private fun broadcastResult(text: String, confidence: Float = 1.0f) {
        _lastRecognizedText.value = text
        _partialText.value = ""

        callbacks.forEach { it.onSpeechRecognized(text, confidence) }

        val intent = Intent(ACTION_SPEECH_RESULT).apply {
            setPackage(packageName)
            putExtra(EXTRA_SPEECH_RESULT, text)
            putExtra(EXTRA_COMMAND, text)
            putExtra(EXTRA_CONFIDENCE, confidence)
        }
        sendBroadcast(intent)
    }

    private fun broadcastPartialResult(partialText: String) {
        _partialText.value = partialText

        callbacks.forEach { it.onPartialSpeech(partialText) }

        val intent = Intent(ACTION_SPEECH_PARTIAL_RESULT).apply {
            setPackage(packageName)
            putExtra(EXTRA_PARTIAL_RESULT, partialText)
        }
        sendBroadcast(intent)
    }

    private fun broadcastError(errorCode: Int, errorMessage: String) {
        callbacks.forEach { it.onError(errorCode, errorMessage) }

        val intent = Intent(ACTION_SPEECH_ERROR).apply {
            setPackage(packageName)
            putExtra(EXTRA_ERROR_CODE, errorCode)
            putExtra(EXTRA_ERROR_MESSAGE, errorMessage)
        }
        sendBroadcast(intent)
    }

    private fun broadcastRms(rmsDb: Float) {
        val normalized = ((rmsDb + 2f) / 12f).coerceIn(0f, 1f)
        _audioRms.value = normalized

        callbacks.forEach { it.onRmsChanged(rmsDb) }

        val intent = Intent(ACTION_SPEECH_RMS_CHANGED).apply {
            setPackage(packageName)
            putExtra(EXTRA_RMS_DB, rmsDb)
        }
        sendBroadcast(intent)
    }

    // RecognitionListener Callbacks

    override fun onReadyForSpeech(params: Bundle?) {
        Log.d(TAG, "onReadyForSpeech")
        updateState(RecognitionState.LISTENING)
        updateNotificationContent("Listening to your voice...")
    }

    override fun onBeginningOfSpeech() {
        Log.d(TAG, "onBeginningOfSpeech")
        updateState(RecognitionState.LISTENING)
    }

    override fun onRmsChanged(rmsdB: Float) {
        broadcastRms(rmsdB)
    }

    override fun onBufferReceived(buffer: ByteArray?) {
        // Raw audio buffer received
    }

    override fun onEndOfSpeech() {
        Log.d(TAG, "onEndOfSpeech")
        updateState(RecognitionState.PROCESSING)
        updateNotificationContent("Processing voice command...")
    }

    override fun onError(error: Int) {
        val msg = getErrorMessage(error)
        Log.w(TAG, "SpeechRecognizer onError: code=$error ($msg)")
        updateState(RecognitionState.ERROR)
        broadcastError(error, msg)

        // Reset to idle after transient error
        mainHandler.postDelayed({
            if (_state.value == RecognitionState.ERROR) {
                updateState(RecognitionState.IDLE)
            }
        }, 1500)
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val scores = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
        val text = matches?.firstOrNull()?.trim().orEmpty()
        val confidence = scores?.firstOrNull() ?: 1.0f

        Log.d(TAG, "onResults: '$text' (confidence=$confidence)")

        if (text.isNotBlank()) {
            broadcastResult(text, confidence)
        } else {
            broadcastError(SpeechRecognizer.ERROR_NO_MATCH, "No speech recognized")
        }

        updateState(RecognitionState.IDLE)
        _audioRms.value = 0f
        updateNotificationContent("Voice processed: $text")
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val partial = matches?.firstOrNull()?.trim().orEmpty()
        if (partial.isNotBlank()) {
            Log.d(TAG, "onPartialResults: '$partial'")
            broadcastPartialResult(partial)
            updateNotificationContent("Hearing: \"$partial\"")
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {
        // Reserved for future events
    }

    // Foreground Notification Management

    private fun promoteToForeground(statusText: String) {
        val notification = buildNotification(statusText)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            isBoundOrStartedAsForeground = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start foreground service", e)
        }
    }

    private fun updateNotificationContent(statusText: String) {
        if (!isBoundOrStartedAsForeground) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }
        try {
            val notification = buildNotification(statusText)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to update notification", e)
        }
    }

    private fun stopForegroundIfActive() {
        if (isBoundOrStartedAsForeground) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            isBoundOrStartedAsForeground = false
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val openAppIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingOpenApp = if (openAppIntent != null) {
            PendingIntent.getActivity(
                this,
                0,
                openAppIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else null

        val stopIntent = Intent(this, SpeechRecognitionService::class.java).apply {
            action = ACTION_STOP_LISTENING
        }
        val pendingStop = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val cancelIntent = Intent(this, SpeechRecognitionService::class.java).apply {
            action = ACTION_CANCEL_LISTENING
        }
        val pendingCancel = PendingIntent.getService(
            this,
            2,
            cancelIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("J.A.R.V.I.S. Voice Assistant")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingOpenApp)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_media_pause, "Finish", pendingStop)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", pendingCancel)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "J.A.R.V.I.S. Speech Recognition",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Speech recognition and voice processing service"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        mainHandler.post {
            try {
                speechRecognizer?.destroy()
                speechRecognizer = null
            } catch (e: Exception) {
                Log.e(TAG, "Error destroying speechRecognizer", e)
            }
        }
        callbacks.clear()
        stopForegroundIfActive()
        super.onDestroy()
    }
}
