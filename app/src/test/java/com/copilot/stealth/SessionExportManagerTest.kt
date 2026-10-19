package com.copilot.stealth

import com.copilot.stealth.data.models.CopilotTurn
import com.copilot.stealth.data.repository.SessionExportManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionExportManagerTest {

    private val exportManager = SessionExportManager()

    @Test
    fun testPlainTextFormatting() {
        val turns = listOf(
            CopilotTurn(
                transcriptEn = "What is your detention rate?",
                transcriptRo = "Care este rata de detentie?",
                answerEn = "Our detention rate is $50/hr after 2 free hours.",
                answerRo = "Rata noastra este $50/ora dupa 2 ore libere.",
                confidence = 0.95f,
                latencyMs = 24L,
                matchedCategory = "Detention Policy"
            )
        )

        val formatted = exportManager.formatPlainText(turns)
        assertTrue(formatted.contains("STEALTH VOICE COPILOT INTERVIEW TRANSCRIPT"))
        assertTrue(formatted.contains("Detention Policy"))
        assertTrue(formatted.contains("What is your detention rate?"))
        assertTrue(formatted.contains("$50/hr"))
        assertTrue(formatted.contains("24ms latency"))
    }

    @Test
    fun testAesGcmEncryptionDecryptionRoundTrip() {
        val originalText = "Apex Freight Logistics MC-1094821 Rate Confirmed at $2,800 with Triumph Factoring"
        val encryptedBytes = exportManager.encryptSession(originalText)

        // Must be larger than IV (12 bytes) and not equal to plain text
        assertTrue(encryptedBytes.size > 12)
        assertNotEquals(originalText, String(encryptedBytes))

        // Decryption produces exact original plaintext
        val decryptedText = exportManager.decryptSession(encryptedBytes)
        assertEquals(originalText, decryptedText)
    }

    @Test
    fun testEmptyTurnsFormatting() {
        val formatted = exportManager.formatPlainText(emptyList())
        assertEquals("NO_TURNS_RECORDED", formatted)
    }
}
