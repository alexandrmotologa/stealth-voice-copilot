package com.copilot.stealth.data.models

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Represents a single question-and-answer interaction turn in the dialogue.
 */
@Serializable
data class CopilotTurn(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val transcriptEn: String,
    val transcriptRo: String? = null,
    val answerEn: String,
    val answerRo: String? = null,
    val translationLangCode: String? = "RO",
    val followUpQuestions: List<String> = emptyList(),
    val confidence: Float = 1.0f,
    val latencyMs: Long = 0L,
    val matchedCategory: String? = null
)
