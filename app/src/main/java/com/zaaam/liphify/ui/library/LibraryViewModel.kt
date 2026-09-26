package com.zaaam.liphify.ui.library

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.zaaam.liphify.data.local.AppDatabase
import com.zaaam.liphify.data.local.MediaStoreScanner
import com.zaaam.liphify.data.repository.toTrack
import com.zaaam.liphify.domain.model.Track
import com.zaaam.liphify.data.repository.MusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.Context

data class LibraryUiState(
    val songs: List<Track> = emptyList(),
    val songCount: Int = 0,
    val artists: List<String> = emptyList(),
    val albums: List<String> = emptyList(),
    val recentlyAdded: List<Track> = emptyList(),
    val playlistNames: List<Pair<Long, String>> = emptyList(),
    val needsPermission: Boolean = false,
    val scanning: Boolean = false,
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val scanner: MediaStoreScanner,
    private val repo: MusicRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state

    init {
        refresh()
    }

    fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun refresh() {
        viewModelScope.launch {
            if (!hasPermission()) {
                _state.value = _state.value.copy(needsPermission = true)
                return@launch
            }
            loadFromDb()
        }
    }

    fun scan() {
        viewModelScope.launch {
            _state.value = _state.value.copy(scanning = true, needsPermission = false)
            try {
                scanner.scan()
            } catch (_: Exception) {
            }
            loadFromDb()
            _state.value = _state.value.copy(scanning = false)
        }
    }

    /** Auto-scan saat pertama buka kalau DB masih kosong (punya izin). */
    fun scanIfEmpty() {
        viewModelScope.launch {
            if (!hasPermission()) {
                _state.value = _state.value.copy(needsPermission = true)
                return@launch
            }
            if (db.trackDao().count() == 0 && !_state.value.scanning) scan()
            else loadFromDb()
        }
    }

    private suspend fun loadFromDb() {
        val songs = repo.localSongs()
        val dao = db.trackDao()
        val recentEntities = dao.recentlyAdded(10)
        _state.value = LibraryUiState(
            songs = songs,
            songCount = songs.size,
            artists = dao.artists(),
            albums = dao.albums(),
            recentlyAdded = recentEntities.map { it.toTrack() },
            playlistNames = db.playlistDao().playlists().map { it.id to it.name },
            needsPermission = false,
            scanning = false,
        )
    }
}
