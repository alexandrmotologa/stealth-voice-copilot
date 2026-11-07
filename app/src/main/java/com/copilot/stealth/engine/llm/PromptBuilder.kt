package com.copilot.stealth.engine.llm

import com.copilot.stealth.data.models.CarrierProfile
import com.copilot.stealth.data.models.CopilotTurn
import com.copilot.stealth.data.models.GuidanceLanguage
import com.copilot.stealth.data.models.ProfileMode
import com.copilot.stealth.data.models.PrompterStyle

/**
 * Constructs prompt templates for on-device and cloud LLM execution (Gemini, Groq, Local Neural),
 * providing real-time Romanian-to-English translation, intelligent question detection,
 * and direct natural spoken answers with customizable profile grounding.
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
                - In 'answer_en', provide a natural conversational opening hook sentence, followed by 2-3 crisp bullet points starting with '• ' highlighting key technical principles, tradeoffs, or metrics in bold/caps so the speaker can glance and talk naturally.
            """.trimIndent()
            PrompterStyle.FULL_SENTENCE -> """
                - In 'answer_en', provide 1 to 2 fluent, confident, natural spoken broadcast English sentences that can be read aloud verbatim off the screen.
            """.trimIndent()
        }

        val isGuidanceActive = isRomanianEnabled && profile.guidanceLanguage != GuidanceLanguage.OFF
        val lang = profile.guidanceLanguage

        val languageRules = if (isGuidanceActive) {
            """
            MULTILINGUAL & QUESTION DETECTION PROTOCOL:
            1. The interlocutor may speak in Romanian, English, or a mix. You must accurately understand both languages.
            2. ASR PHONETIC DE-BLEEDING: Incoming utterances are captured in real-time by a microphone speech recognizer and frequently suffer from cross-lingual phonetic bleed, accents, or phoneme substitutions (e.g. English 'hi' transcribed as Romanian 'hai' such as 'hai how are you', 'why' transcribed as 'uai' or 'oai', 'see' as 'si', or technical terms phonetically spelled in Romanian).
               You MUST deduce the true intended utterance:
               - If it was intended as English (e.g. 'hai how are you', 'uai did you do that'), correct it into proper English ('Hi, how are you?').
               - If it was spoken in Romanian (e.g. 'salut ce faci', 'cum lucrezi cu react'), understand and preserve it as Romanian.
            3. 'transcript_ro': The authentic Romanian utterance (if spoken in Romanian), OR an accurate, natural Romanian translation if spoken in English.
            4. 'transcript_en': The authentic, polished English utterance (if spoken in English, correcting any phonetic ASR errors like 'hai how are you' -> 'Hi, how are you?'), OR an accurate English translation if spoken in Romanian.
            5. 'is_question': Boolean. Set to TRUE whenever the utterance is an inquiry, question, directive, prompt, or interview request (e.g. 'tell me about yourself', 'spune-mi ceva despre tine', 'descrie experiența ta', 'how do you handle...', 'ce tehnologii folosești'). Set to FALSE ONLY for passive acknowledgments or conversational fillers ('ok', 'da', 'mulțumesc', 'I understand').
            6. 'answer_en':
               - IF is_question is true:
                 $formatRule
               - IF is_question is false:
                 Provide a natural, conversational 1-sentence English response acknowledging or answering what was said.
            7. 'answer_ro': Provide a fluent ${lang.displayName} translation and coaching summary of the English answer, explaining key points to emphasize.
            8. 'follow_ups': 1-2 likely technical follow-up questions if it is a question, or empty list if not.
            9. Output STRICT JSON only:
            {"is_question":true,"transcript_ro":"<${lang.displayName} text>","transcript_en":"<English translation>","answer_en":"<English response>","answer_ro":"<${lang.displayName} coaching>","follow_ups":["<follow-up 1>","<follow-up 2>"]}
            """.trimIndent()
        } else {
            """
            MULTILINGUAL & QUESTION DETECTION PROTOCOL:
            1. The interlocutor may speak in Romanian, English, or a mix.
            2. ASR PHONETIC DE-BLEEDING: Correct any speech recognizer phonetic artifacts (e.g. 'hai how are you' -> 'Hi, how are you?') before processing.
            3. 'transcript_ro': null
            4. 'transcript_en': Accurate, high-quality English translation of what was spoken (or original English, cleaned of phonetic ASR slips).
            5. 'is_question': Boolean. Set to TRUE for questions, interview prompts, or directives (e.g. 'tell me about yourself', 'spune-mi ceva despre tine').
            6. 'answer_en':
               - IF is_question is true:
                 $formatRule
               - IF is_question is false:
                 Provide a natural, conversational English acknowledgment / reflection.
            7. 'answer_ro': null
            8. 'follow_ups': 1-2 likely technical follow-up questions if question, or empty list if not.
            9. Output STRICT JSON only:
            {"is_question":true,"transcript_ro":null,"transcript_en":"<English translation>","answer_en":"<English response>","answer_ro":null,"follow_ups":["<follow-up 1>","<follow-up 2>"]}
            """.trimIndent()
        }

        return """
            You are a discreet, intelligent real-time voice teleprompter and translation assistant helping in live interviews and discussions.
            
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
                val q = turn.transcriptRo ?: turn.transcriptEn
                "Q: $q\nA: ${turn.answerEn}"
            }
            "Previous context:\n$formattedTurns\n\n"
        } else {
            ""
        }

        return """
            ${buildSystemPrompt(isRomanianEnabled, profile)}
            
            ${historyContext}Current Utterance: $currentQuestion
            
            Respond with JSON only:
        """.trimIndent()
    }
}
