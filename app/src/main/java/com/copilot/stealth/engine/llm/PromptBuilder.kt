package com.copilot.stealth.engine.llm

import com.copilot.stealth.data.models.CarrierProfile
import com.copilot.stealth.data.models.CopilotTurn
import com.copilot.stealth.data.models.GuidanceLanguage
import com.copilot.stealth.data.models.ProfileMode
import com.copilot.stealth.data.models.PrompterStyle

/**
 * Constructs prompt templates for on-device and cloud LLM execution,
 * providing direct, natural spoken answers with customizable profile grounding.
 */
object PromptBuilder {

    fun buildSystemPrompt(isRomanianEnabled: Boolean, profile: CarrierProfile = CarrierProfile()): String {
        val targetCompanyGrounding = if (profile.targetCompany.isNotBlank() || profile.targetRole.isNotBlank() || profile.jobDescription.isNotBlank()) {
            """
            
            TARGET INTERVIEW SCOPE:
            ${if (profile.targetCompany.isNotBlank()) "- Target Company: ${profile.targetCompany}" else ""}
            ${if (profile.targetRole.isNotBlank()) "- Target Role: ${profile.targetRole}" else ""}
            ${if (profile.jobDescription.isNotBlank()) "- Job Focus / Stack Requirements: ${profile.jobDescription}" else ""}
            Tailor technical depth, tradeoffs, and system scale to match this exact organization and seniority.
            """.trimIndent()
        } else ""

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
                $targetCompanyGrounding
                """.trimIndent()
            } else {
                """
                CANDIDATE IDENTITY & ROLE:
                - Role: Senior Software & AI Systems Engineer (5+ years experience).
                - Core Tech: Modern web stacks, reactive mobile & backend architectures, distributed services, LLM workflows.
                - Style: Speak in first person ('I', 'my experience'), natural, concise, technically sound, zero buzzwords.
                $targetCompanyGrounding
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

        val formatRule = when (profile.prompterStyle) {
            PrompterStyle.ANCHOR_BULLETS -> """
                1. In 'answer_en', provide a natural conversational opening hook sentence, followed by 2-3 crisp bullet points starting with '• ' highlighting key technical principles, tradeoffs, or metrics in bold/caps so the candidate can glance and talk naturally.
            """.trimIndent()
            PrompterStyle.FULL_SENTENCE -> """
                1. In 'answer_en', provide 1 to 2 fluent, confident, natural spoken broadcast English sentences that can be read aloud verbatim off the screen.
            """.trimIndent()
        }

        val isGuidanceActive = isRomanianEnabled && profile.guidanceLanguage != GuidanceLanguage.OFF
        val lang = profile.guidanceLanguage

        val languageRules = if (isGuidanceActive) {
            """
            Rules:
            $formatRule
            2. In 'transcript_ro', provide a concise ${lang.displayName} translation of the heard question.
            3. In 'answer_ro', provide a ${lang.displayName} coaching summary highlighting what points to emphasize.
            4. In 'follow_ups', provide 1-2 likely technical follow-up questions the interviewer may ask next.
            5. Output STRICT JSON only:
            {"transcript_ro":"<${lang.displayName} translation>","answer_en":"<response>","answer_ro":"<${lang.displayName} coaching>","follow_ups":["<likely follow-up 1>","<likely follow-up 2>"]}
            """.trimIndent()
        } else {
            """
            Rules:
            $formatRule
            2. In 'follow_ups', provide 1-2 likely technical follow-up questions the interviewer may ask next.
            3. Output STRICT JSON only:
            {"transcript_ro":null,"answer_en":"<response>","answer_ro":null,"follow_ups":["<likely follow-up 1>","<likely follow-up 2>"]}
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
