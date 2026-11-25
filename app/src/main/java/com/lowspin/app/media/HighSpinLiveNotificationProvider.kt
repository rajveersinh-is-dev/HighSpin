package com.lowspin.app.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
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
 * Clean, minimal, Android 15+ live notification provider for HighSpin.
 *
 * Ensures:
 * 1. Status-bar live capsules, dynamic island chips, and lock screen widgets show the
 *    live track title and artist instead of a static app label fallback.
 * 2. Real-time ongoing progress and transport controls are mapped accurately.
 * 3. High-resolution local album art bitmap is supplied safely to the notification
 *    header (without binder blocking or OOM/IPC overflows) so lockscreen and notification
 *    shade cards render album art crisply.
 * 4. Android 15 & 16 Promoted Ongoing (Live Updates) flags and extras are set.
 * 5. Uses native chronometer ticking so the status bar and notification track
 *    real-time playback elapsed duration without waking the CPU every second.
 * 6. Respects user toggle in [AppearanceSettings.liveNotifications].
 */
@OptIn(UnstableApi::class)
class HighSpinLiveNotificationProvider(
    private val context: Context
) : DefaultMediaNotificationProvider(
    context,
    { NOTIFICATION_ID },
    NOTIFICATION_CHANNEL_ID,
    R.string.notification_channel_name
) {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val NOTIFICATION_CHANNEL_ID = "com.lowspin.app.media"

        const val EXTRA_REQUEST_PROMOTED_ONGOING = "android.requestPromotedOngoing"
        const val EXTRA_SHORT_CRITICAL_TEXT = "android.shortCriticalText"
        const val EXTRA_SUBST_NAME = "android.substName"
        const val EXTRA_ONGOING = "android.ongoing"
        const val EXTRA_SHOW_CHRONOMETER = "android.showChronometer"

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

    init {
        setSmallIcon(R.drawable.ic_stat_vinyl)
    }

    override fun addNotificationActions(
        mediaSession: MediaSession,
        mediaButtons: ImmutableList<CommandButton>,
        builder: NotificationCompat.Builder,
        actionFactory: MediaNotification.ActionFactory
    ): IntArray {
        val actionIndices = super.addNotificationActions(mediaSession, mediaButtons, builder, actionFactory)

        val player = mediaSession.player
        val isPlaying = player.playWhenReady
        val isLiveEnabled = appearance.liveNotifications.value
        val metadata = player.mediaMetadata
        val title = metadata.title?.toString()?.takeIf { it.isNotBlank() } ?: "HighSpin"
        val artist = metadata.artist?.toString()?.takeIf { it.isNotBlank() } ?: "Local Audio"
        val album = metadata.albumTitle?.toString()?.takeIf { it.isNotBlank() } ?: "HighSpin"

        // 1. Live Ticker for Android 15 & OEM Status Bar Live Alerts / Capsule
        if (isLiveEnabled && isPlaying) {
            builder.setTicker(formatTicker(title, artist))
        } else {
            builder.setTicker(null)
        }

        // 2. Ongoing flag: critical for Android 15 live activity / status bar pill
        builder.setOngoing(isPlaying)

        // 3. Clean minimal metadata
        builder.setContentTitle(title)
        builder.setContentText(artist)
        builder.setSubText(album)
        builder.setSmallIcon(R.drawable.ic_stat_vinyl)
        builder.setCategory(NotificationCompat.CATEGORY_TRANSPORT)
        builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        builder.setOnlyAlertOnce(true)

        // 4. Live Chronometer Ticking for Status Bar Pill & Lockscreen Cards
        val position = player.currentPosition
        if (isLiveEnabled && isPlaying && position >= 0L) {
            builder.setShowWhen(true)
            builder.setUsesChronometer(true)
            builder.setWhen(System.currentTimeMillis() - position)
        } else {
            builder.setShowWhen(false)
            builder.setUsesChronometer(false)
        }

        // 5. Live Progress Tracking for Lock Screen and Notification Shade
        val duration = player.duration
        val progress = computeProgress(position, duration)
        if (progress != null) {
            builder.setProgress(progress.first, progress.second, false)
        } else {
            builder.setProgress(0, 0, false)
        }

        // 6. Safe Album Art Bitmap for Large Icon (bounded decode, no binder block)
        val currentMediaItem = player.currentMediaItem
        val songId = currentMediaItem?.mediaId?.toLongOrNull()
        if (songId != null) {
            loadCachedArtBitmapSafe(songId)?.let { bitmap ->
                builder.setLargeIcon(bitmap)
            }
        }

        // 7. Android 15 & 16 Promoted Ongoing Live Updates flags and extras
        val shortCritical = formatShortCritical(title)
        builder.extras.apply {
            putBoolean(EXTRA_ONGOING, isPlaying)
            putCharSequence("android.title", title)
            putCharSequence("android.text", artist)
            putCharSequence(EXTRA_SUBST_NAME, "HighSpin")
            if (isLiveEnabled) {
                putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, isPlaying)
                putCharSequence(EXTRA_SHORT_CRITICAL_TEXT, shortCritical)
                if (isPlaying) {
                    putBoolean(EXTRA_SHOW_CHRONOMETER, true)
                }
            } else {
                putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, false)
            }
        }

        // Forward compatibility reflection if method exists on NotificationCompat.Builder
        if (isLiveEnabled) {
            runCatching {
                val method = builder.javaClass.getMethod(
                    "setRequestPromotedOngoing",
                    Boolean::class.javaPrimitiveType
                )
                method.invoke(builder, isPlaying)
            }
            runCatching {
                val method = builder.javaClass.getMethod(
                    "setShortCriticalText",
                    CharSequence::class.java
                )
                method.invoke(builder, shortCritical)
            }
        }

        return actionIndices
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
