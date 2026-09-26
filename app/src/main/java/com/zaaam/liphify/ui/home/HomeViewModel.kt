package com.zaaam.liphify.ui.home

import android.net.Uri
import com.zaaam.liphify.data.local.AppDatabase
import com.zaaam.liphify.domain.model.PlaybackSource
import com.zaaam.liphify.domain.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class HomeUiState(
    val recent: List<Track> = emptyList(),
    val newMusic: List<Track> = emptyList(),
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val db: AppDatabase,
) : ViewModel() {
    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    fun refresh() {
        viewModelScope.launch {
            val rows = withContext(Dispatchers.IO) { db.historyDao().recent(10) }
            val recent = rows.map {
                val src = if (it.source == "youtube" && it.videoId != null) {
                    PlaybackSource.YouTube(it.videoId)
                } else {
                    PlaybackSource.Local(Uri.parse(it.localUri ?: ""))
                }
                Track(it.trackKey, it.title, it.artist, "", 0L, it.artwork, src)
            }
            val added = withContext(Dispatchers.IO) { db.trackDao().recentlyAdded(10) }
            val newMusic = added.mapNotNull { e ->
                withContext(Dispatchers.IO) { db.trackDao().allSongs().find { it.mediaId == e.mediaId } }
            }.map {
                Track("local:${it.mediaId}", it.title, it.artist, it.album, it.durationMs, it.contentUri, PlaybackSource.Local(Uri.parse(it.contentUri)))
            }
            _state.value = HomeUiState(recent, newMusic)
        }
    }
}
