package com.tushartamrakar.ontime.auth.presentation

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.tushartamrakar.ontime.core.ui.theme.Accent
import com.tushartamrakar.ontime.core.ui.theme.AccentGlow
import com.tushartamrakar.ontime.core.ui.theme.Background
import com.tushartamrakar.ontime.core.ui.theme.Border
import com.tushartamrakar.ontime.core.ui.theme.MulishFamily
import com.tushartamrakar.ontime.core.ui.theme.Primary
import com.tushartamrakar.ontime.core.ui.theme.PrimaryGlow
import com.tushartamrakar.ontime.core.ui.theme.Surface
import com.tushartamrakar.ontime.core.ui.theme.TextMuted
import com.tushartamrakar.ontime.core.ui.theme.TextPrimary
import com.tushartamrakar.ontime.core.ui.theme.TextSecondary
import com.tushartamrakar.ontime.navigation.Screen

@Composable
fun WelcomeScreen(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Spacer(modifier = Modifier.height(64.dp))

        // ─── Logo Section ─────────────────────────────────────────────────────
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Glowing icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(PrimaryGlow),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "⏰", fontSize = 48.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Ontime",
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                fontFamily = MulishFamily,
                color = TextPrimary,
                letterSpacing = (-2).sp,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your personal productivity OS",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = MulishFamily,
                color = TextMuted,
                textAlign = TextAlign.Center,
            )
        }

        // ─── Features Section ─────────────────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PremiumFeatureRow(
                emoji = "⏰",
                title = "Unstoppable alarms",
                subtitle = "Native Android engine — rings no matter what",
                color = Primary,
                glowColor = PrimaryGlow,
            )
            PremiumFeatureRow(
                emoji = "📅",
                title = "Smart calendar",
                subtitle = "Events, reminders & daily planning",
                color = Accent,
                glowColor = AccentGlow,
            )
            PremiumFeatureRow(
                emoji = "🎯",
                title = "Deep focus sessions",
                subtitle = "Pomodoro timer with streak tracking",
                color = Primary,
                glowColor = PrimaryGlow,
            )
            PremiumFeatureRow(
                emoji = "🚫",
                title = "App blocker",
                subtitle = "Block distractions during focus time",
                color = Accent,
                glowColor = AccentGlow,
            )
        }

        // ─── Buttons Section ──────────────────────────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = { navController.navigate(Screen.Register.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                ),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text(
                    text = "Get Started",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color = Color.White,
                    letterSpacing = 0.3.sp,
                )
            }

            OutlinedButton(
                onClick = { navController.navigate(Screen.Login.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Primary,
                ),
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.5.dp,
                    color = Border,
                ),
                shape = RoundedCornerShape(18.dp),
            ) {
                Text(
                    text = "I already have an account",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MulishFamily,
                    color = TextSecondary,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ─── Premium Feature Row ──────────────────────────────────────────────────────
@Composable
fun PremiumFeatureRow(
    emoji: String,
    title: String,
    subtitle: String,
    color: androidx.compose.ui.graphics.Color,
    glowColor: androidx.compose.ui.graphics.Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(glowColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emoji, fontSize = 20.sp)
        }

        Column {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MulishFamily,
                color = TextPrimary,
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = MulishFamily,
                color = TextMuted,
            )
        }
    }
}