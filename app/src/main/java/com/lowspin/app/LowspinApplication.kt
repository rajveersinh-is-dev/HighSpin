package com.lowspin.app

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.lowspin.app.di.AppContainer
import com.lowspin.app.media.HighSpinLiveNotificationProvider
import com.lowspin.app.media.PlaybackService

class LowspinApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels(this)
        container = AppContainer(this)
    }

    companion object {
        fun createNotificationChannels(context: Context) {
            val nm = context.getSystemService(NotificationManager::class.java) ?: return

            // Clean up legacy and previous channels that were stuck on lower importance
            for (oldChannelId in HighSpinLiveNotificationProvider.PREVIOUS_NOTIFICATION_CHANNEL_IDS) {
                runCatching {
                    nm.deleteNotificationChannel(oldChannelId)
                }
            }

            // If the current channel exists with importance lower than IMPORTANCE_HIGH, delete it so it can be recreated
            val existing = nm.getNotificationChannel(PlaybackService.NOTIFICATION_CHANNEL_ID)
            if (existing != null && existing.importance < NotificationManager.IMPORTANCE_HIGH) {
                runCatching {
                    nm.deleteNotificationChannel(PlaybackService.NOTIFICATION_CHANNEL_ID)
                }
            }

            val ch = NotificationChannel(
                PlaybackService.NOTIFICATION_CHANNEL_ID,
                "HighSpin Playback",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Music playback controls and live progress"
                setSound(null, null)
                enableVibration(false)
                vibrationPattern = longArrayOf()
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setShowBadge(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setAllowBubbles(true)
                }
            }
            nm.createNotificationChannel(ch)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val convChannelId = HighSpinLiveNotificationProvider.CONVERSATION_CHANNEL_ID
                val existingConv = nm.getNotificationChannel(convChannelId)
                if (existingConv == null) {
                    val conversationChannel = NotificationChannel(
                        convChannelId,
                        "Now Playing Bubble",
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "HighSpin playback floating bubble conversation"
                        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                        setShowBadge(true)
                        enableVibration(false)
                        setSound(null, null)
                        setAllowBubbles(true)
                        setConversationId(
                            PlaybackService.NOTIFICATION_CHANNEL_ID,
                            HighSpinLiveNotificationProvider.PLAYBACK_SHORTCUT_ID
                        )
                    }
                    nm.createNotificationChannel(conversationChannel)
                }
            }
        }
    }
}
