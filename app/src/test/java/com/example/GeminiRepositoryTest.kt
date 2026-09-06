package com.example

import com.example.network.GeminiRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class GeminiRepositoryTest {

    @Test
    fun testEmptyApiKeyReturnsFailure() = runBlocking {
        val repository = GeminiRepository()
        val result = repository.testApiKeyConnection("   ", "openai/gpt-3.5-turbo")

        assertTrue(result.isFailure)
        assertEquals("API Key cannot be empty.", result.exceptionOrNull()?.message)
    }

    @Test
    fun testApiKeySanitizationOnException() = runBlocking {
        val repository = GeminiRepository()
        val secretKey = "sk-or-v1-secret123456789key"

        // This will attempt a real network connection to openrouter.ai with invalid key/fake endpoint,
        // which throws an Exception. The catch block should sanitize secretKey from error message if present.
        val result = repository.testApiKeyConnection(secretKey, "invalid-model-name")

        assertTrue(result.isFailure)
        val errorMessage = result.exceptionOrNull()?.message ?: ""
        assertFalse(
            "Error message should not contain raw API key",
            errorMessage.contains(secretKey)
        )
    }
}
