package com.lowspin.app.media

import android.content.ComponentName
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.lowspin.app.R

@RequiresApi(Build.VERSION_CODES.N)
class HighSpinQsTile : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        queryStateAndUpdateTile()
    }

    override fun onClick() {
        super.onClick()
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        future.addListener({
            try {
                val controller = future.get()
                if (controller.isPlaying) {
                    controller.pause()
                    updateTile(false, controller.currentMediaItem?.mediaMetadata?.title?.toString())
                } else {
                    controller.play()
                    updateTile(true, controller.currentMediaItem?.mediaMetadata?.title?.toString())
                }
            } catch (_: Exception) {
                updateTile(false, null)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun queryStateAndUpdateTile() {
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        future.addListener({
            try {
                val controller = future.get()
                val isPlaying = controller.isPlaying
                val title = controller.currentMediaItem?.mediaMetadata?.title?.toString()
                updateTile(isPlaying, title)
            } catch (_: Exception) {
                updateTile(false, null)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun updateTile(isPlaying: Boolean, title: String?) {
        val tile = qsTile ?: return
        tile.state = if (isPlaying) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = if (title.isNullOrBlank()) "HighSpin" else title
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            tile.subtitle = if (isPlaying) "Playing" else "Paused"
        }
        tile.icon = Icon.createWithResource(this, R.drawable.ic_stat_vinyl)
        tile.updateTile()
    }
}
