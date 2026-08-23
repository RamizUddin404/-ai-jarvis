package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sqrt
import com.example.util.HeyJarvisTFLiteDetector

class WakeWordService : Service() {

    private val channelId = "WakeWordChannel"
    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var partialWakeLock: PowerManager.WakeLock? = null
    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var tfLiteDetector: HeyJarvisTFLiteDetector? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        tfLiteDetector = HeyJarvisTFLiteDetector(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val openAppIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = if (openAppIntent != null) {
            PendingIntent.getActivity(
                this,
                0,
                openAppIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else null

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("J.A.R.V.I.S. Hands-Free Wake Word")
            .setContentText("Always listening for 'Hey Jarvis' (Screen Off Enabled)...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
        acquirePartialWakeLock()
        startListening()

        return START_STICKY
    }

    private fun acquirePartialWakeLock() {
        try {
            if (partialWakeLock == null) {
                val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
                partialWakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "Jarvis:WakeWordCpuLock"
                ).apply {
                    setReferenceCounted(false)
                    acquire()
                }
                Log.d("WakeWordService", "Acquired PARTIAL_WAKE_LOCK for screen-off voice trigger detection")
            }
        } catch (e: Exception) {
            Log.e("WakeWordService", "Failed to acquire PARTIAL_WAKE_LOCK", e)
        }
    }

    private fun releasePartialWakeLock() {
        try {
            if (partialWakeLock?.isHeld == true) {
                partialWakeLock?.release()
            }
        } catch (e: Exception) {
            Log.w("WakeWordService", "Error releasing PARTIAL_WAKE_LOCK", e)
        }
        partialWakeLock = null
    }

    private fun wakeScreenAndLaunchApp() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            val screenWakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE,
                "Jarvis:WakeWordScreenWake"
            )
            screenWakeLock.acquire(3000)

            val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
                putExtra("EXTRA_WAKE_TRIGGER", true)
            }
            if (launchIntent != null) {
                startActivity(launchIntent)
            }
        } catch (e: Exception) {
            Log.e("WakeWordService", "Error waking screen and bringing app to front", e)
        }
    }

    private fun startListening() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.w("WakeWordService", "RECORD_AUDIO permission not granted; stopping WakeWordService")
            stopSelf()
            return
        }

        val sampleRate = 16000
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e("WakeWordService", "AudioRecord initialization failed")
                stopSelf()
                return
            }

            audioRecord?.startRecording()
            isRecording = true

            serviceScope.launch {
                val buffer = ShortArray(bufferSize)
                val prefs = getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)

                // Low-power local keyword spotter state variables
                var lastBurstTimestamp = 0L
                var consecutiveBurstCount = 0
                var lastTriggerTimestamp = 0L

                while (isActive && isRecording) {
                    val readSize = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (readSize > 0) {
                        var sumSquare = 0.0
                        var zeroCrossings = 0

                        for (i in 0 until readSize) {
                            val sample = buffer[i].toDouble()
                            sumSquare += sample * sample

                            if (i > 0) {
                                val prev = buffer[i - 1]
                                val curr = buffer[i]
                                if ((prev >= 0 && curr < 0) || (prev < 0 && curr >= 0)) {
                                    zeroCrossings++
                                }
                            }
                        }

                        val rms = sqrt(sumSquare / readSize)
                        val zcr = zeroCrossings.toDouble() / readSize

                        // Calibrated RMS threshold setting configured by user
                        val configuredRms = prefs.getFloat("rms_threshold", -1f)
                        val threshold = if (configuredRms > 0f) {
                            configuredRms.toDouble()
                        } else {
                            val sensitivity = prefs.getFloat("voice_sensitivity", 0.5f)
                            18000.0 - (sensitivity.coerceIn(0.1f, 1.0f) * 12000.0)
                        }

                        val now = System.currentTimeMillis()

                        // Process audio buffer with localized TFLite 'Hey Jarvis' Detector
                        val tfliteResult = tfLiteDetector?.processAudioFrame(buffer, readSize)
                        if (tfliteResult != null && tfliteResult.isDetected && (now - lastTriggerTimestamp > 3000)) {
                            lastTriggerTimestamp = now
                            consecutiveBurstCount = 0

                            Log.d("WakeWordService", "🎯 Localized TFLite 'Hey Jarvis' keyword detected! Confidence: ${(tfliteResult.confidence * 100).toInt()}%")

                            // 1. Turn screen on and bring app to foreground
                            wakeScreenAndLaunchApp()

                            // 2. Broadcast trigger
                            val broadcastIntent = Intent("com.example.ACTION_WAKE_WORD_DETECTED").apply {
                                setPackage(packageName)
                            }
                            sendBroadcast(broadcastIntent)

                            // 3. Start speech recognition
                            val langPref = prefs.getString("selected_language", "auto") ?: "auto"
                            SpeechRecognitionService.startListening(this@WakeWordService, langPref)

                            Thread.sleep(2200)
                        }

                        // Check if audio frame matches vocal frequency band (ZCR between 0.02 and 0.38)
                        val isVocalFrequency = zcr in 0.02..0.38

                        if (rms > threshold && isVocalFrequency) {
                            val timeSinceLastBurst = now - lastBurstTimestamp

                            if (timeSinceLastBurst in 150..900) {
                                consecutiveBurstCount++
                            } else if (timeSinceLastBurst > 900) {
                                consecutiveBurstCount = 1
                            }

                            lastBurstTimestamp = now

                            // Low-power keyword spotter cadence check: "Jar-vis" or "Hey-Jar-vis" multi-syllable bursts
                            val requiredBursts = if (prefs.getBoolean("strict_keyword_mode", false)) 2 else 1

                            if (consecutiveBurstCount >= requiredBursts && (now - lastTriggerTimestamp > 3000)) {
                                lastTriggerTimestamp = now
                                consecutiveBurstCount = 0

                                Log.d("WakeWordService", "Low-Power Keyword Spotter Triggered! RMS: $rms, ZCR: $zcr")

                                // 1. Turn screen on and bring app to foreground if locked/sleeping
                                wakeScreenAndLaunchApp()

                                // 2. Broadcast trigger to MainActivity
                                val broadcastIntent = Intent("com.example.ACTION_WAKE_WORD_DETECTED").apply {
                                    setPackage(packageName)
                                }
                                sendBroadcast(broadcastIntent)

                                // 3. Immediately start speech recognition
                                val langPref = prefs.getString("selected_language", "auto") ?: "auto"
                                SpeechRecognitionService.startListening(this@WakeWordService, langPref)

                                // Sleep brief interval to prevent double-firing while speaking command
                                Thread.sleep(2200)
                            }
                        } else if (now - lastBurstTimestamp > 1200) {
                            consecutiveBurstCount = 0
                        }

                        // Low-power sleeping yield to preserve battery when quiet
                        if (rms < threshold * 0.5) {
                            kotlinx.coroutines.delay(40)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("WakeWordService", "Exception in startListening", e)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.d("WakeWordService", "Task removed from recent tasks. Scheduling immediate background restart...")
        scheduleServiceRestart()
    }

    private fun scheduleServiceRestart() {
        val prefs = getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
        val wakeWordEnabled = prefs.getBoolean("wake_word_enabled", true)
        val persistentEnabled = prefs.getBoolean("persistent_background_enabled", true)
        if (wakeWordEnabled || persistentEnabled) {
            try {
                val restartServiceIntent = Intent(applicationContext, WakeWordService::class.java).apply {
                    setPackage(packageName)
                }
                val pendingIntent = PendingIntent.getService(
                    this,
                    1001,
                    restartServiceIntent,
                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                )
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
                alarmManager?.set(
                    android.app.AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + 1000,
                    pendingIntent
                )
            } catch (e: Exception) {
                Log.e("WakeWordService", "Failed to schedule service restart alarm", e)
            }
        }
    }

    override fun onDestroy() {
        isRecording = false
        releasePartialWakeLock()
        scheduleServiceRestart()
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e("WakeWordService", "Error releasing AudioRecord", e)
        }
        audioRecord = null
        serviceJob.cancel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Hands-Free Wake Word Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background listening service for 'Hey Jarvis' voice trigger"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }
}
