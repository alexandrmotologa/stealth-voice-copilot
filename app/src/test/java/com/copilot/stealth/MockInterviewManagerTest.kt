package com.copilot.stealth

import com.copilot.stealth.engine.mock.MockInterviewManager
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MockInterviewManagerTest {

    private val mockManager = MockInterviewManager()

    @Test
    fun testGetRandomQuestionGeneratesValidSession() {
        val session = mockManager.getRandomQuestion()
        assertNotNull(session.scenario)
        assertTrue(session.questionEn.isNotBlank())
        assertTrue(session.questionRo.isNotBlank())
        assertTrue(session.optimalAnswerEn.isNotBlank())
        assertTrue(session.keyPointsExpected.isNotEmpty())
    }

    @Test
    fun testEvaluateAnswerHighReadiness() {
        val session = mockManager.getRandomQuestion()
        // Provide answer covering key trigger keywords
        val candidateResponse = session.keyPointsExpected.joinToString(" ") + " We verify all procedures according to policy."
        val evaluated = mockManager.evaluateAnswer(session, candidateResponse)

        assertTrue(evaluated.scorePercentage >= 70)
        assertTrue(evaluated.feedback.isNotBlank())
    }

    @Test
    fun testEvaluateAnswerShortFallback() {
        val session = mockManager.getRandomQuestion()
        val evaluated = mockManager.evaluateAnswer(session, "no idea")

        assertTrue(evaluated.scorePercentage < 70)
        assertTrue(evaluated.feedback.contains("NEEDS REVISION") || evaluated.feedback.contains("Mention"))
    }
}
