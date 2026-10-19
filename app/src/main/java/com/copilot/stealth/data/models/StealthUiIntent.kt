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
    data object CycleThemeMode : StealthUiIntent
    data object CycleSttLanguage : StealthUiIntent
    data object ToggleWhisperMode : StealthUiIntent
    data object ToggleCalculator : StealthUiIntent
    data object ToggleCarrierProfileEditor : StealthUiIntent
    data object ToggleMockSimulator : StealthUiIntent
    data object PanicWipe : StealthUiIntent
    data object ExportTranscript : StealthUiIntent
    data class SaveCarrierProfile(val profile: CarrierProfile) : StealthUiIntent
    data class SetAudioSource(val source: AudioInputSource) : StealthUiIntent
    data class SetBluetoothConnected(val isConnected: Boolean) : StealthUiIntent
    data class OnSpeechDetected(val audioBuffer: FloatArray) : StealthUiIntent
    data class OnManualQuery(val query: String) : StealthUiIntent
    data class OnAudioLevelChanged(val rms: Float, val speechProb: Float) : StealthUiIntent
    data class OnError(val message: String) : StealthUiIntent
    data class CalculateDetention(val appointmentTime: String, val inTime: String, val outTime: String, val rate: Double) : StealthUiIntent
    data class CalculateRpm(val totalRate: Double, val loadedMiles: Double, val deadheadMiles: Double) : StealthUiIntent
    data class CalculateHos(val remainingHours: Double, val tripMiles: Double) : StealthUiIntent
}
