package com.copilot.stealth.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight

object FreightTextFormatter {

    private val moneyColor = Color(0xFF81C784) // Soft green for dollar amounts
    private val acronymColor = Color(0xFF80D8FF) // Crisp cyan for freight documents/acronyms
    private val timeColor = Color(0xFFFFD54F) // Warm amber for HOS, detention hours, and windows

    private val moneyRegex = Regex("(\\\$\\s*\\d+(?:,\\d{3})*(?:\\.\\d{2})?(?:\\s*(?:to|-)\\s*\\\$?\\s*\\d+(?:,\\d{3})*(?:\\.\\d{2})?)?(?:\\/(?:hr|h|day|mi|mile))?)")
    private val acronymRegex = Regex("\\b(Rate Con(?:firmation)?|BOL|POD|TONU|HOS|ELD|NOA|OS&D|CAT Scale|FMCSA|DAT(?: One)?|Truckstop|Comcheck|EFS|Motive|KeepTruckin|Samsara)\\b", RegexOption.IGNORE_CASE)
    private val timeRegex = Regex("\\b(\\d+(?:\\.\\d+)?\\s*(?:free\\s*hours|hours|hour|hrs|hr|min|minutes|days|day)|90-minute\\s*mark|11-hour|14-hour|34-hour(?:\\s*restart)?)\\b", RegexOption.IGNORE_CASE)

    fun format(
        text: String,
        baseColor: Color,
        overrideColors: HighlightColors? = null
    ): AnnotatedString {
        val money = overrideColors?.money ?: moneyColor
        val acronym = overrideColors?.acronym ?: acronymColor
        val time = overrideColors?.time ?: timeColor

        val annotatedString = buildAnnotatedString {
            append(text)

            // Highlight Money
            moneyRegex.findAll(text).forEach { match ->
                addStyle(
                    style = SpanStyle(color = money, fontWeight = FontWeight.Bold),
                    start = match.range.first,
                    end = match.range.last + 1
                )
            }

            // Highlight Acronyms
            acronymRegex.findAll(text).forEach { match ->
                addStyle(
                    style = SpanStyle(color = acronym, fontWeight = FontWeight.SemiBold),
                    start = match.range.first,
                    end = match.range.last + 1
                )
            }

            // Highlight Time Limits
            timeRegex.findAll(text).forEach { match ->
                addStyle(
                    style = SpanStyle(color = time, fontWeight = FontWeight.Medium),
                    start = match.range.first,
                    end = match.range.last + 1
                )
            }
        }

        return annotatedString
    }

    data class HighlightColors(
        val money: Color,
        val acronym: Color,
        val time: Color
    )
}
