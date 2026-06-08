package com.tushartamrakar.ontime.focus.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tushartamrakar.ontime.core.ui.theme.Background
import com.tushartamrakar.ontime.core.ui.theme.Border
import com.tushartamrakar.ontime.core.ui.theme.BorderLight
import com.tushartamrakar.ontime.core.ui.theme.MulishFamily
import com.tushartamrakar.ontime.core.ui.theme.Primary
import com.tushartamrakar.ontime.core.ui.theme.PrimaryGlow
import com.tushartamrakar.ontime.core.ui.theme.Success
import com.tushartamrakar.ontime.core.ui.theme.Surface
import com.tushartamrakar.ontime.core.ui.theme.SurfaceHigh
import com.tushartamrakar.ontime.core.ui.theme.TextMuted
import com.tushartamrakar.ontime.core.ui.theme.TextPrimary
import com.tushartamrakar.ontime.core.ui.theme.TextSecondary
import com.tushartamrakar.ontime.core.ui.theme.Warning

@Composable
fun FocusDrawer(
    // ── Streak data ──────────────────────────────────────────────────────────
    currentStreakDays: Int,
    todayFocusSeconds: Int,
    todaySessionsCompleted: Int,
    dailyGoalSessions: Int,
    // ── Planner data ─────────────────────────────────────────────────────────
    plannerTotalTasks: Int,
    plannerCompletedTasks: Int,
    // ── Blocker data ─────────────────────────────────────────────────────────
    enabledBlockedAppsCount: Int,
    isAdultFilterOn: Boolean,
    // ── Navigation callbacks ─────────────────────────────────────────────────
    onPlannerClick: () -> Unit,
    onBlockerClick: () -> Unit,
    bottomPadding: Dp = 80.dp,
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(bottom = bottomPadding),
    ) {

        // ── App name header ───────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(PrimaryGlow),
                contentAlignment = Alignment.Center,
            ) {
                Text("🎯", fontSize = 18.sp)
            }
            Text(
                text       = "Focus",
                fontSize   = 22.sp,
                fontWeight = FontWeight.Black,
                fontFamily = MulishFamily,
                color      = TextPrimary,
            )
        }

        // ── Divider ───────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .padding(horizontal = 24.dp)
                .background(Border)
        )

        Spacer(Modifier.height(20.dp))

        // ── Streak banner ─────────────────────────────────────────────────────
        StreakBannerCard(
            currentStreakDays      = currentStreakDays,
            todayFocusSeconds      = todayFocusSeconds,
            todaySessionsCompleted = todaySessionsCompleted,
            dailyGoalSessions      = dailyGoalSessions,
            modifier               = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(Modifier.height(16.dp))

        // ── Section label ─────────────────────────────────────────────────────
        Text(
            text       = "SECTIONS",
            fontSize   = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = MulishFamily,
            color      = TextMuted,
            letterSpacing = 1.5.sp,
            modifier   = Modifier.padding(horizontal = 24.dp),
        )

        Spacer(Modifier.height(10.dp))

        // ── Planner card ──────────────────────────────────────────────────────
        DrawerSectionCard(
            icon        = Icons.Filled.TaskAlt,
            iconColor   = Primary,
            title       = "Planner",
            subtitle    = if (plannerTotalTasks == 0)
                "No tasks planned today"
            else
                "$plannerCompletedTasks of $plannerTotalTasks tasks done",
            trailingBadge = if (plannerTotalTasks > 0 && plannerCompletedTasks < plannerTotalTasks)
                "${plannerTotalTasks - plannerCompletedTasks} left"
            else null,
            trailingBadgeColor = Warning,
            onClick     = onPlannerClick,
            modifier    = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(Modifier.height(10.dp))

        // ── Blocker card ──────────────────────────────────────────────────────
        DrawerSectionCard(
            icon        = Icons.Filled.Shield,
            iconColor   = if (isAdultFilterOn || enabledBlockedAppsCount > 0)
                Success else TextMuted,
            title       = "Blocker",
            subtitle    = buildString {
                if (isAdultFilterOn) append("Adult filter ON")
                else append("Adult filter OFF")
                if (enabledBlockedAppsCount > 0) append(" · $enabledBlockedAppsCount apps blocked")
            },
            trailingBadge = if (isAdultFilterOn) "ON" else null,
            trailingBadgeColor = Success,
            onClick     = onBlockerClick,
            modifier    = Modifier.padding(horizontal = 20.dp),
        )

        Spacer(Modifier.weight(1f))

        // ── Bottom tip ────────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Surface)
                .padding(14.dp),
        ) {
            Text(
                text = "💡 Swipe right or tap ☰ to open this menu anytime.",
                fontSize   = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = MulishFamily,
                color      = TextMuted,
                lineHeight = 17.sp,
            )
        }
    }
}

// ─── Streak Banner Card ────────────────────────────────────────────────────────

@Composable
private fun StreakBannerCard(
    currentStreakDays: Int,
    todayFocusSeconds: Int,
    todaySessionsCompleted: Int,
    dailyGoalSessions: Int,
    modifier: Modifier = Modifier,
) {
    val todayMins = todayFocusSeconds / 60
    val todayHrs  = todayMins / 60
    val todayMin  = todayMins % 60
    val timeStr   = if (todayHrs > 0) "${todayHrs}h ${todayMin}m" else "${todayMin}m"
    val goalMet   = todaySessionsCompleted >= dailyGoalSessions && dailyGoalSessions > 0

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (currentStreakDays > 0)
                    Primary.copy(alpha = 0.12f)
                else
                    Surface
            )
            .then(
                if (currentStreakDays > 0)
                    Modifier.then(
                        Modifier.padding(1.dp)
                    )
                else Modifier
            )
            .padding(16.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Fire icon with streak count
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (currentStreakDays > 0) Primary.copy(alpha = 0.2f) else SurfaceHigh
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text     = if (currentStreakDays > 0) "🔥" else "💤",
                    fontSize = 22.sp,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = if (currentStreakDays > 0)
                        "Day $currentStreakDays streak!"
                    else
                        "Start your streak",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color      = if (currentStreakDays > 0) TextPrimary else TextSecondary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text       = if (todayMins > 0)
                        "Today: $timeStr · $todaySessionsCompleted sessions"
                    else
                        "No sessions yet today",
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = MulishFamily,
                    color      = TextMuted,
                )
            }

            // Goal met checkmark
            if (goalMet) {
                Icon(
                    imageVector       = Icons.Filled.CheckCircle,
                    contentDescription = "Goal met",
                    tint              = Success,
                    modifier          = Modifier.size(20.dp),
                )
            }
        }
    }
}

// ─── Drawer Section Card ───────────────────────────────────────────────────────

@Composable
private fun DrawerSectionCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    trailingBadge: String? = null,
    trailingBadgeColor: Color = Primary,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Icon badge
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector       = icon,
                contentDescription = title,
                tint              = iconColor,
                modifier          = Modifier.size(22.dp),
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = title,
                fontSize   = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily,
                color      = TextPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text       = subtitle,
                fontSize   = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = MulishFamily,
                color      = TextMuted,
                maxLines   = 1,
            )
        }

        // Trailing badge or chevron
        if (trailingBadge != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(trailingBadgeColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text       = trailingBadge,
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color      = trailingBadgeColor,
                )
            }
        } else {
            Icon(
                imageVector       = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint              = TextMuted,
                modifier          = Modifier.size(18.dp),
            )
        }
    }
}
