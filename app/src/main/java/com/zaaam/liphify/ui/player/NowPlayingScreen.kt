package com.zaaam.liphify.ui.player

import android.media.AudioManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

/**
 * PRD-005: Now Playing full-screen ala Apple Music.
 * Background = artwork yang di-blur (fallback gradient netral), seekbar real,
 * volume real via AudioManager, Shuffle/Repeat di panel queue (PRD-008).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NowPlayingScreen(
    state: PlayerUiState,
    player: PlaybackViewModel,
    snack: SnackbarHostState,
    plVm: com.zaaam.liphify.ui.playlist.PlaylistViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
) {
    val cur = state.current
    val ctx = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var menu by remember { mutableStateOf<com.zaaam.liphify.domain.model.Track?>(null) }
    val pls by plVm.playlists.collectAsState()

    LaunchedEffect(state.error) {
        if (state.error != null) {
            snack.showSnackbar(state.error)
            player.clearError()
        }
    }

    Box(Modifier.fillMaxSize()) {
        // Background: artwork blur + overlay gelap (fallback gradient netral).
        Box(
            Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF2C2C2E), Color(0xFF1C1C1E)))),
        )
        if (cur?.artwork != null) {
            AsyncImage(
                model = cur.artwork,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().blur(60.dp).graphicsLayer { scaleX = 1.25f; scaleY = 1.25f },
            )
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))
        }
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { player.setExpanded(false) }) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Tutup")
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { if (cur != null) menu = cur }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Opsi")
                }
            }
            if (cur != null) {
                Spacer(Modifier.height(8.dp))
                if (cur.artwork != null) {
                    AsyncImage(
                        model = cur.artwork,
                        contentDescription = null,
                        modifier = Modifier.size(300.dp).align(Alignment.CenterHorizontally).clip(RoundedCornerShape(12.dp)),
                    )
                } else {
                    Card(
                        Modifier.size(300.dp).align(Alignment.CenterHorizontally),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Box(Modifier.fillMaxSize().background(Color(0x2E000000)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(72.dp))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(cur.title, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.basicMarquee())
                Text(cur.artist, fontSize = 19.sp, color = Color.White.copy(alpha = 0.6f), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.basicMarquee())

                // Seekbar real: drag preview + seekTo saat dilepas.
                var drag by remember(cur.key) { mutableStateOf<Float?>(null) }
                val duration = state.durationMs.coerceAtLeast(1)
                Slider(
                    value = drag ?: state.positionMs.toFloat().coerceIn(0f, duration.toFloat()),
                    onValueChange = { drag = it },
                    valueRange = 0f..duration.toFloat(),
                    onValueChangeFinished = {
                        drag?.let { player.seekTo(it.toLong()) }
                        drag = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth()) {
                    Text(fmtMs(drag?.toLong() ?: state.positionMs), fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                    Spacer(Modifier.weight(1f))
                    Text("−" + fmtMs((duration - (drag?.toLong() ?: state.positionMs)).coerceAtLeast(0)), fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Spacer(Modifier.width(24.dp))
                    IconButton(onClick = { player.prev() }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = "Prev", modifier = Modifier.size(38.dp))
                    }
                    IconButton(onClick = { player.togglePlayPause() }, modifier = Modifier.weight(1f)) {
                        Icon(
                            if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play",
                            modifier = Modifier.size(72.dp),
                        )
                    }
                    IconButton(onClick = { player.next() }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Next", modifier = Modifier.size(38.dp))
                    }
                    Spacer(Modifier.width(24.dp))
                }
                // Volume real via AudioManager.
                val am = remember { ctx.getSystemService(AudioManager::class.java) }
                val max = remember { am.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }
                var vol by remember {
                    mutableFloatStateOf(
                        am.getStreamVolume(AudioManager.STREAM_MUSIC).coerceIn(0, max).toFloat(),
                    )
                }
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.VolumeDown, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                    Slider(
                        value = vol,
                        onValueChange = {
                            vol = it
                            am.setStreamVolume(AudioManager.STREAM_MUSIC, it.toInt(), 0)
                        },
                        valueRange = 0f..max.toFloat(),
                        modifier = Modifier.weight(1f),
                    )
                    Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                }
                Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
                    Spacer(Modifier.weight(1f))
                    // PRD-101 masih P1: disabled jujur.
                    TextButton(onClick = {}, enabled = false) { Text("💬", color = Color.White.copy(alpha = 0.35f)) }
                    Spacer(Modifier.width(24.dp))
                    IconButton(
                        onClick = {
                            scope.launch {
                                snack.showSnackbar("Output audio: hanya speaker/earphone di app ini")
                            }
                        },
                    ) {
                        Text("◎", fontSize = 22.sp, color = Color.White.copy(alpha = 0.35f))
                    }
                    Spacer(Modifier.width(24.dp))
                    IconButton(onClick = { player.toggleQueue() }) {
                        Icon(Icons.Filled.QueueMusic, contentDescription = "Queue")
                    }
                    Spacer(Modifier.weight(1f))
                }
                if (state.showQueue) {
                    QueuePanel(state = state, player = player)
                }
            } else {
                Text("Tidak ada lagu. Pilih dari Library atau Search.")
            }
        }
        com.zaaam.liphify.ui.common.TrackSheet(
            track = menu,
            playlists = pls,
            onDismiss = { menu = null },
            onPlayNext = { player.playNext(it) },
            onPlayLast = { player.addToQueue(it) },
            onCreatePlaylist = { plVm.create(it) },
            onAddToPlaylist = { id, t -> plVm.addTrack(id, t) },
        )
    }
}

/** Panel Up Next: Shuffle/Repeat di header ala Apple Music + reorder real. */
@Composable
private fun QueuePanel(state: PlayerUiState, player: PlaybackViewModel) {
    Column(Modifier.fillMaxWidth()) {
    Text("Playing Next", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        TextButton(
            onClick = { player.setShuffle(!state.shuffleEnabled) },
            modifier = Modifier.weight(1f),
        ) {
            Text(if (state.shuffleEnabled) "🔀 Shuffle On" else "🔀 Shuffle Off")
        }
        val rpLabel = when (state.repeatMode) {
            Player.REPEAT_MODE_ONE -> "🔂 Repeat One"
            Player.REPEAT_MODE_ALL -> "🔁 Repeat All"
            else -> "🔁 Repeat Off"
        }
        TextButton(
            onClick = {
                player.setRepeat(
                    when (state.repeatMode) {
                        Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                        else -> Player.REPEAT_MODE_OFF
                    },
                )
            },
            modifier = Modifier.weight(1f),
        ) {
            Text(rpLabel)
        }
    }
    LazyColumn(Modifier.fillMaxWidth().heightIn(max = 300.dp)) {
        itemsIndexed(state.queue, key = { idx, t -> "$idx:${t.key.hashCode()}" }) { idx, t ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                com.zaaam.liphify.ui.common.Artwork(model = t.artwork, modifier = Modifier.size(44.dp), radius = 6.dp)
                Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                    Text(
                        t.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (t.key == state.current?.key) com.zaaam.liphify.ui.theme.Accent else Color.White,
                    )
                    Text(t.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
                }
                IconButton(onClick = { player.moveQueue(idx, idx - 1) }, enabled = idx > 0) {
                    Icon(Icons.Filled.ArrowUpward, contentDescription = "Naik")
                }
                IconButton(onClick = { player.moveQueue(idx, idx + 1) }, enabled = idx < state.queue.size - 1) {
                    Icon(Icons.Filled.ArrowDownward, contentDescription = "Turun")
                }
            }
        }
    }
    TextButton(onClick = { player.toggleQueue(false) }, modifier = Modifier.fillMaxWidth()) {
        Text("Tutup")
    }
    }
}

private fun fmtMs(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}
