package com.example.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

class JarvisVoiceManager(
    context: Context,
    private val onSpeakingStateChanged: (Boolean) -> Unit = {}
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var speechRate = 1.05f

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("JarvisVoiceManager", "Error initializing TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w("JarvisVoiceManager", "TTS Locale.US not supported, trying default")
                tts?.setLanguage(Locale.getDefault())
            }
            isInitialized = true
            tts?.setPitch(0.95f)
            tts?.setSpeechRate(speechRate)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    onSpeakingStateChanged(true)
                }

                override fun onDone(utteranceId: String?) {
                    onSpeakingStateChanged(false)
                }

                override fun onError(utteranceId: String?) {
                    onSpeakingStateChanged(false)
                }
            })
            Log.d("JarvisVoiceManager", "TTS initialized successfully")
        } else {
            Log.e("JarvisVoiceManager", "TTS Initialization failed with status: $status")
        }
    }

    fun speak(text: String, preferredLanguage: String = "auto") {
        if (text.isBlank()) return
        if (isInitialized && tts != null) {
            try {
                // Dynamically select TTS locale based on content or user preference
                val targetLocale = when {
                    preferredLanguage.startsWith("bn", ignoreCase = true) || text.any { it in '\u0980'..'\u09FF' } -> {
                        Locale("bn", "BD")
                    }
                    preferredLanguage.startsWith("hi", ignoreCase = true) || text.any { it in '\u0900'..'\u097F' } -> {
                        Locale("hi", "IN")
                    }
                    preferredLanguage.startsWith("ar", ignoreCase = true) || text.any { it in '\u0600'..'\u06FF' } -> {
                        Locale("ar")
                    }
                    preferredLanguage.startsWith("es", ignoreCase = true) -> {
                        Locale("es", "ES")
                    }
                    preferredLanguage.startsWith("en", ignoreCase = true) -> {
                        Locale.US
                    }
                    else -> Locale.US
                }

                val langResult = tts?.setLanguage(targetLocale)
                if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Try fallback language
                    if (targetLocale.language == "bn") {
                        tts?.setLanguage(Locale("bn", "IN"))
                    } else {
                        tts?.setLanguage(Locale.getDefault())
                    }
                }

                tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "JARVIS_VOICE_${System.currentTimeMillis()}")
            } catch (e: Exception) {
                Log.e("JarvisVoiceManager", "TTS speak error", e)
                onSpeakingStateChanged(false)
            }
        }
    }

    fun stop() {
        if (isInitialized && tts != null) {
            try {
                tts?.stop()
            } catch (e: Exception) {
                Log.e("JarvisVoiceManager", "TTS stop error", e)
            } finally {
                onSpeakingStateChanged(false)
            }
        }
    }

    fun setSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(0.5f, 2.0f)
        if (isInitialized) {
            tts?.setSpeechRate(speechRate)
        }
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e("JarvisVoiceManager", "TTS shutdown error", e)
        } finally {
            tts = null
            isInitialized = false
        }
    }
}
