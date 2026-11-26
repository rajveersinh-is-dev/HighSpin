package com.lowspin.app.media

import android.os.Bundle
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession

@UnstableApi
class SafeNotificationProvider(
    private val primary: MediaNotification.Provider,
    private val fallback: MediaNotification.Provider
) : MediaNotification.Provider {

    override fun createNotification(
        mediaSession: MediaSession,
        customLayout: com.google.common.collect.ImmutableList<androidx.media3.session.CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback
    ): MediaNotification {
        return try {
            primary.createNotification(mediaSession, customLayout, actionFactory, onNotificationChangedCallback)
        } catch (e: Throwable) {
            android.util.Log.e("SafeNotificationProvider", "Primary provider failed, using fallback", e)
            fallback.createNotification(mediaSession, customLayout, actionFactory, onNotificationChangedCallback)
        }
    }

    override fun handleCustomCommand(
        session: MediaSession,
        action: String,
        extras: Bundle
    ): Boolean {
        return try {
            primary.handleCustomCommand(session, action, extras)
        } catch (e: Throwable) {
            android.util.Log.e("SafeNotificationProvider", "Primary handleCustomCommand failed, using fallback", e)
            fallback.handleCustomCommand(session, action, extras)
        }
    }
}
