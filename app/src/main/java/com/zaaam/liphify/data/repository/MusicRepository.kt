package com.zaaam.liphify.data.repository

import android.net.Uri
import com.zaaam.liphify.data.local.AppDatabase
import com.zaaam.liphify.data.local.TrackEntity
import com.zaaam.liphify.data.youtube.YouTubeRepository
import com.zaaam.liphify.data.youtube.YtResult
import com.zaaam.liphify.domain.model.PlaybackSource
import com.zaaam.liphify.domain.model.Track
import javax.inject.Inject
import javax.inject.Singleton

/** PRD-011: unified search paralel Room (instant) + NewPipeExtractor (async). */
@Singleton
class MusicRepository @Inject constructor(
    private val db: AppDatabase,
    private val yt: YouTubeRepository,
) {
    suspend fun searchLocal(q: String): List<Track> {
        if (q.isBlank()) return emptyList()
        return try {
            // Escape wildcard LIKE supaya ketikan %/_ tidak meledak jadi full-table.
            val safe = q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
            db.trackDao().search(safe).map { it.toTrack() }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    suspend fun searchYouTube(q: String) = yt.search(q)

    suspend fun localSongs(): List<Track> =
        try {
            db.trackDao().allSongs().map { it.toTrack() }
        } catch (_: Throwable) {
            emptyList()
        }

    suspend fun resolveStream(videoId: String) = yt.resolveAudioUrl(videoId)
}

/** Mapping tunggal entity -> domain. Artwork = URI album art nyata (null = fallback UI). */
fun TrackEntity.toTrack(): Track = Track(
    key = "local:$mediaId",
    title = title,
    artist = artist,
    album = album,
    durationMs = durationMs,
    artwork = artworkUri,
    source = PlaybackSource.Local(Uri.parse(contentUri)),
)
