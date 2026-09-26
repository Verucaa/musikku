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
    val controllerReady: Boolean = false,
    val shuffleEnabled: Boolean = false,
    /** 0=off 1=one 2=all (mirror Player.REPEAT_MODE_*) */
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
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
    /** Aksi yang datang sebelum controller tersambung — dijalankan saat connect, tidak hilang diam-diam. */
    private val pending = mutableListOf<(MediaController) -> Unit>()

    init {
        connect()
        scope.launch(Dispatchers.IO) {
            // Restore queue saja (metadata), current dibiarkan null supaya
            // mini-player tidak nampil lagu basi yang belum di-load ke controller.
            val saved = db.queueDao().load()
            if (saved.isNotEmpty()) {
                _state.value = _state.value.copy(queue = saved.map { it.toTrack() })
            }
        }
    }

    private fun connect() {
        val token = SessionToken(context, ComponentName(context, LiPhifySessionService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            val c = future.get()
            controller = c
            c.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _state.value = _state.value.copy(isPlaying = isPlaying)
                    if (isPlaying) startTicker() else ticker?.cancel()
                }

                override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                    val track = _state.value.queue.find { it.key == item?.mediaId }
                    if (track != null) {
                        _state.value = _state.value.copy(current = track)
                        recordHistory(track)
                    }
                }

                override fun onShuffleModeEnabledChanged(enabled: Boolean) {
                    _state.value = _state.value.copy(shuffleEnabled = enabled)
                }

                override fun onRepeatModeChanged(mode: Int) {
                    _state.value = _state.value.copy(repeatMode = mode)
                }
            })
            _state.value = _state.value.copy(
                isPlaying = c.isPlaying,
                controllerReady = true,
                shuffleEnabled = c.shuffleModeEnabled,
                repeatMode = c.repeatMode,
            )
            pending.forEach { it(c) }
            pending.clear()
        }, MoreExecutors.directExecutor())
    }

    private fun withController(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) block(c) else pending.add(block)
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (true) {
                val c = controller
                if (c != null && c.duration > 0) {
                    _state.value = _state.value.copy(
                        positionMs = c.currentPosition.coerceAtLeast(0),
                        durationMs = c.duration,
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

    fun playTrack(track: Track, queue: List<Track> = listOf(track)) {
        scope.launch {
            val items = mutableListOf<MediaItem>()
            for (t in queue) {
                val url = resolveUrl(t) ?: return@launch
                items.add(LiPhifySessionService.buildMediaItem(t.key, t.title, t.artist, t.artwork, url))
            }
            val startIndex = queue.indexOfFirst { it.key == track.key }.coerceAtLeast(0)
            withController { c ->
                c.setMediaItems(items, startIndex, 0)
                c.prepare()
                c.play()
            }
            _state.value = _state.value.copy(queue = queue, current = track, error = null)
            persistQueue(queue)
            recordHistory(track)
        }
    }

    fun playNext(track: Track) {
        scope.launch {
            val url = resolveUrl(track) ?: return@launch
            val item = LiPhifySessionService.buildMediaItem(track.key, track.title, track.artist, track.artwork, url)
            withController { c ->
                val idx = (c.currentMediaItemIndex + 1).coerceAtLeast(0)
                c.addMediaItem(idx, item)
            }
            val q = _state.value.queue.toMutableList()
            q.add((_state.value.queue.indexOf(_state.value.current) + 1).coerceIn(0, q.size), track)
            _state.value = _state.value.copy(queue = q)
            persistQueue(q)
        }
    }

    fun addToQueue(track: Track) {
        scope.launch {
            val url = resolveUrl(track) ?: return@launch
            val item = LiPhifySessionService.buildMediaItem(track.key, track.title, track.artist, track.artwork, url)
            withController { c -> c.addMediaItem(item) }
            val q = _state.value.queue + track
            _state.value = _state.value.copy(queue = q)
            persistQueue(q)
        }
    }

    fun moveQueue(from: Int, to: Int) {
        val q = _state.value.queue
        if (from !in q.indices || to !in q.indices) return
        withController { c -> c.moveMediaItem(from, to) }
        val next = q.toMutableList()
        val t = next.removeAt(from)
        next.add(to, t)
        _state.value = _state.value.copy(queue = next)
        scope.launch(Dispatchers.IO) { persistQueue(next) }
    }

    fun togglePlayPause() = withController { c -> if (c.isPlaying) c.pause() else c.play() }
    fun next() = withController { c -> c.seekToNext() }
    fun prev() = withController { c -> c.seekToPrevious() }
    fun seekTo(ms: Long) = withController { c -> c.seekTo(ms) }
    fun setShuffle(v: Boolean) = withController { c -> c.shuffleModeEnabled = v }
    fun setRepeat(mode: Int) = withController { c -> c.repeatMode = mode }

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
