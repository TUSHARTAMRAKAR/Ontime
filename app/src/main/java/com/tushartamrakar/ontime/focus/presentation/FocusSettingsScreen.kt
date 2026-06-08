package com.tushartamrakar.ontime.focus.presentation

import android.provider.Settings
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoNotDisturb
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhonelinkSetup
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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
import com.tushartamrakar.ontime.focus.admin.DeviceAdminManager
import com.tushartamrakar.ontime.focus.blocker.FocusWebBlocklist
import com.tushartamrakar.ontime.focus.overlay.BatteryOptimizationManager
import com.tushartamrakar.ontime.focus.overlay.DndPermissionManager
import com.tushartamrakar.ontime.focus.overlay.OverlayPermissionManager
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.EntryPoint

// ─── Hilt entry point — gets FocusWebBlocklist singleton in a Composable ─────

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WebBlocklistEntryPoint {
    fun focusWebBlocklist(): FocusWebBlocklist
}

@Composable
fun FocusSettingsScreen(
    navController: NavController,
    viewModel: FocusViewModel = hiltViewModel(),
) {
    val settings          by viewModel.settings.collectAsState()
    val todaySessionCount by viewModel.todaySessionCount.collectAsState()
    val alwaysOnMode      by viewModel.alwaysOnMode.collectAsState()
    var showResetDialog   by remember { mutableStateOf(false) }

    val context            = LocalContext.current

    // ── Web blocker ───────────────────────────────────────────────────────────
    val focusWebBlocklist = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            WebBlocklistEntryPoint::class.java,
        ).focusWebBlocklist()
    }
    var showWebBlocker       by remember { mutableStateOf(false) }
    // refreshKey forces row to re-read state after sheet closes
    var webBlockerRefreshKey by remember { mutableStateOf(0) }

    // ── Device Admin state ────────────────────────────────────────────────────
    val adminManager       = remember { DeviceAdminManager(context) }
    var isAdminActive      by remember { mutableStateOf(adminManager.isAdminActive) }
    val adminLauncher      = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { isAdminActive = adminManager.isAdminActive }

    // ── Display Over Apps state ───────────────────────────────────────────────
    var hasOverlayPerm     by remember { mutableStateOf(OverlayPermissionManager.hasPermission(context)) }
    val overlayLauncher    = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { hasOverlayPerm = OverlayPermissionManager.hasPermission(context) }

    // ── DND bypass state ──────────────────────────────────────────────────────
    var hasDndPerm         by remember { mutableStateOf(DndPermissionManager.hasPermission(context)) }
    val dndLauncher        = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { hasDndPerm = DndPermissionManager.hasPermission(context) }

    // ── Battery optimization exclusion ────────────────────────────────────────
    var isBatteryExcluded  by remember { mutableStateOf(BatteryOptimizationManager.isIgnoring(context)) }
    val batteryLauncher    = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { isBatteryExcluded = BatteryOptimizationManager.isIgnoring(context) }

    // ── Accessibility Service state ───────────────────────────────────────────
    var hasAccessibility   by remember {
        mutableStateOf(isAccessibilityServiceEnabled(context))
    }
    val accessibilityLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { hasAccessibility = isAccessibilityServiceEnabled(context) }

    LazyColumn(
        modifier       = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 40.dp),
    ) {

        // ── Top bar ───────────────────────────────────────────────────────────
        item {
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text       = "Focus Settings",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = MulishFamily,
                    color      = TextPrimary,
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(48.dp))
            }
        }

        // ── Info banner ───────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Primary.copy(alpha = 0.08f))
                    .border(1.dp, Primary.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment     = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector        = Icons.Filled.Edit,
                    contentDescription = null,
                    tint               = Primary,
                    modifier           = Modifier.size(15.dp).padding(top = 1.dp),
                )
                Text(
                    text       = "Timer durations, technique type, and blocked apps are now managed from the Focus screen → Edit button inside the timer.",
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = MulishFamily,
                    color      = Primary.copy(alpha = 0.85f),
                    lineHeight = 16.sp,
                )
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── Daily Goal ────────────────────────────────────────────────────────
        item {
            SettingsSectionHeader(
                title    = "Daily Goal",
                subtitle = "How many focus sessions to aim for each day",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }

        item {
            DailyGoalRow(
                modifier       = Modifier.padding(horizontal = 20.dp),
                goal           = settings.dailyGoalSessions,
                completedToday = todaySessionCount,
                onDecrement    = {
                    viewModel.saveSettings(
                        settings.copy(dailyGoalSessions = (settings.dailyGoalSessions - 1).coerceAtLeast(1))
                    )
                },
                onIncrement    = {
                    viewModel.saveSettings(
                        settings.copy(dailyGoalSessions = (settings.dailyGoalSessions + 1).coerceAtMost(16))
                    )
                },
            )
            Spacer(Modifier.height(20.dp))
        }

        // ── Focus Behaviour ───────────────────────────────────────────────────
        item {
            SettingsSectionHeader(
                title    = "Focus Behaviour",
                subtitle = "What happens during your sessions",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }

        item {
            ToggleSettingsRow(
                icon      = Icons.Filled.DoNotDisturb,
                iconTint  = Primary,
                label     = "Do Not Disturb",
                subtitle  = "Silence all notifications during focus sessions",
                checked   = settings.enableDndDuringFocus,
                onChecked = { viewModel.saveSettings(settings.copy(enableDndDuringFocus = it)) },
                modifier  = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(8.dp))
            // DND bypass permission card — shown right below the toggle so
            // context is obvious: "you enabled DND, now let alarms ring through it"
            PermissionCard(
                icon           = Icons.Filled.Notifications,
                iconTint        = Warning,
                title          = "Ring Through DND",
                description    = "Allows Ontime alarms and session-end alerts to ring even when Do Not Disturb is active. Without this, DND will silence your focus alarms.",
                isGranted      = hasDndPerm,
                grantedLabel   = "DND Bypass Active",
                ungrantedLabel = "Allow Alarms in DND",
                onRequest      = {
                    dndLauncher.launch(
                        android.content.Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    )
                },
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(20.dp))
        }

        // ── Protection ────────────────────────────────────────────────────────
        item {
            SettingsSectionHeader(
                title    = "Protection",
                subtitle = "Always-on content safety",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }

        item {
            ToggleSettingsRow(
                icon      = Icons.Filled.Shield,
                iconTint  = Success,
                label     = "Adult Content Filter",
                subtitle  = "Always-on DNS filter — 50k+ blocked domains",
                checked   = settings.adultFilterEnabled,
                onChecked = { viewModel.toggleAdultFilter(it) },
                modifier  = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(8.dp))
            ToggleSettingsRow(
                icon      = Icons.Filled.Lock,
                iconTint  = Primary,
                label     = "Always-On App Blocker",
                subtitle  = if (alwaysOnMode)
                    "🔒 Blocking apps right now — no session needed"
                else
                    "Block selected apps even without a focus session",
                checked   = alwaysOnMode,
                onChecked = { viewModel.setAlwaysOnMode(it) },
                modifier  = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(20.dp))
        }

        // ── Website Blocker ───────────────────────────────────────────────────
        item {
            SettingsSectionHeader(
                title    = "Website Blocker",
                subtitle = "Block distracting sites during focus sessions",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }

        item {
            val isWebBlockerEnabled = remember(webBlockerRefreshKey) { focusWebBlocklist.isEnabled }
            val webBlockerCount     = remember(webBlockerRefreshKey) { focusWebBlocklist.blockedDomainCount() }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Surface)
                    .border(
                        1.dp,
                        if (isWebBlockerEnabled) Primary.copy(alpha = 0.20f) else Border,
                        RoundedCornerShape(14.dp),
                    )
                    .clickable { showWebBlocker = true }
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector        = Icons.Filled.Language,
                        contentDescription = null,
                        tint               = Primary,
                        modifier           = Modifier.size(18.dp),
                    )
                }
                Column(
                    modifier            = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        "Website Blocker",
                        fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                        fontFamily = MulishFamily, color = TextPrimary,
                    )
                    Text(
                        if (isWebBlockerEnabled && webBlockerCount > 0)
                            "$webBlockerCount sites blocked during focus"
                        else
                            "Tap to configure blocked sites",
                        fontSize = 11.sp, fontWeight = FontWeight.Medium,
                        fontFamily = MulishFamily,
                        color = if (isWebBlockerEnabled) Primary.copy(alpha = 0.8f) else TextMuted,
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isWebBlockerEnabled) Primary.copy(alpha = 0.15f) else SurfaceHigh
                        )
                        .border(
                            1.dp,
                            if (isWebBlockerEnabled) Primary.copy(alpha = 0.30f) else Border,
                            RoundedCornerShape(8.dp),
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        if (isWebBlockerEnabled) "ON" else "OFF",
                        fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                        fontFamily = MulishFamily,
                        color = if (isWebBlockerEnabled) Primary else TextMuted,
                    )
                }
                Icon(
                    Icons.Filled.ChevronRight, null,
                    tint     = TextMuted.copy(alpha = 0.4f),
                    modifier = Modifier.size(14.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── Background Reliability ────────────────────────────────────────────
        item {
            SettingsSectionHeader(
                title    = "Background Reliability",
                subtitle = "Prevent Android from killing Ontime mid-session",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }

        item {
            BatteryOptimizationCard(
                isExcluded = isBatteryExcluded,
                onRequest  = {
                    batteryLauncher.launch(
                        android.content.Intent(
                            android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                            android.net.Uri.parse("package:${context.packageName}"),
                        )
                    )
                },
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(20.dp))
        }

        // ── App Protection ────────────────────────────────────────────────────
        item {
            SettingsSectionHeader(
                title    = "App Protection",
                subtitle = "Make Ontime impossible to delete mid-commitment",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }

        item {
            AppProtectionCard(
                isActive   = isAdminActive,
                onActivate = { adminLauncher.launch(adminManager.buildActivationIntent()) },
                onDeactivate = {
                    adminManager.deactivateAdmin()
                    isAdminActive = false
                },
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(20.dp))
        }

        // ── Display Over Apps ─────────────────────────────────────────────────
        item {
            SettingsSectionHeader(
                title    = "Display Over Apps",
                subtitle = "Show alarm screens over lock screen and any app",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
        item {
            PermissionCard(
                icon        = Icons.Filled.Notifications,
                iconTint    = Warning,
                title       = "Alarm Overlay",
                description = "Lets Ontime show focus reminders and alarm dismiss screens on top of everything — including your lock screen — so you never miss a session end.",
                isGranted   = hasOverlayPerm,
                grantedLabel   = "Overlay Active",
                ungrantedLabel = "Enable Overlay",
                onRequest   = {
                    overlayLauncher.launch(
                        android.content.Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            android.net.Uri.parse("package:${context.packageName}"),
                        )
                    )
                },
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(20.dp))
        }

        // ── Accessibility Service ─────────────────────────────────────────────
        item {
            SettingsSectionHeader(
                title    = "Focus Guard",
                subtitle = "Block distracting apps + detect power button during sessions",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
        item {
            PermissionCard(
                icon        = Icons.Filled.PhonelinkSetup,
                iconTint    = Primary,
                title       = "Accessibility Service",
                description = "When enabled, Ontime instantly closes any blocked app the moment you open it during a focus session. Also detects the power menu so your session survives.",
                isGranted   = hasAccessibility,
                grantedLabel   = "Focus Guard Active",
                ungrantedLabel = "Enable Focus Guard",
                onRequest   = {
                    accessibilityLauncher.launch(
                        android.content.Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    )
                },
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(20.dp))
        }

        // ── Data ──────────────────────────────────────────────────────────────
        item {
            SettingsSectionHeader(
                title    = "Data",
                subtitle = "Manage your focus history",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Danger.copy(alpha = 0.08f))
                    .border(1.dp, Danger.copy(alpha = 0.20f), RoundedCornerShape(14.dp))
                    .clickable { showResetDialog = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Danger.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector        = Icons.Filled.Delete,
                        contentDescription = null,
                        tint               = Danger,
                        modifier           = Modifier.size(20.dp),
                    )
                }
                Column(
                    modifier            = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text       = "Reset all focus data",
                        fontSize   = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = MulishFamily,
                        color      = Danger,
                    )
                    Text(
                        text       = "Clears all sessions, streaks, and stats permanently",
                        fontSize   = 11.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = MulishFamily,
                        color      = Danger.copy(alpha = 0.65f),
                    )
                }
                Icon(
                    imageVector        = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint               = Danger.copy(alpha = 0.5f),
                    modifier           = Modifier.size(18.dp),
                )
            }
        }
    }

    // ── Web Blocker Sheet ─────────────────────────────────────────────────────
    if (showWebBlocker) {
        WebBlockerSheet(
            blocklist = focusWebBlocklist,
            onDismiss = {
                webBlockerRefreshKey++   // force row to re-read updated state
                showWebBlocker = false
            },
        )
    }

    // ── Reset confirmation dialog ─────────────────────────────────────────────
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor   = Surface,
            shape            = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text       = "Reset all data?",
                    fontFamily = MulishFamily,
                    fontWeight = FontWeight.Black,
                    color      = TextPrimary,
                )
            },
            text = {
                Text(
                    text       = "This will permanently delete all your focus sessions, streaks, and statistics. This cannot be undone.",
                    fontFamily = MulishFamily,
                    fontWeight = FontWeight.Medium,
                    color      = TextSecondary,
                    lineHeight = 20.sp,
                )
            },
            confirmButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Reset", color = Danger, fontFamily = MulishFamily, fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = TextMuted, fontFamily = MulishFamily, fontWeight = FontWeight.Bold)
                }
            },
        )
    }
}

// ─── Battery Optimization Card ────────────────────────────────────────────────

@Composable
private fun BatteryOptimizationCard(
    isExcluded: Boolean,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (isExcluded) Success.copy(alpha = 0.40f) else Warning.copy(alpha = 0.40f)
    val bgColor     = if (isExcluded) Success.copy(alpha = 0.06f) else Warning.copy(alpha = 0.06f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ── Header row ────────────────────────────────────────────────────────
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isExcluded) Success.copy(alpha = 0.15f)
                        else Warning.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text     = if (isExcluded) "⚡" else "🔋",
                    fontSize = 22.sp,
                )
            }

            Column(
                modifier            = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text       = "Battery Optimization",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color      = TextPrimary,
                )
                Text(
                    text       = if (isExcluded) "Ontime runs unrestricted"
                                 else "⚠️ Alarms may be silenced",
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = MulishFamily,
                    color      = if (isExcluded) Success else Warning,
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isExcluded) Success.copy(alpha = 0.15f)
                        else Warning.copy(alpha = 0.15f)
                    )
                    .border(
                        1.dp,
                        if (isExcluded) Success.copy(alpha = 0.35f)
                        else Warning.copy(alpha = 0.35f),
                        RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text       = if (isExcluded) "ON" else "OFF",
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color      = if (isExcluded) Success else Warning,
                )
            }
        }

        // ── Description ───────────────────────────────────────────────────────
        Text(
            text       = if (isExcluded)
                "Android will not restrict Ontime in the background. Alarms, session timers, and reminders will fire reliably at all times."
            else
                "Android's battery optimization can kill Ontime mid-session, silencing alarms and losing your progress. Exclude Ontime to prevent this.",
            fontSize   = 11.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = MulishFamily,
            color      = TextSecondary,
            lineHeight = 16.sp,
        )

        // ── OEM note (shown only when not yet excluded) ───────────────────────
        if (!isExcluded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(SurfaceHigh)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text       = "📱 After granting, also check phone-specific settings:",
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color      = TextSecondary,
                )
                OemHint("Samsung", "Settings → Apps → Ontime → Battery → Unrestricted")
                OemHint("Xiaomi",  "Settings → Apps → Manage Apps → Ontime → No restrictions")
                OemHint("OnePlus", "Settings → Battery → Optimization → Ontime → Don't optimize")
                OemHint("Huawei",  "Settings → Apps → Ontime → Battery → Disable power-intensive")
            }
        }

        // ── Action button (only when not excluded) ────────────────────────────
        if (!isExcluded) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Warning.copy(alpha = 0.12f))
                    .border(1.dp, Warning.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
                    .clickable { onRequest() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = "Exclude from Battery Optimization",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color      = Warning,
                )
            }
        }
    }
}

@Composable
private fun OemHint(brand: String, instruction: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text       = "• $brand:",
            fontSize   = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = MulishFamily,
            color      = TextMuted,
        )
        Text(
            text       = instruction,
            fontSize   = 10.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = MulishFamily,
            color      = TextMuted,
        )
    }
}

// ─── Permission Card ──────────────────────────────────────────────────────────

@Composable
private fun PermissionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: androidx.compose.ui.graphics.Color,
    title: String,
    description: String,
    isGranted: Boolean,
    grantedLabel: String,
    ungrantedLabel: String,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (isGranted) Success.copy(alpha = 0.40f) else Border
    val bgColor     = if (isGranted) Success.copy(alpha = 0.06f) else Surface

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isGranted) Success.copy(alpha = 0.15f) else SurfaceHigh),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = icon,
                    contentDescription = null,
                    tint               = if (isGranted) Success else iconTint,
                    modifier           = Modifier.size(22.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text       = title,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color      = TextPrimary,
                )
                Text(
                    text       = if (isGranted) "Permission granted" else "Permission required",
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = MulishFamily,
                    color      = if (isGranted) Success else TextMuted,
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isGranted) Success.copy(alpha = 0.15f) else SurfaceHigh)
                    .border(1.dp, if (isGranted) Success.copy(alpha = 0.35f) else Border, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text       = if (isGranted) "ON" else "OFF",
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color      = if (isGranted) Success else TextMuted,
                )
            }
        }

        Text(
            text       = description,
            fontSize   = 11.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = MulishFamily,
            color      = TextSecondary,
            lineHeight = 16.sp,
        )

        if (!isGranted) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconTint.copy(alpha = 0.12f))
                    .border(1.dp, iconTint.copy(alpha = 0.30f), RoundedCornerShape(10.dp))
                    .clickable { onRequest() }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = ungrantedLabel,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color      = iconTint,
                )
            }
        }
    }
}

// ─── Accessibility Service check ──────────────────────────────────────────────

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    // ENABLED_ACCESSIBILITY_SERVICES stores a colon-separated list of
    // "packageName/fully.qualified.ClassName" — NOT the dot shorthand used in manifests.
    val expected = "${context.packageName}/${context.packageName}" +
                   ".focus.accessibility.OntimeFocusAccessibilityService"
    val enabled  = android.provider.Settings.Secure.getString(
        context.contentResolver,
        android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ) ?: return false
    return enabled.split(":").any { it.equals(expected, ignoreCase = true) }
}

// ─── App Protection Card ──────────────────────────────────────────────────────

@Composable
private fun AppProtectionCard(
    isActive: Boolean,
    onActivate: () -> Unit,
    onDeactivate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (isActive) Success.copy(alpha = 0.40f) else Border
    val bgColor     = if (isActive) Success.copy(alpha = 0.06f) else Surface

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Top row: icon + title + status badge
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isActive) Success.copy(alpha = 0.15f)
                        else SurfaceHigh
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = if (isActive) Icons.Filled.Lock else Icons.Filled.LockOpen,
                    contentDescription = null,
                    tint               = if (isActive) Success else TextMuted,
                    modifier           = Modifier.size(22.dp),
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text       = "Undeletable Mode",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color      = TextPrimary,
                )
                Text(
                    text       = if (isActive)
                        "App is protected — cannot be uninstalled"
                    else
                        "App can currently be deleted",
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = MulishFamily,
                    color      = if (isActive) Success else TextMuted,
                )
            }

            // Status badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isActive) Success.copy(alpha = 0.15f)
                        else SurfaceHigh
                    )
                    .border(
                        1.dp,
                        if (isActive) Success.copy(alpha = 0.35f) else Border,
                        RoundedCornerShape(8.dp),
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
                Text(
                    text       = if (isActive) "ON" else "OFF",
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color      = if (isActive) Success else TextMuted,
                )
            }
        }

        // Description
        Text(
            text       = if (isActive)
                "Device Admin is active. To uninstall Ontime, you must first go to " +
                "Settings → Security → Device Admins and remove it manually."
            else
                "Grant Device Admin access to make Ontime impossible to delete " +
                "mid-session. Perfect for hardcore accountability.",
            fontSize   = 11.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = MulishFamily,
            color      = TextSecondary,
            lineHeight = 16.sp,
        )

        // Action button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (isActive) Danger.copy(alpha = 0.10f)
                    else Primary.copy(alpha = 0.15f)
                )
                .border(
                    1.dp,
                    if (isActive) Danger.copy(alpha = 0.30f) else Primary.copy(alpha = 0.30f),
                    RoundedCornerShape(10.dp),
                )
                .clickable { if (isActive) onDeactivate() else onActivate() }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector        = if (isActive) Icons.Filled.LockOpen else Icons.Filled.Lock,
                    contentDescription = null,
                    tint               = if (isActive) Danger else Primary,
                    modifier           = Modifier.size(16.dp),
                )
                Text(
                    text       = if (isActive) "Remove Protection" else "Activate Protection",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color      = if (isActive) Danger else Primary,
                )
            }
        }
    }
}

// ─── Daily Goal Row ───────────────────────────────────────────────────────────

@Composable
private fun DailyGoalRow(
    goal: Int,
    completedToday: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val goalMet     = completedToday >= goal
    val progressPct = (completedToday.toFloat() / goal.toFloat()).coerceIn(0f, 1f)

    Column(
        modifier            = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier          = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector        = Icons.Filled.Flag,
                    contentDescription = null,
                    tint               = Primary,
                    modifier           = Modifier.size(18.dp),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text       = "Daily goal",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color      = TextPrimary,
                )
                Text(
                    text       = if (goalMet) "✓ Goal reached today!"
                                 else "$completedToday of $goal sessions today",
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = MulishFamily,
                    color      = if (goalMet) Success else TextMuted,
                )
            }
            // Stepper
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (goal > 1) SurfaceHigh else SurfaceHigh.copy(alpha = 0.5f))
                        .clickable(enabled = goal > 1) { onDecrement() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector        = Icons.Filled.Remove,
                        contentDescription = "Decrease",
                        tint               = if (goal > 1) TextPrimary else TextMuted,
                        modifier           = Modifier.size(14.dp),
                    )
                }
                Text(
                    text       = "$goal",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color      = Primary,
                    modifier   = Modifier.width(28.dp),
                    textAlign  = TextAlign.Center,
                )
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(
                            if (goal < 16) Primary.copy(alpha = 0.15f)
                            else SurfaceHigh.copy(alpha = 0.5f)
                        )
                        .clickable(enabled = goal < 16) { onIncrement() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector        = Icons.Filled.Add,
                        contentDescription = "Increase",
                        tint               = if (goal < 16) Primary else TextMuted,
                        modifier           = Modifier.size(14.dp),
                    )
                }
            }
        }

        // Today's progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(SurfaceHigh),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progressPct)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (goalMet) Success else Primary),
            )
        }
    }
}

// ─── Toggle Settings Row ──────────────────────────────────────────────────────

@Composable
private fun ToggleSettingsRow(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    subtitle: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                tint               = iconTint,
                modifier           = Modifier.size(18.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text       = label,
                fontSize   = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily,
                color      = TextPrimary,
            )
            Text(
                text       = subtitle,
                fontSize   = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = MulishFamily,
                color      = TextMuted,
            )
        }
        Switch(
            checked         = checked,
            onCheckedChange = onChecked,
            modifier        = Modifier.scale(0.85f),
            colors          = SwitchDefaults.colors(
                checkedTrackColor    = Primary,
                uncheckedTrackColor  = SurfaceHigh,
                checkedThumbColor    = Color.White,
                uncheckedThumbColor  = TextMuted,
                uncheckedBorderColor = Border,
            ),
        )
    }
}

// ─── Section Header ───────────────────────────────────────────────────────────

@Composable
private fun SettingsSectionHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text       = title,
            fontSize   = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = MulishFamily,
            color      = TextPrimary,
        )
        Text(
            text       = subtitle,
            fontSize   = 12.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = MulishFamily,
            color      = TextMuted,
        )
    }
}
