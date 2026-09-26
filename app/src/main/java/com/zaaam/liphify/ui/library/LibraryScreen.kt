package com.zaaam.liphify.ui.library

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.zaaam.liphify.ui.player.PlaybackViewModel

@Composable
fun LibraryScreen(player: PlaybackViewModel, vm: LibraryViewModel = hiltViewModel()) {
    val state by vm.state.collectAsState()
    val perm = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        vm.refresh()
    }
    LaunchedEffect(Unit) { vm.refresh() }

    if (state.needsPermission) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Text("Perlu izin audio untuk memindai musik di perangkat.")
            Spacer(Modifier.height(12.dp))
            Button(onClick = { launcher.launch(perm) }) { Text("Pindai musik di perangkat") }
        }
        return
    }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Text("Library", Modifier.padding(16.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text("Songs • ${state.songCount}")
            }
            Text("Playlists (${state.playlistNames.size})", Modifier.padding(16.dp))
            Text("Artists (${state.artists.size})", Modifier.padding(16.dp))
            Text("Albums (${state.albums.size})", Modifier.padding(16.dp))
            Button(onClick = { vm.scan() }, modifier = Modifier.padding(16.dp)) {
                Text(if (state.scanning) "Memindai…" else "Refresh / Pindai ulang")
            }
            Divider()
        }
        items(state.songs, key = { it.key }) { t ->
            Row(
                Modifier.fillMaxWidth().clickable { player.playTrack(t, state.songs) }.padding(12.dp),
            ) {
                AsyncImage(model = t.artwork, contentDescription = null)
                Column(Modifier.padding(start = 12.dp)) {
                    Text(t.title)
                    Text(t.artist)
                }
            }
        }
    }
}
