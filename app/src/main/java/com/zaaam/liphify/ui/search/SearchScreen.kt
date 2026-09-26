package com.zaaam.liphify.ui.search

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zaaam.liphify.ui.player.PlaybackViewModel

@Composable
fun SearchScreen(player: PlaybackViewModel, vm: SearchViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    Column(Modifier.fillMaxSize()) {
        TextField(
            value = s.query,
            onValueChange = { vm.onQuery(it) },
            placeholder = { Text("Cari lagu, artis, album…") },
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            singleLine = true,
        )
        LazyColumn(Modifier.fillMaxSize()) {
            if (s.query.isNotBlank()) {
                item { Text("Di perangkat", Modifier.padding(12.dp)) }
                items(s.local, key = { it.key }) { t ->
                    Row(
                        Modifier.fillMaxWidth().clickable { player.playTrack(t, s.local) }.padding(12.dp),
                    ) {
                        Column {
                            Text(t.title)
                            Text(t.artist)
                        }
                    }
                }
                item {
                    Row(Modifier.padding(12.dp)) {
                        Text("YouTube Music")
                        if (s.ytLoading) CircularProgressIndicator(Modifier.padding(start = 8.dp))
                    }
                    if (s.ytError != null) {
                        Text("Gagal ambil data dari YouTube, coba lagi", Modifier.padding(horizontal = 12.dp))
                    }
                }
                items(s.yt, key = { it.videoId }) { y ->
                    val track = vm.ytAsTrack(y)
                    Row(
                        Modifier.fillMaxWidth().clickable { player.playTrack(track) }.padding(12.dp),
                    ) {
                        Column {
                            Text(y.title)
                            Text("YouTube")
                        }
                    }
                }
            } else {
                item { Text("Ketik untuk mencari di perangkat & YouTube Music", Modifier.padding(16.dp)) }
            }
        }
    }
}
