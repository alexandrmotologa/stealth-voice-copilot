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
    fun testLayoverScenarioEvaluation() {
        val query = "The warehouse is closed when the driver arrives. What if they cannot unload until next morning?"
        val result = engine.evaluate(query, isRomanianEnabled = true)

        assertEquals("Layover Procedure", result.category)
        assertTrue(result.answerEn.contains("200 to $300"))
        assertNotNull(result.answerRo)
    }

    @Test
    fun testScaleWeightAxleEvaluation() {
        val query = "Your driver scales the truck and the drive axle is 35,500 pounds on the CAT scale. How do you resolve it?"
        val result = engine.evaluate(query, isRomanianEnabled = true)

        assertEquals("Axle Weight & Cat Scales", result.category)
        assertTrue(result.answerEn.contains("80,000"))
        assertTrue(result.answerEn.contains("tandems"))
    }

    @Test
    fun testFactoringInvoicingEvaluation() {
        val query = "What documents must be included in a complete billing packet for factoring NOA?"
        val result = engine.evaluate(query, isRomanianEnabled = true)

        assertEquals("Factoring and Billing Packet", result.category)
        assertTrue(result.answerEn.contains("signed rate confirmation"))
        assertTrue(result.answerEn.contains("proof of delivery"))
    }

    @Test
    fun testLumperReimbursementEvaluation() {
        val query = "How do you handle a $300 lumper fee request at a grocery warehouse with comcheck?"
        val result = engine.evaluate(query, isRomanianEnabled = true)

        assertEquals("Lumper Reimbursement", result.category)
        assertTrue(result.answerEn.contains("Comcheck") || result.answerEn.contains("advance code"))
    }

    @Test
    fun testReeferTemperatureEvaluation() {
        val query = "What is the difference between continuous and start-stop reefer temperature modes for fresh produce?"
        val result = engine.evaluate(query, isRomanianEnabled = true)

        assertEquals("Reefer Temperature Management", result.category)
        assertTrue(result.answerEn.contains("continuous mode"))
    }

    @Test
    fun testBreakdownDelayEvaluation() {
        val query = "Your truck suffers a tire blowout or mechanical breakdown 150 miles from delivery. What do you do?"
        val result = engine.evaluate(query, isRomanianEnabled = true)

        assertEquals("Breakdowns & Mechanical Delays", result.category)
        assertTrue(result.answerEn.contains("roadside") || result.answerEn.contains("broker"))
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

    @Test
    fun testCarrierProfileGrounding() {
        val customProfile = com.copilot.stealth.data.models.CarrierProfile(
            companyName = "Titan Logistics Express LLC",
            mcNumber = "MC-998877",
            dotNumber = "USDOT 1122334",
            fleetSize = 42,
            equipmentTypes = "53ft Reefer Units with continuous cooling"
        )
        val customEngine = FreightKnowledgeEngine(carrierProfile = customProfile)

        val mcQuery = "What is your MC number and company name?"
        val mcResult = customEngine.evaluate(mcQuery, isRomanianEnabled = true)
        assertEquals("Carrier Identity & Authority", mcResult.category)
        assertTrue(mcResult.answerEn.contains("Titan Logistics Express LLC"))
        assertTrue(mcResult.answerEn.contains("MC-998877"))
        assertTrue(mcResult.answerEn.contains("USDOT 1122334"))

        val fleetQuery = "How many trucks do you currently operate in your fleet?"
        val fleetResult = customEngine.evaluate(fleetQuery, isRomanianEnabled = true)
        assertEquals("Fleet & Equipment Specifications", fleetResult.category)
        assertTrue(fleetResult.answerEn.contains("42 late-model power units"))
    }
}

