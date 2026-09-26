package com.zaaam.liphify.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migrasi 2->3 eksplisit (jangan destructive): tambah kolom scoping folder,
 * lalu purge satu-kali baris non-Music/LiPhify. Playlist/history/queue
 * dipertahankan; entri lokal basi jadi orphan dan gagal gracefully di player.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tracks ADD COLUMN relativePath TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE tracks ADD COLUMN mimeType TEXT")
        db.execSQL("DELETE FROM tracks WHERE relativePath NOT LIKE 'Music/LiPhify/%'")
    }
}

@Database(
    entities = [
        TrackEntity::class,
        YtCacheEntity::class,
        PlaylistEntity::class,
        PlaylistTrackEntity::class,
        HistoryEntity::class,
        QueueEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun historyDao(): HistoryDao
    abstract fun queueDao(): QueueDao
    abstract fun ytCacheDao(): YtCacheDao
}
