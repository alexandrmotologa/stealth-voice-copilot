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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.copilot.stealth.engine.mock.MockInterviewManager
import com.copilot.stealth.engine.mock.MockQuestionSession
import com.copilot.stealth.ui.theme.StealthPalette

@Composable
fun MockInterviewSheet(
    mockManager: MockInterviewManager,
    palette: StealthPalette,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var activeSession by remember { mutableStateOf(mockManager.getRandomQuestion()) }
    var candidateAnswer by remember { mutableStateOf("") }
    var evaluatedSession by remember { mutableStateOf<MockQuestionSession?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(palette.surface)
            .border(1.dp, Color(0xFF222222), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(18.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Drag handle
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF333333))
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "MOCK INTERVIEW SIMULATOR",
                    color = palette.textAnswer,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "Category: ${activeSession.scenario.category.uppercase()}",
                    color = palette.highlightAcronym,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { onDismiss() }
                    .padding(6.dp)
            ) {
                Text("✕", color = palette.textMetadata, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Interlocutor Question Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF101010))
                .border(1.dp, Color(0xFF262626), RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Column {
                Text(
                    text = "INTERVIEWER ASKS:",
                    color = palette.textMetadata,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = activeSession.questionEn,
                    color = palette.textAnswer,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = activeSession.questionRo,
                    color = palette.textRomanian,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Optimal response cheat sheet preview
        Text(
            text = "TARGET INDUSTRY STANDARD ANSWER:",
            color = palette.textMetadata,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = activeSession.optimalAnswerEn,
            color = Color(0xFFAAAAAA),
            fontSize = 12.sp,
            lineHeight = 17.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Candidate response input
        Text(
            text = "YOUR SPOKEN RESPONSE:",
            color = palette.textMetadata,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF0D0D0D))
                .border(1.dp, Color(0xFF222222), RoundedCornerShape(6.dp))
                .padding(10.dp)
        ) {
            BasicTextField(
                value = candidateAnswer,
                onValueChange = { candidateAnswer = it },
                textStyle = TextStyle(
                    color = palette.textAnswer,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                ),
                cursorBrush = SolidColor(palette.highlightAcronym),
                modifier = Modifier.fillMaxWidth()
            )
            if (candidateAnswer.isEmpty()) {
                Text(
                    text = "Type or dictate candidate response to score...",
                    color = palette.textMetadata,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1E281E))
                    .clickable {
                        evaluatedSession = mockManager.evaluateAnswer(activeSession, candidateAnswer)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "EVALUATE ANSWER",
                    color = palette.highlightMoney,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1A1A1A))
                    .clickable {
                        activeSession = mockManager.getRandomQuestion()
                        candidateAnswer = ""
                        evaluatedSession = null
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "NEXT QUESTION →",
                    color = palette.textAnswer,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Evaluation feedback box
        evaluatedSession?.let { evaluation ->
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF141414))
                    .border(1.dp, if (evaluation.scorePercentage >= 75) palette.highlightMoney else palette.highlightTime, RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "READINESS SCORE",
                            color = palette.textMetadata,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "${evaluation.scorePercentage}%",
                            color = if (evaluation.scorePercentage >= 75) palette.highlightMoney else palette.highlightTime,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = evaluation.feedback,
                        color = palette.textAnswer,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}
