package com.copilot.stealth.engine.llm

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ParsedLlmOutput(
    val transcript_ro: String? = null,
    val answer_en: String? = null,
    val answer_ro: String? = null
)

/**
 * Resilient JSON parser for model responses with automated markdown cleanup and error recovery.
 */
object StructuredResponseParser {

    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    fun parse(rawResponse: String): ParsedLlmOutput {
        val cleaned = cleanResponseText(rawResponse)

        return try {
            jsonConfig.decodeFromString<ParsedLlmOutput>(cleaned)
        } catch (_: Exception) {
            // Regex extraction fallback for malformed or truncated JSON
            extractWithRegex(cleaned)
        }
    }

    private fun cleanResponseText(text: String): String {
        var result = text.trim()

        // Strip markdown code fences if present
        if (result.startsWith("```json")) {
            result = result.removePrefix("```json")
        } else if (result.startsWith("```")) {
            result = result.removePrefix("```")
        }

        if (result.endsWith("```")) {
            result = result.removeSuffix("```")
        }

        result = result.trim()

        // Find outer curly braces if wrapped in extraneous thoughts or commentary
        val firstBrace = result.indexOf('{')
        val lastBrace = result.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            result = result.substring(firstBrace, lastBrace + 1)
        }

        return result
    }

    private fun extractWithRegex(text: String): ParsedLlmOutput {
        val answerEnRegex = Regex("\"answer_en\"\\s*:\\s*\"([^\"]+)\"")
        val transcriptRoRegex = Regex("\"transcript_ro\"\\s*:\\s*\"([^\"]+)\"")
        val answerRoRegex = Regex("\"answer_ro\"\\s*:\\s*\"([^\"]+)\"")

        val answerEn = answerEnRegex.find(text)?.groupValues?.getOrNull(1)
            ?: text.take(180) // fallback to text itself if completely unformatted

        val transcriptRo = transcriptRoRegex.find(text)?.groupValues?.getOrNull(1)
        val answerRo = answerRoRegex.find(text)?.groupValues?.getOrNull(1)

        return ParsedLlmOutput(
            transcript_ro = transcriptRo,
            answer_en = answerEn,
            answer_ro = answerRo
        )
    }
}
