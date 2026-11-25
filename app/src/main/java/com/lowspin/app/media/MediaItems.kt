package com.lowspin.app.media

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.lowspin.app.data.db.SongEntity

const val ARTWORK_AUTHORITY = "com.lowspin.app.art"

fun SongEntity.artworkUri(): Uri = Uri.parse("content://$ARTWORK_AUTHORITY/$id")

/** MediaStore MIME types are often generic (audio/mpeg); derive from the file name. */
private fun mimeTypeFor(song: SongEntity): String? {
    val name = song.matchKey.substringAfterLast('/').ifBlank { song.title }
    return when {
        name.endsWith(".flac") -> "audio/flac"
        name.endsWith(".wav") || name.endsWith(".wave") -> "audio/wav"
        name.endsWith(".aif") || name.endsWith(".aiff") -> "audio/aiff"
        name.endsWith(".m4a") -> "audio/mp4"
        name.endsWith(".ogg") -> "audio/ogg"
        name.endsWith(".opus") -> "audio/opus"
        else -> song.mimeType.takeIf { it.startsWith("audio/") }
    }
}

/**
 * Escapes characters for safe JSON serialization.
 */
fun escapeJson(s: String): String =
    s.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
        .replace("\b", "\\b")
        .replace("\u000c", "\\f")

/**
 * Builds standard lyricInfo JSON recognized natively by OnePlus / ColorOS lock screen Live Space
 * and dynamic island capsules. Supports both synchronized LRC text and no-lyric fallback states.
 */
fun buildLyricInfoJson(
    title: String,
    artist: String,
    album: String,
    songId: Long,
    lrcContent: String? = null
): String {
    return if (!lrcContent.isNullOrBlank()) {
        """{"songName":"${escapeJson(title)}","artist":"${escapeJson(artist)}","album":"${escapeJson(album)}","songId":"$songId","lyric":"${escapeJson(lrcContent)}","noLyric":false}"""
    } else {
        """{"songName":"${escapeJson(title)}","artist":"${escapeJson(artist)}","album":"${escapeJson(album)}","songId":"$songId","noLyric":true}"""
    }
}

fun buildLyricInfoJson(song: SongEntity, lrcContent: String? = null): String {
    return buildLyricInfoJson(
        title = song.title,
        artist = song.artist,
        album = song.album,
        songId = song.id,
        lrcContent = lrcContent
    )
}

fun SongEntity.toMediaItem(): MediaItem {
    val lyricJson = buildLyricInfoJson(this)
    val extras = Bundle().apply {
        putString("lyricInfo", lyricJson)
        putCharSequence("songName", title)
        putCharSequence("artist", artist)
        putCharSequence("album", album)
        putBoolean("is_live_media", true)
        putBoolean("oplus_media", true)
    }

    val metadata = MediaMetadata.Builder()
        .setTitle(title)
        .setArtist(artist)
        .setAlbumTitle(album)
        .setArtworkUri(artworkUri())
        .setIsBrowsable(false)
        .setIsPlayable(true)
        .setExtras(extras)
        .build()

    return MediaItem.Builder()
        .setMediaId(id.toString())
        .setUri(contentUri)
        .setMimeType(mimeTypeFor(this))
        .setMediaMetadata(metadata)
        .build()
}
