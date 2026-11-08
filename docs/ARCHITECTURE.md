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

## 4. Speech-to-Text Subsystem

### 4.1 Dual-Channel Engine Architecture
Speech recognition operates through `AndroidOnDeviceSpeechToText`, routing audio across two distinct modes to maximize accuracy and eliminate acoustic confusion:

1. **RO➔EN (Romanian to English Mode):**
   * Configures intent parameters with `RecognizerIntent.EXTRA_LANGUAGE = "ro-RO"`.
   * Directly invokes `SpeechRecognizer.createSpeechRecognizer(context)`, connecting to Google Speech Services cloud acoustic models. This bypasses vendor ROM offline package limitations where non-Pixel devices lack pre-downloaded Romanian neural acoustic models.
   * Transcribes Romanian utterances with high grammatical fidelity, forwarding raw text to the normalizer and LLM translation pipeline.

2. **EN ONLY (English Direct Mode):**
   * Configures intent parameters with `RecognizerIntent.EXTRA_LANGUAGE = "en-US"`.
   * On Android 12+ devices with pre-installed English models, leverages `SpeechRecognizer.createOnDeviceSpeechRecognizer(context)` for ultra-low latency on-device transcription, with automatic fallback to standard recognition.
   * Strips secondary language flags, isolating the acoustic decoder to English phonemes and preventing cross-lingual false friend substitutions.

3. **Fast Language Switching:**
   * Switching between `RO➔EN` and `EN ONLY` via the top status bar pill executes within 50 milliseconds.
   * Recognizer instances are destroyed and re-initialized cleanly on the main application Handler, avoiding Audio HAL driver stops or synchronous binder deadlocks.

### 4.2 Phonetic De-Bleed and Lexicon Normalization
Acoustic decoders listening in multilingual or non-native phonetic spaces occasionally suffer from cross-lingual acoustic bleed or mishear industry acronyms. The `FreightLexiconNormalizer` executes sequential regex replacements in under 1 millisecond:

* **Cross-Lingual Phonetic De-Bleeding:**
  * `hai (how are|how's|what|who|when|where|there|guys|team)` -> `hi $1`
  * `uai / oai (you|did|is|are|do|would|should|could)` -> `why $1`
  * `helau` -> `hello`
  * `pliz` -> `please`
  * `tenx / thenx` -> `thanks`
* **Freight and Logistics Acronyms:**
  * "rate con" / "ray con" / "rate calm" -> `Rate Confirmation`
  * "b o l" / "bowl" / "be o el" -> `BOL`
  * "p o d" / "pee oh dee" -> `POD`
  * "tony" / "toe new" / "t o n u" -> `TONU`
  * "h o s" / "hours of service" -> `HOS`
  * "lumbar fee" / "lumber receipt" -> `Lumper fee`
  * "noa" / "notice of assignment" -> `Notice of Assignment`

## 5. Inference and Knowledge Layer

### 5.1 Multi-Tier Hierarchical Inference Engine
To guarantee zero failure during live interviews, the inference subsystem coordinates a prioritized fallback chain:

1. **Google Gemini (Priority 1):**
   * Targets `gemini-2.5-flash` or `gemini-2.0-flash` through Google AI Studio REST endpoints.
   * Enforces strict structured JSON output via `responseMimeType: "application/json"`.
   * Implements automated dual API key failover: if the primary key exceeds rate limits or fails, the client instantly retries with the backup key.

2. **Groq Cloud LPU (Fallback 1):**
   * Ultra-fast LPUs delivering sub-250ms time-to-first-token using `llama-3.3-70b-versatile` or `llama-3.1-8b-instant`.
   * Activated automatically when Gemini returns network timeouts or HTTP errors.

3. **Gemini Alternative Flash Retry (Fallback 2):**
   * Secondary retry attempting lighter Flash model variants before delegating to local device silicon.

4. **Local Neural Model (Fallback 3 / Offline Mode):**
   * Uses Google AI Edge MediaPipe Tasks GenAI running quantized `Llama-3.2-1B-Instruct` or `Gemma-2-2B-IT` locally on Google Tensor G1 NPU/GPU.
   * If the device is offline, network queries are bypassed immediately, executing local inference with zero timeout latency.

5. **Deterministic Knowledge Engine (Instant Emergency Fallback):**
   * Rule-based expert system containing preloaded question categories and verified answers, operating in under 5ms without neural allocations.

### 5.2 Context Assembly and ASR Phonetic Reconstruction
The `PromptBuilder` constructs prompt contexts tailored to the active profile mode:
* **Candidate Technical Interview:** Grounds persona as a Senior Software and AI Systems Engineer, incorporating candidate context, target company, target role, and job description requirements.
* **General Conversational:** Unconstrained assistant answering freely on any topic.
* **Carrier Fleet Dispatch:** Preloaded with commercial fleet credentials, equipment types, and standard accessorial rates.
* **ASR Phonetic Reconstruction Protocol:** Instructs the LLM to inspect the incoming utterance for speech recognition slips (such as non-native accents or cross-lingual phonetic bleed), reconstruct the true intended question in `transcript_en` and `transcript_ro`, and format the teleprompter output accordingly.
* **Prompter Styles:** Supports `ANCHOR_BULLETS` (conversational hook plus 2 to 3 bold bullet points) and `FULL_SENTENCE` (fluent verbatim read-aloud text).

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
