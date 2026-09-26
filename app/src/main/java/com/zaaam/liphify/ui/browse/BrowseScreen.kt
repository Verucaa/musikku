package com.zaaam.liphify.ui.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val GENRES = listOf(
    "Pop" to (Color(0xFFFC5C7D) to Color(0xFF6A82FB)),
    "Hip-Hop" to (Color(0xFFF7B733) to Color(0xFFFC4A1A)),
    "R&B" to (Color(0xFF8E2DE2) to Color(0xFF4A00E0)),
    "Electronic" to (Color(0xFF11998E) to Color(0xFF38EF7D)),
    "Rock" to (Color(0xFF414345) to Color(0xFF232526)),
    "Jazz" to (Color(0xFFC79081) to Color(0xFFDFA579)),
    "Classical" to (Color(0xFF2C5364) to Color(0xFF0F2027)),
    "Metal" to (Color(0xFF434343) to Color(0xFF000000)),
)

/**
 * PRD-007: konten boleh curated, tapi tap genre WAJIB query nyata —
 * navigasi ke Search dengan preset query genre tersebut.
 */
@Composable
fun BrowseScreen(onGenre: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Text("Browse Categories", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 8.dp))
        Text(
            "Tap genre untuk mencari yang nyata (lokal + YouTube).",
            color = com.zaaam.liphify.ui.theme.TextSecondary,
            fontSize = 13.sp,
        )
        LazyVerticalGrid(columns = GridCells.Fixed(2)) {
            items(GENRES) { (name, colors) ->
                Box(
                    Modifier.padding(6.dp).fillMaxWidth().height(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Brush.linearGradient(listOf(colors.first, colors.second)))
                        .clickable { onGenre(name) }
                        .padding(12.dp),
                    contentAlignment = Alignment.BottomStart,
                ) {
                    Text(name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            }
        }
    }
}
