package com.tushartamrakar.ontime.onboarding.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.tushartamrakar.ontime.core.ui.theme.Background
import com.tushartamrakar.ontime.core.ui.theme.Border
import com.tushartamrakar.ontime.core.ui.theme.Danger
import com.tushartamrakar.ontime.core.ui.theme.MulishFamily
import com.tushartamrakar.ontime.core.ui.theme.Primary
import com.tushartamrakar.ontime.core.ui.theme.Success
import com.tushartamrakar.ontime.core.ui.theme.Surface
import com.tushartamrakar.ontime.core.ui.theme.SurfaceHigh
import com.tushartamrakar.ontime.core.ui.theme.TextMuted
import com.tushartamrakar.ontime.core.ui.theme.TextPrimary
import com.tushartamrakar.ontime.core.ui.theme.TextSecondary
import com.tushartamrakar.ontime.core.ui.theme.Warning
import com.tushartamrakar.ontime.focus.overlay.BatteryOptimizationManager
import com.tushartamrakar.ontime.focus.overlay.OverlayPermissionManager
import com.tushartamrakar.ontime.navigation.Screen
import kotlinx.coroutines.launch

// ─── Onboarding state helpers ─────────────────────────────────────────────────

private const val PREFS_NAME          = "ontime_onboarding"
private const val KEY_COMPLETED       = "onboarding_completed"

fun isOnboardingCompleted(context: Context): Boolean =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_COMPLETED, false)

private fun markOnboardingCompleted(context: Context) =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit().putBoolean(KEY_COMPLETED, true).apply()

// ─────────────────────────────────────────────────────────────────────────────

private enum class PageType { INTRO, NOTIFICATION, BATTERY, OVERLAY, DONE }

private data class OnboardingPage(
    val type:        PageType,
    val emoji:       String,
    val accentColor: Color,
    val title:       String,
    val subtitle:    String,
    val description: String,
    val grantLabel:  String = "",
)

// ─────────────────────────────────────────────────────────────────────────────
//  OnboardingScreen
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun OnboardingScreen(navController: NavController) {
    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    // ── Permission states ─────────────────────────────────────────────────────
    var hasNotif   by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED
            else true  // <13 notifications always allowed
        )
    }
    var hasBattery by remember { mutableStateOf(BatteryOptimizationManager.isIgnoring(context)) }
    var hasOverlay by remember { mutableStateOf(OverlayPermissionManager.hasPermission(context)) }

    // ── Permission launchers ──────────────────────────────────────────────────
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasNotif = granted }

    val batteryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { hasBattery = BatteryOptimizationManager.isIgnoring(context) }

    val overlayLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { hasOverlay = OverlayPermissionManager.hasPermission(context) }

    // ── Pages ─────────────────────────────────────────────────────────────────
    val pages = listOf(
        OnboardingPage(
            type        = PageType.INTRO,
            emoji       = "⏰",
            accentColor = Primary,
            title       = "Welcome to Ontime",
            subtitle    = "Your ultimate productivity companion",
            description = "Smart alarms that never miss, deep focus sessions, and a period tracker — all in one beautiful app built around your life.",
        ),
        OnboardingPage(
            type        = PageType.NOTIFICATION,
            emoji       = "🔔",
            accentColor = Color(0xFF06B6D4),  // cyan
            title       = "Alarms That Talk",
            subtitle    = "Allow notifications",
            description = "Without this permission, your alarms will fire silently. Ontime needs to send you notifications to wake you up and remind you of events.",
            grantLabel  = "Allow Notifications",
        ),
        OnboardingPage(
            type        = PageType.BATTERY,
            emoji       = "⚡",
            accentColor = Warning,
            title       = "Alarms That Always Fire",
            subtitle    = "Disable battery optimization",
            description = "Android can kill background apps to save power, silencing alarms mid-sleep. Excluding Ontime makes it as reliable as your phone's built-in clock.",
            grantLabel  = "Exclude from Battery Saver",
        ),
        OnboardingPage(
            type        = PageType.OVERLAY,
            emoji       = "📱",
            accentColor = Color(0xFF8B5CF6),  // violet
            title       = "Alarms Over Your Screen",
            subtitle    = "Display over other apps",
            description = "Shows alarm dismiss screens on top of everything — including the lock screen — so you can't accidentally ignore an alarm by opening another app.",
            grantLabel  = "Allow Display Over Apps",
        ),
        OnboardingPage(
            type        = PageType.DONE,
            emoji       = "🎉",
            accentColor = Success,
            title       = "You're All Set!",
            subtitle    = "Ontime is ready to go",
            description = "Your alarms will fire reliably every time. You can configure additional protections like Focus Guard and Device Lock in Settings whenever you're ready.",
        ),
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val currentPage = pagerState.currentPage

    // Background color animates with page
    val bgTint by animateColorAsState(
        targetValue   = pages[currentPage].accentColor.copy(alpha = 0.08f),
        animationSpec = tween(400),
        label         = "bg_tint",
    )

    fun isPageGranted(type: PageType) = when (type) {
        PageType.NOTIFICATION -> hasNotif
        PageType.BATTERY      -> hasBattery
        PageType.OVERLAY      -> hasOverlay
        else                  -> true
    }

    fun onGrant(type: PageType) = when (type) {
        PageType.NOTIFICATION -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                hasNotif = true
            }
        }
        PageType.BATTERY -> batteryLauncher.launch(
            android.content.Intent(
                android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                android.net.Uri.parse("package:${context.packageName}"),
            )
        )
        PageType.OVERLAY -> overlayLauncher.launch(
            android.content.Intent(
                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:${context.packageName}"),
            )
        )
        else -> {}
    }

    fun finishOnboarding() {
        markOnboardingCompleted(context)
        navController.navigate(Screen.Alarms.route) {
            popUpTo(0) { inclusive = true }
        }
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .background(
                Brush.radialGradient(
                    colors = listOf(bgTint, Color.Transparent),
                    radius = 1200f,
                )
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {

            // ── Skip button ───────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                if (currentPage < pages.size - 1) {
                    Text(
                        text       = "Skip",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MulishFamily,
                        color      = TextMuted,
                        modifier   = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { finishOnboarding() }
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }

            // ── Pager ─────────────────────────────────────────────────────────
            HorizontalPager(
                state    = pagerState,
                modifier = Modifier.weight(1f),
            ) { pageIndex ->
                val page    = pages[pageIndex]
                val granted = isPageGranted(page.type)
                OnboardingPageContent(
                    page    = page,
                    granted = granted,
                    onGrant = { onGrant(page.type) },
                )
            }

            // ── Progress dots ─────────────────────────────────────────────────
            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                pages.forEachIndexed { i, _ ->
                    val isActive = i == currentPage
                    val dotWidth by animateFloatAsState(
                        targetValue   = if (isActive) 24f else 8f,
                        animationSpec = spring(Spring.DampingRatioMediumBouncy),
                        label         = "dot_$i",
                    )
                    val dotColor by animateColorAsState(
                        targetValue   = if (isActive) pages[currentPage].accentColor else SurfaceHigh,
                        animationSpec = tween(300),
                        label         = "dot_color_$i",
                    )
                    Box(
                        modifier = Modifier
                            .width(dotWidth.dp)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(dotColor),
                    )
                    if (i < pages.size - 1) Spacer(Modifier.width(6.dp))
                }
            }

            // ── Action button ─────────────────────────────────────────────────
            val page    = pages[currentPage]
            val granted = isPageGranted(page.type)
            val isLast  = currentPage == pages.size - 1

            val btnColor = when {
                isLast  -> Success
                !granted && page.type != PageType.INTRO -> page.accentColor
                else    -> Primary
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(btnColor)
                    .clickable {
                        when {
                            isLast -> finishOnboarding()
                            currentPage == 0 -> scope.launch {
                                pagerState.animateScrollToPage(1)
                            }
                            !granted -> onGrant(page.type)
                            else -> scope.launch {
                                pagerState.animateScrollToPage(currentPage + 1)
                            }
                        }
                    }
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when {
                        isLast  -> "Let's Go 🚀"
                        currentPage == 0 -> "Get Started"
                        granted -> "Continue →"
                        else    -> page.grantLabel
                    },
                    fontSize      = 16.sp,
                    fontWeight    = FontWeight.Black,
                    fontFamily    = MulishFamily,
                    color         = Color.White,
                    letterSpacing = 0.3.sp,
                )
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─── Page content composable ──────────────────────────────────────────────────

@Composable
private fun OnboardingPageContent(
    page:    OnboardingPage,
    granted: Boolean,
    onGrant: () -> Unit,
) {
    val isPermissionPage = page.type !in listOf(PageType.INTRO, PageType.DONE)

    // Pulse animation for emoji
    val pulseScale by animateFloatAsState(
        targetValue   = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "emoji_scale",
    )

    Column(
        modifier            = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {

        // ── Emoji container ───────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .size(100.dp)
                .scale(pulseScale)
                .clip(CircleShape)
                .background(page.accentColor.copy(alpha = 0.12f))
                .border(1.dp, page.accentColor.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = page.emoji, fontSize = 44.sp)
        }

        Spacer(Modifier.height(32.dp))

        // ── Title ─────────────────────────────────────────────────────────────
        Text(
            text       = page.title,
            fontSize   = 26.sp,
            fontWeight = FontWeight.Black,
            fontFamily = MulishFamily,
            color      = TextPrimary,
            textAlign  = TextAlign.Center,
            lineHeight = 32.sp,
        )

        Spacer(Modifier.height(8.dp))

        // ── Subtitle ──────────────────────────────────────────────────────────
        Text(
            text       = page.subtitle,
            fontSize   = 14.sp,
            fontWeight = FontWeight.SemiBold,
            fontFamily = MulishFamily,
            color      = page.accentColor,
            textAlign  = TextAlign.Center,
        )

        Spacer(Modifier.height(16.dp))

        // ── Description ───────────────────────────────────────────────────────
        Text(
            text       = page.description,
            fontSize   = 14.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = MulishFamily,
            color      = TextSecondary,
            textAlign  = TextAlign.Center,
            lineHeight = 22.sp,
        )

        // ── Permission status card ────────────────────────────────────────────
        if (isPermissionPage) {
            Spacer(Modifier.height(28.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (granted) Success.copy(alpha = 0.08f)
                        else Warning.copy(alpha = 0.08f)
                    )
                    .border(
                        1.dp,
                        if (granted) Success.copy(alpha = 0.30f)
                        else Warning.copy(alpha = 0.30f),
                        RoundedCornerShape(14.dp),
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (granted) Success.copy(alpha = 0.15f)
                            else Warning.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (granted) {
                        Icon(
                            imageVector        = Icons.Filled.Check,
                            contentDescription = null,
                            tint               = Success,
                            modifier           = Modifier.size(16.dp),
                        )
                    } else {
                        Text("!", fontSize = 16.sp, fontWeight = FontWeight.Black,
                            color = Warning, fontFamily = MulishFamily)
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text       = if (granted) "Permission granted" else "Permission required",
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = MulishFamily,
                        color      = if (granted) Success else Warning,
                    )
                    Text(
                        text       = if (granted)
                            "You're all set for this step ✓"
                        else
                            "Tap the button below to allow",
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = MulishFamily,
                        color      = TextMuted,
                    )
                }
            }
        }

        // ── Done page: permission summary ─────────────────────────────────────
        if (page.type == PageType.DONE) {
            Spacer(Modifier.height(24.dp))
            val context = LocalContext.current
            val items = listOf(
                Triple("Notifications", if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                        PackageManager.PERMISSION_GRANTED else true, "Alarms will notify you"),
                Triple("Battery", BatteryOptimizationManager.isIgnoring(context), "Alarms will always fire"),
                Triple("Overlay", OverlayPermissionManager.hasPermission(context), "Lock screen alarms"),
            )
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Surface)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items.forEach { (name, ok, desc) ->
                    Row(
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    if (ok) Success.copy(alpha = 0.15f)
                                    else Danger.copy(alpha = 0.10f)
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text       = if (ok) "✓" else "✗",
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.Black,
                                color      = if (ok) Success else Danger,
                                fontFamily = MulishFamily,
                            )
                        }
                        Column {
                            Text(
                                text       = name,
                                fontSize   = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = MulishFamily,
                                color      = TextPrimary,
                            )
                            Text(
                                text       = if (ok) desc else "Can be granted in Settings",
                                fontSize   = 10.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = MulishFamily,
                                color      = if (ok) TextMuted else Danger.copy(alpha = 0.7f),
                            )
                        }
                    }
                }
            }
        }
    }
}
