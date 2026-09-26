package com.zaaam.liphify.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.zaaam.liphify.R
import com.zaaam.liphify.data.local.PlaylistEntity
import com.zaaam.liphify.domain.model.Track
import com.zaaam.liphify.ui.theme.Divider as DividerColor
import com.zaaam.liphify.ui.theme.TextSecondary

/**
 * Artwork nyata (MediaStore album art / thumbnail YT) + fallback kaca + ikon
 * kalau null/gagal load. Dipakai di SEMUA list agar tidak ada kotak kosong.
 */
@Composable
fun Artwork(
    model: Any?,
    modifier: Modifier = Modifier,
    radius: Dp = 6.dp,
    fallbackIconSize: Dp = 22.dp,
) {
    if (model != null) {
        AsyncImage(
            model = model,
            contentDescription = null,
            modifier = modifier.clip(RoundedCornerShape(radius)),
            error = painterResource(R.drawable.ic_music_note),
            fallback = painterResource(R.drawable.ic_music_note),
        )
    } else {
        Box(
            modifier
                .clip(RoundedCornerShape(radius))
                .background(Color(0x2E000000)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(R.drawable.ic_music_note),
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(fallbackIconSize),
            )
        }
    }
}

@Composable
fun TrackRow(
    track: Track,
    onPlay: () -> Unit,
    onMenu: () -> Unit,
    subtitle: String = track.artist,
) {
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clickable(onClick = onPlay).padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Artwork(model = track.artwork, modifier = Modifier.size(44.dp), radius = 6.dp)
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(track.title, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = TextSecondary)
            }
            Icon(
                Icons.Filled.MoreVert,
                contentDescription = "Menu lagu",
                tint = TextSecondary,
                modifier = Modifier.clickable(onClick = onMenu).padding(8.dp),
            )
        }
        HorizontalDivider(color = DividerColor)
    }
}

/** Action sheet: Play Next / Play Last / Add to Playlist (Room asli). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackSheet(
    track: Track?,
    playlists: List<PlaylistEntity>,
    onDismiss: () -> Unit,
    onPlayNext: (Track) -> Unit,
    onPlayLast: (Track) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onAddToPlaylist: (Long, Track) -> Unit,
) {
    if (track == null) return
    var picking by remember(track.key) { mutableStateOf(false) }
    var newName by remember(track.key) { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Artwork(model = track.artwork, modifier = Modifier.size(52.dp), radius = 8.dp)
            Column(Modifier.padding(start = 12.dp)) {
                Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${track.artist} • ${if (track.source is com.zaaam.liphify.domain.model.PlaybackSource.YouTube) "YouTube" else "Perangkat"}",
                    color = TextSecondary,
                )
            }
        }
        if (!picking) {
            SheetAction("Play Next", Icons.Filled.PlayArrow) { onPlayNext(track); onDismiss() }
            SheetAction("Play Last", Icons.Filled.PlaylistAdd) { onPlayLast(track); onDismiss() }
            SheetAction("Add to Playlist…", Icons.Filled.CreateNewFolder) { picking = true }
        } else {
            playlists.forEach { p ->
                SheetAction(p.name, Icons.Filled.PlaylistAdd) { onAddToPlaylist(p.id, track); onDismiss() }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Playlist baru…") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { if (newName.isNotBlank()) { onCreatePlaylist(newName.trim()); onDismiss() } }) {
                    Text("Buat")
                }
            }
        }
    }
}

@Composable
private fun SheetAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = TextSecondary, modifier = Modifier.padding(end = 16.dp))
            Text(label)
        }
    }
}

/** Large title 34/700 ala Apple Music, satu per tab. */
@Composable
fun LargeTitle(text: String, modifier: Modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
    Text(
        text,
        fontSize = 34.sp,
        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
        fontFamily = com.zaaam.liphify.ui.theme.AppFont,
        modifier = modifier,
    )
}
