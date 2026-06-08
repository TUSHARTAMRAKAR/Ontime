package com.tushartamrakar.ontime.settings.presentation

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.tushartamrakar.ontime.auth.presentation.AuthViewModel
import com.tushartamrakar.ontime.core.security.AppLockManager
import com.tushartamrakar.ontime.core.security.PinSetupSheet
import com.tushartamrakar.ontime.core.ui.theme.Danger
import com.tushartamrakar.ontime.core.ui.theme.LocalOntimeColors
import com.tushartamrakar.ontime.core.ui.theme.MulishFamily
import com.tushartamrakar.ontime.core.ui.theme.Primary
import com.tushartamrakar.ontime.core.ui.theme.Success
import com.tushartamrakar.ontime.core.ui.theme.ThemeMode
import com.tushartamrakar.ontime.core.ui.theme.Warning
import com.tushartamrakar.ontime.navigation.Screen

// ─── App metadata ─────────────────────────────────────────────────────────────
private const val APP_VERSION      = "0.1.0"
private const val BUILD_TYPE       = "Public Beta"
private const val DEVELOPER_NAME   = "Tushar Tamrakar"
private const val GITHUB_URL       = "https://github.com/TUSHARTAMRAKAR"
private const val FEEDBACK_EMAIL   = "tushartamrakar2003@gmail.com"
private const val PLAY_STORE_URL   = "https://play.google.com/store/apps/details?id=com.tushartamrakar.ontime"
private const val GITHUB_REPO_URL  = "https://github.com/TUSHARTAMRAKAR/Ontime"

// ─── Data classes ─────────────────────────────────────────────────────────────
private data class FaqItem(val question: String, val answer: String)
private data class OssLibrary(val name: String, val license: String, val url: String)

// ─── FAQ content ──────────────────────────────────────────────────────────────
private val faqItems = listOf(
    FaqItem("How do I create an alarm?",
        "Tap the ＋ button on the Alarms tab. Set your time, label, repeat days, and explore advanced options like Gentle Wake, Text-to-Speech Time Announcement, and Weather Reminder."),
    FaqItem("How does cloud sync work?",
        "Your alarms automatically back up to your Google account via Firebase whenever you create or change them. On a new device, sign in and tap 'Cloud Backup & Restore' in Settings to pull everything back."),
    FaqItem("How does the Focus timer work?",
        "Choose a technique — Pomodoro (timed sessions), Stopwatch (free-form), or Custom intervals — then press Start Focus Now. The timer keeps running in the foreground even with the screen off."),
    FaqItem("How does the app blocker work?",
        "Enable it in Focus Settings › App Blocking. The blocker uses Android Accessibility Service to detect when a blocked app comes to the foreground during a focus session and instantly shows an overlay."),
    FaqItem("What is Gentle Wake?",
        "Gentle Wake gradually ramps your alarm volume from near-silence to full over a set duration (up to 5 minutes), so you wake up calmly instead of being jolted awake. Enable it inside any alarm's advanced settings."),
    FaqItem("Does Ontime work without internet?",
        "Yes — all core features (alarms, focus timer, calendar, period tracker) work completely offline. Cloud sync and Weather Reminders require a connection."),
    FaqItem("What permissions does Ontime need?",
        "Core: Exact Alarm, Notifications, Battery Optimization exclusion. Optional: Location (Weather Reminders), Accessibility Service (App Blocker), Camera (QR scanner), Contacts (Calendar invites), SMS (Calendar SMS invites)."),
    FaqItem("Is my data private?",
        "Absolutely. Your data lives on your device. Alarms are optionally synced to your own Google account. Period tracking and health data are never uploaded anywhere. Read our full Privacy Policy for details."),
    FaqItem("How do I track my menstrual cycle?",
        "Go to Settings › Features › Period Tracker. Log your cycle start date, symptoms, and mood. The tracker predicts future cycles and can send you smart reminders."),
    FaqItem("How do I report a bug or suggest a feature?",
        "Go to Settings › Help & Support › Send Feedback. It opens your email app with a pre-filled template — just describe what happened and hit send. We read every message."),
)

// ─── Open-source libraries ────────────────────────────────────────────────────
private val ossLibraries = listOf(
    OssLibrary("Kotlin",                  "Apache 2.0", "https://kotlinlang.org"),
    OssLibrary("Jetpack Compose",         "Apache 2.0", "https://developer.android.com/jetpack/compose"),
    OssLibrary("Material Design 3",       "Apache 2.0", "https://m3.material.io"),
    OssLibrary("Hilt (Dagger)",           "Apache 2.0", "https://dagger.dev/hilt"),
    OssLibrary("Room",                    "Apache 2.0", "https://developer.android.com/training/data-storage/room"),
    OssLibrary("Navigation Compose",      "Apache 2.0", "https://developer.android.com/guide/navigation"),
    OssLibrary("Firebase Authentication", "Apache 2.0", "https://firebase.google.com/products/auth"),
    OssLibrary("Firebase Firestore",      "Apache 2.0", "https://firebase.google.com/products/firestore"),
    OssLibrary("Kotlin Coroutines",       "Apache 2.0", "https://github.com/Kotlin/kotlinx.coroutines"),
    OssLibrary("Kotlin Flow",             "Apache 2.0", "https://kotlinlang.org/docs/flow.html"),
    OssLibrary("Accompanist",             "Apache 2.0", "https://github.com/google/accompanist"),
    OssLibrary("Coil",                    "Apache 2.0", "https://github.com/coil-kt/coil"),
    OssLibrary("OkHttp",                  "Apache 2.0", "https://square.github.io/okhttp"),
    OssLibrary("Gson",                    "Apache 2.0", "https://github.com/google/gson"),
)

// ─── Legal content ────────────────────────────────────────────────────────────
private val termsContent = """
TERMS OF SERVICE
Effective: June 2026 · Version 1.0

━━━━━━━━━━━━━━━━━━━━━━

1. ACCEPTANCE OF TERMS

By downloading or using Ontime, you agree to these Terms. If you disagree, please uninstall the App.

2. ELIGIBILITY

You must be at least 13 years old to use Ontime. By using it, you confirm you meet this requirement.

3. YOUR ACCOUNT

You are responsible for keeping your account credentials secure. Notify us immediately of any unauthorised access. We may suspend accounts that violate these Terms.

4. ACCEPTABLE USE

You agree not to:
• Use Ontime for any unlawful purpose
• Reverse-engineer or decompile the App
• Attempt to disrupt or damage the App or its servers
• Impersonate another person or entity

5. APP FEATURES & LIMITATIONS

Ontime is currently in Public Beta. Features may change, and occasional bugs may occur. We strive to provide a reliable, high-quality experience but cannot guarantee uninterrupted service.

6. INTELLECTUAL PROPERTY

All content, features, design, and code of Ontime are owned by Tushar Tamrakar and are protected by applicable intellectual property laws. You may not copy, modify, or distribute any part of the App without explicit written permission.

7. DISCLAIMER OF WARRANTIES

The App is provided "as is" without warranties of any kind — express or implied — including fitness for a particular purpose, merchantability, or non-infringement.

8. LIMITATION OF LIABILITY

To the maximum extent permitted by applicable law, Tushar Tamrakar shall not be liable for any indirect, incidental, special, or consequential damages arising from your use of, or inability to use, the App.

9. THIRD-PARTY SERVICES

Ontime integrates third-party services (Firebase, Google, OpenWeatherMap). Their own terms and privacy policies apply to their services.

10. CHANGES TO TERMS

We may revise these Terms periodically. Continued use of the App after changes are posted constitutes your acceptance. We will notify users of significant changes through the App.

11. GOVERNING LAW

These Terms are governed by the laws of India.

12. CONTACT

Questions about these Terms?
✉  $FEEDBACK_EMAIL
🌐  $GITHUB_URL
""".trimIndent()

private val privacyContent = """
PRIVACY POLICY
Effective: June 2026 · Version 1.0

━━━━━━━━━━━━━━━━━━━━━━

$DEVELOPER_NAME ("we," "our," or "us") built Ontime as a free, ad-free productivity application. This policy explains what data we collect, how we use it, and the choices you have.

1. INFORMATION WE COLLECT

Account Information
When you sign in with Google, we receive your name, email address, and profile photo from Google's authentication service.

App Data
• Alarm schedules and settings
• Focus session history and statistics
• Calendar events (stored locally on your device only)
• Period tracking data (stored locally ONLY — never uploaded)
• App preferences and theme settings

Device Information
We collect device model and Android version only if you contact us for support.

2. HOW WE USE YOUR INFORMATION

• To provide and personalise App features
• To sync your alarms across your devices via Firebase
• To send local notifications for alarms and reminders
• To improve App stability and performance

We never use your data for advertising.

3. INFORMATION SHARING

We do not sell, trade, rent, or transfer your personal data to third parties. Period.

We use the following services:
• Google Firebase — Authentication and alarm sync under your own Google account
• Google Calendar API — Read-only access to display your calendar events locally
• OpenWeatherMap — For the Weather Reminder alarm feature (only if enabled)

4. DATA STORAGE & SECURITY

• All core data is stored locally on your device
• Alarm data is optionally synced to Firebase Firestore under your personal Google account
• Period tracking and health-related data are NEVER uploaded to any server
• We use industry-standard encryption and Firebase security rules

5. YOUR CHOICES & RIGHTS

• Delete your account and all cloud data: Settings › Account › Delete Account
• Disable cloud sync at any time from Settings › Data
• Revoke Google sign-in: Manage permissions in your Google Account settings
• Request data export or deletion: Email us at $FEEDBACK_EMAIL

6. CHILDREN'S PRIVACY

Ontime is not directed at children under 13. We do not knowingly collect data from children under 13. If you believe a child has provided us personal information, please contact us immediately.

7. THIRD-PARTY SERVICES & LINKS

Our App may link to third-party services. We are not responsible for the privacy practices of those services. We encourage you to read their privacy policies.

8. CHANGES TO THIS POLICY

We will notify you of material changes through an in-app notice before they take effect. Your continued use of the App after changes constitutes acceptance.

9. CONTACT US

Privacy questions or requests:
✉  $FEEDBACK_EMAIL
🌐  $GITHUB_URL
""".trimIndent()

// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AppSettingsScreen(
    navController: NavController,
    bottomPadding: Dp = 80.dp,
    authViewModel:  AuthViewModel  = hiltViewModel(),
    alarmViewModel: com.tushartamrakar.ontime.alarm.presentation.AlarmViewModel = hiltViewModel(),
    themeViewModel: ThemeViewModel = hiltViewModel(),
) {
    val context      = LocalContext.current
    val colors       = LocalOntimeColors.current
    val currentUser  by authViewModel.currentUser.collectAsState()
    val currentTheme by themeViewModel.themeMode.collectAsState()

    // ── Dialog / sheet flags ──────────────────────────────────────────────────
    var showSignOut      by remember { mutableStateOf(false) }
    var showDeleteAcct   by remember { mutableStateOf(false) }
    var showFaq          by remember { mutableStateOf(false) }
    var showAboutDev     by remember { mutableStateOf(false) }
    var showLibraries    by remember { mutableStateOf(false) }
    var showTerms        by remember { mutableStateOf(false) }
    var showPrivacy      by remember { mutableStateOf(false) }

    // ── Cloud sync state ──────────────────────────────────────────────────────
    val isRestoring   by alarmViewModel.isRestoring.collectAsState()
    val restoredCount by alarmViewModel.restoredCount.collectAsState()
    var showRestoreResult by remember { mutableStateOf(false) }
    androidx.compose.runtime.LaunchedEffect(restoredCount) {
        if (restoredCount >= 0) { showRestoreResult = true; kotlinx.coroutines.delay(3000); showRestoreResult = false }
    }

    // ── App Lock state ────────────────────────────────────────────────────────
    val appLockManager = remember { AppLockManager(context) }
    var appLockEnabled by remember { mutableStateOf(appLockManager.isEnabled && appLockManager.hasPinSet) }
    var showPinSetup   by remember { mutableStateOf(false) }
    var showPinVerify  by remember { mutableStateOf(false) }

    // User info
    val displayName = currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Ontime User"
    val email       = currentUser?.email ?: ""
    val initials    = displayName.split(" ").take(2).mapNotNull { it.firstOrNull()?.uppercaseChar() }.joinToString("").ifBlank { "O" }

    LazyColumn(
        modifier       = Modifier.fillMaxSize().background(colors.background).statusBarsPadding().navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = bottomPadding + 16.dp),
    ) {

        // ── Page title ────────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Settings", fontSize = 26.sp, fontWeight = FontWeight.Black, fontFamily = MulishFamily, color = colors.textPrimary)
                    Text("$BUILD_TYPE v$APP_VERSION", fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = MulishFamily, color = colors.textMuted)
                }
                // BETA badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Warning.copy(alpha = 0.13f))
                        .border(1.dp, Warning.copy(alpha = 0.30f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text("BETA", fontSize = 9.sp, fontWeight = FontWeight.Black, fontFamily = MulishFamily, color = Warning, letterSpacing = 1.2.sp)
                }
            }
        }

        // ── User card ─────────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(20.dp)).background(colors.surface)
                    .border(1.dp, Primary.copy(alpha = 0.18f), RoundedCornerShape(20.dp)).padding(16.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Box(
                    modifier = Modifier.size(54.dp).clip(CircleShape)
                        .background(Primary.copy(alpha = 0.12f))
                        .border(2.dp, Primary.copy(alpha = 0.28f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { Text(initials, fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = MulishFamily, color = Primary) }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(displayName, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, fontFamily = MulishFamily, color = colors.textPrimary)
                    if (email.isNotBlank()) Text(email, fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = MulishFamily, color = colors.textMuted)
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(6.dp))
                            .background(Warning.copy(alpha = 0.10f))
                            .border(1.dp, Warning.copy(alpha = 0.22f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    ) { Text("FREE PLAN", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, fontFamily = MulishFamily, color = Warning, letterSpacing = 0.8.sp) }
                }
            }
            Spacer(Modifier.height(24.dp))
        }

        // ── Features ──────────────────────────────────────────────────────────
        item { SettingsSectionLabel("Features") }
        item {
            SettingsGroup {
                SettingsNavRow(Icons.Filled.Timer, Primary, "Focus Settings", "Technique, DND, app blocking & protection") { navController.navigate(Screen.FocusSettings.route) }
                SettingsDivider()
                SettingsNavRow(Icons.Filled.Favorite, Color(0xFFEC4899), "Period Tracker", "Cycle tracking, symptoms & reminders") { navController.navigate(Screen.PeriodTracker.route) }
                SettingsDivider()
                SettingsNavRow(Icons.Filled.CalendarMonth, Color(0xFF06B6D4), "Calendar Sync", "Sync with your device calendar") { navController.navigate(Screen.CalendarSync.route) }
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── Notifications ─────────────────────────────────────────────────────
        item { SettingsSectionLabel("Notifications") }
        item {
            SettingsGroup {
                SettingsNavRow(Icons.Filled.Notifications, Warning, "Notification Preferences", "Manage all Ontime notifications") {
                    context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply { putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName) })
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── Appearance ────────────────────────────────────────────────────────
        item { SettingsSectionLabel("Appearance") }
        item {
            ThemePickerCard(currentMode = currentTheme, onSelect = { themeViewModel.setThemeMode(it) })
            Spacer(Modifier.height(20.dp))
        }

        // ── App Lock ──────────────────────────────────────────────────────────
        item { SettingsSectionLabel("App Lock") }
        item {
            AppLockCard(
                isEnabled    = appLockEnabled, hasPinSet = appLockManager.hasPinSet,
                canBiometric = appLockManager.canUseBiometrics(),
                onToggle     = { if (it) showPinSetup = true else showPinVerify = true },
                onChangePin  = { showPinSetup = true },
                modifier     = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(20.dp))
        }

        // ── Help & Support ────────────────────────────────────────────────────
        item { SettingsSectionLabel("Help & Support") }
        item {
            SettingsGroup {
                SettingsNavRow(Icons.Filled.Help, Color(0xFF06B6D4), "FAQs", "Frequently asked questions") { showFaq = true }
                SettingsDivider()
                SettingsNavRow(Icons.Filled.Feedback, Color(0xFF8B5CF6), "Send Feedback", "Report bugs or suggest features") {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(FEEDBACK_EMAIL))
                        putExtra(Intent.EXTRA_SUBJECT, "Ontime Feedback — $BUILD_TYPE v$APP_VERSION")
                        putExtra(Intent.EXTRA_TEXT,
                            "Hi $DEVELOPER_NAME,\n\n[Describe your feedback, bug, or feature request here]\n\n---\nApp: $BUILD_TYPE v$APP_VERSION\nDevice: ${android.os.Build.MODEL}\nAndroid: ${android.os.Build.VERSION.RELEASE}\n")
                    }
                    if (intent.resolveActivity(context.packageManager) != null) context.startActivity(intent)
                }
                SettingsDivider()
                SettingsNavRow(Icons.Filled.Share, Color(0xFF10B981), "Share Ontime", "Spread the word with friends & family") {
                    context.startActivity(Intent.createChooser(
                        Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Check out Ontime — Premium Productivity App")
                            putExtra(Intent.EXTRA_TEXT, "Hey! I've been using Ontime for alarms, focus sessions & more. Try it!\n\n$PLAY_STORE_URL")
                        }, "Share Ontime"
                    ))
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── Privacy & Security ────────────────────────────────────────────────
        item { SettingsSectionLabel("Privacy & Security") }
        item {
            SettingsGroup {
                SettingsNavRow(Icons.Filled.Shield, Success, "App Protection", "Battery, accessibility & device admin") { navController.navigate(Screen.FocusSettings.route) }
                SettingsDivider()
                // Cloud Backup
                val rc = LocalOntimeColors.current
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(enabled = !isRestoring) { alarmViewModel.restoreFromCloud() }.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(if (showRestoreResult) Success.copy(alpha = 0.12f) else Primary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isRestoring) androidx.compose.material3.CircularProgressIndicator(Modifier.size(18.dp), color = Primary, strokeWidth = 2.dp)
                        else Icon(Icons.Filled.Cloud, null, tint = if (showRestoreResult) Success else Primary, modifier = Modifier.size(18.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("Cloud Backup & Restore", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, fontFamily = MulishFamily, color = rc.textPrimary)
                        Text(
                            when { isRestoring -> "Restoring your alarms..."; showRestoreResult -> if (restoredCount > 0) "✅ $restoredCount alarm${if (restoredCount > 1) "s" else ""} restored" else "✅ Already up to date"; else -> "Restore alarms from your account" },
                            fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = MulishFamily,
                            color = if (showRestoreResult) Success else rc.textMuted,
                        )
                    }
                    if (!isRestoring) Icon(Icons.Filled.ChevronRight, null, tint = rc.textMuted.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── Data ──────────────────────────────────────────────────────────────
        item { SettingsSectionLabel("Data") }
        item {
            SettingsGroup {
                SettingsNavRow(Icons.Filled.DateRange, Primary, "Focus Stats", "View your productivity history") { navController.navigate(Screen.FocusStats.route) }
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── About Ontime ──────────────────────────────────────────────────────
        item { SettingsSectionLabel("About Ontime") }
        item {
            SettingsGroup {
                SettingsNavRow(Icons.Filled.Person, Color(0xFF7C3AED), "About the Developer", "The story behind Ontime") { showAboutDev = true }
                SettingsDivider()
                SettingsNavRow(Icons.Filled.Code, Color(0xFF06B6D4), "Open Source Libraries", "Acknowledgements & licenses") { showLibraries = true }
                SettingsDivider()
                SettingsNavRow(Icons.Filled.Star, Color(0xFF64748B), "Star on GitHub", "Show your support — give us a ⭐") {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_REPO_URL)))
                }
                SettingsDivider()
                // Version row with BETA chip
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val vc = LocalOntimeColors.current
                    Box(modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(vc.surfaceHigh), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Info, null, tint = vc.textMuted, modifier = Modifier.size(18.dp))
                    }
                    Text("Version", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, fontFamily = MulishFamily, color = vc.textPrimary, modifier = Modifier.weight(1f))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("v$APP_VERSION", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = MulishFamily, color = vc.textMuted)
                        Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(Warning.copy(alpha = 0.12f)).border(1.dp, Warning.copy(alpha = 0.25f), RoundedCornerShape(4.dp)).padding(horizontal = 5.dp, vertical = 1.dp)) {
                            Text("BETA", fontSize = 7.sp, fontWeight = FontWeight.Black, fontFamily = MulishFamily, color = Warning, letterSpacing = 0.8.sp)
                        }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── Legal ─────────────────────────────────────────────────────────────
        item { SettingsSectionLabel("Legal") }
        item {
            SettingsGroup {
                SettingsNavRow(Icons.Filled.Gavel, Color(0xFF64748B), "Terms of Service", "Rules and usage agreement") { showTerms = true }
                SettingsDivider()
                SettingsNavRow(Icons.Filled.PrivacyTip, Color(0xFF0EA5E9), "Privacy Policy", "How we handle your data") { showPrivacy = true }
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── Account ───────────────────────────────────────────────────────────
        item { SettingsSectionLabel("Account") }
        item {
            SettingsGroup {
                val ac = LocalOntimeColors.current
                Row(Modifier.fillMaxWidth().clickable { showSignOut = true }.padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Danger.copy(alpha = 0.10f)), Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = Danger, modifier = Modifier.size(18.dp)) }
                    Text("Sign Out", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, fontFamily = MulishFamily, color = Danger, modifier = Modifier.weight(1f))
                }
                SettingsDivider()
                Row(Modifier.fillMaxWidth().clickable { showDeleteAcct = true }.padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Danger.copy(alpha = 0.06f)), Alignment.Center) { Icon(Icons.Filled.Delete, null, tint = Danger.copy(alpha = 0.7f), modifier = Modifier.size(18.dp)) }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("Delete Account", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, fontFamily = MulishFamily, color = Danger.copy(alpha = 0.7f))
                        Text("Permanently removes all your data", fontSize = 10.sp, fontWeight = FontWeight.Medium, fontFamily = MulishFamily, color = ac.textMuted)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // ── Footer with animated signature ─────────────────────────────────────
        item {
            val fc = LocalOntimeColors.current
            Column(
                modifier            = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                AnimatedSignature(textColor = fc.textPrimary)
                Text("$BUILD_TYPE v$APP_VERSION", fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = MulishFamily, color = fc.textMuted)
                Text("For the ones who always believed 🙏", fontSize = 10.sp, fontWeight = FontWeight.Medium, fontFamily = MulishFamily, color = fc.textMuted.copy(alpha = 0.6f), textAlign = TextAlign.Center)
            }
        }
    }

    // ── Sheets ────────────────────────────────────────────────────────────────
    if (showFaq)       FaqSheet          (onDismiss = { showFaq = false })
    if (showAboutDev)  AboutDeveloperSheet(onDismiss = { showAboutDev = false })
    if (showLibraries) LibrariesSheet    (onDismiss = { showLibraries = false })
    if (showTerms)     LegalSheet        (title = "Terms of Service", content = termsContent, onDismiss = { showTerms = false })
    if (showPrivacy)   LegalSheet        (title = "Privacy Policy",   content = privacyContent, onDismiss = { showPrivacy = false })

    if (showPinSetup) {
        PinSetupSheet(appLockManager = appLockManager,
            onSuccess = { appLockManager.isEnabled = true; appLockEnabled = true; showPinSetup = false },
            onDismiss = { if (!appLockManager.hasPinSet) { appLockManager.isEnabled = false; appLockEnabled = false }; showPinSetup = false })
    }
    if (showPinVerify) {
        PinVerifySheet(appLockManager = appLockManager,
            onVerified = { appLockManager.isEnabled = false; appLockManager.clearPin(); appLockEnabled = false; showPinVerify = false },
            onDismiss  = { showPinVerify = false })
    }
    if (showSignOut) {
        ConfirmDialog("Sign out?", "You'll need to sign in again to access your data.", "Sign Out", {
            showSignOut = false; authViewModel.logout()
            navController.navigate(Screen.Welcome.route) { popUpTo(0) { inclusive = true } }
        }, { showSignOut = false })
    }
    if (showDeleteAcct) {
        ConfirmDialog("Delete account?", "This will permanently delete your account and all associated data. This cannot be undone.", "Delete",
            { showDeleteAcct = false }, { showDeleteAcct = false })
    }
}

// ─── Animated signature line ──────────────────────────────────────────────────
// "Made With ❤️ By" is static; "TUSHAR TAMRAKAR" pulses purple → pink → purple.

@Composable
private fun AnimatedSignature(textColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "signature")
    val nameColor by infiniteTransition.animateColor(
        initialValue  = Primary,
        targetValue   = Color(0xFFEC4899),       // Accent pink
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "nameColor",
    )
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        Text(
            text       = "Made With ❤️ By ",
            fontSize   = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = MulishFamily,
            color      = textColor,
        )
        Text(
            text          = "TUSHAR TAMRAKAR",
            fontSize      = 13.sp,
            fontWeight    = FontWeight.Black,
            fontFamily    = MulishFamily,
            color         = nameColor,
            letterSpacing = 0.6.sp,
        )
    }
}

// ─── FAQ Sheet ────────────────────────────────────────────────────────────────

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun FaqSheet(onDismiss: () -> Unit) {
    val colors = LocalOntimeColors.current
    var expandedIndex by remember { mutableStateOf(-1) }

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = colors.background,
        shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle       = {
            Box(Modifier.fillMaxWidth().padding(top = 12.dp), Alignment.Center) {
                Box(Modifier.width(36.dp).height(4.dp).clip(CircleShape).background(colors.border))
            }
        },
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            // Header
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF06B6D4).copy(alpha = 0.12f)), Alignment.Center) {
                    Icon(Icons.Filled.Help, null, tint = Color(0xFF06B6D4), modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("FAQs", fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = MulishFamily, color = colors.textPrimary)
                    Text("Frequently Asked Questions", fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = MulishFamily, color = colors.textMuted)
                }
            }
            LazyColumn(
                modifier       = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            ) {
                items(faqItems.indices.toList()) { i ->
                    val isExpanded = expandedIndex == i
                    val rotDeg by animateFloatAsState(if (isExpanded) 180f else 0f, tween(250), label = "chevron$i")
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(14.dp)).background(colors.surface)
                            .border(1.dp, if (isExpanded) Primary.copy(alpha = 0.22f) else colors.border.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                            .clickable { expandedIndex = if (isExpanded) -1 else i },
                    ) {
                        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(faqItems[i].question, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, fontFamily = MulishFamily, color = colors.textPrimary, modifier = Modifier.weight(1f))
                            Icon(Icons.Filled.ExpandMore, null, tint = if (isExpanded) Primary else colors.textMuted, modifier = Modifier.size(20.dp).rotate(rotDeg))
                        }
                        AnimatedVisibility(visible = isExpanded, enter = expandVertically(tween(250)) + fadeIn(tween(200)), exit = shrinkVertically(tween(200)) + fadeOut(tween(150))) {
                            Box(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(colors.surfaceHigh).padding(12.dp)) {
                                    Text(faqItems[i].answer, fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = MulishFamily, color = colors.textSecondary, lineHeight = 18.sp)
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

// ─── About Developer Sheet ────────────────────────────────────────────────────

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun AboutDeveloperSheet(onDismiss: () -> Unit) {
    val colors = LocalOntimeColors.current
    val context = LocalContext.current

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = colors.background,
        shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle       = {
            Box(Modifier.fillMaxWidth().padding(top = 12.dp), Alignment.Center) {
                Box(Modifier.width(36.dp).height(4.dp).clip(CircleShape).background(colors.border))
            }
        },
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(20.dp))

            // Purple gradient avatar
            Box(
                modifier = Modifier.size(88.dp).clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Primary, Color(0xFF4F1BBF))))
                    .border(3.dp, Primary.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("TT", fontSize = 28.sp, fontWeight = FontWeight.Black, fontFamily = MulishFamily, color = Color.White)
            }

            Spacer(Modifier.height(14.dp))
            Text(DEVELOPER_NAME, fontSize = 22.sp, fontWeight = FontWeight.Black, fontFamily = MulishFamily, color = colors.textPrimary)
            Text("Android Developer · Creator of Ontime", fontSize = 13.sp, fontWeight = FontWeight.Medium, fontFamily = MulishFamily, color = colors.textMuted)

            Spacer(Modifier.height(6.dp))
            Box(
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Primary.copy(alpha = 0.10f)).border(1.dp, Primary.copy(alpha = 0.22f), RoundedCornerShape(8.dp))
                    .clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(GITHUB_URL))) }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Code, null, tint = Primary, modifier = Modifier.size(15.dp))
                    Text("github.com/TUSHARTAMRAKAR", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = MulishFamily, color = Primary)
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = Primary.copy(alpha = 0.7f), modifier = Modifier.size(12.dp))
                }
            }

            Spacer(Modifier.height(24.dp))

            // Story card
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(18.dp)).background(colors.surface)
                    .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("The Story 🌟", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, fontFamily = MulishFamily, color = colors.textPrimary)
                Text(
                    "Ontime was born out of a simple frustration: no single productivity app did everything I needed — smart alarms, real focus sessions, a calendar I could actually trust, and health tracking all in one place. So I built it.\n\nEvery feature in Ontime solves a real problem I personally faced. This app represents countless late nights, hundreds of iterations, and an unwavering belief that the perfect productivity companion could exist.",
                    fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = MulishFamily, color = colors.textSecondary, lineHeight = 19.sp,
                )
            }

            Spacer(Modifier.height(14.dp))

            // Family dedication card
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(listOf(Primary.copy(alpha = 0.08f), Color(0xFFEC4899).copy(alpha = 0.06f))))
                    .border(1.dp, Primary.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("With Gratitude 🙏", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, fontFamily = MulishFamily, color = colors.textPrimary)
                Text(
                    "Ontime was built for the people who believed in me before I believed in myself.",
                    fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = MulishFamily, color = colors.textSecondary, lineHeight = 18.sp,
                )

                DedicationLine("👨‍👩‍👦  My Parents", "Your quiet strength, constant prayers, and unconditional love are the foundation of everything I do. Every feature in this app carries your blessing.")
                DedicationLine("❤️  My Wife Pooja", "Thank you for your patience through the late nights, your encouragement when I doubted myself, and your love that makes every milestone feel complete. You are my greatest supporter.")

                Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Primary.copy(alpha = 0.07f)).padding(12.dp)) {
                    Text(
                        "\"This app is as much yours as it is mine.\"",
                        fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = MulishFamily,
                        color = Primary, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // Animated signature footer
            AnimatedSignature(textColor = colors.textPrimary)
            Spacer(Modifier.height(4.dp))
            Text("$BUILD_TYPE v$APP_VERSION", fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = MulishFamily, color = colors.textMuted)
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DedicationLine(title: String, body: String) {
    val colors = LocalOntimeColors.current
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(colors.surfaceHigh).padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, fontFamily = MulishFamily, color = colors.textPrimary)
        Text(body, fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = MulishFamily, color = colors.textSecondary, lineHeight = 17.sp)
    }
}

// ─── Open Source Libraries Sheet ──────────────────────────────────────────────

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun LibrariesSheet(onDismiss: () -> Unit) {
    val colors = LocalOntimeColors.current
    val context = LocalContext.current

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = colors.background,
        shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle       = {
            Box(Modifier.fillMaxWidth().padding(top = 12.dp), Alignment.Center) {
                Box(Modifier.width(36.dp).height(4.dp).clip(CircleShape).background(colors.border))
            }
        },
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF06B6D4).copy(alpha = 0.12f)), Alignment.Center) {
                    Icon(Icons.Filled.Code, null, tint = Color(0xFF06B6D4), modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("Open Source Libraries", fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = MulishFamily, color = colors.textPrimary)
                    Text("Built on the shoulders of giants", fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = MulishFamily, color = colors.textMuted)
                }
            }
            LazyColumn(contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp)) {
                items(ossLibraries) { lib ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(12.dp)).background(colors.surface)
                            .border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(lib.url))) }
                            .padding(14.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(Modifier.size(36.dp).clip(RoundedCornerShape(9.dp)).background(Primary.copy(alpha = 0.10f)), Alignment.Center) {
                            Icon(Icons.Filled.Code, null, tint = Primary, modifier = Modifier.size(16.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(lib.name, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, fontFamily = MulishFamily, color = colors.textPrimary)
                            Text(lib.license, fontSize = 10.sp, fontWeight = FontWeight.Medium, fontFamily = MulishFamily, color = colors.textMuted)
                        }
                        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = colors.textMuted.copy(alpha = 0.5f), modifier = Modifier.size(14.dp))
                    }
                }
                item { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

// ─── Legal Sheet (Terms + Privacy) ───────────────────────────────────────────

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun LegalSheet(title: String, content: String, onDismiss: () -> Unit) {
    val colors = LocalOntimeColors.current
    val icon   = if (title.contains("Privacy")) Icons.Filled.PrivacyTip else Icons.Filled.Gavel
    val tint   = if (title.contains("Privacy")) Color(0xFF0EA5E9) else Color(0xFF64748B)

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor   = colors.background,
        shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle       = {
            Box(Modifier.fillMaxWidth().padding(top = 12.dp), Alignment.Center) {
                Box(Modifier.width(36.dp).height(4.dp).clip(CircleShape).background(colors.border))
            }
        },
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding()) {
            // Header
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.12f)), Alignment.Center) {
                    Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(title, fontSize = 18.sp, fontWeight = FontWeight.Black, fontFamily = MulishFamily, color = colors.textPrimary)
                    Text("Effective: June 2026", fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = MulishFamily, color = colors.textMuted)
                }
            }
            // Scrollable content
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false)
                    .padding(horizontal = 20.dp).clip(RoundedCornerShape(16.dp))
                    .background(colors.surface).border(1.dp, colors.border.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(18.dp),
                ) {
                    Text(content, fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = MulishFamily, color = colors.textSecondary, lineHeight = 19.sp)
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

// ─── Theme Picker ─────────────────────────────────────────────────────────────

@Composable
private fun ThemePickerCard(currentMode: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val colors = LocalOntimeColors.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).clip(RoundedCornerShape(16.dp)).background(colors.surface)) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(Primary.copy(alpha = 0.12f)), Alignment.Center) { Icon(Icons.Filled.Brightness4, null, tint = Primary, modifier = Modifier.size(18.dp)) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("Theme", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, fontFamily = MulishFamily, color = colors.textPrimary)
                Text("Choose your app appearance", fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = MulishFamily, color = colors.textMuted)
            }
        }
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK).forEach { mode ->
                ThemeOptionCard(mode = mode, isSelected = currentMode == mode, onClick = { onSelect(mode) }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ThemeOptionCard(mode: ThemeMode, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalOntimeColors.current
    val borderColor by animateColorAsState(if (isSelected) Primary else colors.border, tween(250), label = "border_${mode.name}")
    val borderWidth by animateDpAsState(if (isSelected) 1.5.dp else 0.7.dp, tween(250), label = "bw_${mode.name}")
    Column(modifier.clip(RoundedCornerShape(12.dp)).border(borderWidth, borderColor, RoundedCornerShape(12.dp)).clickable { onClick() }.padding(horizontal = 5.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        ThemeMiniPreview(mode, isSelected)
        Text(mode.label, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold, fontFamily = MulishFamily, color = if (isSelected) Primary else colors.textMuted, letterSpacing = 0.3.sp)
    }
}

@Composable
private fun ThemeMiniPreview(mode: ThemeMode, isSelected: Boolean) {
    Box(Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(8.dp))) {
        when (mode) {
            ThemeMode.DARK   -> Box(Modifier.fillMaxSize().background(Color(0xFF0A0A0F)).padding(8.dp)) { Column(verticalArrangement = Arrangement.spacedBy(5.dp)) { Box(Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFF7C3AED))); Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { Box(Modifier.weight(1.4f).height(18.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF1A1A2E))); Box(Modifier.weight(1f).height(18.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF252540))) }; Box(Modifier.fillMaxWidth(0.72f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFF1A1A2E))) } }
            ThemeMode.LIGHT  -> Box(Modifier.fillMaxSize().background(Color(0xFFF6F3F0)).padding(8.dp)) { Column(verticalArrangement = Arrangement.spacedBy(5.dp)) { Box(Modifier.fillMaxWidth().height(9.dp).clip(RoundedCornerShape(5.dp)).background(Color(0xFF7C3AED))); Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { Box(Modifier.weight(1.4f).height(18.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFFFFFFF)).border(0.5.dp, Color(0xFFE4DFEF), RoundedCornerShape(4.dp))); Box(Modifier.weight(1f).height(18.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFEDE9F8))) }; Box(Modifier.fillMaxWidth(0.72f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFFFFFFFF)).border(0.5.dp, Color(0xFFE4DFEF), RoundedCornerShape(3.dp))) } }
            ThemeMode.SYSTEM -> Box(Modifier.fillMaxSize()) {
                Canvas(Modifier.fillMaxSize()) {
                    val dp = Path(); dp.moveTo(0f,0f); dp.lineTo(size.width*.58f,0f); dp.lineTo(size.width*.42f,size.height); dp.lineTo(0f,size.height); dp.close(); drawPath(dp,Color(0xFF0A0A0F))
                    val lp = Path(); lp.moveTo(size.width*.58f,0f); lp.lineTo(size.width,0f); lp.lineTo(size.width,size.height); lp.lineTo(size.width*.42f,size.height); lp.close(); drawPath(lp,Color(0xFFF6F3F0))
                    drawLine(Color(0xFF7C3AED).copy(alpha=0.6f), androidx.compose.ui.geometry.Offset(size.width*.58f,0f), androidx.compose.ui.geometry.Offset(size.width*.42f,size.height), 1.5f)
                }
                Box(Modifier.fillMaxWidth().padding(horizontal=8.dp, vertical=7.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF7C3AED).copy(alpha=0.72f)))
            }
        }
        if (isSelected) Box(Modifier.align(Alignment.TopEnd).padding(5.dp).size(17.dp).clip(CircleShape).background(Primary), Alignment.Center) { Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(10.dp)) }
    }
}

// ─── Shared UI composables ────────────────────────────────────────────────────

@Composable
private fun SettingsSectionLabel(title: String) {
    val colors = LocalOntimeColors.current
    Text(title.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, fontFamily = MulishFamily, color = colors.textMuted, letterSpacing = 1.2.sp, modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp))
}

@Composable
private fun SettingsGroup(content: @Composable () -> Unit) {
    val colors = LocalOntimeColors.current
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).clip(RoundedCornerShape(16.dp)).background(colors.surface)) { content() }
}

@Composable
private fun SettingsDivider() {
    val colors = LocalOntimeColors.current
    Box(Modifier.fillMaxWidth().padding(start = 66.dp).height(0.5.dp).background(colors.border.copy(alpha = 0.5f)))
}

@Composable
private fun SettingsNavRow(icon: ImageVector, iconTint: Color, label: String, subtitle: String, onClick: () -> Unit) {
    val colors = LocalOntimeColors.current
    Row(Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(iconTint.copy(alpha = 0.12f)), Alignment.Center) { Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp)) }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, fontFamily = MulishFamily, color = colors.textPrimary)
            Text(subtitle, fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = MulishFamily, color = colors.textMuted)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = colors.textMuted.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun ConfirmDialog(title: String, message: String, confirmLabel: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val colors = LocalOntimeColors.current
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = colors.surface, shape = RoundedCornerShape(20.dp),
        title  = { Text(title, fontFamily = MulishFamily, fontWeight = FontWeight.Black, color = colors.textPrimary) },
        text   = { Text(message, fontFamily = MulishFamily, fontWeight = FontWeight.Medium, color = colors.textSecondary, lineHeight = 20.sp) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel, color = Danger, fontFamily = MulishFamily, fontWeight = FontWeight.ExtraBold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = colors.textMuted, fontFamily = MulishFamily, fontWeight = FontWeight.Bold) } },
    )
}

// ─── App Lock Card ────────────────────────────────────────────────────────────

@Composable
private fun AppLockCard(isEnabled: Boolean, hasPinSet: Boolean, canBiometric: Boolean, onToggle: (Boolean) -> Unit, onChangePin: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalOntimeColors.current
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surface)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(if (isEnabled) Primary.copy(alpha = 0.12f) else colors.surfaceHigh), Alignment.Center) {
                Icon(if (isEnabled) Icons.Filled.Lock else Icons.Filled.LockOpen, null, tint = if (isEnabled) Primary else colors.textMuted, modifier = Modifier.size(18.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("App Lock", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, fontFamily = MulishFamily, color = colors.textPrimary)
                Text(when { isEnabled && canBiometric -> "PIN + biometric"; isEnabled -> "PIN protected"; else -> "Lock app with PIN" }, fontSize = 11.sp, fontWeight = FontWeight.Medium, fontFamily = MulishFamily, color = colors.textMuted)
            }
            Box(Modifier.clip(RoundedCornerShape(20.dp)).background(if (isEnabled) Primary.copy(alpha = 0.15f) else colors.surfaceHigh).border(1.dp, if (isEnabled) Primary.copy(alpha = 0.3f) else colors.border, RoundedCornerShape(20.dp)).clickable { onToggle(!isEnabled) }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text(if (isEnabled) "ON" else "OFF", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, fontFamily = MulishFamily, color = if (isEnabled) Primary else colors.textMuted)
            }
        }
        if (isEnabled && hasPinSet) {
            Box(Modifier.fillMaxWidth().height(0.5.dp).padding(start = 66.dp).background(colors.border.copy(alpha = 0.5f)))
            Row(Modifier.fillMaxWidth().clickable { onChangePin() }.padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.size(38.dp))
                Text("Change PIN", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = MulishFamily, color = Primary, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ChevronRight, null, tint = colors.textMuted.copy(alpha = 0.4f), modifier = Modifier.size(14.dp))
            }
        }
    }
}

// ─── PIN Verify Sheet ─────────────────────────────────────────────────────────

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun PinVerifySheet(appLockManager: AppLockManager, onVerified: () -> Unit, onDismiss: () -> Unit) {
    val colors = LocalOntimeColors.current
    var pin by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }
    val pinLen = 4
    fun appendDigit(d: String) {
        if (pin.length >= pinLen) return; pin += d; errorMsg = ""
        if (pin.length == pinLen) { if (appLockManager.verifyPin(pin)) onVerified() else { errorMsg = "Wrong PIN — try again"; pin = "" } }
    }
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.background, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), dragHandle = {},
    ) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 32.dp, vertical = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Confirm Current PIN", fontSize = 20.sp, fontWeight = FontWeight.Black, fontFamily = MulishFamily, color = colors.textPrimary)
            Text("Enter your current PIN to disable App Lock", fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = MulishFamily, color = colors.textMuted, textAlign = TextAlign.Center)
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                repeat(pinLen) { i -> val filled = i < pin.length; Box(Modifier.size(14.dp).clip(CircleShape).background(if (filled) Primary else colors.surfaceHigh).border(1.dp, if (!filled) colors.border else Color.Transparent, CircleShape)) }
            }
            Text(errorMsg, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = MulishFamily, color = Danger, modifier = Modifier.height(16.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(listOf("1","2","3"), listOf("4","5","6"), listOf("7","8","9")).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        row.forEach { d -> Box(Modifier.size(60.dp).clip(CircleShape).background(colors.surface).border(1.dp, colors.border, CircleShape).clickable { appendDigit(d) }, Alignment.Center) { Text(d, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, fontFamily = MulishFamily, color = colors.textPrimary) } }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Spacer(Modifier.size(60.dp))
                    Box(Modifier.size(60.dp).clip(CircleShape).background(colors.surface).border(1.dp, colors.border, CircleShape).clickable { appendDigit("0") }, Alignment.Center) { Text("0", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, fontFamily = MulishFamily, color = colors.textPrimary) }
                    Box(Modifier.size(60.dp).clip(CircleShape).background(colors.surfaceHigh).clickable { if (pin.isNotEmpty()) pin = pin.dropLast(1) }, Alignment.Center) { Icon(Icons.AutoMirrored.Filled.Backspace, null, tint = colors.textSecondary, modifier = Modifier.size(20.dp)) }
                }
            }
        }
    }
}



