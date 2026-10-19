package com.copilot.stealth.data.models

/**
 * User intents and asynchronous system events processed by StealthViewModel.
 */
sealed interface StealthUiIntent {
    data object ToggleListening : StealthUiIntent
    data object ToggleRomanian : StealthUiIntent
    data object ClearHistory : StealthUiIntent
    data object ToggleOverlayMode : StealthUiIntent
    data object CycleEngineType : StealthUiIntent
    data class SetAudioSource(val source: AudioInputSource) : StealthUiIntent
    data class OnSpeechDetected(val audioBuffer: FloatArray) : StealthUiIntent
    data class OnManualQuery(val query: String) : StealthUiIntent
    data class OnAudioLevelChanged(val rms: Float, val speechProb: Float) : StealthUiIntent
    data class OnError(val message: String) : StealthUiIntent
}
