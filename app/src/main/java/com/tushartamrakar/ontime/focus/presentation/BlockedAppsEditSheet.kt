package com.tushartamrakar.ontime.focus.presentation

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tushartamrakar.ontime.core.ui.theme.Border
import com.tushartamrakar.ontime.core.ui.theme.Danger
import com.tushartamrakar.ontime.core.ui.theme.MulishFamily
import com.tushartamrakar.ontime.core.ui.theme.Primary
import com.tushartamrakar.ontime.core.ui.theme.Surface
import com.tushartamrakar.ontime.core.ui.theme.SurfaceHigh
import com.tushartamrakar.ontime.core.ui.theme.Success
import com.tushartamrakar.ontime.core.ui.theme.TextMuted
import com.tushartamrakar.ontime.core.ui.theme.TextPrimary
import com.tushartamrakar.ontime.core.ui.theme.TextSecondary
import com.tushartamrakar.ontime.core.ui.theme.Warning
import com.tushartamrakar.ontime.focus.data.local.BlockedAppEntity

// ─────────────────────────────────────────────────────────────────────────────
//  Block state per app
// ─────────────────────────────────────────────────────────────────────────────

enum class BlockState { ALWAYS, FOCUS_ONLY, ALLOW }

private val BROWSER_PACKAGES = setOf(
    "com.android.chrome",
    "org.mozilla.firefox",
    "com.microsoft.emmx",
    "com.opera.browser",
    "com.brave.browser",
    "com.kiwibrowser.browser",
    "com.sec.android.app.sbrowser",
)

// ─────────────────────────────────────────────────────────────────────────────
//  Sheet
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedAppsEditSheet(
    viewModel: FocusViewModel,
    onDismiss: () -> Unit,
) {
    val installedApps  by viewModel.installedApps.collectAsState()
    val blockedApps    by viewModel.blockedApps.collectAsState()
    val isLoadingApps  by viewModel.isLoadingApps.collectAsState()
    val alwaysOnMode   by viewModel.alwaysOnMode.collectAsState()

    var searchQuery      by remember { mutableStateOf("") }
    var specialExpanded  by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) { viewModel.loadInstalledApps() }

    // ── State helpers ────────────────────────────────────────────────────────
    fun blockStateOf(packageName: String): BlockState {
        val entity = blockedApps.find { it.packageName == packageName && it.isEnabled }
        return when {
            entity == null              -> BlockState.ALLOW
            entity.blockOnlyDuringFocus -> BlockState.FOCUS_ONLY
            else                        -> BlockState.ALWAYS
        }
    }

    fun applyState(packageName: String, appName: String, state: BlockState) {
        when (state) {
            BlockState.ALWAYS ->
                viewModel.addBlockedApp(BlockedAppEntity(packageName, appName, isEnabled = true, blockOnlyDuringFocus = false))
            BlockState.FOCUS_ONLY ->
                viewModel.addBlockedApp(BlockedAppEntity(packageName, appName, isEnabled = true, blockOnlyDuringFocus = true))
            BlockState.ALLOW ->
                viewModel.removeBlockedApp(packageName)
        }
    }

    // ── Derived lists ────────────────────────────────────────────────────────
    val query          = searchQuery.trim().lowercase()
    val browsers       = installedApps.filter { it.packageName in BROWSER_PACKAGES }
    val allApps        = installedApps
        .filter { it.packageName !in BROWSER_PACKAGES }
        .filter { query.isEmpty() || it.appName.lowercase().contains(query) }
        .sortedBy { it.appName }
    val browsersShown  = browsers.filter { query.isEmpty() || it.appName.lowercase().contains(query) }

    // Browser group: most restrictive state among installed browsers
    val browserGroupState: BlockState = when {
        browsers.any { blockStateOf(it.packageName) == BlockState.ALWAYS }     -> BlockState.ALWAYS
        browsers.any { blockStateOf(it.packageName) == BlockState.FOCUS_ONLY } -> BlockState.FOCUS_ONLY
        else                                                                    -> BlockState.ALLOW
    }

    fun applyBrowserGroup(state: BlockState) =
        browsers.forEach { applyState(it.packageName, it.appName, state) }

    val totalBlocked   = blockedApps.count { it.isEnabled }
    val alwaysCount    = blockedApps.count { it.isEnabled && !it.blockOnlyDuringFocus }
    val focusCount     = blockedApps.count { it.isEnabled && it.blockOnlyDuringFocus }

    // ─────────────────────────────────────────────────────────────────────────
    //  Sheet
    // ─────────────────────────────────────────────────────────────────────────

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = Surface,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle       = {},            // replaced by custom header
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .navigationBarsPadding(),
        ) {

            // ── Header ────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text       = "Select Apps to Block",
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = MulishFamily,
                        color      = TextPrimary,
                    )
                    Text(
                        text = when {
                            totalBlocked == 0 -> "No apps blocked yet"
                            alwaysCount > 0 && focusCount > 0 ->
                                "$alwaysCount always · $focusCount focus-only"
                            alwaysCount > 0 -> "$alwaysCount always blocked"
                            else -> "$focusCount focus-only blocked"
                        },
                        fontSize   = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = MulishFamily,
                        color      = if (totalBlocked > 0) Primary else TextMuted,
                    )
                }

                // X close button
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(SurfaceHigh)
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector        = Icons.Filled.Close,
                        contentDescription = "Close",
                        tint               = TextSecondary,
                        modifier           = Modifier.size(18.dp),
                    )
                }
            }

            // ── Search bar ────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceHigh)
                    .border(
                        width = 1.dp,
                        color = if (searchQuery.isNotEmpty()) Primary.copy(alpha = 0.5f) else Border,
                        shape = RoundedCornerShape(14.dp),
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector        = Icons.Filled.Search,
                    contentDescription = null,
                    tint               = TextMuted,
                    modifier           = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text       = "Search apps…",
                            fontFamily = MulishFamily,
                            fontSize   = 14.sp,
                            color      = TextMuted,
                        )
                    }
                    BasicTextField(
                        value         = searchQuery,
                        onValueChange = { searchQuery = it },
                        singleLine    = true,
                        textStyle     = TextStyle(
                            fontFamily = MulishFamily,
                            fontSize   = 14.sp,
                            color      = TextPrimary,
                        ),
                    )
                }
                if (searchQuery.isNotEmpty()) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector        = Icons.Filled.Close,
                        contentDescription = "Clear",
                        tint               = TextMuted,
                        modifier           = Modifier
                            .size(16.dp)
                            .clickable { searchQuery = "" },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Content ───────────────────────────────────────────────────
            if (isLoadingApps && installedApps.isEmpty()) {
                Box(
                    modifier         = Modifier.fillMaxWidth().weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator(
                            color       = Primary,
                            strokeWidth = 2.dp,
                            modifier    = Modifier.size(32.dp),
                        )
                        Text(
                            "Loading apps…",
                            fontFamily = MulishFamily,
                            fontSize   = 13.sp,
                            color      = TextMuted,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier        = Modifier.weight(1f),
                    contentPadding  = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                ) {

                    // ── Special Apps (collapsible) ────────────────────────
                    if (query.isEmpty() || browsersShown.isNotEmpty()) {
                        item {
                            CollapsibleSectionHeader(
                                title    = "Special Apps",
                                expanded = specialExpanded,
                                onToggle = { specialExpanded = !specialExpanded },
                            )
                        }
                        if (specialExpanded && browsers.isNotEmpty()) {
                            item {
                                BrowserGroupCard(
                                    browserPackages = browsers.map { it.packageName },
                                    browserCount    = browsers.size,
                                    groupState      = browserGroupState,
                                    onStateChange   = { applyBrowserGroup(it) },
                                )
                                Spacer(Modifier.height(4.dp))
                            }
                        }
                    }

                    // ── All Apps ──────────────────────────────────────────
                    if (allApps.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(4.dp))
                            AllAppsSectionHeader(
                                count        = allApps.size,
                                blockedCount = allApps.count {
                                    blockStateOf(it.packageName) != BlockState.ALLOW
                                },
                            )
                        }
                        items(allApps, key = { it.packageName }) { app ->
                            AppRow(
                                packageName   = app.packageName,
                                appName       = app.appName,
                                blockState    = blockStateOf(app.packageName),
                                onStateChange = { applyState(app.packageName, app.appName, it) },
                            )
                        }
                    }

                    // ── Empty state ───────────────────────────────────────
                    if (browsers.isEmpty() && allApps.isEmpty()) {
                        item {
                            Box(
                                modifier         = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 60.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text("🔍", fontSize = 36.sp)
                                    Text(
                                        text       = if (searchQuery.isBlank())
                                            "No apps found"
                                        else
                                            "No results for \"$searchQuery\"",
                                        fontSize   = 14.sp,
                                        color      = TextMuted,
                                        fontFamily = MulishFamily,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Collapsible section header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CollapsibleSectionHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 0f else -90f,
        label       = "chevron",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            imageVector        = Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint               = TextSecondary,
            modifier           = Modifier
                .size(18.dp)
                .rotate(chevronRotation),
        )
        Text(
            text          = title.uppercase(),
            fontSize      = 11.sp,
            fontWeight    = FontWeight.ExtraBold,
            fontFamily    = MulishFamily,
            color         = TextSecondary,
            letterSpacing = 1.sp,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Browser group card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BrowserGroupCard(
    browserPackages: List<String>,
    browserCount: Int,
    groupState: BlockState,
    onStateChange: (BlockState) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceHigh)
            .border(1.dp, Border, RoundedCornerShape(16.dp)),
    ) {
        // Top row: globe icon + label + browser icon cluster
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Primary.copy(alpha = 0.12f))
                    .border(1.dp, Primary.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text("🌐", fontSize = 22.sp)
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text       = "Browser Apps",
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MulishFamily,
                    color      = TextPrimary,
                )
                Text(
                    text       = "$browserCount browser${if (browserCount > 1) "s" else ""} installed",
                    fontSize   = 11.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = MulishFamily,
                    color      = TextMuted,
                )
            }

            // Overlapping browser icon cluster (max 3 + overflow badge)
            Row(horizontalArrangement = Arrangement.spacedBy((-7).dp)) {
                browserPackages.take(3).forEach { pkg ->
                    AppIcon(
                        packageName = pkg,
                        modifier    = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, Surface, CircleShape),
                    )
                }
                if (browserCount > 3) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Border)
                            .border(1.5.dp, Surface, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text       = "+${browserCount - 3}",
                            fontSize   = 7.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = MulishFamily,
                            color      = TextMuted,
                        )
                    }
                }
            }
        }

        // Thin divider
        Box(Modifier.fillMaxWidth().height(1.dp).background(Border.copy(alpha = 0.5f)))

        // Radio options
        BrowserRadioOption(
            label        = "Block completely",
            subtitle     = "Blocked always, even outside focus sessions",
            isSelected   = groupState == BlockState.ALWAYS,
            dotColor     = Danger,
            onClick      = { onStateChange(BlockState.ALWAYS) },
        )
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(0.5.dp).background(Border.copy(alpha = 0.4f)))
        BrowserRadioOption(
            label        = "Focus only",
            subtitle     = "Blocked only during active focus sessions",
            isSelected   = groupState == BlockState.FOCUS_ONLY,
            dotColor     = Warning,
            onClick      = { onStateChange(BlockState.FOCUS_ONLY) },
        )
        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(0.5.dp).background(Border.copy(alpha = 0.4f)))
        BrowserRadioOption(
            label        = "Allow completely",
            subtitle     = "Not blocked at any time",
            isSelected   = groupState == BlockState.ALLOW,
            dotColor     = Success,
            onClick      = { onStateChange(BlockState.ALLOW) },
        )
    }
}

@Composable
private fun BrowserRadioOption(
    label: String,
    subtitle: String,
    isSelected: Boolean,
    dotColor: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Radio circle
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape)
                .background(if (isSelected) dotColor.copy(alpha = 0.15f) else Color.Transparent)
                .border(2.dp, if (isSelected) dotColor else Border, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor),
                )
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text       = label,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MulishFamily,
                color      = if (isSelected) TextPrimary else TextSecondary,
            )
            Text(
                text       = subtitle,
                fontSize   = 10.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = MulishFamily,
                color      = TextMuted,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  All Apps section header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AllAppsSectionHeader(count: Int, blockedCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text          = "ALL APPS",
                fontSize      = 11.sp,
                fontWeight    = FontWeight.ExtraBold,
                fontFamily    = MulishFamily,
                color         = TextSecondary,
                letterSpacing = 1.sp,
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceHigh)
                    .padding(horizontal = 7.dp, vertical = 2.dp),
            ) {
                Text(
                    text       = "$count",
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color      = TextMuted,
                )
            }
        }
        if (blockedCount > 0) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Primary.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    text       = "$blockedCount blocked",
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color      = Primary,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  App row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AppRow(
    packageName:   String,
    appName:       String,
    blockState:    BlockState,
    onStateChange: (BlockState) -> Unit,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(
            packageName = packageName,
            modifier    = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(11.dp)),
        )

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text       = appName,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MulishFamily,
                color      = TextPrimary,
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            if (blockState != BlockState.ALLOW) {
                Text(
                    text       = when (blockState) {
                        BlockState.ALWAYS     -> "Blocked always — no session needed"
                        BlockState.FOCUS_ONLY -> "Blocked during focus sessions only"
                        else                  -> ""
                    },
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = MulishFamily,
                    color      = when (blockState) {
                        BlockState.ALWAYS     -> Danger
                        BlockState.FOCUS_ONLY -> Warning
                        else                  -> TextMuted
                    },
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        // Cycle-on-tap pill: ALLOW → FOCUS_ONLY → ALWAYS → ALLOW
        BlockStatePill(
            state         = blockState,
            onStateChange = onStateChange,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Block state pill (tap to cycle)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BlockStatePill(
    state: BlockState,
    onStateChange: (BlockState) -> Unit,
) {
    val nextState = when (state) {
        BlockState.ALLOW      -> BlockState.FOCUS_ONLY
        BlockState.FOCUS_ONLY -> BlockState.ALWAYS
        BlockState.ALWAYS     -> BlockState.ALLOW
    }
    val (label, bgColor, textColor, borderColor) = when (state) {
        BlockState.ALLOW      -> Quad("✓ Allow",        SurfaceHigh,               TextMuted, Border)
        BlockState.FOCUS_ONLY -> Quad("⚡ Focus Only",   Warning.copy(alpha = 0.12f), Warning,   Warning.copy(alpha = 0.35f))
        BlockState.ALWAYS     -> Quad("🔴 Always Block", Danger.copy(alpha = 0.12f),  Danger,    Danger.copy(alpha = 0.35f))
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(9.dp))
            .clickable { onStateChange(nextState) }
            .padding(horizontal = 13.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text       = label,
            fontSize   = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = MulishFamily,
            color      = textColor,
        )
    }
}

// tiny helper to destructure 4 values cleanly
private data class Quad<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
private operator fun <A, B, C, D> Quad<A, B, C, D>.component1() = a
private operator fun <A, B, C, D> Quad<A, B, C, D>.component2() = b
private operator fun <A, B, C, D> Quad<A, B, C, D>.component3() = c
private operator fun <A, B, C, D> Quad<A, B, C, D>.component4() = d

// ─────────────────────────────────────────────────────────────────────────────
//  App icon loader
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AppIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap  by produceState<ImageBitmap?>(initialValue = null, packageName) {
        value = try {
            val drawable = context.packageManager.getApplicationIcon(packageName)
            val bmp      = Bitmap.createBitmap(72, 72, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, 72, 72)
            drawable.draw(Canvas(bmp))
            bmp.asImageBitmap()
        } catch (_: PackageManager.NameNotFoundException) { null }
    }

    if (bitmap != null) {
        Image(
            bitmap             = bitmap!!,
            contentDescription = packageName.substringAfterLast('.'),
            modifier           = modifier,
        )
    } else {
        Box(
            modifier         = modifier.background(SurfaceHigh),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text       = packageName.substringAfterLast('.').take(1).uppercase(),
                fontSize   = 16.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MulishFamily,
                color      = TextMuted,
            )
        }
    }
}

// ─── Always-On Mode Card ──────────────────────────────────────────────────────

@Composable
private fun AlwaysOnModeCard(
    isOn:      Boolean,
    activeNow: Boolean,
    onToggle:  () -> Unit,
    modifier:  Modifier = Modifier,
) {
    val bgColor     = if (isOn) com.tushartamrakar.ontime.core.ui.theme.Primary.copy(alpha = 0.07f)
                      else com.tushartamrakar.ontime.core.ui.theme.SurfaceHigh
    val borderColor = if (isOn) com.tushartamrakar.ontime.core.ui.theme.Primary.copy(alpha = 0.25f)
                      else com.tushartamrakar.ontime.core.ui.theme.Border

    androidx.compose.foundation.layout.Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, borderColor, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
            .clickable { onToggle() }
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(11.dp))
                    .background(
                        if (isOn) com.tushartamrakar.ontime.core.ui.theme.Primary.copy(alpha = 0.15f)
                        else com.tushartamrakar.ontime.core.ui.theme.Surface
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(if (isOn) "🔒" else "🔓", fontSize = 18.sp)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    "Always-On Blocker",
                    fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color = if (isOn) com.tushartamrakar.ontime.core.ui.theme.TextPrimary
                            else TextMuted,
                )
                Text(
                    if (isOn) "Blocking right now — no session needed"
                    else "Apps only blocked during focus sessions",
                    fontSize = 11.sp, fontWeight = FontWeight.Medium,
                    fontFamily = MulishFamily,
                    color = if (isOn) com.tushartamrakar.ontime.core.ui.theme.Primary.copy(alpha = 0.75f)
                            else TextMuted,
                )
            }
            // Toggle pill
            Box(
                modifier = Modifier
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                    .background(
                        if (isOn) com.tushartamrakar.ontime.core.ui.theme.Primary.copy(alpha = 0.15f)
                        else com.tushartamrakar.ontime.core.ui.theme.Surface
                    )
                    .border(1.dp,
                        if (isOn) com.tushartamrakar.ontime.core.ui.theme.Primary.copy(alpha = 0.30f)
                        else Border,
                        androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    if (isOn) "ON" else "OFF",
                    fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color = if (isOn) com.tushartamrakar.ontime.core.ui.theme.Primary else TextMuted,
                )
            }
        }

        // Explanation row
        if (!isOn) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .background(com.tushartamrakar.ontime.core.ui.theme.Surface)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Top,
            ) {
                Text("💡", fontSize = 11.sp)
                Text(
                    "Turn this ON to block \"Focus Only\" apps permanently — no need to start a timer. Great for permanently blocking social media.",
                    fontSize = 10.sp, fontWeight = FontWeight.Medium,
                    fontFamily = MulishFamily, color = TextSecondary,
                    lineHeight = 15.sp,
                )
            }
        }
    }
}
