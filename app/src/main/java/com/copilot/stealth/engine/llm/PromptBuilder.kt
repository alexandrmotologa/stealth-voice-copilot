package com.copilot.stealth.engine.llm

import com.copilot.stealth.data.models.CopilotTurn

/**
 * Constructs optimized system and user prompts with token conservation based on language toggles.
 */
object PromptBuilder {

    fun buildSystemPrompt(isRomanianEnabled: Boolean): String {
        val basePrompt = """
            You are an offline interview assistant for a US Freight Dispatcher and Billing Clerk position.
            Domain terms: Load boards (DAT One, Truckstop), Rate Confirmation (Rate Con), Bill of Lading (BOL), Proof of Delivery (POD), Factoring (Notice of Assignment), Detention ($50/hr after 2h standard), Layover ($150-$300/day), TONU ($150-$250), Lumper receipts, HOS (Hours of Service), ELD (KeepTruckin/Motive).
        """.trimIndent()

        return if (isRomanianEnabled) {
            """
            $basePrompt
            
            Rules:
            1. Provide a direct, professional 1 to 2 sentence English response that the candidate can read aloud immediately.
            2. Include the Romanian translation of the question and a brief Romanian explanation of the answer.
            3. Strict JSON format:
            {
              "transcript_ro": "<Romanian translation of question>",
              "answer_en": "<1-2 concise professional sentences in English>",
              "answer_ro": "<brief Romanian explanation of answer>"
            }
            """.trimIndent()
        } else {
            """
            $basePrompt
            
            Rules:
            1. Provide a direct, professional 1 to 2 sentence English response that the candidate can read aloud immediately.
            2. Do NOT generate Romanian text or translations.
            3. Strict JSON format:
            {
              "transcript_ro": null,
              "answer_en": "<1-2 concise professional sentences in English>",
              "answer_ro": null
            }
            """.trimIndent()
        }
    }

    fun buildUserPrompt(
        currentQuestion: String,
        recentTurns: List<CopilotTurn>,
        isRomanianEnabled: Boolean
    ): String {
        val historyContext = if (recentTurns.isNotEmpty()) {
            val formattedTurns = recentTurns.takeLast(2).joinToString("\n") { turn ->
                "Interviewer: ${turn.transcriptEn}\nCandidate: ${turn.answerEn}"
            }
            "Recent Context:\n$formattedTurns\n\n"
        } else {
            ""
        }

        return "${historyContext}Interviewer: \"$currentQuestion\"\nSettings: [Romanian: $isRomanianEnabled]"
    }
}
