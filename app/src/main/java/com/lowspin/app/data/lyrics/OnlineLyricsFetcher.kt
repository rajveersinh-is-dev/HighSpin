package com.lowspin.app.data.lyrics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.abs

/**
 * Lightweight HTTP client for fetching synchronized and plain lyrics online
 * from LRCLIB (open-source lyrics database: https://lrclib.net) and direct URLs,
 * using standard Java HttpURLConnection without bulky third-party dependencies.
 */
object OnlineLyricsFetcher {

    private const val LRCLIB_BASE_URL = "https://lrclib.net/api"
    private const val USER_AGENT = "LowspinMusicPlayer/2.7.1 (https://github.com/lowspin/app)"
    private const val CONNECT_TIMEOUT_MS = 8_000
    private const val READ_TIMEOUT_MS = 10_000
    private const val MAX_RESPONSE_CHARS = 256 * 1024 // 256 KB safety limit

    suspend fun fetchLrc(
        title: String,
        artist: String,
        album: String = "",
        durationMs: Long = 0L
    ): String? = withContext(Dispatchers.IO) {
        val cleanTitle = sanitizeTitle(title)
        val cleanArtist = sanitizeArtist(artist)
        val durationSec = (durationMs / 1000L).toInt()

        // 1. Try exact match GET request
        val exactMatch = fetchExactMatch(cleanTitle, cleanArtist, album, durationSec)
        if (!exactMatch.isNullOrBlank()) {
            return@withContext exactMatch
        }

        // 2. If exact match fails and title was sanitized differently, try original title
        if (cleanTitle != title) {
            val originalMatch = fetchExactMatch(title.trim(), artist.trim(), album, durationSec)
            if (!originalMatch.isNullOrBlank()) {
                return@withContext originalMatch
            }
        }

        // 3. Fallback to search endpoint
        val searchMatch = searchLrclib(cleanTitle, cleanArtist, durationSec)
        if (!searchMatch.isNullOrBlank()) {
            return@withContext searchMatch
        }

        null
    }

    suspend fun fetchFromUrl(urlString: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val targetUrl = urlString.trim()
            if (!targetUrl.startsWith("http://", ignoreCase = true) &&
                !targetUrl.startsWith("https://", ignoreCase = true)
            ) {
                return@runCatching null
            }

            val connection = (URL(targetUrl).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "text/plain, application/json, */*")
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                connection.disconnect()
                return@runCatching null
            }

            val content = connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                readLimitedString(reader, MAX_RESPONSE_CHARS)
            }
            connection.disconnect()

            // If the URL returned a JSON object containing syncedLyrics or plainLyrics (like LRCLIB API URL)
            extractLyricsIfJson(content) ?: content.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun fetchExactMatch(
        title: String,
        artist: String,
        album: String,
        durationSec: Int
    ): String? = runCatching {
        val queryParams = StringBuilder("track_name=").append(encode(title))
            .append("&artist_name=").append(encode(artist))

        if (album.isNotBlank()) {
            queryParams.append("&album_name=").append(encode(album))
        }
        if (durationSec > 0) {
            queryParams.append("&duration=").append(durationSec)
        }

        val url = URL("$LRCLIB_BASE_URL/get?$queryParams")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
        }

        val code = connection.responseCode
        if (code !in 200..299) {
            connection.disconnect()
            return@runCatching null
        }

        val response = connection.inputStream.bufferedReader(Charsets.UTF_8).use {
            readLimitedString(it, MAX_RESPONSE_CHARS)
        }
        connection.disconnect()

        val json = JSONObject(response)
        val synced = json.optString("syncedLyrics").trim()
        if (synced.isNotBlank()) return@runCatching synced

        val plain = json.optString("plainLyrics").trim()
        if (plain.isNotBlank()) return@runCatching plain

        null
    }.getOrNull()

    private fun searchLrclib(
        title: String,
        artist: String,
        durationSec: Int
    ): String? = runCatching {
        val queryParams = StringBuilder("track_name=").append(encode(title))
        if (artist.isNotBlank()) {
            queryParams.append("&artist_name=").append(encode(artist))
        }

        val url = URL("$LRCLIB_BASE_URL/search?$queryParams")
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", "application/json")
        }

        val code = connection.responseCode
        if (code !in 200..299) {
            connection.disconnect()
            return@runCatching null
        }

        val response = connection.inputStream.bufferedReader(Charsets.UTF_8).use {
            readLimitedString(it, MAX_RESPONSE_CHARS)
        }
        connection.disconnect()

        val array = JSONArray(response)
        if (array.length() == 0) return@runCatching null

        // Pick best candidate:
        // Score = (hasSynced ? 100 : 0) - min(durationDiff, 60)
        var bestLyrics: String? = null
        var bestScore = Int.MIN_VALUE

        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val synced = item.optString("syncedLyrics").trim()
            val plain = item.optString("plainLyrics").trim()
            val lyricsContent = if (synced.isNotBlank()) synced else plain
            if (lyricsContent.isBlank()) continue

            val itemDuration = item.optInt("duration", 0)
            val durationDiff = if (durationSec > 0 && itemDuration > 0) {
                abs(durationSec - itemDuration)
            } else {
                30
            }

            var score = 0
            if (synced.isNotBlank()) score += 100
            score -= durationDiff.coerceAtMost(60)

            if (score > bestScore) {
                bestScore = score
                bestLyrics = lyricsContent
            }
        }

        bestLyrics
    }.getOrNull()

    private fun extractLyricsIfJson(content: String): String? {
        val trimmed = content.trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return null
        return runCatching {
            val json = JSONObject(trimmed)
            val synced = json.optString("syncedLyrics").trim()
            if (synced.isNotBlank()) return@runCatching synced
            val plain = json.optString("plainLyrics").trim()
            if (plain.isNotBlank()) return@runCatching plain
            val lyrics = json.optString("lyrics").trim()
            if (lyrics.isNotBlank()) return@runCatching lyrics
            null
        }.getOrNull()
    }

    private fun readLimitedString(reader: BufferedReader, limitChars: Int): String {
        val sb = StringBuilder()
        val buffer = CharArray(4096)
        var charsRead: Int
        var total = 0
        while (reader.read(buffer).also { charsRead = it } != -1) {
            val toAppend = minOf(charsRead, limitChars - total)
            if (toAppend > 0) {
                sb.append(buffer, 0, toAppend)
                total += toAppend
            }
            if (total >= limitChars) break
        }
        return sb.toString()
    }

    private fun encode(value: String): String = URLEncoder.encode(value.trim(), "UTF-8")

    private val TITLE_CLEAN_REGEX = Regex(
        "(?i)\\s*[\\[(](?:feat\\.?|ft\\.?|official|lyrics|video|audio|remastered|deluxe|bonus|version|acoustic|live)[^\\])]*[\\])]"
    )

    private fun sanitizeTitle(title: String): String {
        return title.replace(TITLE_CLEAN_REGEX, "").trim()
    }

    private fun sanitizeArtist(artist: String): String {
        return artist.split(Regex("[,&/]|\\s+feat\\.?\\s+|\\s+ft\\.?\\s+", RegexOption.IGNORE_CASE))
            .firstOrNull()?.trim() ?: artist.trim()
    }
}
