package com.lowspin.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        QueueItemEntity::class,
        PlaybackStateEntity::class,
        RecentSearchEntity::class,
        ExcludedFolderEntity::class,
        HiddenSongEntity::class,
        SongEqualizerEntity::class,
        PlayEventEntity::class,
        SmartPlaylistEntity::class,
        BookmarkEntity::class
    ],
    version = 9,
    exportSchema = false
)
abstract class LowspinDatabase : RoomDatabase() {

    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun queueDao(): QueueDao
    abstract fun playbackStateDao(): PlaybackStateDao
    abstract fun recentSearchDao(): RecentSearchDao
    abstract fun excludedFolderDao(): ExcludedFolderDao
    abstract fun hiddenSongDao(): HiddenSongDao
    abstract fun songEqualizerDao(): SongEqualizerDao
    abstract fun playEventDao(): PlayEventDao
    abstract fun smartPlaylistDao(): SmartPlaylistDao
    abstract fun bookmarkDao(): BookmarkDao

    companion object {
        @Volatile
        private var instance: LowspinDatabase? = null

        /**
         * Adds the user-managed folder exclusion list. Existing songs, likes,
         * play counts, playlists and the saved queue are preserved.
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `excluded_folders` (
                        `folderPath` TEXT NOT NULL,
                        `folderName` TEXT NOT NULL,
                        `songCount` INTEGER NOT NULL,
                        `excludedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`folderPath`)
                    )
                    """.trimIndent()
                )
            }
        }

        /**
         * Adds the hidden-songs list and a stable match key on each scanned row
         * so removed recordings cannot reappear after a rescan.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `songs` ADD COLUMN `matchKey` TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `hidden_songs` (
                        `songId` INTEGER NOT NULL,
                        `matchKey` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `artist` TEXT NOT NULL,
                        `folderPath` TEXT NOT NULL,
                        `hiddenAt` INTEGER NOT NULL,
                        PRIMARY KEY(`songId`)
                    )
                    """.trimIndent()
                )
            }
        }

        /** Adds the per-album ambient color cache. No existing data is touched. */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `album_visuals` (
                        `albumId` INTEGER NOT NULL,
                        `dateModified` INTEGER NOT NULL,
                        `dominantArgb` INTEGER NOT NULL,
                        `vibrantArgb` INTEGER NOT NULL,
                        `darkMutedArgb` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`albumId`)
                    )
                    """.trimIndent()
                )
            }
        }

        /** Removes the short-lived ambient visual cache; library data is untouched. */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP TABLE IF EXISTS `album_visuals`")
            }
        }

        /**
         * Adds playbackPitch column to playback_state to persist independent
         * audio DSP pitch settings across restarts.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `playback_state` ADD COLUMN `playbackPitch` REAL NOT NULL DEFAULT 1.0"
                )
            }
        }

        /**
         * Adds customCoverUri and gradientColorIndex columns to playlists,
         * and creates song_equalizers table for dedicated per-track EQ memory.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `playlists` ADD COLUMN `customCoverUri` TEXT DEFAULT NULL"
                )
                db.execSQL(
                    "ALTER TABLE `playlists` ADD COLUMN `gradientColorIndex` INTEGER DEFAULT NULL"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `song_equalizers` (
                        `songId` INTEGER NOT NULL,
                        `presetIndex` INTEGER NOT NULL,
                        `bassBoost` INTEGER NOT NULL,
                        `virtualizer` INTEGER NOT NULL,
                        `loudnessGain` INTEGER NOT NULL,
                        `customBands` TEXT,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`songId`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `songs` ADD COLUMN `bpm` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `songs` ADD COLUMN `musicalKey` TEXT NOT NULL DEFAULT ''")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `play_events` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `songId` INTEGER NOT NULL,
                        `playedAt` INTEGER NOT NULL,
                        `durationListenedMs` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_play_events_songId` ON `play_events` (`songId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_play_events_playedAt` ON `play_events` (`playedAt`)")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `smart_playlists` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `rules` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `bookmarks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `songId` INTEGER NOT NULL,
                        `positionMs` INTEGER NOT NULL,
                        `label` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_bookmarks_songId` ON `bookmarks` (`songId`)")
            }
        }

        fun get(context: Context): LowspinDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    LowspinDatabase::class.java,
                    "lowspin.db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9
                    )
                    .build()
                    .also { instance = it }
            }
        }
    }
}
