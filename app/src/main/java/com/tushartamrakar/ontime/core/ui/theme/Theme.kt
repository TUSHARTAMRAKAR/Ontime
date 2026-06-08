package com.tushartamrakar.ontime.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

// ─── Theme mode ───────────────────────────────────────────────────────────────

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
}

// ─── Dark color scheme (uses raw DarkXxx vals — non-composable context) ───────

private val OntimeDarkColorScheme = darkColorScheme(
    primary              = Primary,
    onPrimary            = DarkTxtPri,
    primaryContainer     = PrimaryDark,
    onPrimaryContainer   = DarkTxtPri,
    secondary            = Accent,
    onSecondary          = DarkTxtPri,
    secondaryContainer   = Color(0xFF3D1A6E),
    onSecondaryContainer = DarkTxtPri,
    tertiary             = Success,
    onTertiary           = DarkTxtPri,
    background           = DarkBg,
    onBackground         = DarkTxtPri,
    surface              = DarkSrf,
    onSurface            = DarkTxtPri,
    surfaceVariant       = DarkSrfHigh,
    onSurfaceVariant     = DarkTxtSec,
    error                = Danger,
    onError              = DarkTxtPri,
    errorContainer       = Color(0xFF4A1A1A),
    onErrorContainer     = Danger,
    outline              = DarkBorder,
    outlineVariant       = DarkBorderLt,
    scrim                = DarkOverlay,
)

// ─── Light color scheme "Pearl & Amethyst" (uses raw LightXxx vals) ──────────

private val OntimeLightColorScheme = lightColorScheme(
    primary              = Primary,
    onPrimary            = Color(0xFFFFFFFF),
    primaryContainer     = Color(0xFFEDE8FF),
    onPrimaryContainer   = Color(0xFF21005D),
    secondary            = Accent,
    onSecondary          = Color(0xFFFFFFFF),
    secondaryContainer   = Color(0xFFFFD8EC),
    onSecondaryContainer = Color(0xFF3E001E),
    tertiary             = Success,
    onTertiary           = Color(0xFFFFFFFF),
    tertiaryContainer    = Color(0xFFD5F5E9),
    onTertiaryContainer  = Color(0xFF00391F),
    background           = LightBg,
    onBackground         = LightTxtPri,
    surface              = LightSrf,
    onSurface            = LightTxtPri,
    surfaceVariant       = LightSrfHigh,
    onSurfaceVariant     = LightTxtSec,
    error                = Danger,
    onError              = Color(0xFFFFFFFF),
    errorContainer       = Color(0xFFFFDAD6),
    onErrorContainer     = Color(0xFF410002),
    outline              = LightBorder,
    outlineVariant       = LightBorderLt,
    scrim                = LightOverlay,
)

// ─── OntimeTheme ─────────────────────────────────────────────────────────────

@Composable
fun OntimeTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.DARK   -> true
        ThemeMode.LIGHT  -> false
        ThemeMode.SYSTEM -> isSystemDark
    }

    val palette     = if (isDark) OntimeDarkPalette     else OntimeLightPalette
    val colorScheme = if (isDark) OntimeDarkColorScheme else OntimeLightColorScheme

    CompositionLocalProvider(LocalOntimeColors provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = OntimeTypography,
            content     = content,
        )
    }
}
