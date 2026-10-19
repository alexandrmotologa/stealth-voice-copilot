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
            ),
            FreightScenario(
                category = "Interview Greeting & Icebreaker",
                triggerKeywords = listOf("hi", "hello", "how are you", "good morning", "good afternoon", "how are things", "can you hear me", "hey there", "how are you doing"),
                sampleQuestions = listOf(
                    "Hi, how are you?",
                    "Hello, how are you doing today?",
                    "Good morning, can you hear me ok?",
                    "Hi, thanks for joining the call."
                ),
                answerEn = "Hello, I am good, thank you, and you?",
                answerRo = "Buna, sunt bine, multumesc! Dumneavoastra ce mai faceti?",
                questionRo = "Buna, ce mai faci? / Salutare, cum merg lucrurile?"
            ),
            FreightScenario(
                category = "Dispatcher Background & Introduction",
                triggerKeywords = listOf("tell me about yourself", "your background", "experience as dispatcher", "walk me through your resume", "who are you", "how long have you been dispatching"),
                sampleQuestions = listOf(
                    "Can you tell me about yourself?",
                    "Walk me through your freight dispatching background.",
                    "How long have you been dispatching freight?"
                ),
                answerEn = "I have solid freight dispatching experience managing dry van and reefer loads on DAT and Truckstop, focusing on high RPM and ELD compliance.",
                answerRo = "Am experienta solida in dispatching pentru dry van si reefer pe DAT/Truckstop, negociere tarife si conformitate ELD.",
                questionRo = "Povesteste-mi despre tine si experienta ta in dispatch."
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

        // Fast-path for conversational greetings, audio checks, and small talk
        val fastMatch = matchConversationalFastPath(cleanQuery, isRomanianEnabled)
        if (fastMatch != null) {
            return fastMatch
        }

        val queryTokens = tokenize(cleanQuery)

        if (queryTokens.isEmpty()) {
            return MatchResult(
                category = "General Inquiry",
                confidence = 0.6f,
                answerEn = "Could you please repeat that? I am ready to discuss our lanes, rates, and dispatch operations.",
                answerRo = if (isRomanianEnabled) "Puteti repeta? Sunt gata sa discutam despre rute, tarife si disponibilitate." else null,
                questionRo = if (isRomanianEnabled) "Puteti clarifica intrebarea?" else null
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

        if (bestScenario == null || highestScore < 0.15f) {
            return MatchResult(
                category = "General Inquiry",
                confidence = 0.50f,
                answerEn = "Could you please repeat that? I am ready to discuss our lanes, rates, and dispatch operations.",
                answerRo = if (isRomanianEnabled) "Puteti repeta? Sunt gata sa discutam despre rute, tarife si disponibilitate." else null,
                questionRo = if (isRomanianEnabled) "Puteti clarifica intrebarea?" else null
            )
        }

        val scenario = bestScenario
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
            val hasDirectMatch = if (lowerKeyword.length <= 3) {
                Regex("\\b${Regex.escape(lowerKeyword)}\\b").containsMatchIn(rawQuery)
            } else {
                rawQuery.contains(lowerKeyword)
            }

            if (hasDirectMatch) {
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

    private fun matchConversationalFastPath(cleanQuery: String, isRomanianEnabled: Boolean): MatchResult? {
        // Direct checks for how are you / greetings
        val isHowAreYou = cleanQuery.contains("how are you") ||
                cleanQuery.contains("how are you doing") ||
                cleanQuery.contains("how is it going") ||
                cleanQuery.contains("hows it going") ||
                cleanQuery.contains("how are things") ||
                cleanQuery == "how are you" ||
                cleanQuery == "how are u"

        if (isHowAreYou) {
            return MatchResult(
                category = "Interview Greeting",
                confidence = 0.98f,
                answerEn = "Hello, I am good, thank you, and you?",
                answerRo = if (isRomanianEnabled) "Buna, sunt bine, multumesc! Dumneavoastra ce mai faceti?" else null,
                questionRo = if (isRomanianEnabled) "Buna, ce mai faceti?" else null
            )
        }

        // Pure greetings (Hi, Hello, Hey, Good morning, etc.)
        val isGreeting = cleanQuery == "hi" ||
                cleanQuery == "hello" ||
                cleanQuery == "hey" ||
                cleanQuery == "hey there" ||
                cleanQuery.startsWith("good morning") ||
                cleanQuery.startsWith("good afternoon") ||
                cleanQuery.startsWith("good evening")

        if (isGreeting) {
            return MatchResult(
                category = "Interview Greeting",
                confidence = 0.98f,
                answerEn = "Hello! Thank you for having me today.",
                answerRo = if (isRomanianEnabled) "Buna ziua! Va multumesc pentru oportunitate." else null,
                questionRo = if (isRomanianEnabled) "Buna ziua / Salut" else null
            )
        }

        // Audio mic checks: can you hear me
        val isAudioCheck = cleanQuery.contains("can you hear me") ||
                cleanQuery.contains("hear me ok") ||
                cleanQuery.contains("are you there") ||
                cleanQuery.contains("do you hear me")

        if (isAudioCheck) {
            return MatchResult(
                category = "Audio Check",
                confidence = 0.98f,
                answerEn = "Yes, loud and clear! Can you hear me ok?",
                answerRo = if (isRomanianEnabled) "Da, va aud foarte clar! Ma auziti bine?" else null,
                questionRo = if (isRomanianEnabled) "Ma auziti bine?" else null
            )
        }

        // Polite thanks
        val isThanks = cleanQuery == "thank you" ||
                cleanQuery == "thanks" ||
                cleanQuery.startsWith("thank you") ||
                cleanQuery == "thanks a lot"

        if (isThanks) {
            return MatchResult(
                category = "Polite Response",
                confidence = 0.98f,
                answerEn = "You are welcome! Glad to assist.",
                answerRo = if (isRomanianEnabled) "Cu mare placere!" else null,
                questionRo = if (isRomanianEnabled) "Multumesc" else null
            )
        }

        return null
    }

    private fun tokenize(text: String): Set<String> {
        val stopWords = setOf(
            "the", "a", "an", "is", "and", "or", "in", "on", "at", "to", "for", "of", "with"
        )
        return text.split(Regex("[^a-zA-Z0-9]+"))
            .filter { it.isNotBlank() && !stopWords.contains(it) && it.length > 1 }
            .toSet()
    }

    companion object {
        fun quickTranslate(text: String): String {
            val lower = text.trim().lowercase()

            if (lower.startsWith("hi") || lower.startsWith("hello") || lower.startsWith("hey")) {
                if (lower.contains("how are you")) return "Buna, ce mai faci?"
                if (lower.contains("can you hear me")) return "Salut, ma auzi bine?"
                return "Buna ziua / Salut"
            }
            if (lower.contains("how are you")) return "Ce mai faci? / Cum merg lucrurile?"
            if (lower.contains("good morning")) return "Buna dimineata"
            if (lower.contains("good afternoon")) return "Buna ziua"
            if (lower.contains("tell me about yourself")) return "Povesteste-mi despre tine si experienta ta."
            if (lower.contains("what is your mc") || lower.contains("mc number")) return "Care este numarul tau de MC si compania?"
            if (lower.contains("what equipment") || lower.contains("trucks")) return "Ce tip de camioane/echipament aveti in flota?"
            if (lower.contains("rate con") || lower.contains("rate confirmation")) return "Ce verifici pe o confirmare de tarif inainte de semnare?"
            if (lower.contains("detention")) return "Care este politica ta pentru plata orelor de detentie?"
            if (lower.contains("tonu")) return "Cat percepeti pentru anularea comenzii (TONU)?"
            if (lower.contains("layover")) return "Care este tariful de layover daca soferul ramane peste noapte?"
            if (lower.contains("lumper")) return "Cum procedati daca depozitul cere plata cash pentru lumper?"
            if (lower.contains("bol") || lower.contains("bill of lading")) return "Ce face soferul daca marfa nu corespunde cu BOL-ul?"
            if (lower.contains("hos") || lower.contains("eld") || lower.contains("hours of service")) return "Cum planifici orele de condus (HOS/ELD)?"
            if (lower.contains("where are you located")) return "Unde aveti sediul principal?"
            if (lower.contains("who do you factor with") || lower.contains("factoring")) return "Cu ce companie de factoring colaborati?"
            if (lower.contains("deadhead")) return "Cate mile goale (deadhead) acceptati?"
            if (lower.contains("certificate of insurance") || lower.contains("coi")) return "Aveti certificatul de asigurare (COI) pregatit?"

            val dictionary = mapOf(
                "what" to "ce",
                "is" to "este",
                "are" to "sunt",
                "you" to "tu / voi",
                "your" to "al tau / a voastra",
                "rate" to "tarif",
                "load" to "incarcatura",
                "truck" to "camion",
                "driver" to "sofer",
                "broker" to "broker",
                "shipper" to "expeditor",
                "receiver" to "destinatar",
                "pickup" to "incarcare",
                "delivery" to "livrare",
                "miles" to "mile",
                "time" to "timp",
                "money" to "bani",
                "delay" to "intarziere",
                "policy" to "politica",
                "insurance" to "asigurare",
                "available" to "disponibil",
                "today" to "astazi",
                "tomorrow" to "maine",
                "thanks" to "multumesc",
                "thank" to "multumesc"
            )

            val words = lower.split(Regex("\\s+"))
            val translatedWords = words.map { dictionary[it] ?: it }
            return translatedWords.joinToString(" ")
        }
    }
}
