package com.copilot.stealth.engine.stt

/**
 * Repairs phonetic transcription anomalies on specialized US freight logistics jargon,
 * supporting international accents (Indian, Hispanic, Eastern European) and broken English syntax.
 */
object FreightLexiconNormalizer {

    private val replacementRules: List<Pair<Regex, String>> = listOf(
        // International accent & speech-to-text variations (Indian, Slavic, Hispanic)
        Regex("(?i)\\b(die\\s*tension|detenshon|detenshun|dietention|detention time|extra wait|waiting time)\\b") to "detention",
        Regex("(?i)\\b(ray con|rate calm|rate come|re con|red con|rait con|rate confarmation|rate confirm)\\b") to "Rate Confirmation",
        Regex("(?i)\\b(rate con)\\b") to "Rate Con",
        Regex("(?i)\\b(b\\s*o\\s*l|bill of lady|bill of late in|bowl|bill of landing|bill of ladding|stamped paper)\\b") to "BOL",
        Regex("(?i)\\b(p\\s*o\\s*d|pee oh dee|proof of delivery|delivery stamp|receiver stamp)\\b") to "POD",
        Regex("(?i)\\b(toe new|t\\s*o\\s*n\\s*u|tony fee|truck cancel|cancel load fee)\\b") to "TONU",
        Regex("(?i)\\b(h\\s*o\\s*s|hours of services|drive clock|log book|logbook time|hours left)\\b") to "HOS",
        Regex("(?i)\\b(lumbar fee|lumber fee|lumper receipt|lumber receipt|lumba|unloading cash)\\b") to "lumper fee",
        Regex("(?i)\\b(n\\s*o\\s*a|notice of assign|notice of assignment)\\b") to "Notice of Assignment",
        Regex("(?i)\\b(cad scale|cat scales|cat scale ticket)\\b") to "CAT Scale",
        Regex("(?i)\\b(ten dams|tandem pins|slide pins)\\b") to "tandems",
        Regex("(?i)\\b(keep trucking|keep truckin)\\b") to "Motive/KeepTruckin",
        Regex("(?i)\\b(sam sara|sam sara eld)\\b") to "Samsara",
        Regex("(?i)\\b(dead head|deadhead miles|empty miles)\\b") to "deadhead",
        Regex("(?i)\\b(layower|lay over fee|overnight wait|tomorrow unload)\\b") to "layover",
        Regex("(?i)\\b(mc numba|mc number|carrier mc|what is mc)\\b") to "MC number",

        // Romanian spoken loanwords & translated terms
        Regex("(?i)\\b(detenție|detentie|timp de asteptare)\\b") to "detention waiting",
        Regex("(?i)\\b(confirmare de cursa|confirmarea de cursa)\\b") to "Rate Confirmation",
        Regex("(?i)\\b(scrisoare de trasura|scrisoarea de trasura)\\b") to "BOL",
        Regex("(?i)\\b(dovada de livrare|dovada livrarii)\\b") to "POD",
        Regex("(?i)\\b(cursa anulata|taxa de anulare)\\b") to "TONU",
        Regex("(?i)\\b(ore de condus|program de condus)\\b") to "HOS Hours of Service",
        Regex("(?i)\\b(taxa de descarcare|taxa lumper)\\b") to "lumper fee"
    )

    fun normalize(rawTranscript: String): String {
        var result = rawTranscript
        for ((pattern, replacement) in replacementRules) {
            result = result.replace(pattern, replacement)
        }
        return result.trim()
    }
}
