package com.example.util

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.Calendar

enum class SuggestionCategory {
    MEETING_PREP,
    STRESS_RELIEF,
    MORNING_ROUTINE,
    WORK_FOCUS,
    NIGHT_REST
}

data class ContextualSuggestion(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val actionPrompt: String,
    val category: SuggestionCategory,
    val badgeText: String = "Proactive AI"
)

object ContextualAwarenessEngine {

    private val STRESS_KEYWORDS = listOf(
        "stressed", "stress", "anxious", "tired", "exhausted", "headache",
        "overwhelmed", "busy", "deadline", "pressure", "panic", "burnout", "too much work"
    )

    private val MEETING_KEYWORDS = listOf(
        "meeting", "calendar", "call", "zoom", "sync", "presentation",
        "interview", "appointment", "schedule", "discussion"
    )

    /**
     * Evaluates current user input, chat history, time of day, and environmental context
     * to generate intelligent, proactive suggestions.
     */
    fun generateProactiveSuggestions(
        context: Context,
        recentUserMessages: List<String> = emptyList()
    ): List<ContextualSuggestion> {
        val suggestions = mutableListOf<ContextualSuggestion>()
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)

        // 1. Analyze Recent User Messages for Stress or Meeting Signals
        val concatenatedText = recentUserMessages.takeLast(10).joinToString(" ").lowercase()
        val containsStress = STRESS_KEYWORDS.any { concatenatedText.contains(it) }
        val containsMeeting = MEETING_KEYWORDS.any { concatenatedText.contains(it) }

        // Stress Detection Trigger (Highest Priority if active)
        if (containsStress) {
            suggestions.add(
                ContextualSuggestion(
                    id = "stress_relief_ambient",
                    title = "🧘 Play Calming Lofi & Guided Relaxation",
                    description = "Detected signs of stress or heavy workload. Take a 2-minute mindful breather.",
                    icon = Icons.Default.SelfImprovement,
                    actionPrompt = "I am feeling stressed. Please play calming ambient sounds, guide me through a 1-minute deep breathing exercise, and give me a soothing phrase.",
                    category = SuggestionCategory.STRESS_RELIEF,
                    badgeText = "Stress Intervention"
                )
            )
        }

        // Meeting Preparation Trigger
        if (containsMeeting || hour in 9..11 || hour in 14..16) {
            suggestions.add(
                ContextualSuggestion(
                    id = "meeting_prep",
                    title = "📅 Prepare for Upcoming Meeting",
                    description = "Summarize talking points, action items, and generate a quick meeting agenda.",
                    icon = Icons.Default.Event,
                    actionPrompt = "Help me prepare for my upcoming meeting. Generate a 3-bullet talking point summary and key action items to review.",
                    category = SuggestionCategory.MEETING_PREP,
                    badgeText = "Meeting Prep"
                )
            )
        }

        // 2. Time-Based Pattern Recognition
        when (hour) {
            in 5..10 -> { // Morning Routine (5 AM - 10 AM)
                suggestions.add(
                    ContextualSuggestion(
                        id = "morning_briefing",
                        title = "🌅 Good Morning Briefing",
                        description = "Get today's agenda, weather update, and priority task overview.",
                        icon = Icons.Default.WbSunny,
                        actionPrompt = "Good morning J.A.R.V.I.S.! Give me a concise morning briefing with top 3 priorities, daily motivation, and general schedule preview.",
                        category = SuggestionCategory.MORNING_ROUTINE,
                        badgeText = "Morning Routine"
                    )
                )
            }
            in 11..17 -> { // Productivity & Work Focus (11 AM - 5 PM)
                suggestions.add(
                    ContextualSuggestion(
                        id = "focus_productivity",
                        title = "⚡ 25-Min Pomodoro Focus Session",
                        description = "Set a 25-minute focus timer and mute non-essential distractions.",
                        icon = Icons.Default.Timer,
                        actionPrompt = "Start a 25-minute focused work session. Give me 3 tips to stay focused and remind me when time is up.",
                        category = SuggestionCategory.WORK_FOCUS,
                        badgeText = "Focus Mode"
                    )
                )
            }
            in 18..22 -> { // Evening Wind Down (6 PM - 10 PM)
                suggestions.add(
                    ContextualSuggestion(
                        id = "evening_recap",
                        title = "🌆 Evening Review & Daily Progress",
                        description = "Review completed tasks today and log priority goals for tomorrow.",
                        icon = Icons.Default.NightsStay,
                        actionPrompt = "Let's review my daily accomplishments today. Help me list what went well and set 2 main goals for tomorrow.",
                        category = SuggestionCategory.NIGHT_REST,
                        badgeText = "Evening Review"
                    )
                )
            }
            else -> { // Late Night (11 PM - 4 AM)
                suggestions.add(
                    ContextualSuggestion(
                        id = "late_night_sleep",
                        title = "🌙 Bedtime Wind Down & Rest",
                        description = "Late night detected. Enable dark theme and prepare for restorative sleep.",
                        icon = Icons.Default.Bedtime,
                        actionPrompt = "It is late at night. Give me a relaxing bedtime thought, turn down brightness, and wish me good night.",
                        category = SuggestionCategory.NIGHT_REST,
                        badgeText = "Bedtime Mode"
                    )
                )
            }
        }

        // Always ensure at least 2 distinct suggestions
        if (suggestions.size < 2 && !containsStress) {
            suggestions.add(
                ContextualSuggestion(
                    id = "daily_automation",
                    title = "⚡ Execute Daily Automation Check",
                    description = "Verify system permissions, battery health, and background voice triggers.",
                    icon = Icons.Default.SmartToy,
                    actionPrompt = "System health check: confirm battery status, voice trigger readiness, and active permissions.",
                    category = SuggestionCategory.WORK_FOCUS,
                    badgeText = "System Check"
                )
            )
        }

        return suggestions
    }

    /**
     * Constructs dynamic context instructions to be injected into Gemini system prompt.
     */
    fun buildContextPromptInjection(
        recentUserMessages: List<String> = emptyList()
    ): String {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val timeOfDay = when (hour) {
            in 5..11 -> "Morning"
            in 12..17 -> "Afternoon"
            in 18..22 -> "Evening"
            else -> "Late Night"
        }

        val concatenatedText = recentUserMessages.takeLast(10).joinToString(" ").lowercase()
        val isStressed = STRESS_KEYWORDS.any { concatenatedText.contains(it) }

        val contextState = StringBuilder()
        contextState.append("[Context Awareness Engine: Current Time = $timeOfDay ($hour:00). ")
        if (isStressed) {
            contextState.append("User exhibits signs of stress/overwork. Use an extra calm, supportive tone and offer relaxation assistance. ")
        }
        contextState.append("Proactively adapt responses to user productivity patterns.]")

        return contextState.toString()
    }
}
