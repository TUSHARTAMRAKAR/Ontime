package com.tushartamrakar.ontime.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// ─── Dark Color Scheme ────────────────────────────────────────────────────────
private val OntimeDarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = TextPrimary,
    primaryContainer = PrimaryDark,
    onPrimaryContainer = TextPrimary,
    secondary = Accent,
    onSecondary = TextPrimary,
    secondaryContainer = AccentDark,
    onSecondaryContainer = TextPrimary,
    tertiary = Info,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = TextSecondary,
    error = Danger,
    onError = TextPrimary,
    outline = Border,
    outlineVariant = BorderLight,
)

// ─── Ontime Theme ─────────────────────────────────────────────────────────────
@Composable
fun OntimeTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = OntimeDarkColorScheme,
        typography = OntimeTypography,
        content = content,
    )
}