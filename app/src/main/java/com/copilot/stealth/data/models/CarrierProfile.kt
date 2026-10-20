package com.copilot.stealth.data.models

import kotlinx.serialization.Serializable

@Serializable
enum class ProfileMode {
    GENERAL_CONVERSATIONAL, // Free, unconstrained intelligent conversationalist / open discussion
    CANDIDATE_INTERVIEW,    // Technical interview / candidate profile (uses custom candidate notes or default technical background)
    CUSTOM_PROMPT,          // Strictly user-defined context / prompt rules entered in Settings
    CARRIER_DISPATCH        // US freight logistics & carrier dispatch persona
}

@Serializable
enum class TeleprompterFontSize {
    NORMAL,     // 17sp
    LARGE,      // 20sp (default broadcast standard)
    EXTRA_LARGE // 24sp (table / distance view)
}

@Serializable
data class CarrierProfile(
    val profileMode: ProfileMode = ProfileMode.GENERAL_CONVERSATIONAL,
    val customPrompt: String = "",
    val candidateContext: String = "",
    val fontSize: TeleprompterFontSize = TeleprompterFontSize.LARGE,
    // Freight & fleet identity fields (for freight mode)
    val companyName: String = "Apex Freight Logistics LLC",
    val mcNumber: String = "MC-1094821",
    val dotNumber: String = "USDOT 3481920",
    val fleetSize: Int = 18,
    val equipmentTypes: String = "53ft Air-Ride Dry Vans & Thermo King Reefer Units",
    val factoringCompany: String = "Triumph Business Capital (Notice of Assignment active for same-day ACH)",
    val standardDetentionRate: Double = 50.0,
    val standardTonuFee: Double = 200.0,
    val homeBase: String = "Chicago, IL"
)
