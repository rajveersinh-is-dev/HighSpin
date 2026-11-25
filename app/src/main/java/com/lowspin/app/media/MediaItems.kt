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
 * Escapes characters for safe JSON serialization compliant with RFC 8259.
 * Handles quotation mark, reverse solidus, and all control characters (U+0000..U+001F).
 */
fun escapeJson(s: String): String {
    val sb = StringBuilder(s.length + 16)
    for (i in 0 until s.length) {
        val ch = s[i]
        when (ch) {
            '\\' -> sb.append("\\\\")
            '"' -> sb.append("\\\"")
            '\n' -> sb.append("\\n")
            '\r' -> sb.append("\\r")
            '\t' -> sb.append("\\t")
            '\b' -> sb.append("\\b")
            '\u000c' -> sb.append("\\f")
            in '\u0000'..'\u001f' -> {
                val hex = ch.code.toString(16).padStart(4, '0')
                sb.append("\\u").append(hex)
            }
            else -> sb.append(ch)
        }
    }
    return sb.toString()
}

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
    val safeTitle = title.ifBlank { "HighSpin" }
    val safeArtist = artist.ifBlank { "Local Audio" }
    val safeAlbum = album.ifBlank { "HighSpin" }
    return if (!lrcContent.isNullOrBlank()) {
        """{"songName":"${escapeJson(safeTitle)}","artist":"${escapeJson(safeArtist)}","album":"${escapeJson(safeAlbum)}","songId":"$songId","lyric":"${escapeJson(lrcContent)}","noLyric":false}"""
    } else {
        """{"songName":"${escapeJson(safeTitle)}","artist":"${escapeJson(safeArtist)}","album":"${escapeJson(safeAlbum)}","songId":"$songId","noLyric":true}"""
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
