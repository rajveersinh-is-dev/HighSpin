package com.lowspin.app.media

import android.app.Notification
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.lowspin.app.R
import com.lowspin.app.ui.settings.AppearanceSettings
import java.io.File

/**
 * Clean, minimal, Android 15 & 16 + OnePlus OxygenOS 15 Live Notification provider for HighSpin.
 *
 * Implements [MediaNotification.Provider] directly around an underlying [DefaultMediaNotificationProvider]
 * so that:
 * 1. Status-bar live capsules, dynamic island chips, and lock screen widgets show the
 *    live track title and artist instead of a static app fallback.
 * 2. Real-time ongoing progress and transport controls are mapped accurately.
 * 3. High-resolution local album art bitmap is supplied safely to the notification
 *    header (without binder blocking or OOM/IPC overflows) so lockscreen and notification
 *    shade cards render album art crisply.
 * 4. Android 15 & 16 Promoted Ongoing (Live Updates) flags and extras are set.
 * 5. DefaultMediaNotificationProvider hardcodes [Notification.FLAG_ONGOING_EVENT] to false,
 *    which causes Android 15 and OnePlus OxygenOS 15 to reject the live capsule. We enforce
 *    [Notification.FLAG_ONGOING_EVENT] whenever playback is active.
 * 6. OnePlus OxygenOS 15 / ColorOS 15 Fluid Cloud & Live Alert capsule keys are supplied.
 * 7. Uses native chronometer ticking so the status bar and notification track
 *    real-time playback elapsed duration without waking the CPU every second.
 * 8. Respects user toggle in [AppearanceSettings.liveNotifications].
 */
@OptIn(UnstableApi::class)
class HighSpinLiveNotificationProvider(
    private val context: Context
) : MediaNotification.Provider {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val NOTIFICATION_CHANNEL_ID = "com.lowspin.app.playback.live"
        const val LEGACY_NOTIFICATION_CHANNEL_ID = "com.lowspin.app.media"

        const val EXTRA_REQUEST_PROMOTED_ONGOING = "android.requestPromotedOngoing"
        const val EXTRA_SHORT_CRITICAL_TEXT = "android.shortCriticalText"
        const val EXTRA_SUBST_NAME = "android.substName"
        const val EXTRA_ONGOING = "android.ongoing"
        const val EXTRA_SHOW_CHRONOMETER = "android.showChronometer"

        // OnePlus OxygenOS 15 / ColorOS 15 Fluid Cloud & Live Alert capsule extras
        const val EXTRA_OPLUS_ONGOING = "oplus.ongoing"
        const val EXTRA_OPLUS_CAPSULE = "oplus_capsule"
        const val EXTRA_OPLUS_COM_CAPSULE = "com.oplus.capsule"
        const val EXTRA_OPLUS_CAPSULE_TYPE = "oplus.capsule.type"
        const val EXTRA_LIVE_NOTIFICATION = "is_live_notification"
        const val EXTRA_ANDROID_LIVE_NOTIFICATION = "android.liveNotification"

        /**
         * Determines if playback is genuinely active (ready or buffering), preventing
         * ended/idle tracks from staying pinned as ongoing notifications or freezing
         * OxygenOS 15 punch-hole capsules.
         */
        fun isPlaybackActive(playWhenReady: Boolean, playbackState: Int): Boolean {
            return playWhenReady && (
                playbackState == androidx.media3.common.Player.STATE_READY ||
                playbackState == androidx.media3.common.Player.STATE_BUFFERING
            )
        }

        /**
         * Computes notification ongoing flags cleanly without bitwise leaks.
         */
        fun computeOngoingFlags(currentFlags: Int, isOngoing: Boolean): Int {
            return if (isOngoing) {
                currentFlags or Notification.FLAG_ONGOING_EVENT or Notification.FLAG_NO_CLEAR
            } else {
                currentFlags and Notification.FLAG_ONGOING_EVENT.inv() and Notification.FLAG_NO_CLEAR.inv()
            }
        }

        fun formatTicker(title: String?, artist: String?): String {
            val safeTitle = title?.takeIf { it.isNotBlank() } ?: "HighSpin"
            val safeArtist = artist?.takeIf { it.isNotBlank() } ?: "Local Audio"
            return "$safeTitle • $safeArtist"
        }

        fun formatShortCritical(title: String?): String {
            val safeTitle = title?.takeIf { it.isNotBlank() } ?: "HighSpin"
            return safeTitle.take(24)
        }

        fun computeProgress(position: Long, duration: Long): Pair<Int, Int>? {
            if (duration <= 0L || position < 0L) return null
            val safePos = position.coerceIn(0L, duration)
            return Pair(duration.toInt(), safePos.toInt())
        }
    }

    private val appearance by lazy { AppearanceSettings(context) }

    private val defaultProvider = DefaultMediaNotificationProvider(
        context,
        { NOTIFICATION_ID },
        NOTIFICATION_CHANNEL_ID,
        R.string.notification_channel_name
    ).apply {
        setSmallIcon(R.drawable.ic_stat_vinyl)
    }

    override fun createNotification(
        mediaSession: MediaSession,
        customLayout: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback
    ): MediaNotification {
        val wrappedCallback = MediaNotification.Provider.Callback { updatedMediaNotification ->
            val processed = decorateNotification(mediaSession, updatedMediaNotification)
            onNotificationChangedCallback.onNotificationChanged(processed)
        }

        val baseMediaNotification = defaultProvider.createNotification(
            mediaSession,
            customLayout,
            actionFactory,
            wrappedCallback
        )

        return decorateNotification(mediaSession, baseMediaNotification)
    }

    override fun handleCustomCommand(
        mediaSession: MediaSession,
        action: String,
        extras: Bundle
    ): Boolean {
        return defaultProvider.handleCustomCommand(mediaSession, action, extras)
    }

    /**
     * Enforces Android 15 & 16 Promoted Ongoing flags and OnePlus OxygenOS 15 Live Alerts / Fluid Cloud
     * capsule integration on the built notification.
     */
    private fun decorateNotification(
        mediaSession: MediaSession,
        mediaNotification: MediaNotification
    ): MediaNotification {
        val player = mediaSession.player
        val isPlaybackActive = isPlaybackActive(player.playWhenReady, player.playbackState)
        val isLiveEnabled = appearance.liveNotifications.value
        val shouldBeOngoing = isPlaybackActive && isLiveEnabled

        val metadata = player.mediaMetadata
        val title = metadata.title?.toString()?.takeIf { it.isNotBlank() } ?: "HighSpin"
        val artist = metadata.artist?.toString()?.takeIf { it.isNotBlank() } ?: "Local Audio"
        val album = metadata.albumTitle?.toString()?.takeIf { it.isNotBlank() } ?: "HighSpin"
        val position = player.currentPosition
        val duration = player.duration
        val shortCritical = formatShortCritical(title)

        val notification = mediaNotification.notification

        // 1. Android 15 & OxygenOS 15 require FLAG_ONGOING_EVENT for status bar capsule and promoted ongoing.
        // DefaultMediaNotificationProvider hardcodes setOngoing(false) right before build(), so we restore it
        // strictly when playback is active and live notifications are enabled.
        notification.flags = computeOngoingFlags(notification.flags, shouldBeOngoing)

        // 2. High priority transport category & public lockscreen visibility
        notification.category = Notification.CATEGORY_TRANSPORT
        notification.visibility = Notification.VISIBILITY_PUBLIC
        @Suppress("DEPRECATION")
        notification.priority = Notification.PRIORITY_DEFAULT

        // 3. Status-bar live ticker text
        if (shouldBeOngoing) {
            notification.tickerText = formatTicker(title, artist)
        } else {
            notification.tickerText = null
        }

        // 4. Live Chronometer Ticking for status bar pill & lockscreen cards
        // Media3 calls setShowWhen(false), which suppresses chronometers. Restoring android.showWhen
        // ensures Android 15 and OxygenOS 15 Live Alerts render live progress.
        if (shouldBeOngoing && position >= 0L) {
            notification.`when` = System.currentTimeMillis() - position
        } else {
            notification.`when` = System.currentTimeMillis()
        }

        // 5. Safe Album Art Bitmap for large icon if present
        val currentMediaItem = player.currentMediaItem
        val songId = currentMediaItem?.mediaId?.toLongOrNull()
        var artBitmap = songId?.let { loadCachedArtBitmapSafe(it) }
        @Suppress("DEPRECATION")
        if (artBitmap == null && notification.largeIcon != null) {
            artBitmap = notification.largeIcon
        }
        if (artBitmap != null) {
            @Suppress("DEPRECATION")
            notification.largeIcon = artBitmap
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                notification.extras.putParcelable(
                    Notification.EXTRA_LARGE_ICON_BIG,
                    Icon.createWithBitmap(artBitmap)
                )
            }
        }

        // 6. Complete Android 15 Promoted Ongoing (Live Updates) & OnePlus OxygenOS 15 Live Alerts extras
        val lyricJson = metadata.extras?.getString("lyricInfo")
            ?: currentMediaItem?.mediaMetadata?.extras?.getString("lyricInfo")
            ?: buildLyricInfoJson(title, artist, album, songId ?: 0L)

        notification.extras.apply {
            putBoolean(EXTRA_ONGOING, shouldBeOngoing)
            putCharSequence("android.title", title)
            putCharSequence("android.text", artist)
            putCharSequence(EXTRA_SUBST_NAME, "HighSpin")
            putString("songName", title)
            putString("artist", artist)
            putString("album", album)

            putString("lyricInfo", lyricJson)

            putBoolean(Notification.EXTRA_SHOW_WHEN, shouldBeOngoing)
            putBoolean(EXTRA_SHOW_CHRONOMETER, shouldBeOngoing)

            if (isLiveEnabled) {
                putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, shouldBeOngoing)
                putCharSequence(EXTRA_SHORT_CRITICAL_TEXT, shortCritical)

                // OnePlus OxygenOS 15 & ColorOS 15 Live Alerts / Fluid Cloud capsule keys
                putBoolean(EXTRA_OPLUS_ONGOING, shouldBeOngoing)
                putBoolean(EXTRA_OPLUS_CAPSULE, shouldBeOngoing)
                putBoolean(EXTRA_OPLUS_COM_CAPSULE, shouldBeOngoing)
                putString(EXTRA_OPLUS_CAPSULE_TYPE, "music")
                putBoolean("oplus_media", true)
                putBoolean(EXTRA_LIVE_NOTIFICATION, shouldBeOngoing)
                putBoolean(EXTRA_ANDROID_LIVE_NOTIFICATION, shouldBeOngoing)
            } else {
                putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, false)
                putBoolean(EXTRA_OPLUS_ONGOING, false)
                putBoolean(EXTRA_OPLUS_CAPSULE, false)
                putBoolean(EXTRA_OPLUS_COM_CAPSULE, false)
                putBoolean(EXTRA_LIVE_NOTIFICATION, false)
                putBoolean(EXTRA_ANDROID_LIVE_NOTIFICATION, false)
            }

            // Real-time track progress
            val progress = computeProgress(position, duration)
            if (progress != null) {
                putInt(Notification.EXTRA_PROGRESS_MAX, progress.first)
                putInt(Notification.EXTRA_PROGRESS, progress.second)
                putBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false)
            }
        }

        return mediaNotification
    }

    /**
     * Safely reads an already cached artwork file without blocking binder thread
     * on database queries or heavy MediaMetadataRetriever decodes.
     * Downsamples if needed to stay well below the 1MB IPC transaction limit.
     */
    private fun loadCachedArtBitmapSafe(songId: Long): Bitmap? {
        val artFile = File(File(context.cacheDir, "notification_art"), "$songId.jpg")
        if (!artFile.exists() || artFile.length() <= 0L) return null

        return runCatching {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(artFile.absolutePath, boundsOptions)
            val maxDimension = maxOf(boundsOptions.outWidth, boundsOptions.outHeight)

            var sampleSize = 1
            while ((maxDimension / sampleSize) > 384) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565 // Half memory footprint of ARGB_8888
            }
            BitmapFactory.decodeFile(artFile.absolutePath, decodeOptions)
        }.getOrNull()
    }
}
