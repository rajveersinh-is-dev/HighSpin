package com.lowspin.app.data.scanner

import java.util.Locale

/**
 * Decides which MediaStore audio rows are real music.
 *
 * Android's READ_MEDIA_AUDIO permission is all-or-nothing, and plenty of OEM
 * recorder apps tag call/voice recordings with IS_MUSIC=1, so the MediaStore
 * flag alone cannot be trusted. These rules reject the well-known recorder
 * folders and file-name patterns. Anything ambiguous can still be hidden by
 * the user in Manage folders.
 */
object TrackFilter {

    private val excludedFolderSegments = setOf(
        "call",
        "calls",
        "call rec",
        "call recs",
        "callrecord",
        "callrecords",
        "callrec",
        "call_rec",
        "call recorder",
        "callrecorder",
        "call recordings",
        "callrecordings",
        "voice recorder",
        "voicerecorder",
        "voice recordings",
        "sound recorder",
        "soundrecorder",
        "audio recorder",
        "audiorecorder",
        "voice notes",
        "voicenotes",
        "voice memo",
        "voicememo",
        "whatsapp audio",
        "whatsapp voice notes",
        "telegram audio",
        "telegram voice",
        "ptt",
        "reel audio",
        "reelaudio",
        "screen recording",
        "screen recordings",
        "screencast",
        "standard recording",
        "recordings/call",
        "recordings/calls",
        "recordings/voice",
        "recordings/recorder"
    )

    /** Substrings that only ever appear in recorder paths, matched on the full path. */
    private val excludedFolderFragments = listOf(
        "call recording",
        "callrecord",
        "call_rec",
        "voice recorder",
        "sound recorder",
        "audio recorder",
        "voice notes",
        "voicenotes",
        "whatsapp audio",
        "whatsapp voice",
        "telegram audio",
        "telegram voice",
        "sound_recorder",
        "reel audio",
        "reelaudio",
        "screen recording",
        "standard recording",
        "recordings/call",
        "recordings/voice"
    )

    private val excludedFileNamePrefixes = listOf(
        "call_",
        "call-",
        "callrec",
        "call_rec",
        "record_",
        "rec_",
        "rec-",
        "voice_",
        "voice-",
        "vn_",
        "aud-",
        "ptt-",
        "reelaudio",
        "reel audio",
        "reel_",
        "standard recording",
        "screen recording",
        "screencast",
        "screenrec"
    )

    private val excludedTitlePrefixes = listOf(
        "call recording",
        "call with ",
        "recorded call",
        "voice recording",
        "voice note",
        "standard recording",
        "reelaudio",
        "reel audio",
        "screen recording",
        "screencast"
    )

    fun isRecording(folderPath: String, fileName: String, title: String): Boolean {
        val path = normalizePath(folderPath)
        if (path.isNotEmpty()) {
            val segments = path.split('/').filter { it.isNotBlank() }
            if (segments.any { it in excludedFolderSegments }) return true
            if (excludedFolderFragments.any { path.contains(it) }) return true
        }

        val name = fileName.lowercase(Locale.ROOT)
        if (name.isNotEmpty() && excludedFileNamePrefixes.any { name.startsWith(it) }) return true

        val cleanTitle = title.trim().lowercase(Locale.ROOT)
        return cleanTitle.isNotEmpty() && excludedTitlePrefixes.any { cleanTitle.startsWith(it) }
    }

    /** Stable key used to keep user-hidden files hidden across rescans. */
    fun matchKey(folderPath: String, fileName: String): String =
        normalizePath("$folderPath/$fileName")

    fun normalizePath(path: String): String =
        path.replace('\\', '/')
            .trim()
            .replace(Regex("/+"), "/")
            .trim('/')
            .lowercase(Locale.ROOT)
}
