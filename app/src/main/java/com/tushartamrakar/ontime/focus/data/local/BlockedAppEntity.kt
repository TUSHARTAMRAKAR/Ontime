package com.tushartamrakar.ontime.focus.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One row per app the user has added to their block list.
 *
 * packageName is the PK — unique per app on the device.
 * BlockedAppsManager loads all rows where isEnabled = true into an
 * in-memory HashSet for O(1) lookup on every app foreground event.
 *
 * blockOnlyDuringFocus:
 *   true  → blocked only when FocusTimerService is running
 *   false → blocked always (mainly used for adult content category)
 */
@Entity(tableName = "blocked_apps")
data class BlockedAppEntity(
    @PrimaryKey
    val packageName: String,                    // e.g. "com.instagram.android"

    val appName: String,                        // display name e.g. "Instagram"
    val category: String = AppCategory.CUSTOM.name,
    val isEnabled: Boolean = true,
    val blockOnlyDuringFocus: Boolean = true,
    val addedAt: Long = System.currentTimeMillis(),
)

enum class AppCategory {
    SOCIAL,          // Instagram, Twitter, Facebook, TikTok, Snapchat, Reddit
    GAMES,           // any game
    ENTERTAINMENT,   // YouTube, Netflix, Prime Video, Hotstar
    COMMUNICATION,   // WhatsApp, Telegram, Discord (only block when user wants)
    ADULT,           // blocked always (blockOnlyDuringFocus = false)
    CUSTOM,          // user manually added
}

/** Predefined package names for each category — seed data for BlockerScreen. */
object DefaultBlockedPackages {
    val SOCIAL = setOf(
        "com.instagram.android",
        "com.twitter.android",
        "com.facebook.katana",
        "com.zhiliaoapp.musically",     // TikTok
        "com.snapchat.android",
        "com.reddit.frontpage",
        "com.linkedin.android",
    )
    val GAMES = setOf(
        "com.king.candycrushsaga",
        "com.supercell.clashofclans",
        "com.supercell.clashroyale",
        "com.miniclip.eightballpool",
        "com.garena.game.freefire",
        "com.pubg.imobile",
    )
    val ENTERTAINMENT = setOf(
        "com.google.android.youtube",
        "com.netflix.mediaclient",
        "com.amazon.avod.thirdpartyclient",
        "in.startv.hotstar",
        "com.hotstar.android",
        "com.sony.liv.app",
    )
}
