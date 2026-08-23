package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Broadcast Receiver that listens for system boot and package replacement events
 * to automatically start J.A.R.V.I.S. persistent background services (e.g. WakeWordService).
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Received boot broadcast event: $action")

        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON"
        ) {
            val prefs = context.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
            val wakeWordEnabled = prefs.getBoolean("wake_word_enabled", true)
            val persistentBackgroundEnabled = prefs.getBoolean("persistent_background_enabled", true)

            if (wakeWordEnabled || persistentBackgroundEnabled) {
                Log.d(TAG, "Starting WakeWordService automatically on device boot...")
                val serviceIntent = Intent(context, WakeWordService::class.java)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start background service on boot", e)
                }
            }
        }
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
