package com.lowspin.app.data.lyrics

import android.content.Context
import android.os.Environment
import com.lowspin.app.data.db.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.regex.Pattern

object LrcParser {
    private val TIMESTAMP_PATTERN = Pattern.compile("\\[(\\d{1,3}):(\\d{2})(?:[.:](\\d{1,3}))?]")
    private val OFFSET_PATTERN = Pattern.compile("\\[offset:\\s*([+-]?\\d+)\\s*]", Pattern.CASE_INSENSITIVE)
    private val HEADER_PATTERN = Pattern.compile("^\\[[a-zA-Z_-]+:.*]$")

    fun parse(content: String): Lyrics? {
        if (content.isBlank()) return null

        var offsetMs = 0L
        val offsetMatcher = OFFSET_PATTERN.matcher(content)
        if (offsetMatcher.find()) {
            offsetMs = offsetMatcher.group(1)?.toLongOrNull() ?: 0L
        }

        val timedLines = mutableListOf<LyricsLine>()
        val plainLines = mutableListOf<LyricsLine>()
        var hasValidTimestamps = false

        for (rawLine in content.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty() || HEADER_PATTERN.matcher(line).matches()) {
                continue
            }

            val matcher = TIMESTAMP_PATTERN.matcher(line)
            val timestamps = mutableListOf<Long>()
            var lastEnd = 0

            while (matcher.find()) {
                val minutes = matcher.group(1)?.toLongOrNull() ?: 0L
                val seconds = matcher.group(2)?.toLongOrNull() ?: 0L
                val fractionStr = matcher.group(3)
                val fractionMs = when (fractionStr?.length) {
                    null, 0 -> 0L
                    1 -> (fractionStr.toLongOrNull() ?: 0L) * 100L
                    2 -> (fractionStr.toLongOrNull() ?: 0L) * 10L
                    else -> (fractionStr.take(3).toLongOrNull() ?: 0L)
                }

                val timeMs = (minutes * 60_000L + seconds * 1000L + fractionMs + offsetMs).coerceAtLeast(0L)
                timestamps.add(timeMs)
                lastEnd = matcher.end()
            }

            if (timestamps.isNotEmpty()) {
                hasValidTimestamps = true
                val text = line.substring(lastEnd).trim()
                for (timestamp in timestamps) {
                    timedLines.add(LyricsLine(timeMs = timestamp, text = text))
                }
            } else {
                plainLines.add(LyricsLine(timeMs = -1L, text = line))
            }
        }

        return when {
            hasValidTimestamps && timedLines.isNotEmpty() -> {
                timedLines.sortBy { it.timeMs }
                Lyrics(lines = timedLines, synced = true)
            }
            plainLines.isNotEmpty() -> {
                Lyrics(lines = plainLines, synced = false)
            }
            else -> null
        }
    }
}

/**
 * Searches for local `.lrc` files on device storage matching the song.
 *
 * Checks:
 * 1. Matching `.lrc` file in the same folder as the song file.
 * 2. Standard external storage music/lyrics directories.
 */
class LrcLyricsSource(
    private val context: Context
) : LyricsSource {

    override suspend fun load(song: SongEntity): Lyrics? = withContext(Dispatchers.IO) {
        runCatching {
            val candidates = findCandidateLrcFiles(song)
            for (file in candidates) {
                if (file.exists() && file.isFile && file.canRead()) {
                    val text = file.readText(Charsets.UTF_8)
                    val parsed = LrcParser.parse(text)
                    if (parsed != null && parsed.lines.isNotEmpty()) {
                        return@withContext parsed
                    }
                }
            }
            null
        }.getOrNull()
    }

    private fun findCandidateLrcFiles(song: SongEntity): List<File> {
        val candidates = mutableListOf<File>()
        val folder = song.folderPath.trim()
        val baseName = song.title.ifBlank { "unknown" }
        val extStorage = Environment.getExternalStorageDirectory()

        // Extract base audio filename without extension from matchKey if available
        val fileBaseName = song.matchKey.substringAfterLast('/', "")
            .substringBeforeLast('.', "")
            .trim()
            .takeIf { it.isNotBlank() }

        // Direct folder resolution
        if (folder.isNotEmpty()) {
            val fullFolder = when {
                folder.startsWith("/") -> File(folder)
                folder.startsWith("storage/", ignoreCase = true) -> File("/$folder")
                else -> File(extStorage, folder.trim('/'))
            }

            if (fileBaseName != null) {
                candidates.add(File(fullFolder, "$fileBaseName.lrc"))
                candidates.add(File(fullFolder, "$fileBaseName.LRC"))
            }

            candidates.add(File(fullFolder, "$baseName.lrc"))
            candidates.add(File(fullFolder, "$baseName.LRC"))
            candidates.add(File(fullFolder, "${song.artist} - ${song.title}.lrc"))
            candidates.add(File(fullFolder, "${song.artist} - ${song.title}.LRC"))

            // Also check files in folder ending with .lrc with matching name
            fullFolder.listFiles()?.filter { it.extension.equals("lrc", ignoreCase = true) }?.forEach {
                val candidateName = it.nameWithoutExtension
                if (candidateName.equals(baseName, ignoreCase = true) ||
                    (fileBaseName != null && candidateName.equals(fileBaseName, ignoreCase = true)) ||
                    candidateName.contains(baseName, ignoreCase = true)
                ) {
                    candidates.add(it)
                }
            }
        }

        // Standard Music / Download directories
        val standardDirs = listOfNotNull(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            File(extStorage, "Lyrics"),
            File(extStorage, "Music")
        )

        for (dir in standardDirs) {
            if (dir.exists()) {
                if (fileBaseName != null) {
                    candidates.add(File(dir, "$fileBaseName.lrc"))
                }
                candidates.add(File(dir, "$baseName.lrc"))
                candidates.add(File(dir, "${song.artist} - ${song.title}.lrc"))
            }
        }

        return candidates
    }
}
