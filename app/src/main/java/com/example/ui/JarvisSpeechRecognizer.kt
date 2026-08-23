package com.example.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.Locale

data class SpeechDiagnosticReport(
    val hasRecordAudioPermission: Boolean,
    val isRecognitionAvailable: Boolean,
    val availableServicesCount: Int,
    val serviceNames: List<String>,
    val lastRecordedError: String? = null
)

class JarvisSpeechRecognizer(
    private val context: Context,
    private val onCommand: (String) -> Unit,
    private val onStatusChange: (Boolean) -> Unit,
    private val onPartialResult: (String) -> Unit = {},
    private val onRmsChangedCallback: (Float) -> Unit = {},
    private val onErrorCallback: (String) -> Unit = {}
) : RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isCurrentlyListening = false
    private var lastRecordedErrorCode: Int? = null

    companion object {
        private const val TAG = "JarvisSpeech"

        /**
         * Diagnostic helper to translate standard Android SpeechRecognizer error codes
         * into detailed human-readable and technical log descriptions.
         */
        fun getDetailedErrorDiagnostic(errorCode: Int): Pair<String, String> {
            return when (errorCode) {
                SpeechRecognizer.ERROR_AUDIO -> Pair(
                    "Audio recording error",
                    "ERROR_AUDIO (Code 3): Failed to read audio stream. Check microphone hardware or if another app is monopolizing the mic."
                )
                SpeechRecognizer.ERROR_CLIENT -> Pair(
                    "Client side connection error",
                    "ERROR_CLIENT (Code 5): Generic client-side failure. SpeechRecognizer client state became invalid or crashed."
                )
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> Pair(
                    "Microphone permission denied",
                    "ERROR_INSUFFICIENT_PERMISSIONS (Code 9): android.permission.RECORD_AUDIO has NOT been granted by user."
                )
                SpeechRecognizer.ERROR_NETWORK -> Pair(
                    "Network connection error",
                    "ERROR_NETWORK (Code 2): Device cannot connect to speech recognition cloud server. Verify internet connectivity."
                )
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> Pair(
                    "Network timeout",
                    "ERROR_NETWORK_TIMEOUT (Code 1): Speech recognition server timed out waiting for data packets."
                )
                SpeechRecognizer.ERROR_NO_MATCH -> Pair(
                    "No speech recognized",
                    "ERROR_NO_MATCH (Code 7): Speech recognition engine processed audio but did not match any intelligible words."
                )
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> Pair(
                    "Recognition service busy",
                    "ERROR_RECOGNIZER_BUSY (Code 8): SpeechRecognizer is already active or bound by another process. Reinitializing..."
                )
                SpeechRecognizer.ERROR_SERVER -> Pair(
                    "Server error",
                    "ERROR_SERVER (Code 4): Speech recognition server returned an internal server error response."
                )
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> Pair(
                    "No speech input detected",
                    "ERROR_SPEECH_TIMEOUT (Code 6): No audio input was heard within the listening timeout period."
                )
                SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> Pair(
                    "Language not supported",
                    "ERROR_LANGUAGE_NOT_SUPPORTED (Code 12): Requested language locale is not supported on this device."
                )
                SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> Pair(
                    "Language unavailable",
                    "ERROR_LANGUAGE_UNAVAILABLE (Code 13): Language data pack is not downloaded or unavailable."
                )
                SpeechRecognizer.ERROR_SERVER_DISCONNECTED -> Pair(
                    "Server disconnected",
                    "ERROR_SERVER_DISCONNECTED (Code 11): Speech recognition service connection was abruptly terminated."
                )
                SpeechRecognizer.ERROR_TOO_MANY_REQUESTS -> Pair(
                    "Too many requests",
                    "ERROR_TOO_MANY_REQUESTS (Code 10): Speech recognition rate limits exceeded on device or server."
                )
                else -> Pair(
                    "Speech recognition error ($errorCode)",
                    "UNKNOWN_ERROR (Code $errorCode): Uncategorized SpeechRecognizer exception."
                )
            }
        }
    }

    init {
        mainHandler.post {
            runDiagnosticsAndInit()
        }
    }

    /**
     * Comprehensive Diagnostic Check:
     * 1. Validates RECORD_AUDIO runtime permission status
     * 2. Checks SpeechRecognizer platform availability
     * 3. Queries registered recognition services via Intent
     * 4. Logs diagnostic details to Logcat
     */
    fun runDiagnosticCheck(): SpeechDiagnosticReport {
        val hasRecordPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        val isRecognitionAvailable = SpeechRecognizer.isRecognitionAvailable(context)
        val packageManager = context.packageManager
        val recognitionIntent = Intent(android.speech.RecognitionService.SERVICE_INTERFACE)
        val availableServices = packageManager.queryIntentServices(recognitionIntent, 0)
        val serviceNames = availableServices.map { it.serviceInfo.name }
        val lastErrorDesc = lastRecordedErrorCode?.let { getDetailedErrorDiagnostic(it).second }

        Log.i(TAG, "========== SPEECH RECOGNIZER DIAGNOSTICS ==========")
        Log.i(TAG, "RECORD_AUDIO Permission Granted: $hasRecordPermission")
        Log.i(TAG, "SpeechRecognizer.isRecognitionAvailable: $isRecognitionAvailable")
        Log.i(TAG, "Available Recognition Services (${availableServices.size}): $serviceNames")
        Log.i(TAG, "Device: ${Build.MANUFACTURER} ${Build.MODEL} (SDK ${Build.VERSION.SDK_INT})")
        Log.i(TAG, "Package: ${context.packageName}")
        if (lastErrorDesc != null) {
            Log.i(TAG, "Last Recorded Speech Error: $lastErrorDesc")
        }
        Log.i(TAG, "===================================================")

        return SpeechDiagnosticReport(
            hasRecordAudioPermission = hasRecordPermission,
            isRecognitionAvailable = isRecognitionAvailable,
            availableServicesCount = availableServices.size,
            serviceNames = serviceNames,
            lastRecordedError = lastErrorDesc
        )
    }

    private fun runDiagnosticsAndInit() {
        val report = runDiagnosticCheck()

        if (!report.hasRecordAudioPermission) {
            Log.w(TAG, "[DIAGNOSTIC WARNING] android.permission.RECORD_AUDIO is NOT granted.")
        }

        if (!report.isRecognitionAvailable && report.availableServicesCount == 0) {
            Log.e(TAG, "[DIAGNOSTIC ERROR] No Speech Recognition engine installed or available on this device.")
        }

        initRecognizer()
    }

    private fun initRecognizer() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null

            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(this@JarvisSpeechRecognizer)
                }
                Log.d(TAG, "Standard SpeechRecognizer created successfully")
            } else {
                val googleRecognitionService = ComponentName(
                    "com.google.android.googlequicksearchbox",
                    "com.google.android.voicesearch.service.RecognitionService"
                )
                try {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context, googleRecognitionService).apply {
                        setRecognitionListener(this@JarvisSpeechRecognizer)
                    }
                    Log.d(TAG, "Fallback Google QuickSearchBox SpeechRecognizer created")
                } catch (e: Exception) {
                    Log.e(TAG, "[DIAGNOSTIC FAILURE] Speech Recognition is not available on this device", e)
                    onErrorCallback("Speech recognition service not found on device.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[DIAGNOSTIC FAILURE] Failed to initialize SpeechRecognizer instance", e)
            onErrorCallback("Error initializing voice engine: ${e.localizedMessage}")
        }
    }

    fun startListening(language: String = "auto") {
        mainHandler.post {
            try {
                // Diagnostic check for RECORD_AUDIO permission status
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED

                if (!hasPermission) {
                    val errMsg = "Microphone permission (RECORD_AUDIO) required for voice mode."
                    Log.w(TAG, "[DIAGNOSTIC FAILED] Cannot start listening - $errMsg")
                    onErrorCallback(errMsg)
                    onStatusChange(false)
                    return@post
                }

                if (speechRecognizer == null) {
                    Log.d(TAG, "SpeechRecognizer was null; re-initializing...")
                    initRecognizer()
                }

                val targetLanguage = when {
                    language.startsWith("bn", ignoreCase = true) -> "bn-BD"
                    language.startsWith("hi", ignoreCase = true) -> "hi-IN"
                    language.startsWith("es", ignoreCase = true) -> "es-ES"
                    language.startsWith("ar", ignoreCase = true) -> "ar-SA"
                    language.startsWith("en", ignoreCase = true) -> "en-US"
                    else -> Locale.getDefault().toLanguageTag().ifBlank { "en-US" }
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, targetLanguage)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, targetLanguage)
                    putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("bn-BD", "bn-IN", "en-US", "hi-IN"))
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                }

                isCurrentlyListening = true
                onStatusChange(true)
                onPartialResult("")
                speechRecognizer?.startListening(intent)
                Log.d(TAG, "[DIAGNOSTIC] startListening called on SpeechRecognizer successfully with language $targetLanguage")
            } catch (e: Exception) {
                Log.e(TAG, "[DIAGNOSTIC EXCEPTION] Error starting speech listening", e)
                isCurrentlyListening = false
                onStatusChange(false)
                onErrorCallback("Failed to start voice listener: ${e.localizedMessage}")
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            try {
                isCurrentlyListening = false
                speechRecognizer?.stopListening()
                Log.d(TAG, "stopListening called")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping speech listening", e)
            } finally {
                onStatusChange(false)
                onRmsChangedCallback(0f)
            }
        }
    }

    fun cancelListening() {
        mainHandler.post {
            try {
                isCurrentlyListening = false
                speechRecognizer?.cancel()
                Log.d(TAG, "cancelListening called")
            } catch (e: Exception) {
                Log.e(TAG, "Error cancelling speech listening", e)
            } finally {
                onStatusChange(false)
                onRmsChangedCallback(0f)
                onPartialResult("")
            }
        }
    }

    fun destroy() {
        mainHandler.post {
            try {
                isCurrentlyListening = false
                speechRecognizer?.destroy()
                speechRecognizer = null
                Log.d(TAG, "SpeechRecognizer destroyed cleanly")
            } catch (e: Exception) {
                Log.e(TAG, "Error destroying SpeechRecognizer", e)
            }
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {
        Log.d(TAG, "[DIAGNOSTIC EVENT] onReadyForSpeech - Audio recorder ready")
        isCurrentlyListening = true
        onStatusChange(true)
    }

    override fun onBeginningOfSpeech() {
        Log.d(TAG, "[DIAGNOSTIC EVENT] onBeginningOfSpeech - User speech detected")
    }

    override fun onRmsChanged(rmsdB: Float) {
        if (isCurrentlyListening) {
            onRmsChangedCallback(rmsdB)
        }
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        Log.d(TAG, "[DIAGNOSTIC EVENT] onEndOfSpeech - Processing speech input")
        isCurrentlyListening = false
        onRmsChangedCallback(0f)
    }

    override fun onError(error: Int) {
        lastRecordedErrorCode = error
        val (userMessage, detailedLog) = getDetailedErrorDiagnostic(error)

        Log.w(TAG, "[DIAGNOSTIC ERROR] $detailedLog")
        isCurrentlyListening = false
        onStatusChange(false)
        onRmsChangedCallback(0f)

        // For busy or client error, reinit recognizer to clear stale state
        if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_CLIENT) {
            Log.d(TAG, "[DIAGNOSTIC RECOVERY] Reinitializing SpeechRecognizer after error $error...")
            mainHandler.postDelayed({
                initRecognizer()
            }, 300)
        }

        if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
            onErrorCallback(userMessage)
        }
    }

    override fun onResults(results: Bundle?) {
        isCurrentlyListening = false
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val spokenText = matches[0]
            Log.d(TAG, "[DIAGNOSTIC SUCCESS] Spoken text recognized: '$spokenText' (${matches.size} candidate(s))")
            onCommand(spokenText)
        } else {
            Log.d(TAG, "[DIAGNOSTIC WARNING] onResults returned empty candidates")
        }
        onStatusChange(false)
        onRmsChangedCallback(0f)
        onPartialResult("")
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val partialMatches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!partialMatches.isNullOrEmpty()) {
            val liveText = partialMatches[0]
            Log.d(TAG, "[DIAGNOSTIC STREAM] Partial recognition: '$liveText'")
            onPartialResult(liveText)
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {
        Log.d(TAG, "[DIAGNOSTIC EVENT] onEvent (Type: $eventType)")
    }
}
