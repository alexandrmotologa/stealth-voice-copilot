package com.copilot.stealth.data.models

import com.copilot.stealth.engine.calculator.DetentionResult
import com.copilot.stealth.engine.calculator.HosReachResult
import com.copilot.stealth.engine.calculator.RpmResult

/**
 * Single source of truth for the Stealth Voice Copilot UI state.
 */
data class StealthUiState(
    val isListening: Boolean = false,
    val isRomanianEnabled: Boolean = true,
    val guidanceLanguage: GuidanceLanguage = GuidanceLanguage.ROMANIAN,
    val prompterStyle: PrompterStyle = PrompterStyle.ANCHOR_BULLETS,
    val isSpeakingCooldownActive: Boolean = false,
    val speakingCooldownRemainingSeconds: Int = 0,
    val isProcessing: Boolean = false,
    val overlayMode: OverlayMode = OverlayMode.LOCKSCREEN,
    val themeMode: DisplayThemeMode = DisplayThemeMode.AMOLED_STANDARD,
    val sttLanguageMode: SttLanguageMode = SttLanguageMode.ROMANIAN,
    val isWhisperEnabled: Boolean = false,
    val isCalculatorVisible: Boolean = false,
    val isCarrierProfileOpen: Boolean = false,
    val isMockSimulatorOpen: Boolean = false,
    val isEngineSelectionOpen: Boolean = false,
    val geminiApiKey: String = "",
    val geminiBackupApiKey: String = "",
    val geminiModel: String = "gemini-2.5-flash",
    val geminiFallbackModel: String = "gemini-2.0-flash",
    val availableGeminiModels: List<String> = listOf("gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-flash"),
    val isTestingGeminiKey: Boolean = false,
    val geminiKeyTestMessage: String? = null,
    val groqApiKey: String = "",
    val groqModel: String = "llama-3.3-70b-versatile",
    val availableGroqModels: List<String> = listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant", "gemma2-9b-it"),
    val isTestingGroqKey: Boolean = false,
    val groqKeyTestMessage: String? = null,
    val isQuestionAutoAnswerEnabled: Boolean = true,
    val preferredLocalModel: String = "Llama 3.2 1B",
    val isLlamaDownloaded: Boolean = false,
    val isGemmaDownloaded: Boolean = false,
    val livePartialTranscript: String? = null,
    val livePartialTranslation: String? = null,
    val currentTurns: List<CopilotTurn> = emptyList(),
    val errorMessage: String? = null,
    val batterySaverMode: Boolean = false,
    val activeEngineType: EngineType = EngineType.HYBRID,
    val audioInputSource: AudioInputSource = AudioInputSource.MICROPHONE,
    val isBluetoothHeadsetConnected: Boolean = false,
    val speechProbability: Float = 0.0f,
    val audioRmsLevel: Float = 0.0f,
    val lastLatencyMs: Long = 0L,
    val lastDetentionResult: DetentionResult? = null,
    val lastRpmResult: RpmResult? = null,
    val lastHosResult: HosReachResult? = null,
    val carrierProfile: CarrierProfile = CarrierProfile()
)
