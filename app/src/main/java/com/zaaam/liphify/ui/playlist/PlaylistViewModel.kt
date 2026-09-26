package com.zaaam.liphify.ui.playlist

import com.zaaam.liphify.data.local.AppDatabase
import com.zaaam.liphify.data.local.PlaylistEntity
import com.zaaam.liphify.data.local.PlaylistTrackEntity
import androidx.room.withTransaction
import com.zaaam.liphify.domain.model.PlaybackSource
import com.zaaam.liphify.domain.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** PRD-009: playlist campuran lokal + YouTube, persist Room. */
@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val db: AppDatabase,
) : ViewModel() {
    private val _playlists = MutableStateFlow<List<PlaylistEntity>>(emptyList())
    val playlists: StateFlow<List<PlaylistEntity>> = _playlists

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                _playlists.value = db.playlistDao().playlists()
            } catch (e: Exception) {
                android.util.Log.w("LiPhifyPl", "refresh gagal", e)
            }
        }
    }

    fun create(name: String) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                db.playlistDao().create(PlaylistEntity(name = name, createdAt = System.currentTimeMillis()))
                refresh()
            } catch (e: Exception) {
                android.util.Log.w("LiPhifyPl", "create gagal", e)
            }
        }
    }

    fun addTrack(playlistId: Long, track: Track) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val pos = db.playlistDao().maxPosition(playlistId) + 1
                val (source, localUri, videoId) = when (val s = track.source) {
                    is PlaybackSource.Local -> Triple("local", s.uri.toString(), null as String?)
                    is PlaybackSource.YouTube -> Triple("youtube", null, s.videoId)
                }
                db.playlistDao().putTrack(
                    PlaylistTrackEntity(playlistId, pos, track.key, track.title, track.artist, track.artwork, source, localUri, videoId),
                )
                refresh()
            } catch (e: Exception) {
                android.util.Log.w("LiPhifyPl", "addTrack gagal", e)
            }
        }
    }

    suspend fun tracksOf(id: Long) = try {
        db.playlistDao().tracks(id)
    } catch (e: Exception) {
        android.util.Log.w("LiPhifyPl", "tracks gagal", e)
        emptyList()
    }

    fun removeTrack(playlistId: Long, pos: Int) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                db.playlistDao().removeTrack(playlistId, pos)
                refresh()
            } catch (e: Exception) {
                android.util.Log.w("LiPhifyPl", "removeTrack gagal", e)
            }
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                androidx.room.withTransaction(db) {
                    db.playlistDao().deleteAllTracks(id)
                    db.playlistDao().delete(id)
                }
                refresh()
            } catch (e: Exception) {
                android.util.Log.w("LiPhifyPl", "delete gagal", e)
            }
        }
    }
}
