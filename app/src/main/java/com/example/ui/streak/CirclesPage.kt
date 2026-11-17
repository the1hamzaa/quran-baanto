package com.example.ui.streak

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.db.StreakCircleEntity
import com.example.data.db.StreakCircleMemberEntity
import com.example.ui.components.WaveDivider
import com.example.ui.theme.FrauncesFontFamily
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.QuranBaantoDesignSystem
import com.example.util.RoomQrCodeHelper
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer

/**
 * Page 2 of Progress Pager: Circles
 * Matches Circles_b4e31d99.png & Circles___dark_2b993fdc.png precisely.
 * Uses only real circles & members from DB, syncing todayPagesRead live.
 */
@Composable
fun CirclesPage(
    circles: List<StreakCircleEntity>,
    allMembers: List<StreakCircleMemberEntity>,
    todayPagesRead: Int,
    onCircleClick: (Long) -> Unit,
    onOpenCreateCircle: () -> Unit,
    onOpenJoinCircle: () -> Unit,
    onGoToPlans: () -> Unit,
    onSelectPage: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = QuranBaantoDesignSystem.colors
    var showChooserDialog by remember { mutableStateOf(false) }

    val membersByCircle = remember(allMembers) { allMembers.groupBy { it.circleId } }
    val totalFriendsCount = remember(allMembers) {
        allMembers.count { !it.isCurrentUser }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 28.dp)
                    .padding(top = 36.dp)
            ) {
                Text(
                    text = "Circles",
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 44.sp,
                    letterSpacing = (-0.6).sp,
                    color = colors.text
                )
                Spacer(modifier = Modifier.height(4.dp))
                val subtitleText = if (circles.isEmpty()) {
                    "No circles yet"
                } else {
                    val circleWord = if (circles.size == 1) "circle" else "circles"
                    val memberLabel = if (totalFriendsCount > 0) {
                        "$totalFriendsCount ${if (totalFriendsCount == 1) "friend" else "friends"}"
                    } else {
                        val totalMem = allMembers.size
                        "$totalMem ${if (totalMem == 1) "member" else "members"}"
                    }
                    "${circles.size} $circleWord · $memberLabel"
                }
                Text(
                    text = subtitleText,
                    fontFamily = InterFontFamily,
                    fontSize = 15.sp,
                    color = colors.mutedText
                )
            }

            // Centerpiece Organic Cluster OR Clean Empty State
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (circles.isEmpty()) {
                    EmptyCirclesCluster(
                        onClick = { showChooserDialog = true },
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                } else {
                    val boxMaxWidth = maxWidth
                    val boxMaxHeight = maxHeight
                    val sortedCircles = remember(circles, membersByCircle) {
                        circles.sortedByDescending { membersByCircle[it.id]?.size ?: 1 }
                    }
                    val firstCluster = sortedCircles.take(4)
                    val extraCircles = sortedCircles.drop(4)

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        OrganicCirclesCluster(
                            circles = firstCluster,
                            membersByCircle = membersByCircle,
                            todayPagesRead = todayPagesRead,
                            availableWidth = boxMaxWidth,
                            availableHeight = (boxMaxHeight.coerceAtLeast(320.dp)).coerceAtMost(420.dp),
                            onCircleClick = onCircleClick
                        )

                        if (extraCircles.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 28.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                extraCircles.forEach { circle ->
                                    val members = membersByCircle[circle.id].orEmpty()
                                    val doneCount = countDoneMembers(members, todayPagesRead)
                                    val palette = resolveCirclePalette(circle.colorIndex, colors)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(18.dp))
                                            .background(palette.fill)
                                            .clickable { onCircleClick(circle.id) }
                                            .padding(horizontal = 18.dp, vertical = 14.dp)
                                            .testTag("circle_bubble_${circle.id}"),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = circle.name,
                                            fontFamily = FrauncesFontFamily,
                                            fontSize = 18.sp,
                                            color = palette.textColor
                                        )
                                        Text(
                                            text = "$doneCount of ${members.size} read today",
                                            fontFamily = InterFontFamily,
                                            fontSize = 13.sp,
                                            color = palette.subTextColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Action & Pager Footer
            Column(modifier = Modifier.fillMaxWidth()) {
                RightExtendingPillButton(
                    text = "Create or join a circle",
                    onClick = { showChooserDialog = true },
                    modifier = Modifier.testTag("create_or_join_circle_button")
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = if (circles.isEmpty()) {
                        "Create a circle or enter an invite code to read together."
                    } else {
                        "Tap a circle to see who's in it and nudge them."
                    },
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    color = colors.mutedText,
                    modifier = Modifier.padding(horizontal = 28.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                PagerFooterIndicator(
                    currentPage = 2,
                    onSelectPage = onSelectPage,
                    onCharcoal = false,
                    leftLabel = "← Plans",
                    onLeftClick = onGoToPlans
                )
            }
        }
    }

    // Create or Join Circle Modal
    if (showChooserDialog) {
        Dialog(onDismissRequest = { showChooserDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = colors.background,
                border = BorderStroke(1.dp, colors.gold.copy(alpha = 0.55f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "CIRCLES",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            letterSpacing = 1.2.sp,
                            color = colors.mutedText
                        )
                        IconButton(
                            onClick = { showChooserDialog = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = colors.mutedText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Read together",
                        fontFamily = FrauncesFontFamily,
                        fontSize = 28.sp,
                        color = colors.text
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Start a private circle with family or enter a friend's invite code.",
                        fontFamily = InterFontFamily,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = colors.mutedText
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    WaveDivider(color = colors.gold.copy(alpha = 0.55f), strokeWidth = 1.2.dp)
                    Spacer(modifier = Modifier.height(18.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(colors.pine)
                            .clickable {
                                showChooserDialog = false
                                onOpenCreateCircle()
                            }
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                            .testTag("chooser_create_circle_button")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Create a new circle",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = Color(0xFFF1EBDD)
                            )
                            Text(
                                text = "→",
                                fontFamily = InterFontFamily,
                                fontSize = 16.sp,
                                color = Color(0xFFF1EBDD)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.3.dp, colors.gold.copy(alpha = 0.8f), RoundedCornerShape(24.dp))
                            .clickable {
                                showChooserDialog = false
                                onOpenJoinCircle()
                            }
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                            .testTag("chooser_join_circle_button")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Join with invite code",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = colors.text
                            )
                            Text(
                                text = "→",
                                fontFamily = InterFontFamily,
                                fontSize = 16.sp,
                                color = colors.pineText
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyCirclesCluster(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = QuranBaantoDesignSystem.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .clickable { onClick() }
            .padding(vertical = 20.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(164.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = size.minDimension / 2f
                drawCircle(
                    color = colors.gold.copy(alpha = 0.25f),
                    radius = r - 12.dp.toPx(),
                    style = Stroke(width = 1.2.dp.toPx())
                )
                drawArc(
                    color = colors.gold,
                    startAngle = -65f,
                    sweepAngle = 210f,
                    useCenter = false,
                    topLeft = Offset(4.dp.toPx(), 4.dp.toPx()),
                    size = Size(size.width - 8.dp.toPx(), size.height - 8.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
                drawCircle(
                    color = colors.softNeutral,
                    radius = r - 22.dp.toPx()
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "0",
                    fontFamily = FrauncesFontFamily,
                    fontSize = 42.sp,
                    color = colors.pineText
                )
                Text(
                    text = "circles",
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    color = colors.mutedText
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "No circles yet",
            fontFamily = FrauncesFontFamily,
            fontSize = 24.sp,
            color = colors.text
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Keep each other consistent with daily gentle nudges.",
            fontFamily = InterFontFamily,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            color = colors.mutedText
        )
    }
}

@Composable
private fun OrganicCirclesCluster(
    circles: List<StreakCircleEntity>,
    membersByCircle: Map<Long, List<StreakCircleMemberEntity>>,
    todayPagesRead: Int,
    availableWidth: Dp,
    availableHeight: Dp,
    onCircleClick: (Long) -> Unit
) {
    val scale = (availableWidth / 390.dp).coerceIn(0.82f, 1.15f)
    val clusterHeight = availableHeight.coerceIn(280.dp, 390.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(clusterHeight)
    ) {
        when (circles.size) {
            1 -> {
                val c = circles[0]
                CircleOrbBubble(
                    circle = c,
                    members = membersByCircle[c.id].orEmpty(),
                    todayPagesRead = todayPagesRead,
                    diameter = 210.dp * scale,
                    isPrimary = true,
                    onClick = { onCircleClick(c.id) },
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            2 -> {
                val c0 = circles[0]
                val c1 = circles[1]
                CircleOrbBubble(
                    circle = c0,
                    members = membersByCircle[c0.id].orEmpty(),
                    todayPagesRead = todayPagesRead,
                    diameter = 196.dp * scale,
                    isPrimary = true,
                    onClick = { onCircleClick(c0.id) },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = 24.dp * scale, y = 24.dp * scale)
                )
                CircleOrbBubble(
                    circle = c1,
                    members = membersByCircle[c1.id].orEmpty(),
                    todayPagesRead = todayPagesRead,
                    diameter = 144.dp * scale,
                    isPrimary = false,
                    onClick = { onCircleClick(c1.id) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = (-24).dp * scale, y = (-26).dp * scale)
                )
            }
            3 -> {
                val c0 = circles[0]
                val c1 = circles[1]
                val c2 = circles[2]
                CircleOrbBubble(
                    circle = c0,
                    members = membersByCircle[c0.id].orEmpty(),
                    todayPagesRead = todayPagesRead,
                    diameter = 192.dp * scale,
                    isPrimary = true,
                    onClick = { onCircleClick(c0.id) },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = 20.dp * scale, y = 14.dp * scale)
                )
                CircleOrbBubble(
                    circle = c1,
                    members = membersByCircle[c1.id].orEmpty(),
                    todayPagesRead = todayPagesRead,
                    diameter = 140.dp * scale,
                    isPrimary = false,
                    onClick = { onCircleClick(c1.id) },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = (-14).dp * scale, y = 10.dp * scale)
                )
                CircleOrbBubble(
                    circle = c2,
                    members = membersByCircle[c2.id].orEmpty(),
                    todayPagesRead = todayPagesRead,
                    diameter = 116.dp * scale,
                    isPrimary = false,
                    onClick = { onCircleClick(c2.id) },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = 46.dp * scale, y = (-12).dp * scale)
                )
            }
            else -> {
                val c0 = circles[0]
                val c1 = circles[1]
                val c2 = circles[2]
                val c3 = circles[3]
                CircleOrbBubble(
                    circle = c0,
                    members = membersByCircle[c0.id].orEmpty(),
                    todayPagesRead = todayPagesRead,
                    diameter = 188.dp * scale,
                    isPrimary = true,
                    onClick = { onCircleClick(c0.id) },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = 20.dp * scale, y = 10.dp * scale)
                )
                CircleOrbBubble(
                    circle = c1,
                    members = membersByCircle[c1.id].orEmpty(),
                    todayPagesRead = todayPagesRead,
                    diameter = 136.dp * scale,
                    isPrimary = false,
                    onClick = { onCircleClick(c1.id) },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .offset(x = (-16).dp * scale, y = 4.dp * scale)
                )
                CircleOrbBubble(
                    circle = c2,
                    members = membersByCircle[c2.id].orEmpty(),
                    todayPagesRead = todayPagesRead,
                    diameter = 108.dp * scale,
                    isPrimary = false,
                    onClick = { onCircleClick(c2.id) },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .offset(x = 46.dp * scale, y = (-26).dp * scale)
                )
                CircleOrbBubble(
                    circle = c3,
                    members = membersByCircle[c3.id].orEmpty(),
                    todayPagesRead = todayPagesRead,
                    diameter = 86.dp * scale,
                    isPrimary = false,
                    onClick = { onCircleClick(c3.id) },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 6.dp * scale, y = (-4).dp * scale)
                )
            }
        }
    }
}

@Composable
private fun CircleOrbBubble(
    circle: StreakCircleEntity,
    members: List<StreakCircleMemberEntity>,
    todayPagesRead: Int,
    diameter: Dp,
    isPrimary: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = QuranBaantoDesignSystem.colors
    val palette = resolveCirclePalette(circle.colorIndex, colors)
    val totalCount = members.size.coerceAtLeast(1)
    val doneCount = countDoneMembers(members, todayPagesRead)
    val statusText = if (isPrimary) "$doneCount of $totalCount read today" else "$doneCount of $totalCount"
    val ringMargin = 8.dp
    val totalSize = diameter + ringMargin * 2

    Box(
        modifier = modifier
            .size(totalSize)
            .clip(CircleShape)
            .clickable { onClick() }
            .testTag("circle_bubble_${circle.id}"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 2.dp.toPx()
            val arcInset = 4.dp.toPx()
            val arcSize = Size(size.width - arcInset * 2, size.height - arcInset * 2)
            val topLeft = Offset(arcInset, arcInset)

            // Outer orbital ring matching reference screenshots
            if (palette.useFullPineRing) {
                drawArc(
                    color = colors.pine,
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = 1.8.dp.toPx())
                )
            } else {
                drawArc(
                    color = colors.gold,
                    startAngle = -88f,
                    sweepAngle = 225f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
                drawArc(
                    color = colors.gold.copy(alpha = 0.72f),
                    startAngle = 152f,
                    sweepAngle = 32f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }

            // Inner filled orb
            drawCircle(
                color = palette.fill,
                radius = (diameter.toPx()) / 2f
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = circle.name,
                fontFamily = FrauncesFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = when {
                    diameter >= 175.dp -> 26.sp
                    diameter >= 125.dp -> 18.sp
                    else -> 15.sp
                },
                lineHeight = when {
                    diameter >= 175.dp -> 30.sp
                    diameter >= 125.dp -> 21.sp
                    else -> 17.sp
                },
                color = palette.textColor,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = statusText,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                fontSize = if (diameter >= 125.dp) 12.sp else 11.sp,
                color = palette.subTextColor,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Circle Detail Screen matching Circle_detail_3d77f380.png & Circle_detail___dark_b0288214.png.
 */
@Composable
fun CircleDetailScreen(
    circle: StreakCircleEntity,
    members: List<StreakCircleMemberEntity>,
    todayPagesRead: Int,
    onBack: () -> Unit,
    onReadNow: () -> Unit,
    onNudgeMember: (StreakCircleMemberEntity) -> Unit,
    onDeleteCircle: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val colors = QuranBaantoDesignSystem.colors
    val context = LocalContext.current
    var showInviteDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val totalCount = members.size.coerceAtLeast(1)
    val doneCount = countDoneMembers(members, todayPagesRead)
    val domeColor = if (colors.isDark) colors.pine else Color(0xFF1F3D36)
    val cream = Color(0xFFF1EBDD)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Top-Right Pine Quarter-Circle Dome with inner Gold concentric arc
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(width = 214.dp, height = 196.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val r = 192.dp.toPx()
                val topRightCenter = Offset(size.width + 6.dp.toPx(), -14.dp.toPx())
                drawCircle(
                    color = domeColor,
                    radius = r,
                    center = topRightCenter
                )
                drawCircle(
                    color = colors.gold.copy(alpha = 0.55f),
                    radius = r - 52.dp.toPx(),
                    center = topRightCenter,
                    style = Stroke(width = 1.dp.toPx())
                )
            }

            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 24.dp, end = 26.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$doneCount of $totalCount",
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 32.sp,
                    color = cream
                )
                Text(
                    text = "read today",
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    color = colors.gold
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp)
                    .padding(top = 12.dp, bottom = 16.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("circle_detail_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.text
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = "CIRCLE",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    color = colors.mutedText
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = circle.name,
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 42.sp,
                    lineHeight = 46.sp,
                    letterSpacing = (-0.6).sp,
                    color = colors.text
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${members.size} ${if (members.size == 1) "member" else "members"} · ${circle.roomPasscode}",
                        fontFamily = InterFontFamily,
                        fontSize = 14.sp,
                        color = colors.mutedText
                    )
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showDeleteConfirm = true }
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteOutline,
                            contentDescription = "Leave or delete circle",
                            tint = colors.mutedText,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Leave",
                            fontFamily = InterFontFamily,
                            fontSize = 12.sp,
                            color = colors.mutedText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(34.dp))

                Text(
                    text = "TODAY",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    color = colors.mutedText
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (members.isEmpty()) {
                    Text(
                        text = "No members yet. Share your invite code below.",
                        fontFamily = InterFontFamily,
                        fontSize = 14.sp,
                        color = colors.mutedText,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                } else {
                    members.forEach { member ->
                        val effectivePages = if (member.isCurrentUser) {
                            maxOf(member.pagesReadToday, todayPagesRead)
                        } else {
                            member.pagesReadToday
                        }
                        val hasRead = member.hasCompletedToday || (member.isCurrentUser && todayPagesRead > 0)
                        val displayName = if (member.isCurrentUser) {
                            if (member.memberName.equals("You", true)) "You" else "${member.memberName} (You)"
                        } else {
                            member.memberName
                        }
                        val initial = member.memberName.trim().firstOrNull()?.uppercase() ?: "Y"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(if (hasRead) colors.pine else Color.Transparent)
                                    .then(
                                        if (!hasRead) {
                                            Modifier.border(1.4.dp, colors.gold, CircleShape)
                                        } else {
                                            Modifier
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initial,
                                    fontFamily = FrauncesFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 17.sp,
                                    color = if (hasRead) cream else colors.text
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = displayName,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    color = colors.text
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = when {
                                        hasRead && effectivePages > 0 ->
                                            "$effectivePages ${if (effectivePages == 1) "page" else "pages"} today"
                                        hasRead -> "Read today"
                                        else -> "Not yet today"
                                    },
                                    fontFamily = InterFontFamily,
                                    fontSize = 13.sp,
                                    color = colors.mutedText
                                )
                            }

                            if (hasRead) {
                                Text(
                                    text = "Done",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = colors.pineText
                                )
                            } else if (member.isCurrentUser) {
                                Text(
                                    text = "Read →",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    textDecoration = TextDecoration.Underline,
                                    color = colors.pineText,
                                    modifier = Modifier
                                        .clickable { onReadNow() }
                                        .padding(vertical = 6.dp, horizontal = 4.dp)
                                )
                            } else {
                                Text(
                                    text = "Nudge →",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    textDecoration = TextDecoration.Underline,
                                    color = colors.wineText,
                                    modifier = Modifier
                                        .clickable {
                                            onNudgeMember(member)
                                            val nudgeText = "Assalamu alaikum ${member.memberName}, gentle reminder to read today's pages for our '${circle.name}' circle on Quran Baanto."
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, nudgeText)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Nudge ${member.memberName}"))
                                        }
                                        .padding(vertical = 6.dp, horizontal = 4.dp)
                                        .testTag("nudge_member_${member.id}")
                                )
                            }
                        }

                        HorizontalDivider(thickness = 0.8.dp, color = colors.rules)
                    }
                }
            }

            Box(modifier = Modifier.padding(bottom = 24.dp)) {
                RightExtendingPillButton(
                    text = "Invite a friend",
                    onClick = { showInviteDialog = true },
                    modifier = Modifier.testTag("invite_friend_button")
                )
            }
        }
    }

    // Invite Friend QR / Code Dialog
    if (showInviteDialog) {
        val qrBitmap = remember(circle.roomPasscode, circle.name) {
            RoomQrCodeHelper.generateQrCodeBitmap(circle.roomPasscode, circle.name, 520)
        }
        Dialog(onDismissRequest = { showInviteDialog = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = colors.background,
                border = BorderStroke(1.dp, colors.gold.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "INVITE TO CIRCLE",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            letterSpacing = 1.2.sp,
                            color = colors.mutedText
                        )
                        IconButton(
                            onClick = { showInviteDialog = false },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = colors.mutedText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = circle.name,
                        fontFamily = FrauncesFontFamily,
                        fontSize = 26.sp,
                        color = colors.text
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    if (qrBitmap != null) {
                        Box(
                            modifier = Modifier
                                .size(196.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, colors.gold.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                                .background(Color(0xFFFDFBF7))
                                .padding(10.dp)
                        ) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "Circle invite QR code",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    Text(
                        text = circle.roomPasscode,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        letterSpacing = 1.5.sp,
                        color = colors.pineText
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(22.dp))
                                .border(1.2.dp, colors.gold.copy(alpha = 0.7f), RoundedCornerShape(22.dp))
                                .clickable {
                                    copyToClipboard(context, "Circle Invite Code", circle.roomPasscode)
                                    Toast.makeText(context, "Code copied: ${circle.roomPasscode}", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Copy code",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = colors.text
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(22.dp))
                                .background(colors.pine)
                                .clickable {
                                    RoomQrCodeHelper.shareRoomQrCodeImage(
                                        context = context,
                                        roomName = circle.name,
                                        passcode = circle.roomPasscode,
                                        typeSubtitle = "Reading Circle Invite"
                                    )
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Share invite",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = cream
                            )
                        }
                    }
                }
            }
        }
    }

    // Leave/Delete Confirmation Dialog
    if (showDeleteConfirm) {
        Dialog(onDismissRequest = { showDeleteConfirm = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = colors.background,
                border = BorderStroke(1.dp, colors.wine.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Leave circle?",
                        fontFamily = FrauncesFontFamily,
                        fontSize = 24.sp,
                        color = colors.text
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Remove '${circle.name}' and its roster from this device?",
                        fontFamily = InterFontFamily,
                        fontSize = 14.sp,
                        color = colors.mutedText
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .border(1.dp, colors.rules, RoundedCornerShape(20.dp))
                                .clickable { showDeleteConfirm = false }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Cancel",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = colors.text
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(colors.wine)
                                .clickable {
                                    showDeleteConfirm = false
                                    onDeleteCircle()
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Leave",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = Color(0xFFF1EBDD)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Create a Circle Screen matching Create_a_circle_3f3dc715.png & Create_a_circle___dark_68921c5c.png.
 */
@Composable
fun CreateCircleScreen(
    generatePasscode: (String) -> String,
    onCreateCircle: (name: String, colorIndex: Int, passcode: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val colors = QuranBaantoDesignSystem.colors
    val context = LocalContext.current

    var circleName by remember { mutableStateOf("") }
    var selectedColorIndex by remember { mutableIntStateOf(0) }
    var inviteCode by remember { mutableStateOf(generatePasscode("Circle")) }
    var hasManuallyEditedCode by remember { mutableStateOf(false) }

    val swatchColors = listOf(
        colors.pine,
        if (colors.isDark) Color(0xFF424A53) else Color(0xFF1A1C1A),
        colors.gold,
        if (colors.isDark) Color(0xFF282D33) else colors.softNeutral
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        TopRightProgressBubble(color = colors.softNeutral)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 12.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.text
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "NEW CIRCLE",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    color = colors.mutedText
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Name your\ncircle",
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 38.sp,
                    lineHeight = 42.sp,
                    letterSpacing = (-0.5).sp,
                    color = colors.text
                )

                Spacer(modifier = Modifier.height(32.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    if (circleName.isEmpty()) {
                        Text(
                            text = "e.g. Family",
                            fontFamily = InterFontFamily,
                            fontSize = 20.sp,
                            color = colors.mutedText.copy(alpha = 0.7f)
                        )
                    }
                    BasicTextField(
                        value = circleName,
                        onValueChange = { updated ->
                            circleName = updated
                            if (!hasManuallyEditedCode && updated.isNotBlank()) {
                                val suffix = inviteCode.substringAfter("-", "")
                                val letters = updated.uppercase().filter { it in 'A'..'Z' }.take(3).padEnd(3, 'C')
                                inviteCode = if (suffix.isNotBlank()) "$letters-$suffix" else generatePasscode(updated)
                            }
                        },
                        textStyle = TextStyle(
                            fontFamily = InterFontFamily,
                            fontSize = 20.sp,
                            color = colors.text
                        ),
                        cursorBrush = SolidColor(colors.gold),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_circle_name_input")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                WaveDivider(color = colors.gold, strokeWidth = 1.6.dp)

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Friends see this name when they join.",
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    color = colors.mutedText
                )

                Spacer(modifier = Modifier.height(34.dp))

                Text(
                    text = "CIRCLE COLOUR",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    color = colors.mutedText
                )

                Spacer(modifier = Modifier.height(14.dp))

                // 4 Overlapping Circle Swatches with Gold Active Ring
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((-8).dp)
                ) {
                    swatchColors.forEachIndexed { idx, swatch ->
                        val isSelected = selectedColorIndex == idx
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .then(
                                    if (isSelected) {
                                        Modifier.border(1.8.dp, colors.gold, CircleShape)
                                    } else {
                                        Modifier
                                    }
                                )
                                .padding(4.dp)
                                .clip(CircleShape)
                                .background(swatch)
                                .clickable { selectedColorIndex = idx }
                                .testTag("circle_color_$idx")
                        )
                    }
                }

                Spacer(modifier = Modifier.height(34.dp))

                Text(
                    text = "INVITE CODE",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    color = colors.mutedText
                )

                Spacer(modifier = Modifier.height(10.dp))

                BasicTextField(
                    value = inviteCode,
                    onValueChange = {
                        hasManuallyEditedCode = true
                        inviteCode = it.uppercase().take(12)
                    },
                    textStyle = TextStyle(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 24.sp,
                        letterSpacing = 1.sp,
                        color = colors.text
                    ),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    cursorBrush = SolidColor(colors.gold),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                WaveDivider(color = colors.gold, strokeWidth = 1.6.dp)

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Share invite",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        textDecoration = TextDecoration.Underline,
                        color = colors.pineText,
                        modifier = Modifier.clickable {
                            val cleanName = circleName.trim().ifBlank { "Reading Circle" }
                            RoomQrCodeHelper.shareRoomQrCodeImage(
                                context = context,
                                roomName = cleanName,
                                passcode = inviteCode,
                                typeSubtitle = "Reading Circle Invite"
                            )
                        }
                    )

                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(colors.gold)
                    )

                    Text(
                        text = "Copy code",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        textDecoration = TextDecoration.Underline,
                        color = colors.pineText,
                        modifier = Modifier.clickable {
                            copyToClipboard(context, "Circle Invite Code", inviteCode)
                            Toast.makeText(context, "Copied $inviteCode", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            Box(modifier = Modifier.padding(bottom = 24.dp)) {
                RightExtendingPillButton(
                    text = "Create circle",
                    enabled = circleName.trim().isNotEmpty(),
                    onClick = {
                        onCreateCircle(
                            circleName.trim(),
                            selectedColorIndex,
                            inviteCode.trim().ifBlank { generatePasscode(circleName) }
                        )
                    },
                    modifier = Modifier.testTag("confirm_create_circle_button")
                )
            }
        }
    }
}

/**
 * Join a Circle Screen matching Join_a_circle_e6040b50.png & Join_a_circle___dark_016121fd.png.
 * Zero draft/placeholder matches: looks up real DB circles only.
 */
@Composable
fun JoinCircleScreen(
    existingCircles: List<StreakCircleEntity>,
    allMembers: List<StreakCircleMemberEntity>,
    todayPagesRead: Int,
    onJoinCircle: (passcode: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val colors = QuranBaantoDesignSystem.colors
    val context = LocalContext.current
    var codeInput by remember { mutableStateOf("") }

    val normalizedCode = codeInput.trim().uppercase()
    val matchedCircle = remember(normalizedCode, existingCircles) {
        if (normalizedCode.length >= 4) {
            existingCircles.firstOrNull { it.roomPasscode.equals(normalizedCode, ignoreCase = true) }
        } else {
            null
        }
    }

    // Zero-permission Photo Picker for scanning QR code images via ZXing
    val qrPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val decoded = decodeQrCodeFromUri(context, uri)
            if (decoded != null) {
                codeInput = extractInviteCode(decoded)
                Toast.makeText(context, "QR code detected: $codeInput", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "No readable QR code found in image.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        TopRightProgressBubble(color = colors.softNeutral)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp, vertical = 12.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.text
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "JOIN",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    color = colors.mutedText
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Enter the\ncircle code",
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 38.sp,
                    lineHeight = 42.sp,
                    letterSpacing = (-0.5).sp,
                    color = colors.text
                )

                Spacer(modifier = Modifier.height(30.dp))

                Box(modifier = Modifier.fillMaxWidth()) {
                    if (codeInput.isEmpty()) {
                        Text(
                            text = "e.g. FAM-7Q2K",
                            fontFamily = InterFontFamily,
                            fontSize = 24.sp,
                            color = colors.mutedText.copy(alpha = 0.65f)
                        )
                    }
                    BasicTextField(
                        value = codeInput,
                        onValueChange = { codeInput = it.uppercase().take(16) },
                        textStyle = TextStyle(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 24.sp,
                            letterSpacing = 1.sp,
                            color = colors.text
                        ),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        cursorBrush = SolidColor(colors.gold),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("join_circle_code_input")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                WaveDivider(color = colors.gold, strokeWidth = 1.6.dp)

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Ask the circle's admin for the code.",
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    color = colors.mutedText
                )

                Spacer(modifier = Modifier.height(26.dp))

                // Scan QR code instead row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier
                        .clickable {
                            qrPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .padding(vertical = 4.dp)
                        .testTag("scan_qr_code_row")
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .border(1.2.dp, colors.gold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.QrCodeScanner,
                            contentDescription = "Select QR code image",
                            tint = colors.text,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = "Scan a QR code instead",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        textDecoration = TextDecoration.Underline,
                        color = colors.text
                    )
                }

                Spacer(modifier = Modifier.height(34.dp))

                if (matchedCircle != null) {
                    val matchedMembers = remember(matchedCircle.id, allMembers) {
                        allMembers.filter { it.circleId == matchedCircle.id }
                    }
                    val doneCount = countDoneMembers(matchedMembers, todayPagesRead)
                    val palette = resolveCirclePalette(matchedCircle.colorIndex, colors)

                    Text(
                        text = "CIRCLE FOUND",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        letterSpacing = 1.2.sp,
                        color = colors.mutedText
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Two overlapping decorative orbs
                        Box(modifier = Modifier.size(width = 72.dp, height = 56.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(palette.fill)
                            )
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(colors.gold)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = matchedCircle.name,
                                fontFamily = FrauncesFontFamily,
                                fontSize = 22.sp,
                                color = colors.text
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val hostLabel = matchedCircle.hostName.ifBlank { "member" }
                            Text(
                                text = "${matchedMembers.size} ${if (matchedMembers.size == 1) "member" else "members"} · created by $hostLabel",
                                fontFamily = InterFontFamily,
                                fontSize = 13.sp,
                                color = colors.mutedText
                            )
                            Text(
                                text = "$doneCount of ${matchedMembers.size.coerceAtLeast(1)} read today",
                                fontFamily = InterFontFamily,
                                fontSize = 13.sp,
                                color = colors.mutedText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    WaveDivider(color = colors.softNeutral, strokeWidth = 1.2.dp)
                } else if (normalizedCode.length >= 4) {
                    Text(
                        text = "READY TO JOIN",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        letterSpacing = 1.2.sp,
                        color = colors.mutedText
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Code $normalizedCode · tap Join circle below to add this circle.",
                        fontFamily = InterFontFamily,
                        fontSize = 14.sp,
                        color = colors.mutedText
                    )
                }
            }

            Box(modifier = Modifier.padding(bottom = 24.dp)) {
                RightExtendingPillButton(
                    text = "Join circle",
                    enabled = normalizedCode.length >= 4,
                    onClick = { onJoinCircle(normalizedCode) },
                    modifier = Modifier.testTag("confirm_join_circle_button")
                )
            }
        }
    }
}

private data class CircleBubblePalette(
    val fill: Color,
    val textColor: Color,
    val subTextColor: Color,
    val useFullPineRing: Boolean
)

private fun resolveCirclePalette(
    colorIndex: Int,
    colors: com.example.ui.theme.QuranBaantoColors
): CircleBubblePalette {
    val cream = Color(0xFFF1EBDD)
    return when (colorIndex.coerceIn(0, 3)) {
        0 -> CircleBubblePalette(
            fill = colors.pine,
            textColor = cream,
            subTextColor = cream.copy(alpha = 0.85f),
            useFullPineRing = false
        )
        1 -> CircleBubblePalette(
            fill = if (colors.isDark) Color(0xFF424A53) else Color(0xFF1A1C1A),
            textColor = cream,
            subTextColor = cream.copy(alpha = 0.8f),
            useFullPineRing = false
        )
        2 -> CircleBubblePalette(
            fill = colors.gold,
            textColor = Color(0xFF1A1A18),
            subTextColor = Color(0xFF1A1A18).copy(alpha = 0.78f),
            useFullPineRing = true
        )
        else -> CircleBubblePalette(
            fill = if (colors.isDark) Color(0xFF282D33) else colors.softNeutral,
            textColor = colors.text,
            subTextColor = colors.mutedText,
            useFullPineRing = false
        )
    }
}

private fun countDoneMembers(
    members: List<StreakCircleMemberEntity>,
    todayPagesRead: Int
): Int {
    if (members.isEmpty()) return if (todayPagesRead > 0) 1 else 0
    return members.count { member ->
        member.hasCompletedToday || (member.isCurrentUser && todayPagesRead > 0)
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
}

private fun decodeQrCodeFromUri(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val bitmap = BitmapFactory.decodeStream(stream) ?: return null
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            MultiFormatReader().decode(binaryBitmap)?.text
        }
    } catch (_: Exception) {
        null
    }
}

private fun extractInviteCode(rawPayload: String): String {
    val upper = rawPayload.trim().uppercase()
    val codeRegex = Regex("[A-Z0-9]{2,5}-[A-Z0-9]{3,8}")
    val match = codeRegex.find(upper)
    if (match != null) return match.value
    val afterColon = upper.substringAfterLast("PASSCODE:", "").trim()
    if (afterColon.isNotBlank()) return afterColon.lines().first().trim()
    return upper.lines().lastOrNull { it.isNotBlank() }?.take(16) ?: upper.take(16)
}
