package com.tushartamrakar.ontime.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.tushartamrakar.ontime.R

// ─── Mulish Font Family ───────────────────────────────────────────────────────
val MulishFamily = FontFamily(
    Font(R.font.mulish_regular,   FontWeight.Normal),
    Font(R.font.mulish_medium,    FontWeight.Medium),
    Font(R.font.mulish_semibold,  FontWeight.SemiBold),
    Font(R.font.mulish_bold,      FontWeight.Bold),
    Font(R.font.mulish_extrabold, FontWeight.ExtraBold),
    Font(R.font.mulish_black,     FontWeight.Black),
)

// ─── Typography ───────────────────────────────────────────────────────────────
// Colors are intentionally omitted here — Material3 best practice is to let
// colors flow from LocalContentColor (set by the ColorScheme's onBackground /
// onSurface values inside OntimeTheme). This also allows the dynamic
// Light ↔ Dark theme switching to work correctly for every Text composable
// that uses these styles without any extra configuration.
val OntimeTypography = Typography(
    displayLarge = TextStyle(
        fontFamily    = MulishFamily,
        fontWeight    = FontWeight.Black,
        fontSize      = 72.sp,
        lineHeight    = 80.sp,
        letterSpacing = (-3).sp,
    ),
    displayMedium = TextStyle(
        fontFamily    = MulishFamily,
        fontWeight    = FontWeight.Black,
        fontSize      = 48.sp,
        lineHeight    = 56.sp,
        letterSpacing = (-2).sp,
    ),
    displaySmall = TextStyle(
        fontFamily    = MulishFamily,
        fontWeight    = FontWeight.Black,
        fontSize      = 36.sp,
        lineHeight    = 44.sp,
        letterSpacing = (-1).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily    = MulishFamily,
        fontWeight    = FontWeight.ExtraBold,
        fontSize      = 28.sp,
        lineHeight    = 36.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily    = MulishFamily,
        fontWeight    = FontWeight.ExtraBold,
        fontSize      = 22.sp,
        lineHeight    = 30.sp,
        letterSpacing = (-0.5).sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = MulishFamily,
        fontWeight = FontWeight.Bold,
        fontSize   = 18.sp,
        lineHeight = 26.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = MulishFamily,
        fontWeight = FontWeight.Bold,
        fontSize   = 16.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = MulishFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 14.sp,
        lineHeight = 22.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = MulishFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize   = 12.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = MulishFamily,
        fontWeight = FontWeight.Normal,
        fontSize   = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = MulishFamily,
        fontWeight = FontWeight.Normal,
        fontSize   = 14.sp,
        lineHeight = 22.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = MulishFamily,
        fontWeight = FontWeight.Normal,
        fontSize   = 12.sp,
        lineHeight = 18.sp,
    ),
    labelLarge = TextStyle(
        fontFamily    = MulishFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 12.sp,
        lineHeight    = 16.sp,
        letterSpacing = 0.5.sp,
    ),
    labelMedium = TextStyle(
        fontFamily    = MulishFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 10.sp,
        lineHeight    = 14.sp,
        letterSpacing = 0.8.sp,
    ),
    labelSmall = TextStyle(
        fontFamily    = MulishFamily,
        fontWeight    = FontWeight.Bold,
        fontSize      = 9.sp,
        lineHeight    = 12.sp,
        letterSpacing = 1.sp,
    ),
)
