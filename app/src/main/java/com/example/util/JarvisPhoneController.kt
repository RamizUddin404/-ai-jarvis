package com.example.util

import android.accessibilityservice.AccessibilityService
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import com.example.JarvisAccessibilityService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PhoneCommandResult(
    val handled: Boolean,
    val responseText: String,
    val isBengali: Boolean = false
)

object JarvisPhoneController {
    private const val TAG = "JarvisPhoneCtrl"
    private var isTorchOn = false

    /**
     * Executes fast native phone actions based on voice/text command.
     * Supports both English and Bangla commands with full device control.
     */
    fun executeCommand(context: Context, rawCommand: String): PhoneCommandResult {
        val command = rawCommand.trim()
        val lower = command.lowercase(Locale.ROOT)
        val isBn = containsBengali(command)

        // 0. NLP COMMAND EXECUTOR (Wi-Fi toggle, Screen Brightness, App Launching, Bluetooth)
        val nlpResult = com.example.service.CommandExecutor.parseAndExecute(context, command)
        if (nlpResult.handled) {
            return PhoneCommandResult(
                handled = true,
                responseText = nlpResult.responseText,
                isBengali = nlpResult.isBengali
            )
        }

        // 1. FLASHLIGHT / TORCH CONTROL (English & Bangla)
        if (matchesAny(lower, listOf("turn on flashlight", "torch on", "flashlight on", "turn on torch", "enable flashlight", "লাইট জ্বালাও", "ফ্ল্যাশলাইট অন করো", "টর্চ জ্বালাও", "লাইট অন করো", "টর্চ অন"))) {
            val success = setTorch(context, true)
            return PhoneCommandResult(
                handled = true,
                responseText = if (isBn) {
                    if (success) "ফ্ল্যাশলাইট চালু করা হয়েছে।" else "ফ্ল্যাশলাইট চালু করা যায়নি।"
                } else {
                    if (success) "Flashlight turned on." else "Unable to toggle flashlight."
                },
                isBengali = isBn
            )
        }

        if (matchesAny(lower, listOf("turn off flashlight", "torch off", "flashlight off", "turn off torch", "disable flashlight", "লাইট বন্ধ করো", "ফ্ল্যাশলাইট বন্ধ করো", "টর্চ বন্ধ করো", "লাইট অফ করো", "টর্চ অফ"))) {
            val success = setTorch(context, false)
            return PhoneCommandResult(
                handled = true,
                responseText = if (isBn) {
                    if (success) "ফ্ল্যাশলাইট বন্ধ করা হয়েছে।" else "ফ্ল্যাশলাইট বন্ধ করা যায়নি।"
                } else {
                    if (success) "Flashlight turned off." else "Unable to toggle flashlight."
                },
                isBengali = isBn
            )
        }

        // 2. VOLUME CONTROLS (English & Bangla)
        if (matchesAny(lower, listOf("volume up", "increase volume", "louder", "সাউন্ড বাড়াও", "ভলিউম বাড়াও", "শব্দ বাড়াও"))) {
            adjustVolume(context, AudioManager.ADJUST_RAISE)
            return PhoneCommandResult(
                handled = true,
                responseText = if (isBn) "ভলিউম বাড়ানো হয়েছে।" else "Increasing volume level.",
                isBengali = isBn
            )
        }

        if (matchesAny(lower, listOf("volume down", "decrease volume", "lower volume", "সাউন্ড কমাও", "ভলিউম কমাও", "শব্দ কমাও"))) {
            adjustVolume(context, AudioManager.ADJUST_LOWER)
            return PhoneCommandResult(
                handled = true,
                responseText = if (isBn) "ভলিউম কমানো হয়েছে।" else "Decreasing volume level.",
                isBengali = isBn
            )
        }

        if (matchesAny(lower, listOf("mute", "mute volume", "silence", "মিউট করো", "সাউন্ড বন্ধ করো", "শব্দ বন্ধ করো"))) {
            setVolumeMute(context, true)
            return PhoneCommandResult(
                handled = true,
                responseText = if (isBn) "সাউন্ড মিউট করা হয়েছে।" else "Volume muted.",
                isBengali = isBn
            )
        }

        if (matchesAny(lower, listOf("unmute", "max volume", "maximum volume", "full volume", "সর্বোচ্চ সাউন্ড", "ফুল সাউন্ড"))) {
            setMaxVolume(context)
            return PhoneCommandResult(
                handled = true,
                responseText = if (isBn) "ভলিউম সর্বোচ্চ পর্যায়ে সেট করা হয়েছে।" else "Volume set to maximum level.",
                isBengali = isBn
            )
        }

        // 3. CAMERA / TAKE PHOTO (English & Bangla)
        if (matchesAny(lower, listOf("open camera", "take a picture", "take photo", "capture photo", "ক্যামেরা খোলো", "ছবি তোলো", "ক্যামেরা অন করো"))) {
            try {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return PhoneCommandResult(
                    handled = true,
                    responseText = if (isBn) "ক্যামেরা চালু করা হচ্ছে।" else "Launching camera.",
                    isBengali = isBn
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error launching camera", e)
            }
        }

        // 4. BATTERY STATUS & TELEMETRY (English & Bangla)
        if (matchesAny(lower, listOf("battery", "battery percentage", "battery level", "check battery", "ব্যাটারি কত", "চার্জ কত", "ব্যাটারি পার্সেন্টেজ কত", "ফোন স্ট্যাটাস"))) {
            val batteryLevel = getBatteryLevel(context)
            return PhoneCommandResult(
                handled = true,
                responseText = if (isBn) "আপনার ডিভাইসের ব্যাটারি চার্জ $batteryLevel%।" else "Device battery level is currently at $batteryLevel percent.",
                isBengali = isBn
            )
        }

        // 5. CURRENT TIME & DATE (English & Bangla)
        if (matchesAny(lower, listOf("what time is it", "current time", "what is the time", "tell me the time", "কয়টা বাজে", "সময় কত", "এখন কয়টা বাজে"))) {
            val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
            val currentTime = sdf.format(Date())
            return PhoneCommandResult(
                handled = true,
                responseText = if (isBn) "এখন সময় $currentTime।" else "The current time is $currentTime.",
                isBengali = isBn
            )
        }

        if (matchesAny(lower, listOf("what is today's date", "today's date", "current date", "what date is it", "আজকের তারিখ কি", "আজকে কত তারিখ"))) {
            val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
            val currentDate = sdf.format(Date())
            return PhoneCommandResult(
                handled = true,
                responseText = if (isBn) "আজকের তারিখ হলো $currentDate।" else "Today is $currentDate.",
                isBengali = isBn
            )
        }

        // 6. PHONE CALLS (English: "call 017...", Bangla: "কল করো 018...")
        if (lower.startsWith("call ") || lower.startsWith("dial ") || command.startsWith("কল করো ") || command.startsWith("ডায়াল করো ")) {
            val rawTarget = command
                .replace(Regex("^(call|dial|কল করো|ডায়াল করো)\\s+", RegexOption.IGNORE_CASE), "")
                .trim()
            val sanitized = sanitizePhoneNumber(rawTarget)
            try {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$sanitized")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return PhoneCommandResult(
                    handled = true,
                    responseText = if (isBn) "$rawTarget নম্বরে কল ডায়াল করা হচ্ছে।" else "Dialing $rawTarget.",
                    isBengali = isBn
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error dialing phone", e)
            }
        }

        // 7. SMS / TEXT MESSAGE (English & Bangla)
        if (lower.startsWith("send message to ") || lower.startsWith("text ") || command.startsWith("মেসেজ পাঠাও ") || command.startsWith("এসএমএস করো ")) {
            val smsRegex = Regex("^(?:send message to|text|মেসেজ পাঠাও|এসএমএস করো)\\s+(.*?)(?:\\s+(?:saying|বার্তা|মেসেজ)\\s+(.*))?$", RegexOption.IGNORE_CASE)
            val match = smsRegex.find(command)
            if (match != null) {
                val target = match.groupValues[1].trim()
                val body = match.groupValues.getOrNull(2)?.trim() ?: ""
                try {
                    val sanitizedNumber = sanitizePhoneNumber(target)
                    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$sanitizedNumber")).apply {
                        putExtra("sms_body", body)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    return PhoneCommandResult(
                        handled = true,
                        responseText = if (isBn) "$target কে মেসেজ পাঠানোর উইন্ডো খোলা হচ্ছে।" else "Preparing message to $target.",
                        isBengali = isBn
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Error sending SMS", e)
                }
            }
        }

        // 8. GLOBAL ACCESSIBILITY ACTIONS (Home, Back, Recents, Notifications, Screenshot, Lock)
        if (matchesAny(lower, listOf("go home", "home screen", "open home", "হোম স্ক্রিনে যাও", "হোমে যাও"))) {
            JarvisAccessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            return PhoneCommandResult(
                handled = true,
                responseText = if (isBn) "হোম স্ক্রিনে যাওয়া হচ্ছে।" else "Navigating to home screen.",
                isBengali = isBn
            )
        }

        if (matchesAny(lower, listOf("go back", "back", "ব্যাকে যাও", "পিছনে যাও"))) {
            JarvisAccessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            return PhoneCommandResult(
                handled = true,
                responseText = if (isBn) "ব্যাকে যাওয়া হলো।" else "Navigating back.",
                isBengali = isBn
            )
        }

        if (matchesAny(lower, listOf("recent apps", "open recents", "recent tasks", "রিসেন্ট অ্যাপস", "চলমান অ্যাপস"))) {
            JarvisAccessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
            return PhoneCommandResult(
                handled = true,
                responseText = if (isBn) "রিসেন্ট অ্যাপস দেখানো হচ্ছে।" else "Opening recent applications.",
                isBengali = isBn
            )
        }

        if (matchesAny(lower, listOf("notifications", "show notifications", "open notifications", "নোটিফিকেশন দেখাও", "নোটিফিকেশন বার নামাও"))) {
            JarvisAccessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
            return PhoneCommandResult(
                handled = true,
                responseText = if (isBn) "নোটিফিকেশন প্যানেল খোলা হয়েছে।" else "Displaying notification panel.",
                isBengali = isBn
            )
        }

        if (matchesAny(lower, listOf("quick settings", "open quick settings", "কুইক সেটিংস খোলো"))) {
            JarvisAccessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
            return PhoneCommandResult(
                handled = true,
                responseText = if (isBn) "কুইক সেটিংস খোলা হয়েছে।" else "Opening quick settings.",
                isBengali = isBn
            )
        }

        if (matchesAny(lower, listOf("take screenshot", "capture screen", "screenshot", "স্ক্রিনশট নাও", "স্ক্রিনশট তোলো"))) {
            val taken = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                JarvisAccessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
            } else {
                false
            }
            return PhoneCommandResult(
                handled = true,
                responseText = if (isBn) {
                    if (taken) "স্ক্রিনশট গ্রহণ করা হয়েছে।" else "স্ক্রিনশট নেওয়া সম্ভব হয়নি।"
                } else {
                    if (taken) "Screenshot captured." else "Unable to capture screenshot."
                },
                isBengali = isBn
            )
        }

        if (matchesAny(lower, listOf("lock screen", "lock phone", "স্ক্রিন লক করো", "ফোন লক করো"))) {
            val locked = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                JarvisAccessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
            } else {
                false
            }
            return PhoneCommandResult(
                handled = true,
                responseText = if (isBn) {
                    if (locked) "ফোন লক করা হয়েছে।" else "অ্যাক্সেসিবিলিটি পারমিশন প্রয়োজন।"
                } else {
                    if (locked) "Screen locked." else "Lock screen requires accessibility permissions."
                },
                isBengali = isBn
            )
        }

        // 9. SCROLLING & SCREEN INTERACTION (English & Bangla)
        if (matchesAny(lower, listOf("scroll down", "scroll forward", "নিচে স্ক্রোল করো", "নিচে নামাও"))) {
            JarvisAccessibilityService.scrollScreen(true)
            return PhoneCommandResult(
                handled = true,
                responseText = if (isBn) "নিচে স্ক্রোল করা হচ্ছে।" else "Scrolling down.",
                isBengali = isBn
            )
        }

        if (matchesAny(lower, listOf("scroll up", "scroll backward", "উপরে স্ক্রোল করো", "উপরে ওঠাও"))) {
            JarvisAccessibilityService.scrollScreen(false)
            return PhoneCommandResult(
                handled = true,
                responseText = if (isBn) "উপরে স্ক্রোল করা হচ্ছে।" else "Scrolling up.",
                isBengali = isBn
            )
        }

        if (lower.startsWith("click ") || lower.startsWith("tap ") || command.startsWith("ক্লিক করো ") || command.startsWith("ট্যাপ করো ")) {
            val target = command.replace(Regex("^(click|tap|ক্লিক করো|ট্যাপ করো)\\s+(on\\s+)?", RegexOption.IGNORE_CASE), "").trim()
            val success = JarvisAccessibilityService.clickNode(target)
            return PhoneCommandResult(
                handled = true,
                responseText = if (isBn) {
                    if (success) "\"$target\" তে ক্লিক করা হয়েছে।" else "স্ক্রিনে \"$target\" খুঁজে পাওয়া যায়নি।"
                } else {
                    if (success) "Clicking on $target." else "Could not find \"$target\" on screen."
                },
                isBengali = isBn
            )
        }

        // 10. ALARM & TIMER (English & Bangla)
        if (lower.startsWith("set alarm") || command.startsWith("অ্যালার্ম দাও") || command.startsWith("অ্যালার্ম লাগাও")) {
            try {
                val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                    putExtra(AlarmClock.EXTRA_MESSAGE, "Jarvis Alarm")
                    putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return PhoneCommandResult(
                    handled = true,
                    responseText = if (isBn) "অ্যালার্ম সেট করার ইন্টারফেস খোলা হচ্ছে।" else "Opening alarm setup.",
                    isBengali = isBn
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error setting alarm", e)
            }
        }

        // 11. WEB SEARCH & YOUTUBE SEARCH
        if (lower.startsWith("search the web for ") || lower.startsWith("search for ") || lower.startsWith("google ") || command.startsWith("গুগল করো ") || command.startsWith("সার্চ করো ")) {
            val query = command.replace(Regex("^(search the web for|search for|google|গুগল করো|সার্চ করো)\\s+", RegexOption.IGNORE_CASE), "").trim()
            try {
                val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                    putExtra(SearchManager.QUERY, query)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return PhoneCommandResult(
                    handled = true,
                    responseText = if (isBn) "\"$query\" এর জন্য ওয়েব সার্চ করা হচ্ছে।" else "Searching the web for \"$query\".",
                    isBengali = isBn
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error performing web search", e)
            }
        }

        if (lower.startsWith("play ") || command.startsWith("গান চালাও ") || command.startsWith("ইউটিউবে চালাও ")) {
            val query = command.replace(Regex("^(play|গান চালাও|ইউটিউবে চালাও)\\s+", RegexOption.IGNORE_CASE), "").trim()
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return PhoneCommandResult(
                    handled = true,
                    responseText = if (isBn) "ইউটিউবে \"$query\" চালানো হচ্ছে।" else "Playing \"$query\" on YouTube.",
                    isBengali = isBn
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error launching YouTube search", e)
            }
        }

        // 12. APP LAUNCHER (Fast Package Matching + Common Aliases)
        if (lower.startsWith("open ") || lower.startsWith("launch ") || command.startsWith("খোলো ") || command.startsWith("চালু করো ")) {
            val appQuery = command.replace(Regex("^(open|launch|খোলো|চালু করো)\\s+", RegexOption.IGNORE_CASE), "").trim().lowercase(Locale.ROOT)
            
            val launched = launchAppByQuery(context, appQuery)
            if (launched != null) {
                return PhoneCommandResult(
                    handled = true,
                    responseText = if (isBn) "$launched চালু করা হচ্ছে।" else "Opening $launched.",
                    isBengali = isBn
                )
            }
        }

        // 13. SETTINGS & SYSTEM SHORTCUTS
        if (matchesAny(lower, listOf("wifi settings", "open wifi", "ওয়াইফাই খোলো", "ওয়াইফাই সেটিংস"))) {
            openSystemSettings(context, Settings.ACTION_WIFI_SETTINGS)
            return PhoneCommandResult(handled = true, responseText = if (isBn) "ওয়াইফাই সেটিংস খোলা হচ্ছে।" else "Opening Wi-Fi settings.", isBengali = isBn)
        }

        if (matchesAny(lower, listOf("bluetooth settings", "open bluetooth", "ব্লুটুথ খোলো", "ব্লুটুথ সেটিংস"))) {
            openSystemSettings(context, Settings.ACTION_BLUETOOTH_SETTINGS)
            return PhoneCommandResult(handled = true, responseText = if (isBn) "ব্লুটুথ সেটিংস খোলা হচ্ছে।" else "Opening Bluetooth settings.", isBengali = isBn)
        }

        if (matchesAny(lower, listOf("display settings", "brightness settings", "ডিসপ্লে সেটিংস", "ব্রাইটনেস সেটিংস"))) {
            openSystemSettings(context, Settings.ACTION_DISPLAY_SETTINGS)
            return PhoneCommandResult(handled = true, responseText = if (isBn) "ডিসপ্লে সেটিংস খোলা হচ্ছে।" else "Opening display settings.", isBengali = isBn)
        }

        return PhoneCommandResult(handled = false, responseText = "")
    }

    private fun matchesAny(text: String, patterns: List<String>): Boolean {
        return patterns.any { text.equals(it, ignoreCase = true) || text.contains(it, ignoreCase = true) }
    }

    private fun containsBengali(text: String): Boolean {
        return text.any { it in '\u0980'..'\u09FF' }
    }

    private fun sanitizePhoneNumber(input: String): String {
        // Convert Bengali digits to English digits if present
        val bengaliDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        val englishDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
        var result = input
        for (i in 0..9) {
            result = result.replace(bengaliDigits[i], englishDigits[i])
        }
        return result.replace(Regex("[^0-9+]"), "")
    }

    private fun setTorch(context: Context, enable: Boolean): Boolean {
        return try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            val cameraId = cameraManager?.cameraIdList?.firstOrNull() ?: return false
            cameraManager.setTorchMode(cameraId, enable)
            isTorchOn = enable
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to toggle torch: ${e.localizedMessage}")
            false
        }
    }

    private fun adjustVolume(context: Context, direction: Int) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                direction,
                AudioManager.FLAG_SHOW_UI
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to adjust volume", e)
        }
    }

    private fun setVolumeMute(context: Context, mute: Boolean) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            if (mute) {
                audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mute volume", e)
        }
    }

    private fun setMaxVolume(context: Context) {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVol, AudioManager.FLAG_SHOW_UI)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set max volume", e)
        }
    }

    private fun getBatteryLevel(context: Context): Int {
        return try {
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
        } catch (e: Exception) {
            100
        }
    }

    private fun openSystemSettings(context: Context, action: String) {
        try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open settings action: $action", e)
        }
    }

    private fun launchAppByQuery(context: Context, query: String): String? {
        val pm = context.packageManager

        // Known direct aliases
        val directAliases = mapOf(
            "youtube" to "com.google.android.youtube",
            "ইউটিউব" to "com.google.android.youtube",
            "whatsapp" to "com.whatsapp",
            "হোয়াটসঅ্যাপ" to "com.whatsapp",
            "facebook" to "com.facebook.katana",
            "ফেসবুক" to "com.facebook.katana",
            "instagram" to "com.instagram.android",
            "ইনস্টাগ্রাম" to "com.instagram.android",
            "chrome" to "com.android.chrome",
            "ক্রোম" to "com.android.chrome",
            "browser" to "com.android.chrome",
            "ব্রাউজার" to "com.android.chrome",
            "maps" to "com.google.android.apps.maps",
            "ম্যাপস" to "com.google.android.apps.maps",
            "গুগল ম্যাপ" to "com.google.android.apps.maps",
            "calculator" to "com.google.android.calculator",
            "ক্যালকুলেটর" to "com.google.android.calculator",
            "settings" to "com.android.settings",
            "সেটিংস" to "com.android.settings",
            "spotify" to "com.spotify.music",
            "স্পটিফাই" to "com.spotify.music",
            "telegram" to "org.telegram.messenger",
            "টেলিগ্রাম" to "org.telegram.messenger",
            "gmail" to "com.google.android.gm",
            "জিমেইল" to "com.google.android.gm",
            "camera" to "com.android.camera",
            "ক্যামেরা" to "com.android.camera",
            "gallery" to "com.google.android.apps.photos",
            "photos" to "com.google.android.apps.photos",
            "গ্যালারি" to "com.google.android.apps.photos"
        )

        val targetPackage = directAliases[query]
        if (targetPackage != null) {
            val intent = pm.getLaunchIntentForPackage(targetPackage)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return query.replaceFirstChar { it.uppercase() }
            }
        }

        // Fuzzy match installed packages
        try {
            val packages = pm.getInstalledApplications(android.content.pm.PackageManager.GET_META_DATA)
            val matched = packages.firstOrNull { appInfo ->
                val label = pm.getApplicationLabel(appInfo).toString().lowercase(Locale.ROOT)
                (label.contains(query) || query.contains(label)) && pm.getLaunchIntentForPackage(appInfo.packageName) != null
            }

            if (matched != null) {
                val launchIntent = pm.getLaunchIntentForPackage(matched.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return pm.getApplicationLabel(matched).toString()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning installed packages", e)
        }

        return null
    }
}
