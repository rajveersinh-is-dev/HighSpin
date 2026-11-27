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
                parseStream(input)
            }
        }.getOrNull()
    }

    internal fun parseStream(input: java.io.InputStream): Lyrics? {
        val head = ByteArray(16)
        val headRead = readFully(input, head)
        if (headRead < 4) return null
        val magic4 = ascii(head, 0, 4)
        val magic3 = ascii(head, 0, 3)

        return when {
            magic3 == "ID3" && headRead >= HEADER_SIZE -> {
                val version = head[3].toInt() and 0xFF
                if (version !in 3..4) return null
                val flags = head[5].toInt() and 0xFF
                val tagSize = syncSafe(head, 6)
                if (tagSize <= 0 || tagSize > MAX_TAG_BYTES) return null

                val tag = ByteArray(tagSize)
                val already = headRead - HEADER_SIZE
                if (already > 0) {
                    val copyLen = minOf(already, tagSize)
                    System.arraycopy(head, HEADER_SIZE, tag, 0, copyLen)
                    if (copyLen < tagSize) {
                        readFully(input, tag, copyLen, tagSize - copyLen)
                    }
                } else {
                    if (readFully(input, tag) != tagSize) return null
                }
                val decodedTag = if ((flags and 0x80) != 0) removeUnsynchronization(tag) else tag
                val startOffset = if ((flags and 0x40) != 0 && decodedTag.size >= 4) {
                    val extSize = if (version == 4) syncSafe(decodedTag, 0) else bigEndian32(decodedTag, 0) + 4
                    extSize.coerceIn(0, decodedTag.size)
                } else 0
                parseFrames(decodedTag, version, startOffset)
            }
            magic4 == "fLaC" -> parseFlacLyrics(input, head, headRead)
            magic4 == "OggS" -> parseOggLyrics(input, head, headRead)
            headRead >= 8 && ascii(head, 4, 4) == "ftyp" -> parseMp4Lyrics(input, head, headRead)
            else -> null
        }
    }

    private fun removeUnsynchronization(bytes: ByteArray): ByteArray {
        val out = ByteArray(bytes.size)
        var w = 0
        var i = 0
        while (i < bytes.size) {
            out[w++] = bytes[i]
            if ((bytes[i].toInt() and 0xFF) == 0xFF && i + 1 < bytes.size && bytes[i + 1] == 0.toByte()) {
                i++
            }
            i++
        }
        return out.copyOf(w)
    }

    private fun parseFrames(tag: ByteArray, version: Int, startOffset: Int = 0): Lyrics? {
        var position = startOffset
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
                "TXXX" -> parseTxxxLyrics(data)?.let { return it }
            }
            position += size
        }
        return null
    }

    private fun parseTxxxLyrics(data: ByteArray): Lyrics? {
        if (data.size < 4) return null
        val encoding = data[0].toInt() and 0xFF
        val descEnd = terminator(data, 1, encoding)
        val desc = decode(data, 1, descEnd, encoding).trim()
        if (!desc.equals("LYRICS", ignoreCase = true) && !desc.equals("UNSYNCEDLYRICS", ignoreCase = true)) {
            return null
        }
        val start = descEnd + terminatorLength(encoding)
        if (start >= data.size) return null
        val text = decode(data, start, data.size, encoding).trim()
        return parsePlainOrLrc(text, "Embedded ID3")
    }

    private fun parseFlacLyrics(input: java.io.InputStream, head: ByteArray, headRead: Int): Lyrics? {
        val buf = readPrefix(input, head, headRead, MAX_TAG_BYTES)
        var pos = 4
        while (pos + 4 <= buf.size) {
            val headerByte = buf[pos].toInt() and 0xFF
            val isLast = (headerByte and 0x80) != 0
            val blockType = headerByte and 0x7F
            val length = ((buf[pos + 1].toInt() and 0xFF) shl 16) or
                ((buf[pos + 2].toInt() and 0xFF) shl 8) or
                (buf[pos + 3].toInt() and 0xFF)
            pos += 4
            if (pos + length > buf.size) break
            if (blockType == 4) { // VORBIS_COMMENT
                parseVorbisCommentBlock(buf, pos, pos + length, "FLAC Vorbis Tag")?.let { return it }
            }
            pos += length
            if (isLast) break
        }
        return null
    }

    private fun parseVorbisCommentBlock(buf: ByteArray, from: Int, to: Int, sourceLabel: String): Lyrics? {
        var pos = from
        if (pos + 4 > to) return null
        val vendorLen = littleEndian32(buf, pos)
        pos += 4 + vendorLen.coerceAtLeast(0)
        if (pos + 4 > to) return null
        val count = littleEndian32(buf, pos)
        pos += 4
        for (i in 0 until count.coerceIn(0, 512)) {
            if (pos + 4 > to) break
            val len = littleEndian32(buf, pos)
            pos += 4
            if (len <= 0 || pos + len > to) break
            val entry = String(buf, pos, len, Charsets.UTF_8)
            pos += len
            val eqIdx = entry.indexOf('=')
            if (eqIdx > 0) {
                val key = entry.substring(0, eqIdx).trim()
                if (key.equals("LYRICS", ignoreCase = true) ||
                    key.equals("UNSYNCEDLYRICS", ignoreCase = true) ||
                    key.equals("SYNCEDLYRICS", ignoreCase = true)
                ) {
                    val value = entry.substring(eqIdx + 1).trim()
                    parsePlainOrLrc(value, sourceLabel)?.let { return it }
                }
            }
        }
        return null
    }

    private fun parseOggLyrics(input: java.io.InputStream, head: ByteArray, headRead: Int): Lyrics? {
        val buf = readPrefix(input, head, headRead, 128 * 1024)
        val text = String(buf, Charsets.UTF_8)
        for (tag in listOf("LYRICS=", "UNSYNCEDLYRICS=", "lyrics=", "unsyncedlyrics=")) {
            val idx = text.indexOf(tag)
            if (idx >= 4) {
                val len = littleEndian32(buf, idx - 4)
                if (len in tag.length..256_000 && idx + len <= buf.size) {
                    val full = String(buf, idx + tag.length, len - tag.length, Charsets.UTF_8).trim()
                    parsePlainOrLrc(full, "Ogg/Opus Tag")?.let { return it }
                }
            }
        }
        return null
    }

    private fun parseMp4Lyrics(input: java.io.InputStream, head: ByteArray, headRead: Int): Lyrics? {
        val buf = readPrefix(input, head, headRead, 1024 * 1024)
        // Search for iTunes lyrics atom ©lyr (0xA9 'l' 'y' 'r') -> inner 'data' atom
        for (i in 0 until buf.size - 16) {
            if (buf[i] == 0xA9.toByte() && buf[i + 1] == 'l'.code.toByte() &&
                buf[i + 2] == 'y'.code.toByte() && buf[i + 3] == 'r'.code.toByte() && i >= 4
            ) {
                val atomSize = bigEndian32(buf, i - 4)
                val end = (i - 4 + atomSize).coerceAtMost(buf.size)
                var pos = i + 4
                while (pos + 16 <= end) {
                    val subSize = bigEndian32(buf, pos)
                    if (subSize < 16 || pos + subSize > end) break
                    if (ascii(buf, pos + 4, 4) == "data") {
                        val strStart = pos + 16
                        val strEnd = pos + subSize
                        val text = String(buf, strStart, strEnd - strStart, Charsets.UTF_8).trim()
                        parsePlainOrLrc(text, "M4A / MP4 Tag")?.let { return it }
                    }
                    pos += subSize
                }
            }
        }
        return null
    }

    private fun parsePlainOrLrc(text: String, sourceLabel: String): Lyrics? {
        val clean = text.trim()
        if (clean.isEmpty()) return null
        val lrc = LrcParser.parse(clean)
        if (lrc != null && lrc.synced && lrc.lines.isNotEmpty()) {
            return lrc.copy(sourceLabel = sourceLabel)
        }
        val lines = clean.lineSequence()
            .map { it.trimEnd() }
            .filter { it.isNotBlank() }
            .map { LyricsLine(-1L, it) }
            .toList()
        return if (lines.isEmpty()) null else Lyrics(lines, synced = false, sourceLabel = sourceLabel)
    }

    private fun readPrefix(input: java.io.InputStream, head: ByteArray, headRead: Int, maxBytes: Int): ByteArray {
        val buf = ByteArray(maxBytes)
        val initial = minOf(headRead, maxBytes)
        System.arraycopy(head, 0, buf, 0, initial)
        val total = initial + readFully(input, buf, initial, maxBytes - initial)
        return if (total == buf.size) buf else buf.copyOf(total)
    }

    private fun parseUslt(data: ByteArray): Lyrics? {
        if (data.size < 6) return null
        val encoding = data[0].toInt() and 0xFF
        val descEnd = terminator(data, 4, encoding)
        val start = descEnd + terminatorLength(encoding)
        if (start >= data.size) return null
        val text = decode(data, start, data.size, encoding).trim()
        return parsePlainOrLrc(text, "Embedded ID3")
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

        return if (lines.isEmpty()) null else Lyrics(lines, synced = true, sourceLabel = "Embedded SYLT")
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

    private fun readFully(
        input: java.io.InputStream,
        target: ByteArray,
        offset: Int = 0,
        length: Int = target.size
    ): Int {
        var total = 0
        while (total < length) {
            val read = input.read(target, offset + total, length - total)
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

    private fun littleEndian32(bytes: ByteArray, from: Int): Int =
        (bytes[from].toInt() and 0xFF) or
            ((bytes[from + 1].toInt() and 0xFF) shl 8) or
            ((bytes[from + 2].toInt() and 0xFF) shl 16) or
            ((bytes[from + 3].toInt() and 0xFF) shl 24)

    private companion object {
        const val HEADER_SIZE = 10
        const val FRAME_HEADER_SIZE = 10
        const val MAX_TAG_BYTES = 2 * 1024 * 1024
    }
}
