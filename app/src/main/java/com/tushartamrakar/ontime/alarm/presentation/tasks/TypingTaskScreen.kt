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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tushartamrakar.ontime.alarm.domain.TaskType
import com.tushartamrakar.ontime.alarm.domain.TypingCategory
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
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay

// ─── Phrase Library ───────────────────────────────────────────────────────────
val motivationalPhrases = listOf(
    "Every morning is a fresh start to be the best version of yourself",
    "Rise and shine, greatness awaits you today",
    "You are stronger than you think and braver than you believe",
    "Success belongs to those who get up and chase it",
    "Today is another chance to get it right",
    "Dream big, work hard, stay focused and never give up",
    "The only way to do great work is to love what you do",
    "Push yourself because no one else is going to do it for you",
)

val affirmationPhrases = listOf(
    "I am capable of achieving everything I set my mind to",
    "I choose to be happy and grateful for this new day",
    "I am healthy, energetic, and ready to conquer this day",
    "I attract positivity and abundance into my life",
    "I am confident, strong, and full of energy",
    "Today I will make progress toward my goals",
    "I am worthy of success and happiness",
    "I wake up with purpose and intention every single day",
)

val tongueTwisters = listOf(
    "She sells seashells by the seashore and the shells she sells are seashells",
    "Peter Piper picked a peck of pickled peppers",
    "How much wood would a woodchuck chuck if a woodchuck could chuck wood",
    "Red lorry yellow lorry red lorry yellow lorry",
    "Unique New York unique New York you know you need unique New York",
    "If a dog chews shoes whose shoes does he choose to chew",
    "Betty Botter bought some butter but the butter Betty bought was bitter",
)

fun getPhrasesForCategory(category: TypingCategory, customPhrases: List<String>): List<String> {
    return when (category) {
        TypingCategory.MOTIVATIONAL -> motivationalPhrases
        TypingCategory.AFFIRMATIONS -> affirmationPhrases
        TypingCategory.TONGUE_TWISTERS -> tongueTwisters
        TypingCategory.CUSTOM -> customPhrases.ifEmpty { motivationalPhrases }
    }
}

// ─── Typing Task Config UI ────────────────────────────────────────────────────
@Composable
fun TypingTaskConfigSheet(
    onSave: (WakeUpTask) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedCategory by remember { mutableStateOf(TypingCategory.MOTIVATIONAL) }
    var customPhrases by remember { mutableStateOf(listOf<String>()) }
    var newPhrase by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        Text(
            text = "⌨️ Typing Task",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = MulishFamily,
            color = TextPrimary,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Type a phrase exactly to dismiss your alarm",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = MulishFamily,
            color = TextMuted,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "PHRASE CATEGORY",
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
            listOf(TypingCategory.MOTIVATIONAL, TypingCategory.AFFIRMATIONS).forEach { category ->
                val isSelected = selectedCategory == category
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
                        .clickable { selectedCategory = category }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = category.emoji, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = category.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = MulishFamily,
                            color = if (isSelected) Color.White else TextMuted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf(TypingCategory.TONGUE_TWISTERS, TypingCategory.CUSTOM).forEach { category ->
                val isSelected = selectedCategory == category
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
                        .clickable { selectedCategory = category }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = category.emoji, fontSize = 20.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = category.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = MulishFamily,
                            color = if (isSelected) Color.White else TextMuted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (selectedCategory != TypingCategory.CUSTOM) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceHigh)
                    .padding(16.dp),
            ) {
                Text(
                    text = "Preview: \"${getPhrasesForCategory(selectedCategory, emptyList()).first()}\"",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = MulishFamily,
                    color = TextMuted,
                )
            }
        }

        if (selectedCategory == TypingCategory.CUSTOM) {
            Text(
                text = "YOUR PHRASES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MulishFamily,
                color = TextMuted,
                letterSpacing = 1.sp,
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = newPhrase,
                onValueChange = { newPhrase = it },
                placeholder = {
                    Text(
                        text = "Type a custom phrase...",
                        color = TextMuted,
                        fontFamily = MulishFamily,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Primary,
                    unfocusedBorderColor = Border,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = Primary,
                    focusedContainerColor = CardBackground,
                    unfocusedContainerColor = CardBackground,
                ),
                shape = RoundedCornerShape(12.dp),
                textStyle = TextStyle(
                    fontFamily = MulishFamily,
                    fontSize = 14.sp,
                    color = TextPrimary,
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (newPhrase.isNotBlank()) {
                            customPhrases = customPhrases + newPhrase.trim()
                            newPhrase = ""
                        }
                    },
                ),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (newPhrase.isNotBlank()) {
                                customPhrases = customPhrases + newPhrase.trim()
                                newPhrase = ""
                            }
                        },
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Add",
                            tint = Primary,
                        )
                    }
                },
            )

            Spacer(modifier = Modifier.height(8.dp))

            customPhrases.forEachIndexed { index, phrase ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceHigh)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = phrase,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = MulishFamily,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = {
                            customPhrases = customPhrases.toMutableList()
                                .also { it.removeAt(index) }
                        },
                        modifier = Modifier.size(24.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Remove",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp),
                        )
                    }
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
                            type = TaskType.TYPING,
                            typingCategory = selectedCategory.name,
                            typingCustomPhrases = customPhrases,
                        )
                    )
                    onDismiss()
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Add Typing Task",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily,
                color = Color.White,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// ─── Typing Task Runtime UI ───────────────────────────────────────────────────
@Composable
fun TypingTaskRuntimeScreen(
    task: WakeUpTask,
    onUserActiveChange: (Boolean) -> Unit,
    onTaskCompleted: () -> Unit,
) {
    val category = remember { TypingCategory.valueOf(task.typingCategory) }
    val phrases = remember { getPhrasesForCategory(category, task.typingCustomPhrases) }
    val targetPhrase = remember { phrases.random() }
    var userInput by remember { mutableStateOf("") }
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
            text = "⌨️ Type this phrase",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = MulishFamily,
            color = TextMuted,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceHigh)
                .padding(20.dp),
        ) {
            Text(
                text = targetPhrase,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily,
                color = TextPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = userInput,
            onValueChange = {
                userInput = it
                isWrong = false
                lastTypingTime = System.currentTimeMillis() // ← keystroke detected
            },
            placeholder = {
                Text(
                    text = "Type the phrase above...",
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
            textStyle = TextStyle(
                fontFamily = MulishFamily,
                fontSize = 15.sp,
                color = TextPrimary,
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (userInput.trim().equals(targetPhrase.trim(), ignoreCase = true)) {
                        onUserActiveChange(false)
                        onTaskCompleted()
                    } else {
                        isWrong = true
                    }
                },
            ),
        )

        if (isWrong) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "❌ Not quite right! Check spelling",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MulishFamily,
                color = Danger,
                textAlign = TextAlign.Center,
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
                    if (userInput.trim().equals(targetPhrase.trim(), ignoreCase = true)) {
                        onUserActiveChange(false)
                        onTaskCompleted()
                    } else {
                        isWrong = true
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Submit",
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily,
                color = Color.White,
            )
        }
    }
}