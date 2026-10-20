package com.copilot.stealth.engine.llm

import com.copilot.stealth.data.models.CopilotTurn

/**
 * Constructs prompt templates for on-device and cloud LLM execution,
 * providing direct, natural spoken answers on any topic.
 */
object PromptBuilder {

    fun buildSystemPrompt(isRomanianEnabled: Boolean): String {
        return if (isRomanianEnabled) {
            """
            You are a discreet, intelligent real-time voice assistant helping in professional conversations and interviews.
            Rules:
            1. Provide a direct, natural, confident spoken response in 1 or 2 concise English sentences that the user can read aloud immediately.
            2. Provide the Romanian translation of the question and a brief Romanian translation of the answer.
            3. Respond to ANY topic naturally without predefined constraints (technology, personal background, work opinions, general knowledge, or business).
            4. Output STRICT JSON only:
            {"transcript_ro":"<Romanian question translation>","answer_en":"<1-2 concise spoken English sentences>","answer_ro":"<Romanian explanation>"}
            """.trimIndent()
        } else {
            """
            You are a discreet, intelligent real-time voice assistant helping in professional conversations and interviews.
            Rules:
            1. Provide a direct, natural, confident spoken response in 1 or 2 concise English sentences that the user can read aloud immediately.
            2. Respond to ANY topic naturally without predefined constraints.
            3. Output STRICT JSON only:
            {"transcript_ro":null,"answer_en":"<1-2 concise spoken English sentences>","answer_ro":null}
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

        val jsonRequirement = if (isRomanianEnabled) {
            """Respond with JSON format: {"transcript_ro":"...", "answer_en":"...", "answer_ro":"..."}"""
        } else {
            """Respond with JSON format: {"transcript_ro":null, "answer_en":"...", "answer_ro":null}"""
        }

        return """
            ${buildSystemPrompt(isRomanianEnabled)}
            
            ${historyContext}Current Question: $currentQuestion
            
            $jsonRequirement
        """.trimIndent()
    }
}
