package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * EXACT DESIGN TOKENS FOR QURAN BAANTO
 *
 * Light:
 * - background: #F6F0E4
 * - text: #1B1B19
 * - muted text: #6A6558
 * - soft neutral: #E9E1D1
 * - pine: #1F3D36
 * - gold: #C5A77D
 * - wine: #642B3A (accent only)
 * - rules: #E9E1D1
 * - deepest section: #15181C (for lower section in light mode)
 *
 * Dark:
 * - background: #15181C
 * - text: #F1EBDD
 * - muted text: #A29F94
 * - soft neutral: #262B31
 * - pine: #2F6B5A
 * - pine text and links: #7DB8A5
 * - pine strokes: #4B8F7B
 * - gold: #C5A77D
 * - wine text: #D58A9C
 * - wine fill: #8A3A50
 * - rules: #2A2F36
 * - deepest section: #0F1114
 */
data class QuranBaantoColors(
    val isDark: Boolean,
    val background: Color,
    val text: Color,
    val mutedText: Color,
    val softNeutral: Color,
    val pine: Color,
    val pineText: Color,
    val pineStrokes: Color,
    val gold: Color,
    val wine: Color,
    val wineText: Color,
    val wineFill: Color,
    val rules: Color,
    val deepestSection: Color
) {
    val onSurfaceVariant: Color
        get() = mutedText
}

val LightQuranBaantoColors = QuranBaantoColors(
    isDark = false,
    background = Color(0xFFF6F0E4),
    text = Color(0xFF1B1B19),
    mutedText = Color(0xFF6A6558),
    softNeutral = Color(0xFFE9E1D1),
    pine = Color(0xFF1F3D36),
    pineText = Color(0xFF1F3D36),
    pineStrokes = Color(0xFF1F3D36),
    gold = Color(0xFFC5A77D),
    wine = Color(0xFF642B3A),
    wineText = Color(0xFF642B3A),
    wineFill = Color(0xFF642B3A),
    rules = Color(0xFFE9E1D1),
    deepestSection = Color(0xFF15181C)
)

val DarkQuranBaantoColors = QuranBaantoColors(
    isDark = true,
    background = Color(0xFF15181C),
    text = Color(0xFFF1EBDD),
    mutedText = Color(0xFFA29F94),
    softNeutral = Color(0xFF262B31),
    pine = Color(0xFF2F6B5A),
    pineText = Color(0xFF7DB8A5),
    pineStrokes = Color(0xFF4B8F7B),
    gold = Color(0xFFC5A77D),
    wine = Color(0xFF8A3A50),
    wineText = Color(0xFFD58A9C),
    wineFill = Color(0xFF8A3A50),
    rules = Color(0xFF2A2F36),
    deepestSection = Color(0xFF0F1114)
)

val LocalQuranBaantoColors = staticCompositionLocalOf { LightQuranBaantoColors }

object QuranBaantoDesignSystem {
    val colors: QuranBaantoColors
        @Composable
        @ReadOnlyComposable
        get() = LocalQuranBaantoColors.current
}
