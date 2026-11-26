package com.lowspin.app.media

import com.lowspin.app.data.db.SongEntity
import com.lowspin.app.data.scanner.AudioAnalyzer
import com.lowspin.app.data.scanner.DuplicateFinder
import com.lowspin.app.data.scanner.MoodCategory
import com.lowspin.app.data.scanner.MoodClassifier
import com.lowspin.app.ui.screens.decades.groupSongsByDecade
import com.lowspin.app.ui.settings.AppearanceSettings
import com.lowspin.app.ui.settings.SwipeAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class NewFeaturesTest {

    private fun testSong(
        id: Long,
        title: String,
        artist: String = "Artist $id",
        album: String = "Album $id",
        durationMs: Long = 200_000L,
        year: Int = 2020,
        bpm: Int = 0,
        musicalKey: String = "",
        matchKey: String = "/music/$title.mp3"
    ) = SongEntity(
        id = id,
        title = title,
        artist = artist,
        artistId = id,
        album = album,
        albumId = id,
        durationMs = durationMs,
        trackNumber = 1,
        discNumber = 1,
        year = year,
        folderPath = "/music",
        folderName = "Music",
        contentUri = "content://media/external/audio/media/$id",
        mimeType = "audio/mpeg",
        dateAdded = 1000L,
        dateModified = 1000L,
        matchKey = matchKey,
        bpm = bpm,
        musicalKey = musicalKey
    )

    @Test
    fun audioAnalyzer_detectBpm_recognizesSynthesizedBeat() {
        val sampleRate = 22050
        val seconds = 8
        val samples = FloatArray(sampleRate * seconds)
        // 120 BPM = beat every 0.50 seconds
        val beatIntervalSamples = (sampleRate * 0.50).toInt()
        for (i in samples.indices) {
            val posInBeat = i % beatIntervalSamples
            val env = exp(-posInBeat / (sampleRate * 0.04))
            samples[i] = (env * sin(2.0 * PI * 110.0 * i / sampleRate)).toFloat()
        }

        val bpm = AudioAnalyzer.detectBpm(samples, sampleRate)
        assertTrue("Expected ~120 BPM, got $bpm", bpm in 116..124)

        // Silence returns 0
        assertEquals(0, AudioAnalyzer.detectBpm(FloatArray(sampleRate * 4), sampleRate))
    }

    @Test
    fun audioAnalyzer_detectKey_recognizesSynthesizedCMajorAndAMinorTriads() {
        val sampleRate = 22050
        val samplesCMaj = FloatArray(sampleRate * 4)
        // C4 (261.63 Hz), E4 (329.63 Hz), G4 (392.00 Hz)
        for (i in samplesCMaj.indices) {
            val t = i.toDouble() / sampleRate
            samplesCMaj[i] = (
                0.45 * sin(2.0 * PI * 261.63 * t) +
                    0.35 * sin(2.0 * PI * 329.63 * t) +
                    0.35 * sin(2.0 * PI * 392.00 * t)
                ).toFloat()
        }
        assertEquals("C Maj", AudioAnalyzer.detectKey(samplesCMaj, sampleRate))

        val samplesAMin = FloatArray(sampleRate * 4)
        // A3 (220.0 Hz), C4 (261.63 Hz), E4 (329.63 Hz)
        for (i in samplesAMin.indices) {
            val t = i.toDouble() / sampleRate
            samplesAMin[i] = (
                0.45 * sin(2.0 * PI * 220.0 * t) +
                    0.35 * sin(2.0 * PI * 261.63 * t) +
                    0.35 * sin(2.0 * PI * 329.63 * t)
                ).toFloat()
        }
        assertEquals("A Min", AudioAnalyzer.detectKey(samplesAMin, sampleRate))
    }

    @Test
    fun moodClassifier_categorizesFromBpmAndKey() {
        val highEnergy = testSong(1, "Run", bpm = 148, musicalKey = "F Min")
        val party = testSong(2, "Dance", bpm = 124, musicalKey = "G Maj")
        val focus = testSong(3, "Study", bpm = 96, musicalKey = "D Maj", durationMs = 240_000L)
        val calm = testSong(4, "Sleep", bpm = 72, musicalKey = "A Min")
        val unanalyzed = testSong(5, "New", bpm = 0, musicalKey = "")

        assertEquals(MoodCategory.ENERGY, MoodClassifier.classify(highEnergy))
        assertEquals(MoodCategory.PARTY, MoodClassifier.classify(party))
        assertEquals(MoodCategory.FOCUS, MoodClassifier.classify(focus))
        assertEquals(MoodCategory.CALM, MoodClassifier.classify(calm))
        assertEquals(null, MoodClassifier.classify(unanalyzed))
    }

    @Test
    fun duplicateFinder_clustersMatchingTitleAndDuration() {
        val s1 = testSong(1, "Midnight City (Official Audio)", durationMs = 243_000L)
        val s2 = testSong(2, "Midnight City", durationMs = 243_800L, matchKey = "/downloads/Midnight City (1).flac")
        val s3 = testSong(3, "Midnight City", durationMs = 310_000L) // Different mix length
        val s4 = testSong(4, "Completely Different Song", durationMs = 243_000L)

        val groups = DuplicateFinder.findDuplicates(listOf(s1, s2, s3, s4))
        assertEquals(1, groups.size)
        assertEquals(setOf(1L, 2L), groups[0].songs.map { it.id }.toSet())
    }

    @Test
    fun smartRadioEngine_prioritizesHarmonicAndTempoMatches() {
        val seed = testSong(1, "Seed", artist = "Seed Artist", bpm = 120, musicalKey = "C Maj")
        val harmonicClose = testSong(2, "Close", artist = "Other", bpm = 122, musicalKey = "A Min")
        val clashFar = testSong(3, "Far", artist = "Third", bpm = 75, musicalKey = "F♯ Maj")

        assertEquals(0.90f, SmartRadioEngine.harmonicScore("C Maj", "A Min"), 0.01f)
        assertEquals(0.85f, SmartRadioEngine.harmonicScore("C Maj", "G Maj"), 0.01f)
        assertTrue(SmartRadioEngine.tempoScore(140, 70) > 0.95f) // Half/double time affinity

        val queue = SmartRadioEngine.buildRadioQueue(seed, listOf(seed, clashFar, harmonicClose))
        assertEquals(3, queue.size)
        assertEquals(seed.id, queue[0].id)
        assertEquals(harmonicClose.id, queue[1].id)
    }

    @Test
    fun decadeBrowser_groupsAndSortsDecadesProperly() {
        val s80 = testSong(1, "80s Hit", year = 1984)
        val s20 = testSong(2, "2020s Hit", year = 2023)
        val sOld = testSong(3, "Jazz", year = 1958)
        val sNone = testSong(4, "Unknown", year = 0)

        val buckets = groupSongsByDecade(listOf(s80, s20, sOld, sNone))
        assertEquals(4, buckets.size)
        assertEquals("2020s", buckets[0].label)
        assertEquals("1980s", buckets[1].label)
        assertEquals("Pre-1960s Classics", buckets[2].label)
        assertEquals("Undated Tracks", buckets[3].label)
    }

    @Test
    fun scheduledPlayback_nextTriggerRollsToTomorrowWhenPast() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 26, 9, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = cal.timeInMillis

        // 07:00 already passed -> next day 07:00
        val nextMorning = ScheduledPlaybackManager.computeNextTriggerMillis(7, 0, now)
        assertTrue(nextMorning > now)
        val outCal = Calendar.getInstance().apply { timeInMillis = nextMorning }
        assertEquals(27, outCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(7, outCal.get(Calendar.HOUR_OF_DAY))

        // 18:00 still upcoming today -> same day 18:00
        val evening = ScheduledPlaybackManager.computeNextTriggerMillis(18, 0, now)
        val evCal = Calendar.getInstance().apply { timeInMillis = evening }
        assertEquals(26, evCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(18, evCal.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun swipeActionsAndDefaultTabsIncludeRecents() {
        assertTrue(AppearanceSettings.DEFAULT_LIBRARY_TABS.contains("recents"))
        assertEquals(SwipeAction.SEEK_FORWARD_10, SwipeAction.fromId("seek_fwd_10"))
        assertEquals(SwipeAction.NONE, SwipeAction.fromId("invalid_id"))
    }
}
