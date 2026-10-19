package com.copilot.stealth

import com.copilot.stealth.data.repository.FreightKnowledgeEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FreightKnowledgeEngineTest {

    private lateinit var engine: FreightKnowledgeEngine

    @Before
    fun setUp() {
        engine = FreightKnowledgeEngine()
    }

    @Test
    fun testDetentionScenarioEvaluation() {
        val query = "What is your standard policy when a driver is delayed at a facility for two hours?"
        val result = engine.evaluate(query, isRomanianEnabled = true)

        assertEquals("Detention Policy", result.category)
        assertTrue(result.confidence > 0.5f)
        assertTrue(result.answerEn.contains("two free hours"))
        assertNotNull(result.answerRo)
        assertNotNull(result.questionRo)
    }

    @Test
    fun testTonuScenarioEvaluation() {
        val query = "The broker cancels the load while the driver is already on site. What is your TONU fee?"
        val result = engine.evaluate(query, isRomanianEnabled = true)

        assertEquals("TONU (Truck Ordered Not Used)", result.category)
        assertTrue(result.answerEn.contains("150 to $250"))
    }

    @Test
    fun testRomanianSuppressionWhenDisabled() {
        val query = "What do you check on a rate confirmation before signing?"
        val result = engine.evaluate(query, isRomanianEnabled = false)

        assertEquals("Rate Confirmation Review", result.category)
        assertTrue(result.answerEn.isNotBlank())
        assertNull(result.answerRo)
        assertNull(result.questionRo)
    }

    @Test
    fun testCreateTurnContainsValidLatencyAndMetadata() {
        val query = "Explain property carrying Hours of Service regulations."
        val turn = engine.createTurn(query, isRomanianEnabled = true)

        assertEquals(query, turn.transcriptEn)
        assertEquals("Hours of Service (HOS) & ELD", turn.matchedCategory)
        assertTrue(turn.confidence > 0.5f)
        assertTrue(turn.latencyMs >= 0L)
    }

    @Test
    fun testGenericFallbackForUnmatchedQuery() {
        val query = "Hello can you hear me?"
        val result = engine.evaluate(query, isRomanianEnabled = true)

        assertNotNull(result.category)
        assertTrue(result.answerEn.isNotBlank())
    }
}
