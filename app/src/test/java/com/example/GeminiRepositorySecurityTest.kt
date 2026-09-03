package com.example

import com.example.network.GeminiRepository
import org.junit.Assert.assertEquals
import org.junit.Test

class GeminiRepositorySecurityTest {

    private val repository = GeminiRepository()

    @Test
    fun sanitizeApiKey_removesNewlinesAndControlCharacters() {
        val unsafeKey = "sk-or-v1-1234567890abcdef\r\n"
        val sanitized = repository.sanitizeApiKey(unsafeKey)
        assertEquals("sk-or-v1-1234567890abcdef", sanitized)
    }

    @Test
    fun sanitizeApiKey_removesInternalSpacesAndTabs() {
        val unsafeKey = "sk-or-v1-  1234\t567890\u0000abcdef"
        val sanitized = repository.sanitizeApiKey(unsafeKey)
        assertEquals("sk-or-v1-1234567890abcdef", sanitized)
    }

    @Test
    fun sanitizeApiKey_preservesValidKey() {
        val validKey = "sk-or-v1-abcdef1234567890"
        val sanitized = repository.sanitizeApiKey(validKey)
        assertEquals("sk-or-v1-abcdef1234567890", sanitized)
    }
}
