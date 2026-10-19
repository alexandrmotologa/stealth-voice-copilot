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
 * Continuous on-device speech recognizer with resilient auto-reconnect,
 * pause tolerance, and automatic recovery from Android/MIUI IPC binder stalls.
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

    // Master user intent: once started, listen continuously across all questions until explicitly stopped
    @Volatile
    private var isContinuousListeningRequested = false

    private var consecutiveErrors = 0
    private var retryRunnable: Runnable? = null
    private var currentLanguageMode: com.copilot.stealth.data.models.SttLanguageMode =
        com.copilot.stealth.data.models.SttLanguageMode.ENGLISH

    var onSpeechStatusChanged: ((isRecognizing: Boolean) -> Unit)? = null

    fun startListening(languageMode: com.copilot.stealth.data.models.SttLanguageMode = com.copilot.stealth.data.models.SttLanguageMode.ENGLISH) {
        currentLanguageMode = languageMode
        isContinuousListeningRequested = true
        startListeningInternal()
    }

    private fun startListeningInternal() {
        if (!isContinuousListeningRequested) return

        mainHandler.post {
            if (!isContinuousListeningRequested) return@post
            try {
                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    isContinuousListeningRequested = false
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

                val primaryLang = when (currentLanguageMode) {
                    com.copilot.stealth.data.models.SttLanguageMode.ENGLISH -> "en-US"
                    com.copilot.stealth.data.models.SttLanguageMode.ROMANIAN -> "ro-RO"
                    com.copilot.stealth.data.models.SttLanguageMode.BILINGUAL_AUTO -> "en-US"
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, primaryLang)
                    if (currentLanguageMode == com.copilot.stealth.data.models.SttLanguageMode.BILINGUAL_AUTO && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        putExtra("android.speech.extra.ADDITIONAL_LANGUAGES", arrayOf("ro-RO", "en-US"))
                    }
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                }

                cancelPendingRetry()

                // Cancel previous session before starting new one to ensure clean binder state
                try {
                    speechRecognizer?.cancel()
                } catch (_: Exception) {}

                speechRecognizer?.startListening(intent)
                onSpeechStatusChanged?.invoke(true)
            } catch (e: Exception) {
                // Recover gracefully without aborting continuous listening loop
                try {
                    speechRecognizer?.destroy()
                } catch (_: Exception) {}
                speechRecognizer = null
                scheduleRestart(350L)
            }
        }
    }

    fun stopListening() {
        isContinuousListeningRequested = false
        mainHandler.post {
            cancelPendingRetry()
            consecutiveErrors = 0
            onSpeechStatusChanged?.invoke(false)
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
            } catch (_: Exception) {}
        }
    }

    fun destroy() {
        isContinuousListeningRequested = false
        mainHandler.post {
            cancelPendingRetry()
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

    private fun scheduleRestart(delayMs: Long) {
        if (!isContinuousListeningRequested) return
        cancelPendingRetry()
        retryRunnable = Runnable {
            if (isContinuousListeningRequested) {
                startListeningInternal()
            }
        }
        mainHandler.postDelayed(retryRunnable!!, delayMs)
    }

    override fun onResults(results: Bundle?) {
        consecutiveErrors = 0
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val bestText = matches?.firstOrNull { it.isNotBlank() }
        if (!bestText.isNullOrBlank()) {
            val normalized = FreightLexiconNormalizer.normalize(bestText)
            _finalTranscriptFlow.tryEmit(normalized)
        }

        // Seamlessly continue listening for next question/utterance
        if (isContinuousListeningRequested) {
            try {
                speechRecognizer?.cancel()
            } catch (_: Exception) {}
            scheduleRestart(200L)
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
        if (!isContinuousListeningRequested) return

        if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
            isContinuousListeningRequested = false
            cancelPendingRetry()
            onSpeechStatusChanged?.invoke(false)
            return
        }

        // NO_MATCH (7) and SPEECH_TIMEOUT (6) are natural conversational silence pauses.
        // They are completely normal during interviews and MUST NOT kill the continuous session!
        val isSilenceTimeout = error == SpeechRecognizer.ERROR_NO_MATCH ||
                error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT

        if (!isSilenceTimeout) {
            consecutiveErrors++
            if (consecutiveErrors >= 3 || error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_CLIENT) {
                try {
                    speechRecognizer?.cancel()
                    speechRecognizer?.destroy()
                } catch (_: Exception) {}
                speechRecognizer = null
                consecutiveErrors = 0
            }
        } else {
            consecutiveErrors = 0
            try {
                speechRecognizer?.cancel()
            } catch (_: Exception) {}
        }

        val retryDelay = when {
            isSilenceTimeout -> 100L
            error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 350L
            else -> 400L
        }

        scheduleRestart(retryDelay)
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
