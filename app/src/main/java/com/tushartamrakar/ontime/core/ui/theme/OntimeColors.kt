package com.tushartamrakar.ontime.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

// ─── Palette data class ───────────────────────────────────────────────────────

data class OntimeColorPalette(
    val background: Color,
    val surface: Color,
    val surfaceHigh: Color,
    val cardBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val border: Color,
    val borderLight: Color,
    val overlay: Color,
    val isDark: Boolean,
)

// ─── Palette instances ────────────────────────────────────────────────────────

internal val OntimeDarkPalette = OntimeColorPalette(
    background     = DarkBg,
    surface        = DarkSrf,
    surfaceHigh    = DarkSrfHigh,
    cardBackground = DarkCard,
    textPrimary    = DarkTxtPri,
    textSecondary  = DarkTxtSec,
    textMuted      = DarkTxtMuted,
    border         = DarkBorder,
    borderLight    = DarkBorderLt,
    overlay        = DarkOverlay,
    isDark         = true,
)

internal val OntimeLightPalette = OntimeColorPalette(
    background     = LightBg,
    surface        = LightSrf,
    surfaceHigh    = LightSrfHigh,
    cardBackground = LightCard,
    textPrimary    = LightTxtPri,
    textSecondary  = LightTxtSec,
    textMuted      = LightTxtMuted,
    border         = LightBorder,
    borderLight    = LightBorderLt,
    overlay        = LightOverlay,
    isDark         = false,
)

// ─── CompositionLocal ─────────────────────────────────────────────────────────

val LocalOntimeColors = compositionLocalOf { OntimeDarkPalette }

// ─── Composable semantic shorthands ──────────────────────────────────────────
// These are @Composable property getters — valid in any @Composable function,
// including Modifier.background(Background), Text(color = TextPrimary), etc.
//
// WHY this works app-wide with zero per-screen changes:
//   Every screen already imports `Background`, `Surface`, `TextPrimary` etc.
//   from `com.tushartamrakar.ontime.core.ui.theme`. These definitions now live
//   in OntimeColors.kt (same package), so the import resolves here automatically.
//   Since ALL usages are inside @Composable functions, the @Composable getter
//   requirement is always satisfied.
//
// KNOWN EXCEPTION — non-composable lambdas (Canvas / DrawScope):
//   Canvas { drawRect(color = Surface) } will fail because DrawScope is not
//   composable. Fix pattern: read the color BEFORE the Canvas block:
//     val surfaceColor = Surface   ← composable context, works ✅
//     Canvas { drawRect(surfaceColor) }  ← DrawScope, captured val ✅
//   Files that need this fix: CircularTimerComposable.kt, FocusStatsScreen.kt,
//   TasksScreen.kt — please share those to complete full app-wide theming.

val Background: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalOntimeColors.current.background

val Surface: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalOntimeColors.current.surface

val SurfaceHigh: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalOntimeColors.current.surfaceHigh

val CardBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalOntimeColors.current.cardBackground

val TextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalOntimeColors.current.textPrimary

val TextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalOntimeColors.current.textSecondary

val TextMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalOntimeColors.current.textMuted

val Border: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalOntimeColors.current.border

val BorderLight: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalOntimeColors.current.borderLight

val Overlay: Color
    @Composable
    @ReadOnlyComposable
    get() = LocalOntimeColors.current.overlay
