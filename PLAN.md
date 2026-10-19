# Stealth Voice Copilot: Technical Specification and Implementation Plan

An offline, on-device conversational assistant for Android targeting the Google Pixel 6 Pro, built for real-time interview assistance in US freight dispatching, logistics coordination, and freight billing.

## 1. Executive Summary and Hardware Target

### 1.1 Purpose
This application functions as a discreet, ambient copilot during spoken English interviews. It continuously monitors interviewer speech, segments speech using on-device Voice Activity Detection (VAD), transcribes English speech with local Speech-to-Text (STT), matches domain terminology, generates concise suggested answers, and provides optional Romanian translations for fast comprehension. The complete pipeline executes locally on-device without internet access, third-party API dependencies, or external data transmission.

### 1.2 Target Hardware Profile
* Device: Google Pixel 6 Pro
* SoC: Google Tensor G1 (2x Cortex-X1 at 2.80 GHz, 2x Cortex-A76 at 2.25 GHz, 4x Cortex-A55 at 1.80 GHz)
* GPU and NPU: ARM Mali-G78 MP20 with Google EdgeTPU
* Memory: 12 GB LPDDR5 RAM (sufficient to keep quantized models resident in memory without system eviction)
* Target Android Version: Android 14 and Android 15 (API level 34+)

## 2. System Architecture and Data Flow

```
[Interviewer Speaks]
       |
       v
[Microphone AudioRecord / AudioTestBench] (16kHz, 16-bit Mono PCM)
       |
       +--> [Hardware Audio Preprocessing: AEC + Noise Suppressor]
       |
       v
[Voice Activity Detection: Silero VAD v5 / Energy Fallback]
       |
       | (Silence > 800ms) -> Flushes buffer & triggers STT
       v
[Speech-to-Text: Sherpa-ONNX Whisper Base EN / Zipformer / Lexicon Matcher]
       |
       v Transcript String
[Prompt and Context Assembler: Freight Knowledge Base + Last 2 Turns]
       |
       v Context Token Stream
[Inference Engine: Local LLM (MediaPipe Tasks GenAI) / Deterministic Logistics Engine]
       |
       v Structured Output (JSON / Turn Model)
[Stealth AMOLED Lockscreen Activity / Floating System HUD Bubble]
       |-- Interlocutor Transcript (EN)
       |-- Translation (RO) [Hidden when Romanian toggle is OFF]
       |-- Recommended Professional Response (EN)
       \-- Response Explanation (RO) [Hidden when Romanian toggle is OFF]
```

## 3. Power Management and Covert Trigger System

Continuous microphone streaming and local neural network execution require strict power and covertness controls:

### 3.1 Quick Settings Tile (`CopilotTileService`)
Accessible by pulling down the Quick Settings shade directly from the lock screen. A single tap switches between listening and standby states without unlocking the device.

### 3.2 Pixel Quick Tap (Back Tap)
Mapped through Android Accessibility and system gestures to launch or bring `StealthActivity` to the foreground when double-tapping the rear glass of the Pixel 6 Pro.

### 3.3 Hardware Key Listeners
* Volume Up double-click: Clears dialogue history and resets conversational context immediately.
* Volume Down long-press: Toggles Romanian translation on or off without screen touch.

### 3.4 Auto-Standby Watchdog
If the VAD module detects no human speech for 180 consecutive seconds, the audio engine transitions to low-power standby mode, pausing neural evaluation and releasing the high-frequency CPU lock.

### 3.5 Discreet Haptic Signals
Haptic feedback patterns communicate system states through pocket vibration without requiring screen verification:
* Single subtle pulse: speech detected and recording.
* Double soft pulse: answer generated and displayed.
* Triple short pulse: dialogue context wiped.

## 4. Subsystem Specifications

### 4.1 Audio Capture and Noise Suppression
* Package: `com.copilot.stealth.engine.audio`
* Configuration:
  * Sample Rate: 16,000 Hz
  * Channel: `AudioFormat.CHANNEL_IN_MONO`
  * Encoding: `AudioFormat.ENCODING_PCM_16BIT`
  * Buffer Size: 1024 samples per frame
* Hardware Preprocessing: Attaches `NoiseSuppressor` and `AcousticEchoCanceler` to the active `audioSessionId` when available on the device.
* Audio Injector: `AudioTestBench` provides a mock input stream for continuous integration, deterministic unit testing, and benchmarking without relying on physical voice input.

### 4.2 Voice Activity Detection (Silero VAD)
* Package: `com.copilot.stealth.engine.audio.SileroVadDetector`
* Library: `com.microsoft.onnxruntime:onnxruntime-android`
* Model: `silero_vad.onnx` v5
* Buffer Logic:
  * Feeds 512-sample PCM float frames to the model.
  * Active speech threshold: probability > 0.50.
  * Speech release trigger: probability < 0.35 maintained for 800 milliseconds.
  * Automatic fallback: Root Mean Square (RMS) energy detector if the ONNX model asset is unavailable or during cold boot.

### 4.3 Speech-to-Text Engine (Sherpa-ONNX)
* Package: `com.copilot.stealth.engine.stt`
* Library: `com.k2fsa.sherpa.onnx:sherpa-onnx`
* Models: `sherpa-onnx-whisper-base.en` and streaming `zipformer-en-2023-06-26`.
* Latency target on Tensor G1: 250ms to 450ms for a 4-second audio slice.
* Acoustic Phonetic and Lexicon Matcher: Post-processes transcriptions against freight logistics terminology (Rate Con, BOL, POD, TONU, HOS, Lumper, Motive) to fix domain-specific misspellings.

### 4.4 Hybrid On-Device Inference Engine
* Package: `com.copilot.stealth.engine.llm`
* Dual Backend Strategy:
  1. `LocalLlmEngine`: Uses Google AI Edge MediaPipe GenAI LLM API (`com.google.mediapipe:tasks-genai`) running quantized `Llama-3.2-1B-Instruct` or `Gemma-2-2B-IT`.
  2. `FreightInferenceEngine`: Fast, deterministic expert system preloaded with 100+ standard freight dispatch and billing interview scenarios. Provides instant responses (under 5 milliseconds) when large weights are loading or when maximum battery conservation is active.
* Generation Settings:
  * Max tokens: 96
  * Temperature: 0.2
  * Top-K: 20
* Romanian Token Suppression:
  * When Romanian mode is disabled, prompt guidelines instruct the engine to omit Romanian fields completely.
  * Reduces generation from ~80 tokens to ~35 tokens, cutting total response time to 1.0 to 1.5 seconds.

### 4.5 Domain Knowledge and System Prompt
The system persona simulates a senior US freight dispatcher and billing specialist:

```text
You are an offline interview assistant for a US Freight Dispatcher and Billing Clerk position.
Domain terms: Load boards (DAT One, Truckstop), Rate Confirmation (Rate Con), Bill of Lading (BOL), Proof of Delivery (POD), Factoring (Notice of Assignment), Detention ($50/hr after 2h standard), Layover ($150 to $300/day), TONU ($150 to $250), Lumper receipts, HOS (Hours of Service), ELD (KeepTruckin/Motive).

Input format:
Interviewer: "<transcript>"
Settings: [Romanian: <true|false>]

Rules:
1. Provide a direct, professional 1 to 2 sentence English response that the candidate can read aloud immediately.
2. If Romanian is true, include the question translation and answer explanation.
3. Strict JSON format:
{
  "transcript_ro": "<translation or empty>",
  "answer_en": "<1-2 concise professional sentences>",
  "answer_ro": "<brief meaning or empty>"
}
```

### 4.6 Stealth AMOLED Lockscreen UI and Floating HUD
* Package: `com.copilot.stealth.ui`
* Primary View: `StealthActivity`
  * True black background (`#000000`) designed for AMOLED power savings and zero backlight bleed.
  * System flags: `setShowWhenLocked(true)`, `setTurnScreenOn(false)`, `FLAG_KEEP_SCREEN_ON`.
  * High-legibility contrast typography: off-white (`#D0D0D0`) for English answers, muted gray (`#666666`) for interviewer transcripts, olive gray (`#888870`) for Romanian translations.
* Secondary View: `StealthOverlayService`
  * Floating HUD using `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`.
  * Displays a single discreet line of suggested speech over any active application (such as Google Meet or Zoom) with an emergency collapse gesture.

## 5. MVI Architecture and State Machine

```kotlin
data class CopilotTurn(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val transcriptEn: String,
    val transcriptRo: String? = null,
    val answerEn: String,
    val answerRo: String? = null,
    val confidence: Float = 1.0f,
    val latencyMs: Long = 0L
)

data class StealthUiState(
    val isListening: Boolean = false,
    val isRomanianEnabled: Boolean = true,
    val isProcessing: Boolean = false,
    val overlayMode: OverlayMode = OverlayMode.LOCKSCREEN,
    val currentTurns: List<CopilotTurn> = emptyList(),
    val errorMessage: String? = null,
    val batterySaverMode: Boolean = false,
    val activeEngineType: EngineType = EngineType.HYBRID
)

sealed interface StealthUiIntent {
    data object ToggleListening : StealthUiIntent
    data object ToggleRomanian : StealthUiIntent
    data object ClearHistory : StealthUiIntent
    data object ToggleOverlayMode : StealthUiIntent
    data class OnSpeechDetected(val audioBuffer: FloatArray) : StealthUiIntent
    data class OnManualQuery(val query: String) : StealthUiIntent
    data class SetAudioSource(val source: AudioInputSource) : StealthUiIntent
}
```

## 6. Project Directory Tree

```
stealth-voice-copilot/
|-- PLAN.md
|-- README.md
|-- build.gradle.kts
|-- settings.gradle.kts
|-- gradle.properties
|-- gradlew
|-- gradlew.bat
|-- gradle/
|   \-- wrapper/
|       |-- gradle-wrapper.jar
|       \-- gradle-wrapper.properties
|-- docs/
|   |-- ARCHITECTURE.md
|   |-- FREIGHT_LOGISTICS_GUIDE.md
|   |-- HARDWARE_SETUP.md
|   |-- TESTING_AND_VERIFICATION.md
|   \-- images/
|       \-- stealth_voice_copilot_demo.gif
\-- app/
    |-- build.gradle.kts
    |-- proguard-rules.pro
    \-- src/
        |-- main/
        |   |-- AndroidManifest.xml
        |   |-- assets/
        |   |   |-- models/
        |   |   |   |-- silero_vad.onnx
        |   |   |   \-- README.md
        |   |   \-- prompts/
        |   |       \-- dispatch_persona.txt
        |   |-- java/
        |   |   \-- com/
        |   |       \-- copilot/
        |   |           \-- stealth/
        |   |               |-- StealthApplication.kt
        |   |               |-- data/
        |   |               |   |-- models/
        |   |               |   |   |-- CopilotTurn.kt
        |   |               |   |   |-- StealthUiState.kt
        |   |               |   |   \-- StealthUiIntent.kt
        |   |               |   \-- repository/
        |   |               |       |-- FreightGlossary.kt
        |   |               |       \-- FreightKnowledgeEngine.kt
        |   |               |-- engine/
        |   |               |   |-- audio/
        |   |               |   |   |-- AudioRecorder.kt
        |   |               |   |   |-- AudioTestBench.kt
        |   |               |   |   |-- DiscreetHapticHelper.kt
        |   |               |   |   \-- SileroVadDetector.kt
        |   |               |   |-- stt/
        |   |               |   |   |-- SherpaSpeechToText.kt
        |   |               |   |   \-- FreightLexiconNormalizer.kt
        |   |               |   \-- llm/
        |   |               |       |-- LocalLlmEngine.kt
        |   |               |       |-- PromptBuilder.kt
        |   |               |       \-- StructuredResponseParser.kt
        |   |               |-- service/
        |   |               |   |-- CopilotForegroundService.kt
        |   |               |   |-- CopilotTileService.kt
        |   |               |   \-- StealthOverlayService.kt
        |   |               \-- ui/
        |   |                   |-- StealthActivity.kt
        |   |                   |-- StealthViewModel.kt
        |   |                   |-- components/
        |   |                   |   |-- TurnCard.kt
        |   |                   |   |-- TopStatusBar.kt
        |   |                   |   |-- ControlsRow.kt
        |   |                   |   \-- QuickScenarioSelector.kt
        |   |                   \-- theme/
        |   |                       |-- Color.kt
        |   |                       |-- Theme.kt
        |   |                       \-- Type.kt
        |   \-- res/
        |       |-- drawable/
        |       |   |-- ic_tile_copilot.xml
        |       |   \-- ic_launcher_foreground.xml
        |       \-- values/
        |           |-- strings.xml
        |           \-- themes.xml
        \-- test/
            \-- java/
                \-- com/
                    \-- copilot/
                        \-- stealth/
                            |-- FreightKnowledgeEngineTest.kt
                            |-- StructuredResponseParserTest.kt
                            |-- SileroVadDetectorTest.kt
                            \-- StealthViewModelTest.kt
```

## 7. Implementation Roadmap and Milestones

### Phase 1: Project Skeleton, Gradle Configuration, and Documentation
* Task 1.1: Configure root `build.gradle.kts`, `settings.gradle.kts`, and `app/build.gradle.kts` using Kotlin 2.0.0 and AGP 8.5.2.
* Task 1.2: Set up `AndroidManifest.xml` with audio recording, foreground service, overlay, and lockscreen flags.
* Task 1.3: Author comprehensive documentation in `docs/`: architecture overview, logistics guide, hardware setup, and testing protocols.
* Task 1.4: Initialize git repository, configure GitHub repository with strict topic metadata, and execute portfolio synchronization Phase 1.

### Phase 2: Domain Knowledge Base and Deterministic Logistics Engine
* Task 2.1: Implement `FreightGlossary` containing US dispatch definitions (Rate Con, Detention, Layover, TONU, HOS, ELD, Factoring, Lumper, Claims, Accessorials).
* Task 2.2: Implement `FreightKnowledgeEngine` providing deterministic question matching, confidence scoring, English answer synthesis, and Romanian explanation mappings.
* Task 2.3: Implement `StructuredResponseParser` with schema validation and error fallback.
* Task 2.4: Write comprehensive unit tests verifying prompt assembly, response parsing, and domain matching.

### Phase 3: Audio Capture, Noise Suppression, and VAD Engine
* Task 3.1: Implement `AudioRecorder` utilizing `AudioRecord` with 16kHz mono PCM buffer streaming.
* Task 3.2: Add hardware noise suppression (`NoiseSuppressor`) and acoustic echo cancellation (`AcousticEchoCanceler`).
* Task 3.3: Implement `SileroVadDetector` with ONNX Runtime session handling and RMS energy fallback.
* Task 3.4: Implement `AudioTestBench` for deterministic PCM test audio injection.
* Task 3.5: Implement `DiscreetHapticHelper` for pocket vibration state indicators.

### Phase 4: Speech-to-Text and Lexicon Normalization
* Task 4.1: Implement `SherpaSpeechToText` interface and ONNX model loader.
* Task 4.2: Implement `FreightLexiconNormalizer` to repair phonetic transcript anomalies on US freight jargon.
* Task 4.3: Connect VAD silence detection callbacks directly into STT ingestion pipeline.

### Phase 5: Local LLM Engine and Hybrid Inference Coordinator
* Task 5.1: Implement `LocalLlmEngine` integration using MediaPipe GenAI / ONNX runtime.
* Task 5.2: Implement bilingual suppression logic to cut token output by 60% when Romanian is disabled.
* Task 5.3: Implement hybrid inference coordinator that routes queries to the local LLM when ready, falling back to the instant deterministic engine.

### Phase 6: AMOLED Stealth Lockscreen UI, Tile Service, and HUD
* Task 6.1: Build `StealthActivity` in Jetpack Compose with pure black AMOLED theme (`#000000`).
* Task 6.2: Implement `CopilotTileService` for Quick Settings toggle on Android lockscreen.
* Task 6.3: Implement `StealthOverlayService` for floating system HUD support.
* Task 6.4: Implement hardware key listeners (Volume Up double-tap wipe, Volume Down translation toggle).
* Task 6.5: Implement 180-second silence watchdog auto-standby.

### Phase 7: Verification, Asset Generation, and Launch
* Task 7.1: Execute unit tests across parser, knowledge engine, VAD, and ViewModel.
* Task 7.2: Build debug APK with `./gradlew assembleDebug`.
* Task 7.3: Generate authentic UI screenshots and animated demo GIF (`docs/images/stealth_voice_copilot_demo.gif`).
* Task 7.4: Execute portfolio synchronization Phase 2 and publish updates.

## 8. Freight Logistics Knowledge Base Reference

| Term | Context | Standard Response Pattern |
| :--- | :--- | :--- |
| Rate Confirmation | Booking contract with broker | I verify load rate, pickup and delivery windows, weight, commodity, and detention policy before signing. |
| Detention | Waiting at shipper or receiver | Standard is 2 free hours. I notify the broker at 1.5 hours and get the BOL timestamped upon entry and exit. |
| TONU | Truck Ordered Not Used | If the load cancels after the driver is dispatched, I bill the standard TONU fee of $150 to $250. |
| Factoring / NOA | Invoicing payment company | We submit the rate con, signed BOL or POD, and lumper receipts to our factoring company for same-day funding. |
| Layover | Overnight delay caused by facility | If the receiver cannot unload until the next day, I negotiate a $200 to $300 layover fee plus hotel accommodations if required. |
| Lumper Fee | Third-party unloading fee | I request an advance from the broker or have the driver pay and obtain an official receipt for immediate reimbursement. |
| Hours of Service (HOS) | DOT driver log limits | Drivers can drive up to 11 hours within a 14-hour on-duty window, requiring a 10-hour consecutive rest break. |
| Bill of Lading (BOL) | Legal shipping contract | The driver inspects piece count and condition at pickup, signs the BOL, and keeps a clear copy before leaving. |
