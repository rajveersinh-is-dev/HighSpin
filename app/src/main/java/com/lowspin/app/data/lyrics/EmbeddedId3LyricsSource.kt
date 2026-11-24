package com.lowspin.app.data.lyrics

import android.content.Context
import android.net.Uri
import com.lowspin.app.data.db.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.charset.Charset

/** Reads USLT (plain) and SYLT (timestamped) frames from ID3v2 tags. */
class EmbeddedId3LyricsSource(
    private val context: Context
) : LyricsSource {

    override suspend fun load(song: SongEntity): Lyrics? = withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(song.contentUri)
            context.contentResolver.openInputStream(uri)?.use { input ->
                val header = ByteArray(HEADER_SIZE)
                if (readFully(input, header) != HEADER_SIZE) return@use null
                if (ascii(header, 0, 3) != "ID3") return@use null

                val version = header[3].toInt() and 0xFF
                if (version !in 3..4) return@use null
                val tagSize = syncSafe(header, 6)
                if (tagSize <= 0 || tagSize > MAX_TAG_BYTES) return@use null

                val tag = ByteArray(tagSize)
                if (readFully(input, tag) != tagSize) return@use null
                parseFrames(tag, version)
            }
        }.getOrNull()
    }

    private fun parseFrames(tag: ByteArray, version: Int): Lyrics? {
        var position = 0
        while (position + FRAME_HEADER_SIZE <= tag.size) {
            val id = ascii(tag, position, 4)
            if (id[0] == '\u0000') break
            val size = if (version == 4) {
                syncSafe(tag, position + 4)
            } else {
                bigEndian32(tag, position + 4)
            }
            position += FRAME_HEADER_SIZE
            if (size <= 0 || position + size > tag.size) break
            val data = tag.copyOfRange(position, position + size)
            when (id) {
                "USLT" -> parseUslt(data)?.let { return it }
                "SYLT" -> parseSylt(data)?.let { return it }
            }
            position += size
        }
        return null
    }

    private fun parseUslt(data: ByteArray): Lyrics? {
        if (data.size < 6) return null
        val encoding = data[0].toInt() and 0xFF
        val descEnd = terminator(data, 4, encoding)
        val start = descEnd + terminatorLength(encoding)
        if (start >= data.size) return null
        val text = decode(data, start, data.size, encoding).trim()
        if (text.isEmpty()) return null
        val lines = text.lineSequence()
            .map { it.trimEnd() }
            .filter { it.isNotBlank() }
            .map { LyricsLine(-1L, it) }
            .toList()
        return if (lines.isEmpty()) null else Lyrics(lines, synced = false)
    }

    private fun parseSylt(data: ByteArray): Lyrics? {
        if (data.size < 8) return null
        val encoding = data[0].toInt() and 0xFF
        val timestampFormat = data[4].toInt() and 0xFF
        val descEnd = terminator(data, 6, encoding)
        var position = descEnd + terminatorLength(encoding)
        val lines = ArrayList<LyricsLine>()

        while (position < data.size) {
            val textEnd = terminator(data, position, encoding)
            val text = decode(data, position, textEnd, encoding).trim()
            position = textEnd + terminatorLength(encoding)
            if (position + 4 > data.size) break
            val timestamp = bigEndian32(data, position)
            position += 4
            if (text.isNotEmpty()) {
                val ms = if (timestampFormat == 2) timestamp.toLong() else timestamp * 1000L
                lines.add(LyricsLine(ms, text))
            }
        }

        return if (lines.isEmpty()) null else Lyrics(lines, synced = true)
    }

    private fun terminator(data: ByteArray, from: Int, encoding: Int): Int {
        var i = from
        while (i < data.size - 1) {
            if (encoding == 1 || encoding == 2) {
                if (data[i] == 0.toByte() && data[i + 1] == 0.toByte()) return i
                i += 2
            } else {
                if (data[i] == 0.toByte()) return i
                i++
            }
        }
        return data.size
    }

    private fun terminatorLength(encoding: Int): Int = if (encoding == 1 || encoding == 2) 2 else 1

    private fun decode(data: ByteArray, from: Int, to: Int, encoding: Int): String {
        if (from >= to) return ""
        val slice = data.copyOfRange(from, to)
        val charset = when (encoding) {
            0 -> Charset.forName("ISO-8859-1")
            3 -> Charsets.UTF_8
            2 -> Charset.forName("UTF-16BE")
            1 -> if (slice.size >= 2 && slice[0] == 0xFF.toByte() && slice[1] == 0xFE.toByte()) {
                Charset.forName("UTF-16LE")
            } else {
                Charset.forName("UTF-16BE")
            }
            else -> Charsets.UTF_8
        }
        return String(slice, charset).trimStart('\uFEFF')
    }

    private fun readFully(input: java.io.InputStream, target: ByteArray): Int {
        var total = 0
        while (total < target.size) {
            val read = input.read(target, total, target.size - total)
            if (read < 0) break
            total += read
        }
        return total
    }

    private fun ascii(bytes: ByteArray, from: Int, length: Int): String =
        String(bytes, from, length, Charsets.US_ASCII)

    private fun syncSafe(bytes: ByteArray, from: Int): Int =
        ((bytes[from].toInt() and 0x7F) shl 21) or
            ((bytes[from + 1].toInt() and 0x7F) shl 14) or
            ((bytes[from + 2].toInt() and 0x7F) shl 7) or
            (bytes[from + 3].toInt() and 0x7F)

    private fun bigEndian32(bytes: ByteArray, from: Int): Int =
        ((bytes[from].toInt() and 0xFF) shl 24) or
            ((bytes[from + 1].toInt() and 0xFF) shl 16) or
            ((bytes[from + 2].toInt() and 0xFF) shl 8) or
            (bytes[from + 3].toInt() and 0xFF)

    private companion object {
        const val HEADER_SIZE = 10
        const val FRAME_HEADER_SIZE = 10
        const val MAX_TAG_BYTES = 2 * 1024 * 1024
    }
}
