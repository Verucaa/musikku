package com.zaaam.liphify.ui.listennow

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.zaaam.liphify.data.local.AppDatabase
import com.zaaam.liphify.domain.model.PlaybackSource
import com.zaaam.liphify.domain.model.Track
import com.zaaam.liphify.ui.player.PlaybackViewModel
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DbEntry {
    fun db(): AppDatabase
}

@Composable
fun ListenNowScreen(nav: NavController, player: PlaybackViewModel) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var recent by remember { mutableStateOf<List<Track>>(emptyList()) }
    LaunchedEffect(Unit) {
        val db = EntryPointAccessors.fromApplication(ctx, DbEntry::class.java).db()
        val rows = withContext(Dispatchers.IO) { db.historyDao().recent(20) }
        recent = rows.map {
            val src = if (it.source == "youtube" && it.videoId != null) {
                PlaybackSource.YouTube(it.videoId)
            } else {
                PlaybackSource.Local(android.net.Uri.parse(it.localUri ?: ""))
            }
            Track(it.trackKey, it.title, it.artist, "", 0L, it.artwork, src)
        }
    }
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Text("Listen now", Modifier.padding(16.dp))
            // STATIC PER PRD-007 — recommendation algorithm out of scope v1
            Card(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("New Music Mix", Modifier.padding(16.dp))
            }
            Text("Recently played", Modifier.padding(16.dp))
            if (recent.isEmpty()) {
                Text("Belum ada riwayat, mulai putar musik dari Library", Modifier.padding(16.dp))
            }
        }
        items(recent, key = { it.key }) { t ->
            Row(
                Modifier.fillMaxWidth().clickable { player.playTrack(t, recent) }.padding(12.dp),
            ) {
                Column {
                    Text(t.title)
                    Text(t.artist)
                }
            }
        }
    }
}
