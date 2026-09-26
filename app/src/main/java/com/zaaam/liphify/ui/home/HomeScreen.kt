package com.zaaam.liphify.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zaaam.liphify.domain.model.Track
import com.zaaam.liphify.ui.common.Artwork
import com.zaaam.liphify.ui.common.LargeTitle
import com.zaaam.liphify.ui.common.TrackRow
import com.zaaam.liphify.ui.common.TrackSheet
import com.zaaam.liphify.ui.library.LibraryViewModel
import com.zaaam.liphify.ui.player.PlaybackViewModel
import com.zaaam.liphify.ui.playlist.PlaylistViewModel
import com.zaaam.liphify.ui.theme.TextSecondary

/**
 * Home: 100% data nyata. Tidak ada hero rekomendasi dummy — bagian itu
 * di-skip sesuai aturan (rekomendasi algoritmik di luar scope v1).
 */
@Composable
fun HomeScreen(
    player: PlaybackViewModel,
    libVm: LibraryViewModel,
    vm: HomeViewModel = hiltViewModel(),
    plVm: PlaylistViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsState()
    val pls by plVm.playlists.collectAsState()
    val lib by libVm.state.collectAsState()
    var menu by remember { mutableStateOf<Track?>(null) }
    val perm = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        libVm.scanIfEmpty(); vm.refresh()
    }
    LaunchedEffect(Unit) { vm.refresh() }
    // Scan selesai di tab mana pun (songCount berubah) -> Home ikut refresh.
    LaunchedEffect(lib.songCount) { vm.refresh() }

    LazyColumn(Modifier.fillMaxSize()) {
        item { LargeTitle("Home") }
        if (lib.needsPermission) {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text("Perlu izin audio untuk memindai musik di perangkat.", color = TextSecondary)
                    Button(onClick = { launcher.launch(perm) }, modifier = Modifier.padding(vertical = 8.dp)) {
                        Text("Beri izin & pindai")
                    }
                }
            }
        }
        item {
            Text("Recently Played", Modifier.padding(start = 16.dp, top = 8.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        if (s.recent.isEmpty()) {
            item {
                Text(
                    "Belum ada riwayat, mulai putar musik dari Library",
                    Modifier.padding(16.dp),
                    color = TextSecondary,
                )
            }
        } else {
            item {
                LazyRow(Modifier.padding(vertical = 8.dp)) {
                    items(s.recent, key = { it.key }) { t ->
                        Column(Modifier.padding(start = 16.dp).width(140.dp).clickable { player.playTrack(t, s.recent) }) {
                            Artwork(model = t.artwork, modifier = Modifier.width(140.dp).height(140.dp), radius = 10.dp, fallbackIconSize = 48.dp)
                            Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(t.artist, maxLines = 1, color = TextSecondary, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        item {
            Text("Recently Added", Modifier.padding(start = 16.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        if (s.newMusic.isEmpty()) {
            item {
                Text(
                    "Belum ada musik. Beri izin lalu pindai dari Library.",
                    Modifier.padding(16.dp),
                    color = TextSecondary,
                )
            }
        } else {
            item {
                LazyRow(Modifier.padding(vertical = 8.dp)) {
                    items(s.newMusic, key = { it.key }) { t ->
                        Column(Modifier.padding(start = 16.dp).width(140.dp).clickable { player.playTrack(t, s.newMusic) }) {
                            Artwork(model = t.artwork, modifier = Modifier.width(140.dp).height(140.dp), radius = 10.dp, fallbackIconSize = 48.dp)
                            Text(t.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(t.artist, maxLines = 1, color = TextSecondary, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        item {
            Text("New Music", Modifier.padding(start = 16.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        items(s.newMusic, key = { it.key }) { t ->
            Column(Modifier.padding(horizontal = 16.dp)) {
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
