package com.copilot.stealth.engine.llm

import android.content.Context
import com.copilot.stealth.data.models.CarrierProfile
import com.copilot.stealth.data.models.CopilotTurn
import com.copilot.stealth.data.models.EngineType
import com.copilot.stealth.data.repository.CarrierProfileRepository
import com.copilot.stealth.engine.network.NetworkHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * Intelligent Multi-Engine AI Orchestrator implementing strict hierarchical routing:
 *
 * 1. Offline Mode:
 *    - If no internet connection and local neural model is loaded -> Executes on-device immediately.
 *    - If no internet and no local model -> Returns clear offline status notice.
 *
 * 2. Online Mode (Hierarchical Fallback):
 *    - Priority 1: Google Gemini (Google AI Studio) - 1M+ context, high rate limits.
 *    - Fallback 1: Groq Cloud LPU (Sub-250ms latency) if Gemini encounters any error.
 *    - Fallback 2: Gemini Retry (alternative Flash model) if Groq also fails.
 *    - Fallback 3: On-Device Local Neural Model (Llama 3.2 1B / Gemma 2 2B) if cloud fails.
 */
class LocalLlmEngine(
    private val context: Context,
    private val carrierProfileRepository: CarrierProfileRepository = CarrierProfileRepository(context)
) : AutoCloseable {

    private val modelManager = LocalModelManager(context)
    private val neuralRunner = LocalNeuralRunner(context)

    private val geminiClient = GeminiLlmClient(
        apiKey = carrierProfileRepository.getGeminiApiKey(),
        modelName = carrierProfileRepository.getGeminiModel()
    )

    private val groqClient = CloudLlmClient(
        apiKey = carrierProfileRepository.getGroqApiKey(),
        modelName = carrierProfileRepository.getCloudModel()
    )

    private var activeEngineType = carrierProfileRepository.getEngineType()

    val isNeuralModelReady: Boolean
        get() = neuralRunner.isModelLoaded

    val activeModelName: String?
        get() = neuralRunner.activeModelName

    init {
        initializePreferredModel()
    }

    private fun initializePreferredModel() {
        val preferred = carrierProfileRepository.getPreferredLocalModel()
        val candidate = modelManager.getModelByPreference(preferred)
        if (candidate != null) {
            neuralRunner.loadModel(candidate)
        }
    }

    fun setPreferredLocalModel(modelName: String): Boolean {
        carrierProfileRepository.savePreferredLocalModel(modelName)
        val candidate = modelManager.getModelByPreference(modelName)
        return if (candidate != null) {
            neuralRunner.loadModel(candidate)
        } else {
            false
        }
    }

    fun loadCustomModel(file: File): Boolean {
        return neuralRunner.loadModel(file)
    }

    fun reloadCarrierProfile() {
        reloadCloudConfig()
    }

    fun reloadCloudConfig() {
        geminiClient.updateConfig(
            newApiKey = carrierProfileRepository.getGeminiApiKey(),
            newModelName = carrierProfileRepository.getGeminiModel()
        )
        groqClient.updateConfig(
            newApiKey = carrierProfileRepository.getGroqApiKey(),
            newModelName = carrierProfileRepository.getCloudModel()
        )
    }

    suspend fun generateResponse(
        question: String,
        recentTurns: List<CopilotTurn>,
        isRomanianEnabled: Boolean,
        profile: CarrierProfile = carrierProfileRepository.getProfile(),
        forcedEngineType: EngineType = activeEngineType
    ): CopilotTurn = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val isOnline = NetworkHelper.isOnline(context)
        val isLocalModelReady = neuralRunner.isModelLoaded

        // ==========================================
        // RULE 1: OFFLINE DETECTION
        // If no internet connection -> immediately route to local model
        // ==========================================
        if (!isOnline) {
            if (isLocalModelReady) {
                android.util.Log.i(TAG, "No internet connection detected; routing immediately to on-device local model.")
                try {
                    return@withContext executeNeuralInference(question, recentTurns, isRomanianEnabled, profile, startTime)
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Local neural inference failed while offline: ${e.message}", e)
                }
            }

            val offlineNoticeEn = "Offline Mode: No active internet connection and no local model (Llama 3.2 / Gemma 2) is downloaded. Please connect to WiFi/cellular or download a local model in Settings."
            val offlineNoticeRo = if (isRomanianEnabled) {
                "Mod Offline: Fără conexiune activă la internet și niciun model local (Llama 3.2 / Gemma 2) nu este descărcat. Conectați-vă la internet sau descărcați un model din Setări."
            } else null

            return@withContext CopilotTurn(
                transcriptEn = question,
                transcriptRo = if (isRomanianEnabled) "Fără conexiune la internet" else null,
                answerEn = offlineNoticeEn,
                answerRo = offlineNoticeRo,
                confidence = 1.0f,
                latencyMs = (System.currentTimeMillis() - startTime).coerceAtLeast(1L),
                matchedCategory = "Offline Notice",
                isQuestion = false
            )
        }

        // ==========================================
        // RULE 2: FORCED LOCAL NEURAL OVERRIDE
        // If user explicitly configured Local Neural only
        // ==========================================
        if (forcedEngineType == EngineType.LOCAL_NEURAL && isLocalModelReady) {
            try {
                return@withContext executeNeuralInference(question, recentTurns, isRomanianEnabled, profile, startTime)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Local neural inference failed: ${e.message}", e)
            }
        }

        val geminiKey = carrierProfileRepository.getGeminiApiKey().trim()
        val groqKey = carrierProfileRepository.getGroqApiKey().trim()

        // ==========================================
        // RULE 3: ONLINE HIERARCHICAL FALLBACK
        // Step 1: Gemini (Priority 1 - Google AI Studio)
        // ==========================================
        if (geminiKey.isNotBlank()) {
            try {
                val geminiResult = geminiClient.generateResponse(
                    question = question,
                    recentTurns = recentTurns,
                    isRomanianEnabled = isRomanianEnabled,
                    profile = profile,
                    startTimeMs = startTime,
                    isRetry = false
                )
                if (geminiResult != null) {
                    return@withContext geminiResult
                }
                android.util.Log.w(TAG, "Gemini inference failed or timed out. Falling back to Groq...")
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Gemini invocation exception: ${e.message}. Proceeding to Groq fallback...")
            }
        }

        // ==========================================
        // Step 2: Groq Cloud LPU (Fallback 1)
        // ==========================================
        if (groqKey.isNotBlank()) {
            try {
                val groqResult = groqClient.generateResponse(
                    question = question,
                    recentTurns = recentTurns,
                    isRomanianEnabled = isRomanianEnabled,
                    profile = profile,
                    startTimeMs = startTime
                )
                if (groqResult != null) {
                    return@withContext groqResult
                }
                android.util.Log.w(TAG, "Groq inference failed. Falling back to Gemini retry...")
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Groq invocation exception: ${e.message}. Proceeding to Gemini retry...")
            }
        }

        // ==========================================
        // Step 3: Gemini Retry (Fallback 2 with secondary candidate)
        // ==========================================
        if (geminiKey.isNotBlank()) {
            try {
                val geminiRetryResult = geminiClient.generateResponse(
                    question = question,
                    recentTurns = recentTurns,
                    isRomanianEnabled = isRomanianEnabled,
                    profile = profile,
                    startTimeMs = startTime,
                    isRetry = true
                )
                if (geminiRetryResult != null) {
                    return@withContext geminiRetryResult
                }
                android.util.Log.w(TAG, "Gemini retry failed. Falling back to Local Neural Model...")
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Gemini retry exception: ${e.message}. Falling back to Local Neural...")
            }
        }

        // ==========================================
        // Step 4: Local Neural Model (Fallback 3)
        // ==========================================
        if (isLocalModelReady) {
            try {
                android.util.Log.i(TAG, "Cloud providers failed or unavailable; executing On-Device Local Neural Model.")
                return@withContext executeNeuralInference(question, recentTurns, isRomanianEnabled, profile, startTime)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Local neural inference failed: ${e.message}", e)
            }
        }

        // ==========================================
        // Step 5: System Guidance Notice
        // When neither Cloud AI keys nor Local Models are ready
        // ==========================================
        val noticeEn = if (geminiKey.isBlank() && groqKey.isBlank()) {
            "No Cloud AI Key configured. Please enter your Google Gemini (Google AI Studio) or Groq API key in Settings, or download an on-device local model."
        } else {
            "Cloud AI services (Gemini & Groq) are temporarily unreachable and no on-device local model is loaded. Please check your network or load a model."
        }

        val noticeRo = if (isRomanianEnabled) {
            if (geminiKey.isBlank() && groqKey.isBlank()) {
                "Nicio cheie Cloud AI configurată. Introduceți cheia Google Gemini (Google AI Studio) sau Groq din Setări, ori descărcați un model local offline."
            } else {
                "Serviciile Cloud AI (Gemini & Groq) sunt temporar indisponibile și niciun model local nu este descărcat. Verificați conexiunea sau descărcați un model."
            }
        } else null

        CopilotTurn(
            transcriptEn = question,
            transcriptRo = if (isRomanianEnabled) "Configurare necesară" else null,
            answerEn = noticeEn,
            answerRo = noticeRo,
            confidence = 1.0f,
            latencyMs = (System.currentTimeMillis() - startTime).coerceAtLeast(1L),
            matchedCategory = "AI System Notice",
            isQuestion = false
        )
    }

    private suspend fun executeNeuralInference(
        question: String,
        recentTurns: List<CopilotTurn>,
        isRomanianEnabled: Boolean,
        profile: CarrierProfile,
        startTime: Long
    ): CopilotTurn {
        val prompt = PromptBuilder.buildUserPrompt(question, recentTurns, isRomanianEnabled, profile)
        val generatedText = neuralRunner.generateResponse(prompt).trim()
        val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(50L)

        val activeName = neuralRunner.activeModelName ?: "Local Neural"
        val parsed = parseJsonResponse(generatedText, question)
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
            confidence = 0.95f,
            latencyMs = latency,
            matchedCategory = "Local AI ($activeName)",
            isQuestion = parsed.isQuestion
        )
    }

    data class ParsedNeuralResult(
        val isQuestion: Boolean,
        val transcriptRo: String?,
        val transcriptEn: String?,
        val answerEn: String,
        val answerRo: String?,
        val followUps: List<String> = emptyList()
    )

    private fun parseJsonResponse(rawJson: String, originalPrompt: String): ParsedNeuralResult {
        try {
            val startIdx = rawJson.indexOf('{')
            val endIdx = rawJson.lastIndexOf('}')
            if (startIdx != -1 && endIdx != -1 && endIdx > startIdx) {
                val clean = rawJson.substring(startIdx, endIdx + 1)
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
                    originalPrompt.contains("?")
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
                    return ParsedNeuralResult(
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

        return ParsedNeuralResult(
            isQuestion = true,
            transcriptRo = null,
            transcriptEn = originalPrompt,
            answerEn = rawJson.trim(),
            answerRo = null,
            followUps = emptyList()
        )
    }

    fun setEngineType(type: EngineType) {
        activeEngineType = type
        carrierProfileRepository.saveEngineType(type)
    }

    fun getAvailableModels(): List<AvailableModelInfo> {
        return modelManager.scanAvailableModels()
    }

    override fun close() {
        neuralRunner.close()
    }

    companion object {
        private const val TAG = "LocalLlmEngine"
    }
}
