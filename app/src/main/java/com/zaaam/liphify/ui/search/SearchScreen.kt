package com.zaaam.liphify.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zaaam.liphify.domain.model.Track
import com.zaaam.liphify.ui.common.TrackRow
import com.zaaam.liphify.ui.common.TrackSheet
import com.zaaam.liphify.ui.player.PlaybackViewModel
import com.zaaam.liphify.ui.playlist.PlaylistViewModel

@Composable
fun SearchScreen(
    preset: String = "",
    player: PlaybackViewModel,
    vm: SearchViewModel = hiltViewModel(),
    plVm: PlaylistViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsState()
    val pls by plVm.playlists.collectAsState()
    var menu by remember { mutableStateOf<Track?>(null) }
    LaunchedEffect(preset) { vm.setPreset(preset) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        TextField(
            value = s.query,
            onValueChange = { vm.onQuery(it) },
            placeholder = { Text("Search") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
            singleLine = true,
        )
        LazyColumn(Modifier.fillMaxSize()) {
            if (s.query.isNotBlank()) {
                item { Text("Di Perangkat", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                if (s.local.isEmpty()) {
                    item { Text("Tidak ada hasil lokal", color = com.zaaam.liphify.ui.theme.TextSecondary, modifier = Modifier.padding(vertical = 6.dp)) }
                }
                items(s.local, key = { it.key }) { t ->
                    TrackRow(track = t, onPlay = { player.playTrack(t, s.local) }, onMenu = { menu = t })
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
                        Text("YouTube Music", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        if (s.ytLoading) CircularProgressIndicator()
                    }
                    if (s.ytError != null) {
                        Text("Gagal ambil data dari YouTube, coba lagi", color = com.zaaam.liphify.ui.theme.TextSecondary)
                    }
                }
                items(s.yt, key = { it.videoId }) { y ->
                    val track = vm.ytAsTrack(y)
                    TrackRow(
                        track = track,
                        subtitle = "YouTube",
                        onPlay = { player.playTrack(track) },
                        onMenu = { menu = track },
                    )
                }
            } else {
                item {
                    Text(
                        "Ketik untuk mencari di perangkat & YouTube Music",
                        color = com.zaaam.liphify.ui.theme.TextSecondary,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                }
            }
        }
    }
    TrackSheet(
        track = menu,
        playlists = pls,
        onDismiss = { menu = null },
        onPlayNext = { player.playNext(it) },
        onPlayLast = { player.addToQueue(it) },
        onCreatePlaylist = { plVm.create(it) },
        onAddToPlaylist = { id, t -> plVm.addTrack(id, t) },
    )
}
