package com.tushartamrakar.ontime.focus.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.tushartamrakar.ontime.core.ui.theme.Background
import com.tushartamrakar.ontime.core.ui.theme.Border
import com.tushartamrakar.ontime.core.ui.theme.Danger
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
import com.tushartamrakar.ontime.focus.data.local.AppCategory
import com.tushartamrakar.ontime.focus.data.local.BlockedAppEntity
import com.tushartamrakar.ontime.focus.data.local.DefaultBlockedPackages

@Composable
fun BlockerScreen(
    navController: NavController,
    viewModel: FocusViewModel = hiltViewModel(),
) {
    val settings           by viewModel.settings.collectAsState()
    val blockedApps        by viewModel.blockedApps.collectAsState()
    val installedApps      by viewModel.installedApps.collectAsState()
    val isLoadingApps      by viewModel.isLoadingApps.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var expandedCategory by remember { mutableStateOf<AppCategory?>(null) }

    // Load installed apps when screen opens
    LaunchedEffect(Unit) { viewModel.loadInstalledApps() }

    // Build lookup map of blocked packages for O(1) checks in the list
    val blockedPackageMap = remember(blockedApps) {
        blockedApps.associateBy { it.packageName }
    }

    // Filtered app list by search
    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) installedApps
        else installedApps.filter {
            it.appName.contains(searchQuery, ignoreCase = true) ||
            it.packageName.contains(searchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier        = Modifier
            .fillMaxSize()
            .background(Background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding  = PaddingValues(bottom = 40.dp),
    ) {

        // ── Top bar ───────────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        imageVector        = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint               = TextPrimary,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text       = "Blocker",
                    fontSize   = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = MulishFamily,
                    color      = TextPrimary,
                )
                Spacer(Modifier.weight(1f))
                Spacer(Modifier.size(48.dp))
            }
        }

        // ── Section 1 — Adult Content Filter ─────────────────────────────────
        item {
            SectionHeader(
                title    = "Adult Content Filter",
                subtitle = "Always-on DNS blocking — no data leaves your device",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }

        item {
            AdultFilterCard(
                isEnabled = settings.adultFilterEnabled,
                onToggle  = { viewModel.toggleAdultFilter(it) },
                modifier  = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(8.dp))
        }

        // ── Section 2 — Focus App Blocker ─────────────────────────────────────
        item {
            SectionHeader(
                title    = "App Blocker",
                subtitle = "Block apps while a focus session is running",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }

        item {
            AppBlockingToggleCard(
                isEnabled = settings.appBlockingEnabled,
                onToggle  = {
                    viewModel.saveSettings(settings.copy(appBlockingEnabled = it))
                },
                modifier  = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(16.dp))
        }

        // ── Section 3 — Category Quick-Blocks ────────────────────────────────
        item {
            SectionHeader(
                title    = "Quick Block by Category",
                subtitle = "Toggle entire app categories at once",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }

        val categories = listOf(
            CategoryInfo(AppCategory.SOCIAL, "Social Media", "📱",
                "Instagram, Twitter, TikTok, Snapchat, Reddit…",
                DefaultBlockedPackages.SOCIAL),
            CategoryInfo(AppCategory.ENTERTAINMENT, "Entertainment", "🎬",
                "YouTube, Netflix, Prime Video, Hotstar…",
                DefaultBlockedPackages.ENTERTAINMENT),
            CategoryInfo(AppCategory.GAMES, "Games", "🎮",
                "Mobile games and gaming apps",
                DefaultBlockedPackages.GAMES),
        )

        items(categories) { cat ->
            CategoryBlockRow(
                categoryInfo   = cat,
                blockedPackageMap = blockedPackageMap,
                installedApps  = installedApps,
                onToggleAll    = { enable ->
                    val appsInCategory = installedApps.filter {
                        cat.packages.contains(it.packageName)
                    }
                    appsInCategory.forEach { app ->
                        if (enable) {
                            viewModel.addBlockedApp(
                                BlockedAppEntity(
                                    packageName         = app.packageName,
                                    appName             = app.appName,
                                    category            = cat.category.name,
                                    isEnabled           = true,
                                    blockOnlyDuringFocus = true,
                                )
                            )
                        } else {
                            viewModel.removeBlockedApp(app.packageName)
                        }
                    }
                },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }

        item { Spacer(Modifier.height(16.dp)) }

        // ── Section 4 — All Apps ─────────────────────────────────────────────
        item {
            SectionHeader(
                title    = "All Installed Apps",
                subtitle = "Toggle any app individually",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }

        // Search bar
        item {
            AppSearchBar(
                query     = searchQuery,
                onQuery   = { searchQuery = it },
                modifier  = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
            Spacer(Modifier.height(8.dp))
        }

        // Loading state
        if (isLoadingApps) {
            item {
                Box(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment  = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(
                            color    = Primary,
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text       = "Loading apps…",
                            fontSize   = 13.sp,
                            fontFamily = MulishFamily,
                            color      = TextMuted,
                        )
                    }
                }
            }
        } else {
            // App rows
            items(filteredApps, key = { it.packageName }) { app ->
                val blockedApp = blockedPackageMap[app.packageName]
                val isBlocked  = blockedApp?.isEnabled == true

                InstalledAppRow(
                    app          = app,
                    isBlocked    = isBlocked,
                    onToggle     = { enabled ->
                        if (enabled) {
                            viewModel.addBlockedApp(
                                BlockedAppEntity(
                                    packageName          = app.packageName,
                                    appName              = app.appName,
                                    category             = AppCategory.CUSTOM.name,
                                    isEnabled            = true,
                                    blockOnlyDuringFocus = true,
                                )
                            )
                        } else {
                            viewModel.removeBlockedApp(app.packageName)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 3.dp),
                )
            }

            if (filteredApps.isEmpty() && searchQuery.isNotBlank()) {
                item {
                    Box(
                        modifier         = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text       = "No apps found for \"$searchQuery\"",
                            fontSize   = 13.sp,
                            fontFamily = MulishFamily,
                            color      = TextMuted,
                        )
                    }
                }
            }
        }
    }
}

// ─── Adult Filter Card ────────────────────────────────────────────────────────

@Composable
private fun AdultFilterCard(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val accentColor = if (isEnabled) Success else TextMuted
    val bgAlpha     = if (isEnabled) 0.1f else 0.04f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(accentColor.copy(alpha = bgAlpha))
            .border(
                1.dp,
                if (isEnabled) accentColor.copy(alpha = 0.35f) else Border,
                RoundedCornerShape(18.dp),
            )
            .padding(18.dp),
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Shield icon
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = if (isEnabled) "🛡️" else "🔓", fontSize = 26.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = "Adult Content Filter",
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color      = TextPrimary,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text       = if (isEnabled)
                        "Active — blocking adult content"
                    else
                        "Disabled — adult content allowed",
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = MulishFamily,
                    color      = accentColor,
                )
            }

            OntimeSwitch(
                checked   = isEnabled,
                onChecked = onToggle,
            )
        }

        AnimatedVisibility(
            visible = isEnabled,
            enter   = fadeIn() + expandVertically(),
            exit    = fadeOut() + shrinkVertically(),
        ) {
            Column {
                Spacer(Modifier.height(14.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(accentColor.copy(alpha = 0.2f))
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatusPill(emoji = "🔒", text = "Always-on")
                    StatusPill(emoji = "📵", text = "50k+ domains")
                    StatusPill(emoji = "🔐", text = "Local only")
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text       = "Works by intercepting DNS queries on your device — no data leaves your phone. Enable device admin in Settings → Security for full uninstall protection.",
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = MulishFamily,
                    color      = TextMuted,
                    lineHeight = 17.sp,
                )
            }
        }
    }
}

// ─── App Blocking Toggle Card ─────────────────────────────────────────────────

@Composable
private fun AppBlockingToggleCard(
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = Icons.Filled.Block,
                contentDescription = null,
                tint               = Primary,
                modifier           = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = "Block apps during focus",
                fontSize   = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily,
                color      = TextPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text       = "Selected apps are blocked when a focus session is running",
                fontSize   = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = MulishFamily,
                color      = TextMuted,
                lineHeight = 17.sp,
            )
        }
        OntimeSwitch(checked = isEnabled, onChecked = onToggle)
    }
}

// ─── Category Block Row ───────────────────────────────────────────────────────

data class CategoryInfo(
    val category: AppCategory,
    val label: String,
    val emoji: String,
    val description: String,
    val packages: Set<String>,
)

@Composable
private fun CategoryBlockRow(
    categoryInfo: CategoryInfo,
    blockedPackageMap: Map<String, BlockedAppEntity>,
    installedApps: List<InstalledAppsLoader.AppInfo>,
    onToggleAll: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Count how many of this category's apps are installed + blocked
    val installedInCategory = installedApps.count { it.packageName in categoryInfo.packages }
    val blockedInCategory   = categoryInfo.packages.count { pkg ->
        blockedPackageMap[pkg]?.isEnabled == true
    }
    val allBlocked = installedInCategory > 0 && blockedInCategory >= installedInCategory

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Emoji badge
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (allBlocked) Danger.copy(alpha = 0.12f) else SurfaceHigh
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = categoryInfo.emoji, fontSize = 20.sp)
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = categoryInfo.label,
                fontSize   = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily,
                color      = TextPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text       = if (installedInCategory == 0)
                    "None installed"
                else if (allBlocked)
                    "All $installedInCategory apps blocked"
                else if (blockedInCategory > 0)
                    "$blockedInCategory of $installedInCategory blocked"
                else
                    categoryInfo.description,
                fontSize   = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = MulishFamily,
                color      = if (allBlocked) Danger else TextMuted,
                maxLines   = 1,
            )
        }

        OntimeSwitch(
            checked   = allBlocked,
            onChecked = onToggleAll,
            enabled   = installedInCategory > 0,
        )
    }
}

// ─── App Search Bar ───────────────────────────────────────────────────────────

@Composable
private fun AppSearchBar(
    query: String,
    onQuery: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value         = query,
        onValueChange = onQuery,
        singleLine    = true,
        textStyle     = TextStyle(
            fontFamily  = MulishFamily,
            fontWeight  = FontWeight.Medium,
            fontSize    = 14.sp,
            color       = TextPrimary,
        ),
        cursorBrush   = SolidColor(Primary),
        modifier      = modifier.fillMaxWidth(),
        decorationBox = { inner ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Surface)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(
                    imageVector        = Icons.Filled.Search,
                    contentDescription = null,
                    tint               = TextMuted,
                    modifier           = Modifier.size(18.dp),
                )
                Box(modifier = Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text       = "Search apps…",
                            fontFamily = MulishFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize   = 14.sp,
                            color      = TextMuted,
                        )
                    }
                    inner()
                }
                if (query.isNotEmpty()) {
                    Icon(
                        imageVector        = Icons.Filled.Close,
                        contentDescription = "Clear",
                        tint               = TextMuted,
                        modifier           = Modifier
                            .size(16.dp)
                            .clickable { onQuery("") },
                    )
                }
            }
        },
    )
}

// ─── Installed App Row ────────────────────────────────────────────────────────

@Composable
private fun InstalledAppRow(
    app: InstalledAppsLoader.AppInfo,
    isBlocked: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isBlocked) Danger.copy(alpha = 0.06f) else Surface)
            .border(
                1.dp,
                if (isBlocked) Danger.copy(alpha = 0.2f) else Color.Transparent,
                RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // App icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            if (app.icon != null) {
                val bitmap = remember(app.packageName) {
                    app.icon.toBitmap(40, 40)
                }
                Image(
                    painter            = BitmapPainter(bitmap.asImageBitmap()),
                    contentDescription = app.appName,
                    modifier           = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)),
                )
            } else {
                Text(
                    text     = app.appName.take(1).uppercase(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = MulishFamily,
                    color    = Primary,
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text       = app.appName,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MulishFamily,
                color      = TextPrimary,
                maxLines   = 1,
            )
            Text(
                text       = app.packageName,
                fontSize   = 10.sp,
                fontWeight = FontWeight.Normal,
                fontFamily = MulishFamily,
                color      = TextMuted,
                maxLines   = 1,
            )
        }

        if (isBlocked) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Danger.copy(alpha = 0.15f))
                    .padding(horizontal = 6.dp, vertical = 3.dp),
            ) {
                Text(
                    text       = "BLOCKED",
                    fontSize   = 9.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = MulishFamily,
                    color      = Danger,
                    letterSpacing = 0.5.sp,
                )
            }
        }

        OntimeSwitch(checked = isBlocked, onChecked = onToggle)
    }
}

// ─── Ontime Switch ────────────────────────────────────────────────────────────
// Custom styled switch matching Ontime's purple brand

@Composable
private fun OntimeSwitch(
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    enabled: Boolean = true,
) {
    Switch(
        checked         = checked,
        onCheckedChange = onChecked,
        enabled         = enabled,
        modifier        = Modifier.scale(0.85f),
        colors          = SwitchDefaults.colors(
            checkedThumbColor        = Color.White,
            checkedTrackColor        = Primary,
            checkedBorderColor       = Primary,
            uncheckedThumbColor      = TextMuted,
            uncheckedTrackColor      = SurfaceHigh,
            uncheckedBorderColor     = Border,
            disabledCheckedTrackColor   = Primary.copy(alpha = 0.4f),
            disabledUncheckedTrackColor = SurfaceHigh.copy(alpha = 0.4f),
        ),
    )
}

// ─── Section Header ───────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text       = title,
            fontSize   = 16.sp,
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
        )
    }
}

// ─── Status Pill ─────────────────────────────────────────────────────────────

@Composable
private fun StatusPill(emoji: String, text: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Success.copy(alpha = 0.1f))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(text = emoji, fontSize = 11.sp)
        Text(
            text       = text,
            fontSize   = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = MulishFamily,
            color      = Success,
        )
    }
}
