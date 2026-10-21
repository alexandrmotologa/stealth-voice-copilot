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
enum class GuidanceLanguage(val displayName: String, val code: String) {
    ROMANIAN("Romanian", "RO"),
    SPANISH("Spanish", "ES"),
    GERMAN("German", "DE"),
    FRENCH("French", "FR"),
    ITALIAN("Italian", "IT"),
    RUSSIAN("Russian", "RU"),
    UKRAINIAN("Ukrainian", "UA"),
    POLISH("Polish", "PL"),
    PORTUGUESE("Portuguese", "PT"),
    TURKISH("Turkish", "TR"),
    OFF("Disabled (English Only)", "OFF")
}

@Serializable
enum class PrompterStyle {
    ANCHOR_BULLETS, // Hook sentence + 2-3 bold talking points
    FULL_SENTENCE   // Verbatim speech
}

@Serializable
data class CarrierProfile(
    val profileMode: ProfileMode = ProfileMode.GENERAL_CONVERSATIONAL,
    val prompterStyle: PrompterStyle = PrompterStyle.ANCHOR_BULLETS,
    val guidanceLanguage: GuidanceLanguage = GuidanceLanguage.ROMANIAN,
    val customPrompt: String = "",
    val candidateContext: String = "",
    val targetRole: String = "",
    val targetCompany: String = "",
    val jobDescription: String = "",
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
