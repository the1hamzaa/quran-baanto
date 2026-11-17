package com.example.ui.distribution

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.DistributionEntity
import com.example.data.db.DistributionType
import com.example.data.db.ParticipantEntity
import com.example.data.db.RoomMode
import com.example.ui.components.WaveDivider
import com.example.ui.theme.FrauncesFontFamily
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.QuranBaantoDesignSystem
import com.example.ui.viewmodel.DistributionViewModel

/**
 * DISTRIBUTIONS HOME SCREEN
 *
 * Rules:
 * - NO dummy/placeholder data added; uses real user database records or clean empty state.
 * - Admin powers button ONLY appears on Online rooms created by the user (Host) or where user was granted Admin.
 * - Semicircular Join and Create buttons rising from the bottom with centered labels.
 */
@Composable
fun DistributionListScreen(
    distributionViewModel: DistributionViewModel,
    onCreateNewDistribution: (DistributionType, RoomMode) -> Unit,
    onOpenDistributionDetail: (Long) -> Unit,
    onFullscreenOverlayChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colors = QuranBaantoDesignSystem.colors

    val distributions by distributionViewModel.distributions.collectAsState()
    val participantsByDistribution by distributionViewModel.participantsByDistribution.collectAsState()
    val userProfileName by distributionViewModel.userProfileName.collectAsState()

    // 0 = All, 1 = Local, 2 = Online
    var selectedFilterTab by remember { mutableIntStateOf(0) }
    var showJoinScreen by remember { mutableStateOf(false) }
    var adminDistributionId by remember { mutableStateOf<Long?>(null) }

    val isOverlayOpen = showJoinScreen || adminDistributionId != null
    LaunchedEffect(isOverlayOpen) {
        onFullscreenOverlayChanged(isOverlayOpen)
    }

    BackHandler(enabled = isOverlayOpen) {
        if (showJoinScreen) {
            showJoinScreen = false
        } else if (adminDistributionId != null) {
            adminDistributionId = null
        }
    }

    val activeViewKey = when {
        showJoinScreen -> "join"
        adminDistributionId != null -> "admin_${adminDistributionId}"
        else -> "list"
    }

    AnimatedContent(
        targetState = activeViewKey,
        transitionSpec = {
            fadeIn(tween(180, easing = FastOutSlowInEasing)) togetherWith
                fadeOut(tween(140, easing = FastOutSlowInEasing))
        },
        label = "distributions_root_transition",
        modifier = modifier
    ) { key ->
        when {
            key == "join" -> {
                JoinDistributionScreen(
                    distributionViewModel = distributionViewModel,
                    onJoined = { joinedId ->
                        showJoinScreen = false
                        onOpenDistributionDetail(joinedId)
                    },
                    onBack = { showJoinScreen = false }
                )
            }

            key.startsWith("admin_") && adminDistributionId != null -> {
                DistributionAdminScreen(
                    distributionId = adminDistributionId!!,
                    distributionViewModel = distributionViewModel,
                    onBack = { adminDistributionId = null }
                )
            }

            else -> {
                val filteredDistributions = remember(distributions, selectedFilterTab) {
                    when (selectedFilterTab) {
                        1 -> distributions.filter { it.roomMode == RoomMode.LOCAL }
                        2 -> distributions.filter { it.roomMode == RoomMode.ONLINE }
                        else -> distributions
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.background)
                ) {
                    // Soft neutral circle cropped at top right
                    TopRightDistributionsBubble(softNeutral = colors.softNeutral)

                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Upper Header Area
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .windowInsetsPadding(WindowInsets.statusBars)
                                .padding(horizontal = 28.dp, vertical = 16.dp)
                        ) {
                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Distributions",
                                fontFamily = FrauncesFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 44.sp,
                                letterSpacing = (-0.5).sp,
                                color = colors.text
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            // Sub-tabs: All, Local, Online
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(22.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                listOf("All", "Local", "Online").forEachIndexed { index, tabName ->
                                    val isSelected = selectedFilterTab == index
                                    Column(
                                        modifier = Modifier.clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { selectedFilterTab = index }
                                    ) {
                                        Text(
                                            text = tabName,
                                            fontFamily = InterFontFamily,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 16.sp,
                                            color = if (isSelected) colors.text else colors.mutedText
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .width(if (index == 0) 24.dp else if (index == 1) 36.dp else 42.dp)
                                                    .height(2.5.dp)
                                                    .clip(CircleShape)
                                                    .background(colors.pineText)
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.height(2.5.dp))
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "YOUR DISTRIBUTIONS",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                letterSpacing = 1.1.sp,
                                color = colors.mutedText
                            )
                        }

                        // Middle: List or Clean Empty State
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        ) {
                            if (filteredDistributions.isEmpty()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 28.dp, vertical = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    ArchWindowVector(
                                        modifier = Modifier.size(width = 54.dp, height = 66.dp),
                                        outlineColor = colors.gold,
                                        strokeWidth = 1.6.dp
                                    )

                                    Spacer(modifier = Modifier.height(18.dp))

                                    Text(
                                        text = "No distributions yet",
                                        fontFamily = FrauncesFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 22.sp,
                                        color = colors.text
                                    )

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "Create or join a room below to share recitations.",
                                        fontFamily = InterFontFamily,
                                        fontSize = 13.sp,
                                        color = colors.mutedText
                                    )

                                    Spacer(modifier = Modifier.height(80.dp))
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 4.dp)
                                ) {
                                    items(filteredDistributions, key = { it.id }) { dist ->
                                        val participants = participantsByDistribution[dist.id].orEmpty()
                                        DistributionRowItem(
                                            distribution = dist,
                                            participants = participants,
                                            userProfileName = userProfileName,
                                            onClick = { onOpenDistributionDetail(dist.id) },
                                            onAdminClick = { adminDistributionId = dist.id }
                                        )

                                        WaveDivider(
                                            color = colors.rules,
                                            strokeWidth = 1.2.dp,
                                            amplitude = 2.dp,
                                            wavelength = 44.dp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp)
                                        )
                                    }

                                    item {
                                        Spacer(modifier = Modifier.height(110.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Bottom Decorative Rising Semicircle Buttons (Join on Left, Create on Right)
                    BottomRisingActions(
                        onJoinClick = { showJoinScreen = true },
                        onCreateClick = { onCreateNewDistribution(DistributionType.KHATM, RoomMode.ONLINE) },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }
            }
        }
    }
}

@Composable
private fun DistributionRowItem(
    distribution: DistributionEntity,
    participants: List<ParticipantEntity>,
    userProfileName: String,
    onClick: () -> Unit,
    onAdminClick: () -> Unit
) {
    val colors = QuranBaantoDesignSystem.colors

    val modeStr = if (distribution.isOnline) "Online" else "Local"
    val typeStr = when (distribution.type) {
        DistributionType.ONE_TIME_KHATM -> "One-time khatm"
        DistributionType.KHATM -> "Monthly khatm"
        DistributionType.AMAL -> "Amal recitation"
    }

    val claimedCount = when (distribution.type) {
        DistributionType.ONE_TIME_KHATM -> participants.size
        DistributionType.KHATM -> participants.sumOf { it.allocatedCount }.coerceAtMost(30)
        DistributionType.AMAL -> participants.sumOf { it.allocatedCount }
    }

    val cleanProfile = userProfileName.trim()
    val myParticipant = if (cleanProfile.isNotBlank()) {
        participants.firstOrNull { it.name.trim().equals(cleanProfile, ignoreCase = true) }
    } else null

    val statusText = when (distribution.type) {
        DistributionType.ONE_TIME_KHATM -> "$claimedCount of 30 claimed"
        DistributionType.KHATM -> {
            if (myParticipant != null && myParticipant.specificParaNumber != null) {
                if (myParticipant.allocatedCount > 1) {
                    val end = ((myParticipant.specificParaNumber - 1 + myParticipant.allocatedCount - 1) % 30) + 1
                    "You have Paras ${myParticipant.specificParaNumber}–$end · $claimedCount of 30 claimed"
                } else {
                    "You have Para ${myParticipant.specificParaNumber} · $claimedCount of 30 claimed"
                }
            } else {
                "$claimedCount of 30 claimed"
            }
        }
        DistributionType.AMAL -> {
            val totalRecited = participants.sumOf { if (it.isCompleted) it.allocatedCount else it.completedCount }
            "$totalRecited of ${distribution.totalTarget} recited"
        }
    }

    // Admin powers ONLY show in an online room created by user OR where user was assigned Co-Admin
    val isAssignedCoAdmin = cleanProfile.isNotBlank() && participants.any {
        it.isAdmin && it.name.trim().equals(cleanProfile, ignoreCase = true) && !it.isGuestEntry
    }
    val canShowAdminPowers = distribution.isOnline && (distribution.isHost || isAssignedCoAdmin)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier.size(width = 54.dp, height = 66.dp),
            contentAlignment = Alignment.Center
        ) {
            when (distribution.type) {
                DistributionType.ONE_TIME_KHATM -> {
                    ArchWindowVector(
                        modifier = Modifier.size(width = 46.dp, height = 58.dp),
                        outlineColor = colors.gold,
                        strokeWidth = 1.6.dp
                    )
                }
                DistributionType.KHATM -> {
                    BeadedCircleVector(
                        modifier = Modifier.size(54.dp),
                        totalBeads = 30,
                        completedBeads = participants.count { it.isCompleted },
                        goldColor = colors.gold,
                        pineColor = colors.pine
                    )
                }
                DistributionType.AMAL -> {
                    ConcentricCirclesVector(
                        modifier = Modifier.size(54.dp),
                        pineColor = colors.pine,
                        strokeColor = colors.gold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(18.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = distribution.name,
                fontFamily = FrauncesFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 22.sp,
                letterSpacing = (-0.2).sp,
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = "$modeStr · $typeStr",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                color = colors.mutedText
            )

            Spacer(modifier = Modifier.height(3.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = statusText,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = colors.pineText
                )

                if (distribution.type == DistributionType.ONE_TIME_KHATM && claimedCount < 30) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "· Claim a para →",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = colors.wineText,
                        modifier = Modifier.clickable { onClick() }
                    )
                }
            }

            if (canShowAdminPowers) {
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (colors.isDark) Color(0xFF262B31) else Color(0xFF1B1B19))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onAdminClick() }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                        .testTag("admin_powers_button_${distribution.id}")
                ) {
                    Text(
                        text = "Admin powers",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = Color(0xFFF1EBDD)
                    )
                }
            }
        }
    }
}

/**
 * Bottom Rising Semicircle Action Area with true geometric semicircles (arcTo)
 * and optically dead-centered Join / Create labels inside each semicircle.
 */
@Composable
private fun BottomRisingActions(
    onJoinClick: () -> Unit,
    onCreateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = QuranBaantoDesignSystem.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(92.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Left: "Join" true semicircle rising from bottom-left
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .size(width = 114.dp, height = 68.dp)
                .drawWithCache {
                    val semicirclePath = Path().apply {
                        moveTo(0f, size.height)
                        arcTo(
                            rect = Rect(0f, 0f, size.width, size.height * 2f),
                            startAngleDegrees = 180f,
                            sweepAngleDegrees = 180f,
                            forceMoveTo = false
                        )
                        close()
                    }
                    onDrawBehind {
                        drawPath(path = semicirclePath, color = colors.gold)
                    }
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onJoinClick() }
                .testTag("rising_join_button"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Join",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF1B1B19),
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        // Center Hint Text
        Text(
            text = "Have a code? Join with it.",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            color = colors.mutedText,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 8.dp)
        )

        // Right: "Create" true semicircle rising from bottom-right
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(width = 126.dp, height = 78.dp)
                .drawWithCache {
                    val semicirclePath = Path().apply {
                        moveTo(0f, size.height)
                        arcTo(
                            rect = Rect(0f, 0f, size.width, size.height * 2f),
                            startAngleDegrees = 180f,
                            sweepAngleDegrees = 180f,
                            forceMoveTo = false
                        )
                        close()
                    }
                    onDrawBehind {
                        drawPath(path = semicirclePath, color = colors.pine)
                    }
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onCreateClick() }
                .testTag("rising_create_button"),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Create",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFFF1EBDD),
                modifier = Modifier.padding(top = 14.dp)
            )
        }
    }
}

@Composable
private fun TopRightDistributionsBubble(softNeutral: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        val w = size.width
        val circleCenter = Offset(w - 10.dp.toPx(), 20.dp.toPx())
        val circleRadius = 75.dp.toPx()

        drawCircle(
            color = softNeutral,
            radius = circleRadius,
            center = circleCenter
        )
    }
}
