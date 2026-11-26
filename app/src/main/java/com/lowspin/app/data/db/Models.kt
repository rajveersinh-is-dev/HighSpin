package com.lowspin.app.data.db

/**
 * Lightweight projection types used by the library, browse and search screens.
 * Column aliases in the DAO queries match these property names exactly.
 */
data class AlbumSummary(
    val albumId: Long,
    val album: String,
    val artist: String,
    val songCount: Int,
    val totalDurationMs: Long,
    val artSongId: Long,
    val year: Int
)

data class ArtistSummary(
    val artistId: Long,
    val artist: String,
    val songCount: Int,
    val albumCount: Int,
    val artSongId: Long
)

data class FolderSummary(
    val folderName: String,
    val folderPath: String,
    val songCount: Int,
    val artSongId: Long
)

data class PlaylistSummary(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val updatedAt: Long,
    val songCount: Int,
    val artSongId: Long?,
    val customCoverUri: String? = null,
    val gradientColorIndex: Int? = null
)

data class ArtistPlayStat(
    val artist: String,
    val artSongId: Long,
    val playCount: Int,
    val totalDurationMs: Long
)
