package com.tushartamrakar.ontime.core.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Primary Brand (shared between themes) ───────────────────────────────────
val Primary      = Color(0xFF7C3AED)
val PrimaryDark  = Color(0xFF6D28D9)
val PrimaryLight = Color(0xFF8B5CF6)
val PrimaryGlow  = Color(0x407C3AED)

// ─── Accent (shared) ─────────────────────────────────────────────────────────
val Accent     = Color(0xFFEC4899)
val AccentGlow = Color(0x40EC4899)

// ─── Status (shared) ─────────────────────────────────────────────────────────
val Success = Color(0xFF10B981)
val Warning = Color(0xFFF59E0B)
val Danger  = Color(0xFFEF4444)
val Info    = Color(0xFF3B82F6)

// ─── Dark Theme raw values ────────────────────────────────────────────────────
// Used inside non-@Composable contexts: Theme.kt color scheme builders,
// OntimeDarkPalette, and as fallback defaults for the semantic shorthands.
internal val DarkBg        = Color(0xFF0A0A0F)
internal val DarkSrf       = Color(0xFF1A1A2E)
internal val DarkSrfHigh   = Color(0xFF252540)
internal val DarkCard      = Color(0xFF1A1A2E)
internal val DarkTxtPri    = Color(0xFFF8FAFC)
internal val DarkTxtSec    = Color(0xFF94A3B8)
internal val DarkTxtMuted  = Color(0xFF475569)
internal val DarkBorder    = Color(0xFF1E1E3A)
internal val DarkBorderLt  = Color(0xFF2A2A4A)
internal val DarkOverlay   = Color(0x800A0A0F)

// ─── Light Theme "Pearl & Amethyst" raw values ────────────────────────────────
// Warm ivory foundations with amethyst-tinted surfaces and deep navy-violet text.
internal val LightBg        = Color(0xFFF6F3F0)  // Warm ivory-pearl
internal val LightSrf       = Color(0xFFFFFFFF)  // Pure white
internal val LightSrfHigh   = Color(0xFFEDE9F8)  // Soft amethyst tint
internal val LightCard      = Color(0xFFFFFFFF)
internal val LightTxtPri    = Color(0xFF1B1830)  // Deep navy-violet
internal val LightTxtSec    = Color(0xFF4A476B)  // Medium purple-slate
internal val LightTxtMuted  = Color(0xFF9896B4)  // Soft muted purple-gray
internal val LightBorder    = Color(0xFFE4DFEF)  // Delicate lavender
internal val LightBorderLt  = Color(0xFFF1EEF9)  // Barely-there
internal val LightOverlay   = Color(0x80F6F3F0)  // Frosted ivory
