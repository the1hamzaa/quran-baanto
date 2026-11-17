package com.example.ui.streak

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.db.DailyLogEntity
import com.example.data.db.PlanType
import com.example.data.db.ReadingPlanEntity
import com.example.data.quran.QuranPageMapper
import com.example.data.quran.QuranRepository
import com.example.data.quran.SurahInfo
import com.example.ui.components.Toggle
import com.example.ui.components.WaveDivider
import com.example.ui.components.WaveSlider
import com.example.ui.distribution.WaveTrack
import com.example.ui.theme.FrauncesFontFamily
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.QuranBaantoDesignSystem
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Page 1 of Progress Pager: Your Plans
 * Matches Plans_1d54bd6d.png & Plans___dark_1cf5c88f.png precisely.
 * Zero draft data: shows clean empty state when no plans exist.
 */
@Composable
fun PlansPage(
    plans: List<ReadingPlanEntity>,
    todayLog: DailyLogEntity?,
    onCreateNewPlan: () -> Unit,
    onOpenQuranPage: (pageNumber: Int, surahNumber: Int?) -> Unit,
    onMarkPlanTodayDone: (Long) -> Unit,
    onDeletePlan: (Long) -> Unit,
    onGoToStreak: () -> Unit,
    onGoToCircles: () -> Unit,
    onSelectPage: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = QuranBaantoDesignSystem.colors
    val context = LocalContext.current
    val pagesReadToday = todayLog?.pagesReadCount ?: 0
    val ayahsReadToday = todayLog?.ayahsReadCount ?: 0

    var selectedPlanForAction by remember { mutableStateOf<ReadingPlanEntity?>(null) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        TopRightProgressBubble(color = colors.softNeutral)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp)
                    .padding(top = 42.dp, bottom = 12.dp)
            ) {
                Text(
                    text = "YOUR PLANS",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    color = colors.mutedText
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (plans.isEmpty()) {
                    // Clean Empty State (NO draft/fake plans)
                    Text(
                        text = "No plans yet",
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 44.sp,
                        lineHeight = 48.sp,
                        letterSpacing = (-0.5).sp,
                        color = colors.text
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Create a plan to finish the Quran at your own pace, memorize a surah, or keep a weekly habit.",
                        fontFamily = InterFontFamily,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = colors.mutedText
                    )

                    Spacer(modifier = Modifier.height(26.dp))

                    WaveDivider(
                        color = colors.gold.copy(alpha = 0.65f),
                        strokeWidth = 1.4.dp,
                        amplitude = 2.dp,
                        wavelength = 48.dp
                    )

                    Spacer(modifier = Modifier.height(26.dp))

                    Text(
                        text = "+ Create your first plan",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        textDecoration = TextDecoration.Underline,
                        color = colors.pineText,
                        modifier = Modifier
                            .clickable { onCreateNewPlan() }
                            .padding(vertical = 8.dp)
                            .testTag("empty_create_plan_cta")
                    )
                } else {
                    val primaryPlan = plans.first()
                    val otherPlans = plans.drop(1)

                    // Primary Plan Hero
                    val currentDayOrWeek = computeCurrentDayNumber(primaryPlan)
                    val heroHeading = if (primaryPlan.planType == PlanType.WEEKLY) {
                        "Week $currentDayOrWeek"
                    } else {
                        "Day $currentDayOrWeek"
                    }
                    val heroSubtitle = "of ${primaryPlan.totalDaysCount} · ${primaryPlan.title}"

                    val (todayLabel, todayProgressLabel, progressFraction) = computeTodayPlanSummary(
                        plan = primaryPlan,
                        dayNumber = currentDayOrWeek,
                        pagesReadToday = pagesReadToday,
                        ayahsReadToday = ayahsReadToday
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { selectedPlanForAction = primaryPlan }
                    ) {
                        Text(
                            text = heroHeading,
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 56.sp,
                            lineHeight = 60.sp,
                            letterSpacing = (-0.8).sp,
                            color = colors.text
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = heroSubtitle,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 15.sp,
                            color = colors.mutedText
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        WaveTrack(
                            progress = progressFraction,
                            pineColor = colors.pine,
                            trackColor = colors.softNeutral,
                            strokeWidth = 3.dp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    val startPage = computeTodayStartPage(primaryPlan, currentDayOrWeek)
                                    onOpenQuranPage(startPage, primaryPlan.targetSurahNumber)
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = todayLabel,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = colors.text
                            )
                            Text(
                                text = todayProgressLabel,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.sp,
                                color = colors.mutedText
                            )
                        }
                    }

                    if (otherPlans.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(40.dp))

                        Text(
                            text = "OTHER PLANS",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            letterSpacing = 1.2.sp,
                            color = colors.mutedText
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        otherPlans.forEachIndexed { idx, plan ->
                            val currentStep = computeCurrentDayNumber(plan).coerceAtLeast(plan.completedDaysCount)
                            val fractionText = "$currentStep/${plan.totalDaysCount}"
                            val subline = formatOtherPlanSubtitle(plan)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedPlanForAction = plan }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = fractionText,
                                    fontFamily = FrauncesFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 24.sp,
                                    color = colors.pineText,
                                    modifier = Modifier.width(84.dp)
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = plan.title,
                                        fontFamily = FrauncesFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 18.sp,
                                        color = colors.text,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = subline,
                                        fontFamily = InterFontFamily,
                                        fontSize = 13.sp,
                                        color = colors.mutedText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            WaveDivider(
                                color = colors.softNeutral,
                                strokeWidth = 1.2.dp,
                                amplitude = 2.dp,
                                wavelength = 48.dp
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(34.dp))
                        WaveDivider(
                            color = colors.softNeutral,
                            strokeWidth = 1.2.dp,
                            amplitude = 2.dp,
                            wavelength = 48.dp
                        )
                    }

                    Spacer(modifier = Modifier.height(26.dp))

                    Text(
                        text = "+ New plan",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        textDecoration = TextDecoration.Underline,
                        color = colors.pineText,
                        modifier = Modifier
                            .clickable { onCreateNewPlan() }
                            .padding(vertical = 6.dp)
                            .testTag("new_plan_button")
                    )
                }
            }

            PagerFooterIndicator(
                currentPage = 1,
                onSelectPage = onSelectPage,
                onCharcoal = false,
                leftLabel = "← Streak",
                onLeftClick = onGoToStreak,
                rightLabel = "Circles →",
                onRightClick = onGoToCircles
            )
        }
    }

    // Plan Action Dialog (Open in Quran / Mark today done / Delete plan)
    if (selectedPlanForAction != null) {
        val plan = selectedPlanForAction!!
        val todayKey = LocalDate.now().toString()
        val doneTodayAlready = plan.lastProgressDate == todayKey

        Dialog(onDismissRequest = { selectedPlanForAction = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = colors.background,
                border = BorderStroke(1.dp, colors.gold.copy(alpha = 0.5f))
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
                            text = plan.planType.name.replace("_", " "),
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            letterSpacing = 1.1.sp,
                            color = colors.mutedText
                        )
                        IconButton(
                            onClick = { selectedPlanForAction = null },
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
                        text = plan.title,
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 26.sp,
                        color = colors.text
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = formatOtherPlanSubtitle(plan),
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        color = colors.mutedText
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    WaveDivider(color = colors.gold.copy(alpha = 0.55f), strokeWidth = 1.2.dp)
                    Spacer(modifier = Modifier.height(18.dp))

                    // Open in Quran CTA
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(colors.pine)
                            .clickable {
                                val startPage = computeTodayStartPage(plan, computeCurrentDayNumber(plan))
                                selectedPlanForAction = null
                                onOpenQuranPage(startPage, plan.targetSurahNumber)
                            }
                            .padding(horizontal = 20.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Read in Quran",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
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

                    if (!doneTodayAlready) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(24.dp))
                                .border(1.2.dp, colors.pineStrokes, RoundedCornerShape(24.dp))
                                .clickable {
                                    onMarkPlanTodayDone(plan.id)
                                    selectedPlanForAction = null
                                    Toast.makeText(context, "Today's progress recorded", Toast.LENGTH_SHORT).show()
                                }
                                .padding(horizontal = 20.dp, vertical = 13.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Mark today complete",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = colors.pineText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Remove plan",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        textDecoration = TextDecoration.Underline,
                        color = colors.wineText,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clickable {
                                onDeletePlan(plan.id)
                                selectedPlanForAction = null
                            }
                            .padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Full-screen Create a Plan flow matching Create_a_plan_6971cf3c.png & Create_a_plan___dark_2d6897ce.png.
 */
@Composable
fun CreatePlanScreen(
    currentBookmarkPage: Int,
    onCreateFinishQuran: (days: Int, startPage: Int, dailyReminder: Boolean) -> Unit,
    onCreateMemorize: (title: String, surahNumber: Int?, versesPerDay: Int, totalVerses: Int, dailyReminder: Boolean) -> Unit,
    onCreateWeekly: (title: String, surahNumber: Int?, dayOfWeek: Int, totalWeeks: Int, dailyReminder: Boolean) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onBack() }

    val colors = QuranBaantoDesignSystem.colors
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Finish the Quran, 1 = Memorize, 2 = Weekly
    var dailyReminder by remember { mutableStateOf(true) }

    // Tab 0 state: Finish the Quran
    var finishDays by remember { mutableFloatStateOf(30f) }
    var startFromBookmark by remember { mutableStateOf(false) }

    // Tab 1 state: Memorize
    val allSurahs = QuranRepository.ALL_SURAHS
    var selectedMemSurah by remember {
        mutableStateOf(allSurahs.firstOrNull { it.number == 67 } ?: allSurahs.first())
    }
    var versesPerDay by remember { mutableFloatStateOf(5f) }
    var showSurahPicker by remember { mutableStateOf(false) }

    // Tab 2 state: Weekly
    var weeklyTitle by remember { mutableStateOf("Surah Al-Kahf on Fridays") }
    var selectedWeeklySurah by remember {
        mutableStateOf<SurahInfo?>(allSurahs.firstOrNull { it.number == 18 })
    }
    var selectedDayOfWeek by remember { mutableIntStateOf(5) } // 5 = Friday
    var weeklyCount by remember { mutableFloatStateOf(52f) }

    val todayFormatted = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("MMM d", Locale.US))
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
                    text = "NEW PLAN",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.2.sp,
                    color = colors.mutedText
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "What's your goal?",
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 36.sp,
                    letterSpacing = (-0.5).sp,
                    color = colors.text
                )

                Spacer(modifier = Modifier.height(22.dp))

                // 3 Tabs with wavy underline under active tab
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(22.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    listOf("Finish the Quran", "Memorize", "Weekly").forEachIndexed { idx, label ->
                        val isSelected = selectedTab == idx
                        Column(
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { selectedTab = idx }
                                .padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = label,
                                fontFamily = InterFontFamily,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 15.sp,
                                color = if (isSelected) colors.pineText else colors.mutedText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (isSelected) {
                                WaveDivider(
                                    color = colors.gold,
                                    strokeWidth = 1.8.dp,
                                    amplitude = 2.dp,
                                    wavelength = 22.dp,
                                    modifier = Modifier.width(
                                        when (idx) {
                                            0 -> 114.dp
                                            1 -> 72.dp
                                            else -> 54.dp
                                        }
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(26.dp))

                when (selectedTab) {
                    0 -> {
                        // TAB 0: Finish the Quran
                        val daysInt = finishDays.roundToInt().coerceIn(7, 365)
                        val startP = if (startFromBookmark) currentBookmarkPage.coerceIn(1, 604) else 1
                        val remainingPages = (604 - startP + 1).coerceAtLeast(1)
                        val pacePages = ceil(remainingPages.toDouble() / daysInt).toInt().coerceAtLeast(1)

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$daysInt",
                                fontFamily = FrauncesFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 74.sp,
                                lineHeight = 76.sp,
                                color = colors.text
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                                Text(
                                    text = "days",
                                    fontFamily = InterFontFamily,
                                    fontSize = 21.sp,
                                    color = colors.mutedText
                                )
                                Text(
                                    text = "to finish the Quran",
                                    fontFamily = InterFontFamily,
                                    fontSize = 13.sp,
                                    color = colors.mutedText
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        WaveSlider(
                            value = finishDays,
                            onValueChange = { finishDays = it },
                            valueRange = 7f..365f
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "7 days",
                                fontFamily = InterFontFamily,
                                fontSize = 12.sp,
                                color = colors.mutedText
                            )
                            Text(
                                text = "1 year",
                                fontFamily = InterFontFamily,
                                fontSize = 12.sp,
                                color = colors.mutedText
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        PlanSettingRow(
                            label = "Pace",
                            value = "≈ $pacePages ${if (pacePages == 1) "page" else "pages"} a day"
                        )
                        PlanSettingRow(
                            label = "Starts",
                            value = "Today · $todayFormatted"
                        )
                        if (currentBookmarkPage > 1) {
                            PlanSettingRow(
                                label = "Start from",
                                value = if (startFromBookmark) "Page $currentBookmarkPage (bookmark)" else "Page 1 (beginning)",
                                onClick = { startFromBookmark = !startFromBookmark }
                            )
                        }
                        PlanToggleRow(
                            label = "Daily reminder",
                            checked = dailyReminder,
                            onCheckedChange = { dailyReminder = it }
                        )
                    }

                    1 -> {
                        // TAB 1: Memorize
                        val perDayInt = versesPerDay.roundToInt().coerceIn(1, 20)
                        val estDays = ceil(selectedMemSurah.versesCount.toDouble() / perDayInt).toInt().coerceAtLeast(1)

                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$perDayInt",
                                fontFamily = FrauncesFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 74.sp,
                                lineHeight = 76.sp,
                                color = colors.text
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                                Text(
                                    text = if (perDayInt == 1) "verse" else "verses",
                                    fontFamily = InterFontFamily,
                                    fontSize = 21.sp,
                                    color = colors.mutedText
                                )
                                Text(
                                    text = "every day",
                                    fontFamily = InterFontFamily,
                                    fontSize = 13.sp,
                                    color = colors.mutedText
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        WaveSlider(
                            value = versesPerDay,
                            onValueChange = { versesPerDay = it },
                            valueRange = 1f..20f
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("1 verse", fontFamily = InterFontFamily, fontSize = 12.sp, color = colors.mutedText)
                            Text("20 verses", fontFamily = InterFontFamily, fontSize = 12.sp, color = colors.mutedText)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        PlanSettingRow(
                            label = "Surah",
                            value = "${selectedMemSurah.transliteration} (${selectedMemSurah.versesCount}v) ▾",
                            onClick = { showSurahPicker = true }
                        )
                        PlanSettingRow(
                            label = "Duration",
                            value = "≈ $estDays ${if (estDays == 1) "day" else "days"}"
                        )
                        PlanSettingRow(
                            label = "Starts",
                            value = "Today · $todayFormatted"
                        )
                        PlanToggleRow(
                            label = "Daily reminder",
                            checked = dailyReminder,
                            onCheckedChange = { dailyReminder = it }
                        )
                    }

                    else -> {
                        // TAB 2: Weekly
                        val weeksInt = weeklyCount.roundToInt().coerceIn(4, 52)
                        val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                        val dayFullNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

                        Text(
                            text = "HABIT TITLE",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            letterSpacing = 1.1.sp,
                            color = colors.mutedText
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        BasicTextField(
                            value = weeklyTitle,
                            onValueChange = { weeklyTitle = it },
                            textStyle = TextStyle(
                                fontFamily = FrauncesFontFamily,
                                fontSize = 22.sp,
                                color = colors.text
                            ),
                            cursorBrush = SolidColor(colors.gold),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        WaveDivider(color = colors.gold, strokeWidth = 1.4.dp)

                        Spacer(modifier = Modifier.height(12.dp))

                        // Quick Surah presets
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                18 to "Al-Kahf",
                                36 to "Yaseen",
                                67 to "Al-Mulk"
                            ).forEach { (surahNum, shortName) ->
                                val active = selectedWeeklySurah?.number == surahNum
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (active) colors.pine else colors.softNeutral)
                                        .clickable {
                                            val info = allSurahs.firstOrNull { it.number == surahNum }
                                            selectedWeeklySurah = info
                                            weeklyTitle = "Surah $shortName on ${dayFullNames[selectedDayOfWeek - 1]}s"
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = shortName,
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = if (active) Color(0xFFF1EBDD) else colors.text
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        Text(
                            text = "DAY OF WEEK",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            letterSpacing = 1.1.sp,
                            color = colors.mutedText
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            dayNames.forEachIndexed { i, shortDay ->
                                val dayIdx = i + 1
                                val active = selectedDayOfWeek == dayIdx
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (active) colors.pine else Color.Transparent)
                                        .border(
                                            1.2.dp,
                                            if (active) colors.pine else colors.gold.copy(alpha = 0.6f),
                                            CircleShape
                                        )
                                        .clickable {
                                            selectedDayOfWeek = dayIdx
                                            if (selectedWeeklySurah != null) {
                                                weeklyTitle = "Surah ${selectedWeeklySurah!!.transliteration} on ${dayFullNames[i]}s"
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = shortDay.take(1),
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = if (active) Color(0xFFF1EBDD) else colors.text
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        WaveSlider(
                            value = weeklyCount,
                            onValueChange = { weeklyCount = it },
                            valueRange = 4f..52f
                        )

                        PlanSettingRow(
                            label = "Schedule",
                            value = "Every ${dayFullNames[selectedDayOfWeek - 1]}"
                        )
                        PlanSettingRow(
                            label = "Duration",
                            value = "$weeksInt weeks"
                        )
                        PlanToggleRow(
                            label = "Daily reminder",
                            checked = dailyReminder,
                            onCheckedChange = { dailyReminder = it }
                        )
                    }
                }
            }

            // Bottom Right-Extending Pill CTA
            Box(modifier = Modifier.padding(bottom = 24.dp)) {
                RightExtendingPillButton(
                    text = "Start plan",
                    onClick = {
                        when (selectedTab) {
                            0 -> {
                                val daysInt = finishDays.roundToInt().coerceIn(7, 365)
                                val startP = if (startFromBookmark) currentBookmarkPage.coerceIn(1, 604) else 1
                                onCreateFinishQuran(daysInt, startP, dailyReminder)
                            }
                            1 -> {
                                val perDayInt = versesPerDay.roundToInt().coerceIn(1, 20)
                                onCreateMemorize(
                                    "Memorize Surah ${selectedMemSurah.transliteration}",
                                    selectedMemSurah.number,
                                    perDayInt,
                                    selectedMemSurah.versesCount,
                                    dailyReminder
                                )
                            }
                            else -> {
                                val weeksInt = weeklyCount.roundToInt().coerceIn(4, 52)
                                onCreateWeekly(
                                    weeklyTitle.trim().ifBlank { "Weekly Recitation" },
                                    selectedWeeklySurah?.number,
                                    selectedDayOfWeek,
                                    weeksInt,
                                    dailyReminder
                                )
                            }
                        }
                    },
                    modifier = Modifier.testTag("start_plan_button")
                )
            }
        }
    }

    // Surah Selector Dialog for Memorize tab
    if (showSurahPicker) {
        Dialog(onDismissRequest = { showSurahPicker = false }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = colors.background,
                border = BorderStroke(1.dp, colors.gold.copy(alpha = 0.5f)),
                modifier = Modifier.height(480.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Choose a Surah",
                        fontFamily = FrauncesFontFamily,
                        fontSize = 24.sp,
                        color = colors.text
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    WaveDivider(color = colors.gold.copy(alpha = 0.5f), strokeWidth = 1.2.dp)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(allSurahs, key = { it.number }) { surah ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        selectedMemSurah = surah
                                        showSurahPicker = false
                                    }
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${surah.number}. ${surah.transliteration}",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp,
                                    color = colors.text
                                )
                                Text(
                                    text = "${surah.versesCount} verses",
                                    fontFamily = InterFontFamily,
                                    fontSize = 13.sp,
                                    color = colors.mutedText
                                )
                            }
                            HorizontalDivider(thickness = 0.6.dp, color = colors.rules)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlanSettingRow(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    val colors = QuranBaantoDesignSystem.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) Modifier.clickable { onClick() } else Modifier
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 15.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = colors.text
            )
            Text(
                text = value,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                color = if (onClick != null) colors.pineText else colors.mutedText
            )
        }
        HorizontalDivider(thickness = 0.8.dp, color = colors.rules)
    }
}

@Composable
private fun PlanToggleRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = QuranBaantoDesignSystem.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = colors.text
            )
            Toggle(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
        HorizontalDivider(thickness = 0.8.dp, color = colors.rules)
    }
}

private fun computeCurrentDayNumber(plan: ReadingPlanEntity): Int {
    if (plan.startDate.isBlank()) return (plan.completedDaysCount + 1).coerceIn(1, plan.totalDaysCount.coerceAtLeast(1))
    return try {
        val start = LocalDate.parse(plan.startDate)
        val today = LocalDate.now()
        if (plan.planType == PlanType.WEEKLY) {
            val weeks = ChronoUnit.WEEKS.between(start, today).toInt()
            (weeks + 1).coerceIn(1, plan.totalDaysCount.coerceAtLeast(1))
        } else {
            val days = ChronoUnit.DAYS.between(start, today).toInt()
            (days + 1).coerceIn(1, plan.totalDaysCount.coerceAtLeast(1))
        }
    } catch (_: Exception) {
        (plan.completedDaysCount + 1).coerceIn(1, plan.totalDaysCount.coerceAtLeast(1))
    }
}

private fun computeTodayStartPage(plan: ReadingPlanEntity, dayNumber: Int): Int {
    if (plan.planType == PlanType.MEMORIZE || plan.planType == PlanType.WEEKLY) {
        val surahNum = plan.targetSurahNumber ?: 1
        return QuranPageMapper.getPageForAyah(surahNum, 1)
    }
    val dailyPages = plan.dailyTargetPages.coerceAtLeast(1)
    return (((plan.startPage - 1) + (dayNumber - 1) * dailyPages) % 604) + 1
}

private data class PlanTodaySummary(
    val leftLabel: String,
    val rightLabel: String,
    val progressFraction: Float
)

private fun computeTodayPlanSummary(
    plan: ReadingPlanEntity,
    dayNumber: Int,
    pagesReadToday: Int,
    ayahsReadToday: Int
): PlanTodaySummary {
    return when (plan.planType) {
        PlanType.MEMORIZE -> {
            val goal = plan.dailyTargetAyahs.coerceAtLeast(1)
            val doneToday = if (plan.lastProgressDate == LocalDate.now().toString()) goal else minOf(ayahsReadToday, goal)
            val overallProg = (dayNumber.toFloat() / plan.totalDaysCount.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
            PlanTodaySummary(
                leftLabel = "Today · $goal verses",
                rightLabel = "$doneToday of $goal done",
                progressFraction = overallProg
            )
        }
        PlanType.WEEKLY -> {
            val targetDow = DayOfWeek.of(plan.weeklyDayOfWeek.coerceIn(1, 7))
            val today = LocalDate.now()
            val nextOcc = if (today.dayOfWeek == targetDow) "Today" else {
                val next = today.with(TemporalAdjusters.next(targetDow))
                next.format(DateTimeFormatter.ofPattern("EEEE", Locale.US))
            }
            val done = if (plan.lastProgressDate == today.toString()) 1 else 0
            val overallProg = (plan.completedDaysCount.coerceAtLeast(done).toFloat() / plan.totalDaysCount.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
            PlanTodaySummary(
                leftLabel = "Next · $nextOcc",
                rightLabel = if (done == 1) "Done this week" else "0 of 1 done",
                progressFraction = overallProg
            )
        }
        else -> {
            val dailyPages = plan.dailyTargetPages.coerceAtLeast(1)
            val startP = computeTodayStartPage(plan, dayNumber)
            val endP = (startP + dailyPages - 1).coerceAtMost(604)
            val doneToday = minOf(pagesReadToday, dailyPages)
            val overallProg = (dayNumber.toFloat() / plan.totalDaysCount.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
            PlanTodaySummary(
                leftLabel = "Today · pages $startP–$endP",
                rightLabel = "$doneToday of $dailyPages done",
                progressFraction = overallProg
            )
        }
    }
}

private fun formatOtherPlanSubtitle(plan: ReadingPlanEntity): String {
    return when (plan.planType) {
        PlanType.MEMORIZE -> {
            val status = if (plan.lastProgressDate == LocalDate.now().toString()) "done today" else "on track"
            "${plan.dailyTargetAyahs} verses a day · $status"
        }
        PlanType.WEEKLY -> {
            val targetDow = DayOfWeek.of(plan.weeklyDayOfWeek.coerceIn(1, 7))
            val today = LocalDate.now()
            val nextText = if (today.dayOfWeek == targetDow) "today" else "next ${targetDow.name.lowercase().replaceFirstChar { it.uppercase() }}"
            "Weekly · $nextText"
        }
        else -> "≈ ${plan.dailyTargetPages} pages a day · ${plan.totalDaysCount} days"
    }
}
