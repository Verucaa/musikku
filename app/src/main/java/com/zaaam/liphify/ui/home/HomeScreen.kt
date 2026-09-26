package com.zaaam.liphify.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.zaaam.liphify.domain.model.Track
import com.zaaam.liphify.ui.common.TrackRow
import com.zaaam.liphify.ui.common.TrackSheet
import com.zaaam.liphify.ui.player.PlaybackViewModel
import com.zaaam.liphify.ui.playlist.PlaylistViewModel

@Composable
fun HomeScreen(player: PlaybackViewModel, vm: HomeViewModel = hiltViewModel(), plVm: PlaylistViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    val pls by plVm.playlists.collectAsState()
    var menu by remember { mutableStateOf<Track?>(null) }
    LaunchedEffect(Unit) { vm.refresh() }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Text("Top Picks", Modifier.padding(start = 16.dp, top = 8.dp), fontSize = androidx.compose.ui.unit.TextUnit(20f, androidx.compose.ui.unit.TextUnitType.Sp), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            LazyRow(Modifier.padding(vertical = 8.dp)) {
                item {
                    // STATIC PER PRD-007 — recommendation algorithm out of scope v1
                    Box(
                        Modifier.padding(start = 16.dp, end = 8.dp).width(280.dp).height(150.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF7B2FF7), Color(0xFFF72F8F))))
                            .clickable { if (s.newMusic.isNotEmpty()) player.playTrack(s.newMusic.first(), s.newMusic) },
                    ) {
                        Text("New Music Mix", Modifier.padding(16.dp).align(androidx.compose.ui.Alignment.BottomStart))
                    }
                }
            }
        }
        item {
            Text("Recently Played", Modifier.padding(start = 16.dp), fontSize = androidx.compose.ui.unit.TextUnit(20f, androidx.compose.ui.unit.TextUnitType.Sp), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        }
        item {
            if (s.recent.isEmpty()) {
                Text(
                    "Belum ada riwayat, mulai putar musik dari Library",
                    Modifier.padding(16.dp),
                    color = com.zaaam.liphify.ui.theme.TextSecondary,
                )
            } else {
                LazyRow(Modifier.padding(vertical = 8.dp)) {
                    items(s.recent, key = { it.key }) { t ->
                        Column(
                            Modifier.padding(start = 16.dp).width(140.dp).clickable { player.playTrack(t, s.recent) },
                        ) {
                            AsyncImage(
                                model = t.artwork,
                                contentDescription = null,
                                modifier = Modifier.width(140.dp).height(140.dp).clip(RoundedCornerShape(10.dp)),
                            )
                            Text(t.title, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                            Text(t.artist, maxLines = 1, color = com.zaaam.liphify.ui.theme.TextSecondary, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        item {
            Text("New Music", Modifier.padding(start = 16.dp), fontSize = androidx.compose.ui.unit.TextUnit(20f, androidx.compose.ui.unit.TextUnitType.Sp), fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        }
        items(s.newMusic, key = { it.key }) { t ->
            Box(Modifier.padding(horizontal = 16.dp)) {
                TrackRow(track = t, onPlay = { player.playTrack(t, s.newMusic) }, onMenu = { menu = t })
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
