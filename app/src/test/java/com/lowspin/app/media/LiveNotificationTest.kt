package com.lowspin.app.media

import android.app.Notification
import androidx.media3.common.Player
import com.lowspin.app.ui.settings.HighSpinThemes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveNotificationTest {

    @Test
    fun liveNotification_constantsMatchRequirements() {
        assertEquals(
            "com.lowspin.app.playback.live.v4",
            HighSpinLiveNotificationProvider.NOTIFICATION_CHANNEL_ID
        )
        assertEquals(
            "com.lowspin.app.playback_bubble",
            HighSpinLiveNotificationProvider.PLAYBACK_SHORTCUT_ID
        )
        assertTrue(
            HighSpinLiveNotificationProvider.PREVIOUS_NOTIFICATION_CHANNEL_IDS.contains("com.lowspin.app.playback.live.v3")
        )
        assertTrue(
            HighSpinLiveNotificationProvider.PREVIOUS_NOTIFICATION_CHANNEL_IDS.contains("com.lowspin.app.playback.live")
        )
        assertTrue(
            HighSpinLiveNotificationProvider.PREVIOUS_NOTIFICATION_CHANNEL_IDS.contains("com.lowspin.app.media")
        )
        assertEquals(1001, HighSpinLiveNotificationProvider.NOTIFICATION_ID)
        assertEquals(
            "android.requestPromotedOngoing",
            HighSpinLiveNotificationProvider.EXTRA_REQUEST_PROMOTED_ONGOING
        )
        assertEquals(
            "android.shortCriticalText",
            HighSpinLiveNotificationProvider.EXTRA_SHORT_CRITICAL_TEXT
        )
        assertEquals(
            "android.substName",
            HighSpinLiveNotificationProvider.EXTRA_SUBST_NAME
        )
        assertEquals(
            "android.ongoing",
            HighSpinLiveNotificationProvider.EXTRA_ONGOING
        )
        assertEquals(
            "android.showChronometer",
            HighSpinLiveNotificationProvider.EXTRA_SHOW_CHRONOMETER
        )
        assertEquals(
            "oplus.ongoing",
            HighSpinLiveNotificationProvider.EXTRA_OPLUS_ONGOING
        )
        assertEquals(
            "oplus_capsule",
            HighSpinLiveNotificationProvider.EXTRA_OPLUS_CAPSULE
        )
        assertEquals(
            "com.oplus.capsule",
            HighSpinLiveNotificationProvider.EXTRA_OPLUS_COM_CAPSULE
        )
        assertEquals(
            "oplus.capsule.type",
            HighSpinLiveNotificationProvider.EXTRA_OPLUS_CAPSULE_TYPE
        )
        assertEquals(
            "oplus_capsule_status",
            HighSpinLiveNotificationProvider.EXTRA_OPLUS_CAPSULE_STATUS
        )
        assertEquals(
            "capsule_title",
            HighSpinLiveNotificationProvider.EXTRA_CAPSULE_TITLE
        )
        assertEquals(
            "capsule_content",
            HighSpinLiveNotificationProvider.EXTRA_CAPSULE_CONTENT
        )
        assertEquals(
            "capsule_action",
            HighSpinLiveNotificationProvider.EXTRA_CAPSULE_ACTION
        )
        assertEquals(
            "oplus_capsule_action",
            HighSpinLiveNotificationProvider.EXTRA_OPLUS_CAPSULE_ACTION
        )
        assertEquals(
            "capsule_icon",
            HighSpinLiveNotificationProvider.EXTRA_CAPSULE_ICON
        )
        assertEquals(
            "oplus_capsule_icon",
            HighSpinLiveNotificationProvider.EXTRA_OPLUS_CAPSULE_ICON
        )
        assertEquals(
            "oplus.pantanal.support",
            HighSpinLiveNotificationProvider.EXTRA_OPLUS_PANTANAL_SUPPORT
        )
        assertEquals(
            "com.oplus.pantanal.support",
            HighSpinLiveNotificationProvider.EXTRA_COM_OPLUS_PANTANAL_SUPPORT
        )
        assertEquals(
            "oplus.pantanal.type",
            HighSpinLiveNotificationProvider.EXTRA_OPLUS_PANTANAL_TYPE
        )
        assertEquals(
            "is_live_notification",
            HighSpinLiveNotificationProvider.EXTRA_LIVE_NOTIFICATION
        )
        assertEquals(
            "android.liveNotification",
            HighSpinLiveNotificationProvider.EXTRA_ANDROID_LIVE_NOTIFICATION
        )
    }

    @Test
    fun liveNotification_tickerFormattingValid() {
        assertEquals(
            "Song Title • Artist Name",
            HighSpinLiveNotificationProvider.formatTicker("Song Title", "Artist Name")
        )
        assertEquals(
            "Song Title • Local Audio",
            HighSpinLiveNotificationProvider.formatTicker("Song Title", "")
        )
        assertEquals(
            "HighSpin • Artist Name",
            HighSpinLiveNotificationProvider.formatTicker("", "Artist Name")
        )
        assertEquals(
            "HighSpin • Local Audio",
            HighSpinLiveNotificationProvider.formatTicker(null, null)
        )
    }

    @Test
    fun liveNotification_shortCriticalFormattingValid() {
        assertEquals(
            "Bohemian Rhapsody",
            HighSpinLiveNotificationProvider.formatShortCritical("Bohemian Rhapsody")
        )
        assertEquals(
            "HighSpin",
            HighSpinLiveNotificationProvider.formatShortCritical(null)
        )
        assertEquals(
            "HighSpin",
            HighSpinLiveNotificationProvider.formatShortCritical("   ")
        )
        val veryLongTitle = "This is an extremely long song title that exceeds twenty-four characters"
        val formatted = HighSpinLiveNotificationProvider.formatShortCritical(veryLongTitle)
        assertTrue(formatted.length <= 24)
        assertEquals(veryLongTitle.take(24), formatted)
    }

    @Test
    fun liveNotification_progressCalculationValid() {
        val result = HighSpinLiveNotificationProvider.computeProgress(30_000L, 180_000L)
        assertTrue(result != null)
        assertEquals(180_000, result!!.first)
        assertEquals(30_000, result.second)

        // Invalid duration gives null (no indeterminate progress)
        assertNull(HighSpinLiveNotificationProvider.computeProgress(0L, 0L))
        assertNull(HighSpinLiveNotificationProvider.computeProgress(100L, -1L))
        assertNull(HighSpinLiveNotificationProvider.computeProgress(-10L, 100L))

        // Clamped position if position exceeds duration
        val clamped = HighSpinLiveNotificationProvider.computeProgress(200_000L, 180_000L)
        assertTrue(clamped != null)
        assertEquals(180_000, clamped!!.first)
        assertEquals(180_000, clamped.second)
    }

    @Test
    fun liveNotification_lyricInfoJsonFormattingValid() {
        val song = com.lowspin.app.data.db.SongEntity(
            id = 42L,
            title = "Starboy \"Deluxe\"",
            artist = "The Weeknd",
            artistId = 1L,
            album = "Starboy",
            albumId = 1L,
            durationMs = 230_000L,
            trackNumber = 1,
            discNumber = 1,
            year = 2016,
            folderPath = "/storage/emulated/0/Music",
            folderName = "Music",
            contentUri = "content://media/external/audio/media/42",
            mimeType = "audio/mpeg",
            dateAdded = 1000L,
            dateModified = 1000L
        )

        val jsonString = buildLyricInfoJson(song)
        assertTrue(jsonString.contains(""""songName":"Starboy \"Deluxe\"""""))
        assertTrue(jsonString.contains(""""artist":"The Weeknd""""))
        assertTrue(jsonString.contains(""""album":"Starboy""""))
        assertTrue(jsonString.contains(""""songId":"42""""))
        assertTrue(jsonString.contains(""""noLyric":true"""))

        val withLyrics = buildLyricInfoJson(song, "[00:12.34]I'm trying to put you in the worst mood")
        assertTrue(withLyrics.contains(""""noLyric":false"""))
        assertTrue(withLyrics.contains(""""lyric":"[00:12.34]I'm trying to put you in the worst mood""""))

        // Direct overload with complex special characters: quotes, backslashes, tabs, newlines
        val rawJson = buildLyricInfoJson(
            title = "A \"Title\" with \\backslash\\ and \nnewlines",
            artist = "AC/DC \"Live\"",
            album = "Back in Black\\Album",
            songId = 99L
        )
        assertTrue(rawJson.contains(""""songName":"A \"Title\" with \\backslash\\ and \nnewlines"""".replace("\n", "\\n")))
        assertTrue(rawJson.contains(""""artist":"AC/DC \"Live\"""""))
        assertTrue(rawJson.contains(""""album":"Back in Black\\Album""""))
        assertTrue(rawJson.contains(""""songId":"99""""))
        assertTrue(rawJson.contains(""""noLyric":true"""))

        // Direct escapeJson verification
        assertEquals("""Hello \"World\"""", escapeJson("""Hello "World""""))
        assertEquals("""Back\\Slash""", escapeJson("""Back\Slash"""))
        assertEquals("Line1\\nLine2\\rTab\\t", escapeJson("Line1\nLine2\rTab\t"))
        assertEquals("Bell\\bFormFeed\\f", escapeJson("Bell\bFormFeed\u000c"))
        assertEquals("Null\\u0000Esc\\u001bEnd", escapeJson("Null\u0000Esc\u001bEnd"))

        // Blank string fallbacks for OnePlus capsule stability
        val blankJson = buildLyricInfoJson(
            title = "   ",
            artist = "",
            album = "  \t ",
            songId = 123L
        )
        assertTrue(blankJson.contains(""""songName":"HighSpin""""))
        assertTrue(blankJson.contains(""""artist":"Local Audio""""))
        assertTrue(blankJson.contains(""""album":"HighSpin""""))
        assertTrue(blankJson.contains(""""songId":"123""""))
    }

    @Test
    fun liveNotification_isPlaybackActive_stateValidation() {
        // Active when user intended to play AND player is ready or buffering
        assertTrue(HighSpinLiveNotificationProvider.isPlaybackActive(true, Player.STATE_READY))
        assertTrue(HighSpinLiveNotificationProvider.isPlaybackActive(true, Player.STATE_BUFFERING))

        // When playlist or track reaches end, playWhenReady may remain true, but playbackState is ENDED.
        // MUST return false so notification is clearable and OxygenOS 15 Live Alerts dismiss properly:
        assertFalse(HighSpinLiveNotificationProvider.isPlaybackActive(true, Player.STATE_ENDED))
        assertFalse(HighSpinLiveNotificationProvider.isPlaybackActive(true, Player.STATE_IDLE))

        // When paused by user
        assertFalse(HighSpinLiveNotificationProvider.isPlaybackActive(false, Player.STATE_READY))
        assertFalse(HighSpinLiveNotificationProvider.isPlaybackActive(false, Player.STATE_BUFFERING))
        assertFalse(HighSpinLiveNotificationProvider.isPlaybackActive(false, Player.STATE_ENDED))
        assertFalse(HighSpinLiveNotificationProvider.isPlaybackActive(false, Player.STATE_IDLE))
    }

    @Test
    fun liveNotification_computeOngoingFlags_correctMasking() {
        val ongoingMask = Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR
        val baseFlags = 0x10 // FLAG_AUTO_CANCEL

        // When ongoing should be true
        val enabled = HighSpinLiveNotificationProvider.computeOngoingFlags(baseFlags, true)
        assertEquals(baseFlags or ongoingMask, enabled)
        assertTrue((enabled and Notification.FLAG_ONGOING_EVENT) != 0)
        assertTrue((enabled and Notification.FLAG_NO_CLEAR) != 0)

        // When ongoing should be false, flags are completely stripped
        val disabled = HighSpinLiveNotificationProvider.computeOngoingFlags(enabled, false)
        assertEquals(baseFlags, disabled)
        assertEquals(0, disabled and Notification.FLAG_ONGOING_EVENT)
        assertEquals(0, disabled and Notification.FLAG_NO_CLEAR)
    }

    @Test
    fun liveNotification_themesCountAndVisualizerAesthetics() {
        // 12 curated aesthetic themes
        assertEquals(12, HighSpinThemes.size)
        assertTrue(HighSpinThemes.any { it.id == "tokyo" })
        assertTrue(HighSpinThemes.any { it.id == "velvet" })
        assertTrue(HighSpinThemes.any { it.id == "aurora" })
        assertTrue(HighSpinThemes.any { it.id == "synthwave" })
        assertTrue(HighSpinThemes.any { it.id == "frost" })
    }

    @Test
    fun liveNotification_applyPromotedOngoingViaReflection_worksAndDoesNotThrow() {
        class MockBuilder {
            var promoted: Boolean = false
            fun setRequestPromotedOngoing(value: Boolean): MockBuilder {
                promoted = value
                return this
            }
        }

        class MockNotification {
            @JvmField
            var mRequestPromotedOngoing: Boolean = false
        }

        val builder = MockBuilder()
        HighSpinLiveNotificationProvider.applyPromotedOngoingViaReflection(builder, true)
        assertTrue(builder.promoted)
        HighSpinLiveNotificationProvider.applyPromotedOngoingViaReflection(builder, false)
        assertFalse(builder.promoted)

        val notification = MockNotification()
        HighSpinLiveNotificationProvider.applyPromotedOngoingViaReflection(notification, true)
        assertTrue(notification.mRequestPromotedOngoing)

        // Verifies no crash on arbitrary objects lacking the reflection target
        HighSpinLiveNotificationProvider.applyPromotedOngoingViaReflection("ArbitraryString", true)
    }

    @Test
    fun liveNotification_internalFieldsReflection_safeOnMockNotification() {
        class MockNotificationWithFields {
            @JvmField
            var mGroupKey: String? = "media3_group_key"
            @JvmField
            var mShortcutId: String? = null
            @JvmField
            var mChannelId: String? = "old_channel"
            @JvmField
            var mBubbleMetadata: Any? = null
        }

        val mock = MockNotificationWithFields()
        assertEquals("media3_group_key", mock.mGroupKey)
        assertNull(mock.mShortcutId)

        // Clear group
        val groupField = mock.javaClass.getDeclaredField("mGroupKey")
        groupField.isAccessible = true
        groupField.set(mock, null)
        assertNull(mock.mGroupKey)

        // Set shortcut ID
        val shortcutField = mock.javaClass.getDeclaredField("mShortcutId")
        shortcutField.isAccessible = true
        shortcutField.set(mock, HighSpinLiveNotificationProvider.PLAYBACK_SHORTCUT_ID)
        assertEquals(HighSpinLiveNotificationProvider.PLAYBACK_SHORTCUT_ID, mock.mShortcutId)

        // Set channel ID
        val channelField = mock.javaClass.getDeclaredField("mChannelId")
        channelField.isAccessible = true
        channelField.set(mock, HighSpinLiveNotificationProvider.NOTIFICATION_CHANNEL_ID)
        assertEquals(HighSpinLiveNotificationProvider.NOTIFICATION_CHANNEL_ID, mock.mChannelId)
    }
}
