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
 * Ultra-fast Gemini AI client (Google AI Studio) providing 1M+ token context window,
 * high free-tier rate limits (15 RPM / 1,500 RPD), dual-key failover, and dynamic model fetching.
 */
class GeminiLlmClient(
    private var primaryApiKey: String = "",
    private var backupApiKey: String = "",
    private var primaryModel: String = DEFAULT_MODEL,
    private var fallbackModel: String = DEFAULT_FALLBACK_MODEL
) {

    fun updateConfig(
        newPrimaryApiKey: String,
        newBackupApiKey: String = backupApiKey,
        newPrimaryModel: String = primaryModel,
        newFallbackModel: String = fallbackModel
    ) {
        if (newPrimaryApiKey.isNotBlank()) primaryApiKey = newPrimaryApiKey.trim()
        if (newBackupApiKey.isNotBlank()) backupApiKey = newBackupApiKey.trim()
        if (newPrimaryModel.isNotBlank()) primaryModel = newPrimaryModel.trim()
        if (newFallbackModel.isNotBlank()) fallbackModel = newFallbackModel.trim()
    }

    /**
     * Tests the provided Gemini API key by querying Google AI Studio's models endpoint.
     * Returns the sorted list of model identifiers supporting content generation.
     */
    suspend fun fetchAvailableModels(apiKey: String): Result<List<String>> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Gemini API key is blank"))
        }

        try {
            val endpointUrl = "https://generativelanguage.googleapis.com/v1beta/models?key=$cleanKey"
            val url = URL(endpointUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "StealthVoiceCopilot/1.0 (Android; Gemini)")
                connectTimeout = 4500
                readTimeout = 6000
                doInput = true
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseText = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val rootJson = JSONObject(responseText)
                val modelsArray = rootJson.optJSONArray("models") ?: JSONArray()

                val modelIds = mutableListOf<String>()
                for (i in 0 until modelsArray.length()) {
                    val mObj = modelsArray.optJSONObject(i) ?: continue
                    val name = mObj.optString("name", "") // e.g. "models/gemini-2.5-flash"
                    val cleanId = name.removePrefix("models/")
                    val methods = mObj.optJSONArray("supportedGenerationMethods")

                    var canGenerate = false
                    if (methods != null) {
                        for (j in 0 until methods.length()) {
                            if (methods.optString(j) == "generateContent") {
                                canGenerate = true
                                break
                            }
                        }
                    }

                    if (canGenerate && cleanId.contains("gemini", ignoreCase = true)) {
                        modelIds.add(cleanId)
                    }
                }

                // Sort models so flash and newer models appear on top
                val sorted = modelIds.sortedWith(
                    compareByDescending<String> { it.contains("2.5") }
                        .thenByDescending { it.contains("2.0") }
                        .thenByDescending { it.contains("flash") }
                        .thenBy { it }
                )

                if (sorted.isNotEmpty()) {
                    Result.success(sorted)
                } else {
                    Result.success(listOf("gemini-2.5-flash", "gemini-2.0-flash", "gemini-1.5-flash"))
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
        startTimeMs: Long = System.currentTimeMillis(),
        forceFallbackModel: Boolean = false
    ): CopilotTurn? = withContext(Dispatchers.IO) {
        val keysToTry = listOf(primaryApiKey, backupApiKey).filter { it.isNotBlank() }
        if (keysToTry.isEmpty()) {
            android.util.Log.d(TAG, "No Gemini API keys configured; skipping Gemini inference")
            return@withContext null
        }

        val targetModel = if (forceFallbackModel) fallbackModel else primaryModel
        val candidateModels = if (forceFallbackModel) {
            listOf(fallbackModel, "gemini-2.0-flash", "gemini-1.5-flash").distinct()
        } else {
            listOf(primaryModel, "gemini-2.5-flash", fallbackModel).distinct()
        }

        for (activeKey in keysToTry) {
            for (candidate in candidateModels) {
                val result = executeInference(activeKey, candidate, question, recentTurns, isRomanianEnabled, profile, startTimeMs)
                if (result != null) return@withContext result
            }
        }

        null
    }

    private fun executeInference(
        activeKey: String,
        targetModel: String,
        question: String,
        recentTurns: List<CopilotTurn>,
        isRomanianEnabled: Boolean,
        profile: com.copilot.stealth.data.models.CarrierProfile,
        startTimeMs: Long
    ): CopilotTurn? {
        try {
            val systemPrompt = PromptBuilder.buildSystemPrompt(isRomanianEnabled, profile)

            // Construct Gemini contents array
            val contentsArray = JSONArray()

            // Context window: take last 4 turns (Gemini comfortably handles huge context)
            for (turn in recentTurns.takeLast(4)) {
                val userContent = JSONObject().apply {
                    put("role", "user")
                    val parts = JSONArray().apply {
                        val heard = turn.transcriptRo ?: turn.transcriptEn
                        put(JSONObject().apply { put("text", heard) })
                    }
                    put("parts", parts)
                }
                contentsArray.put(userContent)

                val modelContent = JSONObject().apply {
                    put("role", "model")
                    val parts = JSONArray().apply {
                        val reply = if (!turn.answerRo.isNullOrBlank()) {
                            "${turn.answerEn}\n[Guidance: ${turn.answerRo}]"
                        } else turn.answerEn
                        put(JSONObject().apply { put("text", reply) })
                    }
                    put("parts", parts)
                }
                contentsArray.put(modelContent)
            }

            // Current user turn
            val currentTurnContent = JSONObject().apply {
                put("role", "user")
                val parts = JSONArray().apply {
                    put(JSONObject().apply { put("text", question) })
                }
                put("parts", parts)
            }
            contentsArray.put(currentTurnContent)

            // Root payload for Google AI Studio generateContent
            val payload = JSONObject().apply {
                val sysInst = JSONObject().apply {
                    val sysParts = JSONArray().apply {
                        put(JSONObject().apply { put("text", systemPrompt) })
                    }
                    put("parts", sysParts)
                }
                put("system_instruction", sysInst)
                put("contents", contentsArray)

                val genConfig = JSONObject().apply {
                    put("temperature", 0.3)
                    put("maxOutputTokens", 800)
                    put("responseMimeType", "application/json")
                }
                put("generationConfig", genConfig)
            }

            val endpointUrl = "https://generativelanguage.googleapis.com/v1beta/models/$targetModel:generateContent?key=$activeKey"
            val url = URL(endpointUrl)
            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "StealthVoiceCopilot/1.0 (Android; Gemini)")
                connectTimeout = 4000
                readTimeout = 6500
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
                android.util.Log.d(TAG, "Gemini response [$targetModel] in ${latency}ms")

                val rootJson = JSONObject(responseText)
                val candidates = rootJson.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val contentObj = candidate.optJSONObject("content")
                    val parts = contentObj?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        val rawContent = parts.getJSONObject(0).optString("text", "").trim()
                        val parsed = parseJsonResponse(rawContent, question)
                        val shortModelName = targetModel.removePrefix("models/")
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
                            matchedCategory = "AI Neural ($shortModelName)",
                            isQuestion = parsed.isQuestion
                        )
                    }
                }
            } else {
                val err = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                android.util.Log.w(TAG, "Gemini HTTP error code $responseCode [$targetModel]: $err")
            }
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Exception during Gemini request [$targetModel]: ${e.message}")
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
                            originalPrompt.contains("how", ignoreCase = true) ||
                            originalPrompt.contains("why", ignoreCase = true)
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
        private const val TAG = "GeminiLlmClient"
        const val DEFAULT_MODEL = "gemini-2.5-flash"
        const val DEFAULT_FALLBACK_MODEL = "gemini-2.0-flash"
    }
}
