package com.zaaam.liphify.ui.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.zaaam.liphify.data.local.PlaylistEntity
import com.zaaam.liphify.domain.model.Track

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TrackRow(
    track: Track,
    onPlay: () -> Unit,
    onMenu: () -> Unit,
    subtitle: String = track.artist,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onPlay).padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = track.artwork,
            contentDescription = null,
            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)),
        )
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.basicMarquee())
            Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis, color = com.zaaam.liphify.ui.theme.TextSecondary)
        }
        TextButton(onClick = onMenu) { Text("⋯", color = com.zaaam.liphify.ui.theme.TextSecondary) }
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
            AsyncImage(
                model = track.artwork,
                contentDescription = null,
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)),
            )
            Column(Modifier.padding(start = 12.dp)) {
                Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, color = com.zaaam.liphify.ui.theme.TextSecondary)
            }
        }
        if (!picking) {
            SheetAction("▶  Play Next") { onPlayNext(track); onDismiss() }
            SheetAction("＋  Play Last") { onPlayLast(track); onDismiss() }
            SheetAction("📁  Add to Playlist…") { picking = true }
        } else {
            playlists.forEach { p ->
                SheetAction("📁  ${p.name}") { onAddToPlaylist(p.id, track); onDismiss() }
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
private fun SheetAction(label: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp))
    }
}
