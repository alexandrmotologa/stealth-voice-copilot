# System Architecture Specification

## 1. Pipeline Overview

Stealth Voice Copilot operates as a self-contained real-time audio analysis and response generation pipeline. The system processes streaming audio through four modular stages:

1. **Audio Ingestion and Conditioning**: Captures mono 16kHz PCM audio and applies hardware-level echo cancellation and noise filtering.
2. **Speech Segmentation**: Evaluates voice activity in 32ms frames using Silero VAD v5 to identify boundaries between active speech and pauses.
3. **Speech-to-Text Transcription**: Processes speech segments with Sherpa-ONNX Whisper models and normalizes domain-specific logistics terms.
4. **Context Assembly and Inference**: Constructs a prompt containing the conversation state and executes local inference through MediaPipe GenAI or the built-in deterministic expert engine.
5. **Stealth Presentation**: Renders the generated response on a true black AMOLED lockscreen window or a discreet floating HUD overlay.

```
                    +-----------------------------+
                    |  Microphone / AudioRecord   |
                    +--------------+--------------+
                                   | 16kHz PCM Frames
                                   v
                    +-----------------------------+
                    | Acoustic Conditioning (AEC) |
                    +--------------+--------------+
                                   | Clean PCM
                                   v
                    +-----------------------------+
                    |     Silero VAD (ONNX)       |
                    +--------------+--------------+
                                   | Speech Segment (FloatArray)
                                   v
                    +-----------------------------+
                    |     Sherpa-ONNX STT         |
                    +--------------+--------------+
                                   | Raw Transcript
                                   v
                    +-----------------------------+
                    | Freight Lexicon Normalizer  |
                    +--------------+--------------+
                                   | Clean Transcript
                                   v
                    +-----------------------------+
                    | Hybrid Inference Coordinator|
                    +-------+-------------+-------+
                            |             |
           (Model Resident) |             | (Instant Fallback)
                            v             v
             +------------------+     +-------------------+
             | MediaPipe GenAI  |     | Freight Knowledge |
             | Llama-3.2-1B-Q4  |     | Deterministic     |
             +--------+---------+     +---------+---------+
                      |                         |
                      +------------+------------+
                                   | CopilotTurn
                                   v
                    +-----------------------------+
                    |   Stealth MVI StateFlow     |
                    +--------------+--------------+
                                   |
                   +---------------+---------------+
                   |                               |
                   v                               v
       +-----------------------+       +-----------------------+
       | StealthActivity       |       | StealthOverlayService |
       | (Lockscreen AMOLED)   |       | (Floating Mini HUD)   |
       +-----------------------+       +-----------------------+
```

## 2. Audio Subsystem Design

### 2.1 Audio Record Configuration
* Sample Rate: 16,000 Hz
* Channels: Single channel mono (`AudioFormat.CHANNEL_IN_MONO`)
* Encoding: 16-bit linear PCM (`AudioFormat.ENCODING_PCM_16BIT`)
* Frame Size: 512 samples (32 milliseconds per evaluation window)
* Audio Source: `MediaRecorder.AudioSource.VOICE_RECOGNITION`

Using `VOICE_RECOGNITION` prompts the Android audio framework to configure beamforming and noise filtering optimized for speech intelligibility.

### 2.2 Noise and Echo Cancellation
On devices with hardware audio preprocessing capabilities (such as the Google Tensor SoC on Pixel 6 Pro), the engine binds:
* `android.media.audiofx.AcousticEchoCanceler`
* `android.media.audiofx.NoiseSuppressor`

Both processors attach directly to the active `audioSessionId`. If hardware effects are unavailable on a given Android target, the pipeline falls back gracefully to software dynamic range normalization.

### 2.3 Audio Test Bench
For testing in simulated environments, the `AudioTestBench` provides an alternate audio source implementing the same `AudioSource` interface. It injects pre-recorded or synthetic 16kHz PCM audio buffers into the pipeline, allowing full automated test verification without requiring a live speaker.

## 3. Voice Activity Detection (Silero VAD)

### 3.1 Model and Execution
The VAD module runs `silero_vad.onnx` v5 inside the ONNX Runtime for Android (`com.microsoft.onnxruntime:onnxruntime-android`). The model takes 512 samples per frame alongside hidden recurrent state tensors (`h` and `c`) and outputs a speech probability scalar between 0.0 and 1.0.

### 3.2 State Machine
* **Silence to Speech Trigger**: When speech probability exceeds 0.50 for two consecutive frames, state transitions to `SPEECH_ACTIVE`.
* **Speech to Silence Release**: When speech probability drops below 0.35 continuously for 800 milliseconds, state transitions to `SPEECH_ENDED`.
* **Buffer Flush**: The accumulated PCM frame buffer is extracted, converted to normalized IEEE 754 32-bit floats (`-1.0f` to `1.0f`), and dispatched to the STT worker.
* **Energy Detector Fallback**: In the event that the ONNX model fails to load, a Root Mean Square (RMS) threshold energy detector operates automatically to avoid halting the service.

## 4. Speech-to-Text Subsystem (Sherpa-ONNX)

### 4.1 Engine Architecture
Transcription uses the Sherpa-ONNX offline recognizer:
* Whisper Base English model quantized with int8 weights (`sherpa-onnx-whisper-base.en`).
* Fallback to streaming Zipformer for lower latency when available.

### 4.2 Lexicon Normalization
Acoustic models occasionally mishear specialized freight industry jargon. The `FreightLexiconNormalizer` scans raw transcripts and performs regex and phonetic corrections:
* "rate con" / "ray con" / "rate calm" -> `Rate Confirmation`
* "b o l" / "bowl" / "be o el" -> `BOL`
* "p o d" / "pee oh dee" -> `POD`
* "tony" / "toe new" / "t o n u" -> `TONU`
* "h o s" / "hours of service" -> `HOS`
* "lumbar fee" / "lumber receipt" -> `Lumper fee`
* "noa" / "notice of assignment" -> `Notice of Assignment`

## 5. Inference and Knowledge Layer

### 5.1 Hybrid Dual-Engine Architecture
To ensure continuous operation regardless of memory constraints or cold model loads, the inference subsystem coordinates two distinct engines:

1. **Local Neural Engine (`LocalLlmEngine`)**:
   * Uses Google AI Edge MediaPipe Tasks GenAI (`Llama-3.2-1B-Instruct` or `Gemma-2-2B-IT`).
   * Emits tokens asynchronously into a `SharedFlow<String>`.
   * Enforces strict structured JSON output parsing.

2. **Deterministic Logistics Engine (`FreightKnowledgeEngine`)**:
   * A zero-latency, rule-based expert system containing over 100 question categories and standard professional answers.
   * Calculates similarity scores based on Jaccard token overlap and keyword density.
   * Operates instantly (sub-5ms) without allocating GPU or NPU resources.

### 5.2 Token Budget Optimization
When Romanian translations are disabled by the user:
* The system prompt strips Romanian translation instructions.
* The expected output drops from ~80 tokens to ~35 tokens.
* Inference latency on the Pixel 6 Pro drops to 1.0 to 1.5 seconds.

## 6. User Interface and Presentation

### 6.1 StealthActivity (Lockscreen Display)
* Runs with `setShowWhenLocked(true)` and `setTurnScreenOn(false)`.
* AMOLED True Black theme (`#000000`) turns off all OLED pixels except active glyphs.
* Typography is tuned for minimal ocular light reflection:
  * English answers: `#D0D0D0`
  * Interviewer questions: `#666666`
  * Romanian explanations: `#888870`

### 6.2 StealthOverlayService (Floating System HUD)
* Utilizes `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`.
* Renders a movable, single-line text view that sits over meeting apps (Zoom, Teams, Google Meet).
* Supports single-tap minimize and long-press emergency dismissal.

### 6.3 Physical Hardware Overrides
* `Volume Up` double click: Dispatches `StealthUiIntent.ClearHistory`, clearing all dialogue context.
* `Volume Down` long press: Dispatches `StealthUiIntent.ToggleRomanian`, instantly toggling translation.
* `Auto-Standby Watchdog`: Pauses audio engine and releases locks after 180 seconds of silence.
