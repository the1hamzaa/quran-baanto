package com.example.ui.streak

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.QuranBaantoDesignSystem

/**
 * Flush-right pill button matching all reference screenshots:
 * rounded left cap (28dp), extends cleanly to the right edge of the viewport,
 * bold label on the left and subtle "→" on the right.
 */
@Composable
fun RightExtendingPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = QuranBaantoDesignSystem.colors.pine,
    contentColor: Color = Color(0xFFF1EBDD),
    height: Dp = 54.dp
) {
    val shape = RoundedCornerShape(topStart = height / 2, bottomStart = height / 2)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 28.dp)
            .height(height)
            .clip(shape)
            .background(if (enabled) containerColor else containerColor.copy(alpha = 0.4f))
            .clickable(enabled = enabled) { onClick() }
            .semantics { role = Role.Button }
            .padding(start = 32.dp, end = 28.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = if (enabled) contentColor else contentColor.copy(alpha = 0.6f)
            )
            Text(
                text = "→",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp,
                color = if (enabled) contentColor else contentColor.copy(alpha = 0.6f)
            )
        }
    }
}

/**
 * Top-right soft neutral decorative circle cropped at screen corner.
 */
@Composable
fun TopRightProgressBubble(
    color: Color = QuranBaantoDesignSystem.colors.softNeutral,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(170.dp)
    ) {
        val w = size.width
        drawCircle(
            color = color,
            radius = 78.dp.toPx(),
            center = Offset(w - 8.dp.toPx(), 18.dp.toPx())
        )
    }
}

/**
 * 3-page HorizontalPager bottom navigation indicator row (`← Streak   • — •   Circles →`).
 */
@Composable
fun PagerFooterIndicator(
    currentPage: Int,
    onSelectPage: (Int) -> Unit,
    onCharcoal: Boolean,
    leftLabel: String? = null,
    onLeftClick: (() -> Unit)? = null,
    rightLabel: String? = null,
    onRightClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = QuranBaantoDesignSystem.colors
    val activeDotColor = if (onCharcoal) colors.gold else colors.pine
    val inactiveDotColor = if (onCharcoal) Color(0xFF5D594F) else colors.mutedText.copy(alpha = 0.35f)
    val navLabelColor = if (onCharcoal) Color(0xFFA29F94) else colors.mutedText

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 12.dp)
    ) {
        // Left label
        if (leftLabel != null && onLeftClick != null) {
            Text(
                text = leftLabel,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = navLabelColor,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onLeftClick() }
                    .padding(vertical = 6.dp, horizontal = 4.dp)
            )
        }

        // Center 3 dots
        Row(
            modifier = Modifier.align(Alignment.Center),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0..2) {
                val isSelected = i == currentPage
                Box(
                    modifier = Modifier
                        .width(if (isSelected) 22.dp else 5.dp)
                        .height(5.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) activeDotColor else inactiveDotColor)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelectPage(i) }
                )
            }
        }

        // Right label
        if (rightLabel != null && onRightClick != null) {
            Text(
                text = rightLabel,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = navLabelColor,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onRightClick() }
                    .padding(vertical = 6.dp, horizontal = 4.dp)
            )
        }
    }
}

/**
 * Gold half-dome rising sun/arch icon with diagonal checkmark/ray as seen in Streak reference.
 */
@Composable
fun RisingHalfDomeIcon(
    fillColor: Color,
    cutoutColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.size(width = 30.dp, height = 16.dp)) {
        val w = size.width
        val h = size.height
        val domePath = Path().apply {
            moveTo(0f, h)
            arcTo(
                rect = Rect(0f, 0f, w, h * 2f),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            close()
        }
        drawPath(path = domePath, color = fillColor)
        drawLine(
            color = cutoutColor,
            start = Offset(w * 0.25f, h * 0.72f),
            end = Offset(w * 0.62f, h * 0.14f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}
