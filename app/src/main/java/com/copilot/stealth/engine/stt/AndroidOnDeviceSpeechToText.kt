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

    private val _partialTranscriptFlow = MutableSharedFlow<String>(extraBufferCapacity = 32)
    val partialTranscriptFlow: SharedFlow<String> = _partialTranscriptFlow.asSharedFlow()

    private val _finalTranscriptFlow = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val finalTranscriptFlow: SharedFlow<String> = _finalTranscriptFlow.asSharedFlow()

    private val _rmsFlow = MutableSharedFlow<Float>(extraBufferCapacity = 16)
    val rmsFlow: SharedFlow<Float> = _rmsFlow.asSharedFlow()

    // Backwards compatibility transcriptFlow
    val transcriptFlow: SharedFlow<String> = _finalTranscriptFlow

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private var isListening = false
    private var consecutiveErrors = 0
    private var retryRunnable: Runnable? = null
    private var currentLanguageMode: com.copilot.stealth.data.models.SttLanguageMode =
        com.copilot.stealth.data.models.SttLanguageMode.ENGLISH

    var onSpeechStatusChanged: ((isRecognizing: Boolean) -> Unit)? = null

    fun startListening(languageMode: com.copilot.stealth.data.models.SttLanguageMode = com.copilot.stealth.data.models.SttLanguageMode.ENGLISH) {
        currentLanguageMode = languageMode
        mainHandler.post {
            try {
                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    isListening = false
                    onSpeechStatusChanged?.invoke(false)
                    return@post
                }

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
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                }

                cancelPendingRetry()
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
            cancelPendingRetry()
            isListening = false
            consecutiveErrors = 0
            onSpeechStatusChanged?.invoke(false)
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
            } catch (_: Exception) {}
        }
    }

    fun destroy() {
        mainHandler.post {
            cancelPendingRetry()
            isListening = false
            consecutiveErrors = 0
            onSpeechStatusChanged?.invoke(false)
            try {
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
            speechRecognizer = null
        }
    }

    private fun cancelPendingRetry() {
        retryRunnable?.let { mainHandler.removeCallbacks(it) }
        retryRunnable = null
    }

    override fun onResults(results: Bundle?) {
        consecutiveErrors = 0
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val bestText = matches?.firstOrNull { it.isNotBlank() }
        if (!bestText.isNullOrBlank()) {
            val normalized = FreightLexiconNormalizer.normalize(bestText)
            _finalTranscriptFlow.tryEmit(normalized)
        }

        if (isListening) {
            cancelPendingRetry()
            retryRunnable = Runnable {
                if (isListening) startListening(currentLanguageMode)
            }
            mainHandler.postDelayed(retryRunnable!!, 150L)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val partial = matches?.firstOrNull { it.isNotBlank() }
        if (!partial.isNullOrBlank()) {
            val normalized = FreightLexiconNormalizer.normalize(partial)
            _partialTranscriptFlow.tryEmit(normalized)
        }
    }

    override fun onError(error: Int) {
        if (!isListening) return

        if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
            isListening = false
            cancelPendingRetry()
            onSpeechStatusChanged?.invoke(false)
            return
        }

        consecutiveErrors++
        if (consecutiveErrors >= 6) {
            try {
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
            speechRecognizer = null
            consecutiveErrors = 0
        }

        val retryDelay = when (error) {
            SpeechRecognizer.ERROR_NO_MATCH,
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 100L
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 350L
            else -> 500L
        }

        cancelPendingRetry()
        retryRunnable = Runnable {
            if (isListening) startListening(currentLanguageMode)
        }
        mainHandler.postDelayed(retryRunnable!!, retryDelay)
    }

    override fun onReadyForSpeech(params: Bundle?) {
        consecutiveErrors = 0
        onSpeechStatusChanged?.invoke(true)
    }

    override fun onBeginningOfSpeech() {
        consecutiveErrors = 0
    }

    override fun onRmsChanged(rmsdB: Float) {
        val normalized = ((rmsdB + 2.0f) / 12.0f).coerceIn(0.0f, 1.0f)
        _rmsFlow.tryEmit(normalized)
    }

    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {}
    override fun onEvent(eventType: Int, params: Bundle?) {}
}
