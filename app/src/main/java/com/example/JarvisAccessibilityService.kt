package com.example

import android.accessibilityservice.AccessibilityService
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

class JarvisAccessibilityService : AccessibilityService(), TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    var isScreenReaderEnabled = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        tts = TextToSpeech(this, this)
        Log.d("JarvisAccess", "Service Connected")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.US
        } else {
            Log.e("JarvisAccess", "TTS Initialization failed")
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isScreenReaderEnabled || event == null || tts == null) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_HOVER_ENTER) {
            
            val textToSpeak = mutableListOf<String>()
            
            event.contentDescription?.let { textToSpeak.add(it.toString()) }
            event.text?.let { textList -> 
                textToSpeak.addAll(textList.map { it.toString() })
            }

            if (textToSpeak.isNotEmpty()) {
                val combinedText = textToSpeak.joinToString(". ")
                tts?.speak(combinedText, TextToSpeech.QUEUE_FLUSH, null, "SCREEN_READER")
            }
        }
    }

    override fun onInterrupt() {
        Log.d("JarvisAccess", "Service Interrupted")
        tts?.stop()
    }

    override fun onDestroy() {
        tts?.shutdown()
        super.onDestroy()
    }

    fun clickOnText(text: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val nodes = root.findAccessibilityNodeInfosByText(text)
        for (node in nodes) {
            var clickableNode: AccessibilityNodeInfo? = node
            while (clickableNode != null && !clickableNode.isClickable) {
                clickableNode = clickableNode.parent
            }
            if (clickableNode != null && clickableNode.isClickable) {
                clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                return true
            }
        }
        return false
    }

    fun scroll(direction: Int): Boolean {
        val root = rootInActiveWindow ?: return false
        // Basic heuristic: find first scrollable node
        val scrollableNode = findScrollableNode(root)
        if (scrollableNode != null) {
            val action = if (direction > 0) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
            return scrollableNode.performAction(action)
        }
        return false
    }

    private fun findScrollableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.isScrollable) return root
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val result = findScrollableNode(child)
            if (result != null) return result
        }
        return null
    }

    companion object {
        private var instance: JarvisAccessibilityService? = null

        fun getInstance(): JarvisAccessibilityService? = instance

        fun performGlobalAction(action: Int): Boolean {
            return instance?.performGlobalAction(action) ?: false
        }

        fun openApp(packageName: String): Boolean {
            val context = instance ?: return false
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            return if (launchIntent != null) {
                context.startActivity(launchIntent)
                true
            } else {
                false
            }
        }
        
        fun toggleScreenReader(enabled: Boolean) {
            instance?.isScreenReaderEnabled = enabled
        }
        
        fun clickNode(text: String): Boolean {
            return instance?.clickOnText(text) ?: false
        }
        
        fun scrollScreen(forward: Boolean): Boolean {
            return instance?.scroll(if (forward) 1 else -1) ?: false
        }
    }

    init {
        instance = this
    }
}
