package com.zaaam.liphify.ui.playlist

import com.zaaam.liphify.data.local.AppDatabase
import com.zaaam.liphify.data.local.PlaylistEntity
import com.zaaam.liphify.data.local.PlaylistTrackEntity
import com.zaaam.liphify.domain.model.PlaybackSource
import com.zaaam.liphify.domain.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** PRD-009: playlist campuran lokal + YouTube, persist Room. */
@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val db: AppDatabase,
) : androidx.lifecycle.ViewModel() {
    private val _playlists = MutableStateFlow<List<PlaylistEntity>>(emptyList())
    val playlists: StateFlow<List<PlaylistEntity>> = _playlists
    private val scope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main,
    )

    init {
        refresh()
    }

    fun refresh() {
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _playlists.value = db.playlistDao().playlists()
        }
    }

    fun create(name: String) {
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            db.playlistDao().create(PlaylistEntity(name = name, createdAt = System.currentTimeMillis()))
            refresh()
        }
    }

    fun addTrack(playlistId: Long, track: Track) {
        scope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val pos = db.playlistDao().trackCount(playlistId).toInt()
            val (source, localUri, videoId) = when (val s = track.source) {
                is PlaybackSource.Local -> Triple("local", s.uri.toString(), null as String?)
                is PlaybackSource.YouTube -> Triple("youtube", null, s.videoId)
            }
            db.playlistDao().putTrack(
                PlaylistTrackEntity(playlistId, pos, track.key, track.title, track.artist, track.artwork, source, localUri, videoId),
            )
        }
    }
}
