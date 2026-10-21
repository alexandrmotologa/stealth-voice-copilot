package com.copilot.stealth.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.copilot.stealth.data.models.CopilotTurn
import com.copilot.stealth.data.models.TeleprompterFontSize
import com.copilot.stealth.ui.theme.LocalStealthPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * High-legibility TV news teleprompter turn card.
 * Designed for instant live reading during interviews with distinct visual separation:
 * 1. "CE A AUZIT" (Interviewer query context)
 * 2. "CE SĂ RĂSPUNZI" (Large, confident, spoken teleprompter prompt + 1-Tap Quick Copy)
 * 3. "CONTEXT / TRADUCERE" (Romanian assistance & translation)
 */
@Composable
fun TurnCard(
    turn: CopilotTurn,
    isRomanianEnabled: Boolean,
    fontSize: TeleprompterFontSize = TeleprompterFontSize.LARGE,
    modifier: Modifier = Modifier
) {
    val palette = LocalStealthPalette.current
    val clipboardManager = LocalClipboardManager.current
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    var isCopied by remember { mutableStateOf(false) }

    val (answerFontSize, answerLineHeight) = when (fontSize) {
        TeleprompterFontSize.NORMAL -> Pair(17.sp, 24.sp)
        TeleprompterFontSize.LARGE -> Pair(20.sp, 28.sp)
        TeleprompterFontSize.EXTRA_LARGE -> Pair(24.sp, 34.sp)
    }

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
                text = "HEARD (INTERLOCUTOR)",
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(palette.statusActive)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SUGGESTED ANSWER (TELEPROMPTER)",
                    color = palette.statusActive,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }

            // 1-Tap Quick Copy Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(if (isCopied) palette.statusActive.copy(alpha = 0.2f) else Color(0xFF141A16))
                    .border(
                        1.dp,
                        if (isCopied) palette.statusActive else Color(0xFF23362A),
                        RoundedCornerShape(4.dp)
                    )
                    .clickable {
                        clipboardManager.setText(AnnotatedString(turn.answerEn))
                        isCopied = true
                        android.widget.Toast.makeText(context, "Copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                        scope.launch {
                            delay(1800)
                            isCopied = false
                        }
                    }
                    .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    text = if (isCopied) "COPIED ✓" else "COPY",
                    color = if (isCopied) palette.statusActive else palette.textMetadata,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Suggested English Answer formatted with high visual clarity & selected font size
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
            fontSize = answerFontSize,
            fontWeight = FontWeight.SemiBold,
            lineHeight = answerLineHeight,
            letterSpacing = 0.2.sp
        )

        // Section 3: Answer Explanation & Guidance in Target Language (if enabled)
        val langCode = turn.translationLangCode ?: "RO"
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
                        text = "CONTEXT & GUIDANCE ($langCode):",
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

        // Section 4: Predicted Follow-Up Questions
        if (turn.followUpQuestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "PREDICTED FOLLOW-UPS:",
                    color = palette.textMetadata,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                turn.followUpQuestions.take(2).forEach { followUp ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF141A16))
                            .border(1.dp, Color(0xFF222E26), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "↳ $followUp",
                            color = palette.textQuestion.copy(alpha = 0.9f),
                            fontSize = 12.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            lineHeight = 16.sp
                        )
                    }
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
