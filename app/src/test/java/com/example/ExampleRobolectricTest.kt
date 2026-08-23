package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.ui.ApiUsageStats
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {
  @Test
  fun read_string_from_context() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Jarvis AI", appName)
  }

  @Test
  fun test_api_usage_stats_calculation() {
    val stats = ApiUsageStats(
      totalCallsMade = 250,
      monthlyQuotaLimit = 1000
    )
    assertEquals(750, stats.remainingQuota)
    assertEquals(0.25f, stats.usageRatio, 0.001f)
  }

  @Test
  fun test_speech_recognition_service_error_messages() {
    val errorAudio = SpeechRecognitionService.getErrorMessage(android.speech.SpeechRecognizer.ERROR_AUDIO)
    assertTrue(errorAudio.contains("Audio recording error"))

    val errorPermission = SpeechRecognitionService.getErrorMessage(android.speech.SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)
    assertTrue(errorPermission.contains("Microphone permission required"))

    val errorNoMatch = SpeechRecognitionService.getErrorMessage(android.speech.SpeechRecognizer.ERROR_NO_MATCH)
    assertTrue(errorNoMatch.contains("No speech recognized"))
  }

  @Test
  fun test_speech_recognition_service_actions() {
    assertEquals("com.example.action.START_SPEECH_RECOGNITION", SpeechRecognitionService.ACTION_START_LISTENING)
    assertEquals("com.example.action.STOP_SPEECH_RECOGNITION", SpeechRecognitionService.ACTION_STOP_LISTENING)
    assertEquals("com.example.action.SPEECH_RESULT", SpeechRecognitionService.ACTION_SPEECH_RESULT)
  }

  @Test
  fun test_command_executor_wifi_parsing() {
    val parsedEnOn = com.example.service.CommandExecutor.parseQuery("Turn on Wi-Fi")
    assertEquals(com.example.service.CommandIntentType.TOGGLE_WIFI, parsedEnOn.intentType)
    assertEquals(true, parsedEnOn.booleanState)

    val parsedBnOff = com.example.service.CommandExecutor.parseQuery("ওয়াইফাই বন্ধ করো")
    assertEquals(com.example.service.CommandIntentType.TOGGLE_WIFI, parsedBnOff.intentType)
    assertEquals(false, parsedBnOff.booleanState)
    assertTrue(parsedBnOff.isBengali)
  }

  @Test
  fun test_command_executor_brightness_parsing() {
    val parsedEn80 = com.example.service.CommandExecutor.parseQuery("Set screen brightness to 80%")
    assertEquals(com.example.service.CommandIntentType.ADJUST_BRIGHTNESS, parsedEn80.intentType)
    assertEquals(80, parsedEn80.numericValue)

    val parsedBnIncrease = com.example.service.CommandExecutor.parseQuery("ব্রাইটনেস বাড়াও")
    assertEquals(com.example.service.CommandIntentType.ADJUST_BRIGHTNESS, parsedBnIncrease.intentType)
    assertEquals(true, parsedBnIncrease.booleanState)
  }

  @Test
  fun test_command_executor_app_launch_parsing() {
    val parsedEnApp = com.example.service.CommandExecutor.parseQuery("Open YouTube")
    assertEquals(com.example.service.CommandIntentType.LAUNCH_APP, parsedEnApp.intentType)
    assertEquals("YouTube", parsedEnApp.actionTarget)

    val parsedBnApp = com.example.service.CommandExecutor.parseQuery("ইউটিউব ওপেন করো")
    assertEquals(com.example.service.CommandIntentType.LAUNCH_APP, parsedBnApp.intentType)
    assertEquals("ইউটিউব", parsedBnApp.actionTarget)
  }
}
