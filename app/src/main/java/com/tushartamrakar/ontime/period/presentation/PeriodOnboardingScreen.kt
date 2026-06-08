package com.tushartamrakar.ontime.period.presentation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.tushartamrakar.ontime.core.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

val RosePrimary = Color(0xFFE91E8C)
val RoseLight   = Color(0xFFFF6B9D)
val RoseSoft    = Color(0xFFFFB3CC)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodOnboardingScreen(navController: NavHostController) {
    val viewModel: PeriodViewModel = hiltViewModel()
    var step by remember { mutableStateOf(0) }
    var lastPeriodDate by remember { mutableStateOf<LocalDate?>(null) }
    var cycleLength by remember { mutableStateOf(28f) }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis(),
            // Block any date after today — works across all Material3 versions
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    return utcTimeMillis <= System.currentTimeMillis()
                }
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dateState.selectedDateMillis?.let { millis ->
                        lastPeriodDate = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("OK", color = RosePrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = Color(0xFF1A1A2E)),
        ) { DatePicker(state = dateState) }
    }

    Box(
        modifier = Modifier.fillMaxSize().background(Background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // ── Flower icon ──────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(RosePrimary.copy(alpha = 0.15f))
                    .border(2.dp, RosePrimary.copy(alpha = 0.4f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("🌸", fontSize = 40.sp)
            }

            Spacer(Modifier.height(24.dp))

            Text(
                "Welcome to\nPeriod Tracker",
                fontSize = 26.sp, fontWeight = FontWeight.Black,
                fontFamily = MulishFamily, color = TextPrimary,
                textAlign = TextAlign.Center, lineHeight = 32.sp,
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "A private, caring space to understand\nyour cycle and take care of yourself.",
                fontSize = 14.sp, fontFamily = MulishFamily,
                color = TextMuted, textAlign = TextAlign.Center, lineHeight = 20.sp,
            )

            Spacer(Modifier.height(40.dp))

            // ── Step indicator ───────────────────────────────────────────────
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(2) { i ->
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .width(if (step == i) 24.dp else 12.dp)
                            .clip(CircleShape)
                            .background(if (step >= i) RosePrimary else TextMuted.copy(alpha = 0.3f)),
                    )
                }
            }

            Spacer(Modifier.height(32.dp))

            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    slideInHorizontally { it } + fadeIn() togetherWith
                            slideOutHorizontally { -it } + fadeOut()
                },
                label = "onboarding_step",
            ) { currentStep ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    when (currentStep) {
                        0 -> {
                            // Step 1: Last period date
                            Text(
                                "When did your last\nperiod start?",
                                fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                                fontFamily = MulishFamily, color = TextPrimary,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                "This helps us predict your next cycle immediately.",
                                fontSize = 13.sp, fontFamily = MulishFamily,
                                color = TextMuted, textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(8.dp))
                            // Date picker button
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(RosePrimary.copy(alpha = 0.1f))
                                    .border(1.5.dp, RosePrimary.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                                    .clickable { showDatePicker = true }
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(Icons.Filled.CalendarToday, null,
                                    tint = RosePrimary, modifier = Modifier.size(22.dp))
                                Text(
                                    lastPeriodDate?.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
                                        ?: "Tap to select date",
                                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                                    fontFamily = MulishFamily,
                                    color = if (lastPeriodDate != null) TextPrimary else TextMuted,
                                )
                            }
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { if (lastPeriodDate != null) step = 1 },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                enabled = lastPeriodDate != null,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = RosePrimary,
                                    disabledContainerColor = RosePrimary.copy(alpha = 0.3f),
                                ),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text("Continue →", fontFamily = MulishFamily,
                                    fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            }
                        }
                        1 -> {
                            // Step 2: Usual cycle length
                            Text(
                                "How long is your\nusual cycle?",
                                fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                                fontFamily = MulishFamily, color = TextPrimary,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                "From first day of period to next period start.\nAverage is 28 days — don't worry if yours differs!",
                                fontSize = 13.sp, fontFamily = MulishFamily,
                                color = TextMuted, textAlign = TextAlign.Center, lineHeight = 18.sp,
                            )
                            Spacer(Modifier.height(8.dp))
                            // Big day number
                            Text(
                                "${cycleLength.toInt()} days",
                                fontSize = 48.sp, fontWeight = FontWeight.Black,
                                fontFamily = MulishFamily, color = RosePrimary,
                            )
                            Text(
                                when {
                                    cycleLength < 24f -> "Shorter than average"
                                    cycleLength > 32f -> "Longer than average"
                                    else              -> "Typical cycle length ✓"
                                },
                                fontSize = 12.sp, fontFamily = MulishFamily,
                                color = TextMuted,
                            )
                            // Slider
                            Slider(
                                value = cycleLength,
                                onValueChange = { cycleLength = it },
                                valueRange = 21f..42f,
                                steps = 20,
                                colors = SliderDefaults.colors(
                                    thumbColor = RosePrimary,
                                    activeTrackColor = RosePrimary,
                                    inactiveTrackColor = RosePrimary.copy(alpha = 0.2f),
                                ),
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("21 days", fontSize = 11.sp,
                                    fontFamily = MulishFamily, color = TextMuted)
                                Text("42 days", fontSize = 11.sp,
                                    fontFamily = MulishFamily, color = TextMuted)
                            }
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = {
                                    lastPeriodDate?.let { date ->
                                        viewModel.completeOnboarding(date, cycleLength.toInt())
                                        navController.navigate("period_tracker") {
                                            popUpTo("period_onboarding") { inclusive = true }
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = RosePrimary),
                                shape = RoundedCornerShape(14.dp),
                            ) {
                                Text("Start Tracking 🌸", fontFamily = MulishFamily,
                                    fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "🔒 Your data stays private on this device",
                fontSize = 11.sp, fontFamily = MulishFamily,
                color = TextMuted, textAlign = TextAlign.Center,
            )
        }
    }
}
