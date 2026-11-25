package com.lowspin.app.media

import android.content.Context
import android.graphics.BitmapFactory
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.lowspin.app.LowspinApplication
import com.lowspin.app.R

/**
 * Clean, minimal, Android 15+ live notification provider for HighSpin.
 *
 * Ensures:
 * 1. Status-bar live capsules, dynamic island, and lock screen widgets show the
 *    live track title and artist instead of a static app label fallback.
 * 2. Real-time ongoing progress and transport controls are mapped accurately.
 * 3. High-resolution local album art bitmap is supplied directly to the notification
 *    header so lockscreen and notification shade cards render album art crisply.
 * 4. Android 15 & 16 Promoted Ongoing (Live Updates) flags are requested.
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
    }

    private val app: LowspinApplication?
        get() = context.applicationContext as? LowspinApplication

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
        val metadata = player.mediaMetadata
        val title = metadata.title?.toString()?.takeIf { it.isNotBlank() } ?: "HighSpin"
        val artist = metadata.artist?.toString()?.takeIf { it.isNotBlank() } ?: "Local Audio"
        val album = metadata.albumTitle?.toString()?.takeIf { it.isNotBlank() } ?: "HighSpin"

        // 1. Live Ticker for Android 15 & OEM Status Bar Live Alerts / Capsule
        val tickerText = "$title • $artist"
        builder.setTicker(tickerText)

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
        builder.setShowWhen(false)

        // 4. Live Progress Tracking for Lock Screen and Notification Shade
        val duration = player.duration
        val position = player.currentPosition
        if (duration > 0L && position >= 0L) {
            builder.setProgress(duration.toInt(), position.toInt(), false)
        }

        // 5. Album Art Bitmap for Large Icon
        val currentMediaItem = player.currentMediaItem
        val songId = currentMediaItem?.mediaId?.toLongOrNull()
        if (songId != null) {
            val artFile = app?.container?.albumArtRepository?.artworkFileForSongBlocking(songId)
            if (artFile != null && artFile.exists() && artFile.length() > 0L) {
                runCatching {
                    val bitmap = BitmapFactory.decodeFile(artFile.absolutePath)
                    if (bitmap != null) {
                        builder.setLargeIcon(bitmap)
                    }
                }
            }
        }

        // 6. Android 15 & 16 Promoted Ongoing Live Updates
        runCatching {
            val method = builder.javaClass.getMethod(
                "setRequestPromotedOngoing",
                Boolean::class.javaPrimitiveType
            )
            method.invoke(builder, true)
        }

        // 7. Extra metadata for Android 15 Live Notifications and OEM Dynamic Island
        builder.extras.apply {
            putBoolean("android.ongoing", isPlaying)
            putCharSequence("android.title", title)
            putCharSequence("android.text", artist)
            putCharSequence("android.substName", "HighSpin")
            putCharSequence("android.shortCriticalText", title)
        }

        return actionIndices
    }
}
