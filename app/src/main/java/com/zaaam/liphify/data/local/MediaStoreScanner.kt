package com.zaaam.liphify.data.local

import android.content.ContentUris
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

/** Folder kanonis library: hanya folder ini yang dibaca. */
const val APP_RELATIVE_PATH = "Music/LiPhify/"

/**
 * PRD-001: scan MediaStore TERBATAS ke folder [APP_RELATIVE_PATH] —
 * ringtone/notifikasi/voice note di luar folder tidak masuk.
 */
class MediaStoreScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
) {
    suspend fun scan(): Int = withContext(Dispatchers.IO) {
        try {
            ensureFolder()
            val items = mutableListOf<TrackEntity>()
            val volumes = if (Build.VERSION.SDK_INT >= 29) {
                try {
                    MediaStore.getExternalVolumeNames(context)
                } catch (_: Exception) {
                    setOf(MediaStore.VOLUME_EXTERNAL)
                }
            } else {
                setOf(MediaStore.VOLUME_EXTERNAL)
            }
            for (volume in volumes) {
                try {
                    items += queryVolume(volume)
                } catch (e: Exception) {
                    Log.w("LiPhifyScan", "scan volume $volume gagal", e)
                }
            }
            db.trackDao().upsertAll(items)
            if (items.isNotEmpty()) {
                // Chunk per 500: SQLite batas ~999 bound variables.
                items.map { it.mediaId }.chunked(500).forEach { chunk ->
                    try {
                        db.trackDao().pruneMissing(chunk)
                    } catch (e: Exception) {
                        Log.w("LiPhifyScan", "prune gagal", e)
                    }
                }
            }
            Log.d("LiPhifyScan", "scan selesai: ${items.size} lagu di $APP_RELATIVE_PATH")
            items.size
        } catch (e: Exception) {
            Log.w("LiPhifyScan", "scan gagal", e)
            throw e
        }
    }

    /** Buat folder secara lazy (File API) supaya ada sebelum download/scan. */
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
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0" +
            " AND ${MediaStore.Audio.Media.RELATIVE_PATH} LIKE ?" +
            " AND ${MediaStore.Audio.Media.IS_RINGTONE} = 0" +
            " AND ${MediaStore.Audio.Media.IS_NOTIFICATION} = 0" +
            " AND ${MediaStore.Audio.Media.IS_ALARM} = 0" +
            " AND ${MediaStore.Audio.Media.DURATION} >= ?" +
            " AND ${MediaStore.Audio.Media.SIZE} >= ?"
        val args = arrayOf("$APP_RELATIVE_PATH%", "30000", "50000")
        val albumArtBase = Uri.parse("content://media/external/audio/albumart")
        context.contentResolver.query(collection, projection, selection, args, null)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val addedCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val pathCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)
            val mimeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            Log.d("LiPhifyScan", "volume $volume cursor=${c.count}")
            while (c.moveToNext()) {
                try {
                    val id = c.getLong(idCol)
                    val uri = ContentUris.withAppendedId(collection, id).toString()
                    val albumId = try {
                        c.getLong(albumIdCol)
                    } catch (_: Exception) {
                        0L
                    }
                    items.add(
                        TrackEntity(
                            mediaId = id,
                            title = c.getString(titleCol) ?: "Unknown",
                            artist = c.getString(artistCol) ?: "Unknown",
                            album = c.getString(albumCol) ?: "",
                            durationMs = try {
                                c.getLong(durCol)
                            } catch (_: Exception) {
                                0L
                            },
                            contentUri = uri,
                            dateAdded = try {
                                c.getLong(addedCol)
                            } catch (_: Exception) {
                                0L
                            },
                            artworkUri = if (albumId > 0) {
                                ContentUris.withAppendedId(albumArtBase, albumId).toString()
                            } else {
                                null
                            },
                            relativePath = try {
                                c.getString(pathCol) ?: ""
                            } catch (_: Exception) {
                                ""
                            },
                            mimeType = try {
                                c.getString(mimeCol)
                            } catch (_: Exception) {
                                null
                            },
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
