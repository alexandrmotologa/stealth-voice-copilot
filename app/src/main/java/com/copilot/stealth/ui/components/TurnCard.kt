package com.copilot.stealth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.copilot.stealth.data.models.CopilotTurn
import com.copilot.stealth.ui.theme.LocalStealthPalette

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
            .clip(RoundedCornerShape(8.dp))
            .background(palette.surface)
            .padding(14.dp)
    ) {
        // Interlocutor Question
        Text(
            text = turn.transcriptEn,
            color = palette.textQuestion,
            fontSize = 14.sp,
            lineHeight = 19.sp
        )

        // Question Translation in Romanian (if enabled)
        if (isRomanianEnabled && !turn.transcriptRo.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = turn.transcriptRo,
                color = palette.textRomanian,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Suggested English Answer with dynamic syntax highlighting for money, acronyms, and time
        val highlightedAnswer = FreightTextFormatter.format(
            text = turn.answerEn,
            baseColor = palette.textAnswer,
            overrideColors = FreightTextFormatter.HighlightColors(
                money = palette.highlightMoney,
                acronym = palette.highlightAcronym,
                time = palette.highlightTime
            )
        )

        Text(
            text = highlightedAnswer,
            color = palette.textAnswer,
            fontSize = 16.sp,
            lineHeight = 22.sp
        )

        // Answer Explanation in Romanian (if enabled)
        if (isRomanianEnabled && !turn.answerRo.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = turn.answerRo,
                color = palette.textRomanian,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Metadata footer: Category, Confidence, Latency
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
                    letterSpacing = 0.5.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (turn.latencyMs > 0) {
                    Text(
                        text = "${turn.latencyMs}ms",
                        color = palette.textMetadata,
                        fontSize = 10.sp
                    )
                }
                Text(
                    text = "${(turn.confidence * 100).toInt()}% conf",
                    color = palette.textMetadata,
                    fontSize = 10.sp
                )
            }
        }
    }
}
