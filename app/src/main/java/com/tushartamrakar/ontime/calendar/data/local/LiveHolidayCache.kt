package com.tushartamrakar.ontime.calendar.data.local

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.ExponentialBackOff
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.CalendarScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.YearMonth
import java.util.Arrays
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class LiveHoliday(
    val date: String,
    val name: String,
    val localName: String,
    val countryCode: String = "IN",
    val source: String = "nager",
)

@Singleton
class LiveHolidayCache @Inject constructor(
    private val context: Context,
) {
    companion object {
        private const val TAG = "LiveHolidayCache"
        private const val NAGER_BASE = "https://date.nager.at/api/v3/PublicHolidays"
        private const val INDIAN_CALENDAR    = "en.indian#holiday@group.v.calendar.google.com"
        private const val ISLAMIC_CALENDAR   = "en.islamic#holiday@group.v.calendar.google.com"
        private const val CHRISTIAN_CALENDAR = "en.christian#holiday@group.v.calendar.google.com"
    }

    // Mutex to prevent concurrent fetches of same month
    private val fetchMutex = Mutex()

    // Simple separate caches — no getOrPut with suspend
    private val nagerCache  = HashMap<String, List<LiveHoliday>>() // key: "IN_2026"
    private val googleCache = HashMap<String, List<LiveHoliday>>() // key: "IN_2026_5"
    private val finalCache  = HashMap<String, List<LiveHoliday>>() // key: "2026_5"

    // ── Year-level search cache (bypasses per-month mutex for fast parallel search) ──
    // ConcurrentHashMap allows lock-free reads and thread-safe writes across parallel coroutines.
    private val yearSearchCache = ConcurrentHashMap<Int, List<LiveHoliday>>()

    // Stop retrying Google after NEED_REMOTE_CONSENT — user must sign out + in
    @Volatile private var googleConsentDenied = false

    // ─── Disk cache (SharedPreferences) ───────────────────────────────────────
    // Persists holiday data across app restarts and process kills.
    // TTL = 24 hours per month — refreshes once a day when online.
    private val prefs: SharedPreferences =
        context.getSharedPreferences("ontime_holiday_cache_v1", Context.MODE_PRIVATE)
    private val CACHE_TTL_MS = 24 * 60 * 60 * 1000L // 24 hours

    // ─── Connectivity ─────────────────────────────────────────────────────────
    fun isOnline(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) { false }
    }

    fun hasCalendarPermission(): Boolean {
        return try {
            val account = GoogleSignIn.getLastSignedInAccount(context) ?: return false
            val grantedScopes = account.grantedScopes ?: return false
            grantedScopes.any { it.scopeUri.contains("calendar") }
        } catch (e: Exception) { false }
    }

    fun isSignedIn(): Boolean = try {
        GoogleSignIn.getLastSignedInAccount(context) != null
    } catch (e: Exception) { false }

    // ─── Main entry: get holidays for a month ─────────────────────────────────
    suspend fun getHolidaysForMonth(yearMonth: YearMonth): List<LiveHoliday> {
        if (!isOnline()) {
            // Offline: return cached result or fixed-date fallback (never empty)
            val finalKey = "${yearMonth.year}_${yearMonth.monthValue}"
            finalCache[finalKey]?.let {
                Log.d(TAG, "Offline cache hit for $yearMonth → ${it.size} holidays")
                return it
            }
            Log.d(TAG, "Offline — returning fixed-date fallback for $yearMonth")
            val monthStr = yearMonth.monthValue.toString().padStart(2, '0')
            return getFallbackHolidays(yearMonth.year, "IN")
                .filter { it.date.startsWith("${yearMonth.year}-$monthStr") }
        }

        val finalKey = "${yearMonth.year}_${yearMonth.monthValue}"

        // Return cached final result if available — check BEFORE acquiring mutex
        finalCache[finalKey]?.let {
            Log.d(TAG, "Cache hit for $yearMonth → ${it.size} holidays")
            return it
        }

        // Check disk cache before acquiring mutex (cheap read)
        loadMonthFromDisk(finalKey)?.let {
            finalCache[finalKey] = it // warm in-memory cache from disk
            Log.d(TAG, "Disk cache hit for $yearMonth → ${it.size} holidays")
            return it
        }

        // Use mutex to prevent same month being fetched concurrently
        return fetchMutex.withLock {
            // Double-check after acquiring lock (another coroutine may have fetched)
            finalCache[finalKey]?.let { return@withLock it }
            loadMonthFromDisk(finalKey)?.let {
                finalCache[finalKey] = it
                return@withLock it
            }
            fetchHolidaysInternal(yearMonth, finalKey)
        }
    }

    private suspend fun fetchHolidaysInternal(yearMonth: YearMonth, finalKey: String): List<LiveHoliday> {
        @Suppress("NAME_SHADOWING")

        val results = mutableListOf<LiveHoliday>()

        // ── Layer 1: Nager API (always, no sign-in) ───────────────────────────
        val nagerKey = "IN_${yearMonth.year}"
        if (nagerCache[nagerKey] == null) {
            val fetched = fetchNager(yearMonth.year, "IN")
            nagerCache[nagerKey] = fetched
        }
        val nagerMonthHolidays = nagerCache[nagerKey]!!.filter {
            it.date.startsWith(
                "${yearMonth.year}-${yearMonth.monthValue.toString().padStart(2, '0')}"
            )
        }
        results.addAll(nagerMonthHolidays)
        Log.d(TAG, "Nager → ${nagerMonthHolidays.size} holidays for $yearMonth")

        // ── Layer 2: Google API (only if signed in + consent granted) ────────────
        val googleKey = "IN_${yearMonth.year}_${yearMonth.monthValue}"
        if (isSignedIn() && !googleConsentDenied && googleCache[googleKey] == null) {
            val account = GoogleSignIn.getLastSignedInAccount(context)
            if (account != null) {
                val googleHolidays = mutableListOf<LiveHoliday>()
                listOf(INDIAN_CALENDAR, ISLAMIC_CALENDAR, CHRISTIAN_CALENDAR).forEach { calId ->
                    googleHolidays.addAll(fetchGoogle(account, calId, yearMonth))
                }
                // KEY FIX: Only cache if consent wasn't denied during this fetch.
                // If denied, leave googleCache[googleKey] = null so it retries after sign-in.
                if (!googleConsentDenied) {
                    googleCache[googleKey] = googleHolidays
                } else {
                    Log.d(TAG, "Google consent denied — NOT caching, will retry after sign-in")
                }
            }
        }
        if (googleConsentDenied) {
            Log.d(TAG, "Google skipped — consent not granted (sign out + sign in to fix)")
        }
        googleCache[googleKey]?.let {
            results.addAll(it)
            Log.d(TAG, "Google → ${it.size} holidays for $yearMonth")
        }

        // ── Merge + deduplicate ───────────────────────────────────────────────
        val merged = results
            .sortedByDescending { if (it.source == "google") 1 else 0 }
            .distinctBy { "${it.date}_${it.name.lowercase().trim()}" }
            .sortedBy { it.date }

        finalCache[finalKey] = merged
        saveMonthToDisk(finalKey, merged) // persist to survive app restarts
        Log.d(TAG, "✅ Final: ${merged.size} holidays for $yearMonth")
        return merged
    }

    // ─── Nager.Date — fetch whole year ────────────────────────────────────────
    private suspend fun fetchNager(year: Int, countryCode: String): List<LiveHoliday> =
        withContext(Dispatchers.IO) {
            // Retry up to 3 times before using hardcoded fallback
            for (attempt in 1..3) {
            var connection: HttpsURLConnection? = null
            try {
                val url = "$NAGER_BASE/$year/$countryCode"
                Log.d(TAG, "Fetching Nager: $url")
                connection = (URL(url).openConnection() as HttpsURLConnection).apply {
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Android)")
                    connectTimeout = 15000
                    readTimeout = 15000
                    instanceFollowRedirects = true
                    doInput = true
                }
                val responseCode = connection.responseCode
                Log.d(TAG, "Nager HTTP $responseCode for $year/$countryCode (attempt $attempt)")
                if (responseCode == 204) {
                    // 204 = No Content — Nager has no data for this year/country.
                    // This is definitive, NOT a transient error. Never retry.
                    Log.d(TAG, "Nager 204 (no data) for $year/$countryCode — skipping retries")
                    return@withContext getFallbackHolidays(year, countryCode)
                }
                if (responseCode != 200) {
                    Log.w(TAG, "Nager HTTP $responseCode attempt $attempt — will retry")
                    if (attempt < 3) { delay(2000L * attempt); continue }
                    Log.e(TAG, "❌ Nager failed after 3 attempts — using fallback")
                    return@withContext getFallbackHolidays(year, countryCode)
                }
                val text = BufferedReader(InputStreamReader(connection.inputStream))
                    .use { it.readText() }
                Log.d(TAG, "Nager response length: ${text.length} chars")
                if (text.isBlank()) {
                    Log.e(TAG, "❌ Nager empty body — using fallback")
                    return@withContext getFallbackHolidays(year, countryCode)
                }
                val json = JSONArray(text)
                val list = mutableListOf<LiveHoliday>()
                for (i in 0 until json.length()) {
                    val obj = json.getJSONObject(i)
                    list.add(LiveHoliday(
                        date = obj.getString("date"),
                        name = obj.getString("name"),
                        localName = obj.optString("localName", obj.getString("name")),
                        countryCode = countryCode,
                        source = "nager",
                    ))
                }
                Log.d(TAG, "✅ Nager: ${list.size} holidays for $year/$countryCode")
                list
            } catch (e: Exception) {
                Log.w(TAG, "❌ Nager exception attempt $attempt: ${e.javaClass.simpleName}: ${e.message}")
                if (attempt < 3) { delay(2000L * attempt); continue }
            } finally {
                connection?.disconnect()
            }
            } // end retry loop
            Log.e(TAG, "❌ Nager failed all 3 attempts — using fallback")
            getFallbackHolidays(year, countryCode)
        }

    // ─── Fixed-date fallback — works for ANY year, 2026 to 2099 ─────────────────
    // Contains ONLY government-recognized holidays with FIXED calendar dates.
    // Variable/lunar holidays (Diwali, Holi, Eid etc) are intentionally excluded.
    // Google Calendar API provides those with exact dates when user connects.
    private fun getFallbackHolidays(year: Int, countryCode: String): List<LiveHoliday> {
        if (countryCode != "IN") return emptyList()
        Log.d(TAG, "Using fixed-date fallback holidays for $year/IN")
        return listOf(
            // ── JANUARY ──────────────────────────────────────────────────────
            LiveHoliday("$year-01-01", "New Year's Day", "नव वर्ष"),
            LiveHoliday("$year-01-07", "Orthodox Christmas Day", "ऑर्थोडॉक्स क्रिसमस"),
            LiveHoliday("$year-01-12", "National Youth Day", "राष्ट्रीय युवा दिवस"),
            LiveHoliday("$year-01-14", "Makar Sankranti / Pongal / Uttarayan / Bihu", "मकर संक्रांति"),
            LiveHoliday("$year-01-23", "Parakram Diwas (Netaji Jayanti)", "पराक्रम दिवस"),
            LiveHoliday("$year-01-26", "Republic Day", "गणतंत्र दिवस"),
            // ── FEBRUARY ─────────────────────────────────────────────────────
            LiveHoliday("$year-02-28", "National Science Day", "राष्ट्रीय विज्ञान दिवस"),
            // ── MARCH ────────────────────────────────────────────────────────
            LiveHoliday("$year-03-23", "Shaheed Diwas", "शहीद दिवस"),
            // ── APRIL ────────────────────────────────────────────────────────
            LiveHoliday("$year-04-13", "Baisakhi / Vishu / Puthandu / Bihu", "बैसाखी"),
            LiveHoliday("$year-04-14", "Dr. B.R. Ambedkar Jayanti", "डॉ. अम्बेडकर जयंती"),
            // ── MAY ──────────────────────────────────────────────────────────
            LiveHoliday("$year-05-01", "International Labour Day", "मजदूर दिवस"),
            // ── JUNE ─────────────────────────────────────────────────────────
            LiveHoliday("$year-06-05", "World Environment Day", "विश्व पर्यावरण दिवस"),
            LiveHoliday("$year-06-21", "International Yoga Day", "अंतर्राष्ट्रीय योग दिवस"),
            // ── JULY ─────────────────────────────────────────────────────────
            LiveHoliday("$year-07-26", "Kargil Vijay Diwas", "कारगिल विजय दिवस"),
            // ── AUGUST ───────────────────────────────────────────────────────
            LiveHoliday("$year-08-09", "Quit India Movement Day", "भारत छोड़ो दिवस"),
            LiveHoliday("$year-08-15", "Independence Day", "स्वतंत्रता दिवस"),
            // ── SEPTEMBER ────────────────────────────────────────────────────
            LiveHoliday("$year-09-05", "Teachers' Day", "शिक्षक दिवस"),
            LiveHoliday("$year-09-14", "Hindi Diwas", "हिन्दी दिवस"),
            // ── OCTOBER ──────────────────────────────────────────────────────
            LiveHoliday("$year-10-02", "Gandhi Jayanti", "गांधी जयंती"),
            LiveHoliday("$year-10-31", "National Unity Day (Sardar Patel Jayanti)", "राष्ट्रीय एकता दिवस"),
            // ── NOVEMBER ─────────────────────────────────────────────────────
            LiveHoliday("$year-11-14", "Children's Day", "बाल दिवस"),
            LiveHoliday("$year-11-19", "National Integration Day (Indira Gandhi Jayanti)", "राष्ट्रीय एकीकरण दिवस"),
            // ── DECEMBER ─────────────────────────────────────────────────────
            LiveHoliday("$year-12-06", "Mahaparinirvan Diwas (Dr. Ambedkar)", "महापरिनिर्वाण दिवस"),
            LiveHoliday("$year-12-16", "Vijay Diwas", "विजय दिवस"),
            LiveHoliday("$year-12-22", "National Mathematics Day (Ramanujan Jayanti)", "राष्ट्रीय गणित दिवस"),
            LiveHoliday("$year-12-25", "Christmas Day", "क्रिसमस"),
            LiveHoliday("$year-12-26", "Boxing Day", "बॉक्सिंग डे"),
        )
    }

    // ─── Google Calendar API — fetch one month ────────────────────────────────
    private suspend fun fetchGoogle(
        account: com.google.android.gms.auth.api.signin.GoogleSignInAccount,
        calendarId: String,
        yearMonth: YearMonth,
    ): List<LiveHoliday> = withContext(Dispatchers.IO) {
        try {
            val credential = GoogleAccountCredential.usingOAuth2(
                context, Arrays.asList(CalendarScopes.CALENDAR_READONLY)
            ).apply {
                backOff = ExponentialBackOff()
                selectedAccount = account.account
            }
            val service = Calendar.Builder(
                NetHttpTransport(), GsonFactory.getDefaultInstance(), credential
            ).setApplicationName("Ontime").build()

            val mm = yearMonth.monthValue.toString().padStart(2, '0')
            val lastDay = yearMonth.atEndOfMonth().dayOfMonth
            val timeMin = com.google.api.client.util.DateTime(
                "${yearMonth.year}-${mm}-01T00:00:00Z"
            )
            val timeMax = com.google.api.client.util.DateTime(
                "${yearMonth.year}-${mm}-${lastDay}T23:59:59Z"
            )

            val events = service.events().list(calendarId)
                .setTimeMin(timeMin).setTimeMax(timeMax)
                .setSingleEvents(true).setMaxResults(100)
                .execute()

            val list = mutableListOf<LiveHoliday>()
            events.items?.forEach { event ->
                val date = event.start?.date?.toStringRfc3339()?.take(10)
                    ?: event.start?.dateTime?.toStringRfc3339()?.take(10) ?: return@forEach
                val name = event.summary ?: return@forEach
                list.add(LiveHoliday(
                    date = date, name = name, localName = name,
                    countryCode = "IN", source = "google",
                ))
            }
            Log.d(TAG, "✅ Google: ${list.size} from $calendarId for $yearMonth")
            list
        } catch (e: Exception) {
            val msg = e.message ?: ""
            // ⚠️ KEY FIX: NEED_REMOTE_CONSENT exception has an EMPTY message!
            // The string "NEED_REMOTE_CONSENT" appears only in [GoogleAuthUtil] log, NOT in e.message.
            // Must check exception CLASS NAME: UserRecoverableAuthIOException / UserRecoverableAuthException
            val exceptionChain = generateSequence(e as Throwable) { it.cause }
            val isConsentIssue = exceptionChain.any { ex ->
                ex.javaClass.name.contains("UserRecoverable") ||
                (ex.message?.contains("NEED_REMOTE_CONSENT") == true) ||
                (ex.message?.contains("NeedPermission") == true)
            } || (msg.isBlank() && isSignedIn()) // blank msg + signed in = very likely consent
            if (isConsentIssue) {
                googleConsentDenied = true
                Log.w(TAG, "Google consent denied (${e.javaClass.simpleName}) — retries stopped. Sign out + sign in to fix.")
            } else {
                Log.e(TAG, "❌ Google failed for $calendarId: ${e.javaClass.simpleName}: $msg")
            }
            emptyList()
        }
    }

    // ─── Fast year-level fetch for search — ONE API call per year ────────────
    /**
     * Gets all holidays for an entire year in a single Nager API call.
     * Used exclusively by search to avoid the per-month mutex serialisation.
     *
     * Priority: yearSearchCache → nagerCache → disk (all 12 months) → Nager API
     * No mutex — ConcurrentHashMap handles thread safety.
     * Multiple concurrent calls for DIFFERENT years run fully in parallel.
     * Duplicate calls for the SAME year are safe (last write wins, no data loss).
     */
    suspend fun getHolidaysForYearSearch(year: Int): List<LiveHoliday> {
        // 1. Fastest: already cached from a previous search
        yearSearchCache[year]?.let { return it }

        // 2. Already fetched by normal calendar browsing this session
        nagerCache["IN_$year"]?.let { holidays ->
            yearSearchCache[year] = holidays
            return holidays
        }

        // 3. Disk cache — check all 12 months
        val fromDisk = mutableListOf<LiveHoliday>()
        var allMonthsCached = true
        for (month in 1..12) {
            val monthData = loadMonthFromDisk("${year}_$month")
            if (monthData != null) {
                fromDisk.addAll(monthData)
            } else {
                allMonthsCached = false
                break
            }
        }
        if (allMonthsCached && fromDisk.isNotEmpty()) {
            val sorted = fromDisk
                .distinctBy { "${it.date}_${it.name.lowercase().trim()}" }
                .sortedBy { it.date }
            yearSearchCache[year] = sorted
            nagerCache["IN_$year"] = sorted  // warm the month cache too
            Log.d(TAG, "Disk→Search: $year → ${sorted.size} holidays")
            return sorted
        }

        // 4. Network: ONE Nager call for the whole year (not 12 separate calls!)
        return try {
            val fetched = fetchNager(year, "IN")  // fetches all 12 months at once
            nagerCache["IN_$year"] = fetched

            // Write each month to disk for future fast access
            for (month in 1..12) {
                val monthStr = month.toString().padStart(2, '0')
                val monthHolidays = fetched.filter { it.date.startsWith("$year-$monthStr") }
                saveMonthToDisk("${year}_$month", monthHolidays)
            }

            yearSearchCache[year] = fetched
            Log.d(TAG, "API→Search: $year → ${fetched.size} holidays")
            fetched
        } catch (e: Exception) {
            Log.w(TAG, "Search fetch failed for $year: ${e.message}")
            emptyList()
        }
    }

    // ─── Preload upcoming months ──────────────────────────────────────────────
    suspend fun preloadMonths(startMonth: YearMonth, count: Int = 3) {
        if (!isOnline()) return
        for (i in 0 until count) {
            try { getHolidaysForMonth(startMonth.plusMonths(i.toLong())) }
            catch (e: Exception) { /* silent */ }
        }
    }

    // ─── Call this after successful Google sign-in ────────────────────────────
    // Clears stale 0-holiday caches so May/Jun/Jul re-fetch with new consent
    // ─── Manual full refresh — clears everything and reloads all months ───────
    // Returns total holiday count across all months loaded
    // onProgress called after each month: (monthsDone 1-24, runningTotal)
    suspend fun refreshAllMonths(
        year: Int,
        onProgress: (monthsDone: Int, runningTotal: Int) -> Unit = { _, _ -> },
    ): Int {
        clearCache()
        if (!isOnline()) {
            Log.d(TAG, "refreshAllMonths: offline — returning 0")
            return 0
        }
        var total = 0
        var monthsDone = 0
        // Load current year + next year (24 months)
        for (y in year..(year + 1)) {
            for (month in 1..12) {
                try {
                    val ym = YearMonth.of(y, month)
                    val holidays = getHolidaysForMonth(ym)
                    total += holidays.size
                    monthsDone++
                    onProgress(monthsDone, total)
                    Log.d(TAG, "Refreshed $ym → ${holidays.size} (running total: $total)")
                } catch (e: Exception) {
                    Log.e(TAG, "Refresh error $y-$month: ${e.message}")
                    monthsDone++
                    onProgress(monthsDone, total)
                }
            }
        }
        Log.d(TAG, "✅ Full refresh complete: $total holidays across 24 months")
        return total
    }

    fun onGoogleSignedIn() {
        googleCache.clear()
        finalCache.clear()
        googleConsentDenied = false
        clearDiskCache() // force re-fetch with Google data next open
        Log.d(TAG, "Google signed in — all caches cleared, holidays will reload fresh")
    }

    // ─── Disk persistence helpers ─────────────────────────────────────────────

    private fun saveMonthToDisk(finalKey: String, holidays: List<LiveHoliday>) {
        try {
            val array = JSONArray()
            holidays.forEach { h ->
                array.put(JSONObject().apply {
                    put("date",        h.date)
                    put("name",        h.name)
                    put("localName",   h.localName)
                    put("countryCode", h.countryCode)
                    put("source",      h.source)
                })
            }
            prefs.edit()
                .putString("h_$finalKey", array.toString())
                .putLong("h_ts_$finalKey", System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            Log.w(TAG, "Failed to save month to disk: ${e.message}")
        }
    }

    private fun loadMonthFromDisk(finalKey: String): List<LiveHoliday>? {
        val ts = prefs.getLong("h_ts_$finalKey", 0L)
        if (System.currentTimeMillis() - ts > CACHE_TTL_MS) return null // expired
        val json = prefs.getString("h_$finalKey", null) ?: return null
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                LiveHoliday(
                    date        = obj.getString("date"),
                    name        = obj.getString("name"),
                    localName   = obj.optString("localName", obj.getString("name")),
                    countryCode = obj.optString("countryCode", "IN"),
                    source      = obj.optString("source", "nager"),
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load month from disk: ${e.message}")
            null
        }
    }

    private fun clearDiskCache() {
        prefs.edit().clear().apply()
        Log.d(TAG, "Disk cache cleared")
    }

    // ─── Clear all caches ─────────────────────────────────────────────────────
    fun clearCache() {
        nagerCache.clear()
        googleCache.clear()
        finalCache.clear()
        yearSearchCache.clear()
        googleConsentDenied = false
        clearDiskCache()
        Log.d(TAG, "Cache cleared")
    }
}
