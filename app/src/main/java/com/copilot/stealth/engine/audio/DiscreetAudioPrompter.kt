package com.copilot.stealth.engine.audio

import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/**
 * Covert earpiece audio prompter that whispers concise key answers and numbers
 * directly into connected Bluetooth earbuds (Pixel Buds) without leaking sound to phone speakers.
 */
class DiscreetAudioPrompter(
    private val context: Context,
    private val bluetoothMonitor: BluetoothAudioMonitor
) : TextToSpeech.OnInitListener, AutoCloseable {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var isWhisperEnabled = false

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.let { engine ->
                val result = engine.setLanguage(Locale.US)
                if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                    isInitialized = true
                }
                // Calibrate audio attributes for voice call / communication stream
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                engine.setAudioAttributes(audioAttributes)
                engine.setSpeechRate(0.95f)
                engine.setPitch(0.90f) // Slightly lower tone for covert whisper clarity
            }
        }
    }

    fun setWhisperEnabled(enabled: Boolean) {
        isWhisperEnabled = enabled
        if (!enabled) {
            stop()
        }
    }

    /**
     * Whispers the key insight or short extract of an answer into the candidate's earbud.
     * Prevents speech if no Bluetooth earpiece is connected to avoid ambient room leakage.
     */
    fun whisperAnswerInsight(answer: String, isRomanian: Boolean = false) {
        if (!isWhisperEnabled || !isInitialized) return

        // Safety check: ensure Bluetooth earpiece is actively connected so sound does not play on phone loudspeaker
        if (!bluetoothMonitor.isBluetoothConnected.value) {
            return
        }

        val excerpt = extractWhisperSnippet(answer)
        if (excerpt.isBlank()) return

        val engine = tts ?: return

        if (isRomanian) {
            engine.language = Locale("ro", "RO")
        } else {
            engine.language = Locale.US
        }

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 0.40f) // Discreet, intimate whisper volume
        }

        engine.speak(excerpt, TextToSpeech.QUEUE_FLUSH, params, "whisper_${System.currentTimeMillis()}")
    }

    private fun extractWhisperSnippet(fullText: String): String {
        // Extract up to 2 concise sentences or first 16 words for instantaneous audio comprehension
        val sentences = fullText.split(Regex("(?<=[.!?])\\s+"))
        val firstSentence = sentences.firstOrNull() ?: fullText
        val words = firstSentence.split(" ")
        return if (words.size > 14) {
            words.take(14).joinToString(" ")
        } else {
            firstSentence
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (_: Exception) {}
    }

    override fun close() {
        stop()
        try {
            tts?.shutdown()
        } catch (_: Exception) {}
        tts = null
        isInitialized = false
    }
}
