package com.example.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Result data class returned after evaluating and executing a voice command.
 */
data class CommandExecutionResult(
    val handled: Boolean,
    val intentType: CommandIntentType,
    val responseText: String,
    val executedIntent: Intent? = null,
    val isBengali: Boolean = false
)

enum class CommandIntentType {
    TOGGLE_WIFI,
    ADJUST_BRIGHTNESS,
    LAUNCH_APP,
    TOGGLE_BLUETOOTH,
    VOLUME_CONTROL,
    FLASHLIGHT,
    CAMERA,
    PHONE_CALL,
    SEND_SMS,
    SYSTEM_NAV,
    WEB_SEARCH,
    ALARM_TIMER,
    UNKNOWN
}

/**
 * Parsed intent parameter metadata extracted by the NLP parser.
 */
data class ParsedCommandIntent(
    val intentType: CommandIntentType,
    val actionTarget: String = "",
    val numericValue: Int? = null,
    val booleanState: Boolean? = null,
    val isBengali: Boolean = false
)

/**
 * Natural Language Processing (NLP) Command Executor Service.
 * Parses spoken voice queries into specific Android Intent actions,
 * system setting modifications, and package activity launches.
 */
object CommandExecutor {
    private const val TAG = "CommandExecutor"
    // Security: Limit input query length to mitigate DoS / resource exhaustion attacks via excessively long strings.
    private const val MAX_QUERY_LENGTH = 1000

    /**
     * Primary entry point to parse a raw voice query using NLP rules
     * and execute the corresponding Android Intent action.
     */
    fun parseAndExecute(context: Context, rawQuery: String): CommandExecutionResult {
        val query = rawQuery.trim().take(MAX_QUERY_LENGTH)
        if (query.isEmpty()) {
            return CommandExecutionResult(false, CommandIntentType.UNKNOWN, "")
        }

        val parsed = parseQuery(query)
        return executeParsedIntent(context, parsed, query)
    }

    /**
     * Uses NLP pattern recognition, regex extraction, and token analysis
     * to parse voice queries into structured Intent types and parameters.
     */
    fun parseQuery(rawQuery: String): ParsedCommandIntent {
        val query = rawQuery.trim().take(MAX_QUERY_LENGTH)
        val lower = query.lowercase(Locale.ROOT)
        val isBn = containsBengali(query)

        // 1. WI-FI TOGGLE INTENT DETECTION
        if (containsAny(lower, listOf("wifi", "wi-fi", "ওয়াইফাই", "ওয়াইফাই", "ওয়াই ফাই"))) {
            val enable = when {
                containsAny(lower, listOf("on", "enable", "start", "turn on", "অন", "চালু", "চালাও", "অন করো")) -> true
                containsAny(lower, listOf("off", "disable", "stop", "turn off", "বন্ধ", "অফ", "বন্ধ করো", "অফ করো")) -> false
                else -> null
            }
            return ParsedCommandIntent(
                intentType = CommandIntentType.TOGGLE_WIFI,
                booleanState = enable,
                isBengali = isBn
            )
        }

        // 2. SCREEN BRIGHTNESS INTENT DETECTION
        if (containsAny(lower, listOf("brightness", "screen brightness", "display brightness", "ব্রাইটনেস", "আলো", "ডিসপ্লে আলো"))) {
            val level = extractPercentage(query)
            val isIncrease = containsAny(lower, listOf("increase", "up", "higher", "raise", "বাড়াও", "বাড়াও", "বেশি"))
            val isDecrease = containsAny(lower, listOf("decrease", "down", "lower", "dim", "কমাও", "কম"))
            val isMax = containsAny(lower, listOf("max", "maximum", "full", "১০০%", "100%", "সর্বোচ্চ"))
            val isMin = containsAny(lower, listOf("min", "minimum", "lowest", "সর্বনিম্ন"))

            val numericTarget = when {
                level != null -> level
                isMax -> 100
                isMin -> 5
                else -> null
            }

            return ParsedCommandIntent(
                intentType = CommandIntentType.ADJUST_BRIGHTNESS,
                numericValue = numericTarget,
                booleanState = if (isIncrease) true else if (isDecrease) false else null,
                isBengali = isBn
            )
        }

        // 3. APP LAUNCH INTENT DETECTION
        val appLaunchTriggers = listOf(
            "open app", "open", "launch", "start", "run",
            "খোলো", "চালু করো", "ওপেন করো", "চালাও", "খুলুন"
        )
        for (trigger in appLaunchTriggers) {
            val prefix = "$trigger "
            val suffix = " $trigger"
            when {
                lower.startsWith(prefix) || query.startsWith(prefix) -> {
                    val appQuery = query.substring(prefix.length).trim()
                    if (appQuery.isNotEmpty()) {
                        return ParsedCommandIntent(
                            intentType = CommandIntentType.LAUNCH_APP,
                            actionTarget = appQuery,
                            isBengali = isBn
                        )
                    }
                }
                lower.endsWith(suffix) || query.endsWith(suffix) -> {
                    val appQuery = query.substring(0, query.length - suffix.length).trim()
                    if (appQuery.isNotEmpty()) {
                        return ParsedCommandIntent(
                            intentType = CommandIntentType.LAUNCH_APP,
                            actionTarget = appQuery,
                            isBengali = isBn
                        )
                    }
                }
            }
        }

        // 4. BLUETOOTH TOGGLE INTENT DETECTION
        if (containsAny(lower, listOf("bluetooth", "ব্লুটুথ"))) {
            val enable = when {
                containsAny(lower, listOf("on", "enable", "turn on", "অন", "চালু")) -> true
                containsAny(lower, listOf("off", "disable", "turn off", "বন্ধ", "অফ")) -> false
                else -> null
            }
            return ParsedCommandIntent(
                intentType = CommandIntentType.TOGGLE_BLUETOOTH,
                booleanState = enable,
                isBengali = isBn
            )
        }

        return ParsedCommandIntent(intentType = CommandIntentType.UNKNOWN, isBengali = isBn)
    }

    /**
     * Executes the specific parsed Intent action on the Android system.
     */
    private fun executeParsedIntent(
        context: Context,
        parsed: ParsedCommandIntent,
        rawQuery: String
    ): CommandExecutionResult {
        return when (parsed.intentType) {
            CommandIntentType.TOGGLE_WIFI -> executeWifiAction(context, parsed.booleanState, parsed.isBengali)
            CommandIntentType.ADJUST_BRIGHTNESS -> executeBrightnessAction(
                context = context,
                targetPercentage = parsed.numericValue,
                isIncrease = parsed.booleanState,
                isBengali = parsed.isBengali
            )
            CommandIntentType.LAUNCH_APP -> executeLaunchAppAction(context, parsed.actionTarget, parsed.isBengali)
            CommandIntentType.TOGGLE_BLUETOOTH -> executeBluetoothAction(context, parsed.isBengali)
            else -> CommandExecutionResult(false, CommandIntentType.UNKNOWN, "", isBengali = parsed.isBengali)
        }
    }

    /**
     * Toggles or opens Wi-Fi system settings panels depending on Android SDK capabilities.
     */
    fun executeWifiAction(context: Context, targetState: Boolean?, isBengali: Boolean): CommandExecutionResult {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // On Android 10+ (API 29+), programmatically setting wifi is restricted.
                // Open the Wi-Fi Settings Panel intent for fast 1-tap user action.
                val panelIntent = Intent(Settings.Panel.ACTION_WIFI).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(panelIntent)
                CommandExecutionResult(
                    handled = true,
                    intentType = CommandIntentType.TOGGLE_WIFI,
                    responseText = if (isBengali) "ওয়াইফাই কুইক প্যানেল খোলা হয়েছে।" else "Opening Wi-Fi control panel.",
                    executedIntent = panelIntent,
                    isBengali = isBengali
                )
            } else {
                @Suppress("DEPRECATION")
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                if (wifiManager != null) {
                    val newState = targetState ?: !wifiManager.isWifiEnabled
                    @Suppress("DEPRECATION")
                    wifiManager.isWifiEnabled = newState
                    val msg = if (isBengali) {
                        if (newState) "ওয়াইফাই চালু করা হয়েছে।" else "ওয়াইফাই বন্ধ করা হয়েছে।"
                    } else {
                        if (newState) "Wi-Fi turned on." else "Wi-Fi turned off."
                    }
                    CommandExecutionResult(
                        handled = true,
                        intentType = CommandIntentType.TOGGLE_WIFI,
                        responseText = msg,
                        isBengali = isBengali
                    )
                } else {
                    openSystemSettings(context, Settings.ACTION_WIFI_SETTINGS, isBengali, "Wi-Fi settings")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing Wi-Fi intent action", e)
            openSystemSettings(context, Settings.ACTION_WIFI_SETTINGS, isBengali, "Wi-Fi settings")
        }
    }

    /**
     * Adjusts system screen brightness or requests WRITE_SETTINGS permission if ungranted.
     */
    fun executeBrightnessAction(
        context: Context,
        targetPercentage: Int?,
        isIncrease: Boolean?,
        isBengali: Boolean
    ): CommandExecutionResult {
        // Check WRITE_SETTINGS system permission
        if (!Settings.System.canWrite(context)) {
            return try {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                CommandExecutionResult(
                    handled = true,
                    intentType = CommandIntentType.ADJUST_BRIGHTNESS,
                    responseText = if (isBengali) {
                        "ব্রাইটনেস পরিবর্তনের জন্য সিস্টেম পারমিশন দিন।"
                    } else {
                        "Please grant permission to modify system brightness settings."
                    },
                    executedIntent = intent,
                    isBengali = isBengali
                )
            } catch (e: Exception) {
                openSystemSettings(context, Settings.ACTION_DISPLAY_SETTINGS, isBengali, "Display settings")
            }
        }

        return try {
            val contentResolver = context.contentResolver
            val currentBrightness = try {
                Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS)
            } catch (e: Exception) {
                128
            }

            val currentPercentage = ((currentBrightness / 255f) * 100).roundToInt()
            val newPercentage = when {
                targetPercentage != null -> targetPercentage.coerceIn(1, 100)
                isIncrease == true -> (currentPercentage + 20).coerceAtMost(100)
                isIncrease == false -> (currentPercentage - 20).coerceAtLeast(5)
                else -> 80
            }

            val newBrightnessValue = ((newPercentage / 100f) * 255).roundToInt().coerceIn(1, 255)
            Settings.System.putInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, newBrightnessValue)

            val msg = if (isBengali) {
                "স্ক্রিন ব্রাইটনেস $newPercentage% এ সেট করা হলো।"
            } else {
                "Screen brightness set to $newPercentage%."
            }

            CommandExecutionResult(
                handled = true,
                intentType = CommandIntentType.ADJUST_BRIGHTNESS,
                responseText = msg,
                isBengali = isBengali
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error adjusting screen brightness", e)
            openSystemSettings(context, Settings.ACTION_DISPLAY_SETTINGS, isBengali, "Display settings")
        }
    }

    /**
     * Scans all installed packages using token NLP & fuzzy matching to launch the target app.
     */
    fun executeLaunchAppAction(context: Context, appQuery: String, isBengali: Boolean): CommandExecutionResult {
        val pm = context.packageManager
        val cleanQuery = appQuery.lowercase(Locale.ROOT).trim()

        // Known direct package alias map for popular apps (English & Bengali)
        val aliasMap = mapOf(
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
            "গ্যালারি" to "com.google.android.apps.photos",
            "play store" to "com.android.vending",
            "প্লে স্টোর" to "com.android.vending"
        )

        val directPackage = aliasMap[cleanQuery]
        if (directPackage != null) {
            val intent = pm.getLaunchIntentForPackage(directPackage)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return CommandExecutionResult(
                    handled = true,
                    intentType = CommandIntentType.LAUNCH_APP,
                    responseText = if (isBengali) "$appQuery চালু করা হচ্ছে।" else "Opening $appQuery.",
                    executedIntent = intent,
                    isBengali = isBengali
                )
            }
        }

        // Fuzzy match across installed applications
        try {
            val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            var bestMatchPackage: String? = null
            var bestMatchLabel: String = cleanQuery
            var highestScore = 0f

            for (appInfo in installedApps) {
                val launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName) ?: continue
                val label = pm.getApplicationLabel(appInfo).toString().lowercase(Locale.ROOT)

                val score = calculateSimilarity(cleanQuery, label, appInfo.packageName)
                if (score > highestScore && score >= 0.4f) {
                    highestScore = score
                    bestMatchPackage = appInfo.packageName
                    bestMatchLabel = pm.getApplicationLabel(appInfo).toString()
                }
            }

            if (bestMatchPackage != null) {
                val launchIntent = pm.getLaunchIntentForPackage(bestMatchPackage)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return CommandExecutionResult(
                        handled = true,
                        intentType = CommandIntentType.LAUNCH_APP,
                        responseText = if (isBengali) "$bestMatchLabel চালু করা হচ্ছে।" else "Opening $bestMatchLabel.",
                        executedIntent = launchIntent,
                        isBengali = isBengali
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning installed packages for NLP launch", e)
        }

        return CommandExecutionResult(
            handled = false,
            intentType = CommandIntentType.LAUNCH_APP,
            responseText = if (isBengali) "\"$appQuery\" অ্যাপটি খুঁজে পাওয়া যায়নি।" else "Could not find app \"$appQuery\" on device.",
            isBengali = isBengali
        )
    }

    private fun executeBluetoothAction(context: Context, isBengali: Boolean): CommandExecutionResult {
        return openSystemSettings(context, Settings.ACTION_BLUETOOTH_SETTINGS, isBengali, "Bluetooth settings")
    }

    private fun openSystemSettings(
        context: Context,
        settingsAction: String,
        isBengali: Boolean,
        settingName: String
    ): CommandExecutionResult {
        return try {
            val intent = Intent(settingsAction).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            CommandExecutionResult(
                handled = true,
                intentType = CommandIntentType.TOGGLE_WIFI,
                responseText = if (isBengali) "$settingName খোলা হলো।" else "Opening $settingName.",
                executedIntent = intent,
                isBengali = isBengali
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open settings action $settingsAction", e)
            CommandExecutionResult(false, CommandIntentType.UNKNOWN, "", isBengali = isBengali)
        }
    }

    // Helper functions for NLP query parsing

    private fun containsAny(text: String, keywords: List<String>): Boolean {
        return keywords.any { text.contains(it) }
    }

    private fun containsBengali(text: String): Boolean {
        return text.any { it in '\u0980'..'\u09FF' }
    }

    private fun extractPercentage(query: String): Int? {
        val sanitized = convertBengaliDigitsToEnglish(query)
        // Match numbers like 80%, 80 percent, set brightness to 50
        val regex = Regex("(\\d{1,3})\\s*(?:%|percent|পার্সেন্ট)?")
        val match = regex.find(sanitized)
        if (match != null) {
            val value = match.groupValues[1].toIntOrNull()
            if (value != null && value in 1..100) {
                return value
            }
        }
        return null
    }

    private fun convertBengaliDigitsToEnglish(input: String): String {
        val bnDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
        val enDigits = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9')
        var result = input
        for (i in 0..9) {
            result = result.replace(bnDigits[i], enDigits[i])
        }
        return result
    }

    /**
     * Calculates fuzzy string similarity between search query and app labels/package names.
     */
    private fun calculateSimilarity(query: String, label: String, packageName: String): Float {
        if (label == query || packageName == query) return 1.0f
        if (label.startsWith(query) || query.startsWith(label)) return 0.85f
        if (label.contains(query) || query.contains(label)) return 0.7f

        val dist = levenshteinDistance(query, label)
        val maxLen = maxOf(query.length, label.length)
        if (maxLen == 0) return 0f
        return 1.0f - (dist.toFloat() / maxLen)
    }

    private fun levenshteinDistance(lhs: CharSequence, rhs: CharSequence): Int {
        val lhsLength = lhs.length
        val rhsLength = rhs.length

        var cost = IntArray(lhsLength + 1) { it }
        var newCost = IntArray(lhsLength + 1)

        for (i in 1..rhsLength) {
            newCost[0] = i
            for (j in 1..lhsLength) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1
                newCost[j] = minOf(minOf(costInsert, costDelete), costReplace)
            }
            val swap = cost
            cost = newCost
            newCost = swap
        }
        return cost[lhsLength]
    }
}
