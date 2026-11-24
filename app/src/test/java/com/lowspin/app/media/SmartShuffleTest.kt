package com.lowspin.app.media

import androidx.media3.common.Player
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartShuffleTest {

    private val smartShuffle = SmartShuffleManager()

    @Test
    fun shouldSkipImmediateRepeat_skipsWhenSameSongRepeatsOnAutoTransition() {
        smartShuffle.onPlayed(3)

        val shouldSkip = smartShuffle.shouldSkipImmediateRepeat(
            nextIndex = 3,
            reason = Player.MEDIA_ITEM_TRANSITION_REASON_AUTO,
            shuffleEnabled = true,
            repeatMode = Player.REPEAT_MODE_ALL,
            queueSize = 10
        )
        assertTrue(shouldSkip)
    }

    @Test
    fun shouldSkipImmediateRepeat_doesNotSkipWhenDifferentIndex() {
        smartShuffle.onPlayed(3)

        val shouldSkip = smartShuffle.shouldSkipImmediateRepeat(
            nextIndex = 4,
            reason = Player.MEDIA_ITEM_TRANSITION_REASON_AUTO,
            shuffleEnabled = true,
            repeatMode = Player.REPEAT_MODE_ALL,
            queueSize = 10
        )
        assertFalse(shouldSkip)
    }

    @Test
    fun shouldSkipImmediateRepeat_doesNotSkipWhenShuffleDisabled() {
        smartShuffle.onPlayed(3)

        val shouldSkip = smartShuffle.shouldSkipImmediateRepeat(
            nextIndex = 3,
            reason = Player.MEDIA_ITEM_TRANSITION_REASON_AUTO,
            shuffleEnabled = false,
            repeatMode = Player.REPEAT_MODE_ALL,
            queueSize = 10
        )
        assertFalse(shouldSkip)
    }

    @Test
    fun shouldSkipImmediateRepeat_doesNotSkipWhenRepeatModeIsOne() {
        smartShuffle.onPlayed(3)

        val shouldSkip = smartShuffle.shouldSkipImmediateRepeat(
            nextIndex = 3,
            reason = Player.MEDIA_ITEM_TRANSITION_REASON_AUTO,
            shuffleEnabled = true,
            repeatMode = Player.REPEAT_MODE_ONE,
            queueSize = 10
        )
        assertFalse(shouldSkip)
    }

    @Test
    fun shouldSkipImmediateRepeat_doesNotSkipWhenQueueSizeIsOne() {
        smartShuffle.onPlayed(0)

        val shouldSkip = smartShuffle.shouldSkipImmediateRepeat(
            nextIndex = 0,
            reason = Player.MEDIA_ITEM_TRANSITION_REASON_AUTO,
            shuffleEnabled = true,
            repeatMode = Player.REPEAT_MODE_ALL,
            queueSize = 1
        )
        assertFalse(shouldSkip)
    }

    @Test
    fun shouldSkipImmediateRepeat_doesNotSkipOnManualSeekOrRepeatReason() {
        smartShuffle.onPlayed(3)

        val shouldSkipSeek = smartShuffle.shouldSkipImmediateRepeat(
            nextIndex = 3,
            reason = Player.MEDIA_ITEM_TRANSITION_REASON_SEEK,
            shuffleEnabled = true,
            repeatMode = Player.REPEAT_MODE_ALL,
            queueSize = 10
        )
        assertFalse(shouldSkipSeek)

        val shouldSkipRepeat = smartShuffle.shouldSkipImmediateRepeat(
            nextIndex = 3,
            reason = Player.MEDIA_ITEM_TRANSITION_REASON_REPEAT,
            shuffleEnabled = true,
            repeatMode = Player.REPEAT_MODE_ALL,
            queueSize = 10
        )
        assertFalse(shouldSkipRepeat)
    }
}
