package com.example.data

import kotlinx.serialization.Serializable

@Serializable
data class OpenRouterRequest(
    val model: String,
    val messages: List<OpenRouterMessage>
)

@Serializable
data class OpenRouterMessage(
    val role: String,
    val content: String
)

@Serializable
data class OpenRouterResponse(
    val choices: List<OpenRouterChoice>
)

@Serializable
data class OpenRouterChoice(
    val message: OpenRouterMessage
)

data class OpenRouterModelOption(
    val id: String,
    val displayName: String,
    val provider: String,
    val description: String,
    val badge: String = ""
)

object OpenRouterPresets {
    val defaultModel = "openai/gpt-4o-mini"
    
    val popularModels = listOf(
        OpenRouterModelOption(
            id = "openai/gpt-4o-mini",
            displayName = "GPT-4o Mini",
            provider = "OpenAI",
            description = "Fast, affordable, intelligent flagship compact model",
            badge = "RECOMMENDED"
        ),
        OpenRouterModelOption(
            id = "google/gemini-2.0-flash-exp:free",
            displayName = "Gemini 2.0 Flash",
            provider = "Google",
            description = "Ultra fast, latest multimodal Gemini generation",
            badge = "FREE"
        ),
        OpenRouterModelOption(
            id = "anthropic/claude-3.5-sonnet",
            displayName = "Claude 3.5 Sonnet",
            provider = "Anthropic",
            description = "Industry-leading reasoning and nuance",
            badge = "POWERFUL"
        ),
        OpenRouterModelOption(
            id = "meta-llama/llama-3.3-70b-instruct",
            displayName = "Llama 3.3 70B",
            provider = "Meta",
            description = "State of the art open weights model",
            badge = "OPEN SOURCE"
        ),
        OpenRouterModelOption(
            id = "deepseek/deepseek-chat",
            displayName = "DeepSeek V3",
            provider = "DeepSeek",
            description = "High efficiency reasoning and general intelligence",
            badge = "ECONOMICAL"
        ),
        OpenRouterModelOption(
            id = "mistralai/mistral-small-24b-instruct-2501",
            displayName = "Mistral Small 24B",
            provider = "Mistral",
            description = "Fast European open-weight multilingual model",
            badge = "FAST"
        ),
        OpenRouterModelOption(
            id = "openai/gpt-3.5-turbo",
            displayName = "GPT-3.5 Turbo",
            provider = "OpenAI",
            description = "Classic standard conversational model",
            badge = "LEGACY"
        )
    )
}
