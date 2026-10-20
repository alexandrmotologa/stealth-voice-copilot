package com.copilot.stealth.engine.llm

import android.content.Context
import com.copilot.stealth.data.models.CarrierProfile
import com.copilot.stealth.data.models.CopilotTurn
import com.copilot.stealth.data.models.EngineType
import com.copilot.stealth.data.repository.CarrierProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * Dual-Engine AI Coordinator managing ultra-fast Groq Cloud AI inference (sub-250ms)
 * and on-device quantized neural inference (Llama 3.2 1B / Gemma 2 2B) with zero pre-scripted text.
 */
class LocalLlmEngine(
    private val context: Context,
    private val carrierProfileRepository: CarrierProfileRepository = CarrierProfileRepository(context)
) : AutoCloseable {

    private val modelManager = LocalModelManager(context)
    private val neuralRunner = LocalNeuralRunner(context)
    private val cloudClient = CloudLlmClient(
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
        cloudClient.updateConfig(
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

        // 1. In HYBRID mode: Attempt ultra-fast Cloud AI first (Groq LPU: sub-250ms latency for ANY question)
        if (forcedEngineType == EngineType.HYBRID) {
            try {
                val cloudResult = cloudClient.generateResponse(question, recentTurns, isRomanianEnabled, profile, startTime)
                if (cloudResult != null) {
                    return@withContext cloudResult
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Cloud AI inference failed: ${e.message}", e)
            }
        }

        // 2. On-device local neural model if requested or as fallback
        val shouldRunNeural = when (forcedEngineType) {
            EngineType.LOCAL_NEURAL -> true
            EngineType.HYBRID -> true
            EngineType.DETERMINISTIC_EXPERT -> neuralRunner.isModelLoaded
        }

        if (shouldRunNeural && neuralRunner.isModelLoaded) {
            try {
                return@withContext executeNeuralInference(question, recentTurns, isRomanianEnabled, profile, startTime)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Local neural inference failed: ${e.message}", e)
            }
        }

        // 3. Natural status turn when neither Cloud nor Local model is available
        val noticeEn = "Cloud AI is unavailable and no local model (Llama 3.2 or Gemma 2) is downloaded. Please check internet connection or download a model in Settings."
        val noticeRo = if (isRomanianEnabled) {
            "Cloud AI este indisponibil și niciun model local (Llama 3.2 sau Gemma 2) nu este descărcat. Verificați conexiunea sau descărcați un model din Setări."
        } else null

        CopilotTurn(
            transcriptEn = question,
            transcriptRo = if (isRomanianEnabled) "Conexiune indisponibilă" else null,
            answerEn = noticeEn,
            answerRo = noticeRo,
            confidence = 1.0f,
            latencyMs = (System.currentTimeMillis() - startTime).coerceAtLeast(1L),
            matchedCategory = "AI System Notice"
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
        val parsed = parseJsonResponse(generatedText)

        return CopilotTurn(
            transcriptEn = question,
            transcriptRo = parsed.first,
            answerEn = parsed.second,
            answerRo = parsed.third,
            confidence = 0.95f,
            latencyMs = latency,
            matchedCategory = "Local AI ($activeName)"
        )
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
