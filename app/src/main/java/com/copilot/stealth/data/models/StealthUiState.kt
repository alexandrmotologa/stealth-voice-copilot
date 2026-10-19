package com.copilot.stealth.data.models

/**
 * Single source of truth for the Stealth Voice Copilot UI state.
 */
data class StealthUiState(
    val isListening: Boolean = false,
    val isRomanianEnabled: Boolean = true,
    val isProcessing: Boolean = false,
    val overlayMode: OverlayMode = OverlayMode.LOCKSCREEN,
    val currentTurns: List<CopilotTurn> = emptyList(),
    val errorMessage: String? = null,
    val batterySaverMode: Boolean = false,
    val activeEngineType: EngineType = EngineType.HYBRID,
    val audioInputSource: AudioInputSource = AudioInputSource.MICROPHONE,
    val speechProbability: Float = 0.0f,
    val audioRmsLevel: Float = 0.0f,
    val lastLatencyMs: Long = 0L
)
