package com.copilot.stealth.engine.calculator

import java.util.Locale
import kotlin.math.roundToInt

data class DetentionResult(
    val totalWaitHours: Double,
    val freeHours: Double,
    val billableHours: Double,
    val billableAmount: Double,
    val hourlyRate: Double,
    val formattedQuote: String
)

data class RpmResult(
    val grossRpm: Double,
    val loadedRpm: Double,
    val totalMiles: Double,
    val estimatedFuelCost: Double,
    val netRevenueAfterFuel: Double,
    val formattedSummary: String
)

data class HosReachResult(
    val isLegal: Boolean,
    val maxMilesReach: Double,
    val requiredDriveHours: Double,
    val requiresBreak: Boolean,
    val formattedSummary: String
)

/**
 * High-speed mathematical engine for US freight dispatching calculations:
 * Detention billing, Rate Per Mile (RPM), and FMCSA Hours of Service transit reach.
 */
object FreightCalculatorEngine {

    /**
     * Calculates facility detention based on standard industry rules.
     * Times should be formatted in 24h format (e.g., "08:00", "14:30") or decimal hours.
     */
    fun calculateDetention(
        appointmentTime: String,
        inTime: String,
        outTime: String,
        hourlyRate: Double = 50.0,
        freeHours: Double = 2.0
    ): DetentionResult {
        val apptHour = parseTimeToDecimal(appointmentTime) ?: 0.0
        val inHour = parseTimeToDecimal(inTime) ?: apptHour
        val outHour = parseTimeToDecimal(outTime) ?: (inHour + 3.0)

        // Industry rule: If driver arrives before appointment, detention clock starts at scheduled appointment
        val startClock = if (inHour < apptHour && apptHour > 0.0) apptHour else inHour
        val rawWait = if (outHour >= startClock) {
            outHour - startClock
        } else {
            (outHour + 24.0) - startClock // overnight roll
        }

        val totalWait = roundToTwoDecimals(rawWait)
        val billableHours = roundToTwoDecimals((totalWait - freeHours).coerceAtLeast(0.0))
        val billableAmount = roundToTwoDecimals(billableHours * hourlyRate)

        val formatted = if (billableHours > 0.0) {
            "Total wait: ${totalWait}h ($freeHours free). Billable: ${billableHours}h at $${hourlyRate.toInt()}/h = $${billableAmount.toInt()}"
        } else {
            "Total wait: ${totalWait}h. Within ${freeHours}h free time window. No detention billable."
        }

        return DetentionResult(
            totalWaitHours = totalWait,
            freeHours = freeHours,
            billableHours = billableHours,
            billableAmount = billableAmount,
            hourlyRate = hourlyRate,
            formattedQuote = formatted
        )
    }

    /**
     * Calculates Gross RPM, Loaded RPM, and estimated fuel expenses.
     */
    fun calculateRpm(
        totalRate: Double,
        loadedMiles: Double,
        deadheadMiles: Double = 0.0,
        fuelPricePerGallon: Double = 3.80,
        avgMpg: Double = 6.5
    ): RpmResult {
        val safeLoaded = loadedMiles.coerceAtLeast(1.0)
        val totalMiles = safeLoaded + deadheadMiles.coerceAtLeast(0.0)

        val grossRpm = roundToTwoDecimals(totalRate / totalMiles)
        val loadedRpm = roundToTwoDecimals(totalRate / safeLoaded)

        val gallonsNeeded = totalMiles / avgMpg
        val estimatedFuel = roundToTwoDecimals(gallonsNeeded * fuelPricePerGallon)
        val netRevenue = roundToTwoDecimals(totalRate - estimatedFuel)

        val formatted = String.format(
            Locale.US,
            "Total: %.0f mi | Gross RPM: $%.2f | Loaded RPM: $%.2f | Fuel: $%.0f | Net: $%.0f",
            totalMiles, grossRpm, loadedRpm, estimatedFuel, netRevenue
        )

        return RpmResult(
            grossRpm = grossRpm,
            loadedRpm = loadedRpm,
            totalMiles = totalMiles,
            estimatedFuelCost = estimatedFuel,
            netRevenueAfterFuel = netRevenue,
            formattedSummary = formatted
        )
    }

    /**
     * Evaluates whether a transit distance is legally achievable within remaining drive clock.
     */
    fun calculateHosReach(
        remainingDriveHours: Double,
        tripMiles: Double,
        avgSpeedMph: Double = 55.0
    ): HosReachResult {
        val safeSpeed = avgSpeedMph.coerceAtLeast(30.0)
        val maxReach = roundToTwoDecimals(remainingDriveHours * safeSpeed)
        val requiredHours = roundToTwoDecimals(tripMiles / safeSpeed)
        val isLegal = requiredHours <= remainingDriveHours
        val requiresBreak = requiredHours > 8.0

        val formatted = if (isLegal) {
            val breakNote = if (requiresBreak) " (30-min break required at 8h)" else ""
            String.format(Locale.US, "LEGAL: %.0f mi requires %.1fh drive clock. Max reach: %.0f mi.%s", tripMiles, requiredHours, maxReach, breakNote)
        } else {
            val deficit = roundToTwoDecimals(requiredHours - remainingDriveHours)
            String.format(Locale.US, "ILLEGAL: %.0f mi needs %.1fh (Short by %.1fh / %.0f mi). Requires 10h break or relay.", tripMiles, requiredHours, deficit, deficit * safeSpeed)
        }

        return HosReachResult(
            isLegal = isLegal,
            maxMilesReach = maxReach,
            requiredDriveHours = requiredHours,
            requiresBreak = requiresBreak,
            formattedSummary = formatted
        )
    }

    private fun parseTimeToDecimal(timeStr: String): Double? {
        val clean = timeStr.trim()
        if (clean.contains(":")) {
            val parts = clean.split(":")
            val hours = parts[0].toDoubleOrNull() ?: return null
            val mins = parts.getOrNull(1)?.toDoubleOrNull() ?: 0.0
            return hours + (mins / 60.0)
        }
        return clean.toDoubleOrNull()
    }

    private fun roundToTwoDecimals(value: Double): Double {
        return (value * 100.0).roundToInt() / 100.0
    }
}
