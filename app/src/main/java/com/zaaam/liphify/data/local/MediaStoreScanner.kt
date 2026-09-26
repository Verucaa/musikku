package com.zaaam.liphify.data.local

import android.content.ContentUris
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/** Folder kanonis library: hanya folder ini yang dibaca. */
const val APP_RELATIVE_PATH = "Music/LiPhify/"

/**
 * PRD-001: scan MediaStore TERBATAS ke folder [APP_RELATIVE_PATH] —
 * ringtone/notifikasi/voice note di luar folder tidak masuk.
 * Single-flight (Mutex): double-tap Refresh tidak interleave.
 */
class MediaStoreScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
) {
    private val mutex = Mutex()

    suspend fun scan(): Int = withContext(Dispatchers.IO) {
        mutex.withLock {
            ensureFolder()
            val items = mutableListOf<TrackEntity>()
            val volumes = if (Build.VERSION.SDK_INT >= 29) {
                try {
                    MediaStore.getExternalVolumeNames(context)
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    Log.w("LiPhifyScan", "getExternalVolumeNames gagal", e)
                    setOf(MediaStore.VOLUME_EXTERNAL)
                }
            } else {
                setOf(MediaStore.VOLUME_EXTERNAL)
            }
            for (volume in volumes) {
                try {
                    items += queryVolume(volume)
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    Log.w("LiPhifyScan", "scan volume $volume gagal", e)
                }
            }
            // Sinkronisasi atomik: upsert + hapus yang hilang + purge non-folder.
            db.withTransaction {
                db.trackDao().upsertAll(items)
                val keep = items.map { it.contentUri }.toSet()
                val toDelete = db.trackDao().existingUris().filter { it !in keep }
                toDelete.chunked(500).forEach { chunk ->
                    db.trackDao().deleteByUris(chunk)
                }
                try {
                    val purged = db.trackDao().purgeNonAppFolder()
                    if (purged > 0) Log.d("LiPhifyScan", "purge non-folder: $purged baris")
                } catch (e: Exception) {
                    Log.w("LiPhifyScan", "purge gagal", e)
                }
            }
            Log.d("LiPhifyScan", "scan selesai: ${items.size} lagu di $APP_RELATIVE_PATH")
            items.size
        }
    }

    /** Buat folder secara lazy (best-effort) supaya ada sebelum download/scan. */
    private fun ensureFolder() {
        try {
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                "LiPhify",
            )
            if (!dir.exists()) {
                dir.mkdirs()
                MediaScannerConnection.scanFile(context, arrayOf(dir.absolutePath), null, null)
            }
        } catch (e: Exception) {
            Log.w("LiPhifyScan", "ensureFolder gagal", e)
        }
    }

    private fun queryVolume(volume: String): List<TrackEntity> {
        val items = mutableListOf<TrackEntity>()
        val collection = MediaStore.Audio.Media.getContentUri(volume)
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE,
        )
        // NOCASE + varian tanpa trailing slash: sebagian OEM menulis path berbeda.
        // NULL durasi/size ditoleransi (OR IS NULL) agar tidak buang lagu sah diam-diam.
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0" +
            " AND (${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ? ESCAPE '\\' COLLATE NOCASE" +
            " OR ${MediaStore.Audio.Media.RELATIVE_PATH} = ? COLLATE NOCASE)" +
            " AND ${MediaStore.Audio.Media.IS_RINGTONE} = 0" +
            " AND ${MediaStore.Audio.Media.IS_NOTIFICATION} = 0" +
            " AND ${MediaStore.Audio.Media.IS_ALARM} = 0" +
            " AND (${MediaStore.Audio.Media.DURATION} IS NULL OR ${MediaStore.Audio.Media.DURATION} >= 30000)" +
            " AND (${MediaStore.Audio.Media.SIZE} IS NULL OR ${MediaStore.Audio.Media.SIZE} >= 50000)"
        val args = arrayOf("Music/LiPhify/%", "Music/LiPhify")
        val albumArtBase = Uri.parse("content://media/external/audio/albumart")
        context.contentResolver.query(collection, projection, selection, args, null)?.use { c ->
            // getColumnIndex (bukan OrThrow): satu kolom hilang di OEM tidak
            // menggugurkan seluruh volume.
            val idCol = c.getColumnIndex(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndex(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndex(MediaStore.Audio.Media.ARTIST)
            val albumCol = c.getColumnIndex(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = c.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
            val durCol = c.getColumnIndex(MediaStore.Audio.Media.DURATION)
            val addedCol = c.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
            val pathCol = c.getColumnIndex(MediaStore.Audio.Media.RELATIVE_PATH)
            val mimeCol = c.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
            if (idCol < 0) {
                Log.w("LiPhifyScan", "volume $volume tanpa _ID, diskip")
                return emptyList()
            }
            Log.d("LiPhifyScan", "volume $volume cursor=${c.count}")
            fun str(col: Int): String? = if (col < 0) null else try {
                c.getString(col)
            } catch (_: Exception) {
                null
            }
            fun lng(col: Int): Long = if (col < 0) 0L else try {
                c.getLong(col)
            } catch (_: Exception) {
                0L
            }
            while (c.moveToNext()) {
                try {
                    val id = c.getLong(idCol)
                    val uri = ContentUris.withAppendedId(collection, id).toString()
                    val albumId = lng(albumIdCol)
                    items.add(
                        TrackEntity(
                            rowKey = uri,
                            volume = volume,
                            mediaId = id,
                            title = str(titleCol) ?: "Unknown",
                            artist = str(artistCol) ?: "Unknown",
                            album = str(albumCol) ?: "",
                            durationMs = lng(durCol),
                            contentUri = uri,
                            dateAdded = lng(addedCol),
                            artworkUri = if (albumId > 0) {
                                ContentUris.withAppendedId(albumArtBase, albumId).toString()
                            } else {
                                null
                            },
                            relativePath = str(pathCol) ?: "",
                            mimeType = str(mimeCol),
                        ),
                    )
                } catch (e: Exception) {
                    Log.w("LiPhifyScan", "baris rusak diskip", e)
                }
            }
        }
        return items
    }
}
