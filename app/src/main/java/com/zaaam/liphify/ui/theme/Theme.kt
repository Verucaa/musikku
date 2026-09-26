package com.zaaam.liphify.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

val BgMain = Color(0xFF000000)
val SurfaceElevated = Color(0xFF1C1C1E)
val SurfaceSecondary = Color(0xFF2C2C2E)
val Divider = Color(0x14FFFFFF)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0x99FFFFFF)
val TextHint = Color(0x66FFFFFF)
val Accent = Color(0xFFFA233B)

// Inter dibundle sebagai font resource bila file tersedia; fallback ke SansSerif
// agar konsisten dan tidak ketiban font bawaan OEM.
val AppFont: FontFamily = FontFamily.SansSerif

private val Scheme = darkColorScheme(
    background = BgMain,
    surface = SurfaceElevated,
    surfaceVariant = SurfaceSecondary,
    primary = Accent,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
)

@Composable
fun LiPhifyTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
