package com.tushartamrakar.ontime.focus.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.tushartamrakar.ontime.focus.blocker.FocusWebBlocklist
import com.tushartamrakar.ontime.focus.blocker.WebCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WebBlockerSheet(
    blocklist:  FocusWebBlocklist,
    onDismiss:  () -> Unit,
) {
    val context  = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current

    // Local state — drives immediate UI updates
    var isEnabled       by remember { mutableStateOf(blocklist.isEnabled) }
    var categoryStates  by remember {
        mutableStateOf(WebCategory.values().associateWith { blocklist.isCategoryEnabled(it) })
    }
    var customDomains   by remember { mutableStateOf(blocklist.getCustomDomains().toList().sorted()) }
    var customInput     by remember { mutableStateOf("") }
    var inputError      by remember { mutableStateOf("") }

    val totalBlocked = remember(categoryStates, customDomains, isEnabled) {
        if (!isEnabled) 0
        else {
            val catCount = WebCategory.values()
                .filter { categoryStates[it] == true }
                .sumOf { it.domains.size }
            catCount + customDomains.size
        }
    }

    fun toggleCategory(cat: WebCategory) {
        val newState = !(categoryStates[cat] ?: false)
        blocklist.setCategoryEnabled(cat, newState)
        categoryStates = categoryStates.toMutableMap().apply { put(cat, newState) }
    }

    fun addDomain() {
        val input = customInput.trim().lowercase()
            .removePrefix("www.").removePrefix("https://").removePrefix("http://").trimEnd('/')
        if (input.isBlank()) { inputError = "Enter a website address"; return }
        if (!input.contains('.')) { inputError = "Must be a valid domain (e.g. example.com)"; return }
        blocklist.addCustomDomain(input)
        customDomains = blocklist.getCustomDomains().toList().sorted()
        customInput = ""
        inputError  = ""
        keyboard?.hide()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = Background,
        shape            = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle       = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(40.dp).height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Border),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {

            // ── Header ────────────────────────────────────────────────────────
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "🌐 Focus Web Blocker",
                        fontSize = 18.sp, fontWeight = FontWeight.Black,
                        fontFamily = MulishFamily, color = TextPrimary,
                    )
                    Text(
                        "Block distracting sites during focus sessions",
                        fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        fontFamily = MulishFamily, color = TextMuted,
                    )
                }
                // Master toggle
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isEnabled) Primary.copy(alpha = 0.15f) else SurfaceHigh)
                        .border(1.dp,
                            if (isEnabled) Primary.copy(alpha = 0.3f) else Border,
                            RoundedCornerShape(20.dp))
                        .clickable {
                            isEnabled = !isEnabled
                            blocklist.isEnabled = isEnabled
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Text(
                        if (isEnabled) "ON" else "OFF",
                        fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                        fontFamily = MulishFamily,
                        color = if (isEnabled) Primary else TextMuted,
                    )
                }
            }

            // ── Active summary pill ───────────────────────────────────────────
            if (isEnabled) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Success.copy(alpha = 0.08f))
                        .border(1.dp, Success.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Text(
                        "🛡  $totalBlocked websites blocked during focus sessions",
                        fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        fontFamily = MulishFamily, color = Success,
                    )
                }
            }

            // ── Category section ──────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "CATEGORIES",
                    fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily, color = TextMuted,
                    letterSpacing = 1.2.sp,
                )
                Spacer(Modifier.height(4.dp))
                WebCategory.values().forEach { cat ->
                    val catEnabled = categoryStates[cat] ?: cat.defaultOn
                    CategoryRow(
                        category  = cat,
                        enabled   = catEnabled && isEnabled,
                        toggled   = catEnabled,
                        onToggle  = { if (isEnabled) toggleCategory(cat) },
                    )
                }
            }

            // ── Custom domains section ────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "CUSTOM SITES",
                    fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily, color = TextMuted,
                    letterSpacing = 1.2.sp,
                )

                // Input field
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value         = customInput,
                        onValueChange = { customInput = it; inputError = "" },
                        modifier      = Modifier.weight(1f),
                        placeholder   = {
                            Text("e.g. example.com", fontSize = 13.sp,
                                fontFamily = MulishFamily, color = TextMuted)
                        },
                        singleLine    = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction    = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { addDomain() }),
                        isError       = inputError.isNotBlank(),
                        supportingText = if (inputError.isNotBlank()) {
                            { Text(inputError, fontSize = 11.sp,
                                fontFamily = MulishFamily, color = Danger) }
                        } else null,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor   = Primary,
                            unfocusedBorderColor = Border,
                            focusedTextColor     = TextPrimary,
                            unfocusedTextColor   = TextPrimary,
                            cursorColor          = Primary,
                        ),
                        shape = RoundedCornerShape(12.dp),
                    )
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Primary)
                            .clickable { addDomain() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.Add, null, tint = Color.White,
                            modifier = Modifier.size(20.dp))
                    }
                }

                // Custom domains list
                if (customDomains.isEmpty()) {
                    Text(
                        "No custom sites added yet",
                        fontSize = 12.sp, fontWeight = FontWeight.Medium,
                        fontFamily = MulishFamily, color = TextMuted,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Surface),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        customDomains.forEachIndexed { i, domain ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment     = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(Icons.Filled.Language, null,
                                    tint = Primary.copy(alpha = 0.7f),
                                    modifier = Modifier.size(14.dp))
                                Text(
                                    domain,
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                    fontFamily = MulishFamily, color = TextPrimary,
                                    modifier = Modifier.weight(1f),
                                )
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Danger.copy(alpha = 0.10f))
                                        .clickable {
                                            blocklist.removeCustomDomain(domain)
                                            customDomains = blocklist.getCustomDomains()
                                                .toList().sorted()
                                        },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Filled.Close, null,
                                        tint = Danger, modifier = Modifier.size(12.dp))
                                }
                            }
                            if (i < customDomains.size - 1) {
                                Box(Modifier.fillMaxWidth().height(0.5.dp)
                                    .padding(start = 40.dp).background(Border.copy(alpha = 0.4f)))
                            }
                        }
                    }
                }
            }

            // ── Footer note ───────────────────────────────────────────────────
            Text(
                "Sites are blocked by intercepting DNS queries — no data leaves your device. " +
                "Blocking activates when a focus session starts.",
                fontSize    = 11.sp,
                fontWeight  = FontWeight.Medium,
                fontFamily  = MulishFamily,
                color       = TextMuted.copy(alpha = 0.7f),
                lineHeight  = 16.sp,
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

// ─── Category row ─────────────────────────────────────────────────────────────

@Composable
private fun CategoryRow(
    category: WebCategory,
    enabled:  Boolean,
    toggled:  Boolean,
    onToggle: () -> Unit,
) {
    val bgColor by animateColorAsState(
        targetValue   = if (enabled) Primary.copy(alpha = 0.06f) else Surface,
        animationSpec = tween(200),
        label         = "cat_bg_${category.key}",
    )
    val borderColor by animateColorAsState(
        targetValue   = if (enabled) Primary.copy(alpha = 0.20f) else Border.copy(alpha = 0.5f),
        animationSpec = tween(200),
        label         = "cat_border_${category.key}",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(category.emoji, fontSize = 22.sp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                category.displayName,
                fontSize = 13.sp, fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily,
                color = if (enabled) TextPrimary else TextSecondary,
            )
            Text(
                category.description,
                fontSize = 11.sp, fontWeight = FontWeight.Medium,
                fontFamily = MulishFamily, color = TextMuted,
            )
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (toggled) Primary.copy(alpha = 0.12f) else SurfaceHigh
                    )
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            ) {
                Text(
                    if (toggled) "ON" else "OFF",
                    fontSize = 10.sp, fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color = if (toggled) Primary else TextMuted,
                )
            }
            Text(
                "${category.domains.size} sites",
                fontSize = 9.sp, fontWeight = FontWeight.Medium,
                fontFamily = MulishFamily, color = TextMuted,
            )
        }
    }
}
