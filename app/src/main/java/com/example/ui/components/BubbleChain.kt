package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.QuranBaantoDesignSystem

enum class DayBubbleStatus {
    PAST_READ,    // Prior day: user read -> Wine
    PAST_MISSED,  // Prior day: user missed -> Grey
    TODAY,        // Today: not read yet -> Gold outline
    TODAY_READ,   // Today: read -> Pine fill with gold outline
    FUTURE        // Future days: faint soft neutral
}

data class DayBubbleItem(
    val dayLetter: String,
    val status: DayBubbleStatus
)

/**
 * HIGH-PERFORMANCE BUBBLE CHAIN
 * Displays 7 overlapping circles for Monday to Sunday with day letters inside each bubble.
 * - Days prior to today: Wine if user has read, Grey if missed.
 * - Today: Highlighted with Gold outline (and pine if read).
 * - Future days: Faint soft neutral.
 */
@Composable
fun BubbleChain(
    modifier: Modifier = Modifier,
    circleDiameter: Dp = 32.dp,
    overlap: Dp = 10.dp,
    days: List<DayBubbleItem> = defaultDayBubbles()
) {
    val colors = QuranBaantoDesignSystem.colors
    val stepDp = circleDiameter - overlap
    val totalWidthDp = circleDiameter + stepDp * (days.size - 1).coerceAtLeast(0)

    Box(
        modifier = modifier
            .width(totalWidthDp)
            .height(circleDiameter)
    ) {
        days.forEachIndexed { index, item ->
            val isDark = colors.isDark

            // Colors based on status & theme
            val (bgColor, borderColor, borderWidth, textColor) = when (item.status) {
                DayBubbleStatus.PAST_READ -> {
                    // Wine colored bubble for read past days
                    val bg = if (isDark) Color(0xFF8A3A50) else Color(0xFF762E40)
                    val text = if (isDark) Color(0xFFF1EBDD) else Color(0xFFF6F0E4)
                    QuadColor(bg, Color.Transparent, 0.dp, text)
                }
                DayBubbleStatus.PAST_MISSED -> {
                    // Grey bubble for missed past days
                    val bg = if (isDark) Color(0xFF2C3037) else Color(0xFFD6CFC3)
                    val text = if (isDark) Color(0xFF88847C) else Color(0xFF7A756C)
                    QuadColor(bg, Color.Transparent, 0.dp, text)
                }
                DayBubbleStatus.TODAY -> {
                    // Gold outline for today (uncompleted)
                    val bg = if (isDark) colors.softNeutral.copy(alpha = 0.5f) else colors.background
                    val border = colors.gold
                    val text = colors.gold
                    QuadColor(bg, border, 1.8.dp, text)
                }
                DayBubbleStatus.TODAY_READ -> {
                    // Pine fill with gold outline for today (completed)
                    val bg = colors.pine
                    val border = colors.gold
                    val text = if (isDark) Color(0xFFF1EBDD) else Color(0xFFF6F0E4)
                    QuadColor(bg, border, 1.8.dp, text)
                }
                DayBubbleStatus.FUTURE -> {
                    // Faint soft neutral for future days
                    val bg = if (isDark) colors.softNeutral else Color(0xFFE9E1D1)
                    val border = if (isDark) Color(0xFF333840) else Color(0xFFC5A77D).copy(alpha = 0.35f)
                    val text = if (isDark) Color(0xFFA29F94).copy(alpha = 0.45f) else Color(0xFF6A6558).copy(alpha = 0.5f)
                    QuadColor(bg, border, 1.dp, text)
                }
            }

            Box(
                modifier = Modifier
                    .offset(x = stepDp * index)
                    .size(circleDiameter)
                    .clip(CircleShape)
                    .background(bgColor)
                    .then(
                        if (borderWidth > 0.dp) {
                            Modifier.border(borderWidth, borderColor, CircleShape)
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.dayLetter,
                    fontFamily = InterFontFamily,
                    fontWeight = if (item.status == DayBubbleStatus.TODAY || item.status == DayBubbleStatus.TODAY_READ) {
                        FontWeight.Bold
                    } else {
                        FontWeight.SemiBold
                    },
                    fontSize = 11.sp,
                    color = textColor
                )
            }
        }
    }
}

private data class QuadColor(
    val bg: Color,
    val border: Color,
    val borderWidth: Dp,
    val text: Color
)

private fun defaultDayBubbles(): List<DayBubbleItem> {
    val labels = listOf("M", "T", "W", "T", "F", "S", "S")
    return labels.mapIndexed { index, letter ->
        val status = when {
            index < 4 -> DayBubbleStatus.PAST_READ
            index == 4 -> DayBubbleStatus.PAST_MISSED
            index == 5 -> DayBubbleStatus.TODAY
            else -> DayBubbleStatus.FUTURE
        }
        DayBubbleItem(letter, status)
    }
}
