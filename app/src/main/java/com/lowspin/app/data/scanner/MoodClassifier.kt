package com.lowspin.app.data.scanner

import com.lowspin.app.data.db.SongEntity

enum class MoodCategory(
    val id: String,
    val label: String,
    val subtitle: String,
    val colorTopHex: Long,
    val colorBottomHex: Long
) {
    ENERGY("energy", "High Energy", "Fast & driving tracks (122+ BPM)", 0xFFFF512F, 0xFFDD2476),
    PARTY("party", "Upbeat & Party", "Danceable Major-key grooves", 0xFFF7971E, 0xFFFFD200),
    FOCUS("focus", "Deep Focus", "Steady mid-tempo flow (84–118 BPM)", 0xFF00B0FF, 0xFF2979FF),
    CALM("calm", "Late Night Calm", "Slow or mellow Minor-key atmosphere", 0xFF7B2CBF, 0xFF3A0CA3)
}

/**
 * Classifies local library tracks into moods using real detected BPM, musical key/mode,
 * and duration without external network tags.
 */
object MoodClassifier {

    fun classify(song: SongEntity): MoodCategory? {
        val bpm = song.bpm
        val isMinor = song.musicalKey.endsWith("Min", ignoreCase = true)
        val isMajor = song.musicalKey.endsWith("Maj", ignoreCase = true)

        if (bpm <= 0) return null

        return when {
            bpm >= 122 -> if (isMajor && bpm <= 132) MoodCategory.PARTY else MoodCategory.ENERGY
            bpm in 102..121 -> if (isMajor) MoodCategory.PARTY else MoodCategory.FOCUS
            bpm in 84..101 -> if (isMinor && song.durationMs < 210_000L) MoodCategory.CALM else MoodCategory.FOCUS
            else -> MoodCategory.CALM
        }
    }

    fun groupByMood(songs: List<SongEntity>): Map<MoodCategory, List<SongEntity>> {
        val grouped = linkedMapOf<MoodCategory, MutableList<SongEntity>>()
        MoodCategory.entries.forEach { grouped[it] = mutableListOf() }

        for (song in songs) {
            val mood = classify(song) ?: continue
            grouped[mood]?.add(song)
        }
        return grouped
    }
}
