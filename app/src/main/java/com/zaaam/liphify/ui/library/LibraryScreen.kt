package com.zaaam.liphify.ui.library

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.zaaam.liphify.data.local.PlaylistTrackEntity
import com.zaaam.liphify.domain.model.Track
import com.zaaam.liphify.ui.common.TrackRow
import com.zaaam.liphify.ui.common.TrackSheet
import com.zaaam.liphify.ui.player.PlaybackViewModel
import com.zaaam.liphify.ui.playlist.PlaylistViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private sealed interface LibView {
    data object Main : LibView
    data object Songs : LibView
    data object Artists : LibView
    data class Artist(val name: String) : LibView
    data object Albums : LibView
    data class Album(val name: String) : LibView
    data object Playlists : LibView
    data class Playlist(val id: Long, val name: String) : LibView
}

@Composable
fun LibraryScreen(
    player: PlaybackViewModel,
    vm: LibraryViewModel = hiltViewModel(),
    plVm: PlaylistViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsState()
    val pls by plVm.playlists.collectAsState()
    var view by remember { mutableStateOf<LibView>(LibView.Main) }
    var menu by remember { mutableStateOf<Track?>(null) }
    val perm = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { vm.scanIfEmpty() }
    LaunchedEffect(Unit) { vm.scanIfEmpty() }

    if (state.needsPermission) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Text("Perlu izin audio untuk memindai musik di perangkat.")
            Spacer(Modifier.height(12.dp))
            Button(onClick = { launcher.launch(perm) }) { Text("Pindai musik di perangkat") }
        }
        return
    }

    val songList = state.songs
    fun playAll(from: Track) = player.playTrack(from, songList)

    Column(Modifier.fillMaxSize()) {
        if (view != LibView.Main) {
            TextButton(onClick = { view = LibView.Main }) { Text("‹ Library") }
        }
        when (val v = view) {
            LibView.Main -> {
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    item {
                        CatRow("🎧", "Playlists", pls.size) { view = LibView.Playlists }
                        CatRow("🎤", "Artists", state.artists.size) { view = LibView.Artists }
                        CatRow("💿", "Albums", state.albums.size) { view = LibView.Albums }
                        CatRow("🎵", "Songs", state.songCount) { view = LibView.Songs }
                    }
                    item {
                        Text("Recently Added", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
                        LazyRow {
                            items(state.recentlyAdded, key = { it.key }) { t ->
                                Column(Modifier.padding(end = 12.dp).width(140.dp).clickable { playAll(t) }) {
                                    AsyncImage(model = t.artwork, contentDescription = null, modifier = Modifier.width(140.dp).height(140.dp).clip(RoundedCornerShape(10.dp)))
                                    Text(t.title, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                    Text(t.artist, maxLines = 1, color = com.zaaam.liphify.ui.theme.TextSecondary, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Songs • ${state.songCount}", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(vertical = 8.dp))
                            TextButton(onClick = { vm.scan() }) { Text(if (state.scanning) "Memindai…" else "Refresh") }
                        }
                    }
                    items(songList, key = { it.key }) { t ->
                        TrackRow(track = t, onPlay = { playAll(t) }, onMenu = { menu = t })
                    }
                }
            }
            LibView.Songs -> SongList(songList, onPlay = { playAll(it) }, onMenu = { menu = it })
            LibView.Artists -> NameList("Artists", state.artists) { view = LibView.Artist(it) }
            is LibView.Artist -> {
                val list = songList.filter { it.artist == v.name }
                SongList(list, title = v.name, onPlay = { player.playTrack(it, list) }, onMenu = { menu = it })
            }
            LibView.Albums -> NameList("Albums", state.albums) { view = LibView.Album(it) }
            is LibView.Album -> {
                val list = songList.filter { it.album == v.name }
                SongList(list, title = v.name, onPlay = { player.playTrack(it, list) }, onMenu = { menu = it })
            }
            LibView.Playlists -> PlaylistList(
                onOpen = { id, name -> view = LibView.Playlist(id, name) },
                onCreate = { plVm.create(it) },
            )
            is LibView.Playlist -> PlaylistDetail(
                id = v.id,
                name = v.name,
                plVm = plVm,
                player = player,
                onDelete = { plVm.deletePlaylist(v.id); view = LibView.Playlists },
            )
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

@Composable
private fun CatRow(icon: String, label: String, count: Int, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(30.dp).clip(RoundedCornerShape(7.dp))
                .background(com.zaaam.liphify.ui.theme.Accent),
            contentAlignment = Alignment.Center,
        ) { Text(icon) }
        Text(label, Modifier.padding(start = 12.dp).weight(1f), fontSize = 17.sp)
        Text("$count ›", color = com.zaaam.liphify.ui.theme.TextHint)
    }
}

@Composable
private fun SongList(list: List<Track>, title: String? = null, onPlay: (Track) -> Unit, onMenu: (Track) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        if (title != null) item { Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp)) }
        if (list.isEmpty()) item { Text("Kosong.", color = com.zaaam.liphify.ui.theme.TextSecondary) }
        items(list, key = { it.key }) { t -> TrackRow(track = t, onPlay = { onPlay(t) }, onMenu = { onMenu(t) }) }
    }
}

@Composable
private fun NameList(title: String, names: List<String>, onOpen: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item { Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp)) }
        if (names.isEmpty()) item { Text("Kosong.", color = com.zaaam.liphify.ui.theme.TextSecondary) }
        items(names) { n ->
            Row(Modifier.fillMaxWidth().clickable { onOpen(n) }.padding(vertical = 12.dp)) {
                Text(n, Modifier.weight(1f), fontSize = 17.sp)
                Text("›", color = com.zaaam.liphify.ui.theme.TextHint)
            }
        }
    }
}

@Composable
private fun PlaylistList(onOpen: (Long, String) -> Unit, onCreate: (String) -> Unit) {
    val plVm: PlaylistViewModel = hiltViewModel()
    val pls by plVm.playlists.collectAsState()
    var name by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item {
            Text("Playlists", fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                androidx.compose.material3.OutlinedTextField(
                    value = name, onValueChange = { name = it }, label = { Text("Playlist baru…") },
                    singleLine = true, modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { if (name.isNotBlank()) { onCreate(name.trim()); name = "" } }) { Text("Buat") }
            }
        }
        items(pls, key = { it.id }) { p ->
            Row(Modifier.fillMaxWidth().clickable { onOpen(p.id, p.name) }.padding(vertical = 12.dp)) {
                Text(p.name, Modifier.weight(1f), fontSize = 17.sp)
                Text("›", color = com.zaaam.liphify.ui.theme.TextHint)
            }
        }
    }
}

@Composable
private fun PlaylistDetail(
    id: Long,
    name: String,
    plVm: PlaylistViewModel,
    player: PlaybackViewModel,
    onDelete: () -> Unit,
) {
    var tracks by remember(id) { mutableStateOf<List<PlaylistTrackEntity>>(emptyList()) }
    var menu by remember { mutableStateOf<Track?>(null) }
    val pls by plVm.playlists.collectAsState()
    LaunchedEffect(id) {
        CoroutineScope(Dispatchers.IO).launch {
            tracks = plVm.tracksOf(id)
        }
    }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f).padding(vertical = 8.dp))
                TextButton(onClick = onDelete) { Text("Hapus", color = Color.Red) }
            }
        }
        if (tracks.isEmpty()) item { Text("Belum ada lagu.", color = com.zaaam.liphify.ui.theme.TextSecondary) }
        items(tracks, key = { "${it.playlistId}:${it.position}" }) { e ->
            val t = e.toTrack()
            TrackRow(
                track = t,
                subtitle = "${e.title} • ${e.artist}",
                onPlay = {
                    val list = tracks.map { it.toTrack() }
                    player.playTrack(t, list)
                },
                onMenu = { menu = t },
            )
        }
    }
    TrackSheet(
        track = menu, playlists = pls, onDismiss = { menu = null },
        onPlayNext = { player.playNext(it) }, onPlayLast = { player.addToQueue(it) },
        onCreatePlaylist = { plVm.create(it) }, onAddToPlaylist = { pid, t -> plVm.addTrack(pid, t) },
    )
}

private fun PlaylistTrackEntity.toTrack(): Track {
    val src = if (source == "youtube" && videoId != null) {
        com.zaaam.liphify.domain.model.PlaybackSource.YouTube(videoId)
    } else {
        com.zaaam.liphify.domain.model.PlaybackSource.Local(android.net.Uri.parse(localUri ?: ""))
    }
    return Track(trackKey, title, artist, "", 0L, artwork, src)
}
