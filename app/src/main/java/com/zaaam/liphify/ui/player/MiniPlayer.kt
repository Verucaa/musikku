package com.zaaam.liphify.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/** PRD-010: tidak dirender sama sekali saat idle (caller yang hide). */
@Composable
fun MiniPlayer(
    state: PlayerUiState,
    onTap: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
) {
    val cur = state.current ?: return
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onTap).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(model = cur.artwork, contentDescription = null, modifier = Modifier.size(34.dp))
        Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
            Text(cur.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(cur.artist, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        IconButton(onClick = onToggle) {
            Icon(if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null)
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Filled.SkipNext, contentDescription = null)
        }
    }
}
