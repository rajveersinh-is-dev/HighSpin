package com.lowspin.app.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "songs",
    indices = [
        Index("albumId"),
        Index("artistId"),
        Index("title"),
        Index("playCount"),
        Index("lastPlayedAt"),
        Index("isLiked"),
        Index("folderName")
    ]
)
data class SongEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val artistId: Long,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val trackNumber: Int,
    val discNumber: Int,
    val year: Int,
    val folderPath: String,
    val folderName: String,
    val contentUri: String,
    val mimeType: String,
    val dateAdded: Long,
    val dateModified: Long,
    val isLiked: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedAt: Long = 0L,
    val dominantColorArgb: Int = 0,
    val metadataEnriched: Boolean = false,
    val lastSeenScan: Long = 0L,
    /** Stable folder + filename key so a hidden file stays hidden after a rescan. */
    val matchKey: String = ""
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long
)

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("playlistId"), Index("songId")]
)
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: Long,
    val position: Int,
    val addedAt: Long
)

@Entity(
    tableName = "queue_items",
    foreignKeys = [
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("songId"), Index("position")]
)
data class QueueItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val songId: Long,
    val position: Int,
    val originalIndex: Int,
    val addedAt: Long
)

@Entity(tableName = "playback_state")
data class PlaybackStateEntity(
    @PrimaryKey val id: Int = 1,
    val currentQueuePosition: Int = 0,
    val positionMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = 0,
    val playbackSpeed: Float = 1f,
    val playbackPitch: Float = 1f,
    val sourceLabel: String = "Your library",
    val updatedAt: Long = 0L
)

@Entity(tableName = "recent_searches")
data class RecentSearchEntity(
    @PrimaryKey val query: String,
    val searchedAt: Long
)

/**
 * Folders the user has hidden from the library (call recordings, voice notes,
 * audiobooks, anything they do not want in the music list). The scanner skips
 * these paths entirely; toggling an entry triggers a rescan.
 */
@Entity(tableName = "excluded_folders")
data class ExcludedFolderEntity(
    @PrimaryKey val folderPath: String,
    val folderName: String,
    val songCount: Int,
    val excludedAt: Long
)

/** Songs the user removed from the library. The underlying file is never touched. */
@Entity(tableName = "hidden_songs")
data class HiddenSongEntity(
    @PrimaryKey val songId: Long,
    val matchKey: String,
    val title: String,
    val artist: String,
    val folderPath: String,
    val hiddenAt: Long
)

