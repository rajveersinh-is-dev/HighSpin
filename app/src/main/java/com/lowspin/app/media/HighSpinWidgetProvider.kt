package com.lowspin.app.media

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.lowspin.app.LowspinApplication
import com.lowspin.app.MainActivity
import com.lowspin.app.R

/**
 * Home & Lock Screen (keyguard) Now Playing AppWidget synced live by [PlaybackService].
 */
class HighSpinWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val container = (context.applicationContext as? LowspinApplication)?.container
        val state = container?.playbackController?.state?.value
        val song = state?.currentSong
        updateWidgets(
            context = context,
            appWidgetManager = appWidgetManager,
            appWidgetIds = appWidgetIds,
            title = song?.title ?: "HighSpin",
            artist = song?.artist ?: "Tap to open library",
            isPlaying = state?.isPlaying == true,
            songId = song?.id ?: -1L
        )
    }

    companion object {
        fun pushUpdate(
            context: Context,
            title: String,
            artist: String,
            isPlaying: Boolean,
            songId: Long
        ) {
            runCatching {
                val manager = AppWidgetManager.getInstance(context) ?: return
                val ids = manager.getAppWidgetIds(ComponentName(context, HighSpinWidgetProvider::class.java))
                if (ids.isEmpty()) return
                updateWidgets(context, manager, ids, title, artist, isPlaying, songId)
            }
        }

        private fun updateWidgets(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetIds: IntArray,
            title: String,
            artist: String,
            isPlaying: Boolean,
            songId: Long
        ) {
            val artBitmap = loadCompactArtwork(context, songId)
            val openAppIntent = PendingIntent.getActivity(
                context,
                300,
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val prevPi = serviceActionPendingIntent(context, 301, HighSpinLiveNotificationProvider.ACTION_PREV)
            val playPausePi = serviceActionPendingIntent(context, 302, HighSpinLiveNotificationProvider.ACTION_PLAY_PAUSE)
            val nextPi = serviceActionPendingIntent(context, 303, HighSpinLiveNotificationProvider.ACTION_NEXT)

            for (id in appWidgetIds) {
                val views = RemoteViews(context.packageName, R.layout.widget_now_playing).apply {
                    setTextViewText(R.id.widget_title, title.ifBlank { "HighSpin" })
                    setTextViewText(R.id.widget_artist, artist.ifBlank { "Local Audio" })
                    setImageViewResource(
                        R.id.widget_play_pause,
                        if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
                    )
                    if (artBitmap != null) {
                        setImageViewBitmap(R.id.widget_art, artBitmap)
                    } else {
                        setImageViewResource(R.id.widget_art, R.drawable.ic_stat_vinyl)
                    }
                    setOnClickPendingIntent(R.id.widget_root, openAppIntent)
                    setOnClickPendingIntent(R.id.widget_prev, prevPi)
                    setOnClickPendingIntent(R.id.widget_play_pause, playPausePi)
                    setOnClickPendingIntent(R.id.widget_next, nextPi)
                }
                appWidgetManager.updateAppWidget(id, views)
            }
        }

        private fun loadCompactArtwork(context: Context, songId: Long): Bitmap? {
            if (songId <= 0L) return null
            val app = context.applicationContext as? LowspinApplication ?: return null
            val file = runCatching { app.container.albumArtRepository.artworkFileForSongBlocking(songId) }.getOrNull()
                ?: return null
            val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
            return runCatching { BitmapFactory.decodeFile(file.absolutePath, opts) }.getOrNull()
        }

        private fun serviceActionPendingIntent(
            context: Context,
            requestCode: Int,
            action: String
        ): PendingIntent {
            val intent = Intent(context, PlaybackService::class.java).apply {
                this.action = action
            }
            return PendingIntent.getService(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
