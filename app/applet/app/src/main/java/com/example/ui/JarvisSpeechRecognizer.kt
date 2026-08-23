package com.example.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognitionService
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat

data class SpeechDiagnosticReport(
    val hasRecordAudioPermission: Boolean,
    val permissionStatusString: String,
    val isRecognitionAvailable: Boolean,
    val availableRecognitionServices: List<String>,
    val defaultEngineReady: Boolean,
    val diagnosticSummary: String
)

class JarvisSpeechRecognizer(
    private val context: Context,
    private val onCommand: (String) -> Unit,
    private val onStatusChange: (Boolean) -> Unit,
    private val onPartialResult: (String) -> Unit = {},
    private val onRmsChangedCallback: (Float) -> Unit = {},
    private val onErrorCallback: (String) -> Unit = {}
) : RecognitionListener {

    companion object {
        private const val TAG = "JarvisSpeech"
        private const val DIAGNOSTIC_TAG = "JarvisSpeechDiagnostic"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isCurrentlyListening = false

    init {
        mainHandler.post {
            runDiagnosticCheck()
            initRecognizer()
        }
    }

    /**
     * Diagnostic check validating RECORD_AUDIO permission status and SpeechRecognizer engine availability.
     */
    fun runDiagnosticCheck(): SpeechDiagnosticReport {
        val permissionState = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        )
        val hasPermission = permissionState == PackageManager.PERMISSION_GRANTED
        val permStatusStr = if (hasPermission) "GRANTED" else "DENIED (permission_state=$permissionState)"

        val isAvailable = SpeechRecognizer.isRecognitionAvailable(context)

        // Query available recognition service packages
        val availableServices = mutableListOf<String>()
        try {
            val recognitionIntent = Intent(RecognitionService.SERVICE_INTERFACE)
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PackageManager.MATCH_ALL
            } else {
                0
            }
            val resolveInfos: List<ResolveInfo> = context.packageManager.queryIntentServices(recognitionIntent, flags)
            for (info in resolveInfos) {
                val serviceInfo = info.serviceInfo
                if (serviceInfo != null) {
                    availableServices.add("${serviceInfo.packageName}/${serviceInfo.name}")
                }
            }
        } catch (e: Exception) {
            Log.w(DIAGNOSTIC_TAG, "Could not query available recognition services", e)
        }

        val summaryBuilder = StringBuilder()
        summaryBuilder.append("=== SPEECH RECOGNITION DIAGNOSTICS ===\n")
        summaryBuilder.append("• RECORD_AUDIO Permission: $permStatusStr\n")
        summaryBuilder.append("• SpeechRecognizer.isRecognitionAvailable(): $isAvailable\n")
        summaryBuilder.append("• Installed Recognition Services (${availableServices.size}):\n")
        if (availableServices.isEmpty()) {
            summaryBuilder.append("   - None detected via queryIntentServices\n")
        } else {
            availableServices.forEach { service ->
                summaryBuilder.append("   - $service\n")
            }
        }
        summaryBuilder.append("• Target SDK: ${context.applicationInfo.targetSdkVersion}, Device OS: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
        summaryBuilder.append("======================================")

        val diagnosticSummary = summaryBuilder.toString()
        Log.i(DIAGNOSTIC_TAG, diagnosticSummary)

        return SpeechDiagnosticReport(
            hasRecordAudioPermission = hasPermission,
            permissionStatusString = permStatusStr,
            isRecognitionAvailable = isAvailable,
            availableRecognitionServices = availableServices,
            defaultEngineReady = speechRecognizer != null,
            diagnosticSummary = diagnosticSummary
        )
    }

    private fun initRecognizer() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null

            val isAvailable = SpeechRecognizer.isRecognitionAvailable(context)
            Log.d(TAG, "Initializing SpeechRecognizer: isRecognitionAvailable = $isAvailable")

            if (isAvailable) {
                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(this@JarvisSpeechRecognizer)
                }
                Log.d(TAG, "Standard SpeechRecognizer created successfully")
            } else {
                Log.w(TAG, "SpeechRecognizer not reported as available by default; attempting Google Voice Service fallback...")
                val googleRecognitionService = ComponentName(
                    "com.google.android.googlequicksearchbox",
                    "com.google.android.voicesearch.service.RecognitionService"
                )
                try {
                    speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context, googleRecognitionService).apply {
                        setRecognitionListener(this@JarvisSpeechRecognizer)
                    }
                    Log.d(TAG, "Fallback Google SpeechRecognizer created")
                } catch (e: Exception) {
                    Log.e(TAG, "Speech Recognition service fallback failed", e)
                    onErrorCallback("Speech recognition service not found or disabled on this device.")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize SpeechRecognizer instance", e)
            onErrorCallback("Error initializing voice engine: ${e.localizedMessage}")
        }
    }

    fun startListening() {
        mainHandler.post {
            try {
                // Diagnostic check for audio permission
                val diagnostic = runDiagnosticCheck()
                if (!diagnostic.hasRecordAudioPermission) {
                    Log.w(TAG, "[Diagnostic Alert] startListening() aborted: RECORD_AUDIO is ${diagnostic.permissionStatusString}")
                    onErrorCallback("Microphone permission required for voice mode.")
                    onStatusChange(false)
                    return@post
                }

                if (speechRecognizer == null) {
                    Log.d(TAG, "SpeechRecognizer was null during startListening, re-initializing...")
                    initRecognizer()
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                }

                isCurrentlyListening = true
                onStatusChange(true)
                onPartialResult("")
                speechRecognizer?.startListening(intent)
                Log.d(TAG, "startListening() called successfully on SpeechRecognizer")
            } catch (e: Exception) {
                Log.e(TAG, "Exception starting speech listening session", e)
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
                Log.d(TAG, "stopListening() called")
            } catch (e: Exception) {
                Log.e(TAG, "Error in stopListening()", e)
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
                Log.d(TAG, "cancelListening() called")
            } catch (e: Exception) {
                Log.e(TAG, "Error in cancelListening()", e)
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
        Log.d(TAG, "RecognitionListener: onReadyForSpeech")
        isCurrentlyListening = true
        onStatusChange(true)
    }

    override fun onBeginningOfSpeech() {
        Log.d(TAG, "RecognitionListener: onBeginningOfSpeech")
    }

    override fun onRmsChanged(rmsdB: Float) {
        if (isCurrentlyListening) {
            onRmsChangedCallback(rmsdB)
        }
    }

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        Log.d(TAG, "RecognitionListener: onEndOfSpeech")
        isCurrentlyListening = false
        onRmsChangedCallback(0f)
    }

    override fun onError(error: Int) {
        val (errorName, errorDescription) = resolveSpeechErrorCode(error)

        val permissionGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        Log.e(
            TAG,
            "[SpeechRecognizer Error Diagnostic] Code: $error ($errorName) -> $errorDescription | RECORD_AUDIO granted: $permissionGranted | isRecognitionAvailable: ${SpeechRecognizer.isRecognitionAvailable(context)}"
        )

        isCurrentlyListening = false
        onStatusChange(false)
        onRmsChangedCallback(0f)

        // For busy or client error, re-init recognizer after slight delay to restore health
        if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_CLIENT) {
            mainHandler.postDelayed({
                Log.d(TAG, "Attempting recovery re-initialization after error code $error ($errorName)")
                initRecognizer()
            }, 300)
        }

        if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
            onErrorCallback(errorDescription)
        }
    }

    override fun onResults(results: Bundle?) {
        isCurrentlyListening = false
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val spokenText = matches[0]
            Log.d(TAG, "RecognitionListener: onResults received -> '$spokenText'")
            onCommand(spokenText)
        }
        onStatusChange(false)
        onRmsChangedCallback(0f)
        onPartialResult("")
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val partialMatches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!partialMatches.isNullOrEmpty()) {
            val liveText = partialMatches[0]
            Log.d(TAG, "RecognitionListener: onPartialResults -> '$liveText'")
            onPartialResult(liveText)
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {
        Log.d(TAG, "RecognitionListener: onEvent (type: $eventType)")
    }

    /**
     * Resolves Android SpeechRecognizer integer error codes into named constants and diagnostic descriptions.
     */
    private fun resolveSpeechErrorCode(errorCode: Int): Pair<String, String> {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> Pair("ERROR_AUDIO (3)", "Audio recording error. Check microphone hardware / input stream.")
            SpeechRecognizer.ERROR_CLIENT -> Pair("ERROR_CLIENT (5)", "Client side error. Recognizer client disconnected or invalidated.")
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> Pair("ERROR_INSUFFICIENT_PERMISSIONS (9)", "Insufficient permissions. RECORD_AUDIO permission is denied.")
            SpeechRecognizer.ERROR_NETWORK -> Pair("ERROR_NETWORK (2)", "Network connection error while connecting to speech recognition servers.")
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> Pair("ERROR_NETWORK_TIMEOUT (1)", "Network timeout waiting for speech recognition server response.")
            SpeechRecognizer.ERROR_NO_MATCH -> Pair("ERROR_NO_MATCH (7)", "No speech recognition match found for audio.")
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> Pair("ERROR_RECOGNIZER_BUSY (8)", "RecognitionService is busy processing another request.")
            SpeechRecognizer.ERROR_SERVER -> Pair("ERROR_SERVER (4)", "Server sent an error response.")
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> Pair("ERROR_SPEECH_TIMEOUT (6)", "No speech input detected within timeout window.")
            10 -> Pair("ERROR_TOO_MANY_REQUESTS (10)", "Too many requests to speech recognition service.")
            11 -> Pair("ERROR_SERVER_DISCONNECTED (11)", "Server disconnected prematurely.")
            12 -> Pair("ERROR_LANGUAGE_NOT_SUPPORTED (12)", "Requested language is not supported.")
            13 -> Pair("ERROR_LANGUAGE_UNAVAILABLE (13)", "Requested language is currently unavailable.")
            14 -> Pair("ERROR_CANNOT_CHECK_SUPPORT (14)", "Cannot check recognition language support.")
            else -> Pair("UNKNOWN_ERROR ($errorCode)", "Unknown SpeechRecognizer error code: $errorCode")
        }
    }
}
