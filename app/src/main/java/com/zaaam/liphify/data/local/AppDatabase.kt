package com.zaaam.liphify.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** 1->2: kolom artwork. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tracks ADD COLUMN artworkUri TEXT")
    }
}

/**
 * 2->3: kolom scoping folder. SENGAJA tanpa DELETE — purge baris non-folder
 * dilakukan eksplisit pasca-scan (purgeNonAppFolder), bukan di migrasi,
 * supaya migrasi tidak pernah menghancurkan data.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE tracks ADD COLUMN relativePath TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE tracks ADD COLUMN mimeType TEXT")
    }
}

/**
 * 3->4: PK mediaId (tabrakan antar-volume) -> rowKey=contentUri (unik per volume).
 * SQLite tidak bisa ubah PK in-place: recreate tabel, data terbawa.
 */
val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE tracks_new (" +
                "rowKey TEXT NOT NULL PRIMARY KEY, " +
                "volume TEXT NOT NULL DEFAULT '', " +
                "mediaId INTEGER NOT NULL DEFAULT 0, " +
                "title TEXT NOT NULL, artist TEXT NOT NULL, album TEXT NOT NULL, " +
                "durationMs INTEGER NOT NULL, contentUri TEXT NOT NULL, " +
                "dateAdded INTEGER NOT NULL, artworkUri TEXT, " +
                "relativePath TEXT NOT NULL DEFAULT '', mimeType TEXT)",
        )
        db.execSQL(
            "INSERT INTO tracks_new (rowKey, volume, mediaId, title, artist, album, " +
                "durationMs, contentUri, dateAdded, artworkUri, relativePath, mimeType) " +
                "SELECT contentUri, '', mediaId, title, artist, album, durationMs, " +
                "contentUri, dateAdded, artworkUri, relativePath, mimeType FROM tracks",
        )
        db.execSQL("DROP TABLE tracks")
        db.execSQL("ALTER TABLE tracks_new RENAME TO tracks")
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
    version = 4,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun historyDao(): HistoryDao
    abstract fun queueDao(): QueueDao
    abstract fun ytCacheDao(): YtCacheDao
}
