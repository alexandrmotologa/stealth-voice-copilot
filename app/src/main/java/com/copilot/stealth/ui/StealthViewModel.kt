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
import com.copilot.stealth.data.repository.FreightKnowledgeEngine
import com.copilot.stealth.service.StealthOverlayService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
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
    private val downloadManager = com.copilot.stealth.engine.llm.ModelDownloadManager(application)

    private var audioCaptureJob: Job? = null
    private var watchdogJob: Job? = null
    private var lastSpeechTimestamp = System.currentTimeMillis()

    init {
        checkModelAvailability()
        val isFirstLaunch = !carrierProfileRepository.isInitialSetupCompleted()
        _uiState.update {
            it.copy(
                activeEngineType = carrierProfileRepository.getEngineType(),
                preferredLocalModel = carrierProfileRepository.getPreferredLocalModel(),
                isEngineSelectionOpen = isFirstLaunch
            )
        }
        setupVadCallbacks()
        setupTranscriptCollector()
        setupBluetoothCollector()
        startAutoStandbyWatchdog()
    }

    private fun checkModelAvailability() {
        val llamaPresent = downloadManager.isModelDownloaded(com.copilot.stealth.engine.llm.ModelDownloadManager.LLAMA_3_2_1B)
        val gemmaPresent = downloadManager.isModelDownloaded(com.copilot.stealth.engine.llm.ModelDownloadManager.GEMMA_2_2B)
        _uiState.update {
            it.copy(
                isLlamaDownloaded = llamaPresent,
                isGemmaDownloaded = gemmaPresent
            )
        }
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
            val curProb = _uiState.value.speechProbability
            if (kotlin.math.abs(prob - curProb) > 0.08f || (prob > 0.45f != curProb > 0.45f)) {
                _uiState.update { it.copy(speechProbability = prob, audioRmsLevel = rms) }
            }
        }
    }

    private fun setupTranscriptCollector() {
        // Real-time partial results as interlocutor speaks (instant feedback on screen)
        viewModelScope.launch {
            nativeStt.partialTranscriptFlow.collect { partial ->
                if (_uiState.value.isListening && partial.isNotBlank()) {
                    lastSpeechTimestamp = System.currentTimeMillis()
                    val ro = if (_uiState.value.isRomanianEnabled) {
                        FreightKnowledgeEngine.quickTranslate(partial)
                    } else null
                    _uiState.update {
                        it.copy(
                            livePartialTranscript = partial,
                            livePartialTranslation = ro,
                            speechProbability = 0.95f
                        )
                    }
                }
            }
        }

        // Final utterance when interlocutor finishes question or pauses
        viewModelScope.launch {
            nativeStt.finalTranscriptFlow.collect { transcript ->
                if (_uiState.value.isListening && transcript.isNotBlank()) {
                    lastSpeechTimestamp = System.currentTimeMillis()
                    _uiState.update {
                        it.copy(
                            livePartialTranscript = null,
                            livePartialTranslation = null
                        )
                    }
                    executeInference(transcript)
                }
            }
        }

        // Live RMS audio levels for glowing mic
        viewModelScope.launch {
            nativeStt.rmsFlow.collect { rms ->
                if (_uiState.value.isListening) {
                    _uiState.update { it.copy(audioRmsLevel = rms) }
                }
            }
        }

        // Fallback synthetic test bench collector
        viewModelScope.launch {
            sttEngine.transcriptFlow.collect { transcript ->
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
            is StealthUiIntent.ToggleEngineSelection -> {
                carrierProfileRepository.setInitialSetupCompleted(true)
                checkModelAvailability()
                _uiState.update { it.copy(isEngineSelectionOpen = !it.isEngineSelectionOpen) }
            }
            is StealthUiIntent.SelectEngineType -> {
                carrierProfileRepository.saveEngineType(intent.engineType)
                carrierProfileRepository.setInitialSetupCompleted(true)
                llmEngine.setEngineType(intent.engineType)
                _uiState.update { it.copy(activeEngineType = intent.engineType) }
            }
            is StealthUiIntent.SelectLocalModel -> {
                carrierProfileRepository.savePreferredLocalModel(intent.modelName)
                carrierProfileRepository.setInitialSetupCompleted(true)
                llmEngine.setPreferredLocalModel(intent.modelName)
                _uiState.update { it.copy(preferredLocalModel = intent.modelName) }
            }
            is StealthUiIntent.TriggerModelDownload -> {
                val spec = if (intent.modelId.contains("gemma")) {
                    com.copilot.stealth.engine.llm.ModelDownloadManager.GEMMA_2_2B
                } else {
                    com.copilot.stealth.engine.llm.ModelDownloadManager.LLAMA_3_2_1B
                }
                downloadManager.startDownload(spec)
            }
            is StealthUiIntent.SaveCarrierProfile -> saveCarrierProfile(intent.profile)
            is StealthUiIntent.SetFontSize -> {
                val updated = _uiState.value.carrierProfile.copy(fontSize = intent.fontSize)
                saveCarrierProfile(updated)
            }
            is StealthUiIntent.SetProfileMode -> {
                val updated = _uiState.value.carrierProfile.copy(profileMode = intent.mode)
                saveCarrierProfile(updated)
            }
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

        _uiState.update {
            it.copy(
                isListening = true,
                errorMessage = null,
                livePartialTranscript = null,
                livePartialTranslation = null
            )
        }
        com.copilot.stealth.widget.StealthLockScreenWidget.updateAll(
            context = getApplication(),
            isListening = true,
            isBtConnected = _uiState.value.isBluetoothHeadsetConnected
        )

        val source = _uiState.value.audioInputSource
        if (source == AudioInputSource.MICROPHONE) {
            // Live on-device Google SpeechRecognizer handles mic stream directly
            nativeStt.startListening(_uiState.value.sttLanguageMode)
        } else if (source == AudioInputSource.SYSTEM_CALL) {
            audioCaptureJob = viewModelScope.launch(Dispatchers.Default) {
                try {
                    audioRecorder.start(this, inputSource = source)
                    audioRecorder.audioFlow.collect { frame ->
                        vadDetector.processFrame(frame)
                    }
                } catch (e: CancellationException) {
                    // Clean coroutine cancellation on stop / toggle listening - do NOT emit error
                } catch (e: Exception) {
                    processIntent(StealthUiIntent.OnError(e.localizedMessage ?: "Audio record error"))
                }
            }
        } else {
            audioCaptureJob = viewModelScope.launch(Dispatchers.Default) {
                try {
                    testBench.startSyntheticTestPattern(this)
                    testBench.pcmFlow.collect { frame ->
                        vadDetector.processFrame(frame)
                    }
                } catch (e: CancellationException) {
                    // Clean coroutine cancellation - do NOT emit error
                } catch (e: Exception) {
                    processIntent(StealthUiIntent.OnError(e.localizedMessage ?: "Audio record error"))
                }
            }
        }
    }

    private fun stopAudioStream() {
        try {
            audioCaptureJob?.cancel()
        } catch (_: Exception) {}
        audioCaptureJob = null

        audioRecorder.stop()
        testBench.stop()
        nativeStt.stopListening()
        sttEngine.stopLiveStream()
        audioPrompter.stop()
        vadDetector.reset()

        _uiState.update {
            it.copy(
                isListening = false,
                isProcessing = false,
                livePartialTranscript = null,
                livePartialTranslation = null,
                errorMessage = null
            )
        }

        com.copilot.stealth.widget.StealthLockScreenWidget.updateAll(
            context = getApplication(),
            isListening = false,
            isBtConnected = _uiState.value.isBluetoothHeadsetConnected
        )
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

        val app = getApplication<Application>()
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M && android.provider.Settings.canDrawOverlays(app)) {
            val intent = android.content.Intent(app, StealthOverlayService::class.java)
            if (nextMode == OverlayMode.FLOATING_HUD || nextMode == OverlayMode.MINIMIZED) {
                app.startService(intent)
            } else {
                app.stopService(intent)
            }
        }
    }

    private fun cycleEngineType() {
        val nextEngine = when (_uiState.value.activeEngineType) {
            EngineType.HYBRID -> EngineType.LOCAL_NEURAL
            EngineType.LOCAL_NEURAL -> EngineType.HYBRID
            EngineType.DETERMINISTIC_EXPERT -> EngineType.HYBRID
        }
        carrierProfileRepository.saveEngineType(nextEngine)
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
                profile = _uiState.value.carrierProfile,
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
            com.copilot.stealth.widget.StealthLockScreenWidget.updateAll(
                context = getApplication(),
                isListening = _uiState.value.isListening,
                category = turn.matchedCategory ?: "DISPATCH INTENT",
                answerEn = turn.answerEn,
                answerRo = turn.answerRo,
                latencyMs = turn.latencyMs,
                isBtConnected = _uiState.value.isBluetoothHeadsetConnected
            )

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
