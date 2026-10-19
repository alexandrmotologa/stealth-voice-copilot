package com.copilot.stealth.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.copilot.stealth.R
import com.copilot.stealth.engine.audio.AudioRecorder
import com.copilot.stealth.engine.audio.DiscreetHapticHelper
import com.copilot.stealth.engine.audio.SileroVadDetector
import com.copilot.stealth.ui.StealthActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Persistent foreground service that manages continuous low-power microphone streaming,
 * Silero VAD evaluation, and wake lock acquisition during live interview sessions.
 */
class CopilotForegroundService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private var wakeLock: PowerManager.WakeLock? = null
    private lateinit var audioRecorder: AudioRecorder
    private lateinit var vadDetector: SileroVadDetector
    private lateinit var hapticHelper: DiscreetHapticHelper

    private val _speechSegmentsFlow = MutableSharedFlow<FloatArray>(extraBufferCapacity = 8)
    val speechSegmentsFlow: SharedFlow<FloatArray> = _speechSegmentsFlow.asSharedFlow()

    private val _vadEventsFlow = MutableSharedFlow<Pair<Float, Float>>(extraBufferCapacity = 16)
    val vadEventsFlow: SharedFlow<Pair<Float, Float>> = _vadEventsFlow.asSharedFlow()

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): CopilotForegroundService = this@CopilotForegroundService
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        hapticHelper = DiscreetHapticHelper(this)
        audioRecorder = AudioRecorder(sampleRate = 16000, frameSize = 512)
        vadDetector = SileroVadDetector(this, sampleRate = 16000)

        vadDetector.onSpeechStarted = {
            hapticHelper.pulseSpeechDetected()
        }

        vadDetector.onSpeechFinished = { audioBuffer ->
            serviceScope.launch {
                _speechSegmentsFlow.emit(audioBuffer)
            }
        }

        vadDetector.onVADProbabilityChanged = { prob, rms ->
            serviceScope.launch {
                _vadEventsFlow.emit(Pair(prob, rms))
            }
        }

        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = buildNotification()
        startForeground(NOTIFICATION_ID, notification)

        when (intent?.action) {
            ACTION_START_LISTENING -> startListening()
            ACTION_STOP_LISTENING -> stopListening()
        }

        return START_STICKY
    }

    fun startListening() {
        if (!audioRecorder.isRecording) {
            vadDetector.reset()
            audioRecorder.start(serviceScope)

            serviceScope.launch(Dispatchers.Default) {
                audioRecorder.audioFlow.collect { pcmFrame ->
                    vadDetector.processFrame(pcmFrame)
                }
            }
        }
    }

    fun stopListening() {
        if (audioRecorder.isRecording) {
            audioRecorder.stop()
            vadDetector.reset()
        }
    }

    val isListening: Boolean
        get() = audioRecorder.isRecording

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        stopListening()
        vadDetector.close()
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
        wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "StealthVoiceCopilot::AudioCaptureLock"
        )?.apply {
            setReferenceCounted(false)
            acquire(10 * 60 * 1000L) // 10 minutes timeout
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {}
        wakeLock = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_description)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_SECRET
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val launchIntent = Intent(this, StealthActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(R.drawable.ic_tile_copilot)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_SECRET)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "stealth_copilot_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_LISTENING = "com.copilot.stealth.action.START_LISTENING"
        const val ACTION_STOP_LISTENING = "com.copilot.stealth.action.STOP_LISTENING"
    }
}
