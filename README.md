<div align="center">

# Stealth Voice Copilot

**Offline on-device Android assistant for US freight dispatching, logistics, and billing interviews.**

[![Android](https://img.shields.io/badge/Platform-Android_14+-3DDC84?logo=android&logoColor=white)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.0-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-AMOLED_True_Black-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![ONNX](https://img.shields.io/badge/ONNX_Runtime-Silero_VAD-005CED?logo=onnx&logoColor=white)](https://onnxruntime.ai)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

<br/>

<img src="docs/images/stealth_voice_copilot_demo.gif" width="420" alt="Stealth Voice Copilot Demo" />

</div>

## Overview

Stealth Voice Copilot is an offline Android application engineered for the Google Pixel 6 Pro. It listens to interviewer questions during US freight dispatching and billing interviews, transcribes spoken English locally, extracts freight domain concepts, and displays concise professional answers in English with optional Romanian translations.

The entire processing pipeline runs strictly on-device without internet access, third-party cloud APIs, or outbound network calls.

## Core Capabilities

* **On-Device Speech Pipeline:** Combines Silero VAD v5 with Sherpa-ONNX Whisper and streaming models for rapid transcription directly on the Tensor G1 processor.
* **Hybrid Inference Coordinator:** Employs an on-device quantized LLM (Llama 3.2 1B or Gemma 2 2B via MediaPipe GenAI) backed by a deterministic logistics knowledge engine for sub-5ms fallback and high reliability.
* **Carrier Profile Grounding:** Injects real candidate carrier authority data (MC number, USDOT, fleet capacity, equipment types, factoring partner, and baseline accessorial rates) directly into dialogue answers.
* **Real-Time Freight Math Engine:** Live on-screen calculator (`FreightMathBar`) computing billable detention hours, gross and net RPM, and FMCSA HOS feasibility windows with one-tap quote insertion.
* **Covert Glance Syntax Highlighting:** Formats answers with peripheral vision cues highlighting monetary amounts in green, legal freight acronyms in cyan, and critical time limits in amber.
* **Zero-Light Covert Privacy Themes:** Supports AMOLED Standard, Night Vision Red, and Amber Diffuse display modes to eliminate light reflection in dark or low-visibility interview environments.
* **Bluetooth Audio Route Detection:** Continuously detects covert wireless earpieces (Pixel Buds, BLE headsets) versus internal phone microphone routing with on-screen route confirmation.
* **Internal Audio and VoIP In-Call Capture:** Supports capturing internal speakerphone or conference calls via `AudioPlaybackCaptureConfiguration` and `VOICE_COMMUNICATION` capture streams.
* **Encrypted Transcripts and Panic Wipe:** Exports full question-and-answer transcripts secured by AES-256-GCM encryption, while a rapid Volume Up double-click triggers an emergency panic wipe of RAM, disk cache, and clipboard.
* **AMOLED True Black Overlay:** Renders directly over the lockscreen without requiring device unlock, using `#000000` background to minimize battery drain and reduce display illumination.
* **Floating System HUD:** Provides a secondary discreet overlay mode that displays a single line of suggested speech over conference apps such as Zoom or Google Meet.
* **Covert Physical Controls:** Volume Up double-tap initiates panic wipe; Volume Down long-press toggles Romanian translations; Quick Settings tile enables one-tap activation from the lockscreen shade.
* **Acoustic Preprocessing:** Leverages hardware-backed Acoustic Echo Cancellation (AEC) and Noise Suppression to filter HVAC hum and speaker echo.
* **US Freight Domain Grounding:** Preloaded with industry practices covering Rate Confirmations, Bill of Lading (BOL), Proof of Delivery (POD), Factoring NOAs, Detention calculations, Layover fees, and TONU claims.

## System Architecture

```
[Interviewer Audio]
       |
       v
[AudioRecord 16kHz PCM] ---> [AEC & Hardware Noise Suppression]
       |
       v
[Silero VAD v5 Engine]
       |
       | (Speech completed, silence > 800ms)
       v
[Sherpa-ONNX STT Engine]
       |
       v English Transcript
[Freight Lexicon Normalizer & Prompt Assembler]
       |
       v
[Hybrid Inference Engine]
       |-- Local MediaPipe LLM (Llama 3.2 1B / Gemma 2B)
       \-- Deterministic Freight Knowledge Engine (instant fallback)
       |
       v Structured Result
[AMOLED Lockscreen Activity / Floating HUD]
       |-- Interlocutor Question (EN)
       |-- Question Translation (RO) [Optional]
       |-- Recommended Response (EN)
       \-- Concept Notes (RO) [Optional]
```

## Hardware and System Requirements

* **Primary Device:** Google Pixel 6 Pro (Google Tensor G1, 12 GB RAM)
* **Minimum Android Version:** Android 14 (API level 34)
* **Target Android Version:** Android 15 (API level 35) or Android 16 (API level 36)
* **Required Permissions:**
  * `RECORD_AUDIO`: Continuous local PCM stream capture
  * `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_MICROPHONE`: Background audio session persistence
  * `SYSTEM_ALERT_WINDOW`: Floating HUD overlay across applications
  * `WAKE_LOCK`: Retaining CPU inference states during active speech

## Project Structure

```
stealth-voice-copilot/
├── app/
│   ├── src/main/
│   │   ├── java/com/copilot/stealth/
│   │   │   ├── data/            # MVI models, state machine, freight glossary
│   │   │   ├── engine/audio/    # AudioRecord, Silero VAD, AudioTestBench
│   │   │   ├── engine/stt/      # Sherpa-ONNX STT and lexicon normalizer
│   │   │   ├── engine/llm/      # MediaPipe LLM and response parsers
│   │   │   ├── service/         # Foreground and Quick Settings tile services
│   │   │   └── ui/              # Jetpack Compose AMOLED lockscreen and HUD
│   │   └── res/                 # Vector drawables, themes, strings
│   └── src/test/                # Unit tests for domain logic and parsers
├── docs/                        # Architecture, hardware setup, logistics guide
├── PLAN.md                      # Detailed technical specification
└── README.md                    # Project overview and quick start
```

## Building and Verification

### Prerequisites
* JDK 21
* Android SDK (API 34+)
* CMake and Android NDK (for native ONNX components)

### Build Commands
```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew testDebugUnitTest

# Install to connected Pixel 6 Pro
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Documentation

* [Architecture Specification](docs/ARCHITECTURE.md): Detailed subsystem contracts and signal flows.
* [Freight Logistics Knowledge Guide](docs/FREIGHT_LOGISTICS_GUIDE.md): Reference data for freight dispatch terminology and standard answers.
* [Hardware and Gestures Setup](docs/HARDWARE_SETUP.md): Step-by-step setup for Pixel Quick Tap, Quick Settings Tile, and lockscreen overlay.
* [Testing and Verification Protocol](docs/TESTING_AND_VERIFICATION.md): Audio bench testing, latency benchmarks, and scenario evaluations.

## License

MIT License. See [LICENSE](LICENSE) for details.
