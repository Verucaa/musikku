package com.zaaam.liphify.ui.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
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
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
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
    private var ticker: Job? = null
    /** Aksi yang datang sebelum controller tersambung — dijalankan saat connect, tidak hilang diam-diam. */
    private val pending = mutableListOf<(MediaController) -> Unit>()
    private var connectAttempts = 0

    private fun withController(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) {
            try {
                block(c)
            } catch (e: Exception) {
                Log.w("LiPhifyPlayer", "controller call gagal", e)
            }
        } else {
            if (pending.size >= 20) pending.removeAt(0)
            pending.add(block)
        }
    }

    init {
        connect()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Restore queue saja (metadata), current dibiarkan null supaya
                // mini-player tidak nampil lagu basi yang belum di-load ke controller.
                val saved = db.queueDao().load()
                    .filter { it.source == "youtube" || !it.localUri.isNullOrBlank() }
                if (saved.isNotEmpty()) {
                    _state.value = _state.value.copy(queue = saved.map { it.toTrack() })
                }
            } catch (e: Exception) {
                Log.w("LiPhifyPlayer", "restore queue gagal", e)
            }
        }
    }

    private fun connect() {
        try {
            val token = SessionToken(context, ComponentName(context, LiPhifySessionService::class.java))
            val future = MediaController.Builder(context, token).buildAsync()
            future.addListener({
                val c = try {
                    future.get()
                } catch (e: Exception) {
                    Log.w("LiPhifyPlayer", "controller connect gagal", e)
                    connectAttempts++
                    if (connectAttempts <= 3) {
                        viewModelScope.launch {
                            delay(3000)
                            connect()
                        }
                    } else {
                        _state.value = _state.value.copy(
                            error = "Tidak bisa tersambung ke layanan putar, restart app",
                        )
                    }
                    return@addListener
                }
                connectAttempts = 0
                controller = c
                c.addListener(playerListener)
                _state.value = _state.value.copy(
                    isPlaying = c.isPlaying,
                    controllerReady = true,
                    shuffleEnabled = c.shuffleModeEnabled,
                    repeatMode = c.repeatMode,
                )
                if (c.isPlaying) startTicker()
                // Sinkronkan track lokal restore ke controller (tanpa autoplay).
                // Track YouTube diskip: URL expired, di-resolve ulang saat di-tap.
                viewModelScope.launch(Dispatchers.IO) {
                    try {
                        val locals = _state.value.queue.filter { it.source is PlaybackSource.Local }
                        if (locals.isNotEmpty() && c.mediaItemCount == 0) {
                            val items = locals.mapNotNull { t ->
                                val uri = (t.source as PlaybackSource.Local).uri.toString()
                                if (uri.isBlank()) return@mapNotNull null
                                LiPhifySessionService.buildMediaItem(t.key, t.title, t.artist, t.artwork, uri)
                            }
                            if (items.isNotEmpty()) {
                                withController { cc ->
                                    cc.setMediaItems(items)
                                    cc.prepare()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("LiPhifyPlayer", "sync restore gagal", e)
                    }
                }
                pending.forEach { block ->
                    try {
                        block(c)
                    } catch (e: Exception) {
                        Log.w("LiPhifyPlayer", "pending gagal", e)
                    }
                }
                pending.clear()
            }, MoreExecutors.directExecutor())
        } catch (e: Exception) {
            Log.w("LiPhifyPlayer", "build controller gagal", e)
        }
    }

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.value = _state.value.copy(isPlaying = isPlaying)
            if (isPlaying) startTicker() else ticker?.cancel()
        }

        override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
            val found = _state.value.queue.find { it.key == item?.mediaId }
            // Fallback: turunkan current dari metadata controller bila queue divergen.
            val track = found ?: item?.let { fallbackTrack(it) }
            if (track != null) {
                if (!_state.value.queue.any { it.key == track.key }) {
                    _state.value = _state.value.copy(queue = _state.value.queue + track)
                }
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

        override fun onPlayerError(error: PlaybackException) {
            _state.value = _state.value.copy(
                isPlaying = false,
                error = "Gagal memutar lagu ini (${error.errorCodeName}), coba lagu lain",
            )
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (true) {
                try {
                    val c = controller
                    if (c != null && c.duration > 0) {
                        _state.value = _state.value.copy(
                            positionMs = c.currentPosition.coerceAtLeast(0),
                            durationMs = c.duration,
                        )
                    }
                } catch (_: Exception) {
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
        viewModelScope.launch {
            val items = mutableListOf<MediaItem>()
            val okQueue = mutableListOf<Track>()
            var failed = 0
            for (t in queue) {
                val url = resolveUrl(t)
                if (url == null) {
                    failed++
                    continue
                }
                items.add(LiPhifySessionService.buildMediaItem(t.key, t.title, t.artist, t.artwork, url))
                okQueue.add(t)
            }
            if (items.isEmpty()) {
                if (_state.value.error == null) {
                    _state.value = _state.value.copy(error = "Semua lagu gagal dimuat, coba lagi")
                }
                return@launch
            }
            val target = if (okQueue.any { it.key == track.key }) track else okQueue[0]
            val startIndex = okQueue.indexOfFirst { it.key == target.key }.coerceAtLeast(0)
            withController { c ->
                c.setMediaItems(items, startIndex, 0)
                c.prepare()
                c.play()
            }
            _state.value = _state.value.copy(queue = okQueue, current = target, error = null)
            persistQueue(okQueue)
            recordHistory(target)
            if (failed > 0) {
                _state.value = _state.value.copy(error = "$failed lagu dilewati (gagal dimuat)")
            }
        }
    }

    fun playNext(track: Track) {
        viewModelScope.launch {
            val url = resolveUrl(track) ?: return@launch
            val item = LiPhifySessionService.buildMediaItem(track.key, track.title, track.artist, track.artwork, url)
            withController { c ->
                val idx = if (c.mediaItemCount > 0) (c.currentMediaItemIndex + 1).coerceIn(0, c.mediaItemCount) else 0
                c.addMediaItem(idx, item)
            }
            val cur = _state.value.current
            val q = _state.value.queue.toMutableList()
            q.removeAll { it.key == track.key }
            val at = if (cur != null) (q.indexOfFirst { it.key == cur.key } + 1).coerceIn(0, q.size) else 0
            q.add(at, track)
            _state.value = _state.value.copy(queue = q)
            persistQueue(q)
        }
    }

    fun addToQueue(track: Track) {
        viewModelScope.launch {
            val url = resolveUrl(track) ?: return@launch
            val item = LiPhifySessionService.buildMediaItem(track.key, track.title, track.artist, track.artwork, url)
            withController { c -> c.addMediaItem(item) }
            val q = _state.value.queue.toMutableList()
            q.removeAll { it.key == track.key }
            q.add(track)
            _state.value = _state.value.copy(queue = q)
            persistQueue(q)
        }
    }

    fun moveQueue(from: Int, to: Int) {
        val q = _state.value.queue
        if (from !in q.indices || to !in q.indices) return
        withController { c ->
            if (from < c.mediaItemCount && to < c.mediaItemCount) {
                c.moveMediaItem(from, to)
            }
        }
        val next = q.toMutableList()
        val t = next.removeAt(from)
        next.add(to, t)
        _state.value = _state.value.copy(queue = next)
        viewModelScope.launch(Dispatchers.IO) { persistQueue(next) }
    }

    fun togglePlayPause() = withController { c ->
        if (c.mediaItemCount == 0) return@withController
        if (c.isPlaying) c.pause() else c.play()
    }

    fun next() = withController { c -> if (c.mediaItemCount > 0) c.seekToNext() }
    fun prev() = withController { c -> if (c.mediaItemCount > 0) c.seekToPrevious() }
    fun seekTo(ms: Long) = withController { c ->
        if (c.duration > 0) c.seekTo(ms.coerceIn(0, c.duration))
    }

    fun setShuffle(v: Boolean) = withController { c -> c.shuffleModeEnabled = v }
    fun setRepeat(mode: Int) = withController { c -> c.repeatMode = mode }

    /** Lazy resolve: lokal = contentUri langsung; YT = re-resolve tiap mau play (URL expired). */
    private suspend fun resolveUrl(t: Track): String? {
        return when (val s = t.source) {
            is PlaybackSource.Local -> {
                val uri = s.uri.toString()
                if (uri.isBlank()) {
                    _state.value = _state.value.copy(error = "File lagu tidak ditemukan, scan ulang Library")
                    null
                } else {
                    uri
                }
            }
            is PlaybackSource.YouTube -> try {
                when (val r = withTimeout(20_000) { repo.resolveStream(s.videoId) }) {
                    is YtResult.Ok -> r.value
                    is YtResult.Fail -> {
                        _state.value = _state.value.copy(error = r.message)
                        null
                    }
                }
            } catch (e: Throwable) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _state.value = _state.value.copy(error = "Gagal ambil data dari YouTube, coba lagi")
                null
            }
        }
    }

    private suspend fun persistQueue(q: List<Track>) {
        try {
            val entities = q.mapIndexed { i, t ->
                val (source, localUri, videoId) = when (val s = t.source) {
                    is PlaybackSource.Local -> Triple("local", s.uri.toString(), null as String?)
                    is PlaybackSource.YouTube -> Triple("youtube", null, s.videoId)
                }
                QueueEntity(i, t.key, t.title, t.artist, t.artwork, source, localUri, videoId)
            }
            androidx.room.withTransaction(db) {
                db.queueDao().clear()
                db.queueDao().saveAll(entities)
            }
        } catch (e: Exception) {
            Log.w("LiPhifyPlayer", "persist queue gagal", e)
        }
    }

    private fun recordHistory(t: Track) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val (source, localUri, videoId) = when (val s = t.source) {
                    is PlaybackSource.Local -> Triple("local", s.uri.toString(), null as String?)
                    is PlaybackSource.YouTube -> Triple("youtube", null, s.videoId)
                }
                db.historyDao().upsert(
                    HistoryEntity(t.key, t.title, t.artist, t.artwork, source, localUri, videoId, System.currentTimeMillis()),
                )
            } catch (e: Exception) {
                Log.w("LiPhifyPlayer", "history gagal", e)
            }
        }
    }

    /** Bentuk Track darurat dari metadata controller (queue divergen). */
    private fun fallbackTrack(item: MediaItem): Track? {
        val meta = item.mediaMetadata ?: return null
        val key = item.mediaId.ifBlank { return null }
        val uri = item.localConfiguration?.uri?.toString() ?: ""
        val src = when {
            key.startsWith("yt:") -> PlaybackSource.YouTube(key.removePrefix("yt:"))
            uri.isNotBlank() -> PlaybackSource.Local(Uri.parse(uri))
            else -> return null
        }
        return Track(
            key = key,
            title = meta.title?.toString() ?: "Unknown",
            artist = meta.artist?.toString() ?: "Unknown",
            artwork = meta.artworkUri?.toString(),
            source = src,
        )
    }

    private fun QueueEntity.toTrack(): Track {
        val src = if (source == "youtube" && videoId != null) {
            PlaybackSource.YouTube(videoId)
        } else {
            PlaybackSource.Local(Uri.parse(localUri ?: ""))
        }
        return Track(trackKey, title, artist, "", 0L, artwork, src)
    }

    override fun onCleared() {
        ticker?.cancel()
        try {
            controller?.release()
        } catch (_: Exception) {
        }
    }
}
