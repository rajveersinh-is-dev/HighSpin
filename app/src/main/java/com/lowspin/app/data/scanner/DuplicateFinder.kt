package com.lowspin.app.data.scanner

import com.lowspin.app.data.db.SongEntity
import kotlin.math.abs

data class DuplicateGroup(
    val normalizedName: String,
    val songs: List<SongEntity>,
    val matchReason: String
)

/**
 * Detects duplicate or redundant local audio tracks using normalized filename/title
 * bigram similarity paired with strict duration matching (±2.5s).
 */
object DuplicateFinder {

    private const val MAX_DURATION_DIFF_MS = 2_500L
    private const val MIN_NAME_SIMILARITY = 0.84f

    private val NOISE_PARENS = Regex(
        "\\((?:[^)]*\\b(?:official|audio|video|lyrics?|hd|hq|remaster(?:ed)?|live|version|edit|mix)\\b[^)]*|\\d{1,3})\\)",
        RegexOption.IGNORE_CASE
    )
    private val NOISE_BRACKETS = Regex("\\[(.*?)\\]")
    private val EXTENSION_REGEX = Regex("\\.[a-z0-9]{2,5}$", RegexOption.IGNORE_CASE)
    private val COPY_SUFFIX_REGEX = Regex("(?:\\s*[-_]\\s*copy|\\s*\\(\\d+\\)|\\s+\\d+)$", RegexOption.IGNORE_CASE)
    private val NON_ALNUM = Regex("[^a-z0-9\\s]")
    private val MULTI_SPACE = Regex("\\s+")

    fun normalizeTitle(raw: String): String {
        var s = raw.trim().lowercase()
        s = EXTENSION_REGEX.replace(s, "")
        s = NOISE_PARENS.replace(s, "")
        s = NOISE_BRACKETS.replace(s, "")
        s = COPY_SUFFIX_REGEX.replace(s, "")
        s = NON_ALNUM.replace(s, " ")
        s = MULTI_SPACE.replace(s, " ").trim()
        return s
    }

    fun diceSimilarity(a: String, b: String): Float {
        if (a == b) return if (a.isEmpty()) 0f else 1f
        if (a.length < 2 || b.length < 2) return 0f

        val bigramsA = HashMap<String, Int>()
        for (i in 0 until a.length - 1) {
            val bg = a.substring(i, i + 2)
            bigramsA[bg] = (bigramsA[bg] ?: 0) + 1
        }
        val bigramsB = HashMap<String, Int>()
        for (i in 0 until b.length - 1) {
            val bg = b.substring(i, i + 2)
            bigramsB[bg] = (bigramsB[bg] ?: 0) + 1
        }

        var intersection = 0
        for ((bg, countA) in bigramsA) {
            val countB = bigramsB[bg] ?: 0
            intersection += minOf(countA, countB)
        }
        val total = (a.length - 1) + (b.length - 1)
        return (2f * intersection) / total
    }

    fun findDuplicates(songs: List<SongEntity>): List<DuplicateGroup> {
        if (songs.size < 2) return emptyList()

        val normNames = songs.map { song ->
            val fileStem = song.matchKey.substringAfterLast('/').takeIf { it.isNotBlank() } ?: song.title
            val titleNorm = normalizeTitle(song.title)
            val fileNorm = normalizeTitle(fileStem)
            if (titleNorm.length >= 2) titleNorm else fileNorm
        }

        val visited = BooleanArray(songs.size)
        val groups = ArrayList<DuplicateGroup>()

        for (i in songs.indices) {
            if (visited[i]) continue
            val nameA = normNames[i]
            if (nameA.length < 2) continue

            val cluster = ArrayList<SongEntity>()
            cluster.add(songs[i])
            var exactTitleMatch = true

            for (j in i + 1 until songs.size) {
                if (visited[j]) continue
                val durDiff = abs(songs[i].durationMs - songs[j].durationMs)
                if (durDiff > MAX_DURATION_DIFF_MS) continue

                val nameB = normNames[j]
                if (nameB.length < 2) continue

                val sim = diceSimilarity(nameA, nameB)
                if (sim >= MIN_NAME_SIMILARITY) {
                    visited[j] = true
                    cluster.add(songs[j])
                    if (nameA != nameB) exactTitleMatch = false
                }
            }

            if (cluster.size > 1) {
                visited[i] = true
                val reason = if (exactTitleMatch) {
                    "Identical title & matching duration"
                } else {
                    "Similar filename/title & matching duration"
                }
                groups.add(DuplicateGroup(normalizedName = nameA, songs = cluster, matchReason = reason))
            }
        }

        return groups.sortedByDescending { it.songs.size }
    }
}
