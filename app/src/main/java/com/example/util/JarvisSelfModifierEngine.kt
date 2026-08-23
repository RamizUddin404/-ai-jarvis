package com.example.util

import android.content.Context
import com.example.ui.JarvisViewModel

data class SelfModificationResult(
    val modified: Boolean,
    val description: String
)

object JarvisSelfModifierEngine {

    /**
     * Parses incoming command for self-modification instructions.
     * If matched, updates ViewModel settings and returns execution feedback.
     */
    fun processSelfModificationCommand(
        command: String,
        viewModel: JarvisViewModel,
        context: Context
    ): SelfModificationResult {
        val lower = command.lowercase().trim()
        val modifications = mutableListOf<String>()

        // 1. Theme / Visualizer Palette Change
        if (lower.contains("palette") || lower.contains("theme") || lower.contains("color")) {
            when {
                lower.contains("violet") || lower.contains("purple") -> {
                    viewModel.updateWaveformPalette("violet")
                    modifications.add("Set visual theme palette to Neon Violet")
                }
                lower.contains("emerald") || lower.contains("green") -> {
                    viewModel.updateWaveformPalette("emerald")
                    modifications.add("Set visual theme palette to Cyber Emerald")
                }
                lower.contains("amber") || lower.contains("orange") || lower.contains("gold") -> {
                    viewModel.updateWaveformPalette("amber")
                    modifications.add("Set visual theme palette to Arc Amber")
                }
                lower.contains("silver") || lower.contains("monochrome") || lower.contains("white") -> {
                    viewModel.updateWaveformPalette("monochrome")
                    modifications.add("Set visual theme palette to Matrix Silver")
                }
                lower.contains("cyan") || lower.contains("blue") || lower.contains("default") -> {
                    viewModel.updateWaveformPalette("cyan")
                    modifications.add("Set visual theme palette to Cyan Jarvis")
                }
            }
        }

        // 2. Waveform Style Mode
        if (lower.contains("waveform") || lower.contains("visualizer") || lower.contains("mode")) {
            when {
                lower.contains("bar") || lower.contains("spectrum") -> {
                    viewModel.updateWaveformStyle("bar")
                    modifications.add("Updated audio visualizer to Spectrum Bars")
                }
                lower.contains("line") || lower.contains("oscilloscope") -> {
                    viewModel.updateWaveformStyle("line")
                    modifications.add("Updated audio visualizer to Oscilloscope Grid")
                }
                lower.contains("ripple") || lower.contains("ring") -> {
                    viewModel.updateWaveformStyle("ripple")
                    modifications.add("Updated audio visualizer to Sound Ripples")
                }
                lower.contains("wave") || lower.contains("fluid") -> {
                    viewModel.updateWaveformStyle("wave")
                    modifications.add("Updated audio visualizer to Fluid Sine Wave")
                }
            }
        }

        // 3. Voice Speech Rate
        if (lower.contains("voice speed") || lower.contains("speak faster") || lower.contains("speak slower") || lower.contains("speed")) {
            val currentRate = viewModel.settings.value.voicePlaybackSpeed
            if (lower.contains("faster") || lower.contains("increase")) {
                val newRate = (currentRate + 0.2f).coerceAtMost(2.0f)
                viewModel.updateVoicePlaybackSpeed(newRate)
                modifications.add("Increased voice playback speed to ${"%.2f".format(newRate)}x")
            } else if (lower.contains("slower") || lower.contains("decrease")) {
                val newRate = (currentRate - 0.2f).coerceAtLeast(0.5f)
                viewModel.updateVoicePlaybackSpeed(newRate)
                modifications.add("Decreased voice playback speed to ${"%.2f".format(newRate)}x")
            } else {
                // Check if specific number passed (e.g. 1.2x)
                val numberMatch = Regex("(\\d+\\.\\d+|\\d+)").find(lower)
                numberMatch?.value?.toFloatOrNull()?.let { rate ->
                    if (rate in 0.5f..2.5f) {
                        viewModel.updateVoicePlaybackSpeed(rate)
                        modifications.add("Set voice speed to ${rate}x")
                    }
                }
            }
        }

        // 4. Language Selection
        if (lower.contains("language") || lower.contains("bengali") || lower.contains("bangla") || lower.contains("english")) {
            if (lower.contains("bangla") || lower.contains("bengali") || lower.contains("bn")) {
                viewModel.updateLanguage("bn")
                modifications.add("Switched primary voice and UI language to Bangla (bn-BD)")
            } else if (lower.contains("english") || lower.contains("en")) {
                viewModel.updateLanguage("en")
                modifications.add("Switched primary voice and UI language to English (en-US)")
            }
        }

        // 5. Wake Word & 24/7 Background Mode Toggle
        if (lower.contains("wake word") || lower.contains("hands free") || lower.contains("background")) {
            if (lower.contains("enable") || lower.contains("turn on") || lower.contains("start")) {
                viewModel.toggleWakeWord(true)
                viewModel.togglePersistentBackground(true)
                modifications.add("Enabled 24/7 background mode & hands-free 'Hey Jarvis' wake word detection")
            } else if (lower.contains("disable") || lower.contains("turn off") || lower.contains("stop")) {
                viewModel.toggleWakeWord(false)
                modifications.add("Disabled background wake word listener")
            }
        }

        // 6. System Prompt Personality Update
        if (lower.contains("act like") || lower.contains("personality") || lower.contains("system prompt")) {
            val newPrompt = when {
                lower.contains("tony stark") || lower.contains("iron man") -> {
                    "You are J.A.R.V.I.S., Tony Stark's ultra-intelligent, witty, and sophisticated AI assistant. Respond with technical precision, sleek humor, and helpful speed."
                }
                lower.contains("developer") || lower.contains("coder") || lower.contains("engineer") -> {
                    "You are J.A.R.V.I.S. Senior AI Engineer and Code Architect. Provide modular, production-ready Kotlin, Python, JS, and HTML code snippets with zero fluff."
                }
                else -> {
                    "You are J.A.R.V.I.S., an advanced AI assistant capable of real-time voice, phone execution, web code building, and contextual reasoning."
                }
            }
            viewModel.updateSystemPrompt(newPrompt)
            modifications.add("Reconfigured AI system prompt personality core")
        }

        return if (modifications.isNotEmpty()) {
            SelfModificationResult(
                modified = true,
                description = "⚡ J.A.R.V.I.S. Self-Modification Executed:\n• " + modifications.joinToString("\n• ")
            )
        } else {
            SelfModificationResult(modified = false, description = "")
        }
    }
}
