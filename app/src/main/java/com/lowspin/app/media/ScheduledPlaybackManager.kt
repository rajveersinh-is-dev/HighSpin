package com.lowspin.app.media

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Calendar

data class ScheduledPlaybackConfig(
    val enabled: Boolean = false,
    val hour: Int = 7,
    val minute: Int = 0,
    val source: String = SOURCE_SHUFFLE_ALL
) {
    companion object {
        const val SOURCE_LAST_QUEUE = "last_queue"
        const val SOURCE_LIKED = "liked"
        const val SOURCE_SHUFFLE_ALL = "shuffle_all"
    }
}

/**
 * Schedules a recurring daily music alarm using AlarmManager that wakes [PlaybackService]
 * with a gentle morning volume fade-in.
 */
class ScheduledPlaybackManager(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("highspin_alarm", Context.MODE_PRIVATE)

    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<ScheduledPlaybackConfig> = _config.asStateFlow()

    private fun loadConfig(): ScheduledPlaybackConfig {
        return ScheduledPlaybackConfig(
            enabled = prefs.getBoolean(KEY_ENABLED, false),
            hour = prefs.getInt(KEY_HOUR, 7).coerceIn(0, 23),
            minute = prefs.getInt(KEY_MINUTE, 0).coerceIn(0, 59),
            source = prefs.getString(KEY_SOURCE, ScheduledPlaybackConfig.SOURCE_SHUFFLE_ALL)
                ?: ScheduledPlaybackConfig.SOURCE_SHUFFLE_ALL
        )
    }

    fun updateSchedule(enabled: Boolean, hour: Int, minute: Int, source: String) {
        val safeHour = hour.coerceIn(0, 23)
        val safeMin = minute.coerceIn(0, 59)
        prefs.edit()
            .putBoolean(KEY_ENABLED, enabled)
            .putInt(KEY_HOUR, safeHour)
            .putInt(KEY_MINUTE, safeMin)
            .putString(KEY_SOURCE, source)
            .apply()

        val updated = ScheduledPlaybackConfig(enabled, safeHour, safeMin, source)
        _config.value = updated

        if (enabled) {
            scheduleNext(safeHour, safeMin)
        } else {
            cancelAlarm()
        }
    }

    fun rescheduleIfEnabled() {
        val current = _config.value
        if (current.enabled) {
            scheduleNext(current.hour, current.minute)
        }
    }

    private fun scheduleNext(hour: Int, minute: Int) {
        val am = appContext.getSystemService(AlarmManager::class.java) ?: return
        val pendingIntent = buildAlarmPendingIntent(appContext)
        val triggerMs = computeNextTriggerMillis(hour, minute, System.currentTimeMillis())

        runCatching {
            val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                am.canScheduleExactAlarms()
            } else true

            if (canExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
            }
        }
    }

    private fun cancelAlarm() {
        val am = appContext.getSystemService(AlarmManager::class.java) ?: return
        runCatching { am.cancel(buildAlarmPendingIntent(appContext)) }
    }

    companion object {
        const val ACTION_ALARM_TRIGGER = "com.lowspin.app.action.ALARM_PLAYBACK"
        const val EXTRA_ALARM_SOURCE = "alarm_source"
        private const val REQUEST_CODE = 4401

        private const val KEY_ENABLED = "alarm_enabled"
        private const val KEY_HOUR = "alarm_hour"
        private const val KEY_MINUTE = "alarm_minute"
        private const val KEY_SOURCE = "alarm_source"

        fun computeNextTriggerMillis(hour: Int, minute: Int, nowMs: Long): Long {
            val cal = Calendar.getInstance().apply {
                timeInMillis = nowMs
                set(Calendar.HOUR_OF_DAY, hour.coerceIn(0, 23))
                set(Calendar.MINUTE, minute.coerceIn(0, 59))
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (cal.timeInMillis <= nowMs) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
            return cal.timeInMillis
        }

        private fun buildAlarmPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, PlaybackAlarmReceiver::class.java).apply {
                action = ACTION_ALARM_TRIGGER
            }
            return PendingIntent.getBroadcast(
                context,
                REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}

class PlaybackAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val ctx = context ?: return
        if (intent?.action != ScheduledPlaybackManager.ACTION_ALARM_TRIGGER) return

        val manager = ScheduledPlaybackManager(ctx)
        val cfg = manager.config.value
        if (!cfg.enabled) return

        // Schedule tomorrow's alarm occurrence immediately
        manager.rescheduleIfEnabled()

        val serviceIntent = Intent(ctx, PlaybackService::class.java).apply {
            action = ScheduledPlaybackManager.ACTION_ALARM_TRIGGER
            putExtra(ScheduledPlaybackManager.EXTRA_ALARM_SOURCE, cfg.source)
        }
        runCatching {
            ContextCompat.startForegroundService(ctx, serviceIntent)
        }
    }
}
