package com.tushartamrakar.ontime.alarm.presentation

import android.media.AudioManager
import android.media.MediaPlayer
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.tushartamrakar.ontime.core.ui.theme.Background
import com.tushartamrakar.ontime.core.ui.theme.Border
import com.tushartamrakar.ontime.core.ui.theme.CardBackground
import com.tushartamrakar.ontime.core.ui.theme.Danger
import com.tushartamrakar.ontime.core.ui.theme.MulishFamily
import com.tushartamrakar.ontime.core.ui.theme.Primary
import com.tushartamrakar.ontime.core.ui.theme.SurfaceHigh
import com.tushartamrakar.ontime.core.ui.theme.TextMuted
import com.tushartamrakar.ontime.core.ui.theme.TextPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtraLoudInfoScreen(
    navController: NavHostController,
    currentSound: String,
    onEnable: () -> Unit,
) {
    val context = LocalContext.current

    var isSamplePlaying by remember { mutableStateOf(false) }
    var samplePhase by remember { mutableStateOf("idle") }
    var samplePlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // ─── Pulsing animation ────────────────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse_scale",
    )

    DisposableEffect(Unit) {
        onDispose {
            samplePlayer?.apply {
                try { if (isPlaying) stop() } catch (e: Exception) { }
                release()
            }
            samplePlayer = null
        }
    }

    // ─── Helper to stop sample cleanly ───────────────────────────────────────
    fun stopSample() {
        samplePlayer?.apply {
            try { if (isPlaying) stop() } catch (e: Exception) { }
            release()
        }
        samplePlayer = null
        isSamplePlaying = false
        samplePhase = "idle"
    }

    // ─── Play first available heavy tone ─────────────────────────────────────
    fun getFirstHeavyResId(): Int {
        // Try tones in order until one is found
        val heavyToneCandidates = listOf(
            "alarm_air_raid_siren",
            "alarm_buzzer",
            "alarm_emergency_alert",
            "alarm_fire_alarm",
            "alarm_klaxon",
            "alarm_civil_defense",
            "alarm_nuclear_alert",
            "alarm_submarine",
            "alarm_tornado_siren",
            "alarm_warning_horn",
        )
        for (tone in heavyToneCandidates) {
            val resId = context.resources.getIdentifier(tone, "raw", context.packageName)
            if (resId != 0) return resId
        }
        return 0
    }

    // ─── Sample play logic ────────────────────────────────────────────────────
    fun playSample() {
        if (isSamplePlaying) {
            stopSample()
            return
        }

        isSamplePlaying = true
        samplePhase = "ringtone"

        // ── Phase 1: User ringtone at soft volume for 3 seconds ───────────────
        val ringtoneResId = context.resources.getIdentifier(
            currentSound, "raw", context.packageName
        )

        if (ringtoneResId != 0) {
            try {
                val ringtonePlayer = MediaPlayer.create(context, ringtoneResId)
                ringtonePlayer?.apply {
                    setVolume(0.35f, 0.35f)
                    isLooping = false
                    start()
                    samplePlayer = this
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // ── Phase 2: After 3 seconds → BOOM! heavy tone at MAX ────────────────
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (!isSamplePlaying) return@postDelayed

            // Stop ringtone
            samplePlayer?.apply {
                try { if (isPlaying) stop() } catch (e: Exception) { }
                release()
            }
            samplePlayer = null
            samplePhase = "heavy"

            // ── Boost system alarm volume to absolute MAX ──────────────────────
            try {
                val audioManager = context.getSystemService(
                    android.content.Context.AUDIO_SERVICE
                ) as AudioManager
                val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVol, 0)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // ── Play heavy tone at max MediaPlayer volume ──────────────────────
            val heavyResId = getFirstHeavyResId()
            if (heavyResId != 0) {
                try {
                    val heavyPlayer = MediaPlayer.create(context, heavyResId)
                    heavyPlayer?.apply {
                        setVolume(1.0f, 1.0f)
                        isLooping = false
                        start()
                        samplePlayer = this
                    }

                    // Auto stop after 3.5 seconds
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        stopSample()
                    }, 3500)
                } catch (e: Exception) {
                    e.printStackTrace()
                    stopSample()
                }
            } else {
                stopSample()
            }
        }, 3000)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .verticalScroll(rememberScrollState()),
    ) {
        // ─── Top Bar ──────────────────────────────────────────────────────────
        TopAppBar(
            title = {},
            navigationIcon = {
                IconButton(onClick = {
                    stopSample()
                    navController.popBackStack()
                }) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Primary,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
        )

        // ─── Hero section ─────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Pulsing icon
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .scale(if (isSamplePlaying && samplePhase == "heavy") pulseScale else 1f)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                if (isSamplePlaying && samplePhase == "heavy")
                                    Danger.copy(alpha = 0.4f)
                                else Danger.copy(alpha = 0.2f),
                                Danger.copy(alpha = 0.05f),
                            )
                        )
                    )
                    .border(
                        width = if (isSamplePlaying && samplePhase == "heavy") 2.5.dp else 1.5.dp,
                        color = if (isSamplePlaying && samplePhase == "heavy")
                            Danger else Danger.copy(alpha = 0.5f),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when {
                        isSamplePlaying && samplePhase == "heavy" -> "💥"
                        isSamplePlaying && samplePhase == "ringtone" -> "🔔"
                        else -> "🔊"
                    },
                    fontSize = 48.sp,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Extra Loud Effect",
                fontSize = 26.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily,
                color = TextPrimary,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Danger.copy(alpha = 0.15f))
                    .padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Text(
                    text = "FOR HEAVY SLEEPERS ONLY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color = Danger,
                    letterSpacing = 1.sp,
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Forces you out of bed with an aggressive\nsound blast you simply cannot ignore",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = MulishFamily,
                color = TextMuted,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // ─── Step cards ───────────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Step 1
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSamplePlaying && samplePhase == "ringtone")
                            Primary.copy(alpha = 0.1f) else CardBackground
                    )
                    .border(
                        width = if (isSamplePlaying && samplePhase == "ringtone") 1.5.dp else 1.dp,
                        color = if (isSamplePlaying && samplePhase == "ringtone")
                            Primary else Border,
                        shape = RoundedCornerShape(16.dp),
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "🔔", fontSize = 22.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "First 35 Seconds",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = MulishFamily,
                            color = if (isSamplePlaying && samplePhase == "ringtone")
                                Primary else TextPrimary,
                        )
                        if (isSamplePlaying && samplePhase == "ringtone") {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Primary.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = "PLAYING",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = MulishFamily,
                                    color = Primary,
                                    letterSpacing = 0.5.sp,
                                )
                            }
                        }
                    }
                    Text(
                        text = "Your chosen ringtone plays normally",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = MulishFamily,
                        color = TextMuted,
                    )
                }
            }

            // Arrow
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "⬇️", fontSize = 20.sp)
            }

            // Step 2
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (isSamplePlaying && samplePhase == "heavy")
                            Danger.copy(alpha = 0.15f) else Danger.copy(alpha = 0.06f)
                    )
                    .border(
                        width = if (isSamplePlaying && samplePhase == "heavy") 2.dp else 1.dp,
                        color = if (isSamplePlaying && samplePhase == "heavy")
                            Danger else Danger.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp),
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .scale(if (isSamplePlaying && samplePhase == "heavy") pulseScale else 1f)
                        .clip(CircleShape)
                        .background(Danger.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "💥", fontSize = 22.sp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "BOOM! After 35 Seconds",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = MulishFamily,
                            color = Danger,
                        )
                        if (isSamplePlaying && samplePhase == "heavy") {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Danger.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            ) {
                                Text(
                                    text = "BLASTING!",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontFamily = MulishFamily,
                                    color = Danger,
                                    letterSpacing = 0.5.sp,
                                )
                            }
                        }
                    }
                    Text(
                        text = "Aggressive sirens & buzzers at MAXIMUM volume until dismissed!",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = MulishFamily,
                        color = TextMuted,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // ─── Warning ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceHigh)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = "⚠️", fontSize = 18.sp)
            Text(
                text = "Extremely loud! Not recommended in shared spaces or near children.",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = MulishFamily,
                color = TextMuted,
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ─── Sample section ───────────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Phase description
            Text(
                text = when {
                    isSamplePlaying && samplePhase == "ringtone" ->
                        "🔔 Your ringtone is playing softly..."
                    isSamplePlaying && samplePhase == "heavy" ->
                        "💥 BOOM! Loud effect blasting at MAX!"
                    else ->
                        "Tap to hear a short preview of both phases"
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = MulishFamily,
                color = when {
                    isSamplePlaying && samplePhase == "heavy" -> Danger
                    isSamplePlaying -> Primary
                    else -> TextMuted
                },
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Sample button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        when {
                            isSamplePlaying && samplePhase == "heavy" ->
                                Danger.copy(alpha = 0.15f)
                            isSamplePlaying -> Primary.copy(alpha = 0.15f)
                            else -> SurfaceHigh
                        }
                    )
                    .border(
                        width = 1.5.dp,
                        color = when {
                            isSamplePlaying && samplePhase == "heavy" -> Danger
                            else -> Primary
                        },
                        shape = RoundedCornerShape(16.dp),
                    )
                    .clickable { playSample() },
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = if (isSamplePlaying) Icons.Filled.Pause
                        else Icons.Filled.PlayArrow,
                        contentDescription = "Sample",
                        tint = if (isSamplePlaying && samplePhase == "heavy") Danger
                        else Primary,
                        modifier = Modifier.size(22.dp),
                    )
                    Text(
                        text = when {
                            isSamplePlaying && samplePhase == "heavy" -> "Stop"
                            isSamplePlaying -> "Playing Sample..."
                            else -> "Play Sample  (3s soft + 3s LOUD)"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = MulishFamily,
                        color = if (isSamplePlaying && samplePhase == "heavy") Danger
                        else Primary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "⚡ Turn up your volume before playing!",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = MulishFamily,
                color = TextMuted,
                textAlign = TextAlign.Center,
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ─── Enable button ────────────────────────────────────────────────────
        Button(
            onClick = {
                stopSample()
                onEnable()
                navController.popBackStack()
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(58.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Danger),
            shape = RoundedCornerShape(18.dp),
        ) {
            Text(
                text = "Enable Extra Loud Effect",
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                fontFamily = MulishFamily,
                color = Color.White,
                letterSpacing = 0.3.sp,
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
