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
                category = "Specialized Freight Experience",
                triggerKeywords = listOf("freight dispatching background", "freight experience", "experience as dispatcher", "how long have you been dispatching freight", "dispatch experience"),
                sampleQuestions = listOf(
                    "Walk me through your freight dispatching background.",
                    "How long have you been dispatching freight?",
                    "What is your direct experience as a freight dispatcher?"
                ),
                answerEn = "I have solid freight dispatching experience managing dry van and reefer loads on DAT and Truckstop, focusing on high RPM and ELD compliance.",
                answerRo = "Am experienta solida in dispatching pentru dry van si reefer pe DAT/Truckstop, negociere tarife si conformitate ELD.",
                questionRo = "Povesteste-mi despre experienta ta specifica in dispatch de marfa."
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

        // Fast-path for conversational greetings, personal intro, work outlook, and universal Q&A
        val fastMatch = matchConversationalFastPath(cleanQuery, isRomanianEnabled)
        if (fastMatch != null) {
            return fastMatch
        }

        val queryTokens = tokenize(cleanQuery)

        if (queryTokens.isEmpty()) {
            return MatchResult(
                category = "Conversational Inquiry",
                confidence = 0.6f,
                answerEn = "Could you please elaborate on that? I would be glad to share my perspective.",
                answerRo = if (isRomanianEnabled) "Puteti detalia putin? As fi bucuros sa va impartasesc perspectiva mea." else null,
                questionRo = if (isRomanianEnabled) "Puteti detalia intrebarea?" else null
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

        if (bestScenario == null || highestScore < 0.20f) {
            return MatchResult(
                category = "Conversational Inquiry",
                confidence = 0.50f,
                answerEn = "Could you please elaborate on that? I would be glad to share my perspective.",
                answerRo = if (isRomanianEnabled) "Puteti detalia putin? As fi bucuros sa va impartasesc perspectiva mea." else null,
                questionRo = if (isRomanianEnabled) "Puteti detalia intrebarea?" else null
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
                if (matchedParts > 0 && (matchedParts.toFloat() / kwParts.size) >= 0.70f) {
                    score += 0.35f * (matchedParts.toFloat() / kwParts.size)
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
        // 1. Job / Career / Work opinions & thoughts
        val isJobOpinion = cleanQuery.contains("think about your job") ||
                cleanQuery.contains("think about your work") ||
                cleanQuery.contains("about your job") ||
                cleanQuery.contains("about your work") ||
                cleanQuery.contains("like your job") ||
                cleanQuery.contains("like your work") ||
                cleanQuery.contains("how is your job") ||
                cleanQuery.contains("how do you feel about your job") ||
                (cleanQuery.contains("job") && (cleanQuery.contains("think") || cleanQuery.contains("feel") || cleanQuery.contains("opinion")))

        if (isJobOpinion) {
            return MatchResult(
                category = "Career & Work Outlook",
                confidence = 0.98f,
                answerEn = "I really enjoy my work because it keeps me engaged, challenges me to solve practical problems, and allows me to deliver tangible value every day.",
                answerRo = if (isRomanianEnabled) "Imi place foarte mult munca mea fiindca este dinamica, ma provoaca sa rezolv probleme practice si imi ofera satisfactie zilnic." else null,
                questionRo = if (isRomanianEnabled) "Ce parere ai despre munca ta?" else null
            )
        }

        // 2. Personal introduction / Say something about you / Tell me about yourself
        val isAboutYou = cleanQuery.contains("about you") ||
                cleanQuery.contains("about yourself") ||
                cleanQuery.contains("say something about you") ||
                cleanQuery.contains("tell me about you") ||
                cleanQuery.contains("who are you") ||
                cleanQuery.contains("introduce yourself") ||
                cleanQuery.contains("describe yourself") ||
                cleanQuery == "about you" ||
                cleanQuery == "who are you"

        if (isAboutYou) {
            return MatchResult(
                category = "Personal Introduction",
                confidence = 0.98f,
                answerEn = "I am a dedicated professional with strong communication and problem-solving skills, always focused on delivering reliable results and collaborating effectively.",
                answerRo = if (isRomanianEnabled) "Sunt o persoana dedicata, cu abilitati bune de comunicare si rezolvare a problemelor, axata pe rezultate de calitate si colaborare." else null,
                questionRo = if (isRomanianEnabled) "Spune ceva despre tine / Cine esti?" else null
            )
        }

        // 3. Greetings & How are you (Ensure it doesn't collide with "job" or "think")
        val isHowAreYou = (cleanQuery.contains("how are you") ||
                cleanQuery.contains("how are you doing") ||
                cleanQuery.contains("how is it going") ||
                cleanQuery.contains("hows it going") ||
                cleanQuery.contains("how are things") ||
                cleanQuery == "how are you" ||
                cleanQuery == "how are u") && !cleanQuery.contains("job") && !cleanQuery.contains("work")

        if (isHowAreYou) {
            return MatchResult(
                category = "Interview Greeting",
                confidence = 0.98f,
                answerEn = "Hello, I am good, thank you, and you?",
                answerRo = if (isRomanianEnabled) "Buna, sunt bine, multumesc! Dumneavoastra ce mai faceti?" else null,
                questionRo = if (isRomanianEnabled) "Buna, ce mai faceti?" else null
            )
        }

        // 4. Pure greetings (Hi, Hello, Hey, Good morning, etc.)
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

        // 5. Strengths & Skills
        val isStrengths = cleanQuery.contains("strength") ||
                cleanQuery.contains("good at") ||
                cleanQuery.contains("what do you do best") ||
                cleanQuery.contains("your skills")

        if (isStrengths) {
            return MatchResult(
                category = "Strengths & Skills",
                confidence = 0.97f,
                answerEn = "My greatest strengths are adaptability, quick problem-solving, attention to detail, and maintaining composure in fast-paced situations.",
                answerRo = if (isRomanianEnabled) "Punctele mele forte sunt adaptabilitatea, rezolvarea rapida a problemelor, atentia la detalii si calmul in situatii presante." else null,
                questionRo = if (isRomanianEnabled) "Care sunt punctele tale forte?" else null
            )
        }

        // 6. Why should we hire you / Why choose you
        val isWhyHire = cleanQuery.contains("why should we hire you") ||
                cleanQuery.contains("why choose you") ||
                cleanQuery.contains("why hire you") ||
                cleanQuery.contains("why work with you")

        if (isWhyHire) {
            return MatchResult(
                category = "Value Proposition",
                confidence = 0.97f,
                answerEn = "I bring a strong work ethic, rapid learning ability, and a proactive attitude that ensures tasks are handled accurately and on time.",
                answerRo = if (isRomanianEnabled) "Aduc o etica profesionala solida, capacitate rapida de invatare si o atitudine proactiva ce asigura livrarea la timp." else null,
                questionRo = if (isRomanianEnabled) "De ce ar trebui sa te alegem?" else null
            )
        }

        // 7. Handling stress & pressure
        val isStress = cleanQuery.contains("handle stress") ||
                cleanQuery.contains("handle pressure") ||
                cleanQuery.contains("under pressure") ||
                cleanQuery.contains("difficult situation")

        if (isStress) {
            return MatchResult(
                category = "Stress Management",
                confidence = 0.97f,
                answerEn = "I stay calm by prioritizing urgent tasks, breaking complex issues into clear steps, and keeping transparent communication with my team.",
                answerRo = if (isRomanianEnabled) "Imi pastrez calmul prioritizand urgentele, impartind problemele in pasi clari si comunicand deschis cu echipa." else null,
                questionRo = if (isRomanianEnabled) "Cum gestionezi stresul si presiunea?" else null
            )
        }

        // 8. Future goals / 5 years
        val isGoals = cleanQuery.contains("5 years") ||
                cleanQuery.contains("five years") ||
                cleanQuery.contains("future goals") ||
                cleanQuery.contains("where do you see yourself")

        if (isGoals) {
            return MatchResult(
                category = "Career Goals",
                confidence = 0.97f,
                answerEn = "My goal is to continually master my craft, take on greater responsibilities, and contribute meaningfully to long-term team growth.",
                answerRo = if (isRomanianEnabled) "Scopul meu este sa ma perfectionez continuu, sa preiau mai multe responsabilitati si sa contribui la cresterea echipei." else null,
                questionRo = if (isRomanianEnabled) "Unde te vezi in 5 ani?" else null
            )
        }

        // 9. Free time & Hobbies
        val isHobbies = cleanQuery.contains("free time") ||
                cleanQuery.contains("hobbies") ||
                cleanQuery.contains("outside of work") ||
                cleanQuery.contains("what do you do for fun")

        if (isHobbies) {
            return MatchResult(
                category = "Interests & Lifestyle",
                confidence = 0.97f,
                answerEn = "In my free time, I enjoy staying active, reading, learning new technologies, and spending quality time with family.",
                answerRo = if (isRomanianEnabled) "In timpul liber imi place sa fac miscare, sa citesc, sa descopar tehnologii noi si sa petrec timp cu familia." else null,
                questionRo = if (isRomanianEnabled) "Ce faci in timpul liber?" else null
            )
        }

        // 10. Audio mic checks: can you hear me
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

        // 11. Polite thanks
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
            if (lower.contains("think about your job") || lower.contains("about your job") || lower.contains("about your work")) return "Ce parere ai despre munca ta?"
            if (lower.contains("tell me about yourself") || lower.contains("about you") || lower.contains("say something about you") || lower.contains("who are you")) return "Spune ceva despre tine / Cine esti?"
            if (lower.contains("how are you")) return "Ce mai faci? / Cum merg lucrurile?"
            if (lower.contains("strengths") || lower.contains("good at")) return "Care sunt punctele tale forte?"
            if (lower.contains("why should we hire you") || lower.contains("why hire you")) return "De ce ar trebui sa te alegem?"
            if (lower.contains("free time") || lower.contains("hobbies")) return "Ce faci in timpul liber?"
            if (lower.contains("handle stress") || lower.contains("pressure")) return "Cum gestionezi stresul?"
            if (lower.contains("5 years") || lower.contains("five years")) return "Unde te vezi in 5 ani?"
            if (lower.contains("what do you do")) return "Cu ce te ocupi?"
            if (lower.contains("good morning")) return "Buna dimineata"
            if (lower.contains("good afternoon")) return "Buna ziua"
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
