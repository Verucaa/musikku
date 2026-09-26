package com.zaaam.liphify.data.local

import android.content.ContentUris
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** PRD-001: scan MediaStore -> index ke Room. */
class MediaStoreScanner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
) {
    suspend fun scan(): Int = withContext(Dispatchers.IO) {
        val items = mutableListOf<TrackEntity>()
        val collection = if (Build.VERSION.SDK_INT >= 29) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        context.contentResolver.query(collection, projection, selection, null, null)?.use { c ->
            val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val addedCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
            val albumArtBase = android.net.Uri.parse("content://media/external/audio/albumart")
            while (c.moveToNext()) {
                val id = c.getLong(idCol)
                val uri = ContentUris.withAppendedId(collection, id).toString()
                val albumId = c.getLong(albumIdCol)
                items.add(
                    TrackEntity(
                        mediaId = id,
                        title = c.getString(titleCol) ?: "Unknown",
                        artist = c.getString(artistCol) ?: "Unknown",
                        album = c.getString(albumCol) ?: "",
                        durationMs = c.getLong(durCol),
                        contentUri = uri,
                        dateAdded = c.getLong(addedCol),
                        artworkUri = if (albumId > 0) {
                            ContentUris.withAppendedId(albumArtBase, albumId).toString()
                        } else {
                            null
                        },
                    ),
                )
            }
        }
        db.trackDao().upsertAll(items)
        if (items.isNotEmpty()) {
            db.trackDao().pruneMissing(items.map { it.mediaId })
        }
        items.size
    }
}
