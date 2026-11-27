package com.lowspin.app.data.art

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.LruCache
import androidx.palette.graphics.Palette
import com.lowspin.app.data.db.SongDao
import com.lowspin.app.data.db.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Offline album-art pipeline:
 * memory cache -> disk cache -> embedded ID3 picture -> MediaStore thumbnail.
 * No network loader is involved anywhere.
 */
class AlbumArtRepository(
    private val context: Context,
    private val songDao: SongDao
) {

    private val memoryCache = object : LruCache<String, Bitmap>(memoryCacheSize()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
    }

    /**
     * MediaMetadataRetriever and thumbnail decoding allocate large native
     * buffers. Capping parallel decodes keeps fast scrolling smooth on phones
     * with modest memory instead of firing dozens of decoders at once.
     */
    private val decodeSemaphore = Semaphore(4)

    private val diskDir: File by lazy {
        File(context.cacheDir, "art_cache").apply { mkdirs() }
    }

    suspend fun loadBitmap(songId: Long, sizePx: Int): Bitmap? = withContext(Dispatchers.IO) {
        val key = "$songId@$sizePx"
        memoryCache.get(key)?.let { return@withContext it }

        val file = File(diskDir, "${songId}_${sizePx}.jpg")
        if (file.exists() && file.length() > 0L) {
            val cached = BitmapFactory.decodeFile(file.absolutePath)
            if (cached != null) {
                memoryCache.put(key, cached)
                return@withContext cached
            }
        }

        val decoded = decodeSemaphore.withPermit {
            decodeFromMedia(songId, sizePx)
        } ?: return@withContext null
        runCatching {
            file.outputStream().use { out ->
                decoded.compress(Bitmap.CompressFormat.JPEG, 88, out)
            }
        }
        memoryCache.put(key, decoded)
        decoded
    }

    suspend fun dominantColor(song: SongEntity): Int = withContext(Dispatchers.IO) {
        if (song.dominantColorArgb != 0) return@withContext song.dominantColorArgb

        val bitmap = loadBitmap(song.id, PALETTE_SAMPLE_PX)
        val paletteColor = bitmap?.let { sample ->
            val palette = Palette.from(sample).generate()
            (
                palette.darkVibrantSwatch
                    ?: palette.vibrantSwatch
                    ?: palette.darkMutedSwatch
                    ?: palette.dominantSwatch
                )?.rgb
        }
        val color = paletteColor
            ?: GradientPlaceholder.primaryColor(song.album.ifBlank { song.title })

        songDao.setDominantColor(song.id, color)
        color
    }

    /**
     * Blocking on purpose: called from [ArtworkProvider.openFile], which already
     * runs on a binder thread and needs a real file descriptor for the
     * notification / lock-screen artwork loader.
     */
    fun artworkFileForSongBlocking(songId: Long): File? {
        val dir = File(context.cacheDir, "notification_art").apply { mkdirs() }
        val file = File(dir, "$songId.jpg")
        if (file.exists() && file.length() > 0L) return file

        val song = runBlocking { songDao.getById(songId) } ?: return null
        val bitmap = decodeFromMedia(song.id, NOTIFICATION_ART_PX) ?: return null
        val written = runCatching {
            file.outputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
        }.isSuccess
        return if (written && file.length() > 0L) file else null
    }

    fun clearCaches() {
        memoryCache.evictAll()
        diskDir.deleteRecursively()
        File(context.cacheDir, "notification_art").deleteRecursively()
    }

    fun diskCacheSizeBytes(): Long {
        fun sizeOf(dir: File): Long =
            dir.listFiles()?.sumOf { if (it.isFile) it.length() else sizeOf(it) } ?: 0L
        return sizeOf(diskDir) + sizeOf(File(context.cacheDir, "notification_art"))
    }

    private fun decodeFromFolder(songId: Long, sizePx: Int): Bitmap? {
        val song = runBlocking { songDao.getById(songId) } ?: return null
        if (song.folderPath.isBlank()) return null
        val folder = File(song.folderPath)
        if (!folder.exists() || !folder.isDirectory) return null

        val candidates = listOf("cover.jpg", "cover.png", "folder.jpg", "folder.png", "albumart.jpg", "albumartsmall.jpg", "album.jpg", "album.png")
        val exactFile = candidates.map { File(folder, it) }.firstOrNull { it.exists() && it.isFile }
            ?: folder.listFiles()?.firstOrNull { file ->
                val name = file.name.lowercase()
                (name.startsWith("cover") || name.startsWith("folder") || name.startsWith("album")) &&
                (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png"))
            } ?: return null

        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(exactFile.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= sizePx && bounds.outHeight / (sample * 2) >= sizePx) {
                sample *= 2
            }
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = BitmapFactory.decodeFile(exactFile.absolutePath, options)
            bmp?.let { scaleCrop(it, sizePx) }
        }.getOrNull()
    }

    private fun decodeFromMedia(songId: Long, sizePx: Int): Bitmap? {
        val songUri = ContentUris.withAppendedId(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            songId
        )

        // Embedded artwork is the most reliable source on every API level.
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, songUri)
            val bytes = retriever.embeddedPicture
            if (bytes != null && bytes.isNotEmpty()) {
                decodeBytes(bytes, sizePx)?.let { return it }
            }
        } catch (_: Exception) {
            // Fall through to the platform thumbnail loader.
        } finally {
            runCatching { retriever.release() }
        }

        decodeFromFolder(songId, sizePx)?.let { return it }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                context.contentResolver.loadThumbnail(
                    songUri,
                    android.util.Size(sizePx, sizePx),
                    null
                )
            }.getOrNull()?.let { return scaleCrop(it, sizePx) }
        }
        return null
    }

    private fun decodeBytes(bytes: ByteArray, sizePx: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        var sample = 1
        while (bounds.outWidth / (sample * 2) >= sizePx &&
            bounds.outHeight / (sample * 2) >= sizePx
        ) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        return bitmap?.let { scaleCrop(it, sizePx) }
    }

    private fun scaleCrop(source: Bitmap, sizePx: Int): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= 0 || height <= 0) return source
        val side = minOf(width, height)
        val x = (width - side) / 2
        val y = (height - side) / 2
        val square = Bitmap.createBitmap(source, x, y, side, side)
        return Bitmap.createScaledBitmap(square, sizePx, sizePx, true)
    }

    private fun memoryCacheSize(): Int {
        val eighth = Runtime.getRuntime().maxMemory() / 8
        return eighth.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
    }

    private companion object {
        const val PALETTE_SAMPLE_PX = 96
        const val NOTIFICATION_ART_PX = 512
    }
}
