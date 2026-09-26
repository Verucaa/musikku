package com.zaaam.liphify.data.repository

import android.net.Uri
import com.zaaam.liphify.data.local.AppDatabase
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
        return db.trackDao().search(q).map {
            Track(
                key = "local:${it.mediaId}",
                title = it.title,
                artist = it.artist,
                album = it.album,
                durationMs = it.durationMs,
                artwork = it.contentUri,
                source = PlaybackSource.Local(Uri.parse(it.contentUri)),
            )
        }
    }

    suspend fun searchYouTube(q: String) = yt.search(q)

    suspend fun localSongs(): List<Track> =
        db.trackDao().allSongs().map {
            Track(
                key = "local:${it.mediaId}",
                title = it.title,
                artist = it.artist,
                album = it.album,
                durationMs = it.durationMs,
                artwork = it.contentUri,
                source = PlaybackSource.Local(Uri.parse(it.contentUri)),
            )
        }

    suspend fun resolveStream(videoId: String) = yt.resolveAudioUrl(videoId)
}
