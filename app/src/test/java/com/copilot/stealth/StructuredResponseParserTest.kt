package com.copilot.stealth

import com.copilot.stealth.engine.llm.StructuredResponseParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class StructuredResponseParserTest {

    @Test
    fun testParseCleanJson() {
        val json = """
            {
              "transcript_ro": "Care este politica ta de detentie?",
              "answer_en": "Standard policy is two free hours.",
              "answer_ro": "Standardul este de 2 ore libere."
            }
        """.trimIndent()

        val parsed = StructuredResponseParser.parse(json)
        assertEquals("Standard policy is two free hours.", parsed.answer_en)
        assertEquals("Care este politica ta de detentie?", parsed.transcript_ro)
        assertEquals("Standardul este de 2 ore libere.", parsed.answer_ro)
    }

    @Test
    fun testParseMarkdownWrappedJson() {
        val markdownJson = """
            Here is your answer:
            ```json
            {
              "transcript_ro": null,
              "answer_en": "We bill $150 TONU if cancelled after dispatch.",
              "answer_ro": null
            }
            ```
        """.trimIndent()

        val parsed = StructuredResponseParser.parse(markdownJson)
        assertEquals("We bill $150 TONU if cancelled after dispatch.", parsed.answer_en)
        assertNull(parsed.transcript_ro)
        assertNull(parsed.answer_ro)
    }

    @Test
    fun testRegexExtractionOnMalformedJson() {
        val malformed = """
            Sure! "answer_en": "I verify the gross rate and appointment times before signing." Some trailing text without braces
        """.trimIndent()

        val parsed = StructuredResponseParser.parse(malformed)
        assertEquals("I verify the gross rate and appointment times before signing.", parsed.answer_en)
    }

    @Test
    fun testUnformattedTextFallback() {
        val plainText = "Standard detention rate is fifty dollars per hour after two hours."
        val parsed = StructuredResponseParser.parse(plainText)

        assertNotNull(parsed.answer_en)
        assertEquals(plainText, parsed.answer_en)
    }
}
