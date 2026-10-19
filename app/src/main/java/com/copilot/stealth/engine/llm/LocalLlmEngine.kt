package com.copilot.stealth.engine.llm

import android.content.Context
import com.copilot.stealth.data.models.CopilotTurn
import com.copilot.stealth.data.models.EngineType
import com.copilot.stealth.data.repository.CarrierProfileRepository
import com.copilot.stealth.data.repository.FreightKnowledgeEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Hybrid inference coordinator managing ultra-fast cloud LLMs (Groq LPU / OpenAI),
 * on-device quantized neural execution via MediaPipe Tasks GenAI, and instant deterministic fallback.
 */
class LocalLlmEngine(
    private val context: Context,
    private val carrierProfileRepository: CarrierProfileRepository = CarrierProfileRepository(context),
    private var knowledgeEngine: FreightKnowledgeEngine = FreightKnowledgeEngine(
        carrierProfile = carrierProfileRepository.getProfile()
    )
) : AutoCloseable {

    private val modelManager = LocalModelManager(context)
    private val neuralRunner = LocalNeuralRunner(context)
    private val cloudClient = CloudLlmClient(
        apiKey = carrierProfileRepository.getGroqApiKey(),
        modelName = carrierProfileRepository.getCloudModel()
    )
    private var activeEngineType = EngineType.HYBRID

    val isNeuralModelReady: Boolean
        get() = neuralRunner.isModelLoaded

    val activeModelName: String?
        get() = neuralRunner.activeModelName

    init {
        initializeNeuralModel()
    }

    private fun initializeNeuralModel() {
        val defaultModel = modelManager.getDefaultModel()
        if (defaultModel != null) {
            neuralRunner.loadModel(defaultModel)
        }
    }

    fun loadCustomModel(file: File): Boolean {
        return neuralRunner.loadModel(file)
    }

    fun reloadCarrierProfile() {
        knowledgeEngine = FreightKnowledgeEngine(carrierProfile = carrierProfileRepository.getProfile())
        cloudClient.updateConfig(
            newApiKey = carrierProfileRepository.getGroqApiKey(),
            newModelName = carrierProfileRepository.getCloudModel()
        )
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
        forcedEngineType: EngineType = activeEngineType
    ): CopilotTurn = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()

        // 1. In HYBRID mode: Attempt ultra-fast Cloud AI first (Groq LPU: sub-250ms latency for ANY question)
        if (forcedEngineType == EngineType.HYBRID) {
            try {
                val cloudResult = cloudClient.generateResponse(question, recentTurns, isRomanianEnabled, startTime)
                if (cloudResult != null) {
                    return@withContext cloudResult
                }
            } catch (_: Exception) {
                // Graceful fallback to on-device neural or deterministic engine
            }
        }

        // 2. On-device local neural model if loaded or requested
        val shouldRunNeural = when (forcedEngineType) {
            EngineType.LOCAL_NEURAL -> neuralRunner.isModelLoaded
            EngineType.DETERMINISTIC_EXPERT -> false
            EngineType.HYBRID -> neuralRunner.isModelLoaded
        }

        if (shouldRunNeural && neuralRunner.isModelLoaded) {
            try {
                return@withContext executeNeuralInference(question, recentTurns, isRomanianEnabled, startTime)
            } catch (_: Exception) {
                // Graceful instant fallback to deterministic expert engine
            }
        }

        // 3. Instant local fallback engine (covers fast-path small talk and freight specifics)
        knowledgeEngine.createTurn(question, isRomanianEnabled, startTime)
    }

    private suspend fun executeNeuralInference(
        question: String,
        recentTurns: List<CopilotTurn>,
        isRomanianEnabled: Boolean,
        startTime: Long
    ): CopilotTurn {
        val prompt = PromptBuilder.buildUserPrompt(question, recentTurns, isRomanianEnabled)
        val generatedText = neuralRunner.generateResponse(prompt)

        val latency = (System.currentTimeMillis() - startTime).coerceAtLeast(100L)
        val dynamicMatch = knowledgeEngine.evaluate(question, isRomanianEnabled)

        val cleanAnswer = if (generatedText.isNotBlank() && generatedText.length > 20) {
            generatedText.trim()
        } else {
            dynamicMatch.answerEn
        }

        return CopilotTurn(
            transcriptEn = question,
            transcriptRo = dynamicMatch.questionRo,
            answerEn = cleanAnswer,
            answerRo = dynamicMatch.answerRo,
            confidence = (dynamicMatch.confidence * 1.05f).coerceAtMost(0.99f),
            latencyMs = latency,
            matchedCategory = "${dynamicMatch.category} (Neural G1)"
        )
    }

    fun setEngineType(type: EngineType) {
        activeEngineType = type
    }

    fun getAvailableModels(): List<AvailableModelInfo> {
        return modelManager.scanAvailableModels()
    }

    override fun close() {
        neuralRunner.close()
    }
}
