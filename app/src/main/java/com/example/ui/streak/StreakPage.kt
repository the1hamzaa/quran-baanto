package com.example.ui.streak

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.DailyLogEntity
import com.example.data.db.StreakEntity
import com.example.ui.distribution.WaveTrack
import com.example.ui.theme.FrauncesFontFamily
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.QuranBaantoDesignSystem

private val STREAK_MILESTONES = listOf(3, 7, 14, 21, 30, 60, 100, 180, 365)

/**
 * Exact Start a Streak onboarding screen matching Start_a_streak_9d704002.png.
 */
@Composable
fun StartStreakScreen(
    onStartToday: () -> Unit,
    onMaybeLater: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = QuranBaantoDesignSystem.colors
    val pineBg = if (colors.isDark) Color(0xFF1A342D) else Color(0xFF1F3D36)
    val cream = Color(0xFFF1EBDD)
    val gold = colors.gold

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(pineBg)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Upper Concentric Rings with "0" center circle
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height * 0.54f)
                val baseR = 54.dp.toPx()
                val ringStep = 44.dp.toPx()
                for (i in 1..4) {
                    val alpha = (0.38f - (i - 1) * 0.08f).coerceAtLeast(0.08f)
                    drawCircle(
                        color = gold.copy(alpha = alpha),
                        radius = baseR + ringStep * i,
                        center = center,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }

            Box(
                modifier = Modifier
                    .padding(top = 24.dp)
                    .size(104.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF171817)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "0",
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 46.sp,
                    color = cream
                )
            }
        }

        // Bottom Prompt & CTA
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 28.dp)) {
                Text(
                    text = "DAILY READING",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.3.sp,
                    color = gold
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Begin a streak.",
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 40.sp,
                    letterSpacing = (-0.5).sp,
                    color = cream
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Read a little every day.\nWe'll keep count.",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 16.sp,
                    lineHeight = 23.sp,
                    color = cream.copy(alpha = 0.86f)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            RightExtendingPillButton(
                text = "Start today",
                onClick = onStartToday,
                containerColor = gold,
                contentColor = Color(0xFF1B1B19),
                modifier = Modifier.testTag("start_today_button")
            )

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .defaultMinSize(minWidth = 120.dp, minHeight = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onMaybeLater() }
                    .padding(horizontal = 8.dp, vertical = 10.dp)
                    .testTag("maybe_later_button"),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "Maybe later",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    textDecoration = TextDecoration.Underline,
                    color = cream.copy(alpha = 0.82f)
                )
            }
        }
    }
}

/**
 * Page 0 of the Progress Pager: Daily Streak Screen
 * Matches Streak_48475705.png & Streak___dark_c9f5029c.png precisely.
 * Uses real streak and todayLog.pagesReadCount data with zero draft data.
 */
@Composable
fun StreakPage(
    streak: StreakEntity?,
    todayLog: DailyLogEntity?,
    onReadNow: () -> Unit,
    onStartStreak: () -> Unit = onReadNow,
    onGoToPlans: () -> Unit,
    onSelectPage: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = QuranBaantoDesignSystem.colors
    val bgColor = if (colors.isDark) colors.background else Color(0xFF181611)
    val cream = Color(0xFFF1EBDD)
    val mutedCream = Color(0xFFA29F94)
    val gold = colors.gold
    val pineCircleColor = if (colors.isDark) colors.pine else Color(0xFF1F3D36)
    val isOptedIn = streak?.isOptedIn == true

    val currentStreak = streak?.currentStreakDays ?: 0
    val pagesReadToday = todayLog?.pagesReadCount ?: 0
    val hasReadToday = pagesReadToday > 0 || (todayLog?.hasMetGoal == true)

    val nextMilestone = STREAK_MILESTONES.firstOrNull { it > currentStreak } ?: (currentStreak + 30)
    val daysToGo = (nextMilestone - currentStreak).coerceAtLeast(0)
    val milestoneProgress = (currentStreak.toFloat() / nextMilestone.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
            .windowInsetsPadding(WindowInsets.statusBars),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Centerpiece: Concentric Rings + Massive Streak Count
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val c = Offset(size.width / 2f, size.height * 0.49f)
                val r1 = 108.dp.toPx()
                val r2 = 148.dp.toPx()
                val r3 = 192.dp.toPx()

                // Outer faint gold ring
                drawCircle(
                    color = gold.copy(alpha = 0.24f),
                    radius = r3,
                    center = c,
                    style = Stroke(width = 1.dp.toPx())
                )
                // Middle subtle pine ring
                drawCircle(
                    color = pineCircleColor.copy(alpha = 0.55f),
                    radius = r2,
                    center = c,
                    style = Stroke(width = 1.2.dp.toPx())
                )
                // Inner gold frame ring
                drawCircle(
                    color = gold.copy(alpha = 0.55f),
                    radius = r1,
                    center = c,
                    style = Stroke(width = 1.1.dp.toPx())
                )
            }

            // Inner pine circle with streak number (or unstarted indicator if user chose Maybe later)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Text(
                    text = if (isOptedIn) "CURRENT STREAK" else "STREAK PAUSED",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.4.sp,
                    color = gold
                )

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .size(132.dp)
                        .clip(CircleShape)
                        .background(if (isOptedIn) pineCircleColor else Color(0xFF23221D)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isOptedIn) currentStreak.toString() else "—",
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = if (currentStreak >= 100) 64.sp else 86.sp,
                        lineHeight = 88.sp,
                        color = if (isOptedIn) cream else mutedCream
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = when {
                        !isOptedIn -> "not started"
                        currentStreak == 1 -> "day"
                        else -> "days"
                    },
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 15.sp,
                    color = mutedCream
                )
            }
        }

        // Bottom Status, Milestone Wave & Pager Indicator
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 28.dp)) {
                if (!isOptedIn) {
                    Text(
                        text = "No streak active",
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 26.sp,
                        color = cream
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (pagesReadToday > 0) {
                            "$pagesReadToday ${if (pagesReadToday == 1) "page" else "pages"} read today are still counted toward your plans and circles."
                        } else {
                            "You chose not to track a daily streak yet. Start whenever you're ready."
                        },
                        fontFamily = InterFontFamily,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = mutedCream
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(gold)
                            .clickable { onStartStreak() }
                            .padding(horizontal = 22.dp, vertical = 12.dp)
                            .testTag("streak_page_begin_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Start a reading streak →",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color(0xFF1B1B19)
                        )
                    }
                } else {
                    // Status row with rising half-dome icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!hasReadToday) onReadNow()
                        }
                    ) {
                        RisingHalfDomeIcon(
                            fillColor = if (hasReadToday) gold else gold.copy(alpha = 0.35f),
                            cutoutColor = bgColor
                        )

                        if (hasReadToday) {
                            Column {
                                Text(
                                    text = "Completed today",
                                    fontFamily = FrauncesFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 23.sp,
                                    color = cream
                                )
                                Text(
                                    text = "$pagesReadToday ${if (pagesReadToday == 1) "page" else "pages"} read today",
                                    fontFamily = InterFontFamily,
                                    fontSize = 12.sp,
                                    color = mutedCream
                                )
                            }
                        } else {
                            Column {
                                Text(
                                    text = "Read 1 page today",
                                    fontFamily = FrauncesFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 23.sp,
                                    color = cream
                                )
                                Text(
                                    text = "Start reading →",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    textDecoration = TextDecoration.Underline,
                                    color = gold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(34.dp))

                    // Next Milestone section
                    Text(
                        text = "NEXT MILESTONE · $nextMilestone DAYS",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        letterSpacing = 1.3.sp,
                        color = gold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    WaveTrack(
                        progress = milestoneProgress,
                        pineColor = gold,
                        trackColor = Color(0xFF393731),
                        strokeWidth = 2.8.dp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "$daysToGo ${if (daysToGo == 1) "day" else "days"} to go",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 14.sp,
                        color = mutedCream
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            PagerFooterIndicator(
                currentPage = 0,
                onSelectPage = onSelectPage,
                onCharcoal = true,
                rightLabel = "Plans →",
                onRightClick = onGoToPlans
            )
        }
    }
}
