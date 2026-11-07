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
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Continuous speech recognizer with resilient auto-reconnect,
 * pause tolerance, and automatic recovery from Android/MIUI IPC binder stalls.
 * Fixes rapid beep-looping by eliminating premature cancellation and providing
 * smooth, debounce-protected continuous listening.
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

    @Volatile
    private var isContinuousListeningRequested = false

    @Volatile
    private var isSessionActive = false

    private var consecutiveErrors = 0
    private var retryRunnable: Runnable? = null
    private var currentLanguageMode: com.copilot.stealth.data.models.SttLanguageMode =
        com.copilot.stealth.data.models.SttLanguageMode.ENGLISH
    private var currentTargetLocale: String = "ro-RO"

    var onSpeechStatusChanged: ((isRecognizing: Boolean) -> Unit)? = null

    fun startListening(
        languageMode: com.copilot.stealth.data.models.SttLanguageMode = com.copilot.stealth.data.models.SttLanguageMode.ENGLISH,
        targetLocale: String = "ro-RO"
    ) {
        Log.d(TAG, "startListening called with mode=$languageMode, targetLocale=$targetLocale")
        currentLanguageMode = languageMode
        currentTargetLocale = targetLocale
        isContinuousListeningRequested = true
        startListeningInternal()
    }

    private fun startListeningInternal() {
        if (!isContinuousListeningRequested) return

        mainHandler.post {
            if (!isContinuousListeningRequested) return@post
            if (isSessionActive) {
                Log.d(TAG, "Session already active; skipping redundant startListening")
                return@post
            }

            try {
                if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                    Log.e(TAG, "Speech recognition is not available on this device")
                    isContinuousListeningRequested = false
                    isSessionActive = false
                    onSpeechStatusChanged?.invoke(false)
                    return@post
                }

                if (speechRecognizer == null) {
                    speechRecognizer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                        SpeechRecognizer.isOnDeviceRecognitionAvailable(context)
                    ) {
                        Log.d(TAG, "Creating on-device SpeechRecognizer")
                        SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                    } else {
                        Log.d(TAG, "Creating standard SpeechRecognizer")
                        SpeechRecognizer.createSpeechRecognizer(context)
                    }
                    speechRecognizer?.setRecognitionListener(this)
                }

                val primaryLang = when (currentLanguageMode) {
                    com.copilot.stealth.data.models.SttLanguageMode.ENGLISH -> "en-US"
                    com.copilot.stealth.data.models.SttLanguageMode.ROMANIAN -> currentTargetLocale
                    com.copilot.stealth.data.models.SttLanguageMode.BILINGUAL_AUTO -> "en-US"
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, primaryLang)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, primaryLang)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)

                    // Generous silence windows to prevent premature cutoffs while user pauses or thinks
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 2500L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
                    putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1800L)
                    putExtra("android.speech.extra.DICTATION_MODE", true)

                    if (currentLanguageMode == com.copilot.stealth.data.models.SttLanguageMode.BILINGUAL_AUTO) {
                        val additionalLangs = arrayOf("en-US", currentTargetLocale)
                        putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", additionalLangs)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            putExtra("android.speech.extra.ADDITIONAL_LANGUAGES", additionalLangs)
                        }
                    }
                }

                cancelPendingRetry()
                isSessionActive = true
                speechRecognizer?.startListening(intent)
                onSpeechStatusChanged?.invoke(true)
                Log.d(TAG, "startListening invoked successfully with primaryLang=$primaryLang")
            } catch (e: Exception) {
                Log.e(TAG, "Error starting speech recognition: ${e.message}", e)
                safeDestroyRecognizer()
                isSessionActive = false
                scheduleRestart(750L)
            }
        }
    }

    fun stopListening() {
        Log.d(TAG, "stopListening invoked by user/system")
        isContinuousListeningRequested = false
        isSessionActive = false
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
        Log.d(TAG, "destroy invoked")
        isContinuousListeningRequested = false
        isSessionActive = false
        mainHandler.post {
            cancelPendingRetry()
            consecutiveErrors = 0
            onSpeechStatusChanged?.invoke(false)
            safeDestroyRecognizer()
        }
    }

    private fun safeDestroyRecognizer() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        isSessionActive = false
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
        isSessionActive = false
        consecutiveErrors = 0
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val bestText = matches?.firstOrNull { it.isNotBlank() }
        Log.d(TAG, "onResults recognized: '$bestText'")
        if (!bestText.isNullOrBlank()) {
            val normalized = FreightLexiconNormalizer.normalize(bestText)
            _finalTranscriptFlow.tryEmit(normalized)
        }

        // Seamlessly continue listening for next question/utterance
        if (isContinuousListeningRequested) {
            scheduleRestart(300L)
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        val partial = matches?.firstOrNull { it.isNotBlank() }
        if (!partial.isNullOrBlank()) {
            Log.d(TAG, "onPartialResults: '$partial'")
            val normalized = FreightLexiconNormalizer.normalize(partial)
            _partialTranscriptFlow.tryEmit(normalized)
        }
    }

    override fun onError(error: Int) {
        isSessionActive = false
        Log.w(TAG, "onError: code=$error (isContinuousListeningRequested=$isContinuousListeningRequested)")

        if (!isContinuousListeningRequested) return

        if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
            Log.e(TAG, "Permission denied for SpeechRecognizer")
            isContinuousListeningRequested = false
            cancelPendingRetry()
            onSpeechStatusChanged?.invoke(false)
            return
        }

        // NO_MATCH (7) and SPEECH_TIMEOUT (6) are natural conversational silence pauses.
        // They are completely normal during calls and MUST NOT trigger rapid restarts!
        val isSilenceTimeout = error == SpeechRecognizer.ERROR_NO_MATCH ||
                error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT

        val retryDelay: Long
        if (isSilenceTimeout) {
            consecutiveErrors = 0
            // Calm pause between silence timeouts avoids earcon chime machine-gunning
            retryDelay = 650L
        } else {
            consecutiveErrors++
            if (consecutiveErrors >= 3 || error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_CLIENT) {
                Log.w(TAG, "Recreating SpeechRecognizer instance after error $error")
                safeDestroyRecognizer()
                consecutiveErrors = 0
                retryDelay = 850L
            } else {
                retryDelay = 700L
            }
        }

        scheduleRestart(retryDelay)
    }

    override fun onReadyForSpeech(params: Bundle?) {
        Log.d(TAG, "onReadyForSpeech")
        isSessionActive = true
        consecutiveErrors = 0
        onSpeechStatusChanged?.invoke(true)
    }

    override fun onBeginningOfSpeech() {
        Log.d(TAG, "onBeginningOfSpeech")
        consecutiveErrors = 0
    }

    override fun onRmsChanged(rmsdB: Float) {
        val normalized = ((rmsdB + 2.0f) / 12.0f).coerceIn(0.0f, 1.0f)
        _rmsFlow.tryEmit(normalized)
    }

    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {
        Log.d(TAG, "onEndOfSpeech")
    }
    override fun onEvent(eventType: Int, params: Bundle?) {}

    companion object {
        private const val TAG = "AndroidSTT"
    }
}
