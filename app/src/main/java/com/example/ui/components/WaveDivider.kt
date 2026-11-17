package com.example.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.QuranBaantoDesignSystem

/**
 * HIGH-PERFORMANCE WAVE DIVIDER
 * A flowing wave line drawn using smooth cubic bezier arcs for zero frame drop and silky smooth scrolling.
 */
@Composable
fun WaveDivider(
    modifier: Modifier = Modifier,
    color: Color? = null,
    strokeWidth: Dp = 1.8.dp,
    amplitude: Dp = 3.dp,
    wavelength: Dp = 48.dp
) {
    val strokeColor = color ?: QuranBaantoDesignSystem.colors.gold

    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(amplitude * 2 + strokeWidth + 2.dp)
            .drawWithCache {
                val w = size.width
                val midY = size.height / 2f
                val amp = amplitude.toPx()
                val wl = wavelength.toPx()
                val halfWl = wl / 2f
                val strokePx = strokeWidth.toPx()

                val path = Path().apply {
                    moveTo(0f, midY)
                    var currentX = 0f
                    var goingUp = true

                    while (currentX < w) {
                        val nextX = currentX + halfWl
                        val controlY = if (goingUp) midY - amp else midY + amp

                        cubicTo(
                            currentX + halfWl * 0.36f, controlY,
                            currentX + halfWl * 0.64f, controlY,
                            nextX, midY
                        )

                        currentX = nextX
                        goingUp = !goingUp
                    }
                }

                onDrawBehind {
                    drawPath(
                        path = path,
                        color = strokeColor,
                        style = Stroke(
                            width = strokePx,
                            cap = StrokeCap.Round
                        )
                    )
                }
            }
    )
}
