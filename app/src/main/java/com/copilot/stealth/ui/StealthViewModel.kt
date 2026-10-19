package com.copilot.stealth.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.copilot.stealth.data.models.AudioInputSource
import com.copilot.stealth.data.models.CarrierProfile
import com.copilot.stealth.data.models.DisplayThemeMode
import com.copilot.stealth.data.models.EngineType
import com.copilot.stealth.data.models.OverlayMode
import com.copilot.stealth.data.models.StealthUiIntent
import com.copilot.stealth.data.models.StealthUiState
import com.copilot.stealth.data.models.SttLanguageMode
import com.copilot.stealth.data.repository.CarrierProfileRepository
import com.copilot.stealth.data.repository.SessionExportManager
import com.copilot.stealth.engine.audio.AudioRecorder
import com.copilot.stealth.engine.audio.AudioTestBench
import com.copilot.stealth.engine.audio.BluetoothAudioMonitor
import com.copilot.stealth.engine.audio.DiscreetAudioPrompter
import com.copilot.stealth.engine.audio.DiscreetHapticHelper
import com.copilot.stealth.engine.audio.SileroVadDetector
import com.copilot.stealth.engine.calculator.FreightCalculatorEngine
import com.copilot.stealth.engine.llm.LocalLlmEngine
import com.copilot.stealth.engine.stt.AndroidOnDeviceSpeechToText
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
 * bilingual speech recognition, on-device inference, whisper prompter, and watchdog.
 */
class StealthViewModel(application: Application) : AndroidViewModel(application) {

    private val carrierProfileRepository = CarrierProfileRepository(application)
    private val _uiState = MutableStateFlow(
        StealthUiState(carrierProfile = carrierProfileRepository.getProfile())
    )
    val uiState: StateFlow<StealthUiState> = _uiState.asStateFlow()

    private val hapticHelper = DiscreetHapticHelper(application)
    private val audioRecorder = AudioRecorder(sampleRate = 16000, frameSize = 512)
    private val testBench = AudioTestBench(sampleRate = 16000, frameSize = 512)
    private val vadDetector = SileroVadDetector(application, sampleRate = 16000)
    private val sttEngine = SherpaSpeechToText(application, sampleRate = 16000)
    private val nativeStt = AndroidOnDeviceSpeechToText(application)
    private val llmEngine = LocalLlmEngine(application, carrierProfileRepository)
    private val bluetoothMonitor = BluetoothAudioMonitor(application)
    private val exportManager = SessionExportManager(application)
    private val audioPrompter = DiscreetAudioPrompter(application, bluetoothMonitor)

    private var audioCaptureJob: Job? = null
    private var watchdogJob: Job? = null
    private var lastSpeechTimestamp = System.currentTimeMillis()

    init {
        setupVadCallbacks()
        setupTranscriptCollector()
        setupBluetoothCollector()
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

    private fun setupTranscriptCollector() {
        viewModelScope.launch {
            sttEngine.transcriptFlow.collect { transcript ->
                if (transcript.isNotBlank() && _uiState.value.isListening) {
                    lastSpeechTimestamp = System.currentTimeMillis()
                    executeInference(transcript)
                }
            }
        }
        viewModelScope.launch {
            nativeStt.transcriptFlow.collect { transcript ->
                if (transcript.isNotBlank() && _uiState.value.isListening) {
                    lastSpeechTimestamp = System.currentTimeMillis()
                    executeInference(transcript)
                }
            }
        }
    }

    private fun setupBluetoothCollector() {
        viewModelScope.launch {
            bluetoothMonitor.isBluetoothConnected.collect { isConnected ->
                _uiState.update { it.copy(isBluetoothHeadsetConnected = isConnected) }
            }
        }
    }

    fun processIntent(intent: StealthUiIntent) {
        when (intent) {
            is StealthUiIntent.ToggleListening -> toggleListening()
            is StealthUiIntent.ToggleRomanian -> toggleRomanian()
            is StealthUiIntent.ClearHistory -> clearHistory()
            is StealthUiIntent.PanicWipe -> panicWipe()
            is StealthUiIntent.ExportTranscript -> exportTranscript()
            is StealthUiIntent.ToggleOverlayMode -> toggleOverlayMode()
            is StealthUiIntent.CycleEngineType -> cycleEngineType()
            is StealthUiIntent.CycleThemeMode -> cycleThemeMode()
            is StealthUiIntent.CycleSttLanguage -> cycleSttLanguage()
            is StealthUiIntent.ToggleWhisperMode -> toggleWhisperMode()
            is StealthUiIntent.ToggleCalculator -> toggleCalculator()
            is StealthUiIntent.ToggleCarrierProfileEditor -> toggleCarrierProfileEditor()
            is StealthUiIntent.ToggleMockSimulator -> toggleMockSimulator()
            is StealthUiIntent.SaveCarrierProfile -> saveCarrierProfile(intent.profile)
            is StealthUiIntent.SetAudioSource -> setAudioSource(intent.source)
            is StealthUiIntent.SetBluetoothConnected -> {
                _uiState.update { it.copy(isBluetoothHeadsetConnected = intent.isConnected) }
            }
            is StealthUiIntent.OnSpeechDetected -> handleSpeechDetected(intent.audioBuffer)
            is StealthUiIntent.OnManualQuery -> handleManualQuery(intent.query)
            is StealthUiIntent.OnAudioLevelChanged -> {
                _uiState.update { it.copy(audioRmsLevel = intent.rms, speechProbability = intent.speechProb) }
            }
            is StealthUiIntent.OnError -> {
                _uiState.update { it.copy(errorMessage = intent.message, isProcessing = false) }
            }
            is StealthUiIntent.CalculateDetention -> {
                val res = FreightCalculatorEngine.calculateDetention(
                    intent.appointmentTime, intent.inTime, intent.outTime, intent.rate
                )
                _uiState.update { it.copy(lastDetentionResult = res) }
            }
            is StealthUiIntent.CalculateRpm -> {
                val res = FreightCalculatorEngine.calculateRpm(
                    intent.totalRate, intent.loadedMiles, intent.deadheadMiles
                )
                _uiState.update { it.copy(lastRpmResult = res) }
            }
            is StealthUiIntent.CalculateHos -> {
                val res = FreightCalculatorEngine.calculateHosReach(
                    intent.remainingHours, intent.tripMiles
                )
                _uiState.update { it.copy(lastHosResult = res) }
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

        val source = _uiState.value.audioInputSource
        if (source == AudioInputSource.MICROPHONE || source == AudioInputSource.SYSTEM_CALL) {
            nativeStt.startListening(_uiState.value.sttLanguageMode)
            sttEngine.startLiveStream {}
            audioCaptureJob = viewModelScope.launch {
                try {
                    audioRecorder.start(this, inputSource = source)
                    audioRecorder.audioFlow.collect { frame ->
                        vadDetector.processFrame(frame)
                    }
                } catch (e: Exception) {
                    processIntent(StealthUiIntent.OnError(e.localizedMessage ?: "Audio record error"))
                }
            }
        } else {
            audioCaptureJob = viewModelScope.launch {
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
        nativeStt.stopListening()
        sttEngine.stopLiveStream()
        audioPrompter.stop()
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

    private fun panicWipe() {
        clearHistory()
        exportManager.panicWipe()
        audioPrompter.stop()
        hapticHelper.pulseContextCleared()
    }

    private fun exportTranscript() {
        val turns = _uiState.value.currentTurns
        val plainText = exportManager.formatPlainText(turns)
        exportManager.copyToClipboard("Stealth Interview Transcript", plainText)
        hapticHelper.pulseAnswerReady()
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

    private fun cycleThemeMode() {
        val nextTheme = when (_uiState.value.themeMode) {
            DisplayThemeMode.AMOLED_STANDARD -> DisplayThemeMode.NIGHT_VISION_RED
            DisplayThemeMode.NIGHT_VISION_RED -> DisplayThemeMode.AMBER_DIFFUSE
            DisplayThemeMode.AMBER_DIFFUSE -> DisplayThemeMode.AMOLED_STANDARD
        }
        _uiState.update { it.copy(themeMode = nextTheme) }
    }

    private fun cycleSttLanguage() {
        val nextLang = when (_uiState.value.sttLanguageMode) {
            SttLanguageMode.ENGLISH -> SttLanguageMode.ROMANIAN
            SttLanguageMode.ROMANIAN -> SttLanguageMode.BILINGUAL_AUTO
            SttLanguageMode.BILINGUAL_AUTO -> SttLanguageMode.ENGLISH
        }
        _uiState.update { it.copy(sttLanguageMode = nextLang) }
        if (_uiState.value.isListening) {
            startAudioStream()
        }
    }

    private fun toggleWhisperMode() {
        val next = !_uiState.value.isWhisperEnabled
        audioPrompter.setWhisperEnabled(next)
        _uiState.update { it.copy(isWhisperEnabled = next) }
        if (next) {
            hapticHelper.pulseSpeechDetected()
        }
    }

    private fun toggleCalculator() {
        _uiState.update { it.copy(isCalculatorVisible = !it.isCalculatorVisible) }
    }

    private fun toggleCarrierProfileEditor() {
        _uiState.update { it.copy(isCarrierProfileOpen = !it.isCarrierProfileOpen) }
    }

    private fun toggleMockSimulator() {
        _uiState.update { it.copy(isMockSimulatorOpen = !it.isMockSimulatorOpen) }
    }

    private fun saveCarrierProfile(profile: CarrierProfile) {
        carrierProfileRepository.saveProfile(profile)
        llmEngine.reloadCarrierProfile()
        _uiState.update {
            it.copy(carrierProfile = profile, isCarrierProfileOpen = false)
        }
        hapticHelper.pulseAnswerReady()
    }

    private fun setAudioSource(source: AudioInputSource) {
        _uiState.update { it.copy(audioInputSource = source) }
        if (_uiState.value.isListening) {
            startAudioStream()
        }
    }

    private fun handleSpeechDetected(audioBuffer: FloatArray) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true) }
            val transcript = sttEngine.transcribe(audioBuffer)
            if (transcript.isNotBlank()) {
                executeInference(transcript)
            } else {
                _uiState.update { it.copy(isProcessing = false) }
            }
        }
    }

    fun handleManualQuery(query: String) {
        if (query.isBlank()) return
        executeInference(query)
    }

    private fun executeInference(query: String) {
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

            // Whisper key answer into earpiece if enabled
            if (_uiState.value.isWhisperEnabled) {
                audioPrompter.whisperAnswerInsight(turn.answerEn)
            }
        }
    }

    fun runSyntheticTestBenchBurst() {
        setAudioSource(AudioInputSource.TEST_BENCH)
        startAudioStream()
    }

    private fun startAutoStandbyWatchdog() {
        watchdogJob?.cancel()
        watchdogJob = viewModelScope.launch {
            while (true) {
                delay(10_000L)
                if (_uiState.value.isListening) {
                    val idleTime = System.currentTimeMillis() - lastSpeechTimestamp
                    if (idleTime >= 180_000L) {
                        stopAudioStream()
                        _uiState.update { it.copy(batterySaverMode = true) }
                    }
                }
            }
        }
    }

    override fun onCleared() {
        stopAudioStream()
        sttEngine.destroy()
        nativeStt.destroy()
        vadDetector.close()
        bluetoothMonitor.release()
        audioPrompter.close()
        llmEngine.close()
        watchdogJob?.cancel()
        super.onCleared()
    }
}
