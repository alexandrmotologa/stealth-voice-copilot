package com.copilot.stealth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.copilot.stealth.data.models.CopilotTurn
import com.copilot.stealth.ui.theme.LocalStealthPalette

/**
 * High-legibility TV news teleprompter turn card.
 * Designed for instant live reading during interviews with distinct visual separation:
 * 1. "CE A AUZIT" (Interviewer query context)
 * 2. "CE SĂ RĂSPUNZI" (Large, confident, spoken teleprompter prompt)
 * 3. "CONTEXT / TRADUCERE" (Romanian assistance & translation)
 */
@Composable
fun TurnCard(
    turn: CopilotTurn,
    isRomanianEnabled: Boolean,
    modifier: Modifier = Modifier
) {
    val palette = LocalStealthPalette.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(palette.surface)
            .border(1.dp, Color(0xFF222624), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        // Section 1: Question Heard / Interlocutor Context
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(palette.textMetadata)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "CE A AUZIT (ÎNTREBARE)",
                color = palette.textMetadata,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = turn.transcriptEn,
            color = palette.textQuestion,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 21.sp
        )

        // Question Translation in Romanian (if enabled)
        if (isRomanianEnabled && !turn.transcriptRo.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = turn.transcriptRo,
                color = palette.textRomanian.copy(alpha = 0.85f),
                fontSize = 13.sp,
                lineHeight = 17.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Color(0xFF1E2620))
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Section 2: Prompter Spoken Answer (Large, High-Legibility TV News Teleprompter)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(palette.statusActive)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "CE SĂ RĂSPUNZI (TELEPROMPTER)",
                color = palette.statusActive,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Suggested English Answer formatted with high visual clarity
        val highlightedAnswer = FreightTextFormatter.format(
            text = turn.answerEn,
            baseColor = Color.White,
            overrideColors = FreightTextFormatter.HighlightColors(
                money = palette.highlightMoney,
                acronym = palette.highlightAcronym,
                time = palette.highlightTime
            )
        )

        Text(
            text = highlightedAnswer,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 28.sp,
            letterSpacing = 0.2.sp
        )

        // Section 3: Answer Explanation & Translation in Romanian (if enabled)
        if (isRomanianEnabled && !turn.answerRo.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0F1A14))
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Column {
                    Text(
                        text = "CONTEXT & TRADUCERE (RO):",
                        color = palette.highlightAcronym,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = turn.answerRo,
                        color = palette.textRomanian,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Metadata footer: Model, Latency, Confidence
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            turn.matchedCategory?.let { category ->
                Text(
                    text = category.uppercase(),
                    color = palette.textMetadata,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (turn.latencyMs > 0) {
                    Text(
                        text = "⚡ ${turn.latencyMs}ms",
                        color = palette.statusActive,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Text(
                    text = "${(turn.confidence * 100).toInt()}% conf",
                    color = palette.textMetadata,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
