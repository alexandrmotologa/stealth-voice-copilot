package com.copilot.stealth.data.repository

import com.copilot.stealth.data.models.CopilotTurn

/**
 * Deterministic expert inference engine that matches incoming queries
 * against US freight dispatch and billing scenarios with high precision.
 */
class FreightKnowledgeEngine(
    private val glossary: List<FreightScenario> = FreightGlossary.scenarios,
    private val carrierProfile: com.copilot.stealth.data.models.CarrierProfile = com.copilot.stealth.data.models.CarrierProfile()
) {

    private val allScenarios: List<FreightScenario> by lazy {
        val carrierScenarios = listOf(
            FreightScenario(
                category = "Carrier Identity & Authority",
                triggerKeywords = listOf("mc number", "mc#", "dot number", "what is your mc", "carrier name", "company name", "operating authority", "who do you dispatch for"),
                sampleQuestions = listOf(
                    "What is your MC number and company name?",
                    "Can you give me your MC and DOT numbers?",
                    "What carrier are you dispatching for?"
                ),
                answerEn = "We operate as ${carrierProfile.companyName} under ${carrierProfile.mcNumber} and ${carrierProfile.dotNumber}, based out of ${carrierProfile.homeBase} with verified active authority.",
                answerRo = "Operam ca ${carrierProfile.companyName}, ${carrierProfile.mcNumber}, ${carrierProfile.dotNumber}, din ${carrierProfile.homeBase} cu autorizatie activa.",
                questionRo = "Care este numarul de MC si compania pentru care faci dispatch?"
            ),
            FreightScenario(
                category = "Fleet & Equipment Specifications",
                triggerKeywords = listOf("how many trucks", "fleet size", "what equipment", "reefer or dry van", "trailer type", "equipment type"),
                sampleQuestions = listOf(
                    "What equipment do you have available?",
                    "How many trucks do you currently operate in your fleet?",
                    "Do you run dry vans or reefers?"
                ),
                answerEn = "Our fleet consists of ${carrierProfile.fleetSize} late-model power units pulling ${carrierProfile.equipmentTypes}.",
                answerRo = "Flota noastra numara ${carrierProfile.fleetSize} camioane moderne ce tracteaza ${carrierProfile.equipmentTypes}.",
                questionRo = "Ce fel de echipament si cate camioane aveti in flota?"
            ),
            FreightScenario(
                category = "Factoring & Payment Partners",
                triggerKeywords = listOf("who do you factor with", "factoring company", "credit check", "triumph", "notice of assignment active"),
                sampleQuestions = listOf(
                    "Who is your factoring company for credit check?",
                    "Who do you factor through?",
                    "Do you have a Notice of Assignment for quick pay?"
                ),
                answerEn = "We factor with ${carrierProfile.factoringCompany}. We can send our NOA and W-9 immediately with the carrier packet.",
                answerRo = "Facturam prin ${carrierProfile.factoringCompany}. Trimitem NOA si formularul W-9 imediat in pachetul de setup.",
                questionRo = "Cu ce companie de factoring colaborati pentru plata?"
            )
        )
        carrierScenarios + glossary
    }

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

        for (scenario in allScenarios) {
            val score = calculateScore(queryTokens, cleanQuery, scenario)
            if (score > highestScore) {
                highestScore = score
                bestScenario = scenario
            }
        }

        val scenario = bestScenario ?: allScenarios.first()
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

        // Keyword trigger presence with fuzzy distance for heavy accents
        for (keyword in scenario.triggerKeywords) {
            val lowerKeyword = keyword.lowercase()
            if (rawQuery.contains(lowerKeyword)) {
                score += 0.40f
            } else {
                val kwParts = lowerKeyword.split(" ")
                val matchedParts = kwParts.count { kw ->
                    queryTokens.any { q ->
                        q == kw || (kw.length >= 4 && q.length >= 4 && levenshteinDistance(q, kw) <= 2)
                    }
                }
                if (matchedParts > 0) {
                    score += 0.25f * (matchedParts.toFloat() / kwParts.size)
                }
            }
        }

        // Direct overlap with sample questions with fuzzy token tolerance
        for (sample in scenario.sampleQuestions) {
            val sampleTokens = tokenize(sample.lowercase())
            val matchedCount = sampleTokens.count { s ->
                queryTokens.any { q -> q == s || (s.length >= 5 && q.length >= 5 && levenshteinDistance(q, s) <= 1) }
            }
            val union = queryTokens.union(sampleTokens).size
            if (union > 0) {
                val jaccard = matchedCount.toFloat() / union.toFloat()
                if (jaccard > 0.12f) {
                    score += jaccard * 0.50f
                }
            }
        }

        return score
    }

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[s1.length][s2.length]
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
