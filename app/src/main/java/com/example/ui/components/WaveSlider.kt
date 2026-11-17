package com.example.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.QuranBaantoDesignSystem
import kotlin.math.PI
import kotlin.math.sin

/**
 * HIGH-PERFORMANCE WAVE SLIDER COMPONENT
 * The text-size slider is a wavy track with a pine fill and a round knob.
 * Optimized with hardware-accelerated clipping and cached wave Path for zero drag lag.
 */
@Composable
fun WaveSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 18f..42f,
    modifier: Modifier = Modifier,
    waveAmplitude: Dp = 4.dp,
    wavelength: Dp = 56.dp,
    knobRadius: Dp = 8.dp
) {
    val colors = QuranBaantoDesignSystem.colors
    val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .pointerInput(valueRange) {
                detectTapGestures { offset ->
                    val newFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    val newValue = valueRange.start + newFraction * (valueRange.endInclusive - valueRange.start)
                    onValueChange(newValue)
                }
            }
            .pointerInput(valueRange) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val newFraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    val newValue = valueRange.start + newFraction * (valueRange.endInclusive - valueRange.start)
                    onValueChange(newValue)
                }
            }
    ) {
        Spacer(
            modifier = Modifier
                .matchParentSize()
                .drawWithCache {
                    val w = size.width
                    val midY = size.height / 2f
                    val amp = waveAmplitude.toPx()
                    val wl = wavelength.toPx()
                    val halfWl = wl / 2f
                    val inactiveStroke = 2.dp.toPx()
                    val activeStroke = 2.4.dp.toPx()
                    val knobR = knobRadius.toPx()

                    val wavePath = Path().apply {
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
                        val thumbX = (w * fraction).coerceIn(0f, w)

                        clipRect(left = thumbX, right = w) {
                            drawPath(
                                path = wavePath,
                                color = colors.rules,
                                style = Stroke(width = inactiveStroke, cap = StrokeCap.Round)
                            )
                        }

                        if (thumbX > 0f) {
                            clipRect(left = 0f, right = thumbX) {
                                drawPath(
                                    path = wavePath,
                                    color = if (colors.isDark) colors.pineStrokes else colors.pine,
                                    style = Stroke(width = activeStroke, cap = StrokeCap.Round)
                                )
                            }
                        }

                        val wavePhase = (thumbX % wl) / wl
                        val thumbY = midY - amp * sin(wavePhase * 2f * PI).toFloat()

                        drawCircle(
                            color = if (colors.isDark) colors.pineText else colors.pine,
                            radius = knobR,
                            center = Offset(thumbX, thumbY)
                        )
                    }
                }
        )
    }
}
