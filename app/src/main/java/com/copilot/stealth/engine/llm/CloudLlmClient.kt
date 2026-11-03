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
 * providing sub-250ms conversational intelligence with completely free production models:
 * - llama-3.3-70b-versatile (Flagship Free Meta model, 128k context, advanced reasoning)
 * - llama-3.1-8b-instant (Sub-150ms ultra low latency)
 */
class CloudLlmClient(
    private var apiKey: String = "",
    private var modelName: String = DEFAULT_GROQ_MODEL,
    private var baseUrl: String = "https://api.groq.com/openai/v1/chat/completions"
) {

    fun updateConfig(newApiKey: String, newModelName: String = modelName) {
        if (newApiKey.isNotBlank()) apiKey = newApiKey.trim()
        if (newModelName.isNotBlank()) modelName = newModelName.trim()
    }

    /**
     * Tests the provided Groq API key by querying Groq's models endpoint.
     * Returns the list of free active models available on the user's account.
     */
    suspend fun fetchAvailableModels(apiKey: String): Result<List<String>> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Groq API key is blank"))
        }

        try {
            val url = URL("https://api.groq.com/openai/v1/models")
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer $cleanKey")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "StealthVoiceCopilot/1.0 (Android; Groq)")
                connectTimeout = 4000
                readTimeout = 6000
                doInput = true
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val rootJson = JSONObject(responseText)
                val dataArray = rootJson.optJSONArray("data") ?: JSONArray()

                val modelIds = mutableListOf<String>()
                for (i in 0 until dataArray.length()) {
                    val mObj = dataArray.optJSONObject(i) ?: continue
                    val id = mObj.optString("id", "")
                    if (id.isNotBlank() && !id.contains("whisper", ignoreCase = true) && !id.contains("embedding", ignoreCase = true)) {
                        modelIds.add(id)
                    }
                }

                // Sort models prioritizing flagship free models
                val sorted = modelIds.sortedWith(
                    compareByDescending<String> { it.contains("llama-3.3-70b") }
                        .thenByDescending { it.contains("llama-3.1-8b") }
                        .thenByDescending { it.contains("gemma2") }
                        .thenBy { it }
                )

                if (sorted.isNotEmpty()) {
                    Result.success(sorted)
                } else {
                    Result.success(listOf("llama-3.3-70b-versatile", "llama-3.1-8b-instant", "gemma2-9b-it"))
                }
            } else {
                val err = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: "HTTP $responseCode"
                Result.failure(Exception("HTTP $responseCode: $err"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun generateResponse(
        question: String,
        recentTurns: List<CopilotTurn>,
        isRomanianEnabled: Boolean,
        profile: com.copilot.stealth.data.models.CarrierProfile = com.copilot.stealth.data.models.CarrierProfile(),
        startTimeMs: Long = System.currentTimeMillis()
    ): CopilotTurn? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            android.util.Log.d(TAG, "Groq API key is blank; skipping cloud inference")
            return@withContext null
        }

        // 100% Free Tier production models on Groq
        val candidateModels = listOf(
            modelName,
            DEFAULT_GROQ_MODEL,
            FAST_FALLBACK_MODEL,
            "gemma2-9b-it"
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
                    put("content", turn.transcriptRo ?: turn.transcriptEn)
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
                put("content", "Utterance: $question")
            }
            messagesArray.put(currentMsg)

            val payload = JSONObject().apply {
                put("model", targetModel)
                put("messages", messagesArray)
                put("temperature", 0.3)
                put("max_tokens", 500)
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
                setRequestProperty("User-Agent", "StealthVoiceCopilot/1.0 (Android; Groq)")
                connectTimeout = 3500
                readTimeout = 5000
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

                    val parsed = parseJsonResponse(rawContent, question)
                    val shortModelName = targetModel.substringAfterLast("/")
                    val langCode = if (profile.guidanceLanguage != com.copilot.stealth.data.models.GuidanceLanguage.OFF) {
                        profile.guidanceLanguage.code
                    } else null

                    return CopilotTurn(
                        transcriptEn = parsed.transcriptEn ?: question,
                        transcriptRo = parsed.transcriptRo,
                        answerEn = parsed.answerEn,
                        answerRo = parsed.answerRo,
                        translationLangCode = langCode,
                        followUpQuestions = parsed.followUps,
                        confidence = 0.99f,
                        latencyMs = latency,
                        matchedCategory = "AI Neural (Groq $shortModelName)",
                        isQuestion = parsed.isQuestion
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

    data class ParsedLlmResult(
        val isQuestion: Boolean,
        val transcriptRo: String?,
        val transcriptEn: String?,
        val answerEn: String,
        val answerRo: String?,
        val followUps: List<String> = emptyList()
    )

    private fun parseJsonResponse(rawJson: String, originalPrompt: String): ParsedLlmResult {
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
                val transcriptEn = if (json.has("transcript_en") && !json.isNull("transcript_en")) {
                    json.getString("transcript_en").trim().takeIf { it.isNotBlank() && it != "null" }
                } else null
                val transcriptRo = if (json.has("transcript_ro") && !json.isNull("transcript_ro")) {
                    json.getString("transcript_ro").trim().takeIf { it.isNotBlank() && it != "null" }
                } else null
                val answerRo = if (json.has("answer_ro") && !json.isNull("answer_ro")) {
                    json.getString("answer_ro").trim().takeIf { it.isNotBlank() && it != "null" }
                } else null

                val isQuestion = if (json.has("is_question")) {
                    json.optBoolean("is_question", true)
                } else {
                    originalPrompt.contains("?") ||
                            originalPrompt.contains("care", ignoreCase = true) ||
                            originalPrompt.contains("cum", ignoreCase = true) ||
                            originalPrompt.contains("ce ", ignoreCase = true) ||
                            originalPrompt.contains("what", ignoreCase = true) ||
                            originalPrompt.contains("how", ignoreCase = true)
                }

                val followUps = mutableListOf<String>()
                val followUpsArray = json.optJSONArray("follow_ups")
                if (followUpsArray != null) {
                    for (i in 0 until followUpsArray.length()) {
                        val item = followUpsArray.optString(i, "").trim()
                        if (item.isNotBlank()) {
                            followUps.add(item)
                        }
                    }
                }

                if (answerEn.isNotBlank() || !transcriptEn.isNullOrBlank()) {
                    return ParsedLlmResult(
                        isQuestion = isQuestion,
                        transcriptRo = transcriptRo,
                        transcriptEn = transcriptEn ?: originalPrompt,
                        answerEn = if (answerEn.isNotBlank()) answerEn else (transcriptEn ?: originalPrompt),
                        answerRo = answerRo,
                        followUps = followUps
                    )
                }
            }
        } catch (_: Exception) {}

        return ParsedLlmResult(
            isQuestion = true,
            transcriptRo = null,
            transcriptEn = originalPrompt,
            answerEn = rawJson.trim(),
            answerRo = null,
            followUps = emptyList()
        )
    }

    companion object {
        private const val TAG = "CloudLlmClient"
        const val DEFAULT_GROQ_MODEL = "llama-3.3-70b-versatile"
        const val FAST_FALLBACK_MODEL = "llama-3.1-8b-instant"
    }
}
