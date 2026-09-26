package com.zaaam.liphify.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zaaam.liphify.domain.model.Track
import com.zaaam.liphify.ui.common.LargeTitle
import com.zaaam.liphify.ui.common.TrackRow
import com.zaaam.liphify.ui.common.TrackSheet
import com.zaaam.liphify.ui.player.PlaybackViewModel
import com.zaaam.liphify.ui.playlist.PlaylistViewModel
import com.zaaam.liphify.ui.theme.SurfaceSecondary
import com.zaaam.liphify.ui.theme.TextSecondary

private val GENRES = listOf(
    "Pop" to (Color(0xFFFC5C7D) to Color(0xFF6A82FB)),
    "Hip-Hop" to (Color(0xFFF7B733) to Color(0xFFFC4A1A)),
    "R&B" to (Color(0xFF8E2DE2) to Color(0xFF4A00E0)),
    "Electronic" to (Color(0xFF11998E) to Color(0xFF38EF7D)),
)

@Composable
fun SearchScreen(
    preset: String = "",
    player: PlaybackViewModel,
    plVm: PlaylistViewModel,
    vm: SearchViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsState()
    val pls by plVm.playlists.collectAsState()
    var menu by remember { mutableStateOf<Track?>(null) }
    /** 0=Semua 1=Perangkat 2=YouTube — filter tampilan nyata. */
    var scope by remember { mutableIntStateOf(0) }
    val ctx = LocalContext.current
    var recent by remember { mutableStateOf(listOf<String>()) }
    LaunchedEffect(preset) { vm.setPreset(preset) }
    LaunchedEffect(Unit) { recent = RecentQueries.load(ctx) }
    LaunchedEffect(s.query) {
        val q = s.query.trim()
        if (q.length >= 2) {
            kotlinx.coroutines.delay(800)
            // Simpan hanya kalau user berhenti mengetik (bukan tiap keystroke).
            if (q == s.query.trim()) {
                RecentQueries.save(ctx, q)
                recent = RecentQueries.load(ctx)
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        LargeTitle("Search")
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(SurfaceSecondary)
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("🔍 ", color = TextSecondary)
            BasicTextField(
                value = s.query,
                onValueChange = { vm.onQuery(it) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                cursorBrush = SolidColor(Color.White),
                decorationBox = { inner ->
                    if (s.query.isEmpty()) Text("Search", color = TextSecondary, fontSize = 16.sp)
                    inner()
                },
            )
        }
        if (s.query.isNotBlank()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                listOf("Semua", "Perangkat", "YouTube").forEachIndexed { i, label ->
                    TextButton(onClick = { scope = i }) {
                        Text(
                            label,
                            color = if (scope == i) Color.White else TextSecondary,
                            fontWeight = if (scope == i) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            if (s.query.isBlank()) {
                if (recent.isNotEmpty()) {
                    item { Text("Recent Searches", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp)) }
                    item {
                        Column {
                            recent.forEach { q ->
                                Text(
                                    q,
                                    Modifier.fillMaxWidth().clickable { vm.onQuery(q) }.padding(vertical = 10.dp),
                                    fontSize = 16.sp,
                                )
                            }
                        }
                    }
                }
                item { Text("Browse Categories", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp)) }
                item {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.height(240.dp),
                        userScrollEnabled = false,
                    ) {
                        items(GENRES) { (name, colors) ->
                            Box(
                                Modifier.padding(6.dp).fillMaxWidth().height(104.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Brush.linearGradient(listOf(colors.first, colors.second)))
                                    .clickable { vm.onQuery(name) }
                                    .padding(12.dp),
                                contentAlignment = Alignment.BottomStart,
                            ) { Text(name, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            } else {
                if (scope != 2) {
                    item { Text("Di Perangkat", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp)) }
                    if (s.local.isEmpty()) {
                        item { Text("Tidak ada hasil lokal", color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp)) }
                    } else {
                        // Key komposit index+key: kebal duplikat, tidak force close.
                        items(s.local.size, key = { i -> "l$i:${s.local[i].key}" }) { i ->
                            val t = s.local[i]
                            TrackRow(track = t, onPlay = { player.playTrack(t, s.local) }, onMenu = { menu = t })
                        }
                    }
                }
                if (scope != 1) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                            Text("YouTube Music", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            if (s.ytLoading) CircularProgressIndicator()
                        }
                        if (s.ytError != null) {
                            Text("Gagal ambil data dari YouTube, coba lagi", color = TextSecondary)
                        }
                    }
                    // Key komposit: videoId bisa kolaps untuk non-video, index menjamin unik.
                    items(s.yt.size, key = { i -> "y$i:${s.yt[i].videoId.hashCode()}" }) { i ->
                        val y = s.yt[i]
                        val track = vm.ytAsTrack(y)
                        TrackRow(
                            track = track,
                            subtitle = "YouTube",
                            onPlay = { player.playTrack(track) },
                            onMenu = { menu = track },
                        )
                    }
                    if (!s.ytLoading && s.yt.isEmpty() && s.ytError == null) {
                        item { Text("Tidak ada hasil YouTube", color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp)) }
                    }
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
