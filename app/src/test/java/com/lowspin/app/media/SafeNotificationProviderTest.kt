package com.lowspin.app.media

import android.app.Notification
import android.os.Bundle
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

@UnstableApi
class SafeNotificationProviderTest {

    private class TestProvider(
        var shouldThrow: Boolean = false,
        val notificationId: Int = 1001,
        var commandResult: Boolean = true
    ) : MediaNotification.Provider {
        var createCount = 0
        var commandCount = 0

        override fun createNotification(
            mediaSession: MediaSession,
            customLayout: ImmutableList<CommandButton>,
            actionFactory: MediaNotification.ActionFactory,
            onNotificationChangedCallback: MediaNotification.Provider.Callback
        ): MediaNotification {
            createCount++
            if (shouldThrow) throw RuntimeException("Primary provider simulated failure")
            return MediaNotification(notificationId, Notification())
        }

        override fun handleCustomCommand(
            session: MediaSession,
            action: String,
            extras: Bundle
        ): Boolean {
            commandCount++
            if (shouldThrow) throw IllegalStateException("Primary command simulated failure")
            return commandResult
        }
    }

    private fun createDummySession(): MediaSession {
        val unsafeClass = Class.forName("sun.misc.Unsafe")
        val theUnsafe = unsafeClass.getDeclaredField("theUnsafe").apply { isAccessible = true }.get(null)
        val allocateInstance = unsafeClass.getMethod("allocateInstance", Class::class.java)
        return allocateInstance.invoke(theUnsafe, MediaSession::class.java) as MediaSession
    }

    private fun createDummyActionFactory(): MediaNotification.ActionFactory {
        return Proxy.newProxyInstance(
            MediaNotification.ActionFactory::class.java.classLoader,
            arrayOf(MediaNotification.ActionFactory::class.java)
        ) { _, _, _ -> null } as MediaNotification.ActionFactory
    }

    @Test
    fun safeNotificationProvider_delegatesToPrimary_whenPrimarySucceeds() {
        val primary = TestProvider(shouldThrow = false, notificationId = 1001)
        val fallback = TestProvider(shouldThrow = false, notificationId = 9999)
        val provider = SafeNotificationProvider(primary, fallback)

        val session = createDummySession()
        val actionFactory = createDummyActionFactory()
        val callback = MediaNotification.Provider.Callback {}

        val result = provider.createNotification(
            session,
            ImmutableList.of(),
            actionFactory,
            callback
        )

        assertEquals(1001, result.notificationId)
        assertEquals(1, primary.createCount)
        assertEquals(0, fallback.createCount)
    }

    @Test
    fun safeNotificationProvider_fallsBackToFallback_whenPrimaryThrows() {
        val primary = TestProvider(shouldThrow = true, notificationId = 1001)
        val fallback = TestProvider(shouldThrow = false, notificationId = 2002)
        val provider = SafeNotificationProvider(primary, fallback)

        val session = createDummySession()
        val actionFactory = createDummyActionFactory()
        val callback = MediaNotification.Provider.Callback {}

        val result = provider.createNotification(
            session,
            ImmutableList.of(),
            actionFactory,
            callback
        )

        assertEquals(2002, result.notificationId)
        assertEquals(1, primary.createCount)
        assertEquals(1, fallback.createCount)
    }

    @Test
    fun safeNotificationProvider_delegatesCustomCommandToPrimary() {
        val primary = TestProvider(shouldThrow = false, commandResult = true)
        val fallback = TestProvider(shouldThrow = false, commandResult = false)
        val provider = SafeNotificationProvider(primary, fallback)

        val session = createDummySession()
        val handled = provider.handleCustomCommand(session, "CUSTOM_ACTION", Bundle())

        assertTrue(handled)
        assertEquals(1, primary.commandCount)
        assertEquals(0, fallback.commandCount)
    }

    @Test
    fun safeNotificationProvider_fallsBackOnCustomCommandFailure() {
        val primary = TestProvider(shouldThrow = true)
        val fallback = TestProvider(shouldThrow = false, commandResult = true)
        val provider = SafeNotificationProvider(primary, fallback)

        val session = createDummySession()
        val handled = provider.handleCustomCommand(session, "CUSTOM_ACTION", Bundle())

        assertTrue(handled)
        assertEquals(1, primary.commandCount)
        assertEquals(1, fallback.commandCount)
    }
}
