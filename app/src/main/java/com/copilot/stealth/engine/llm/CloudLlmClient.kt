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
    private var modelName: String = "openai/gpt-oss-20b",
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
        startTimeMs: Long = System.currentTimeMillis()
    ): CopilotTurn? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null

        try {
            val systemPrompt = if (isRomanianEnabled) {
                """
                You are a discreet, intelligent real-time voice assistant helping a candidate in an interview or professional dialog.
                Rules:
                1. Provide a direct, natural, confident spoken response in 1 or 2 concise English sentences that the user can read aloud immediately.
                2. Provide the Romanian translation of the question and a brief Romanian translation of the answer.
                3. Respond to ANY topic naturally without predefined domain constraints unless freight/logistics is specifically asked.
                4. Output STRICT JSON only:
                {"transcript_ro":"<Romanian question translation>","answer_en":"<1-2 concise English spoken sentences>","answer_ro":"<Romanian answer explanation>"}
                """.trimIndent()
            } else {
                """
                You are a discreet, intelligent real-time voice assistant helping a candidate in an interview or professional dialog.
                Rules:
                1. Provide a direct, natural, confident spoken response in 1 or 2 concise English sentences that the user can read aloud immediately.
                2. Respond to ANY topic naturally without predefined domain constraints unless freight/logistics is specifically asked.
                3. Output STRICT JSON only:
                {"transcript_ro":null,"answer_en":"<1-2 concise English spoken sentences>","answer_ro":null}
                """.trimIndent()
            }

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
                put("model", modelName)
                put("messages", messagesArray)
                put("temperature", 0.3)
                put("max_tokens", 150)
                val respFormat = JSONObject().apply { put("type", "json_object") }
                put("response_format", respFormat)
            }

            val url = URL(baseUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "StealthVoiceCopilot/1.0")
                connectTimeout = 3000
                readTimeout = 4000
                doOutput = true
                doInput = true
            }

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                android.util.Log.d(TAG, "Groq Cloud response received in ${System.currentTimeMillis() - startTimeMs}ms: $responseText")
                val rootJson = JSONObject(responseText)
                val choices = rootJson.getJSONArray("choices")
                if (choices.length() > 0) {
                    val message = choices.getJSONObject(0).getJSONObject("message")
                    val rawContent = message.getString("content").trim()

                    val parsed = parseJsonResponse(rawContent)
                    val latency = (System.currentTimeMillis() - startTimeMs).coerceAtLeast(1L)

                    return@withContext CopilotTurn(
                        transcriptEn = question,
                        transcriptRo = parsed.first,
                        answerEn = parsed.second,
                        answerRo = parsed.third,
                        confidence = 0.99f,
                        latencyMs = latency,
                        matchedCategory = "AI Neural (${modelName.substringAfterLast("/")})"
                    )
                }
            } else {
                val err = connection.errorStream?.bufferedReader()?.use { it.readText() }
                android.util.Log.e(TAG, "Groq HTTP error code $responseCode: $err")
            }
            null
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Exception in CloudLlmClient: ${e.message}", e)
            null
        }
    }

    private fun parseJsonResponse(rawJson: String): Triple<String?, String, String?> {
        try {
            val startIdx = rawJson.indexOf('{')
            val endIdx = rawJson.lastIndexOf('}')
            if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                val clean = rawJson.substring(startIdx, endIdx + 1)
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
