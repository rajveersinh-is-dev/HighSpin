package com.lowspin.app.media

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.palette.graphics.Palette
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import com.lowspin.app.BubbleActivity
import com.lowspin.app.LowspinApplication
import com.lowspin.app.MainActivity
import com.lowspin.app.R
import com.lowspin.app.ui.settings.AppearanceSettings
import com.lowspin.app.ui.settings.HighSpinThemes
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
 * 5. DefaultMediaNotificationProvider hardcodes [Notification.FLAG_ONGOING_EVENT] to false
 *    and groups children with "media3_group_key", which causes Android 15 Promoted Ongoing
 *    (Notification.hasPromotableCharacteristics) and OnePlus OxygenOS 15 to reject the live capsule.
 *    We clear grouping, enforce [Notification.FLAG_ONGOING_EVENT] whenever playback is active,
 *    and clear colorization so promotable criteria are fully satisfied.
 * 6. OnePlus OxygenOS 15 / ColorOS 15 Fluid Cloud & Live Alert capsule keys, icons, and actions are supplied.
 * 7. Android 11+ Notification Bubbles are fully enabled by publishing a registered dynamic shortcut
 *    and binding matching shortcutId and BubbleMetadata.
 * 8. Uses native chronometer ticking so the status bar and notification track
 *    real-time playback elapsed duration without waking the CPU every second.
 * 9. Respects user toggle in [AppearanceSettings.liveNotifications].
 */
@OptIn(UnstableApi::class)
class HighSpinLiveNotificationProvider(
    private val context: Context
) : MediaNotification.Provider {

    companion object {
        const val NOTIFICATION_ID = 1001
        const val BUBBLE_NOTIFICATION_ID = 1002
        const val NOTIFICATION_CHANNEL_ID = "com.lowspin.app.playback.live.v10"
        const val PLAYBACK_SHORTCUT_ID = "com.lowspin.app.playback_bubble"
        val CONVERSATION_CHANNEL_ID = "${NOTIFICATION_CHANNEL_ID}_$PLAYBACK_SHORTCUT_ID"
        val PREVIOUS_NOTIFICATION_CHANNEL_IDS = listOf(
            "com.lowspin.app.playback.live.v9",
            "com.lowspin.app.playback.live.v8",
            "com.lowspin.app.playback.live.v7",
            "com.lowspin.app.playback.live.v6",
            "com.lowspin.app.playback.live.v5",
            "com.lowspin.app.playback.live.v4",
            "com.lowspin.app.playback.live.v3",
            "com.lowspin.app.playback.live",
            "com.lowspin.app.media"
        )
        val PLAYBACK_SHORTCUT_CATEGORIES = setOf(
            NotificationCompat.CATEGORY_TRANSPORT,
            "com.lowspin.app.category.PLAYBACK",
            "android.shortcut.conversation"
        )

        const val ACTION_PLAY_PAUSE = "com.lowspin.app.action.PLAY_PAUSE"
        const val ACTION_NEXT = "com.lowspin.app.action.NEXT"
        const val ACTION_PREV = "com.lowspin.app.action.PREV"

        const val EXTRA_REQUEST_PROMOTED_ONGOING = "android.requestPromotedOngoing"
        const val EXTRA_SHORT_CRITICAL_TEXT = "android.shortCriticalText"
        const val EXTRA_SUBST_NAME = "android.substName"
        const val EXTRA_ONGOING = "android.ongoing"
        const val EXTRA_SHOW_CHRONOMETER = "android.showChronometer"
        const val EXTRA_ANDROID_COLORIZED = "android.colorized"

        // OnePlus OxygenOS 15 / ColorOS 15 Fluid Cloud & Live Alert capsule extras
        const val EXTRA_OPLUS_ONGOING = "oplus.ongoing"
        const val EXTRA_OPLUS_CAPSULE = "oplus_capsule"
        const val EXTRA_OPLUS_COM_CAPSULE = "com.oplus.capsule"
        const val EXTRA_OPLUS_CAPSULE_TYPE = "oplus.capsule.type"
        const val EXTRA_OPLUS_CAPSULE_STATUS = "oplus_capsule_status"
        const val EXTRA_CAPSULE_TITLE = "capsule_title"
        const val EXTRA_CAPSULE_CONTENT = "capsule_content"
        const val EXTRA_CAPSULE_ACTION = "capsule_action"
        const val EXTRA_OPLUS_CAPSULE_ACTION = "oplus_capsule_action"
        const val EXTRA_CAPSULE_ICON = "capsule_icon"
        const val EXTRA_OPLUS_CAPSULE_ICON = "oplus_capsule_icon"
        const val EXTRA_OPLUS_PANTANAL_SUPPORT = "oplus.pantanal.support"
        const val EXTRA_COM_OPLUS_PANTANAL_SUPPORT = "com.oplus.pantanal.support"
        const val EXTRA_COM_OPLUS_PANTANAL_CAPSULE_SUPPORT = "com.oplus.pantanal.capsule.support"
        const val EXTRA_OPLUS_CAPSULE_SUPPORT = "oplus.capsule.support"
        const val EXTRA_COM_OPLUS_LIVEALERT_SUPPORT = "com.oplus.livealert.support"
        const val EXTRA_OPLUS_LIVEALERT_SUPPORT = "oplus.livealert.support"
        const val EXTRA_COM_OPLUS_FLUIDCLOUD_SUPPORT = "com.oplus.fluidcloud.support"
        const val EXTRA_OPLUS_FLUIDCLOUD_SUPPORT = "oplus.fluidcloud.support"
        const val EXTRA_OPLUS_PANTANAL_TYPE = "oplus.pantanal.type"
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

        /**
         * Builds the intent used by the dynamic shortcut. Targets [BubbleActivity] with multi-task
         * flags so SystemUI bubble controllers on Android 11+ and Android 15 can embed it safely
         * into floating TaskViews.
         */
        fun buildPlaybackShortcutIntent(context: Context): Intent {
            return Intent(context, BubbleActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            }
        }

        /**
         * Builds the pending intent target for [BubbleMetadata].
         */
        fun buildBubbleIntent(context: Context): Intent {
            return Intent(context, BubbleActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            }
        }

        /**
         * Publishes a long-lived dynamic shortcut to ShortcutManagerCompat.
         * On Android 11+ (API 30+), notifications MUST be associated with a valid
         * registered dynamic shortcut in order to be eligible for Notification Bubbles.
         * The shortcut intent targets [BubbleActivity] with multi-task flags so SystemUI's
         * BubbleExtractor/canLaunchInTaskView verifies allowEmbedded and documentLaunchMode successfully.
         */
        fun publishPlaybackShortcut(context: Context) {
            runCatching {
                val shortcutIntent = buildPlaybackShortcutIntent(context)

                val person = Person.Builder()
                    .setName("HighSpin")
                    .setKey("highspin_playback")
                    .setBot(false)
                    .setImportant(true)
                    .build()

                val shortcut = ShortcutInfoCompat.Builder(context, PLAYBACK_SHORTCUT_ID)
                    .setShortLabel("Now Playing")
                    .setLongLabel("HighSpin Playback")
                    .setIcon(IconCompat.createWithResource(context, R.drawable.ic_stat_vinyl))
                    .setIntent(shortcutIntent)
                    .setLongLived(true)
                    .setPerson(person)
                    .setCategories(PLAYBACK_SHORTCUT_CATEGORIES)
                    .build()

                ShortcutManagerCompat.pushDynamicShortcut(context, shortcut)
            }
        }

        /**
         * Creates Android 11+ Notification Bubble metadata targeting BubbleActivity.
         * Ensures the dynamic shortcut is registered and sets FLAG_MUTABLE on Android 12+ (API 31+)
         * so SystemUI can configure task launch flags. Uses shortcutId constructor on Android 11+
         * (API 30+) as required by BubbleController.
         */
        fun createBubbleMetadata(context: Context): NotificationCompat.BubbleMetadata? {
            publishPlaybackShortcut(context)
            return runCatching {
                val bubbleIntent = buildBubbleIntent(context)
                val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    bubbleIntent,
                    pendingFlags
                ) ?: return null
                val icon = IconCompat.createWithResource(context, R.drawable.ic_stat_vinyl)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    NotificationCompat.BubbleMetadata.Builder(PLAYBACK_SHORTCUT_ID)
                        .setDesiredHeight(600)
                        .setAutoExpandBubble(false)
                        .setSuppressNotification(false)
                        .setIcon(icon)
                        .setIntent(pendingIntent)
                        .build()
                } else {
                    NotificationCompat.BubbleMetadata.Builder(pendingIntent, icon)
                        .setDesiredHeight(600)
                        .setAutoExpandBubble(false)
                        .setSuppressNotification(false)
                        .build()
                }
            }.getOrNull()
        }

        /**
         * Posts or clears the Companion Conversation Notification (ID 1002).
         * On Android 11 to 15, Android's BubbleExtractor requires a notification to use
         * Notification.MessagingStyle and be linked to an approved conversation channel
         * in order to be displayed as a floating bubble or show the bubble icon.
         */
        fun updateCompanionBubbleNotification(
            context: Context,
            title: String,
            artist: String,
            artBitmap: Bitmap?,
            bubbleMetadata: NotificationCompat.BubbleMetadata?,
            platformBubble: Any?,
            sessionActivity: PendingIntent?,
            shouldPost: Boolean,
            isPlaybackActive: Boolean = true,
            capsuleExtras: Bundle? = null
        ) {
            val nm = context.getSystemService(android.app.NotificationManager::class.java) ?: return
            if (!shouldPost || bubbleMetadata == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                runCatching { nm.cancel(BUBBLE_NOTIFICATION_ID) }
                return
            }

            runCatching {
                val convChannel = nm.getNotificationChannel(CONVERSATION_CHANNEL_ID)
                val targetChannel = if (convChannel != null) CONVERSATION_CHANNEL_ID else NOTIFICATION_CHANNEL_ID

                val person = Person.Builder()
                    .setName("HighSpin")
                    .setKey("highspin_playback")
                    .setIcon(IconCompat.createWithResource(context, R.drawable.ic_stat_vinyl))
                    .setBot(false)
                    .setImportant(true)
                    .build()

                val messagingStyle = NotificationCompat.MessagingStyle(person)
                    .addMessage("$title • $artist", System.currentTimeMillis(), person)
                    .setConversationTitle("Now Playing")

                val pendingFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                } else {
                    PendingIntent.FLAG_UPDATE_CURRENT
                }

                val prevIntent = PendingIntent.getService(
                    context,
                    101,
                    Intent(context, PlaybackService::class.java).apply { action = ACTION_PREV },
                    pendingFlags
                )
                val playPauseIntent = PendingIntent.getService(
                    context,
                    102,
                    Intent(context, PlaybackService::class.java).apply { action = ACTION_PLAY_PAUSE },
                    pendingFlags
                )
                val nextIntent = PendingIntent.getService(
                    context,
                    103,
                    Intent(context, PlaybackService::class.java).apply { action = ACTION_NEXT },
                    pendingFlags
                )

                val themeColor = runCatching {
                    val appearance = (context.applicationContext as? LowspinApplication)?.container?.appearanceSettings
                        ?: AppearanceSettings(context)
                    HighSpinThemes.getOrNull(appearance.themeIndex.value)?.primary?.toArgb()
                }.getOrNull() ?: 0xFFA78BFA.toInt()

                val builder = NotificationCompat.Builder(context, targetChannel)
                    .setSmallIcon(R.drawable.ic_stat_vinyl)
                    .setContentTitle(title)
                    .setContentText(artist)
                    .setStyle(messagingStyle)
                    .setShortcutId(PLAYBACK_SHORTCUT_ID)
                    .addPerson(person)
                    .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                    .setBubbleMetadata(bubbleMetadata)
                    .setOngoing(true)
                    .setShowWhen(true)
                    .setOnlyAlertOnce(true)
                    .setColor(themeColor)
                    .setColorized(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                    .addAction(R.drawable.ic_stat_vinyl, "Prev", prevIntent)
                    .addAction(R.drawable.ic_stat_vinyl, if (isPlaybackActive) "Pause" else "Play", playPauseIntent)
                    .addAction(R.drawable.ic_stat_vinyl, "Next", nextIntent)

                if (sessionActivity != null) {
                    builder.setContentIntent(sessionActivity)
                }
                if (artBitmap != null) {
                    builder.setLargeIcon(artBitmap)
                }

                val bubbleNotif = builder.build()
                applyPromotedOngoingViaReflection(bubbleNotif, true)
                bubbleNotif.flags = bubbleNotif.flags or Notification.FLAG_ONGOING_EVENT
                bubbleNotif.color = themeColor

                if (capsuleExtras != null) {
                    bubbleNotif.extras.putAll(capsuleExtras)
                } else {
                    bubbleNotif.extras.apply {
                        putBoolean(EXTRA_OPLUS_ONGOING, isPlaybackActive)
                        putBoolean(EXTRA_OPLUS_CAPSULE, isPlaybackActive)
                        putBoolean(EXTRA_OPLUS_COM_CAPSULE, isPlaybackActive)
                        putString(EXTRA_OPLUS_CAPSULE_TYPE, "music")
                        putString("oplus_capsule_type", "music")
                        putString("com.oplus.capsule.type", "music")
                        putInt(EXTRA_OPLUS_CAPSULE_STATUS, if (isPlaybackActive) 1 else 0)
                        putInt("oplus_capsule_state", if (isPlaybackActive) 1 else 0)
                        putInt("com.oplus.capsule.status", if (isPlaybackActive) 1 else 0)
                        putString(EXTRA_CAPSULE_TITLE, title)
                        putString(EXTRA_CAPSULE_CONTENT, artist)
                        putBoolean(EXTRA_OPLUS_PANTANAL_SUPPORT, true)
                        putBoolean(EXTRA_COM_OPLUS_PANTANAL_SUPPORT, true)
                        putBoolean(EXTRA_COM_OPLUS_PANTANAL_CAPSULE_SUPPORT, true)
                        putBoolean(EXTRA_OPLUS_CAPSULE_SUPPORT, true)
                        putBoolean(EXTRA_COM_OPLUS_LIVEALERT_SUPPORT, true)
                        putBoolean(EXTRA_OPLUS_LIVEALERT_SUPPORT, true)
                        putBoolean(EXTRA_COM_OPLUS_FLUIDCLOUD_SUPPORT, true)
                        putBoolean(EXTRA_OPLUS_FLUIDCLOUD_SUPPORT, true)
                        putString(EXTRA_OPLUS_PANTANAL_TYPE, "music")
                        putInt("com.oplus.pantanal.capsule.status", if (isPlaybackActive) 1 else 0)
                        putBoolean(EXTRA_LIVE_NOTIFICATION, isPlaybackActive)
                        putBoolean(EXTRA_ANDROID_LIVE_NOTIFICATION, isPlaybackActive)
                        putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, isPlaybackActive)
                    }
                }
                bubbleNotif.extras.putBoolean(EXTRA_ANDROID_COLORIZED, true)

                nm.notify(BUBBLE_NOTIFICATION_ID, bubbleNotif)
            }
        }

        /**
         * Safely invokes Android 15 Promoted Ongoing (Live Updates) platform APIs via reflection.
         */
        fun applyPromotedOngoingViaReflection(target: Any, isPromoted: Boolean) {
            runCatching {
                val method = target.javaClass.getMethod("setRequestPromotedOngoing", Boolean::class.javaPrimitiveType)
                method.invoke(target, isPromoted)
            }
            runCatching {
                val field = target.javaClass.getDeclaredField("mRequestPromotedOngoing")
                field.isAccessible = true
                field.set(target, isPromoted)
            }
        }

        /**
         * Resolves a declared field across the class hierarchy, supporting proxy/subclass objects.
         */
        fun findDeclaredField(clazz: Class<*>, fieldName: String): java.lang.reflect.Field? {
            var current: Class<*>? = clazz
            while (current != null && current != Any::class.java) {
                try {
                    return current.getDeclaredField(fieldName)
                } catch (_: NoSuchFieldException) {
                    current = current.superclass
                }
            }
            return null
        }

        /**
         * Directly overrides internal fields (mGroupKey, mShortcutId, mChannelId, mBubbleMetadata)
         * via reflection to guarantee that Promoted Ongoing and Notification Bubble criteria are
         * satisfied even when builder recovery is unavailable or falls back on OEM systems.
         */
        fun applyNotificationInternalOverrides(
            target: Any,
            channelId: String? = null,
            shortcutId: String? = null,
            bubbleMetadata: Any? = null,
            clearGroup: Boolean = true
        ) {
            if (clearGroup) {
                runCatching {
                    findDeclaredField(target.javaClass, "mGroupKey")?.let { field ->
                        field.isAccessible = true
                        field.set(target, null)
                    }
                }
            }
            if (shortcutId != null) {
                runCatching {
                    findDeclaredField(target.javaClass, "mShortcutId")?.let { field ->
                        field.isAccessible = true
                        field.set(target, shortcutId)
                    }
                }
            }
            if (channelId != null) {
                runCatching {
                    findDeclaredField(target.javaClass, "mChannelId")?.let { field ->
                        field.isAccessible = true
                        field.set(target, channelId)
                    }
                }
            }
            if (bubbleMetadata != null) {
                runCatching {
                    findDeclaredField(target.javaClass, "mBubbleMetadata")?.let { field ->
                        field.isAccessible = true
                        field.set(target, bubbleMetadata)
                    }
                }
            }
        }
    }

    private val appearance by lazy {
        (context.applicationContext as? LowspinApplication)?.container?.appearanceSettings
            ?: AppearanceSettings(context)
    }

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
            val processed = runCatching {
                decorateNotification(mediaSession, updatedMediaNotification)
            }.getOrElse { t ->
                android.util.Log.e("HighSpinLiveNotification", "Failed to decorate notification in callback, using raw", t)
                updatedMediaNotification
            }
            onNotificationChangedCallback.onNotificationChanged(processed)
        }

        val baseMediaNotification = defaultProvider.createNotification(
            mediaSession,
            customLayout,
            actionFactory,
            wrappedCallback
        )

        return runCatching {
            decorateNotification(mediaSession, baseMediaNotification)
        }.getOrElse { t ->
            android.util.Log.e("HighSpinLiveNotification", "Failed to decorate base notification, using raw", t)
            baseMediaNotification
        }
    }

    override fun handleCustomCommand(
        mediaSession: MediaSession,
        action: String,
        extras: Bundle
    ): Boolean {
        return defaultProvider.handleCustomCommand(mediaSession, action, extras)
    }

    /**
     * Enforces Android 15 & 16 Promoted Ongoing flags, Notification Bubble metadata, and
     * OnePlus OxygenOS 15 Live Alerts / Fluid Cloud capsule integration.
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

        val rawNotification = mediaNotification.notification
        val bubbleMetadata = if (isLiveEnabled) createBubbleMetadata(context) else null
        val platformBubble = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && bubbleMetadata != null) {
            NotificationCompat.BubbleMetadata.toPlatform(bubbleMetadata)
        } else null

        val currentMediaItem = player.currentMediaItem
        val songId = currentMediaItem?.mediaId?.toLongOrNull()
        var artBitmap = songId?.let { loadCachedArtBitmapSafe(it) }
        @Suppress("DEPRECATION")
        if (artBitmap == null && rawNotification.largeIcon != null) {
            artBitmap = rawNotification.largeIcon
        }

        // DefaultMediaNotificationProvider creates baseMediaNotification with pure MediaStyle,
        // media session token (android.mediaSession), and playback transport actions.
        // We preserve rawNotification directly rather than rebuilding it with a bare
        // NotificationCompat.Builder (which strips MediaStyle and causes Android 15 MediaDataManager
        // and OxygenOS 15 Live Alerts to drop the media capsule).
        val notification = rawNotification
        val themeColor = runCatching {
            val appearance = (context.applicationContext as? LowspinApplication)?.container?.appearanceSettings
                ?: AppearanceSettings(context)
            HighSpinThemes.getOrNull(appearance.themeIndex.value)?.primary?.toArgb()
        }.getOrNull() ?: 0xFFA78BFA.toInt()

        val bloomColor = runCatching {
            if (artBitmap != null) {
                val palette = Palette.from(artBitmap).generate()
                (palette.darkVibrantSwatch ?: palette.vibrantSwatch ?: palette.dominantSwatch)?.rgb
            } else null
        }.getOrNull() ?: themeColor

        notification.color = bloomColor

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                val field = Notification::class.java.getDeclaredField("mForegroundServiceBehavior")
                field.isAccessible = true
                field.setInt(notification, Notification.FOREGROUND_SERVICE_IMMEDIATE)
            }
        }

        // 1. Android 15 & OxygenOS 15 require FLAG_ONGOING_EVENT for status bar capsule and promoted ongoing.
        val flags = computeOngoingFlags(notification.flags, shouldBeOngoing)
        notification.flags = flags

        // 2. High priority transport category & public lockscreen visibility
        notification.category = Notification.CATEGORY_TRANSPORT
        notification.visibility = Notification.VISIBILITY_PUBLIC
        @Suppress("DEPRECATION")
        notification.priority = Notification.PRIORITY_HIGH

        // 3. Status-bar live ticker text
        if (shouldBeOngoing) {
            notification.tickerText = formatTicker(title, artist)
        } else {
            notification.tickerText = null
        }

        // 4. Live Chronometer Ticking for status bar pill & lockscreen cards
        if (shouldBeOngoing && position >= 0L) {
            notification.`when` = System.currentTimeMillis() - position
        } else {
            notification.`when` = System.currentTimeMillis()
        }

        // 5. Safe Album Art Bitmap for large icon if present
        if (artBitmap != null) {
            @Suppress("DEPRECATION")
            notification.largeIcon = artBitmap
            notification.extras.putParcelable(
                Notification.EXTRA_LARGE_ICON_BIG,
                Icon.createWithBitmap(artBitmap)
            )
        }

        // Unconditionally remove media3 groupKey so Android 15 & OxygenOS 15 can promote it cleanly
        notification.extras.remove("android.support.groupKey")
        applyNotificationInternalOverrides(
            target = notification,
            channelId = NOTIFICATION_CHANNEL_ID,
            clearGroup = true
        )

        // 6. Complete Android 15 Promoted Ongoing (Live Updates) & OnePlus OxygenOS 15 Live Alerts extras
        val lyricJson = metadata.extras?.getString("lyricInfo")
            ?: currentMediaItem?.mediaMetadata?.extras?.getString("lyricInfo")
            ?: buildLyricInfoJson(title, artist, album, songId ?: 0L)

        applyPromotedOngoingViaReflection(notification, shouldBeOngoing)

        notification.extras.apply {
            putBoolean(EXTRA_ONGOING, shouldBeOngoing)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                putBoolean(EXTRA_ANDROID_COLORIZED, true)
                putInt("android.color", bloomColor)
            }
            putCharSequence("android.title", title)
            putCharSequence("android.text", artist)
            putCharSequence(EXTRA_SUBST_NAME, "HighSpin")
            putString("songName", title)
            putString("artist", artist)
            putString("album", album)

            putString("lyricInfo", lyricJson)
            putString("lyric_info", lyricJson)
            putString("oplus_lyric_info", lyricJson)

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
                putString("oplus_capsule_type", "music")
                putString("com.oplus.capsule.type", "music")
                putBoolean("oplus_media", true)
                putInt(EXTRA_OPLUS_CAPSULE_STATUS, if (shouldBeOngoing) 1 else 0)
                putInt("oplus_capsule_state", if (shouldBeOngoing) 1 else 0)
                putInt("com.oplus.capsule.status", if (shouldBeOngoing) 1 else 0)
                putString(EXTRA_CAPSULE_TITLE, title)
                putString(EXTRA_CAPSULE_CONTENT, artist)
                putString("capsule_subcontent", album)
                putString("oplus.capsule.title", title)
                putString("oplus.capsule.content", artist)
                putString("com.oplus.capsule.title", title)
                putString("com.oplus.capsule.content", artist)
                putBoolean(EXTRA_LIVE_NOTIFICATION, shouldBeOngoing)
                putBoolean("live_notification", shouldBeOngoing)
                putBoolean("is_live", shouldBeOngoing)
                putBoolean(EXTRA_ANDROID_LIVE_NOTIFICATION, shouldBeOngoing)

                // Fluid Cloud / Pantanal service compatibility
                putBoolean(EXTRA_OPLUS_PANTANAL_SUPPORT, true)
                putBoolean(EXTRA_COM_OPLUS_PANTANAL_SUPPORT, true)
                putBoolean(EXTRA_COM_OPLUS_PANTANAL_CAPSULE_SUPPORT, true)
                putBoolean(EXTRA_OPLUS_CAPSULE_SUPPORT, true)
                putBoolean(EXTRA_COM_OPLUS_LIVEALERT_SUPPORT, true)
                putBoolean(EXTRA_OPLUS_LIVEALERT_SUPPORT, true)
                putBoolean(EXTRA_COM_OPLUS_FLUIDCLOUD_SUPPORT, true)
                putBoolean(EXTRA_OPLUS_FLUIDCLOUD_SUPPORT, true)
                putString(EXTRA_OPLUS_PANTANAL_TYPE, "music")
                putString("com.oplus.pantanal.capsule.type", "music")
                putInt("com.oplus.pantanal.capsule.status", if (shouldBeOngoing) 1 else 0)

                // Icon and action for capsule
                val sessionActivity = mediaSession.sessionActivity
                if (sessionActivity != null) {
                    putParcelable(EXTRA_CAPSULE_ACTION, sessionActivity)
                    putParcelable(EXTRA_OPLUS_CAPSULE_ACTION, sessionActivity)
                    putParcelable("com.oplus.capsule.action", sessionActivity)
                }
                val capsuleIcon = Icon.createWithResource(context, R.drawable.ic_stat_vinyl)
                putParcelable(EXTRA_CAPSULE_ICON, capsuleIcon)
                putParcelable(EXTRA_OPLUS_CAPSULE_ICON, capsuleIcon)
                putParcelable("com.oplus.capsule.icon", capsuleIcon)
            } else {
                putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, false)
                putBoolean(EXTRA_OPLUS_ONGOING, false)
                putBoolean(EXTRA_OPLUS_CAPSULE, false)
                putBoolean(EXTRA_OPLUS_COM_CAPSULE, false)
                putInt(EXTRA_OPLUS_CAPSULE_STATUS, 0)
                putBoolean(EXTRA_LIVE_NOTIFICATION, false)
                putBoolean(EXTRA_ANDROID_LIVE_NOTIFICATION, false)
                putBoolean(EXTRA_OPLUS_PANTANAL_SUPPORT, false)
                putBoolean(EXTRA_COM_OPLUS_PANTANAL_SUPPORT, false)
                putBoolean(EXTRA_COM_OPLUS_PANTANAL_CAPSULE_SUPPORT, false)
                putBoolean(EXTRA_OPLUS_CAPSULE_SUPPORT, false)
                putBoolean(EXTRA_COM_OPLUS_LIVEALERT_SUPPORT, false)
                putBoolean(EXTRA_OPLUS_LIVEALERT_SUPPORT, false)
                putBoolean(EXTRA_COM_OPLUS_FLUIDCLOUD_SUPPORT, false)
                putBoolean(EXTRA_OPLUS_FLUIDCLOUD_SUPPORT, false)
            }

            // Real-time track progress
            val progress = computeProgress(position, duration)
            if (progress != null) {
                putInt(Notification.EXTRA_PROGRESS_MAX, progress.first)
                putInt(Notification.EXTRA_PROGRESS, progress.second)
                putBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false)
            }
        }

        // Keep companion MessagingStyle conversation bubble notification synchronized for Android 15 & OxygenOS 15
        updateCompanionBubbleNotification(
            context = context,
            title = title,
            artist = artist,
            artBitmap = artBitmap,
            bubbleMetadata = bubbleMetadata,
            platformBubble = platformBubble,
            sessionActivity = mediaSession.sessionActivity,
            shouldPost = shouldBeOngoing && isLiveEnabled,
            isPlaybackActive = isPlaybackActive,
            capsuleExtras = notification.extras
        )

        return MediaNotification(mediaNotification.notificationId, notification)
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
