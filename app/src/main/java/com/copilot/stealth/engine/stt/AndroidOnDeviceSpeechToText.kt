package com.copilot.stealth.engine.stt

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Native on-device speech recognizer leveraging Google Pixel 6 Pro Tensor speech models
 * via Android SpeechRecognizer with EXTRA_PREFER_OFFLINE = true.
 */
class AndroidOnDeviceSpeechToText(private val context: Context) : RecognitionListener {

    private val _transcriptFlow = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val transcriptFlow: SharedFlow<String> = _transcriptFlow.asSharedFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isListening = false

    var onSpeechStatusChanged: ((isRecognizing: Boolean) -> Unit)? = null

    fun startListening(languageMode: com.copilot.stealth.data.models.SttLanguageMode = com.copilot.stealth.data.models.SttLanguageMode.ENGLISH) {
        mainHandler.post {
            try {
                if (speechRecognizer == null) {
                    speechRecognizer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
                    ) {
                        SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                    } else {
                        SpeechRecognizer.createSpeechRecognizer(context)
                    }
                    speechRecognizer?.setRecognitionListener(this)
                }

                val primaryLang = when (languageMode) {
                    com.copilot.stealth.data.models.SttLanguageMode.ENGLISH -> "en-US"
                    com.copilot.stealth.data.models.SttLanguageMode.ROMANIAN -> "ro-RO"
                    com.copilot.stealth.data.models.SttLanguageMode.BILINGUAL_AUTO -> "en-US"
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, primaryLang)
                    if (languageMode == com.copilot.stealth.data.models.SttLanguageMode.BILINGUAL_AUTO && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        putExtra("android.speech.extra.ADDITIONAL_LANGUAGES", arrayOf("ro-RO", "en-US"))
                    }
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                    }
                }

                isListening = true
                speechRecognizer?.startListening(intent)
                onSpeechStatusChanged?.invoke(true)
            } catch (e: Exception) {
                isListening = false
                onSpeechStatusChanged?.invoke(false)
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            isListening = false
            onSpeechStatusChanged?.invoke(false)
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {}
        }
    }

    fun destroy() {
        mainHandler.post {
            isListening = false
            onSpeechStatusChanged?.invoke(false)
            try {
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
            speechRecognizer = null
        }
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val bestText = matches?.firstOrNull { it.isNotBlank() }
        if (!bestText.isNullOrBlank()) {
            val normalized = FreightLexiconNormalizer.normalize(bestText)
            _transcriptFlow.tryEmit(normalized)
        }

        // Restart listening loop if continuous ambient monitoring is still enabled
        if (isListening) {
            mainHandler.postDelayed({
                if (isListening) startListening()
            }, 100)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val partial = matches?.firstOrNull { it.isNotBlank() }
        if (!partial.isNullOrBlank()) {
            val normalized = FreightLexiconNormalizer.normalize(partial)
            _transcriptFlow.tryEmit(normalized)
        }
    }

    override fun onError(error: Int) {
        // Continuous ambient restart on silence timeout or no match
        if (isListening) {
            val retryDelay = when (error) {
                SpeechRecognizer.ERROR_NO_MATCH,
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 150L
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 400L
                else -> 1000L
            }
            mainHandler.postDelayed({
                if (isListening) startListening()
            }, retryDelay)
        }
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {}
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onEvent(eventType: Int, params: Bundle?) {}
}
