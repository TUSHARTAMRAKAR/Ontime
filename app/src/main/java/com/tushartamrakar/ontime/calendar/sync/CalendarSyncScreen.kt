package com.tushartamrakar.ontime.calendar.sync

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.tushartamrakar.ontime.calendar.data.repository.CalendarRepository
import com.tushartamrakar.ontime.core.ui.theme.Background
import com.tushartamrakar.ontime.core.ui.theme.Border
import com.tushartamrakar.ontime.core.ui.theme.CardBackground
import com.tushartamrakar.ontime.core.ui.theme.Danger
import com.tushartamrakar.ontime.core.ui.theme.MulishFamily
import com.tushartamrakar.ontime.core.ui.theme.Primary
import com.tushartamrakar.ontime.core.ui.theme.SurfaceHigh
import com.tushartamrakar.ontime.core.ui.theme.TextMuted
import com.tushartamrakar.ontime.core.ui.theme.TextPrimary
import com.tushartamrakar.ontime.calendar.data.local.LiveHolidayCache
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.launch
import java.time.LocalDate

@EntryPoint
@InstallIn(SingletonComponent::class)
interface LiveHolidayCacheEntryPoint {
    fun liveHolidayCache(): LiveHolidayCache
}

@Composable
fun CalendarSyncScreen(
    navController: NavHostController,
    repository: CalendarRepository,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val syncManager = remember { GoogleCalendarSyncManager(context, repository) }

    // ─── LiveHolidayCache via Hilt EntryPoint ────────────────────────────────
    val liveHolidayCache = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            LiveHolidayCacheEntryPoint::class.java,
        ).liveHolidayCache()
    }

    var isSignedIn by remember { mutableStateOf(GoogleSignInHelper.isSignedIn(context)) }
    var userEmail by remember { mutableStateOf("") }
    var isSyncing by remember { mutableStateOf(false) }
    var lastSyncResult by remember { mutableStateOf<GoogleCalendarSyncManager.SyncResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Load signed in account
    LaunchedEffect(isSignedIn) {
        val account = GoogleSignInHelper.getSignedInAccount(context)
        userEmail = account?.email ?: ""
    }

    // ─── Sign In launcher ─────────────────────────────────────────────────────
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.result
                isSignedIn = true
                userEmail = account?.email ?: ""
                errorMessage = null
                // Clear stale caches so all months reload with Google data
                liveHolidayCache.onGoogleSignedIn()
                // Auto sync after sign in
                scope.launch {
                    isSyncing = true
                    val syncResult = syncManager.fullSync(account)
                    lastSyncResult = syncResult
                    isSyncing = false
                }
            } catch (e: Exception) {
                errorMessage = "Sign in failed: ${e.message}"
            }
        }
    }

    // ─── Spinning animation for sync icon ────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "sync_spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
        label = "spin",
    )

    Box(modifier = Modifier.fillMaxSize().background(Background)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ─── Top Bar ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back",
                        tint = TextPrimary, modifier = Modifier.size(24.dp))
                }
                Text(text = "Google Calendar Sync", fontSize = 17.sp,
                    fontWeight = FontWeight.Bold, fontFamily = MulishFamily,
                    color = TextPrimary, modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.size(48.dp))
            }

            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // ─── Google Account Card ──────────────────────────────────────
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))
                        .background(CardBackground)
                        .border(1.dp, Border, RoundedCornerShape(20.dp))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Google logo circle
                    Box(
                        modifier = Modifier.size(72.dp).clip(CircleShape)
                            .background(if (isSignedIn) Primary.copy(alpha = 0.15f) else SurfaceHigh)
                            .border(2.dp, if (isSignedIn) Primary else Border, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = if (isSignedIn) "✓" else "G",
                            fontSize = if (isSignedIn) 32.sp else 36.sp,
                            fontWeight = FontWeight.Black, color = if (isSignedIn) Primary else TextMuted,
                            fontFamily = MulishFamily)
                    }

                    if (isSignedIn) {
                        Text(text = "Connected", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                            fontFamily = MulishFamily, color = Primary)
                        Text(text = userEmail, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                            fontFamily = MulishFamily, color = TextMuted, textAlign = TextAlign.Center)
                    } else {
                        Text(text = "Not Connected", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold,
                            fontFamily = MulishFamily, color = TextPrimary)
                        Text(text = "Connect your Google account to sync events",
                            fontSize = 13.sp, fontWeight = FontWeight.Medium,
                            fontFamily = MulishFamily, color = TextMuted, textAlign = TextAlign.Center)
                    }
                }

                // ─── Error message ────────────────────────────────────────────
                if (errorMessage != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(Danger.copy(alpha = 0.1f))
                            .border(1.dp, Danger.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(text = "⚠️", fontSize = 18.sp)
                        Text(text = errorMessage!!, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                            fontFamily = MulishFamily, color = Danger)
                    }
                }

                // ─── Last sync result ─────────────────────────────────────────
                if (lastSyncResult != null) {
                    val result = lastSyncResult!!
                    val raw = result.pullError ?: result.pushError ?: ""
                    val friendlyError = when {
                        raw.contains("503") || raw.contains("backendError") || raw.contains("Service Unavailable") ->
                            "Google servers had a hiccup (503 Backend Error). Tap Sync Now to retry."
                        raw.contains("401") || raw.contains("Unauthorized") || raw.contains("Invalid Credentials") ->
                            "Session expired — disconnect and reconnect your Google account."
                        raw.contains("403") || raw.contains("insufficientPermissions") ->
                            "Permission denied — disconnect and reconnect with full access."
                        raw.contains("429") || raw.contains("Rate Limit") ->
                            "Too many requests — wait a minute and try again."
                        raw.contains("UnknownHostException") || raw.contains("SocketTimeout") ->
                            "No internet connection. Check your network and retry."
                        raw.isNotBlank() -> "Sync failed. Tap Sync Now to retry."
                        else -> ""
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .background(if (result.isSuccess) Primary.copy(alpha = 0.1f) else Danger.copy(alpha = 0.07f))
                            .border(1.dp, if (result.isSuccess) Primary.copy(alpha = 0.4f) else Danger.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            if (result.isSuccess) Icons.Filled.Check else Icons.Filled.Refresh,
                            contentDescription = null,
                            tint = if (result.isSuccess) Primary else TextMuted,
                            modifier = Modifier.size(18.dp),
                        )
                        Column {
                            Text(text = "Last sync: ${result.summary}", fontSize = 13.sp,
                                fontWeight = FontWeight.Bold, fontFamily = MulishFamily,
                                color = if (result.isSuccess) Primary else TextMuted)
                            if (!result.isSuccess && friendlyError.isNotBlank()) {
                                Text(text = friendlyError,
                                    fontSize = 11.sp, fontWeight = FontWeight.Medium,
                                    fontFamily = MulishFamily, color = Danger)
                            }
                        }
                    }
                }

                // ─── What sync does info card ─────────────────────────────────
                Column(
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .background(CardBackground)
                        .border(1.dp, Border, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(text = "What gets synced?", fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold, fontFamily = MulishFamily, color = TextPrimary)
                    SyncInfoRow(emoji = "↓", text = "Pull events from Google Calendar into Ontime")
                    SyncInfoRow(emoji = "↑", text = "Push Ontime events to Google Calendar")
                    SyncInfoRow(emoji = "🔄", text = "Full two-way sync keeps both in sync")
                    SyncInfoRow(emoji = "🔒", text = "Only accesses your calendar data, nothing else")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // ─── Action Buttons ───────────────────────────────────────────
                if (!isSignedIn) {
                    // Connect button
                    Button(
                        onClick = {
                            val signInIntent = GoogleSignInHelper.getSignInIntent(context)
                            signInLauncher.launch(signInIntent)
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Text(text = "Connect Google Calendar", fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold, fontFamily = MulishFamily,
                            color = Color.White)
                    }
                } else {
                    // Sync now button
                    Button(
                        onClick = {
                            scope.launch {
                                isSyncing = true
                                errorMessage = null
                                val account = GoogleSignInHelper.getSignedInAccount(context)
                                if (account != null) {
                                    val result = syncManager.fullSync(account)
                                    lastSyncResult = result
                                    if (!result.isSuccess) {
                                        errorMessage = result.pullError ?: result.pushError
                                    }
                                }
                                isSyncing = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        enabled = !isSyncing,
                        colors = ButtonDefaults.buttonColors(containerColor = Primary),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        if (isSyncing) {
                            Icon(Icons.Filled.Sync, contentDescription = null,
                                modifier = Modifier.size(20.dp).rotate(rotation), tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Syncing...", fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold, fontFamily = MulishFamily,
                                color = Color.White)
                        } else {
                            Icon(Icons.Filled.Refresh, contentDescription = null,
                                modifier = Modifier.size(20.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Sync Now", fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold, fontFamily = MulishFamily,
                                color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Disconnect button
                    Box(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .border(1.dp, Danger.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .clickable {
                                GoogleSignInHelper.signOut(context) {
                                    isSignedIn = false
                                    userEmail = ""
                                    lastSyncResult = null
                                }
                            }
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "Disconnect Account", fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold, fontFamily = MulishFamily,
                            color = Danger)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SyncInfoRow(emoji: String, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier.size(28.dp).clip(CircleShape)
                .background(Primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = emoji, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold,
                color = Primary, fontFamily = MulishFamily)
        }
        Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.Medium,
            fontFamily = MulishFamily, color = TextMuted)
    }
}
