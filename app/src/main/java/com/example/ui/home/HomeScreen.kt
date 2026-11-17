package com.example.ui.home

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.DistributionType
import com.example.ui.components.ArchProgress
import com.example.ui.components.BubbleChain
import com.example.ui.components.DayBubbleItem
import com.example.ui.components.DayBubbleStatus
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.FrauncesFontFamily
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.QuranBaantoDesignSystem
import com.example.ui.viewmodel.DistributionViewModel
import com.example.ui.viewmodel.QuranViewModel
import com.example.ui.viewmodel.StreakViewModel
import java.util.Calendar
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * EXACT HOME SCREEN IMPLEMENTATION
 * Matches the target design screenshots precisely with zero text wrapping and silky smooth performance.
 *
 * - Hero: Displays exact "Page 38" (or "Page 1" for new users), "Al-Baqarah · Juz 2",
 *   Arabic "البَقَرَة" in pine, and "Resume reading →" in wine. No text wrapping.
 * - Today's Progress: Real pages read today (defaults to 0, 0/10 with empty arch fill).
 * - This Week: Dynamic 7-day streak bubbles (clean empty state for new users).
 * - Your Circles: Real distribution circle or clean empty state without awkward wrapping.
 * - Bottom Navigation: Bold active tab with 44x22dp rising gold semicircle.
 */
@Composable
fun HomeScreen(
    distributionViewModel: DistributionViewModel,
    quranViewModel: QuranViewModel? = null,
    streakViewModel: StreakViewModel? = null,
    onNavigateToStreaks: () -> Unit,
    onCreateNewDistribution: (DistributionType) -> Unit = {},
    onOpenDistributionDetail: (Long) -> Unit = {},
    onOpenParaReader: (Int, Int?, Int?) -> Unit = { _, _, _ -> },
    onOpenSurahReader: (Int, Int?) -> Unit = { _, _ -> },
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = QuranBaantoDesignSystem.colors

    // Live state
    val lastBookmark by quranViewModel?.lastReadBookmark?.collectAsState()
        ?: remember { androidx.compose.runtime.mutableStateOf(null) }
    val todayLog by streakViewModel?.todayLog?.collectAsState()
        ?: remember { androidx.compose.runtime.mutableStateOf(null) }
    val streak by streakViewModel?.streakState?.collectAsState()
        ?: remember { androidx.compose.runtime.mutableStateOf(null) }
    val recentLogs by streakViewModel?.recentLogsState?.collectAsState()
        ?: remember { androidx.compose.runtime.mutableStateOf(emptyList()) }
    val distributions by distributionViewModel.distributions.collectAsState()
    val participantsByDistribution by distributionViewModel.participantsByDistribution.collectAsState()

    val activePlans by streakViewModel?.activePlansState?.collectAsState()
        ?: remember { androidx.compose.runtime.mutableStateOf(emptyList()) }
    val activeFinishPlan = activePlans.firstOrNull {
        it.planType == com.example.data.db.PlanType.FINISH_QURAN ||
            it.planType == com.example.data.db.PlanType.PARA_RANGE_TARGET
    }

    // 1. Pages read today (exact distinct Madinah Mushaf pages read today, synced with Reader, Plans & Circles)
    val pagesReadToday = todayLog?.pagesReadCount ?: 0
    val pagesGoal = (activeFinishPlan?.dailyTargetPages ?: todayLog?.targetPagesCount ?: 10).coerceAtLeast(1)
    val progress = (pagesReadToday.toFloat() / pagesGoal.toFloat()).coerceIn(0f, 1f)

    // 2. Hero Section: Exactly matching screenshot format (Page X, Surah · Juz Y, Arabic)
    val hasBookmark = lastBookmark != null
    val headerKicker = "CONTINUE"
    val bookmarkPage = if (hasBookmark) {
        com.example.data.quran.QuranPageMapper.getPageForAyah(
            lastBookmark!!.surahNumber,
            lastBookmark!!.ayahNumber,
            lastBookmark!!.id
        )
    } else 1
    val heroTitle = "Page $bookmarkPage"
    val heroSubtitle = if (hasBookmark) {
        val juz = com.example.data.quran.QuranPageMapper.getJuzForPage(bookmarkPage)
        "${lastBookmark!!.title} · Juz $juz"
    } else {
        "Al-Fatihah · Juz 1"
    }
    val heroArabic = if (hasBookmark && lastBookmark!!.arabicTitle.isNotBlank()) {
        lastBookmark!!.arabicTitle
    } else {
        "الفَاتِحَة"
    }
    val resumeActionText = if (hasBookmark) "Resume reading →" else "Start reading →"

    // 3. This Week: 7 Overlapping Bubbles with day-of-week letters
    // - Days prior to today: Wine if user read, Grey if missed
    // - Today: Gold outline (pine if read today)
    // - Future days: Faint soft neutral
    val weekBubbleStates = remember(streak, recentLogs, todayLog) {
        val dayLetters = listOf("M", "T", "W", "T", "F", "S", "S")
        val cal = Calendar.getInstance()
        val dayOfWeekIndex = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 // Monday = 0 .. Sunday = 6

        val todayCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val mondayCal = Calendar.getInstance().apply {
            timeInMillis = todayCalendar.timeInMillis
            add(Calendar.DAY_OF_YEAR, -dayOfWeekIndex)
        }

        val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        val currentStreak = streak?.currentStreakDays ?: 0
        val hasReadToday = (todayLog?.pagesReadCount ?: 0) > 0 || (todayLog?.hasMetGoal == true)

        dayLetters.mapIndexed { index, letter ->
            val bubbleCal = (mondayCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, index)
            }
            val dateKey = dateFormat.format(bubbleCal.time)

            val status = when {
                index < dayOfWeekIndex -> {
                    val loggedOnDay = recentLogs.any { it.dateKey == dateKey && (it.pagesReadCount > 0 || it.hasMetGoal) }
                    val daysAgo = dayOfWeekIndex - index
                    val withinActiveStreak = currentStreak >= daysAgo
                    if (loggedOnDay || withinActiveStreak) {
                        DayBubbleStatus.PAST_READ
                    } else {
                        DayBubbleStatus.PAST_MISSED
                    }
                }
                index == dayOfWeekIndex -> {
                    if (hasReadToday) {
                        DayBubbleStatus.TODAY_READ
                    } else {
                        DayBubbleStatus.TODAY
                    }
                }
                else -> {
                    DayBubbleStatus.FUTURE
                }
            }

            DayBubbleItem(
                dayLetter = letter,
                status = status
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Decorative background cropped bubbles at top right
        TopRightBackgroundBubbles(isDark = colors.isDark)

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Upper Content Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Top Header Row: "CONTINUE" and 44dp Settings Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(top = 16.dp, bottom = 8.dp)
                        .padding(horizontal = 28.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = headerKicker,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        letterSpacing = 1.2.sp,
                        color = colors.mutedText,
                        maxLines = 1,
                        softWrap = false
                    )

                    RoundSettingsButton(
                        onClick = onOpenSettings,
                        isDark = colors.isDark
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Hero & Today's Progress Arch Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Left Hero Column
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = heroTitle,
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 56.sp,
                            lineHeight = 60.sp,
                            letterSpacing = (-0.5).sp,
                            color = colors.text,
                            maxLines = 1,
                            softWrap = false
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = heroSubtitle,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 15.sp,
                            color = colors.text,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = heroArabic,
                            fontFamily = AmiriFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 42.sp,
                            color = colors.pineText,
                            maxLines = 1,
                            softWrap = false
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = resumeActionText,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 15.sp,
                            textDecoration = TextDecoration.Underline,
                            color = colors.wineText,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (hasBookmark) {
                                        onOpenSurahReader(lastBookmark!!.surahNumber, lastBookmark!!.ayahNumber)
                                    } else {
                                        onOpenSurahReader(1, 1)
                                    }
                                }
                                .testTag("resume_reading_link")
                        )
                    }

                    // Right: Arch window progress (dynamic: 0/10 when fresh)
                    ArchProgress(
                        pagesRead = pagesReadToday,
                        pagesGoal = pagesGoal,
                        progress = progress,
                        width = 120.dp,
                        height = 176.dp,
                        modifier = Modifier.padding(start = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // "THIS WEEK" Section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp)
                ) {
                    Text(
                        text = "THIS WEEK",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        letterSpacing = 1.1.sp,
                        color = colors.mutedText,
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    BubbleChain(
                        circleDiameter = 32.dp,
                        overlap = 10.dp,
                        days = weekBubbleStates,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onNavigateToStreaks()
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Below the bubble chain: exact days if streak exists, or CTA if no streak
                    val currentStreakCount = streak?.currentStreakDays ?: 0
                    if (currentStreakCount > 0) {
                        val streakText = if (currentStreakCount == 1) "1 day reading streak" else "$currentStreakCount days reading streak"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onNavigateToStreaks() }
                        ) {
                            Text(
                                text = streakText,
                                fontFamily = FrauncesFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 15.sp,
                                color = colors.text,
                                maxLines = 1
                            )
                            val statusNote = if ((todayLog?.pagesReadCount ?: 0) > 0 || (todayLog?.hasMetGoal == true)) {
                                "· Completed today"
                            } else {
                                "· Read today to continue"
                            }
                            Text(
                                text = statusNote,
                                fontFamily = InterFontFamily,
                                fontSize = 12.sp,
                                color = colors.mutedText,
                                maxLines = 1
                            )
                        }
                    } else {
                        // Clean CTA to prompt to create a streak
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (streak?.isOptedIn != true) {
                                        streakViewModel?.reopenStreakOnboarding()
                                    }
                                    onNavigateToStreaks()
                                }
                                .testTag("start_streak_cta")
                        ) {
                            Text(
                                text = "Start a reading streak →",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = colors.wineText,
                                textDecoration = TextDecoration.Underline,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Read 1 page today",
                                fontFamily = InterFontFamily,
                                fontSize = 12.sp,
                                color = colors.mutedText,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Lower Section: Charcoal area with wavy top edge for Distributions (sits flush above shared BottomNav)
            val hasDistributions = distributions.isNotEmpty()
            val activeDistribution = distributions.firstOrNull()
            val distributionParticipants = activeDistribution?.let { participantsByDistribution[it.id].orEmpty() } ?: emptyList()

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
            ) {
                // Wavy top edge with cached Path
                Spacer(
                    modifier = Modifier
                        .fillMaxSize()
                        .drawWithCache {
                            val w = size.width
                            val h = size.height
                            val waveAmp = 18.dp.toPx()
                            val waveBaseY = 26.dp.toPx()

                            val path = Path().apply {
                                moveTo(0f, waveBaseY)
                                cubicTo(
                                    w * 0.25f, waveBaseY - waveAmp,
                                    w * 0.70f, waveBaseY + waveAmp,
                                    w, waveBaseY - waveAmp * 0.35f
                                )
                                lineTo(w, h)
                                lineTo(0f, h)
                                close()
                            }

                            onDrawBehind {
                                drawPath(
                                    path = path,
                                    color = colors.deepestSection
                                )
                            }
                        }
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    // "YOUR DISTRIBUTIONS" block
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (hasDistributions && activeDistribution != null) {
                                    onOpenDistributionDetail(activeDistribution.id)
                                } else {
                                    onCreateNewDistribution(DistributionType.KHATM)
                                }
                            },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "YOUR DISTRIBUTIONS",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                letterSpacing = 1.1.sp,
                                color = colors.gold,
                                maxLines = 1
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            if (hasDistributions && activeDistribution != null) {
                                val completedCount = distributionParticipants.count { it.isCompleted }
                                Text(
                                    text = "${activeDistribution.name.take(18)}\n$completedCount of ${activeDistribution.totalTarget} paras read.",
                                    fontFamily = FrauncesFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 22.sp,
                                    lineHeight = 28.sp,
                                    color = Color(0xFFF1EBDD),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                val subText = if (distributionParticipants.size > 1) {
                                    "${distributionParticipants.first().name} and ${distributionParticipants.size - 1} others participating."
                                } else if (distributionParticipants.isNotEmpty()) {
                                    "${distributionParticipants.first().name} participating."
                                } else {
                                    "Tap to view distribution room."
                                }

                                Text(
                                    text = subText,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = Color(0xFFA29F94),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else {
                                // Clean Distributions Empty State
                                Text(
                                    text = "Start a distribution\nwith family & friends.",
                                    fontFamily = FrauncesFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 22.sp,
                                    lineHeight = 28.sp,
                                    color = Color(0xFFF1EBDD),
                                    maxLines = 2
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Distribute 30 paras & complete Khatm together.",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = Color(0xFFA29F94),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Overlapping circles with participant initials or clean empty state
                        val initial1 = if (hasDistributions) {
                            distributionParticipants.getOrNull(0)?.name?.trim()?.firstOrNull()?.uppercase() ?: "K"
                        } else {
                            "+"
                        }
                        val initial2 = if (hasDistributions) {
                            distributionParticipants.getOrNull(1)?.name?.trim()?.firstOrNull()?.uppercase() ?: "30"
                        } else {
                            "30"
                        }

                        OverlappingInitialsCircles(
                            initial1 = initial1,
                            initial2 = initial2,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Top right decorative overlapping circles cropped by screen edge
 */
@Composable
private fun TopRightBackgroundBubbles(
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        val w = size.width

        val circle1Center = Offset(w - 20.dp.toPx(), 40.dp.toPx())
        val circle1Radius = 75.dp.toPx()
        val circle1Color = if (isDark) Color(0xFF3B3934) else Color(0xFFE9E1D1)

        drawCircle(
            color = circle1Color,
            radius = circle1Radius,
            center = circle1Center
        )

        val circle2Center = Offset(w + 10.dp.toPx(), 0f)
        val circle2Radius = 55.dp.toPx()
        val circle2Color = if (isDark) Color(0xFF2F6B5A) else Color(0xFF1F3D36)

        drawCircle(
            color = circle2Color,
            radius = circle2Radius,
            center = circle2Center
        )
    }
}

/**
 * 44dp round settings button containing a stroke gear icon (1.8 stroke width)
 */
@Composable
private fun RoundSettingsButton(
    onClick: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isDark) Color(0xFF262B31) else Color(0xFFF6F0E4)
    val borderColor = if (isDark) Color(0xFF4B8F7B) else Color(0xFFC5A77D)
    val iconColor = if (isDark) Color(0xFFF1EBDD) else Color(0xFF1B1B19)

    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(bgColor)
            .border(1.2.dp, borderColor, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .testTag("home_settings_button")
            .semantics { role = Role.Button },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(24.dp)) {
            val center = center
            val outerRadius = 8.5.dp.toPx()
            val innerRadius = 3.6.dp.toPx()
            val toothLength = 2.4.dp.toPx()
            val strokePx = 1.8.dp.toPx()

            drawCircle(
                color = iconColor,
                radius = innerRadius,
                center = center,
                style = Stroke(width = strokePx)
            )

            val numTeeth = 8
            for (i in 0 until numTeeth) {
                val angle = (i * 2f * PI / numTeeth).toFloat()
                val startX = center.x + outerRadius * cos(angle)
                val startY = center.y + outerRadius * sin(angle)
                val endX = center.x + (outerRadius + toothLength) * cos(angle)
                val endY = center.y + (outerRadius + toothLength) * sin(angle)

                drawLine(
                    color = iconColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = strokePx,
                    cap = StrokeCap.Round
                )
            }

            drawCircle(
                color = iconColor,
                radius = outerRadius,
                center = center,
                style = Stroke(width = strokePx)
            )
        }
    }
}

/**
 * Two overlapping circles with initials:
 * Circle 1: 58dp diameter in gold
 * Circle 2: 40dp diameter in pine, offset to bottom-right
 */
@Composable
private fun OverlappingInitialsCircles(
    initial1: String,
    initial2: String,
    modifier: Modifier = Modifier
) {
    val colors = QuranBaantoDesignSystem.colors

    Box(
        modifier = modifier.size(width = 84.dp, height = 68.dp),
        contentAlignment = Alignment.TopStart
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(colors.gold),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial1,
                fontFamily = FrauncesFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = if (initial1.length > 1) 16.sp else 21.sp,
                color = Color(0xFF1B1B19)
            )
        }

        Box(
            modifier = Modifier
                .offset(x = 40.dp, y = 26.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(colors.pine),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial2,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = if (initial2.length > 1) 12.sp else 15.sp,
                color = Color(0xFFF1EBDD)
            )
        }
    }
}
