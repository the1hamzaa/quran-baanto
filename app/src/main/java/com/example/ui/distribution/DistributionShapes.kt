package com.example.ui.distribution

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FrauncesFontFamily
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.QuranBaantoDesignSystem
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Arch window shape: rounded semi-circle top, straight vertical sides, flat bottom.
 */
val ArchWindowShape = GenericShape { size, _ ->
    val r = size.width / 2f
    val topRect = Rect(0f, 0f, size.width, size.width)
    arcTo(rect = topRect, startAngleDegrees = 180f, sweepAngleDegrees = 180f, forceMoveTo = false)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}

/**
 * Clean Arch Window vector outline or filled icon.
 */
@Composable
fun ArchWindowVector(
    modifier: Modifier = Modifier,
    outlineColor: Color = QuranBaantoDesignSystem.colors.gold,
    fillColor: Color? = null,
    strokeWidth: Dp = 1.6.dp
) {
    Spacer(
        modifier = modifier.drawWithCache {
            val w = size.width
            val h = size.height
            val r = w / 2f
            val strokePx = strokeWidth.toPx()

            val path = Path().apply {
                moveTo(0f, h)
                lineTo(0f, r)
                arcTo(
                    rect = Rect(0f, 0f, w, w),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )
                lineTo(w, h)
                close()
            }

            onDrawBehind {
                if (fillColor != null) {
                    drawPath(path = path, color = fillColor)
                }
                drawPath(
                    path = path,
                    color = outlineColor,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }
    )
}

/**
 * Beaded 30-portion circular ring vector icon.
 * Matches Monthly Quran khatm icon from reference screenshots.
 */
@Composable
fun BeadedCircleVector(
    modifier: Modifier = Modifier,
    totalBeads: Int = 30,
    completedBeads: Int = 0,
    singleWineIndex: Int? = null,
    wineIndices: Set<Int> = emptySet(),
    goldColor: Color = QuranBaantoDesignSystem.colors.gold,
    pineColor: Color = QuranBaantoDesignSystem.colors.pine,
    wineColor: Color = QuranBaantoDesignSystem.colors.wineFill,
    unclaimedColor: Color = Color(0xFF333842)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)
        val radius = (w / 2f) * 0.82f
        val beadRadius = (w / 2f) * 0.082f

        for (i in 0 until totalBeads) {
            // angle from top (-90 degrees) clockwise
            val angle = (-90.0 + (i.toDouble() / totalBeads) * 360.0) * (PI / 180.0)
            val bx = center.x + radius * cos(angle).toFloat()
            val by = center.y + radius * sin(angle).toFloat()

            val color = when {
                i == singleWineIndex || i in wineIndices -> wineColor
                i < completedBeads -> pineColor
                else -> goldColor.copy(alpha = 0.85f)
            }

            drawCircle(
                color = color,
                radius = beadRadius,
                center = Offset(bx, by)
            )
        }
    }
}

/**
 * Concentric Target Circles vector icon with center filled dot.
 * Matches Amal recitation icon from reference screenshots.
 */
@Composable
fun ConcentricCirclesVector(
    modifier: Modifier = Modifier,
    pineColor: Color = QuranBaantoDesignSystem.colors.pine,
    strokeColor: Color = QuranBaantoDesignSystem.colors.gold.copy(alpha = 0.65f),
    strokeWidth: Dp = 1.4.dp
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)
        val outerRadius = (w / 2f) * 0.88f
        val innerRadius = outerRadius * 0.65f
        val dotRadius = innerRadius * 0.48f

        // Outer ring
        drawCircle(
            color = strokeColor,
            radius = outerRadius,
            center = center,
            style = Stroke(width = strokeWidth.toPx())
        )

        // Middle ring
        drawCircle(
            color = strokeColor.copy(alpha = 0.85f),
            radius = innerRadius,
            center = center,
            style = Stroke(width = strokeWidth.toPx())
        )

        // Center filled pine circle
        drawCircle(
            color = pineColor,
            radius = dotRadius,
            center = center
        )
    }
}

/**
 * Overlapping circles vector icon.
 * Matches Online Room icon from reference screenshots.
 */
@Composable
fun OverlappingCirclesVector(
    modifier: Modifier = Modifier,
    colors: com.example.ui.theme.QuranBaantoColors = QuranBaantoDesignSystem.colors
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Circle 1 (Pine)
        drawCircle(
            color = colors.pine,
            radius = w * 0.38f,
            center = Offset(w * 0.38f, h * 0.52f)
        )

        // Circle 2 (Gold)
        drawCircle(
            color = colors.gold,
            radius = w * 0.30f,
            center = Offset(w * 0.68f, h * 0.40f)
        )

        // Circle 3 (Small Charcoal/Accent)
        drawCircle(
            color = if (colors.isDark) Color(0xFF15181C) else Color(0xFF1B1B19),
            radius = w * 0.16f,
            center = Offset(w * 0.62f, h * 0.76f)
        )
    }
}

/**
 * Smooth flowing wave progress track.
 * Progress fills in pine from left to right, background track in soft gold or neutral.
 */
@Composable
fun WaveTrack(
    progress: Float,
    modifier: Modifier = Modifier,
    pineColor: Color = QuranBaantoDesignSystem.colors.pine,
    trackColor: Color = QuranBaantoDesignSystem.colors.gold.copy(alpha = 0.35f),
    strokeWidth: Dp = 2.4.dp
) {
    val clamped = progress.coerceIn(0f, 1f)

    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp)
            .drawWithCache {
                val w = size.width
                val h = size.height
                val midY = h / 2f
                val amp = 2.8.dp.toPx()
                val strokePx = strokeWidth.toPx()

                val fullPath = Path().apply {
                    moveTo(0f, midY)
                    cubicTo(w * 0.25f, midY - amp, w * 0.75f, midY + amp, w, midY)
                }

                val progressPath = if (clamped > 0.01f) {
                    val progressW = w * clamped
                    Path().apply {
                        moveTo(0f, midY)
                        val c1x = (w * 0.25f) * clamped
                        val c2x = (w * 0.75f) * clamped
                        cubicTo(c1x, midY - amp, c2x, midY + amp * clamped, progressW, midY)
                    }
                } else null

                onDrawBehind {
                    drawPath(
                        path = fullPath,
                        color = trackColor,
                        style = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                    if (progressPath != null) {
                        drawPath(
                            path = progressPath,
                            color = pineColor,
                            style = Stroke(width = strokePx * 1.15f, cap = StrokeCap.Round)
                        )
                    }
                }
            }
    )
}

/**
 * Segmented allocation bar for Local Rooms (30 Paras or custom Amal target).
 * Matches Local_one-time_room and Local_monthly_room screenshots.
 */
@Composable
fun SegmentedAllocationBar(
    allocations: List<Int>,
    total: Int = 30,
    modifier: Modifier = Modifier
) {
    val colors = listOf(
        QuranBaantoDesignSystem.colors.pine,
        Color(0xFF333842),
        QuranBaantoDesignSystem.colors.gold,
        Color(0xFF3F6C5B),
        Color(0xFF756A54),
        Color(0xFF9FA8A3),
        QuranBaantoDesignSystem.colors.wineFill
    )

    val valid = allocations.filter { it > 0 }
    val sum = valid.sum().coerceAtLeast(1)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(QuranBaantoDesignSystem.colors.softNeutral.copy(alpha = 0.5f)),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp)
    ) {
        if (valid.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(QuranBaantoDesignSystem.colors.softNeutral)
            )
        } else {
            valid.forEachIndexed { index, count ->
                val weight = (count.toFloat() / maxOf(sum, total).toFloat()).coerceAtLeast(0.01f)
                Box(
                    modifier = Modifier
                        .weight(weight)
                        .fillMaxHeight()
                        .background(colors[index % colors.size])
                )
            }
            if (sum < total) {
                val remainingWeight = ((total - sum).toFloat() / total.toFloat()).coerceAtLeast(0.01f)
                Box(
                    modifier = Modifier
                        .weight(remainingWeight)
                        .fillMaxHeight()
                        .background(Color.Transparent)
                )
            }
        }
    }
}

enum class ParaTileState {
    OPEN,      // Gold outline, transparent bg
    CLAIMED,   // Black/dark slate fill, cream text
    COMPLETED, // Pine green fill, cream text
    YOURS      // Wine fill, cream text
}

/**
 * Arch-shaped button tile for the 30-para grid (Online One-Time room & Choose Paras).
 * Exactly matches Online_one-time_room and Online_monthly___choose_paras screenshots.
 */
@Composable
fun ArchParaTile(
    paraNumber: Int,
    state: ParaTileState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = QuranBaantoDesignSystem.colors
    val isDark = colors.isDark

    val bgColor = when (state) {
        ParaTileState.OPEN -> Color.Transparent
        ParaTileState.CLAIMED -> if (isDark) Color(0xFF1B1E23) else Color(0xFF1B1B19)
        ParaTileState.COMPLETED -> colors.pine
        ParaTileState.YOURS -> if (isDark) Color(0xFF8A3A50) else Color(0xFF642B3A)
    }

    val textColor = when (state) {
        ParaTileState.OPEN -> colors.text
        ParaTileState.CLAIMED, ParaTileState.COMPLETED, ParaTileState.YOURS -> Color(0xFFF1EBDD)
    }

    val borderModifier = if (state == ParaTileState.OPEN) {
        Modifier.border(1.4.dp, colors.gold, ArchWindowShape)
    } else Modifier

    Box(
        modifier = modifier
            .size(width = 46.dp, height = 56.dp)
            .clip(ArchWindowShape)
            .background(bgColor)
            .then(borderModifier)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = paraNumber.toString(),
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = textColor
        )
    }
}

/**
 * Wave shape for the bottom edge of the Online Room pine banner.
 */
val OnlineHeaderWaveShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(0f, 0f)
    lineTo(w, 0f)
    lineTo(w, h - 30f)
    cubicTo(
        w * 0.72f, h - 8f,
        w * 0.28f, h - 52f,
        0f, h - 22f
    )
    close()
}

/**
 * Top Architectural Pine Banner for Online Rooms.
 * Matches Online_one-time_room, Online_monthly_room, and Online_amal_room screenshots precisely.
 */
@Composable
fun OnlineArchitecturalHeader(
    title: String,
    roomPasscode: String?,
    isHost: Boolean = false,
    roleBadgeText: String = "HOST",
    onBack: () -> Unit,
    onShowQrCode: () -> Unit,
    onOpenDueReminder: () -> Unit,
    onCopyCode: () -> Unit,
    onOpenAdminPowers: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val pineBg = Color(0xFF1E3831)
    val creamText = Color(0xFFF1EBDD)
    val goldAccent = Color(0xFFE4C58F)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(OnlineHeaderWaveShape)
            .background(pineBg)
    ) {
        // Decorative concentric stroke circles in top-right corner
        Canvas(modifier = Modifier.fillMaxWidth().height(230.dp)) {
            val w = size.width
            val center = Offset(w * 0.88f, 30f)
            drawCircle(
                color = Color(0x18F1EBDD),
                radius = 160.dp.toPx(),
                center = center,
                style = Stroke(width = 1.2.dp.toPx())
            )
            drawCircle(
                color = Color(0x18F1EBDD),
                radius = 110.dp.toPx(),
                center = center,
                style = Stroke(width = 1.2.dp.toPx())
            )
            drawCircle(
                color = Color(0x18F1EBDD),
                radius = 60.dp.toPx(),
                center = center,
                style = Stroke(width = 1.2.dp.toPx())
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 44.dp)
        ) {
            // Top row: Back button & Online / Host badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = creamText,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isHost && onOpenAdminPowers != null) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(goldAccent)
                                .clickable { onOpenAdminPowers() }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                                .testTag("admin_powers_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = roleBadgeText,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = Color(0xFF15181C),
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = if (isHost && onOpenAdminPowers != null) {
                            Modifier.clickable { onOpenAdminPowers() }
                        } else Modifier
                    ) {
                        Text(
                            text = "ONLINE",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = goldAccent,
                            letterSpacing = 1.2.sp
                        )
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(goldAccent)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Room Title
            Text(
                text = title,
                fontFamily = FrauncesFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = creamText,
                maxLines = 2
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ROOM CODE caption
            Text(
                text = "ROOM CODE",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 10.5.sp,
                color = goldAccent,
                letterSpacing = 1.4.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Code & Action icons row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = roomPasscode.orEmpty(),
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = creamText,
                    letterSpacing = 1.sp,
                    modifier = Modifier.weight(1f)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // QR Code icon button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color(0x44F1EBDD), CircleShape)
                            .clickable { onShowQrCode() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode2,
                            contentDescription = "Show QR Code",
                            tint = creamText,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    // Bell / Reminder icon button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color(0x44F1EBDD), CircleShape)
                            .clickable { onOpenDueReminder() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Due reminder",
                            tint = creamText,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Copy Code icon button
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .border(1.dp, Color(0x44F1EBDD), CircleShape)
                            .clickable { onCopyCode() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy code",
                            tint = creamText,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}

