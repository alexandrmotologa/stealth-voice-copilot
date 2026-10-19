package com.copilot.stealth.engine.mock

import com.copilot.stealth.data.repository.FreightGlossary
import com.copilot.stealth.data.repository.FreightScenario

data class MockQuestionSession(
    val scenario: FreightScenario,
    val questionEn: String,
    val questionRo: String,
    val optimalAnswerEn: String,
    val keyPointsExpected: List<String>,
    var candidateResponse: String? = null,
    var scorePercentage: Int = 0,
    var feedback: String = ""
)

/**
 * Interactive logistics interview simulator that generates random scenarios,
 * evaluates candidate responses against freight domain standards, and tracks readiness.
 */
class MockInterviewManager {

    private val allScenarios = FreightGlossary.scenarios
    private var currentSessionIndex = 0

    fun getRandomQuestion(): MockQuestionSession {
        val scenario = allScenarios.random()
        val question = scenario.sampleQuestions.random()
        return MockQuestionSession(
            scenario = scenario,
            questionEn = question,
            questionRo = scenario.questionRo,
            optimalAnswerEn = scenario.answerEn,
            keyPointsExpected = scenario.triggerKeywords.take(4)
        )
    }

    fun evaluateAnswer(session: MockQuestionSession, candidateText: String): MockQuestionSession {
        val lowerText = candidateText.lowercase()
        var matchedPoints = 0
        val missingPoints = mutableListOf<String>()

        for (point in session.keyPointsExpected) {
            if (lowerText.contains(point.lowercase())) {
                matchedPoints++
            } else {
                missingPoints.add(point)
            }
        }

        val baseScore = if (session.keyPointsExpected.isNotEmpty()) {
            ((matchedPoints.toDouble() / session.keyPointsExpected.size) * 100).toInt()
        } else {
            70
        }

        val lengthBonus = (candidateText.split(" ").size * 2).coerceAtMost(20)
        val finalScore = (baseScore + lengthBonus).coerceIn(10, 100)

        val feedbackMsg = when {
            finalScore >= 85 -> "EXCELLENT: Solid professional freight terminology and accessorial policy coverage."
            finalScore >= 60 -> "GOOD: Covered core concept. Mention: ${missingPoints.joinToString(", ")}."
            else -> "NEEDS REVISION: Remember standard industry rules (${session.keyPointsExpected.joinToString(", ")})."
        }

        return session.copy(
            candidateResponse = candidateText,
            scorePercentage = finalScore,
            feedback = feedbackMsg
        )
    }
}
