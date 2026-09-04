package com.example.network

import com.example.BuildConfig
import com.example.data.OpenRouterMessage
import com.example.data.OpenRouterRequest
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://openrouter.ai/"
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    val service: OpenRouterApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(OpenRouterApiService::class.java)
    }
}

class GeminiRepository {
    suspend fun generateResponse(
        prompt: String,
        systemPrompt: String? = null,
        userApiKey: String? = null,
        userModel: String? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = when {
            !userApiKey.isNullOrBlank() -> userApiKey.trim()
            BuildConfig.OPENROUTER_API_KEY.isNotBlank() && BuildConfig.OPENROUTER_API_KEY != "MY_OPENROUTER_API_KEY" -> BuildConfig.OPENROUTER_API_KEY.trim()
            else -> ""
        }
        
        if (apiKey.isEmpty()) {
            return@withContext "OpenRouter API Key is not configured. Please tap Settings (⚙️) at the top right to enter your OpenRouter API Key."
        }
        
        val effectiveModel = if (!userModel.isNullOrBlank()) userModel.trim() else "openai/gpt-3.5-turbo"
        val defaultSystemPrompt = "You are J.A.R.V.I.S., an advanced AI assistant. You have full multilingual capabilities with native support for English, Bangla (বাংলা), and all languages. Always respond in the exact language the user speaks or writes (if the user speaks Bangla, reply in natural, fluent Bangla; if English, reply in English). Provide concise, clear, and professional answers formatted as plain text suitable for a Text-to-Speech engine (no markdown asterisks, bolding, or lists)."
        val effectivePrompt = if (!systemPrompt.isNullOrBlank()) systemPrompt else defaultSystemPrompt
        
        val messages = listOf(
            OpenRouterMessage(
                role = "system",
                content = effectivePrompt
            ),
            OpenRouterMessage(
                role = "user",
                content = prompt
            )
        )
        
        val request = OpenRouterRequest(
            model = effectiveModel,
            messages = messages
        )
        
        try {
            val response = RetrofitClient.service.generateContent(
                authHeader = "Bearer $apiKey",
                referer = "https://aistudio.google.com",
                title = "Jarvis Assistant",
                request = request
            )
            response.choices.firstOrNull()?.message?.content ?: "No response from J.A.R.V.I.S."
        } catch (e: retrofit2.HttpException) {
            when (e.code()) {
                401 -> "Authentication error (HTTP 401): Invalid OpenRouter API Key. Please verify the key in Settings."
                402 -> "Payment required (HTTP 402): Insufficient OpenRouter credits/balance. Please recharge your account at openrouter.ai."
                429 -> "Rate limit exceeded (HTTP 429). Please check your OpenRouter API quota limit."
                else -> "An error occurred while processing your request (HTTP ${e.code()})."
            }
        } catch (e: Exception) {
            "Connection error: Unable to reach the service. Please check your network connection."
        }
    }

    suspend fun testApiKeyConnection(apiKey: String, model: String): Result<String> = withContext(Dispatchers.IO) {
        val trimmedKey = apiKey.trim()
        if (trimmedKey.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("API Key cannot be empty."))
        }
        val targetModel = if (model.isNotBlank()) model.trim() else "openai/gpt-3.5-turbo"
        val request = OpenRouterRequest(
            model = targetModel,
            messages = listOf(
                OpenRouterMessage(role = "user", content = "Reply with 'Connection verified.' in 2 words.")
            )
        )
        try {
            val response = RetrofitClient.service.generateContent(
                authHeader = "Bearer $trimmedKey",
                referer = "https://aistudio.google.com",
                title = "Jarvis API Verification",
                request = request
            )
            val reply = response.choices.firstOrNull()?.message?.content ?: "Connected."
            Result.success(reply.trim())
        } catch (e: retrofit2.HttpException) {
            val msg = when (e.code()) {
                401 -> "Invalid API Key (HTTP 401). Please check key characters."
                402 -> "Insufficient OpenRouter balance / credits (HTTP 402)."
                429 -> "Rate limit / Quota exceeded (HTTP 429)."
                else -> "HTTP error ${e.code()} occurred during connection test."
            }
            Result.failure(Exception(msg))
        } catch (e: Exception) {
            Result.failure(Exception("Network error connecting to OpenRouter"))
        }
    }

    suspend fun generateStudySummary(
        topic: String,
        sessionContent: String,
        userApiKey: String? = null,
        userModel: String? = null
    ): String {
        val prompt = """
            Generate a structured study session summary for topic: "$topic".

            Session Content and Notes:
            $sessionContent

            Please highlight:
            - Key Concepts
            - Core Facts
            - Questions & Answers Encountered

            Provide a concise, clear, and well-structured summary.
        """.trimIndent()

        val studySystemPrompt = "You are an expert AI tutor. Generate clear, structured study summaries highlighting key concepts, core facts, and questions/answers from study sessions."
        return generateResponse(
            prompt = prompt,
            systemPrompt = studySystemPrompt,
            userApiKey = userApiKey,
            userModel = userModel
        )
    }
}
