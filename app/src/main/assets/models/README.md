# On-Device Neural Models

This directory holds the quantized offline models deployed on-device:

1. `silero_vad.onnx`: Silero Voice Activity Detector v5 (approx. 2.2 MB). Used for high-efficiency voice activity detection on 512-sample PCM frames.
2. `sherpa-whisper-base-en/`: Sherpa-ONNX quantized int8 Whisper encoder and decoder files for English transcription.
3. `llama-3.2-1b-instruct-q4.bin`: MediaPipe GenAI compatible 4-bit quantized Llama 3.2 1B parameter instruct model for local answer generation.

When neural model assets are omitted or loading in the background, `FreightKnowledgeEngine` provides zero-latency deterministic responses for 100+ logistics interview prompts.
