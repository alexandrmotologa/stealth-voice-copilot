package com.copilot.stealth.engine.audio

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.os.Build
import com.copilot.stealth.data.models.AudioInputSource

/**
 * Audio capture factory and helper for capturing system audio (AudioPlaybackCapture)
 * and two-way phone/VoIP call audio (VOICE_COMMUNICATION).
 */
object SystemAudioCaptureHelper {

    /**
     * Creates an AudioRecord configured for the specified AudioInputSource.
     * When [AudioInputSource.SYSTEM_CALL] is chosen, it prioritizes AudioPlaybackCapture if a
     * MediaProjection is provided on API 29+, or VOICE_COMMUNICATION for call audio.
     */
    @SuppressLint("MissingPermission")
    fun createAudioRecord(
        inputSource: AudioInputSource,
        sampleRate: Int = 16000,
        bufferSize: Int,
        mediaProjection: MediaProjection? = null
    ): AudioRecord {
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
            .build()

        if (inputSource == AudioInputSource.SYSTEM_CALL) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mediaProjection != null) {
                val playbackConfig = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                    .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                    .addMatchingUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                    .build()

                return AudioRecord.Builder()
                    .setAudioFormat(format)
                    .setBufferSizeInBytes(bufferSize)
                    .setAudioPlaybackCaptureConfig(playbackConfig)
                    .build()
            } else {
                // Fallback to VOICE_COMMUNICATION for VOIP / phone call capturing
                return AudioRecord(
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
                )
            }
        }

        // Standard microphone voice recognition source
        return AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
    }
}
