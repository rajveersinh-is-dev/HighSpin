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
            "com.lowspin.app.playback.live.v10",
            HighSpinLiveNotificationProvider.NOTIFICATION_CHANNEL_ID
        )
        assertEquals(
            "com.lowspin.app.playback_bubble",
            HighSpinLiveNotificationProvider.PLAYBACK_SHORTCUT_ID
        )
        assertEquals(
            "com.lowspin.app.playback.live.v10_com.lowspin.app.playback_bubble",
            HighSpinLiveNotificationProvider.CONVERSATION_CHANNEL_ID
        )
        assertTrue(
            HighSpinLiveNotificationProvider.PREVIOUS_NOTIFICATION_CHANNEL_IDS.contains("com.lowspin.app.playback.live.v9")
        )
        assertTrue(
            HighSpinLiveNotificationProvider.PREVIOUS_NOTIFICATION_CHANNEL_IDS.contains("com.lowspin.app.playback.live.v7")
        )
        assertTrue(
            HighSpinLiveNotificationProvider.PREVIOUS_NOTIFICATION_CHANNEL_IDS.contains("com.lowspin.app.playback.live.v6")
        )
        assertTrue(
            HighSpinLiveNotificationProvider.PREVIOUS_NOTIFICATION_CHANNEL_IDS.contains("com.lowspin.app.playback.live.v5")
        )
        assertTrue(
            HighSpinLiveNotificationProvider.PREVIOUS_NOTIFICATION_CHANNEL_IDS.contains("com.lowspin.app.playback.live.v4")
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
        assertTrue(
            HighSpinLiveNotificationProvider.PLAYBACK_SHORTCUT_CATEGORIES.contains("android.shortcut.conversation")
        )
        assertEquals(1001, HighSpinLiveNotificationProvider.NOTIFICATION_ID)
        assertEquals(1002, HighSpinLiveNotificationProvider.BUBBLE_NOTIFICATION_ID)
        assertEquals(
            "android.colorized",
            HighSpinLiveNotificationProvider.EXTRA_ANDROID_COLORIZED
        )
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
            "com.oplus.pantanal.capsule.support",
            HighSpinLiveNotificationProvider.EXTRA_COM_OPLUS_PANTANAL_CAPSULE_SUPPORT
        )
        assertEquals(
            "oplus.capsule.support",
            HighSpinLiveNotificationProvider.EXTRA_OPLUS_CAPSULE_SUPPORT
        )
        assertEquals(
            "com.oplus.livealert.support",
            HighSpinLiveNotificationProvider.EXTRA_COM_OPLUS_LIVEALERT_SUPPORT
        )
        assertEquals(
            "oplus.livealert.support",
            HighSpinLiveNotificationProvider.EXTRA_OPLUS_LIVEALERT_SUPPORT
        )
        assertEquals(
            "com.oplus.fluidcloud.support",
            HighSpinLiveNotificationProvider.EXTRA_COM_OPLUS_FLUIDCLOUD_SUPPORT
        )
        assertEquals(
            "oplus.fluidcloud.support",
            HighSpinLiveNotificationProvider.EXTRA_OPLUS_FLUIDCLOUD_SUPPORT
        )
        assertEquals(
            "android.colorized",
            HighSpinLiveNotificationProvider.EXTRA_ANDROID_COLORIZED
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
        // 15 curated aesthetic themes including Pure Black, Cherry Noir, and Cyber Platinum
        assertEquals(15, HighSpinThemes.size)
        assertTrue(HighSpinThemes.any { it.id == "tokyo" })
        assertTrue(HighSpinThemes.any { it.id == "velvet" })
        assertTrue(HighSpinThemes.any { it.id == "aurora" })
        assertTrue(HighSpinThemes.any { it.id == "synthwave" })
        assertTrue(HighSpinThemes.any { it.id == "frost" })
        assertTrue(HighSpinThemes.any { it.id == "amoled" })
        assertTrue(HighSpinThemes.any { it.id == "noir" })
        assertTrue(HighSpinThemes.any { it.id == "platinum" })
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
        assertEquals("old_channel", mock.mChannelId)
        assertNull(mock.mBubbleMetadata)

        val mockBubble = "MockBubbleObject"
        HighSpinLiveNotificationProvider.applyNotificationInternalOverrides(
            target = mock,
            channelId = HighSpinLiveNotificationProvider.NOTIFICATION_CHANNEL_ID,
            shortcutId = HighSpinLiveNotificationProvider.PLAYBACK_SHORTCUT_ID,
            bubbleMetadata = mockBubble,
            clearGroup = true
        )

        assertNull(mock.mGroupKey)
        assertEquals(HighSpinLiveNotificationProvider.PLAYBACK_SHORTCUT_ID, mock.mShortcutId)
        assertEquals(HighSpinLiveNotificationProvider.NOTIFICATION_CHANNEL_ID, mock.mChannelId)
        assertEquals(mockBubble, mock.mBubbleMetadata)

        // Verifies no crash on arbitrary objects lacking the reflection target
        HighSpinLiveNotificationProvider.applyNotificationInternalOverrides("ArbitraryObject")
    }

    @Test
    fun liveNotification_internalFieldsReflection_resolvesOnSubclass() {
        open class SuperNotification {
            @JvmField
            var mGroupKey: String? = "media3_group_key"
            @JvmField
            var mShortcutId: String? = null
            @JvmField
            var mChannelId: String? = "old_channel"
            @JvmField
            var mBubbleMetadata: Any? = null
        }

        class SubclassNotification : SuperNotification()

        val sub = SubclassNotification()
        HighSpinLiveNotificationProvider.applyNotificationInternalOverrides(
            target = sub,
            channelId = HighSpinLiveNotificationProvider.NOTIFICATION_CHANNEL_ID,
            shortcutId = HighSpinLiveNotificationProvider.PLAYBACK_SHORTCUT_ID,
            bubbleMetadata = "SubBubble",
            clearGroup = true
        )

        assertNull(sub.mGroupKey)
        assertEquals(HighSpinLiveNotificationProvider.PLAYBACK_SHORTCUT_ID, sub.mShortcutId)
        assertEquals(HighSpinLiveNotificationProvider.NOTIFICATION_CHANNEL_ID, sub.mChannelId)
        assertEquals("SubBubble", sub.mBubbleMetadata)
    }

    @Test
    fun liveNotification_findDeclaredField_searchesRecursively() {
        open class GrandParent {
            @JvmField
            var mGrandField: String = "found"
        }
        open class Parent : GrandParent()
        class Child : Parent()

        val field = HighSpinLiveNotificationProvider.findDeclaredField(Child::class.java, "mGrandField")
        assertTrue(field != null)
        assertEquals("mGrandField", field!!.name)

        val nonExistent = HighSpinLiveNotificationProvider.findDeclaredField(Child::class.java, "mDoesNotExist")
        assertNull(nonExistent)
    }

    @Test
    fun liveNotification_conversationChannelMatchesShortcut() {
        val expected = "${HighSpinLiveNotificationProvider.NOTIFICATION_CHANNEL_ID}_${HighSpinLiveNotificationProvider.PLAYBACK_SHORTCUT_ID}"
        assertEquals(expected, HighSpinLiveNotificationProvider.CONVERSATION_CHANNEL_ID)
    }

    @Test
    fun bubbleActivity_classIsAvailableForBubbleIntent() {
        val clazz = com.lowspin.app.BubbleActivity::class.java
        assertTrue(android.app.Activity::class.java.isAssignableFrom(clazz))
    }

    @Test
    fun liveNotification_shortcutIntentCreatedSuccessfully() {
        val context = object : android.content.ContextWrapper(null) {
            override fun getPackageName(): String = "com.lowspin.app"
        }
        val intent = HighSpinLiveNotificationProvider.buildPlaybackShortcutIntent(context)
        org.junit.Assert.assertNotNull(intent)
    }

    @Test
    fun liveNotification_bubbleIntentCreatedSuccessfully() {
        val context = object : android.content.ContextWrapper(null) {
            override fun getPackageName(): String = "com.lowspin.app"
        }
        val intent = HighSpinLiveNotificationProvider.buildBubbleIntent(context)
        org.junit.Assert.assertNotNull(intent)
    }

    @Test
    fun liveNotification_companionBubbleNotificationId_isDistinctFromPlaybackNotification() {
        // HighSpin dual-channel notification architecture:
        // ID 1001 for MediaStyle playback notification (OxygenOS 15 Live Alerts / Media Carousel)
        // ID 1002 for MessagingStyle companion conversation bubble (Android 11-15 Floating Bubbles)
        assertTrue(HighSpinLiveNotificationProvider.NOTIFICATION_ID != HighSpinLiveNotificationProvider.BUBBLE_NOTIFICATION_ID)
        assertEquals(1001, HighSpinLiveNotificationProvider.NOTIFICATION_ID)
        assertEquals(1002, HighSpinLiveNotificationProvider.BUBBLE_NOTIFICATION_ID)
        assertEquals(
            "android.colorized",
            HighSpinLiveNotificationProvider.EXTRA_ANDROID_COLORIZED
        )
    }

    @Test
    fun liveNotification_pureBlackThemePreset_configuredCorrectly() {
        val pureBlack = HighSpinThemes.find { it.id == "amoled" }
        org.junit.Assert.assertNotNull(pureBlack)
        assertEquals("AMOLED Pitch Black", pureBlack!!.name)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF000000), pureBlack.background)
        assertEquals(androidx.compose.ui.graphics.Color(0xFF000000), pureBlack.surface)
    }

    @Test
    fun bubbleActivity_pipActionsAndExtras_configuredCorrectly() {
        assertEquals("EXTRA_START_PIP", com.lowspin.app.BubbleActivity.EXTRA_START_PIP)
        assertEquals("com.lowspin.app.ACTION_PIP_PLAY_PAUSE", com.lowspin.app.BubbleActivity.ACTION_PIP_PLAY_PAUSE)
        assertEquals("com.lowspin.app.ACTION_PIP_PREV", com.lowspin.app.BubbleActivity.ACTION_PIP_PREV)
        assertEquals("com.lowspin.app.ACTION_PIP_NEXT", com.lowspin.app.BubbleActivity.ACTION_PIP_NEXT)
    }

    @Test
    fun liveNotification_actionsAndConstants_definedCorrectly() {
        assertEquals("com.lowspin.app.action.PLAY_PAUSE", HighSpinLiveNotificationProvider.ACTION_PLAY_PAUSE)
        assertEquals("com.lowspin.app.action.NEXT", HighSpinLiveNotificationProvider.ACTION_NEXT)
        assertEquals("com.lowspin.app.action.PREV", HighSpinLiveNotificationProvider.ACTION_PREV)
    }

    @Test
    fun playbackService_gestureThresholdsAndConstants_calibratedCorrectly() {
        // Stationary baseline is 1.0g. Shake threshold lowered to 1.65g with 1200ms debounce
        assertEquals(1.65f, PlaybackService.SHAKE_G_FORCE_THRESHOLD, 0.001f)
        assertEquals(1200L, PlaybackService.SHAKE_DEBOUNCE_MS)

        // OnePlus camera bump calibrated flip-to-pause parameters
        assertEquals(-6.5f, PlaybackService.FLIP_Z_THRESHOLD, 0.001f)
        assertEquals(6.5f, PlaybackService.FLIP_PLANAR_MAX, 0.001f)
        assertEquals(-2.0f, PlaybackService.FLIP_RESET_Z_THRESHOLD, 0.001f)
    }

    @Test
    fun playbackService_gestureDetectionLogic_validatesAccurately() {
        // Stationary or slight movement should NOT trigger shake
        assertFalse(PlaybackService.isShakeGesture(1.0f))
        assertFalse(PlaybackService.isShakeGesture(1.5f))
        assertFalse(PlaybackService.isShakeGesture(1.64f))

        // Normal deliberate shake reaches 1.65g+
        assertTrue(PlaybackService.isShakeGesture(1.66f))
        assertTrue(PlaybackService.isShakeGesture(2.0f))
        assertTrue(PlaybackService.isShakeGesture(3.0f))
    }

    @Test
    fun playbackService_faceDownHysteresisAndCameraBumpAngle_validatesAccurately() {
        // Perfectly flat on desk face down (z ~ -9.8, x=0, y=0)
        assertTrue(PlaybackService.isFaceDownOrientation(0f, 0f, -9.8f))

        // OnePlus phone with large camera bump resting tilted face down (z ~ -7.2, planar ~ 2.2)
        assertTrue(PlaybackService.isFaceDownOrientation(1.0f, 2.0f, -7.2f))

        // On OnePlus phone with camera bump, z is between -6.5f and -8.0f
        assertTrue(PlaybackService.isFaceDownOrientation(0f, 2.5f, -6.6f))

        // Face up on desk (z = +9.8)
        assertFalse(PlaybackService.isFaceDownOrientation(0f, 0f, 9.8f))

        // Standing upright in hand (y = 9.8, z = 0)
        assertFalse(PlaybackService.isFaceDownOrientation(0f, 9.8f, 0f))

        // Held at steep slant or sideways (large planar acceleration >= 6.5)
        assertFalse(PlaybackService.isFaceDownOrientation(5.0f, 5.0f, -7.0f))

        // Hysteresis reset: face-down state is only reset when phone is flipped back up past -2.0f
        assertTrue(PlaybackService.isFaceDownReset(0f))
        assertTrue(PlaybackService.isFaceDownReset(-1.5f))
        assertFalse(PlaybackService.isFaceDownReset(-2.5f))
        assertFalse(PlaybackService.isFaceDownReset(-7.0f))
    }

    @Test
    fun liveNotification_pantanalAndOxygenOsMetadata_configuredProperly() {
        val pantanalSupport = listOf(
            HighSpinLiveNotificationProvider.EXTRA_OPLUS_PANTANAL_SUPPORT,
            HighSpinLiveNotificationProvider.EXTRA_COM_OPLUS_PANTANAL_SUPPORT,
            HighSpinLiveNotificationProvider.EXTRA_COM_OPLUS_PANTANAL_CAPSULE_SUPPORT,
            HighSpinLiveNotificationProvider.EXTRA_OPLUS_CAPSULE_SUPPORT,
            HighSpinLiveNotificationProvider.EXTRA_COM_OPLUS_LIVEALERT_SUPPORT,
            HighSpinLiveNotificationProvider.EXTRA_OPLUS_LIVEALERT_SUPPORT,
            HighSpinLiveNotificationProvider.EXTRA_COM_OPLUS_FLUIDCLOUD_SUPPORT,
            HighSpinLiveNotificationProvider.EXTRA_OPLUS_FLUIDCLOUD_SUPPORT
        )
        for (key in pantanalSupport) {
            assertTrue(key.isNotEmpty())
            assertTrue(key.contains("pantanal") || key.contains("capsule") || key.contains("livealert") || key.contains("fluidcloud"))
        }
    }

    @Test
    fun playbackService_findFrameworkMediaSession_traversalChainResolution() {
        // Mock the exact 4-tier hierarchy of Media3 to framework MediaSession:
        // Tier 1: androidx.media3.session.MediaSession (has field 'impl')
        // Tier 2: androidx.media3.session.MediaSessionImpl (has field 'sessionCompat')
        // Tier 3: android.support.v4.media.session.MediaSessionCompat (has field 'mImpl')
        // Tier 4: MediaSessionImplApi21 (has field 'mSessionFwk' of type android.media.session.MediaSession)

        class Tier4(val mSessionFwk: android.media.session.MediaSession?)
        class Tier3(val mImpl: Tier4)
        class Tier2(val sessionCompat: Tier3)
        class Tier1(val impl: Tier2)

        // Pass a mock or null
        assertNull(PlaybackService.findFrameworkMediaSession(null))

        val emptyHierarchy = Tier1(Tier2(Tier3(Tier4(null))))
        assertNull(PlaybackService.findFrameworkMediaSession(emptyHierarchy))

        // When mSessionFwk is non-null
        val context = object : android.content.ContextWrapper(null) {
            override fun getPackageName(): String = "com.lowspin.app"
        }
        val fwkSession = android.media.session.MediaSession(context, "test_tag")
        org.junit.Assert.assertNotNull(fwkSession)
        val validHierarchy = Tier1(Tier2(Tier3(Tier4(fwkSession))))
        val found = PlaybackService.findFrameworkMediaSession(validHierarchy)
        assertEquals(fwkSession, found)

        // Direct object resolution
        assertEquals(fwkSession, PlaybackService.findFrameworkMediaSession(fwkSession))
    }
}
