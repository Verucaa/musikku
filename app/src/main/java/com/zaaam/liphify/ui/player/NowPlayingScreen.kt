package com.zaaam.liphify.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import com.zaaam.liphify.ui.theme.SurfaceSecondary

/** PRD-005: Now Playing full-screen + PRD-008 queue panel real. */
@Composable
fun NowPlayingScreen(state: PlayerUiState, player: PlaybackViewModel, snack: SnackbarHostState) {
    val cur = state.current
    var bg by remember { mutableStateOf(Brush.verticalGradient(listOf(Color(0xFF2C2C2E), Color(0xFF1C1C1E)))) }

    LaunchedEffect(state.error) {
        if (state.error != null) {
            snack.showSnackbar(state.error)
            player.clearError()
        }
    }

    Box(
        Modifier.fillMaxSize().background(bg).padding(20.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            IconButton(onClick = { player.setExpanded(false) }) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Tutup")
            }
            if (cur != null) {
                // Artwork 1:1 real; fallback kaca + ikon (bukan placeholder abu-abu)
                if (cur.artwork != null) {
                    AsyncImage(
                        model = cur.artwork,
                        contentDescription = null,
                        modifier = Modifier.size(280.dp).align(Alignment.CenterHorizontally),
                        onSuccess = { result ->
                            try {
                                val bmp = (result.result.drawable as? android.graphics.drawable.BitmapDrawable)?.bitmap
                                if (bmp != null) {
                                    val pal = Palette.Builder(bmp).generate()
                                    val vibrant = pal.getVibrantSwatch() ?: pal.getMutedSwatch()
                                    if (vibrant != null) {
                                        fun darken(c: Int): Color {
                                            val r = (android.graphics.Color.red(c) * 0.65).toInt()
                                            val g = (android.graphics.Color.green(c) * 0.65).toInt()
                                            val b = (android.graphics.Color.blue(c) * 0.65).toInt()
                                            return Color(r, g, b)
                                        }
                                        bg = Brush.linearGradient(
                                            listOf(darken(vibrant.rgb), darken(vibrant.rgb)),
                                        )
                                    }
                                }
                            } catch (_: Exception) {
                            }
                        },
                    )
                } else {
                    Card(Modifier.size(280.dp).align(Alignment.CenterHorizontally)) {
                        Box(Modifier.fillMaxSize().background(Color(0x2E000000)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.MusicNote, contentDescription = null, tint = Color.White)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Text(cur.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(cur.artist, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Slider(
                    value = state.positionMs.toFloat(),
                    onValueChange = {},
                    valueRange = 0f..(state.durationMs.coerceAtLeast(1).toFloat()),
                    onValueChangeFinished = {},
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { player.toggleShuffle() }) {
                        Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle")
                    }
                    IconButton(onClick = { player.prev() }) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = "Prev")
                    }
                    IconButton(onClick = { player.togglePlayPause() }) {
                        Icon(if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = "Play")
                    }
                    IconButton(onClick = { player.next() }) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Next")
                    }
                    IconButton(onClick = { player.cycleRepeat() }) {
                        Icon(Icons.Filled.Repeat, contentDescription = "Repeat")
                    }
                }
                Row {
                    IconButton(onClick = { player.toggleQueue() }) {
                        Icon(Icons.Filled.QueueMusic, contentDescription = "Queue")
                    }
                    // PRD-101 masih P1: ikon lirik disabled jujur
                    IconButton(onClick = {}, enabled = false) {
                        Icon(Icons.Filled.MusicNote, contentDescription = "Lirik belum tersedia")
                    }
                }
                if (state.showQueue) {
                    Text("Queue (${state.queue.size})", Modifier.padding(vertical = 8.dp))
                    LazyColumn(Modifier.fillMaxWidth().weight(1f)) {
                        itemsIndexed(state.queue, key = { _, t -> t.key }) { idx, t ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(t.artist, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                if (idx > 0) {
                                    IconButton(onClick = { player.moveQueue(idx, idx - 1) }) {
                                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Pindah")
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Text("Tidak ada lagu. Pilih dari Library atau Search.")
            }
        }
    }
}
