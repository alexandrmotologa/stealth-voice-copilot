package com.copilot.stealth.engine.llm

import com.copilot.stealth.data.models.CopilotTurn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Ultra-fast cloud inference client (Groq LPU / OpenAI-compatible)
 * providing sub-250ms conversational intelligence to answer ANY question naturally.
 */
class CloudLlmClient(
    private var apiKey: String,
    private var modelName: String = "qwen/qwen3.8-27b",
    private var baseUrl: String = "https://api.groq.com/openai/v1/chat/completions"
) {

    fun updateConfig(newApiKey: String, newModelName: String = modelName) {
        if (newApiKey.isNotBlank()) apiKey = newApiKey.trim()
        if (newModelName.isNotBlank()) modelName = newModelName.trim()
    }

    suspend fun generateResponse(
        question: String,
        recentTurns: List<CopilotTurn>,
        isRomanianEnabled: Boolean,
        profile: com.copilot.stealth.data.models.CarrierProfile = com.copilot.stealth.data.models.CarrierProfile(),
        startTimeMs: Long = System.currentTimeMillis()
    ): CopilotTurn? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            android.util.Log.w(TAG, "Groq API key is blank; skipping cloud inference")
            return@withContext null
        }

        // Ordered list of candidate models on Groq Free Tier
        val candidateModels = listOf(
            modelName,
            if (modelName == "qwen/qwen3.8-27b") "openai/gpt-oss-20b" else "qwen/qwen3.8-27b"
        ).distinct()

        for (candidate in candidateModels) {
            // First attempt with strict response_format
            var result = executeInference(candidate, question, recentTurns, isRomanianEnabled, profile, startTimeMs, useJsonMode = true)
            if (result != null) return@withContext result

            // Fallback attempt without strict response_format (fixes json_validate_failed)
            result = executeInference(candidate, question, recentTurns, isRomanianEnabled, profile, startTimeMs, useJsonMode = false)
            if (result != null) return@withContext result
        }

        null
    }

    private fun executeInference(
        targetModel: String,
        question: String,
        recentTurns: List<CopilotTurn>,
        isRomanianEnabled: Boolean,
        profile: com.copilot.stealth.data.models.CarrierProfile,
        startTimeMs: Long,
        useJsonMode: Boolean
    ): CopilotTurn? {
        try {
            val systemPrompt = PromptBuilder.buildSystemPrompt(isRomanianEnabled, profile)

            val messagesArray = JSONArray()

            val sysMsg = JSONObject().apply {
                put("role", "system")
                put("content", systemPrompt)
            }
            messagesArray.put(sysMsg)

            // Context window: take last 2 turns
            for (turn in recentTurns.takeLast(2)) {
                val userMsg = JSONObject().apply {
                    put("role", "user")
                    put("content", turn.transcriptEn)
                }
                messagesArray.put(userMsg)

                val assistantMsg = JSONObject().apply {
                    put("role", "assistant")
                    put("content", turn.answerEn)
                }
                messagesArray.put(assistantMsg)
            }

            val currentMsg = JSONObject().apply {
                put("role", "user")
                put("content", "Question: $question")
            }
            messagesArray.put(currentMsg)

            val payload = JSONObject().apply {
                put("model", targetModel)
                put("messages", messagesArray)
                put("temperature", 0.3)
                // Set max_tokens to 400 so reasoning tokens or long answers don't get truncated
                put("max_tokens", 400)
                if (useJsonMode) {
                    val respFormat = JSONObject().apply { put("type", "json_object") }
                    put("response_format", respFormat)
                }
            }

            val url = URL(baseUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "StealthVoiceCopilot/1.0 (Android)")
                connectTimeout = 4000
                readTimeout = 6000
                doOutput = true
                doInput = true
            }

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val latency = (System.currentTimeMillis() - startTimeMs).coerceAtLeast(1L)
                android.util.Log.d(TAG, "Groq response [$targetModel, jsonMode=$useJsonMode] in ${latency}ms")

                val rootJson = JSONObject(responseText)
                val choices = rootJson.getJSONArray("choices")
                if (choices.length() > 0) {
                    val message = choices.getJSONObject(0).getJSONObject("message")
                    val rawContent = message.getString("content").trim()

                    val parsed = parseJsonResponse(rawContent)
                    val shortModelName = targetModel.substringAfterLast("/")

                    return CopilotTurn(
                        transcriptEn = question,
                        transcriptRo = parsed.first,
                        answerEn = parsed.second,
                        answerRo = parsed.third,
                        confidence = 0.99f,
                        latencyMs = latency,
                        matchedCategory = "AI Neural ($shortModelName)"
                    )
                }
            } else {
                val err = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                android.util.Log.w(TAG, "Groq HTTP error code $responseCode [$targetModel, jsonMode=$useJsonMode]: $err")
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Exception during Groq request [$targetModel, jsonMode=$useJsonMode]: ${e.message}")
        }
        return null
    }

    private fun parseJsonResponse(rawJson: String): Triple<String?, String, String?> {
        try {
            var text = rawJson.trim()
            if (text.startsWith("```json")) {
                text = text.removePrefix("```json").trim()
            } else if (text.startsWith("```")) {
                text = text.removePrefix("```").trim()
            }
            if (text.endsWith("```")) {
                text = text.removeSuffix("```").trim()
            }

            val startIdx = text.indexOf('{')
            val endIdx = text.lastIndexOf('}')
            if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                val clean = text.substring(startIdx, endIdx + 1)
                val json = JSONObject(clean)
                val answerEn = json.optString("answer_en", "").trim()
                val transcriptRo = if (json.has("transcript_ro") && !json.isNull("transcript_ro")) {
                    json.getString("transcript_ro").trim().takeIf { it.isNotBlank() && it != "null" }
                } else null
                val answerRo = if (json.has("answer_ro") && !json.isNull("answer_ro")) {
                    json.getString("answer_ro").trim().takeIf { it.isNotBlank() && it != "null" }
                } else null
                if (answerEn.isNotBlank()) {
                    return Triple(transcriptRo, answerEn, answerRo)
                }
            }
        } catch (_: Exception) {}

        return Triple(null, rawJson.trim(), null)
    }

    companion object {
        private const val TAG = "CloudLlmClient"
    }
}
