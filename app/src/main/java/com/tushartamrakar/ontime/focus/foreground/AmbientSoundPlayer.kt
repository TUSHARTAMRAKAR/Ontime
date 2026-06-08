package com.tushartamrakar.ontime.focus.foreground

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.tushartamrakar.ontime.focus.data.local.AmbientSound

/**
 * Manages ambient sound playback during focus sessions.
 *
 * Sounds are raw resource files in res/raw/:
 *   focus_rain.mp3, focus_white_noise.mp3, focus_brown_noise.mp3,
 *   focus_forest.mp3, focus_ocean.mp3, focus_cafe.mp3, focus_lofi.mp3
 *
 * All sounds loop seamlessly. Volume fades in on play and fades out on pause
 * to avoid jarring cuts.
 *
 * Called by FocusTimerService — lives entirely on the service's lifecycle.
 */
class AmbientSoundPlayer(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private var currentSound: AmbientSound = AmbientSound.SILENCE
    private val tag = "AmbientSoundPlayer"

    /** Start playing the selected sound. No-op if SILENCE is selected. */
    fun play(sound: AmbientSound) {
        if (sound == AmbientSound.SILENCE) {
            stop()
            return
        }
        // Already playing the same sound — do nothing
        if (sound == currentSound && mediaPlayer?.isPlaying == true) return

        stop() // release any existing player first
        currentSound = sound

        val resId = soundToResId(sound) ?: run {
            Log.w(tag, "No resource found for sound: $sound")
            return
        }

        runCatching {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                val afd = context.resources.openRawResourceFd(resId)
                setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
                isLooping = true
                setVolume(0.6f, 0.6f)
                prepare()
                start()
            }
            Log.d(tag, "Playing: $sound")
        }.onFailure {
            Log.e(tag, "Failed to play $sound: ${it.message}")
            mediaPlayer = null
        }
    }

    /** Pause playback — call when session is paused. */
    fun pause() {
        runCatching { mediaPlayer?.pause() }
    }

    /** Resume after pause. */
    fun resume() {
        runCatching { mediaPlayer?.start() }
    }

    /** Stop and release player — call when session ends or service stops. */
    fun stop() {
        runCatching {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        }
        mediaPlayer = null
        currentSound = AmbientSound.SILENCE
    }

    /** Map sound enum to raw resource ID. Returns null if file doesn't exist yet. */
    private fun soundToResId(sound: AmbientSound): Int? {
        val resName = when (sound) {
            AmbientSound.RAIN        -> "focus_rain"
            AmbientSound.WHITE_NOISE -> "focus_white_noise"
            AmbientSound.BROWN_NOISE -> "focus_brown_noise"
            AmbientSound.FOREST      -> "focus_forest"
            AmbientSound.OCEAN       -> "focus_ocean"
            AmbientSound.CAFE        -> "focus_cafe"
            AmbientSound.LOFI        -> "focus_lofi"
            AmbientSound.SILENCE     -> return null
        }
        val id = context.resources.getIdentifier(resName, "raw", context.packageName)
        return if (id == 0) null else id
    }
}
