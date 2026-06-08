package com.tushartamrakar.ontime.focus.blocker

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads and holds the adult content domain blocklist.
 *
 * Source: assets/adult_blocklist.txt — one domain per line.
 * Stored as a HashSet for O(1) lookup.
 *
 * HOW TO GET THE FULL BLOCKLIST
 * ─────────────────────────────
 * 1. Download Steven Black's porn-only hosts file:
 *    https://raw.githubusercontent.com/StevenBlack/hosts/master/alternates/porn/hosts
 * 2. Extract only the domain lines (the ones starting with "0.0.0.0")
 * 3. Strip "0.0.0.0 " prefix — keep only the domain names
 * 4. Save to app/src/main/assets/adult_blocklist.txt
 *
 * The bundled version below contains common domains as a starter set.
 * The full Steven Black list has ~50,000+ entries.
 *
 * Called once by AdultContentVpnService on startup. After that,
 * all lookups are O(1) in-memory HashSet operations.
 */
@Singleton
class AdultDomainBlocklist @Inject constructor() {

    private val tag = "AdultBlocklist"
    private val blocklist = HashSet<String>(65536)  // pre-size for ~50k entries
    private var loaded = false

    /**
     * Load the blocklist from assets. Safe to call multiple times —
     * only loads once. Should be called from a background coroutine.
     */
    suspend fun loadIfNeeded(context: Context) {
        if (loaded) return
        withContext(Dispatchers.IO) {
            var count = 0
            // Try loading from bundled asset file first
            try {
                context.assets.open("adult_blocklist.txt").bufferedReader().use { reader ->
                    reader.forEachLine { line ->
                        val domain = line.trim().lowercase()
                        if (domain.isNotBlank() && !domain.startsWith("#")) {
                            // Handle both plain domain and "0.0.0.0 domain.com" format
                            val clean = domain.removePrefix("0.0.0.0 ").trim()
                            if (clean.isNotBlank() && clean.contains('.')) {
                                blocklist.add(clean)
                                count++
                            }
                        }
                    }
                }
                Log.d(tag, "Loaded $count domains from assets/adult_blocklist.txt")
            } catch (e: Exception) {
                Log.w(tag, "adult_blocklist.txt not found — using built-in list")
                loadBuiltInList()
                count = blocklist.size
            }
            loaded = true
            Log.d(tag, "Blocklist ready: ${blocklist.size} domains total")
        }
    }

    /**
     * Returns true if the domain (or any parent domain) is in the blocklist.
     * e.g. "images.example-adult-site.com" → checks:
     *   1. "images.example-adult-site.com"
     *   2. "example-adult-site.com"
     */
    fun isBlocked(domain: String): Boolean {
        if (!loaded) return false
        val lower = domain.lowercase().trimEnd('.')
        if (blocklist.contains(lower)) return true
        // Check parent domains
        var dotIdx = lower.indexOf('.')
        while (dotIdx != -1) {
            val parent = lower.substring(dotIdx + 1)
            if (blocklist.contains(parent)) return true
            dotIdx = lower.indexOf('.', dotIdx + 1)
        }
        return false
    }

    fun size(): Int = blocklist.size

    /**
     * Built-in starter list — common adult content domains.
     * The real protection comes from the full asset file (~50k domains).
     * Add assets/adult_blocklist.txt using Steven Black's list for full coverage.
     */
    private fun loadBuiltInList() {
        val domains = listOf(
            "pornhub.com", "xvideos.com", "xnxx.com", "redtube.com", "youporn.com",
            "tube8.com", "spankbang.com", "xhamster.com", "brazzers.com", "naughtyamerica.com",
            "onlyfans.com", "fapello.com", "erome.com", "bongacams.com", "stripchat.com",
            "chaturbate.com", "livejasmin.com", "camsoda.com", "myfreecams.com", "cam4.com",
            "rule34.xxx", "e621.net", "gelbooru.com", "nhentai.net", "hentaihaven.xxx",
            "mangahentai.com", "hanime.tv", "hentai2read.com", "hentaisea.com",
            "pornmd.com", "drtuber.com", "youjizz.com", "xmoviesforyou.com", "hclips.com",
            "eporner.com", "beeg.com", "tnaflix.com", "porntrex.com", "txxx.com",
            "freeones.com", "iafd.com", "adultdvdempire.com", "gamelink.com",
        )
        blocklist.addAll(domains)
    }
}
