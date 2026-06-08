package com.tushartamrakar.ontime.core.security

import android.util.Log
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
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

private const val TAG       = "AppLockScreen"
private const val PIN_LEN   = 4

@Composable
fun AppLockScreen(
    appLockManager: AppLockManager,
    onUnlocked: () -> Unit,
) {
    val context      = LocalContext.current
    val canBiometric = remember { appLockManager.canUseBiometrics() }

    var pin              by remember { mutableStateOf("") }
    var errorMsg         by remember { mutableStateOf("") }
    var shakeOffset      by remember { mutableStateOf(0f) }
    var unlockSuccess    by remember { mutableStateOf(false) }
    var failCount        by remember { mutableIntStateOf(appLockManager.failCount) }

    // Shake animation on wrong PIN
    val animatedShake by animateFloatAsState(
        targetValue   = shakeOffset,
        animationSpec = keyframes {
            durationMillis = 400
            0f  at  0
            -12f at 50; 12f at 100; -12f at 150; 12f at 200
            -8f  at 250; 8f  at 300; 0f   at 400
        },
        label = "shake",
        finishedListener = { shakeOffset = 0f },
    )

    // ── Biometric prompt ──────────────────────────────────────────────────────

    fun launchBiometric() {
        if (!canBiometric) return
        val activity  = context as? FragmentActivity ?: return
        val executor  = ContextCompat.getMainExecutor(activity)
        val callback  = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                unlockSuccess = true
                appLockManager.onUnlocked()
                onUnlocked()
            }
            override fun onAuthenticationError(code: Int, msg: CharSequence) {
                if (code != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                    code != BiometricPrompt.ERROR_USER_CANCELED
                ) {
                    errorMsg = "Biometric failed — use PIN"
                }
                Log.d(TAG, "Biometric error $code: $msg")
            }
            override fun onAuthenticationFailed() {
                errorMsg = "Not recognised — try again"
            }
        }
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Ontime")
            .setSubtitle("Use your fingerprint to continue")
            .setNegativeButtonText("Use PIN")
            .build()
        BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
    }

    // Auto-trigger biometric on first appearance
    LaunchedEffect(Unit) { if (canBiometric) launchBiometric() }

    // ── PIN logic ─────────────────────────────────────────────────────────────

    fun appendDigit(d: String) {
        if (pin.length >= PIN_LEN) return
        pin += d
        errorMsg = ""

        if (pin.length == PIN_LEN) {
            if (appLockManager.verifyPin(pin)) {
                unlockSuccess = true
                appLockManager.onUnlocked()
                onUnlocked()
            } else {
                failCount = appLockManager.failCount
                shakeOffset = 1f   // triggers shake animation
                errorMsg = if (failCount >= AppLockManager.MAX_ATTEMPTS)
                    "Too many attempts. Try biometrics."
                else
                    "Wrong PIN — ${AppLockManager.MAX_ATTEMPTS - failCount} tries left"
                pin = ""
            }
        }
    }

    fun backspace() {
        if (pin.isNotEmpty()) pin = pin.dropLast(1)
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier            = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {

            // ── Top: app name + subtitle ──────────────────────────────────────
            Column(
                modifier            = Modifier.padding(top = 56.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Primary.copy(alpha = 0.12f))
                        .border(1.dp, Primary.copy(alpha = 0.25f), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("⏰", fontSize = 32.sp)
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    text       = "Ontime is locked",
                    fontSize   = 22.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = MulishFamily,
                    color      = TextPrimary,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text       = "Enter your PIN to continue",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = MulishFamily,
                    color      = TextMuted,
                )
            }

            // ── Middle: dots + error ──────────────────────────────────────────
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // PIN dots
                Row(
                    modifier              = Modifier.scale(1f + animatedShake / 200f),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    repeat(PIN_LEN) { i ->
                        val filled = i < pin.length
                        val dotColor by animateColorAsState(
                            targetValue   = when {
                                filled && errorMsg.isNotEmpty() -> Danger
                                filled                          -> Primary
                                else                            -> SurfaceHigh
                            },
                            label = "dot_$i",
                        )
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                                .border(
                                    width = if (!filled) 1.dp else 0.dp,
                                    color = Border,
                                    shape = CircleShape,
                                ),
                        )
                    }
                }

                // Error or empty spacer
                Text(
                    text       = errorMsg,
                    fontSize   = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = MulishFamily,
                    color      = Danger,
                    textAlign  = TextAlign.Center,
                    modifier   = Modifier.height(18.dp),
                )
            }

            // ── Bottom: numpad ────────────────────────────────────────────────
            Column(
                modifier            = Modifier.padding(bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Rows 1-3
                listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                ).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        row.forEach { digit ->
                            NumpadKey(
                                label   = digit,
                                onClick = { appendDigit(digit) },
                            )
                        }
                    }
                }
                // Row 4: biometric | 0 | backspace
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    // Biometric shortcut or empty slot
                    if (canBiometric) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(SurfaceHigh)
                                .clickable { launchBiometric() },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector        = Icons.Filled.Fingerprint,
                                contentDescription = "Biometric",
                                tint               = Primary,
                                modifier           = Modifier.size(28.dp),
                            )
                        }
                    } else {
                        Spacer(Modifier.size(72.dp))
                    }
                    NumpadKey(label = "0", onClick = { appendDigit("0") })
                    // Backspace
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(SurfaceHigh)
                            .clickable { backspace() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector        = Icons.Filled.Backspace,
                            contentDescription = "Delete",
                            tint               = TextSecondary,
                            modifier           = Modifier.size(24.dp),
                        )
                    }
                }
            }
        }
    }
}

// ─── Numpad key ───────────────────────────────────────────────────────────────

@Composable
private fun NumpadKey(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(Surface)
            .border(1.dp, Border, CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text       = label,
            fontSize   = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            fontFamily = MulishFamily,
            color      = TextPrimary,
        )
    }
}
