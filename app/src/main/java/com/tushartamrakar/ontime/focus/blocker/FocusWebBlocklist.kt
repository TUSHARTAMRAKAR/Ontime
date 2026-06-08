package com.tushartamrakar.ontime.focus.blocker

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// ─── Categories ───────────────────────────────────────────────────────────────

enum class WebCategory(
    val key:         String,
    val displayName: String,
    val emoji:       String,
    val description: String,
    val domains:     Set<String>,
    val defaultOn:   Boolean = true,
) {
    SOCIAL_MEDIA(
        key         = "social_media",
        displayName = "Social Media",
        emoji       = "📱",
        description = "Instagram, Twitter, TikTok, Reddit & more",
        defaultOn   = true,
        domains     = setOf(
            "instagram.com", "twitter.com", "x.com", "facebook.com",
            "tiktok.com", "snapchat.com", "reddit.com", "pinterest.com",
            "tumblr.com", "discord.com", "threads.net", "bereal.com",
            "whatsapp.com", "telegram.org", "messenger.com",
        ),
    ),
    ENTERTAINMENT(
        key         = "entertainment",
        displayName = "Entertainment",
        emoji       = "🎬",
        description = "YouTube, Netflix, Twitch & streaming sites",
        defaultOn   = true,
        domains     = setOf(
            "youtube.com", "twitch.tv", "netflix.com", "hulu.com",
            "disneyplus.com", "primevideo.com", "max.com", "peacocktv.com",
            "vimeo.com", "dailymotion.com", "crunchyroll.com",
            "funimation.com", "curiositystream.com",
        ),
    ),
    GAMING(
        key         = "gaming",
        displayName = "Gaming",
        emoji       = "🎮",
        description = "Steam, Roblox, Epic Games & browser games",
        defaultOn   = false,
        domains     = setOf(
            "store.steampowered.com", "roblox.com", "epicgames.com",
            "miniclip.com", "poki.com", "addictinggames.com",
            "kongregate.com", "agame.com",
        ),
    ),
    NEWS(
        key         = "news",
        displayName = "News & Media",
        emoji       = "📰",
        description = "News sites that can pull you down a rabbit hole",
        defaultOn   = false,
        domains     = setOf(
            "news.google.com", "bbc.com", "cnn.com", "nytimes.com",
            "theguardian.com", "huffpost.com", "buzzfeed.com",
            "vice.com", "dailymail.co.uk", "foxnews.com",
        ),
    ),
}

// ─── FocusWebBlocklist ────────────────────────────────────────────────────────

/**
 * Manages the focus-mode website blocklist.
 *
 * Two layers of blocking:
 *  1. Category blocks (Social Media, Entertainment, Gaming, News) — preset domain lists
 *  2. Custom domains — user-added sites specific to their distractions
 *
 * Data is stored in SharedPreferences so the VPN service can read it
 * without going through Hilt (important — VPN reads during DNS loop).
 *
 * Only blocks during active focus sessions.
 * AdultDomainBlocklist always blocks, regardless of session state.
 */
@Singleton
class FocusWebBlocklist @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val PREFS_NAME               = "ontime_focus_web_blocklist"
        private const val KEY_ENABLED              = "web_blocking_enabled"
        private const val KEY_ENABLED_CATEGORIES   = "enabled_categories"
        private const val KEY_CUSTOM_DOMAINS       = "custom_domains"
        private const val TAG                      = "FocusWebBlocklist"
    }

    private val prefs get() = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Global toggle ─────────────────────────────────────────────────────────

    var isEnabled: Boolean
        get()  = prefs.getBoolean(KEY_ENABLED, false)
        set(v) = prefs.edit().putBoolean(KEY_ENABLED, v).apply()

    // ── Category management ───────────────────────────────────────────────────

    fun getEnabledCategories(): Set<String> {
        val defaults = WebCategory.values()
            .filter { it.defaultOn }
            .map { it.key }
            .toSet()
        return prefs.getStringSet(KEY_ENABLED_CATEGORIES, defaults) ?: defaults
    }

    fun isCategoryEnabled(category: WebCategory): Boolean =
        category.key in getEnabledCategories()

    fun setCategoryEnabled(category: WebCategory, enabled: Boolean) {
        val current = getEnabledCategories().toMutableSet()
        if (enabled) current.add(category.key) else current.remove(category.key)
        prefs.edit().putStringSet(KEY_ENABLED_CATEGORIES, current).apply()
    }

    // ── Custom domains ────────────────────────────────────────────────────────

    fun getCustomDomains(): Set<String> =
        prefs.getStringSet(KEY_CUSTOM_DOMAINS, emptySet()) ?: emptySet()

    fun addCustomDomain(domain: String) {
        val clean   = domain.trim().lowercase().removePrefix("www.")
            .removePrefix("https://").removePrefix("http://").trimEnd('/')
        if (clean.isBlank() || !clean.contains('.')) return
        val current = getCustomDomains().toMutableSet()
        current.add(clean)
        prefs.edit().putStringSet(KEY_CUSTOM_DOMAINS, current).apply()
        Log.d(TAG, "Added custom domain: $clean")
    }

    fun removeCustomDomain(domain: String) {
        val current = getCustomDomains().toMutableSet()
        current.remove(domain)
        prefs.edit().putStringSet(KEY_CUSTOM_DOMAINS, current).apply()
    }

    // ── Domain lookup (called from VPN DNS loop) ──────────────────────────────

    /**
     * Returns true if [domain] should be blocked during a focus session.
     * O(1) lookup — safe to call from hot DNS proxy loop.
     */
    fun isBlockedDuringFocus(domain: String): Boolean {
        if (!isEnabled) return false
        val lower = domain.lowercase().trimEnd('.')

        // Check custom domains first
        if (isInSet(lower, getCustomDomains())) return true

        // Check enabled categories
        val enabledCats = getEnabledCategories()
        for (cat in WebCategory.values()) {
            if (cat.key !in enabledCats) continue
            if (isInSet(lower, cat.domains)) return true
        }
        return false
    }

    /** Checks domain and all parent domains against a set. */
    private fun isInSet(domain: String, set: Set<String>): Boolean {
        if (set.contains(domain)) return true
        var dotIdx = domain.indexOf('.')
        while (dotIdx != -1) {
            val parent = domain.substring(dotIdx + 1)
            if (set.contains(parent)) return true
            dotIdx = domain.indexOf('.', dotIdx + 1)
        }
        return false
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    /** Total number of domains blocked across enabled categories + custom. */
    fun blockedDomainCount(): Int {
        val catCount = WebCategory.values()
            .filter { isCategoryEnabled(it) }
            .sumOf { it.domains.size }
        return catCount + getCustomDomains().size
    }
}
