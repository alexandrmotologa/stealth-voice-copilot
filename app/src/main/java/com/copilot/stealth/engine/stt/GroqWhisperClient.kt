package com.copilot.stealth.engine.stt

import android.util.Log
import com.copilot.stealth.data.repository.CarrierProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class WhisperResult(
    val text: String,
    val language: String,
    val durationSeconds: Float = 0f
)

/**
 * Ultra-fast multilingual speech recognition powered by Groq Whisper (LPU inference).
 * Automatically detects Romanian, English, or any spoken language without manual mode switching.
 */
class GroqWhisperClient(
    private val carrierProfileRepository: CarrierProfileRepository
) {
    companion object {
        private const val TAG = "GroqWhisperClient"
        private const val WHISPER_API_URL = "https://api.groq.com/openai/v1/audio/transcriptions"
        private const val MODEL_NAME = "whisper-large-v3-turbo"
    }

    /**
     * Converts a 16kHz float PCM array into valid 16-bit mono RIFF WAV byte array.
     */
    fun pcmFloatToWavBytes(samples: FloatArray, sampleRate: Int = 16000): ByteArray {
        val pcmShorts = ShortArray(samples.size) { i ->
            (samples[i].coerceIn(-1.0f, 1.0f) * 32767.0f).toInt().toShort()
        }
        val byteDataSize = pcmShorts.size * 2
        val totalSize = 36 + byteDataSize

        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN).apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt(totalSize)
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16) // Subchunk1Size for PCM
            putShort(1.toShort()) // AudioFormat 1 = PCM
            putShort(1.toShort()) // NumChannels = 1 (Mono)
            putInt(sampleRate)
            putInt(sampleRate * 2) // ByteRate = sampleRate * 1 * 16/8
            putShort(2.toShort()) // BlockAlign = 1 * 16/8
            putShort(16.toShort()) // BitsPerSample = 16
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(byteDataSize)
        }.array()

        val pcmBytes = ByteArray(byteDataSize)
        ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(pcmShorts)

        val wav = ByteArray(44 + byteDataSize)
        System.arraycopy(header, 0, wav, 0, 44)
        System.arraycopy(pcmBytes, 0, wav, 44, byteDataSize)
        return wav
    }

    suspend fun transcribe(
        audioSamples: FloatArray,
        prompt: String? = null
    ): WhisperResult? = withContext(Dispatchers.IO) {
        if (audioSamples.isEmpty()) return@withContext null

        val apiKey = carrierProfileRepository.getGroqApiKey().trim()
        if (apiKey.isBlank()) {
            Log.w(TAG, "Cannot transcribe with Groq Whisper: No Groq API Key set")
            return@withContext null
        }

        val wavBytes = pcmFloatToWavBytes(audioSamples, 16000)
        val boundary = "===StealthWhisper${System.currentTimeMillis()}==="
        val lineEnd = "\r\n"
        val twoHyphens = "--"

        var connection: HttpURLConnection? = null
        try {
            val url = URL(WHISPER_API_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doInput = true
                doOutput = true
                useCaches = false
                connectTimeout = 7000
                readTimeout = 10000
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
            }

            DataOutputStream(connection.outputStream).use { dos ->
                // Part: model
                dos.writeBytes("$twoHyphens$boundary$lineEnd")
                dos.writeBytes("Content-Disposition: form-data; name=\"model\"$lineEnd$lineEnd")
                dos.writeBytes(MODEL_NAME + lineEnd)

                // Part: response_format
                dos.writeBytes("$twoHyphens$boundary$lineEnd")
                dos.writeBytes("Content-Disposition: form-data; name=\"response_format\"$lineEnd$lineEnd")
                dos.writeBytes("verbose_json$lineEnd")

                // Part: temperature
                dos.writeBytes("$twoHyphens$boundary$lineEnd")
                dos.writeBytes("Content-Disposition: form-data; name=\"temperature\"$lineEnd$lineEnd")
                dos.writeBytes("0.0$lineEnd")

                // Optional prompt for domain context
                if (!prompt.isNullOrBlank()) {
                    dos.writeBytes("$twoHyphens$boundary$lineEnd")
                    dos.writeBytes("Content-Disposition: form-data; name=\"prompt\"$lineEnd$lineEnd")
                    dos.write(prompt.toByteArray(Charsets.UTF_8))
                    dos.writeBytes(lineEnd)
                }

                // Part: file (audio.wav)
                dos.writeBytes("$twoHyphens$boundary$lineEnd")
                dos.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"speech.wav\"$lineEnd")
                dos.writeBytes("Content-Type: audio/wav$lineEnd$lineEnd")
                dos.write(wavBytes)
                dos.writeBytes(lineEnd)

                // End of multipart
                dos.writeBytes("$twoHyphens$boundary$twoHyphens$lineEnd")
                dos.flush()
            }

            val responseCode = connection.responseCode
            val responseStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val responseBody = responseStream?.bufferedReader()?.use { it.readText() } ?: ""
            if (responseCode in 200..299) {
                val json = JSONObject(responseBody)
                val rawText = json.optString("text", "").trim()
                val detectedLang = json.optString("language", "auto").lowercase()
                val duration = json.optDouble("duration", 0.0).toFloat()

                // Filter out Whisper hallucinated silence phrases like "Thank you.", "Subtitle by...", etc. on empty noise
                if (isWhisperHallucination(rawText)) {
                    Log.d(TAG, "Ignored Whisper hallucination on silence: '$rawText'")
                    return@withContext null
                }

                val normalizedText = FreightLexiconNormalizer.normalize(rawText)
                Log.d(TAG, "Whisper success [$detectedLang, ${duration}s]: $normalizedText")
                return@withContext WhisperResult(
                    text = normalizedText,
                    language = detectedLang,
                    durationSeconds = duration
                )
            } else {
                Log.e(TAG, "Groq Whisper API error (HTTP $responseCode): $responseBody")
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Whisper transcription exception: ${e.message}", e)
            return@withContext null
        } finally {
            connection?.disconnect()
        }
    }

    private fun isWhisperHallucination(text: String): Boolean {
        val clean = text.trim().lowercase().removeSuffix(".")
        return clean.isEmpty() ||
                clean == "thank you" ||
                clean == "thanks for watching" ||
                clean == "you" ||
                clean == "bye" ||
                clean.startsWith("subtitles by") ||
                clean.startsWith("amara.org") ||
                clean == "transcription by"
    }
}
