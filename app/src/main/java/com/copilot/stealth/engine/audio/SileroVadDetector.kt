package com.copilot.stealth.engine.audio

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File
import java.io.FileOutputStream
import java.nio.FloatBuffer
import kotlin.math.sqrt

/**
 * On-device Voice Activity Detector powered by official Silero VAD with energy-based fallback.
 */
class SileroVadDetector(
    private val context: Context,
    private val sampleRate: Int = 16000,
    private val threshold: Float = 0.50f,
    private val releaseSilenceMs: Long = 800L
) {

    private var ortEnv: OrtEnvironment? = null
    private var ortSession: OrtSession? = null

    // Hidden states for Silero v4 (2, 1, 64) or v5 (2, 1, 128)
    private var isV5Model = false
    private var stateV5: Array<Array<FloatArray>> = Array(2) { Array(1) { FloatArray(128) } }
    private var hStateV4: Array<Array<FloatArray>> = Array(2) { Array(1) { FloatArray(64) } }
    private var cStateV4: Array<Array<FloatArray>> = Array(2) { Array(1) { FloatArray(64) } }

    private var isSpeechActive = false
    private var silenceAccumulatedMs = 0L
    private val speechAudioBuffer = ArrayList<Float>()

    var onSpeechStarted: (() -> Unit)? = null
    var onSpeechFinished: ((FloatArray) -> Unit)? = null
    var onVADProbabilityChanged: ((prob: Float, rms: Float) -> Unit)? = null

    init {
        initializeOnnx()
    }

    private fun initializeOnnx() {
        try {
            val modelFile = copyAssetToCache("models/silero_vad.onnx")
            if (modelFile != null && modelFile.exists() && modelFile.length() > 1000) {
                ortEnv = OrtEnvironment.getEnvironment()
                val sessionOptions = OrtSession.SessionOptions().apply {
                    setInterOpNumThreads(1)
                    setIntraOpNumThreads(2)
                }
                ortSession = ortEnv?.createSession(modelFile.absolutePath, sessionOptions)
                ortSession?.let { session ->
                    isV5Model = session.inputNames.contains("state")
                }
            }
        } catch (_: Exception) {
            ortSession = null
        }
    }

    /**
     * Processes a 512-sample 16kHz PCM audio frame.
     */
    fun processFrame(frame: ShortArray) {
        val floatFrame = FloatArray(frame.size) { frame[it] / 32768.0f }
        val rms = calculateRms(floatFrame)

        val prob = if (ortSession != null && ortEnv != null) {
            evaluateOnnx(floatFrame)
        } else {
            evaluateEnergyFallback(rms)
        }

        onVADProbabilityChanged?.invoke(prob, rms)

        val frameDurationMs = (frame.size * 1000L) / sampleRate

        if (prob >= threshold) {
            if (!isSpeechActive) {
                isSpeechActive = true
                speechAudioBuffer.clear()
                onSpeechStarted?.invoke()
            }
            silenceAccumulatedMs = 0L
            for (sample in floatFrame) {
                speechAudioBuffer.add(sample)
            }
        } else {
            if (isSpeechActive) {
                silenceAccumulatedMs += frameDurationMs
                for (sample in floatFrame) {
                    speechAudioBuffer.add(sample)
                }

                if (silenceAccumulatedMs >= releaseSilenceMs) {
                    isSpeechActive = false
                    val finishedAudio = speechAudioBuffer.toFloatArray()
                    speechAudioBuffer.clear()
                    silenceAccumulatedMs = 0L
                    if (finishedAudio.size >= sampleRate * 0.4) { // At least 400ms of audio
                        onSpeechFinished?.invoke(finishedAudio)
                    }
                }
            }
        }
    }

    fun reset() {
        isSpeechActive = false
        silenceAccumulatedMs = 0L
        speechAudioBuffer.clear()
        stateV5 = Array(2) { Array(1) { FloatArray(128) } }
        hStateV4 = Array(2) { Array(1) { FloatArray(64) } }
        cStateV4 = Array(2) { Array(1) { FloatArray(64) } }
    }

    fun close() {
        try {
            ortSession?.close()
            ortEnv?.close()
        } catch (_: Exception) {}
        ortSession = null
        ortEnv = null
    }

    private fun evaluateOnnx(floatFrame: FloatArray): Float {
        return try {
            val env = ortEnv ?: return evaluateEnergyFallback(calculateRms(floatFrame))
            val session = ortSession ?: return evaluateEnergyFallback(calculateRms(floatFrame))

            val inputBuffer = FloatBuffer.wrap(floatFrame)
            val inputTensor = OnnxTensor.createTensor(env, inputBuffer, longArrayOf(1, floatFrame.size.toLong()))
            val srTensor = OnnxTensor.createTensor(env, longArrayOf(sampleRate.toLong()))

            val inputs = mutableMapOf<String, OnnxTensor>(
                "input" to inputTensor,
                "sr" to srTensor
            )

            val stateTensor = if (isV5Model) {
                val st = OnnxTensor.createTensor(env, stateV5)
                inputs["state"] = st
                st
            } else {
                val ht = OnnxTensor.createTensor(env, hStateV4)
                val ct = OnnxTensor.createTensor(env, cStateV4)
                inputs["h"] = ht
                inputs["c"] = ct
                ht // just keep reference
            }

            val results = session.run(inputs)
            val outputTensor = results.get(0) as? OnnxTensor
            val outputArray = outputTensor?.value as? Array<FloatArray>
            val prob = outputArray?.get(0)?.get(0) ?: 0.0f

            if (isV5Model) {
                val newStateTensor = results.get(1) as? OnnxTensor
                if (newStateTensor != null) {
                    @Suppress("UNCHECKED_CAST")
                    stateV5 = newStateTensor.value as Array<Array<FloatArray>>
                }
            } else {
                val newH = results.get(1) as? OnnxTensor
                val newC = results.get(2) as? OnnxTensor
                if (newH != null && newC != null) {
                    @Suppress("UNCHECKED_CAST")
                    hStateV4 = newH.value as Array<Array<FloatArray>>
                    @Suppress("UNCHECKED_CAST")
                    cStateV4 = newC.value as Array<Array<FloatArray>>
                }
            }

            results.close()
            inputTensor.close()
            srTensor.close()
            stateTensor.close()

            prob
        } catch (_: Exception) {
            evaluateEnergyFallback(calculateRms(floatFrame))
        }
    }

    private fun evaluateEnergyFallback(rms: Float): Float {
        return when {
            rms > 0.06f -> 0.95f
            rms > 0.03f -> 0.75f
            rms > 0.015f -> 0.55f
            rms > 0.008f -> 0.35f
            else -> 0.10f
        }
    }

    private fun calculateRms(frame: FloatArray): Float {
        var sumSquares = 0.0
        for (sample in frame) {
            sumSquares += sample * sample
        }
        return sqrt((sumSquares / frame.size).coerceAtLeast(0.0)).toFloat()
    }

    private fun copyAssetToCache(assetPath: String): File? {
        return try {
            val file = File(context.cacheDir, File(assetPath).name)
            if (file.exists() && file.length() > 1000) return file

            context.assets.open(assetPath).use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file
        } catch (_: Exception) {
            null
        }
    }
}
