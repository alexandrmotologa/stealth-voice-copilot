package com.copilot.stealth

import com.copilot.stealth.engine.calculator.FreightCalculatorEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FreightCalculatorEngineTest {

    @Test
    fun testDetentionWithStandardWait() {
        // Driver scheduled at 08:00, in at 08:00, out at 13:30 (5.5h total, 2h free -> 3.5h billable at $50/h = $175)
        val result = FreightCalculatorEngine.calculateDetention(
            appointmentTime = "08:00",
            inTime = "08:00",
            outTime = "13:30",
            hourlyRate = 50.0,
            freeHours = 2.0
        )

        assertEquals(5.5, result.totalWaitHours, 0.01)
        assertEquals(2.0, result.freeHours, 0.01)
        assertEquals(3.5, result.billableHours, 0.01)
        assertEquals(175.0, result.billableAmount, 0.01)
        assertTrue(result.formattedQuote.contains("$175"))
    }

    @Test
    fun testDetentionEarlyArrivalStartsAtAppointment() {
        // Driver arrived early at 07:15 for 08:00 appt, left at 11:00
        // Wait from 08:00 to 11:00 is 3.0h -> 2h free, 1.0h billable at $50/h = $50
        val result = FreightCalculatorEngine.calculateDetention(
            appointmentTime = "08:00",
            inTime = "07:15",
            outTime = "11:00",
            hourlyRate = 50.0,
            freeHours = 2.0
        )

        assertEquals(3.0, result.totalWaitHours, 0.01)
        assertEquals(1.0, result.billableHours, 0.01)
        assertEquals(50.0, result.billableAmount, 0.01)
    }

    @Test
    fun testDetentionWithinFreeTime() {
        // Wait 1.5 hours (< 2.0h free)
        val result = FreightCalculatorEngine.calculateDetention(
            appointmentTime = "09:00",
            inTime = "09:00",
            outTime = "10:30",
            hourlyRate = 50.0,
            freeHours = 2.0
        )

        assertEquals(1.5, result.totalWaitHours, 0.01)
        assertEquals(0.0, result.billableHours, 0.01)
        assertEquals(0.0, result.billableAmount, 0.01)
        assertTrue(result.formattedQuote.contains("No detention billable"))
    }

    @Test
    fun testRpmCalculation() {
        // $3,000 gross for 1,000 loaded miles + 200 deadhead miles = 1,200 total miles
        // Gross RPM = $2.50 | Loaded RPM = $3.00
        val result = FreightCalculatorEngine.calculateRpm(
            totalRate = 3000.0,
            loadedMiles = 1000.0,
            deadheadMiles = 200.0,
            fuelPricePerGallon = 3.80,
            avgMpg = 6.0
        )

        assertEquals(1200.0, result.totalMiles, 0.01)
        assertEquals(2.50, result.grossRpm, 0.01)
        assertEquals(3.00, result.loadedRpm, 0.01)
        assertEquals(760.0, result.estimatedFuelCost, 0.01) // 1200/6 * 3.80 = 760
        assertEquals(2240.0, result.netRevenueAfterFuel, 0.01)
    }

    @Test
    fun testHosReachLegalTrip() {
        // 9 hours remaining on drive clock, trip is 450 miles at 55 mph
        // Required = 450/55 = 8.18 hours -> Legal!
        val result = FreightCalculatorEngine.calculateHosReach(
            remainingDriveHours = 9.0,
            tripMiles = 450.0,
            avgSpeedMph = 55.0
        )

        assertTrue(result.isLegal)
        assertEquals(495.0, result.maxMilesReach, 0.01)
        assertTrue(result.requiresBreak) // > 8.0h
    }

    @Test
    fun testHosReachIllegalTrip() {
        // 6 hours remaining, trip is 500 miles at 55 mph (needs 9.09h)
        val result = FreightCalculatorEngine.calculateHosReach(
            remainingDriveHours = 6.0,
            tripMiles = 500.0,
            avgSpeedMph = 55.0
        )

        assertFalse(result.isLegal)
        assertTrue(result.formattedSummary.contains("ILLEGAL"))
    }
}
