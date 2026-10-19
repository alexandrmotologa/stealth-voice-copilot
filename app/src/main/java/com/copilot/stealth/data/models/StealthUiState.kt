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
    val isProcessing: Boolean = false,
    val overlayMode: OverlayMode = OverlayMode.LOCKSCREEN,
    val themeMode: DisplayThemeMode = DisplayThemeMode.AMOLED_STANDARD,
    val isCalculatorVisible: Boolean = false,
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
    val lastHosResult: HosReachResult? = null
)
