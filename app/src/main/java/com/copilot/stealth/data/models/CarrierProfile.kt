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
enum class GuidanceLanguage(
    val displayName: String,
    val code: String,
    val bcp47Locale: String = "ro-RO"
) {
    ROMANIAN("Romanian", "RO", "ro-RO"),
    SPANISH("Spanish", "ES", "es-ES"),
    GERMAN("German", "DE", "de-DE"),
    FRENCH("French", "FR", "fr-FR"),
    ITALIAN("Italian", "IT", "it-IT"),
    RUSSIAN("Russian", "RU", "ru-RU"),
    UKRAINIAN("Ukrainian", "UA", "uk-UA"),
    POLISH("Polish", "PL", "pl-PL"),
    PORTUGUESE("Portuguese", "PT", "pt-PT"),
    TURKISH("Turkish", "TR", "tr-TR"),
    OFF("Disabled (English Only)", "OFF", "en-US")
}

@Serializable
enum class PrompterStyle(val displayName: String) {
    ANCHOR_BULLETS("Bullet Anchors"),       // Hook sentence + 2-3 bold talking points
    KEYWORD_ANCHORS("Keyword Badges"),      // High-glance badge chips [Anchor 1] [Anchor 2]
    FULL_SENTENCE("Verbatim Script")        // Full spoken sentences
}

@Serializable
data class CarrierProfile(
    val profileMode: ProfileMode = ProfileMode.GENERAL_CONVERSATIONAL,
    val prompterStyle: PrompterStyle = PrompterStyle.ANCHOR_BULLETS,
    val guidanceLanguage: GuidanceLanguage = GuidanceLanguage.ROMANIAN,
    val customPrompt: String = "",
    val candidateContext: String = "",
    val resumeFileName: String? = null,
    val targetRole: String = "",
    val targetCompany: String = "",
    val jobDescription: String = "",
    val jobDescriptionFileName: String? = null,
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
