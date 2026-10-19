package com.copilot.stealth

import com.copilot.stealth.engine.stt.FreightLexiconNormalizer
import org.junit.Assert.assertEquals
import org.junit.Test

class FreightLexiconNormalizerTest {

    @Test
    fun testNormalizeRateCon() {
        val input = "Please send over the ray con for this dry van load"
        val output = FreightLexiconNormalizer.normalize(input)
        assertEquals("Please send over the Rate Confirmation for this dry van load", output)
    }

    @Test
    fun testNormalizeBolAndPod() {
        val input = "Make sure the driver gets the b o l signed as pee oh dee"
        val output = FreightLexiconNormalizer.normalize(input)
        assertEquals("Make sure the driver gets the BOL signed as POD", output)
    }

    @Test
    fun testNormalizeTonu() {
        val input = "Can we bill a toe new for this cancellation"
        val output = FreightLexiconNormalizer.normalize(input)
        assertEquals("Can we bill a TONU for this cancellation", output)
    }

    @Test
    fun testNormalizeHosAndEld() {
        val input = "Verify the driver h o s on keep truckin before booking"
        val output = FreightLexiconNormalizer.normalize(input)
        assertEquals("Verify the driver HOS on Motive/KeepTruckin before booking", output)
    }
}
