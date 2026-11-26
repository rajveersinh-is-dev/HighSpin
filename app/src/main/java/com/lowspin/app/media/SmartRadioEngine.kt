package com.lowspin.app.media

import com.lowspin.app.data.db.SongEntity
import kotlin.math.abs
import kotlin.math.min

/**
 * Generates harmonically compatible Smart Radio queues using Camelot wheel distance,
 * tempo (BPM) proximity (including half/double time), and artist diversity.
 */
object SmartRadioEngine {

    // Maps standard musical key names to Camelot Wheel (number 1..12, minor='A'/major='B')
    private val CAMELOT_MAP = mapOf(
        "A♭ Min" to (1 to 'A'), "G♯ Min" to (1 to 'A'), "B Maj" to (1 to 'B'),
        "E♭ Min" to (2 to 'A'), "D♯ Min" to (2 to 'A'), "F♯ Maj" to (2 to 'B'), "G♭ Maj" to (2 to 'B'),
        "B♭ Min" to (3 to 'A'), "A♯ Min" to (3 to 'A'), "D♭ Maj" to (3 to 'B'), "C♯ Maj" to (3 to 'B'),
        "F Min" to (4 to 'A'), "A♭ Maj" to (4 to 'B'),
        "C Min" to (5 to 'A'), "E♭ Maj" to (5 to 'B'),
        "G Min" to (6 to 'A'), "B♭ Maj" to (6 to 'B'),
        "D Min" to (7 to 'A'), "F Maj" to (7 to 'B'),
        "A Min" to (8 to 'A'), "C Maj" to (8 to 'B'),
        "E Min" to (9 to 'A'), "G Maj" to (9 to 'B'),
        "B Min" to (10 to 'A'), "D Maj" to (10 to 'B'),
        "F♯ Min" to (11 to 'A'), "G♭ Min" to (11 to 'A'), "A Maj" to (11 to 'B'),
        "C♯ Min" to (12 to 'A'), "D♭ Min" to (12 to 'A'), "E Maj" to (12 to 'B')
    )

    fun harmonicScore(keyA: String, keyB: String): Float {
        if (keyA.isBlank() || keyB.isBlank()) return 0.35f
        val camA = CAMELOT_MAP[keyA] ?: return 0.35f
        val camB = CAMELOT_MAP[keyB] ?: return 0.35f

        val rawDist = abs(camA.first - camB.first)
        val wheelDist = min(rawDist, 12 - rawDist)

        return when {
            wheelDist == 0 && camA.second == camB.second -> 1.0f // Exact key
            wheelDist == 0 && camA.second != camB.second -> 0.90f // Relative Maj/Min
            wheelDist == 1 && camA.second == camB.second -> 0.85f // Adjacent fifth
            wheelDist == 2 && camA.second == camB.second -> 0.55f // Energy boost/drop
            wheelDist <= 1 -> 0.45f
            else -> 0.15f
        }
    }

    fun tempoScore(bpmA: Int, bpmB: Int): Float {
        if (bpmA <= 0 || bpmB <= 0) return 0.35f
        val directDiff = abs(bpmA - bpmB)
        val halfDiff = abs(bpmA - bpmB * 2)
        val doubleDiff = abs(bpmA * 2 - bpmB)
        val minDiff = min(directDiff, min(halfDiff, doubleDiff))
        return (1.0f - (minDiff / 45.0f)).coerceIn(0f, 1f)
    }

    fun buildRadioQueue(
        seed: SongEntity,
        library: List<SongEntity>,
        limit: Int = 35
    ): List<SongEntity> {
        val candidates = library.filter { it.id != seed.id }
        if (candidates.isEmpty()) return listOf(seed)

        val scored = candidates.map { candidate ->
            val hScore = harmonicScore(seed.musicalKey, candidate.musicalKey)
            val tScore = tempoScore(seed.bpm, candidate.bpm)
            val durationAffinity = (1.0f - abs(seed.durationMs - candidate.durationMs) / 240_000f).coerceIn(0f, 1f)
            val sameArtistBonus = if (candidate.artist.equals(seed.artist, ignoreCase = true)) 0.08f else 0.12f
            val analyzedBonus = if (candidate.bpm > 0 && candidate.musicalKey.isNotBlank()) 0.10f else 0f

            val total = (hScore * 0.42f) + (tScore * 0.38f) + (durationAffinity * 0.08f) + sameArtistBonus + analyzedBonus
            candidate to total
        }.sortedByDescending { it.second }

        // Interleave slightly so no single artist dominates consecutive slots
        val remaining = scored.map { it.first }.toMutableList()
        val result = ArrayList<SongEntity>(min(limit, remaining.size) + 1)
        result.add(seed)

        var lastArtist = seed.artist
        while (result.size < limit + 1 && remaining.isNotEmpty()) {
            val pickIdx = remaining.indexOfFirst { !it.artist.equals(lastArtist, ignoreCase = true) }
                .takeIf { it >= 0 && it <= 3 }
                ?: 0
            val chosen = remaining.removeAt(pickIdx)
            result.add(chosen)
            lastArtist = chosen.artist
        }
        return result
    }
}
