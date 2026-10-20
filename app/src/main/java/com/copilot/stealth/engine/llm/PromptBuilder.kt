package com.copilot.stealth.engine.llm

import com.copilot.stealth.data.models.CarrierProfile
import com.copilot.stealth.data.models.CopilotTurn
import com.copilot.stealth.data.models.ProfileMode

/**
 * Constructs prompt templates for on-device and cloud LLM execution,
 * providing direct, natural spoken answers with customizable profile grounding.
 */
object PromptBuilder {

    fun buildSystemPrompt(isRomanianEnabled: Boolean, profile: CarrierProfile = CarrierProfile()): String {
        val identityGrounding = when (profile.profileMode) {
            ProfileMode.GENERAL_CONVERSATIONAL -> """
                CONVERSATION MODE:
                General, unconstrained, highly intelligent voice assistant. Answer freely, naturally, and directly on any topic without predefined domain constraints.
            """.trimIndent()

            ProfileMode.CANDIDATE_INTERVIEW -> if (profile.candidateContext.isNotBlank()) {
                """
                CANDIDATE PROFILE & CONTEXT:
                ${profile.candidateContext.trim()}
                - Style: Speak in first person ('I', 'my experience'), natural, direct, humble yet authoritative.
                """.trimIndent()
            } else {
                """
                CANDIDATE IDENTITY & ROLE:
                - Role: Senior Software & AI Systems Engineer (5+ years experience).
                - Core Tech: Modern web stacks, reactive mobile & backend architectures, distributed services, LLM workflows.
                - Style: Speak in first person ('I', 'my experience'), natural, concise, technically sound, zero buzzwords.
                """.trimIndent()
            }

            ProfileMode.CUSTOM_PROMPT -> if (profile.customPrompt.isNotBlank()) {
                """
                CUSTOM USER CONTEXT & INSTRUCTIONS:
                ${profile.customPrompt.trim()}
                """.trimIndent()
            } else {
                "You are an intelligent, articulate assistant in a live professional conversation."
            }

            ProfileMode.CARRIER_DISPATCH -> """
                CARRIER & FLEET DISPATCH GROUNDING:
                - Carrier: ${profile.companyName} (${profile.mcNumber}, ${profile.dotNumber}), base: ${profile.homeBase}
                - Fleet: ${profile.fleetSize} late-model power units (${profile.equipmentTypes})
                - Factoring: ${profile.factoringCompany}
                - Standard Rates: Detention $${profile.standardDetentionRate}/hr after 2 hrs, TONU $${profile.standardTonuFee}
            """.trimIndent()
        }

        val languageRules = if (isRomanianEnabled) {
            """
            Rules:
            1. Provide a direct, natural, confident spoken response in 1 or 2 concise English sentences that can be read aloud immediately off the teleprompter.
            2. Provide the Romanian translation of the question and a brief Romanian translation of the answer.
            3. Output STRICT JSON only:
            {"transcript_ro":"<Romanian question translation>","answer_en":"<1-2 concise spoken English sentences>","answer_ro":"<Romanian explanation>"}
            """.trimIndent()
        } else {
            """
            Rules:
            1. Provide a direct, natural, confident spoken response in 1 or 2 concise English sentences that can be read aloud immediately off the teleprompter.
            2. Output STRICT JSON only:
            {"transcript_ro":null,"answer_en":"<1-2 concise spoken English sentences>","answer_ro":null}
            """.trimIndent()
        }

        return """
            You are a discreet, intelligent real-time voice teleprompter assistant helping in live interviews and discussions.
            
            $identityGrounding
            
            $languageRules
        """.trimIndent()
    }

    fun buildUserPrompt(
        currentQuestion: String,
        recentTurns: List<CopilotTurn>,
        isRomanianEnabled: Boolean,
        profile: CarrierProfile = CarrierProfile()
    ): String {
        val historyContext = if (recentTurns.isNotEmpty()) {
            val formattedTurns = recentTurns.takeLast(2).joinToString("\n") { turn ->
                "Q: ${turn.transcriptEn}\nA: ${turn.answerEn}"
            }
            "Previous context:\n$formattedTurns\n\n"
        } else {
            ""
        }

        return """
            ${buildSystemPrompt(isRomanianEnabled, profile)}
            
            ${historyContext}Current Question: $currentQuestion
            
            Respond with JSON only:
        """.trimIndent()
    }
}
