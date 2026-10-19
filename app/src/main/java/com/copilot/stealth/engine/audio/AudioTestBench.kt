package com.copilot.stealth.engine.audio

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * Synthetic audio test bench providing deterministic PCM streams for testing and benchmarks.
 */
class AudioTestBench(
    private val sampleRate: Int = 16000,
    private val frameSize: Int = 512
) {

    private val _pcmFlow = MutableSharedFlow<ShortArray>(extraBufferCapacity = 64)
    val pcmFlow: SharedFlow<ShortArray> = _pcmFlow.asSharedFlow()

    private var playbackJob: Job? = null

    /**
     * Injects synthetic speech pattern: 500ms silence, 2.5s speech tone burst, followed by silence.
     */
    fun startSyntheticTestPattern(scope: CoroutineScope) {
        stop()
        playbackJob = scope.launch(Dispatchers.Default) {
            val frameDurationMs = (frameSize * 1000L) / sampleRate

            // 1. Initial silence (500 ms)
            val silenceFrames = (500 / frameDurationMs).toInt()
            repeat(silenceFrames) {
                if (!isActive) return@launch
                _pcmFlow.emit(ShortArray(frameSize) { 0 })
                delay(frameDurationMs)
            }

            // 2. Simulated voice activity (2.5 seconds modulated speech frequency ~300Hz-800Hz)
            val speechFrames = (2500 / frameDurationMs).toInt()
            var phase = 0.0
            for (f in 0 until speechFrames) {
                if (!isActive) return@launch
                val frame = ShortArray(frameSize)
                for (i in 0 until frameSize) {
                    val frequency = 400.0 + 150.0 * sin(2.0 * Math.PI * f / 20.0)
                    phase += 2.0 * Math.PI * frequency / sampleRate
                    val sample = (sin(phase) * 12000.0).toInt().toShort()
                    frame[i] = sample
                }
                _pcmFlow.emit(frame)
                delay(frameDurationMs)
            }

            // 3. Post-speech silence to trigger VAD release (1200 ms > 800ms)
            val trailingSilenceFrames = (1200 / frameDurationMs).toInt()
            repeat(trailingSilenceFrames) {
                if (!isActive) return@launch
                _pcmFlow.emit(ShortArray(frameSize) { 0 })
                delay(frameDurationMs)
            }
        }
    }

    fun stop() {
        playbackJob?.cancel()
        playbackJob = null
    }
}
