package com.zaaam.liphify.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.zaaam.liphify.domain.model.Track
import com.zaaam.liphify.ui.common.Artwork
import com.zaaam.liphify.ui.common.GENRES
import com.zaaam.liphify.ui.common.GenreTile
import com.zaaam.liphify.ui.common.LargeTitle
import com.zaaam.liphify.ui.common.ScanEmptyState
import com.zaaam.liphify.ui.common.TrackRow
import com.zaaam.liphify.ui.common.TrackSheet
import com.zaaam.liphify.ui.library.LibraryViewModel
import com.zaaam.liphify.ui.player.PlaybackViewModel
import com.zaaam.liphify.ui.playlist.PlaylistViewModel
import com.zaaam.liphify.ui.theme.Accent
import com.zaaam.liphify.ui.theme.TextSecondary
import java.util.Calendar

/**
 * Home: 100% data nyata. Tidak ada hero rekomendasi dummy — bagian itu
 * di-skip sesuai aturan (rekomendasi algoritmik di luar scope v1).
 * Section "Browse" reuse GENRES/GenreTile yang sama dengan tab New/Search,
 * biar Home tetap ada konten nyata & fungsional walau histori masih dikit.
 */
@Composable
fun HomeScreen(
    player: PlaybackViewModel,
    libVm: LibraryViewModel,
    plVm: PlaylistViewModel,
    onGenre: (String) -> Unit,
    onOpenBrowse: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenFavorit: () -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val s by vm.state.collectAsState()
    val pls by plVm.playlists.collectAsState()
    val favKeys by plVm.favoritKeys.collectAsState()
    val lib by libVm.state.collectAsState()
    var menu by remember { mutableStateOf<Track?>(null) }
    val audioPerm = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    val allPerms = remember {
        if (Build.VERSION.SDK_INT >= 33) {
            arrayOf(audioPerm, Manifest.permission.POST_NOTIFICATIONS)
        } else {
            arrayOf(audioPerm)
        }
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        libVm.scanIfEmpty(); vm.refresh()
    }
    LaunchedEffect(Unit) { vm.refresh() }
    // Scan selesai di tab mana pun (songCount berubah) -> Home ikut refresh.
    LaunchedEffect(lib.songCount) { vm.refresh() }

    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 4..10 -> "Selamat pagi"
            in 11..14 -> "Selamat siang"
            in 15..17 -> "Selamat sore"
            else -> "Selamat malam"
        }
    }

    LazyColumn(Modifier.fillMaxSize()) {
        item { LargeTitle("Home") }
        item {
            Text(greeting, Modifier.padding(start = 16.dp, bottom = 4.dp), color = TextSecondary, fontSize = 14.sp)
        }
        if (s.recent.isNotEmpty()) {
            item {
                val last = s.recent.first()
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF3A2F5A), Color(0xFF1C1C1E))))
                        .clickable { player.playTrack(last, s.recent) }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Artwork(model = last.artwork, modifier = Modifier.size(64.dp), radius = 12.dp)
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text("Lanjut Dengerin", fontSize = 12.sp, color = TextSecondary)
                        Text(last.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                        Text(last.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp, color = TextSecondary)
                    }
                    Box(
                        Modifier.size(40.dp).clip(CircleShape).background(Color.White)
                            .clickable { player.playTrack(last, s.recent) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Putar", tint = Color.Black)
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                QuickChip("Favorit", Icons.Filled.Favorite, onOpenFavorit)
                QuickChip("Jelajah", Icons.Filled.Explore, onOpenBrowse)
                QuickChip("Cari", Icons.Filled.Search, onOpenSearch)
            }
        }
        item {
            Text("Trending", Modifier.padding(start = 16.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        when {
            s.trendingLoading && s.trending.isEmpty() -> {
                item {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text("Memuat trending…", Modifier.padding(start = 12.dp), color = TextSecondary)
                    }
                }
            }
            s.trendingError != null && s.trending.isEmpty() -> {
                item {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text("Gagal ambil data dari YouTube, coba lagi", color = TextSecondary)
                        Button(onClick = { vm.retryTrending() }, modifier = Modifier.padding(top = 8.dp)) {
                            Text("Coba lagi")
                        }
                    }
                }
            }
            s.trending.isNotEmpty() -> {
                item {
                    LazyRow(Modifier.padding(vertical = 8.dp)) {
                        items(s.trending, key = { it.key }) { t -> MediaCard(t) { player.playTrack(t, s.trending) } }
                    }
                }
                item {
                    Text("Artis Terpopuler", Modifier.padding(start = 16.dp, top = 8.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                item {
                    LazyRow(Modifier.padding(vertical = 8.dp)) {
                        // Diturunin dari data Trending yang nyata; tap = cari artisnya.
                        items(s.trending.distinctBy { it.artist }.take(8), key = { it.artist }) { t ->
                            Column(
                                Modifier.padding(start = 16.dp).width(80.dp).clickable { onGenre(t.artist) },
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Artwork(model = t.artwork, modifier = Modifier.size(76.dp), radius = 38.dp, fallbackIconSize = 32.dp)
                                Text(t.artist, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                }
            }
        }
        item {
            Text("Browse", Modifier.padding(start = 16.dp, top = 8.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        item {
            LazyRow(Modifier.padding(vertical = 8.dp)) {
                items(GENRES) { (name, colors) ->
                    Box(Modifier.padding(start = 16.dp).width(160.dp)) {
                        GenreTile(name, colors) { onGenre(name) }
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
            items(s.recent.take(5), key = { it.key }) { t ->
                Box(Modifier.padding(horizontal = 16.dp)) {
                    TrackRow(track = t, onPlay = { player.playTrack(t, s.recent) }, onMenu = { menu = t })
                }
            }
        }
        item {
            Text("Recently Added", Modifier.padding(start = 16.dp), fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        if (s.newMusic.isEmpty()) {
            item {
                ScanEmptyState(
                    scanning = lib.scanning,
                    needsPermission = lib.needsPermission,
                    onScan = { if (libVm.hasPermission()) libVm.scan() else launcher.launch(allPerms) },
                    modifier = Modifier.padding(16.dp),
                )
            }
        } else {
            item {
                LazyRow(Modifier.padding(vertical = 8.dp)) {
                    items(s.newMusic, key = { it.key }) { t -> MediaCard(t) { player.playTrack(t, s.newMusic) } }
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
        isFavorite = menu?.let { favKeys.contains(it.key) } ?: false,
        onToggleFavorite = { plVm.toggleFavorite(it) },
    )
}

@Composable
private fun QuickChip(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(22.dp)).background(Color(0xFF1C1C1E)).clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = Accent, modifier = Modifier.size(18.dp))
        Text(label, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun MediaCard(t: Track, onClick: () -> Unit) {
    Column(Modifier.padding(start = 16.dp).width(148.dp).clickable(onClick = onClick)) {
        Artwork(model = t.artwork, modifier = Modifier.size(148.dp), radius = 12.dp, fallbackIconSize = 48.dp)
        Text(t.title, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 8.dp))
        Text(t.artist, fontSize = 12.sp, maxLines = 1, color = TextSecondary, overflow = TextOverflow.Ellipsis)
    }
}
