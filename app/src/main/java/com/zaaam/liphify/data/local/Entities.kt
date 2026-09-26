package com.zaaam.liphify.data.local

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey val mediaId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val contentUri: String,
    val dateAdded: Long,
    /** URI artwork album nyata dari MediaStore (null kalau tidak ada). */
    val artworkUri: String? = null,
    /** RELATIVE_PATH saat scan — dasar scoping folder Music/LiPhify. */
    val relativePath: String = "",
    val mimeType: String? = null,
)

@Entity(tableName = "yt_cache")
data class YtCacheEntity(
    @PrimaryKey val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String,
    val durationSec: Long,
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

@Entity(tableName = "playlist_tracks", primaryKeys = ["playlistId", "position"])
data class PlaylistTrackEntity(
    val playlistId: Long,
    val position: Int,
    val trackKey: String,
    val title: String,
    val artist: String,
    val artwork: String?,
    val source: String,
    val localUri: String?,
    val videoId: String?,
)

@Entity(tableName = "history", primaryKeys = ["trackKey"])
data class HistoryEntity(
    val trackKey: String,
    val title: String,
    val artist: String,
    val artwork: String?,
    val source: String,
    val localUri: String?,
    val videoId: String?,
    val lastPlayedAt: Long,
)

@Entity(tableName = "queue", primaryKeys = ["position"])
data class QueueEntity(
    val position: Int,
    val trackKey: String,
    val title: String,
    val artist: String,
    val artwork: String?,
    val source: String,
    val localUri: String?,
    val videoId: String?,
)

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks ORDER BY title ASC")
    suspend fun allSongs(): List<TrackEntity>

    @Query("SELECT COUNT(*) FROM tracks")
    suspend fun count(): Int

    @Query("SELECT * FROM tracks ORDER BY dateAdded DESC LIMIT :limit")
    suspend fun recentlyAdded(limit: Int = 20): List<TrackEntity>

    @Query("SELECT DISTINCT artist FROM tracks WHERE artist != '' ORDER BY artist ASC")
    suspend fun artists(): List<String>

    @Query("SELECT DISTINCT album FROM tracks WHERE album != '' ORDER BY album ASC")
    suspend fun albums(): List<String>

    @Query("SELECT * FROM tracks WHERE title LIKE '%' || :q || '%' ESCAPE '\\' OR artist LIKE '%' || :q || '%' ESCAPE '\\' OR album LIKE '%' || :q || '%' ESCAPE '\\' ORDER BY title ASC LIMIT 100")
    suspend fun search(q: String): List<TrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<TrackEntity>)

    @Query("DELETE FROM tracks WHERE mediaId NOT IN (:keep)")
    suspend fun pruneMissing(keep: List<Long>)

    @Query("DELETE FROM tracks")
    suspend fun clearAll()

    /** Purge satu-kali: hapus baris di luar folder aplikasi. */
    @Query("DELETE FROM tracks WHERE relativePath NOT LIKE 'Music/LiPhify/%'")
    suspend fun purgeNonAppFolder(): Int
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    suspend fun playlists(): List<PlaylistEntity>

    @Insert
    suspend fun create(p: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM playlist_tracks WHERE playlistId = :id ORDER BY position ASC")
    suspend fun tracks(id: Long): List<PlaylistTrackEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun putTrack(t: PlaylistTrackEntity)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :id AND position = :pos")
    suspend fun removeTrack(id: Long, pos: Int)

    @Query("SELECT COUNT(*) FROM playlist_tracks WHERE playlistId = :id")
    suspend fun trackCount(id: Long): Int

    @Query("SELECT COALESCE(MAX(position), -1) FROM playlist_tracks WHERE playlistId = :id")
    suspend fun maxPosition(id: Long): Int
}

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY lastPlayedAt DESC LIMIT :limit")
    suspend fun recent(limit: Int = 20): List<HistoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(h: HistoryEntity)
}

@Dao
interface QueueDao {
    @Query("SELECT * FROM queue ORDER BY position ASC")
    suspend fun load(): List<QueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAll(items: List<QueueEntity>)

    @Query("DELETE FROM queue")
    suspend fun clear()
}

@Dao
interface YtCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: YtCacheEntity)
}
