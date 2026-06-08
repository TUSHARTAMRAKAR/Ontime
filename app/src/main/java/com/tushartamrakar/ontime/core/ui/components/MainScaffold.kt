package com.tushartamrakar.ontime.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.tushartamrakar.ontime.core.ui.theme.LocalOntimeColors
import com.tushartamrakar.ontime.core.ui.theme.MulishFamily
import com.tushartamrakar.ontime.core.ui.theme.Primary
import com.tushartamrakar.ontime.navigation.Screen

// ─── Tab definition ───────────────────────────────────────────────────────────

data class TabItem(
    val screen: Screen,
    val label:  String,
    val icon:   ImageVector,
)

val bottomTabs = listOf(
    TabItem(Screen.Alarms,   "Alarms",   Icons.Filled.AccessAlarm),
    TabItem(Screen.Calendar, "Calendar", Icons.Filled.CalendarMonth),
    TabItem(Screen.Focus,    "Focus",    Icons.Filled.Timer),
    TabItem(Screen.Settings, "Settings", Icons.Filled.Settings),
)

// ─── Main Scaffold ────────────────────────────────────────────────────────────

@Composable
fun MainScaffold(
    navController: NavHostController,
    content: @Composable (PaddingValues) -> Unit,
) {
    val colors = LocalOntimeColors.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        containerColor = colors.background,
        bottomBar = {
            PremiumNavBar(
                currentRoute = currentRoute,
                onNavigate   = { tab ->
                    if (currentRoute != tab.screen.route) {
                        navController.navigate(tab.screen.route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    }
                },
            )
        },
        content = content,
    )
}

// ─── Premium Nav Bar ──────────────────────────────────────────────────────────

@Composable
private fun PremiumNavBar(
    currentRoute: String?,
    onNavigate:   (TabItem) -> Unit,
) {
    val colors = LocalOntimeColors.current

    Column(modifier = Modifier.fillMaxWidth()) {

        // ── Separator: gradient rule above the bar ────────────────────────────
        // In light mode: soft lavender line for elegant separation
        // In dark mode: subtle purple glow line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color.Transparent,
                            if (colors.isDark) Primary.copy(alpha = 0.10f)
                            else colors.border.copy(alpha = 0.55f),
                            Primary.copy(alpha = if (colors.isDark) 0.18f else 0.08f),
                            if (colors.isDark) Primary.copy(alpha = 0.10f)
                            else colors.border.copy(alpha = 0.55f),
                            Color.Transparent,
                        )
                    )
                )
        )

        // ── Nav pill container ────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.background)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation    = if (colors.isDark) 28.dp else 10.dp,
                        shape        = RoundedCornerShape(28.dp),
                        spotColor    = Primary.copy(alpha = if (colors.isDark) 0.24f else 0.09f),
                        ambientColor = Primary.copy(alpha = if (colors.isDark) 0.14f else 0.05f),
                    )
                    .clip(RoundedCornerShape(28.dp))
                    .background(colors.surface)
                    .border(
                        width = 1.dp,
                        color = if (colors.isDark)
                            Primary.copy(alpha = 0.13f)
                        else
                            colors.border.copy(alpha = 0.65f),
                        shape = RoundedCornerShape(28.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                bottomTabs.forEach { tab ->
                    PremiumNavItem(
                        tab        = tab,
                        isSelected = currentRoute == tab.screen.route,
                        isDark     = colors.isDark,
                        onClick    = { onNavigate(tab) },
                    )
                }
            }
        }
    }
}

// ─── Premium Nav Item ─────────────────────────────────────────────────────────

@Composable
fun NavItem(tab: TabItem, isSelected: Boolean, onClick: () -> Unit) {
    PremiumNavItem(tab = tab, isSelected = isSelected, isDark = LocalOntimeColors.current.isDark, onClick = onClick)
}

@Composable
private fun PremiumNavItem(
    tab:        TabItem,
    isSelected: Boolean,
    isDark:     Boolean,
    onClick:    () -> Unit,
) {
    // Unselected icon tint:
    //   Dark  → dark purple-gray (feels recessed but still premium)
    //   Light → soft lavender-gray (muted but warm)
    val unselectedColor = if (isDark) Color(0xFF555875) else Color(0xFFB0ACCC)

    val iconTint by animateColorAsState(
        targetValue   = if (isSelected) Primary else unselectedColor,
        animationSpec = tween(durationMillis = 200),
        label         = "iconTint_${tab.label}",
    )
    val iconScale by animateFloatAsState(
        targetValue   = if (isSelected) 1.10f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness    = Spring.StiffnessMediumLow,
        ),
        label = "scale_${tab.label}",
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication        = null,  // No ripple — the pill bg is the indicator
                onClick           = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(
                    color = if (isSelected)
                        Primary.copy(alpha = if (isDark) 0.13f else 0.09f)
                    else
                        Color.Transparent
                )
                .padding(
                    horizontal = if (isSelected) 16.dp else 12.dp,
                    vertical   = 9.dp,
                ),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector        = tab.icon,
                contentDescription = tab.label,
                tint               = iconTint,
                modifier           = Modifier
                    .size(19.dp)
                    .scale(iconScale),
            )

            // Label slides in/out with a spring on tab change
            AnimatedVisibility(
                visible = isSelected,
                enter   = expandHorizontally(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness    = Spring.StiffnessMedium,
                    ),
                    expandFrom = Alignment.Start,
                ) + fadeIn(tween(120)),
                exit    = shrinkHorizontally(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness    = Spring.StiffnessMedium,
                    ),
                    shrinkTowards = Alignment.Start,
                ) + fadeOut(tween(80)),
            ) {
                Text(
                    text       = tab.label,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color      = iconTint,
                )
            }
        }
    }
}
