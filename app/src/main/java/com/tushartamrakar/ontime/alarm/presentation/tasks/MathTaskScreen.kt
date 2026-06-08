package com.tushartamrakar.ontime.alarm.presentation.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tushartamrakar.ontime.alarm.domain.MathDifficulty
import com.tushartamrakar.ontime.alarm.domain.TaskType
import com.tushartamrakar.ontime.alarm.domain.WakeUpTask
import com.tushartamrakar.ontime.core.ui.theme.Background
import com.tushartamrakar.ontime.core.ui.theme.Border
import com.tushartamrakar.ontime.core.ui.theme.CardBackground
import com.tushartamrakar.ontime.core.ui.theme.Danger
import com.tushartamrakar.ontime.core.ui.theme.MulishFamily
import com.tushartamrakar.ontime.core.ui.theme.Primary
import com.tushartamrakar.ontime.core.ui.theme.SurfaceHigh
import com.tushartamrakar.ontime.core.ui.theme.TextMuted
import com.tushartamrakar.ontime.core.ui.theme.TextPrimary
import kotlinx.coroutines.delay
import kotlin.random.Random

// ─── Math Question Generator ──────────────────────────────────────────────────
data class MathQuestion(
    val expression: String,
    val answer: Int,
)

fun generateMathQuestion(difficulty: MathDifficulty): MathQuestion {
    return when (difficulty) {
        MathDifficulty.EASY -> {
            val a = Random.nextInt(1, 20)
            val b = Random.nextInt(1, 20)
            val ops = listOf("+", "-")
            val op = ops.random()
            when (op) {
                "+" -> MathQuestion("$a + $b", a + b)
                else -> MathQuestion("${a + b} - $b", a)
            }
        }
        MathDifficulty.MEDIUM -> {
            val type = Random.nextInt(3)
            when (type) {
                0 -> {
                    val a = Random.nextInt(2, 12)
                    val b = Random.nextInt(2, 12)
                    MathQuestion("$a × $b", a * b)
                }
                1 -> {
                    val b = Random.nextInt(2, 10)
                    val ans = Random.nextInt(2, 12)
                    MathQuestion("${b * ans} ÷ $b", ans)
                }
                else -> {
                    val a = Random.nextInt(10, 99)
                    val b = Random.nextInt(10, 99)
                    MathQuestion("$a + $b", a + b)
                }
            }
        }
        MathDifficulty.HARD -> {
            val type = Random.nextInt(3)
            when (type) {
                0 -> {
                    val a = Random.nextInt(10, 30)
                    val b = Random.nextInt(10, 30)
                    MathQuestion("$a × $b", a * b)
                }
                1 -> {
                    val a = Random.nextInt(100, 999)
                    val b = Random.nextInt(100, 999)
                    MathQuestion("$a + $b", a + b)
                }
                else -> {
                    val a = Random.nextInt(50, 200)
                    val b = Random.nextInt(10, 50)
                    MathQuestion("$a - $b", a - b)
                }
            }
        }
    }
}

// ─── Math Task Config UI ──────────────────────────────────────────────────────
@Composable
fun MathTaskConfigSheet(
    onSave: (WakeUpTask) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedDifficulty by remember { mutableStateOf(MathDifficulty.EASY) }
    var questionCount by remember { mutableIntStateOf(3) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Background)
            .padding(24.dp),
    ) {
        Text(
            text = "🧮 Math Challenge",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = MulishFamily,
            color = TextPrimary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Solve math problems to dismiss your alarm",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = MulishFamily,
            color = TextMuted,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "DIFFICULTY",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = MulishFamily,
            color = TextMuted,
            letterSpacing = 1.sp,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MathDifficulty.entries.forEach { difficulty ->
                val isSelected = selectedDifficulty == difficulty
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Primary else SurfaceHigh)
                        .border(
                            width = 1.5.dp,
                            color = if (isSelected) Primary else Border,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .clickable { selectedDifficulty = difficulty }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = when (difficulty) {
                                MathDifficulty.EASY -> "😊"
                                MathDifficulty.MEDIUM -> "🤔"
                                MathDifficulty.HARD -> "🔥"
                            },
                            fontSize = 20.sp,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = difficulty.label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = MulishFamily,
                            color = if (isSelected) Color.White else TextMuted,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = selectedDifficulty.description,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = MulishFamily,
            color = TextMuted,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "NUMBER OF QUESTIONS",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = MulishFamily,
            color = TextMuted,
            letterSpacing = 1.sp,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(1, 3, 5, 10).forEach { count ->
                val isSelected = questionCount == count
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) Primary else SurfaceHigh)
                        .border(
                            width = 1.5.dp,
                            color = if (isSelected) Primary else Border,
                            shape = RoundedCornerShape(12.dp),
                        )
                        .clickable { questionCount = count },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "$count",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = MulishFamily,
                        color = if (isSelected) Color.White else TextMuted,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Primary)
                .clickable {
                    onSave(
                        WakeUpTask(
                            type = TaskType.MATH,
                            mathDifficulty = selectedDifficulty.name,
                            mathQuestionCount = questionCount,
                        )
                    )
                    onDismiss()
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Add Math Challenge",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily,
                color = Color.White,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ─── Math Task Runtime UI ─────────────────────────────────────────────────────
@Composable
fun MathTaskRuntimeScreen(
    task: WakeUpTask,
    onUserActiveChange: (Boolean) -> Unit,
    onTaskCompleted: () -> Unit,
) {
    val difficulty = remember { MathDifficulty.valueOf(task.mathDifficulty) }
    val totalQuestions = task.mathQuestionCount
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var currentQuestion by remember { mutableStateOf(generateMathQuestion(difficulty)) }
    var userAnswer by remember { mutableStateOf("") }
    var isWrong by remember { mutableStateOf(false) }
    var lastTypingTime by remember { mutableStateOf(0L) }

    // Timer pauses only for 1.5 seconds after last keystroke
    LaunchedEffect(lastTypingTime) {
        if (lastTypingTime > 0L) {
            onUserActiveChange(true)
            delay(1500)
            onUserActiveChange(false)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Question ${currentQuestionIndex + 1} of $totalQuestions",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = MulishFamily,
            color = TextMuted,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Primary.copy(alpha = 0.15f))
                .padding(horizontal = 12.dp, vertical = 4.dp),
        ) {
            Text(
                text = "${difficulty.label} Mode",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MulishFamily,
                color = Primary,
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = currentQuestion.expression,
            fontSize = 56.sp,
            fontWeight = FontWeight.Black,
            fontFamily = MulishFamily,
            color = TextPrimary,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(48.dp))

        OutlinedTextField(
            value = userAnswer,
            onValueChange = {
                userAnswer = it
                isWrong = false
                lastTypingTime = System.currentTimeMillis()
            },
            placeholder = {
                Text(
                    text = "Your answer...",
                    color = TextMuted,
                    fontFamily = MulishFamily,
                )
            },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = if (isWrong) Danger else Primary,
                unfocusedBorderColor = if (isWrong) Danger else Border,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = Primary,
                focusedContainerColor = CardBackground,
                unfocusedContainerColor = CardBackground,
            ),
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            keyboardActions = KeyboardActions(
                onDone = {
                    val answer = userAnswer.trim().toIntOrNull()
                    if (answer == currentQuestion.answer) {
                        onUserActiveChange(false)
                        if (currentQuestionIndex + 1 >= totalQuestions) {
                            onTaskCompleted()
                        } else {
                            currentQuestionIndex++
                            currentQuestion = generateMathQuestion(difficulty)
                            userAnswer = ""
                        }
                    } else {
                        isWrong = true
                    }
                },
            ),
            textStyle = TextStyle(
                fontFamily = MulishFamily,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                color = TextPrimary,
            ),
        )

        if (isWrong) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "❌ Wrong! Try again",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MulishFamily,
                color = Danger,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Primary)
                .clickable {
                    val answer = userAnswer.trim().toIntOrNull()
                    if (answer == currentQuestion.answer) {
                        onUserActiveChange(false)
                        if (currentQuestionIndex + 1 >= totalQuestions) {
                            onTaskCompleted()
                        } else {
                            currentQuestionIndex++
                            currentQuestion = generateMathQuestion(difficulty)
                            userAnswer = ""
                            isWrong = false
                        }
                    } else {
                        isWrong = true
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
                Text(
                    text = "Submit Answer",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color = Color.White,
                )
            }
        }
    }
}