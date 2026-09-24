package com.arstudio.gameinfo

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/**
 * Finds OFFICIAL artwork for a game. Nothing is drawn or invented by the app.
 * Sources, tried in order, each accepted ONLY on an exact title match:
 *  1. Steam store            - artwork uploaded by the publisher
 *  2. Apple App Store        - the game's real app icon
 *  3. IGDB (needs free keys in ApiKeys.kt) - cover art, title AND release year must match
 * Every source is tried for every game regardless of its platform tags (a "Console" game can
 * still have a Steam or App Store listing), so coverage no longer depends on how the game was
 * tagged. If the exact title has no hit, a handful of common edition/subtitle suffixes
 * ("- Definitive Edition", "(Remastered)", etc.) are stripped and retried, since that is the
 * single biggest cause of an otherwise-real official icon not being found.
 * If nothing matches, [icons] returns an empty list and the UI shows "No icon".
 * Results are cached on the device so each game is looked up only once.
 */
object IconRepo {
    private lateinit var sp: SharedPreferences
    private val gate = Semaphore(2)
    private val memory = ConcurrentHashMap<String, List<String>>()
    private const val RETRY_MISS_MS = 7L * 24 * 60 * 60 * 1000
    private var igdbToken: String? = null
    private var igdbTokenExpiry = 0L

    fun init(ctx: Context) {
        if (!::sp.isInitialized) sp = ctx.applicationContext.getSharedPreferences("gameinfo_icons", Context.MODE_PRIVATE)
    }

    private fun norm(s: String) = s.lowercase().replace(Regex("[^a-z0-9]"), "")

    // Common edition/subtitle suffixes that make an otherwise-identical Steam/App Store/IGDB
    // listing fail an exact-title match. Stripped one at a time, outermost first.
    private val EDITION_SUFFIXES = listOf(
        Regex("(?i)\\s*[:\\-–]\\s*(the\\s+)?(complete|definitive|goty|game of the year|ultimate|deluxe|gold|enhanced|remastered|remake|director'?s cut|anniversary|special|extended|legendary|premium|standard|digital)\\s*(edition)?\\s*$"),
        Regex("(?i)\\s*\\((.*?)(edition|remaster(ed)?|goty)(.*?)\\)\\s*$")
    )

    /** The game's name plus, if a known edition/subtitle suffix is present, progressively stripped variants. */
    private fun candidateTitles(name: String): List<String> {
        val out = LinkedHashSet<String>()
        out.add(name)
        var cur = name
        var changed = true
        while (changed) {
            changed = false
            for (re in EDITION_SUFFIXES) {
                val stripped = re.replace(cur, "").trim()
                if (stripped.isNotBlank() && stripped != cur && out.add(stripped)) {
                    cur = stripped
                    changed = true
                }
            }
        }
        return out.toList()
    }

    private fun http(url: String, method: String = "GET", headers: Map<String, String> = emptyMap(), body: String? = null): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
            if (body != null) { conn.doOutput = true; conn.outputStream.use { it.write(body.toByteArray()) } }
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally { conn.disconnect() }
    }

    private fun steamOne(title: String): List<String>? {
        val q = URLEncoder.encode(title, "UTF-8")
        val items = JSONObject(http("https://store.steampowered.com/api/storesearch/?term=$q&l=english&cc=US")).optJSONArray("items") ?: return null
        for (i in 0 until items.length()) {
            val o = items.getJSONObject(i)
            if (o.optString("type") == "app" && norm(o.optString("name")) == norm(title)) {
                val id = o.getInt("id")
                return listOf(
                    "https://cdn.cloudflare.steamstatic.com/steam/apps/$id/library_600x900.jpg",
                    "https://cdn.cloudflare.steamstatic.com/steam/apps/$id/header.jpg")
            }
        }
        return null
    }

    // Tried for every game, regardless of platform tags: an exact-title match is safe even if
    // the game wasn't tagged "PC", and many "Console" entries do have a Steam listing.
    private fun steam(names: List<String>): List<String>? {
        for (n in names) { steamOne(n)?.let { return it } }
        return null
    }

    private fun appStoreOne(title: String): List<String>? {
        val q = URLEncoder.encode(title, "UTF-8")
        val items = JSONObject(http("https://itunes.apple.com/search?term=$q&entity=software&limit=10&country=us")).optJSONArray("results") ?: return null
        for (i in 0 until items.length()) {
            val o = items.getJSONObject(i)
            val genres = o.optJSONArray("genres")
            var isGame = false
            if (genres != null) for (j in 0 until genres.length()) if (genres.optString(j) == "Games") isGame = true
            val art = o.optString("artworkUrl512")
            if (isGame && art.isNotBlank() && norm(o.optString("trackName")) == norm(title)) return listOf(art)
        }
        return null
    }

    // Tried for every game, regardless of platform tags, for the same reason as steam() above.
    private fun appStore(names: List<String>): List<String>? {
        for (n in names) {
            appStoreOne(n)?.let { return it }
            delay(350) // Apple's search endpoint is rate limited
        }
        return null
    }

    private fun igdbAuth(): String {
        val now = System.currentTimeMillis()
        igdbToken?.let { if (now < igdbTokenExpiry) return it }
        val r = JSONObject(http(
            "https://id.twitch.tv/oauth2/token?client_id=${ApiKeys.IGDB_CLIENT_ID}&client_secret=${ApiKeys.IGDB_CLIENT_SECRET}&grant_type=client_credentials",
            method = "POST"))
        val t = r.getString("access_token")
        igdbToken = t
        igdbTokenExpiry = now + (r.optLong("expires_in", 3600L) - 300L) * 1000L
        return t
    }

    private fun igdbOne(title: String, year: Int): List<String>? {
        val safe = title.replace("\\", " ").replace("\"", " ")
        val res = http(
            "https://api.igdb.com/v4/games", "POST",
            mapOf("Client-ID" to ApiKeys.IGDB_CLIENT_ID, "Authorization" to "Bearer ${igdbAuth()}", "Accept" to "application/json"),
            "search \"$safe\"; fields name,first_release_date,cover.image_id; limit 15;")
        val arr = org.json.JSONArray(res)
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (norm(o.optString("name")) != norm(title)) continue
            val imageId = o.optJSONObject("cover")?.optString("image_id") ?: continue
            if (imageId.isBlank()) continue
            val secs = o.optLong("first_release_date", 0L)
            if (secs > 0) {
                val relYear = 1970 + (secs / 31556952L).toInt()
                if (kotlin.math.abs(relYear - year) > 1) continue // same title, different game/release
            }
            return listOf("https://images.igdb.com/igdb/image/upload/t_cover_big/$imageId.jpg")
        }
        return null
    }

    private fun igdb(names: List<String>, year: Int): List<String>? {
        for (n in names) { igdbOne(n, year)?.let { return it } }
        return null
    }

    /** null = couldn't check yet (offline/rate-limited, will retry). Empty list = checked, no official icon found. */
    suspend fun icons(game: Game): List<String>? {
        val key = game.name
        memory[key]?.let { return it }
        val tag = if (ApiKeys.igdbConfigured) "k" else "n"
        val saved = sp.getString("urls$tag:$key", null)
        if (saved != null) {
            if (saved.isNotEmpty()) { val l = saved.split("|"); memory[key] = l; return l }
            if (System.currentTimeMillis() - sp.getLong("missAt$tag:$key", 0L) < RETRY_MISS_MS) { memory[key] = emptyList(); return emptyList() }
        }
        return withContext(Dispatchers.IO) {
            gate.withPermit {
                var hadError = false
                var found: List<String>? = null
                val names = candidateTitles(game.name)
                runCatching { found = steam(names) }.onFailure { hadError = true }
                if (found == null) runCatching { found = appStore(names) }.onFailure { hadError = true }
                if (found == null && ApiKeys.igdbConfigured) runCatching { found = igdb(names, game.year) }.onFailure { hadError = true }
                val f = found
                when {
                    f != null -> {
                        sp.edit().putString("urls$tag:$key", f.joinToString("|")).apply()
                        memory[key] = f; f
                    }
                    hadError -> null
                    else -> {
                        sp.edit().putString("urls$tag:$key", "").putLong("missAt$tag:$key", System.currentTimeMillis()).apply()
                        memory[key] = emptyList(); emptyList()
                    }
                }
            }
        }
    }
}
