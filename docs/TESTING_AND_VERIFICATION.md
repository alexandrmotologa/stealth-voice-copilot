# Testing and Verification Protocol

Protocols and benchmarks for validating the audio, transcription, inference, and UI subsystems in Stealth Voice Copilot.

## 1. Automated Unit Testing

The test suite covers prompt generation, structured JSON parsing, deterministic freight knowledge matching, Silero VAD state machine logic, and MVI ViewModel flows.

### Running Unit Tests
```bash
# Run all unit tests
./gradlew testDebugUnitTest

# Run specific domain test
./gradlew testDebugUnitTest --tests "com.copilot.stealth.FreightKnowledgeEngineTest"
```

### Key Test Suites
* `FreightKnowledgeEngineTest`: Verifies domain keyword scoring, standard responses, and Romanian explanations across interview prompts.
* `StructuredResponseParserTest`: Validates JSON schema parsing, edge case recovery from truncated LLM tokens, and fallback mechanisms.
* `SileroVadDetectorTest`: Tests frame energy calculation, speech probability threshold transitions, and silence release timing.
* `StealthViewModelTest`: Verifies MVI state flows, audio intent handling, history clearing, and Romanian toggle events.

## 2. Audio Pipeline Verification with AudioTestBench

Physical microphone testing is supplemented by `AudioTestBench`, which injects deterministic 16kHz PCM audio buffers directly into the VAD and transcription engine.

### Synthetic Test Vectors
* **Silence Vector**: 3 seconds of zeros (`0.0f`). Asserts that VAD state remains `SILENCE` and STT is not triggered.
* **Tonal Speech Mock**: 2 seconds of 440 Hz sinusoidal waveform modulated with envelope pauses. Asserts speech activation and subsequent silence release trigger after 800ms.
* **Pre-Recorded Logistics Questions**: 16kHz WAV test clips containing spoken interview questions (such as "What is your policy for driver detention?"). Asserts transcription accuracy and correct answer generation.

## 3. End-to-End Latency Benchmarking

Benchmarks measured on Google Pixel 6 Pro (Android 14, 12 GB RAM):

| Pipeline Stage | Target Latency | Observed Tensor G1 Range |
| :--- | :--- | :--- |
| Audio Frame Capture (512 samples) | 32.0 ms | 32.0 ms |
| Silero VAD Frame Evaluation | < 2.0 ms | 1.1 to 1.8 ms |
| VAD Silence Hangover (Release) | 800.0 ms | 800.0 ms |
| Sherpa-ONNX STT (4s audio segment) | < 450.0 ms | 280 to 420 ms |
| Freight Lexicon Normalization | < 5.0 ms | 0.8 to 2.2 ms |
| Deterministic Freight Engine (Fallback) | < 5.0 ms | 1.2 to 3.5 ms |
| Local LLM (Llama 3.2 1B Q4, RO Disabled) | < 1500.0 ms | 1100 to 1450 ms |
| Local LLM (Llama 3.2 1B Q4, RO Enabled) | < 2500.0 ms | 1850 to 2350 ms |
| Compose UI State Render | < 16.0 ms | 4.0 to 9.0 ms |

## 4. Hardware Panic and Recovery Verification

1. **Volume Up Double Click**: Verify that pressing Volume Up twice within 400 milliseconds clears dialogue history and resets context to an empty state within 50 milliseconds.
2. **Auto-Standby**: Verify that after 180 seconds of silence, the audio engine enters standby mode, reducing CPU usage below 1%.
3. **Memory Stability**: Verify that continuous 30-minute test cycles maintain total app memory footprint under 2.2 GB without native heap leakage.
