package com.copilot.stealth.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.copilot.stealth.data.models.AudioInputSource
import com.copilot.stealth.data.models.EngineType
import com.copilot.stealth.data.models.OverlayMode
import com.copilot.stealth.data.models.StealthUiIntent
import com.copilot.stealth.data.models.StealthUiState
import com.copilot.stealth.engine.audio.AudioRecorder
import com.copilot.stealth.engine.audio.AudioTestBench
import com.copilot.stealth.engine.audio.DiscreetHapticHelper
import com.copilot.stealth.engine.audio.SileroVadDetector
import com.copilot.stealth.engine.llm.LocalLlmEngine
import com.copilot.stealth.engine.stt.SherpaSpeechToText
import com.copilot.stealth.service.StealthOverlayService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State machine coordinator managing audio ingestion, VAD segmentation,
 * speech recognition, on-device inference, and the 180s auto-standby watchdog.
 */
class StealthViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(StealthUiState())
    val uiState: StateFlow<StealthUiState> = _uiState.asStateFlow()

    private val hapticHelper = DiscreetHapticHelper(application)
    private val audioRecorder = AudioRecorder(sampleRate = 16000, frameSize = 512)
    private val testBench = AudioTestBench(sampleRate = 16000, frameSize = 512)
    private val vadDetector = SileroVadDetector(application, sampleRate = 16000)
    private val sttEngine = SherpaSpeechToText(application, sampleRate = 16000)
    private val llmEngine = LocalLlmEngine(application)

    private var audioCaptureJob: Job? = null
    private var watchdogJob: Job? = null
    private var lastSpeechTimestamp = System.currentTimeMillis()

    init {
        setupVadCallbacks()
        startAutoStandbyWatchdog()
    }

    private fun setupVadCallbacks() {
        vadDetector.onSpeechStarted = {
            lastSpeechTimestamp = System.currentTimeMillis()
            hapticHelper.pulseSpeechDetected()
        }

        vadDetector.onSpeechFinished = { audioBuffer ->
            lastSpeechTimestamp = System.currentTimeMillis()
            processIntent(StealthUiIntent.OnSpeechDetected(audioBuffer))
        }

        vadDetector.onVADProbabilityChanged = { prob, rms ->
            _uiState.update { it.copy(speechProbability = prob, audioRmsLevel = rms) }
        }
    }

    fun processIntent(intent: StealthUiIntent) {
        when (intent) {
            is StealthUiIntent.ToggleListening -> toggleListening()
            is StealthUiIntent.ToggleRomanian -> toggleRomanian()
            is StealthUiIntent.ClearHistory -> clearHistory()
            is StealthUiIntent.ToggleOverlayMode -> toggleOverlayMode()
            is StealthUiIntent.CycleEngineType -> cycleEngineType()
            is StealthUiIntent.SetAudioSource -> setAudioSource(intent.source)
            is StealthUiIntent.OnSpeechDetected -> handleSpeechDetected(intent.audioBuffer)
            is StealthUiIntent.OnManualQuery -> handleManualQuery(intent.query)
            is StealthUiIntent.OnAudioLevelChanged -> {
                _uiState.update { it.copy(audioRmsLevel = intent.rms, speechProbability = intent.speechProb) }
            }
            is StealthUiIntent.OnError -> {
                _uiState.update { it.copy(errorMessage = intent.message, isProcessing = false) }
            }
        }
    }

    private fun toggleListening() {
        val willListen = !_uiState.value.isListening
        if (willListen) {
            startAudioStream()
        } else {
            stopAudioStream()
        }
    }

    private fun startAudioStream() {
        stopAudioStream()
        vadDetector.reset()
        lastSpeechTimestamp = System.currentTimeMillis()

        _uiState.update { it.copy(isListening = true, errorMessage = null) }

        audioCaptureJob = viewModelScope.launch {
            if (_uiState.value.audioInputSource == AudioInputSource.MICROPHONE) {
                try {
                    audioRecorder.start(this)
                    audioRecorder.audioFlow.collect { frame ->
                        vadDetector.processFrame(frame)
                    }
                } catch (e: Exception) {
                    processIntent(StealthUiIntent.OnError(e.localizedMessage ?: "Audio record error"))
                }
            } else {
                testBench.startSyntheticTestPattern(this)
                testBench.pcmFlow.collect { frame ->
                    vadDetector.processFrame(frame)
                }
            }
        }
    }

    private fun stopAudioStream() {
        audioCaptureJob?.cancel()
        audioCaptureJob = null
        audioRecorder.stop()
        testBench.stop()
        vadDetector.reset()
        _uiState.update { it.copy(isListening = false, isProcessing = false) }
    }

    private fun toggleRomanian() {
        _uiState.update { it.copy(isRomanianEnabled = !it.isRomanianEnabled) }
    }

    private fun clearHistory() {
        _uiState.update { it.copy(currentTurns = emptyList(), errorMessage = null) }
        hapticHelper.pulseContextCleared()
    }

    private fun toggleOverlayMode() {
        val nextMode = when (_uiState.value.overlayMode) {
            OverlayMode.LOCKSCREEN -> OverlayMode.FLOATING_HUD
            OverlayMode.FLOATING_HUD -> OverlayMode.MINIMIZED
            OverlayMode.MINIMIZED -> OverlayMode.LOCKSCREEN
        }
        _uiState.update { it.copy(overlayMode = nextMode) }
    }

    private fun cycleEngineType() {
        val nextEngine = when (_uiState.value.activeEngineType) {
            EngineType.HYBRID -> EngineType.DETERMINISTIC_EXPERT
            EngineType.DETERMINISTIC_EXPERT -> EngineType.LOCAL_NEURAL
            EngineType.LOCAL_NEURAL -> EngineType.HYBRID
        }
        llmEngine.setEngineType(nextEngine)
        _uiState.update { it.copy(activeEngineType = nextEngine) }
    }

    private fun setAudioSource(source: AudioInputSource) {
        _uiState.update { it.copy(audioInputSource = source) }
        if (_uiState.value.isListening) {
            startAudioStream() // restart with new source
        }
    }

    private fun handleSpeechDetected(audioBuffer: FloatArray) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val transcript = sttEngine.transcribe(audioBuffer)

            if (transcript.isNotBlank()) {
                val turn = llmEngine.generateResponse(
                    question = transcript,
                    recentTurns = _uiState.value.currentTurns,
                    isRomanianEnabled = _uiState.value.isRomanianEnabled,
                    forcedEngineType = _uiState.value.activeEngineType
                )

                _uiState.update { state ->
                    state.copy(
                        currentTurns = state.currentTurns + turn,
                        isProcessing = false,
                        lastLatencyMs = turn.latencyMs
                    )
                }

                hapticHelper.pulseAnswerReady()
                StealthOverlayService.updateHudText(getApplication(), turn.answerEn)
            } else {
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    fun handleManualQuery(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val turn = llmEngine.generateResponse(
                question = query,
                recentTurns = _uiState.value.currentTurns,
                isRomanianEnabled = _uiState.value.isRomanianEnabled,
                forcedEngineType = _uiState.value.activeEngineType
            )

            _uiState.update { state ->
                state.copy(
                    currentTurns = state.currentTurns + turn,
                    isProcessing = false,
                    lastLatencyMs = turn.latencyMs
                )
            }

            hapticHelper.pulseAnswerReady()
            StealthOverlayService.updateHudText(getApplication(), turn.answerEn)
        }
    }

    fun runSyntheticTestBenchBurst() {
        setAudioSource(AudioInputSource.TEST_BENCH)
        startAudioStream()
    }

    /**
     * Auto-standby watchdog: if no speech is detected for 180 consecutive seconds,
     * the audio engine transitions to low-power standby mode.
     */
    private fun startAutoStandbyWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = viewModelScope.launch {
            while (true) {
                delay(10_000L) // evaluate every 10 seconds
                if (_uiState.value.isListening) {
                    val idleTime = System.currentTimeMillis() - lastSpeechTimestamp
                    if (idleTime >= 180_000L) { // 180 seconds
                        stopAudioStream()
                        _uiState.update { it.copy(batterySaverMode = true) }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        stopAudioStream()
        vadDetector.close()
        watchdogJob?.cancel()
        super.onCleared()
    }
}
