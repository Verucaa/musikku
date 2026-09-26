package com.zaaam.liphify.ui.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import com.zaaam.liphify.data.local.AppDatabase
import com.zaaam.liphify.data.local.HistoryEntity
import com.zaaam.liphify.data.local.QueueEntity
import com.zaaam.liphify.data.repository.MusicRepository
import com.zaaam.liphify.data.youtube.YtResult
import com.zaaam.liphify.domain.model.PlaybackSource
import com.zaaam.liphify.domain.model.Track
import com.zaaam.liphify.playback.LiPhifySessionService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val current: Track? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isExpanded: Boolean = false,
    val error: String? = null,
    val queue: List<Track> = emptyList(),
    val showQueue: Boolean = false,
)

@HiltViewModel
class PlaybackViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: AppDatabase,
    private val repo: MusicRepository,
) : androidx.lifecycle.ViewModel() {
    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state

    private var controller: MediaController? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var ticker: Job? = null

    init {
        connect()
        scope.launch(Dispatchers.IO) {
            val saved = db.queueDao().load()
            if (saved.isNotEmpty()) {
                val tracks = saved.map { it.toTrack() }
                _state.value = _state.value.copy(queue = tracks, current = tracks.firstOrNull())
            }
        }
    }

    private fun connect() {
        val token = SessionToken(context, ComponentName(context, LiPhifySessionService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            controller = future.get()
            controller?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _state.value = _state.value.copy(isPlaying = isPlaying)
                    if (isPlaying) startTicker() else ticker?.cancel()
                }

                override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                    val key = item?.mediaId
                    val track = _state.value.queue.find { it.key == key }
                    if (track != null) {
                        _state.value = _state.value.copy(current = track)
                        recordHistory(track)
                    }
                }
            })
            _state.value = _state.value.copy(isPlaying = controller?.isPlaying == true)
        }, MoreExecutors.directExecutor())
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (true) {
                val c = controller
                if (c != null) {
                    _state.value = _state.value.copy(
                        positionMs = c.currentPosition.coerceAtLeast(0),
                        durationMs = c.duration.coerceAtLeast(0).takeIf { it > 0 } ?: _state.value.durationMs,
                    )
                }
                delay(400)
            }
        }
    }

    fun setExpanded(v: Boolean) {
        _state.value = _state.value.copy(isExpanded = v)
    }

    fun toggleQueue(v: Boolean? = null) {
        _state.value = _state.value.copy(showQueue = v ?: !_state.value.showQueue)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    /** Play track tunggal (ganti queue) atau tambah ke queue campur lokal+YT. */
    fun playTrack(track: Track, queue: List<Track> = listOf(track)) {
        scope.launch {
            val items = mutableListOf<MediaItem>()
            for (t in queue) {
                val url = resolveUrl(t) ?: return@launch
                items.add(
                    LiPhifySessionService.buildMediaItem(
                        t.key, t.title, t.artist, t.artwork, url,
                    ),
                )
            }
            val startIndex = queue.indexOfFirst { it.key == track.key }.coerceAtLeast(0)
            controller?.setMediaItems(items, startIndex, 0)
            controller?.prepare()
            controller?.play()
            _state.value = _state.value.copy(queue = queue, current = track, error = null)
            persistQueue(queue)
            recordHistory(track)
        }
    }

    fun playNext(track: Track) {
        scope.launch {
            val url = resolveUrl(track) ?: return@launch
            val c = controller ?: return@launch
            val item = LiPhifySessionService.buildMediaItem(track.key, track.title, track.artist, track.artwork, url)
            val idx = (c.currentMediaItemIndex + 1).coerceAtLeast(0)
            c.addMediaItem(idx, item)
            val q = _state.value.queue.toMutableList()
            q.add(idx.coerceAtMost(q.size), track)
            _state.value = _state.value.copy(queue = q)
            persistQueue(q)
        }
    }

    fun addToQueue(track: Track) {
        scope.launch {
            val url = resolveUrl(track) ?: return@launch
            val item = LiPhifySessionService.buildMediaItem(track.key, track.title, track.artist, track.artwork, url)
            controller?.addMediaItem(item)
            val q = _state.value.queue + track
            _state.value = _state.value.copy(queue = q)
            persistQueue(q)
        }
    }

    fun moveQueue(from: Int, to: Int) {
        controller?.moveMediaItem(from, to)
        val q = _state.value.queue.toMutableList()
        if (from in q.indices && to in q.indices) {
            val t = q.removeAt(from)
            q.add(to, t)
            _state.value = _state.value.copy(queue = q)
            scope.launch(Dispatchers.IO) { persistQueue(q) }
        }
    }

    fun togglePlayPause() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() = scope.launch { controller?.seekToNext() }
    fun prev() = scope.launch { controller?.seekToPrevious() }
    fun seekTo(ms: Long) = scope.launch { controller?.seekTo(ms) }
    fun toggleShuffle() = scope.launch { controller?.let { it.shuffleModeEnabled = !it.shuffleModeEnabled } }
    fun cycleRepeat() = scope.launch {
        controller?.let {
            it.repeatMode = when (it.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    /** Lazy resolve: lokal = contentUri langsung; YT = re-resolve tiap mau play (URL expired). */
    private suspend fun resolveUrl(t: Track): String? {
        return when (val s = t.source) {
            is PlaybackSource.Local -> s.uri.toString()
            is PlaybackSource.YouTube -> when (val r = repo.resolveStream(s.videoId)) {
                is YtResult.Ok -> r.value
                is YtResult.Fail -> {
                    _state.value = _state.value.copy(error = r.message)
                    null
                }
            }
        }
    }

    private suspend fun persistQueue(q: List<Track>) {
        val entities = q.mapIndexed { i, t ->
            val (source, localUri, videoId) = when (val s = t.source) {
                is PlaybackSource.Local -> Triple("local", s.uri.toString(), null as String?)
                is PlaybackSource.YouTube -> Triple("youtube", null, s.videoId)
            }
            QueueEntity(i, t.key, t.title, t.artist, t.artwork, source, localUri, videoId)
        }
        db.queueDao().clear()
        db.queueDao().saveAll(entities)
    }

    private fun recordHistory(t: Track) {
        scope.launch(Dispatchers.IO) {
            val (source, localUri, videoId) = when (val s = t.source) {
                is PlaybackSource.Local -> Triple("local", s.uri.toString(), null as String?)
                is PlaybackSource.YouTube -> Triple("youtube", null, s.videoId)
            }
            db.historyDao().upsert(
                HistoryEntity(t.key, t.title, t.artist, t.artwork, source, localUri, videoId, System.currentTimeMillis()),
            )
        }
    }

    private fun QueueEntity.toTrack(): Track {
        val src = if (source == "youtube" && videoId != null) {
            PlaybackSource.YouTube(videoId)
        } else {
            PlaybackSource.Local(android.net.Uri.parse(localUri ?: ""))
        }
        return Track(trackKey, title, artist, "", 0L, artwork, src)
    }

    override fun onCleared() {
        ticker?.cancel()
        controller?.release()
        scope.cancel()
    }
}
