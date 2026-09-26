package com.zaaam.liphify.ui.search

import com.zaaam.liphify.data.repository.MusicRepository
import com.zaaam.liphify.data.youtube.YtResult
import com.zaaam.liphify.data.youtube.YtTrack
import com.zaaam.liphify.domain.model.PlaybackSource
import com.zaaam.liphify.domain.model.Track
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val local: List<Track> = emptyList(),
    val yt: List<YtTrack> = emptyList(),
    val ytLoading: Boolean = false,
    val ytError: String? = null,
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repo: MusicRepository,
) : androidx.lifecycle.ViewModel() {
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state
    private val queryFlow = MutableStateFlow("")
    private val scope = kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main,
    )

    init {
        scope.launch {
            queryFlow.debounce(150).collectLatest { q ->
                if (q.isBlank()) {
                    _state.value = _state.value.copy(local = emptyList(), yt = emptyList(), ytError = null)
                    return@collectLatest
                }
                val local = repo.searchLocal(q)
                _state.value = _state.value.copy(local = local)
            }
        }
        scope.launch {
            queryFlow.debounce(400).collectLatest { q ->
                if (q.isBlank()) return@collectLatest
                _state.value = _state.value.copy(ytLoading = true, ytError = null)
                when (val r = repo.searchYouTube(q)) {
                    is YtResult.Ok -> _state.value = _state.value.copy(yt = r.value, ytLoading = false)
                    is YtResult.Fail -> _state.value = _state.value.copy(yt = emptyList(), ytLoading = false, ytError = r.message)
                }
            }
        }
    }

    fun onQuery(q: String) {
        queryFlow.value = q
        _state.value = _state.value.copy(query = q)
    }

    fun ytAsTrack(t: YtTrack): Track = Track(
        key = "yt:${t.videoId}",
        title = t.title,
        artist = t.artist.ifBlank { "YouTube" },
        artwork = t.thumbnailUrl.ifBlank { null },
        source = PlaybackSource.YouTube(t.videoId),
    )
}
