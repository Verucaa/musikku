package com.zaaam.liphify.ui.browse

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zaaam.liphify.ui.player.PlaybackViewModel

/** PRD-007: chip genre harus trigger query nyata (bukan diam). Navigasi ke search via callback sederhana. */
@Composable
fun BrowseScreen(player: PlaybackViewModel) {
    val genres = listOf("Pop", "Rock", "Jazz", "Hip-Hop", "Electronic", "Classical", "R&B", "Metal")
    var picked by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Browse")
        LazyRow {
            items(genres) { g ->
                FilterChip(
                    selected = picked == g,
                    onClick = { picked = g },
                    label = { Text(g) },
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
        }
        Row(Modifier.padding(top = 12.dp)) {
            Text(
                picked?.let { "Genre '$it' dipilih — buka tab Search dan ketik genre ini untuk hasil nyata (lokal + YouTube)." }
                    ?: "Pilih genre untuk mulai jelajah.",
            )
        }
    }
}
