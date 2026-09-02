package com.example

import com.example.service.CommandExecutor
import com.example.service.CommandIntentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CommandExecutorTest {

    @Test
    fun testParseQueryWifiIntent() {
        val parsed = CommandExecutor.parseQuery("turn on wifi")
        assertEquals(CommandIntentType.TOGGLE_WIFI, parsed.intentType)
        assertEquals(true, parsed.booleanState)
        assertFalse(parsed.isBengali)
    }

    @Test
    fun testParseQueryBengaliWifiIntent() {
        val parsed = CommandExecutor.parseQuery("ওয়াইফাই চালু করো")
        assertEquals(CommandIntentType.TOGGLE_WIFI, parsed.intentType)
        assertEquals(true, parsed.booleanState)
        assertTrue(parsed.isBengali)
    }

    @Test
    fun testParseQueryExcessivelyLongInputTruncatedSafely() {
        // Create an input longer than MAX_QUERY_LENGTH (1000 characters)
        val longQuery = "wifi on " + "a".repeat(2000)
        val parsed = CommandExecutor.parseQuery(longQuery)

        // Should still correctly identify wifi intent without memory/performance failure
        assertEquals(CommandIntentType.TOGGLE_WIFI, parsed.intentType)
        assertEquals(true, parsed.booleanState)
    }

    @Test
    fun testParseQueryEmptyOrWhitespace() {
        val parsed = CommandExecutor.parseQuery("   ")
        assertEquals(CommandIntentType.UNKNOWN, parsed.intentType)
    }
}
