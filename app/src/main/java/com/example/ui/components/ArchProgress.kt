package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FrauncesFontFamily
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.QuranBaantoDesignSystem

/**
 * ARCH PROGRESS COMPONENT
 *
 * Arch window filled from the bottom with pine to specified percentage (e.g. 40%),
 * with a gold outline, "4/10" in Fraunces inside, and "pages today" under it.
 */
@Composable
fun ArchProgress(
    pagesRead: Int = 4,
    pagesGoal: Int = 10,
    progress: Float = 0.4f,
    width: Dp = 124.dp,
    height: Dp = 180.dp,
    modifier: Modifier = Modifier
) {
    val colors = QuranBaantoDesignSystem.colors

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier.size(width = width, height = height),
            contentAlignment = Alignment.BottomCenter
        ) {
            val clampedProgress = progress.coerceIn(0f, 1f)
            Spacer(
                modifier = Modifier
                    .size(width = width, height = height)
                    .drawWithCache {
                        val w = size.width
                        val h = size.height
                        val r = w / 2f
                        val strokePx = 1.5.dp.toPx()

                        val archPath = Path().apply {
                            moveTo(0f, r)
                            arcTo(
                                rect = Rect(0f, 0f, w, 2f * r),
                                startAngleDegrees = 180f,
                                sweepAngleDegrees = 180f,
                                forceMoveTo = false
                            )
                            lineTo(w, h)
                            lineTo(0f, h)
                            close()
                        }

                        onDrawBehind {
                            drawPath(path = archPath, color = colors.softNeutral)
                            val fillHeight = h * clampedProgress
                            val fillTop = h - fillHeight
                            if (fillHeight > 0f) {
                                clipPath(archPath) {
                                    drawRect(
                                        color = colors.pine,
                                        topLeft = androidx.compose.ui.geometry.Offset(0f, fillTop),
                                        size = androidx.compose.ui.geometry.Size(w, fillHeight)
                                    )
                                }
                            }
                            drawPath(
                                path = archPath,
                                color = colors.gold,
                                style = Stroke(width = strokePx)
                            )
                        }
                    }
            )

            // Text display: inside pine fill if progress > 0, or on soft neutral arch if 0
            val isZeroProgress = progress <= 0.05f
            val textContainerHeightDp = if (isZeroProgress) 72.dp else (height * progress.coerceIn(0.25f, 1f))
            val textColor = if (isZeroProgress) colors.text else Color(0xFFF6F0E4)

            Box(
                modifier = Modifier
                    .size(width = width, height = textContainerHeightDp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$pagesRead/$pagesGoal",
                    fontFamily = FrauncesFontFamily,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Normal,
                    fontSize = 26.sp,
                    color = textColor
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "pages today",
            fontFamily = InterFontFamily,
            fontSize = 13.sp,
            color = colors.mutedText
        )
    }
}
