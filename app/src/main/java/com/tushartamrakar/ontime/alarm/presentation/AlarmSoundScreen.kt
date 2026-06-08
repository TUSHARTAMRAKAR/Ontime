package com.tushartamrakar.ontime.alarm.presentation

import android.media.MediaPlayer
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.tushartamrakar.ontime.core.ui.theme.Background
import com.tushartamrakar.ontime.core.ui.theme.Border
import com.tushartamrakar.ontime.core.ui.theme.CardBackground
import com.tushartamrakar.ontime.core.ui.theme.MulishFamily
import com.tushartamrakar.ontime.core.ui.theme.Primary
import com.tushartamrakar.ontime.core.ui.theme.SurfaceHigh
import com.tushartamrakar.ontime.core.ui.theme.TextMuted
import com.tushartamrakar.ontime.core.ui.theme.TextPrimary

// ─── Data Models ──────────────────────────────────────────────────────────────
data class AlarmTone(
    val rawResName: String,
    val displayName: String,
    val category: String,
)

data class AlarmCategory(
    val name: String,
    val emoji: String,
)

val alarmCategories = listOf(
    AlarmCategory("All", "🎵"),
    AlarmCategory("Emergency", "🚨"),
    AlarmCategory("Animals", "🐾"),
    AlarmCategory("Motivation", "💪"),
    AlarmCategory("Melody", "🎵"),
    AlarmCategory("Funny", "😂"),
    AlarmCategory("World Alarms", "🌍"),
)

val allAlarmTones = listOf(
    // 🚨 Emergency
    AlarmTone("alarm_air_raid", "Air Raid", "Emergency"),
    AlarmTone("alarm_alert_in_hall", "Alert in Hall", "Emergency"),
    AlarmTone("alarm_digital_alarm", "Digital Alarm", "Emergency"),
    AlarmTone("alarm_digital_alarm_clock_in_alarmy", "Digital Clock", "Emergency"),
    AlarmTone("alarm_end_of_the_world", "End of The World", "Emergency"),
    AlarmTone("alarm_facility_alarm_sound", "Facility Alarm", "Emergency"),
    AlarmTone("alarm_fire_alarm", "Fire Alarm", "Emergency"),
    AlarmTone("alarm_heartbeat_warning", "Heartbeat Warning", "Emergency"),
    AlarmTone("alarm_lab_emergency_escape", "Lab Emergency", "Emergency"),
    AlarmTone("alarm_slot_machine_payout", "Slot Machine", "Emergency"),

    // 🐾 Animals
    AlarmTone("alarm_baby_poodle", "Baby Poodle", "Animals"),
    AlarmTone("alarm_baby_rabbit", "Baby Rabbit", "Animals"),
    AlarmTone("alarm_cat_dance", "Cat Dance", "Animals"),
    AlarmTone("alarm_dj_cat", "DJ Cat", "Animals"),
    AlarmTone("alarm_get_up_hooman", "Get Up Hooman", "Animals"),
    AlarmTone("alarm_golden_retriever_home", "Golden Retriever", "Animals"),
    AlarmTone("alarm_hamster_rise_and_shine", "Hamster Rise", "Animals"),
    AlarmTone("alarm_marmot", "Marmot", "Animals"),
    AlarmTone("alarm_quoka", "Quoka", "Animals"),
    AlarmTone("alarm_rooster_minecraft", "Minecraft Rooster", "Animals"),
    AlarmTone("alarm_sea_otter", "Sea Otter", "Animals"),

    // 💪 Motivation
    AlarmTone("alarm_at_this_time_you_are_sleeping", "Still Sleeping?", "Motivation"),
    AlarmTone("alarm_brain_hertz_en", "Brain Hertz", "Motivation"),
    AlarmTone("alarm_david_goggins", "David Goggins", "Motivation"),
    AlarmTone("alarm_exercise_en", "Exercise Time", "Motivation"),
    AlarmTone("alarm_gotowork", "Go To Work", "Motivation"),
    AlarmTone("alarm_mot_dont_waste_year_en", "Don't Waste Your Year", "Motivation"),
    AlarmTone("alarm_mot_life_gets_better_en", "Life Gets Better", "Motivation"),
    AlarmTone("alarm_mot_wake_smile_en", "Wake Up & Smile", "Motivation"),
    AlarmTone("alarm_mot_wake_up_scream_en", "Wake Up Scream", "Motivation"),
    AlarmTone("alarm_mot_wake_up_you_lazy_en", "Wake Up Lazy", "Motivation"),

    // 🎵 Melody
    AlarmTone("alarm_cherry_blossom_walk", "Cherry Blossom", "Melody"),
    AlarmTone("alarm_good_morning", "Good Morning", "Melody"),
    AlarmTone("alarm_greenfields", "Green Fields", "Melody"),
    AlarmTone("alarm_morning_birds", "Morning Birds", "Melody"),
    AlarmTone("alarm_morning_drive", "Morning Drive", "Melody"),
    AlarmTone("alarm_narcissus", "Narcissus", "Melody"),
    AlarmTone("alarm_peaceful_morning", "Peaceful Morning", "Melody"),

    // 😂 Funny
    AlarmTone("alarm_get_up_you_stupid_fuck", "Wake Up!!", "Funny"),
    AlarmTone("alarm_dj_cat", "DJ Cat", "Funny"),
    AlarmTone("alarm_marmot", "Marmot", "Funny"),
    AlarmTone("alarm_rooster_minecraft", "Minecraft Rooster", "Funny"),
    AlarmTone("alarm_at_this_time_you_are_sleeping", "Still Sleeping?", "Funny"),

    // 🌍 World Alarms
    AlarmTone("alarm_greece_eas_alarm", "Greece EAS", "World Alarms"),
    AlarmTone("alarm_malaysia_eas_alarm_alt", "Malaysia EAS", "World Alarms"),
    AlarmTone("alarm_minas_gerais_brazil_eas_alarm", "Brazil EAS", "World Alarms"),
    AlarmTone("alarm_taiwan_eas_alarm", "Taiwan EAS", "World Alarms"),
    AlarmTone("alarm_uk_eas_alarm", "UK EAS", "World Alarms"),
)

// ─── Alarm Sound Screen ───────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmSoundScreen(
    navController: NavHostController,
    currentSound: String,
    onSoundSelected: (String) -> Unit,
) {
    val context = LocalContext.current
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedSound by remember { mutableStateOf(currentSound) }
    var playingSound by remember { mutableStateOf<String?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    // Cleanup media player on exit
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
            mediaPlayer = null
        }
    }

    fun playPreview(rawResName: String) {
        // Stop current
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null

        if (playingSound == rawResName) {
            playingSound = null
            return
        }

        try {
            val resId = context.resources.getIdentifier(
                rawResName, "raw", context.packageName
            )
            if (resId != 0) {
                mediaPlayer = MediaPlayer.create(context, resId)?.apply {
                    setOnCompletionListener {
                        playingSound = null
                        release()
                        mediaPlayer = null
                    }
                    start()
                }
                playingSound = rawResName
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val filteredTones = if (selectedCategory == "All") {
        allAlarmTones.distinctBy { it.rawResName }
    } else {
        allAlarmTones.filter { it.category == selectedCategory }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
    ) {
        // ─── Top Bar ──────────────────────────────────────────────────────────
        TopAppBar(
            title = {
                Text(
                    text = "Alarm Sound",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = MulishFamily,
                    color = TextPrimary,
                )
            },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Primary,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Background,
            ),
        )

        // ─── Category Pills ───────────────────────────────────────────────────
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(alarmCategories) { category ->
                val isSelected = selectedCategory == category.name
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) Primary else SurfaceHigh)
                        .border(
                            width = 1.5.dp,
                            color = if (isSelected) Primary else Border,
                            shape = RoundedCornerShape(20.dp),
                        )
                        .clickable { selectedCategory = category.name }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${category.emoji} ${category.name}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = MulishFamily,
                        color = if (isSelected) Color.White else TextMuted,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ─── Tone List ────────────────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(filteredTones) { tone ->
                val isSelected = selectedSound == tone.rawResName
                val isPlaying = playingSound == tone.rawResName

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (isSelected) Primary.copy(alpha = 0.12f) else CardBackground)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) Primary else Border,
                            shape = RoundedCornerShape(14.dp),
                        )
                        .clickable {
                            selectedSound = tone.rawResName
                            onSoundSelected(tone.rawResName)
                        }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Play/Pause button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) Primary else SurfaceHigh)
                            .clickable { playPreview(tone.rawResName) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause
                            else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = if (isPlaying) Color.White else TextMuted,
                            modifier = Modifier.size(22.dp),
                        )
                    }

                    // Tone name
                    Text(
                        text = tone.displayName,
                        fontSize = 15.sp,
                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                        fontFamily = MulishFamily,
                        color = if (isSelected) Primary else TextPrimary,
                        modifier = Modifier.weight(1f),
                    )

                    // Selected indicator
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Primary),
                        )
                    }
                }
            }
        }
    }
}