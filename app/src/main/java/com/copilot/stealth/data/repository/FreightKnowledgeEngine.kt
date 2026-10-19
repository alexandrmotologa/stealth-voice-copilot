package com.copilot.stealth.data.repository

import com.copilot.stealth.data.models.CopilotTurn

/**
 * Deterministic expert inference engine that matches incoming queries
 * against US freight dispatch and billing scenarios with high precision.
 */
class FreightKnowledgeEngine(
    private val glossary: List<FreightScenario> = FreightGlossary.scenarios
) {

    data class MatchResult(
        val category: String,
        val confidence: Float,
        val answerEn: String,
        val answerRo: String?,
        val questionRo: String?
    )

    fun evaluate(query: String, isRomanianEnabled: Boolean): MatchResult {
        val cleanQuery = query.trim().lowercase()
        val queryTokens = tokenize(cleanQuery)

        if (queryTokens.isEmpty()) {
            return MatchResult(
                category = "General Logistics",
                confidence = 0.5f,
                answerEn = "I maintain active communication with brokers and drivers, verify all rate confirmations and appointment windows, and resolve accessorial delays proactively.",
                answerRo = if (isRomanianEnabled) "Mentin comunicarea constanta cu brokerii si soferii, verific toate confirmarile si rezolv prompt intarzierile." else null,
                questionRo = if (isRomanianEnabled) "Cum gestionezi operatiunile generale de dispatch?" else null
            )
        }

        var bestScenario: FreightScenario? = null
        var highestScore = 0.0f

        for (scenario in glossary) {
            val score = calculateScore(queryTokens, cleanQuery, scenario)
            if (score > highestScore) {
                highestScore = score
                bestScenario = scenario
            }
        }

        val scenario = bestScenario ?: glossary.first()
        val normalizedConfidence = highestScore.coerceIn(0.45f, 0.98f)

        return MatchResult(
            category = scenario.category,
            confidence = normalizedConfidence,
            answerEn = scenario.answerEn,
            answerRo = if (isRomanianEnabled) scenario.answerRo else null,
            questionRo = if (isRomanianEnabled) scenario.questionRo else null
        )
    }

    fun createTurn(
        transcript: String,
        isRomanianEnabled: Boolean,
        startTimeMs: Long = System.currentTimeMillis()
    ): CopilotTurn {
        val match = evaluate(transcript, isRomanianEnabled)
        val latency = (System.currentTimeMillis() - startTimeMs).coerceAtLeast(1L)

        return CopilotTurn(
            transcriptEn = transcript,
            transcriptRo = match.questionRo,
            answerEn = match.answerEn,
            answerRo = match.answerRo,
            confidence = match.confidence,
            latencyMs = latency,
            matchedCategory = match.category
        )
    }

    private fun calculateScore(
        queryTokens: Set<String>,
        rawQuery: String,
        scenario: FreightScenario
    ): Float {
        var score = 0.0f

        // Keyword trigger presence
        for (keyword in scenario.triggerKeywords) {
            val lowerKeyword = keyword.lowercase()
            if (rawQuery.contains(lowerKeyword)) {
                score += 0.40f
            } else {
                val kwParts = lowerKeyword.split(" ")
                val matchedParts = kwParts.count { queryTokens.contains(it) }
                if (matchedParts > 0) {
                    score += 0.20f * (matchedParts.toFloat() / kwParts.size)
                }
            }
        }

        // Direct overlap with sample questions
        for (sample in scenario.sampleQuestions) {
            val sampleTokens = tokenize(sample.lowercase())
            val intersection = queryTokens.intersect(sampleTokens).size
            val union = queryTokens.union(sampleTokens).size
            if (union > 0) {
                val jaccard = intersection.toFloat() / union.toFloat()
                if (jaccard > 0.15f) {
                    score += jaccard * 0.50f
                }
            }
        }

        return score
    }

    private fun tokenize(text: String): Set<String> {
        val stopWords = setOf(
            "the", "a", "an", "is", "are", "do", "you", "what", "how", "when",
            "where", "why", "if", "and", "or", "in", "on", "at", "to", "for",
            "with", "my", "your", "our", "their", "of", "it", "this", "that"
        )
        return text.split(Regex("[^a-zA-Z0-9]+"))
            .filter { it.isNotBlank() && !stopWords.contains(it) && it.length > 1 }
            .toSet()
    }
}
