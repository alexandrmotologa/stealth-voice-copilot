package com.copilot.stealth.engine.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.NoiseSuppressor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * High-performance audio capture engine using Android AudioRecord with hardware noise reduction.
 */
class AudioRecorder(
    private val sampleRate: Int = 16000,
    private val frameSize: Int = 512
) {

    private var audioRecord: AudioRecord? = null
    private var noiseSuppressor: NoiseSuppressor? = null
    private var echoCanceler: AcousticEchoCanceler? = null

    private var recordingJob: Job? = null
    private val _audioFlow = MutableSharedFlow<ShortArray>(extraBufferCapacity = 64)
    val audioFlow: SharedFlow<ShortArray> = _audioFlow.asSharedFlow()

    @SuppressLint("MissingPermission")
    fun start(scope: CoroutineScope) {
        if (recordingJob != null) return

        val minBufSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = minBufSize.coerceAtLeast(frameSize * 4)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            val audioSessionId = audioRecord?.audioSessionId ?: 0
            if (audioSessionId != 0) {
                if (NoiseSuppressor.isAvailable()) {
                    try {
                        noiseSuppressor = NoiseSuppressor.create(audioSessionId).apply {
                            enabled = true
                        }
                    } catch (_: Exception) {}
                }

                if (AcousticEchoCanceler.isAvailable()) {
                    try {
                        echoCanceler = AcousticEchoCanceler.create(audioSessionId).apply {
                            enabled = true
                        }
                    } catch (_: Exception) {}
                }
            }

            audioRecord?.startRecording()

            recordingJob = scope.launch(Dispatchers.IO) {
                val buffer = ShortArray(frameSize)
                while (isActive) {
                    val readSamples = audioRecord?.read(buffer, 0, frameSize) ?: -1
                    if (readSamples > 0) {
                        val chunk = if (readSamples == frameSize) {
                            buffer.clone()
                        } else {
                            buffer.copyOf(readSamples)
                        }
                        _audioFlow.emit(chunk)
                    }
                }
            }
        } catch (e: Exception) {
            stop()
            throw e
        }
    }

    fun stop() {
        recordingJob?.cancel()
        recordingJob = null

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null

        try {
            noiseSuppressor?.release()
            echoCanceler?.release()
        } catch (_: Exception) {}
        noiseSuppressor = null
        echoCanceler = null
    }

    val isRecording: Boolean
        get() = recordingJob != null
}
