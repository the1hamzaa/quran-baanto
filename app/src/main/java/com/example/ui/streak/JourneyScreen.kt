package com.example.ui.streak

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.quran.QuranPageMapper
import com.example.ui.viewmodel.StreakViewModel
import kotlinx.coroutines.launch

private sealed interface ProgressSubScreen {
    data object None : ProgressSubScreen
    data object CreatePlan : ProgressSubScreen
    data object CreateCircle : ProgressSubScreen
    data object JoinCircle : ProgressSubScreen
    data class CircleDetail(val circleId: Long) : ProgressSubScreen
}

/**
 * Progress Root Screen (Streak · Plans · Circles)
 * Hosts:
 * 1. StartStreakScreen onboarding when streak has not been started or dismissed yet.
 * 2. 3-page HorizontalPager: Page 0 = StreakPage, Page 1 = PlansPage, Page 2 = CirclesPage.
 * 3. Full-screen Create/Join/Detail sub-screens with BackHandler support.
 */
@Composable
fun JourneyScreen(
    streakViewModel: StreakViewModel,
    currentBookmarkPage: Int = 1,
    onContinueReading: (paraNum: Int, surahNum: Int?, ayahNum: Int?) -> Unit,
    onOpenSurahReader: (surahNum: Int) -> Unit = { onContinueReading(1, it, 1) },
    onDismissOnboarding: () -> Unit = {},
    onFullscreenOverlayChanged: (Boolean) -> Unit = {},
    onCharcoalStateChanged: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val streak by streakViewModel.streakState.collectAsStateWithLifecycle()
    val todayLog by streakViewModel.todayLog.collectAsStateWithLifecycle()
    val activePlans by streakViewModel.activePlansState.collectAsStateWithLifecycle()
    val circles by streakViewModel.streakCirclesState.collectAsStateWithLifecycle()
    val allCircleMembers by streakViewModel.allCircleMembersState.collectAsStateWithLifecycle()
    val userProfileName by streakViewModel.userProfileName.collectAsStateWithLifecycle()

    val todayPagesRead = todayLog?.pagesReadCount ?: 0

    var subScreen by remember { mutableStateOf<ProgressSubScreen>(ProgressSubScreen.None) }
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })

    val openReaderAtLastPosition: () -> Unit = {
        val lastPara = streak?.lastReadPara
        if (lastPara != null && lastPara > 0) {
            onContinueReading(
                lastPara,
                streak?.lastReadSurah?.takeIf { it > 0 },
                streak?.lastReadAyah?.takeIf { it > 0 }
            )
        } else {
            onContinueReading(1, 1, 1)
        }
    }

    // Show Start a Streak onboarding if Streak entity has loaded and user hasn't opted in or dismissed yet
    val showStartOnboarding = streak != null &&
        streak?.isOptedIn == false &&
        streak?.hasDismissedStreakOnboarding == false &&
        subScreen == ProgressSubScreen.None

    LaunchedEffect(showStartOnboarding, subScreen) {
        onFullscreenOverlayChanged(showStartOnboarding || subScreen != ProgressSubScreen.None)
    }

    LaunchedEffect(pagerState.currentPage, showStartOnboarding, subScreen) {
        onCharcoalStateChanged(!showStartOnboarding && subScreen == ProgressSubScreen.None && pagerState.currentPage == 0)
    }

    BackHandler(enabled = subScreen != ProgressSubScreen.None) {
        subScreen = ProgressSubScreen.None
    }

    BackHandler(enabled = showStartOnboarding) {
        streakViewModel.dismissStreakOnboarding()
        onDismissOnboarding()
    }

    if (showStartOnboarding) {
        StartStreakScreen(
            onStartToday = {
                streakViewModel.optInToStreak()
                openReaderAtLastPosition()
            },
            onMaybeLater = {
                streakViewModel.dismissStreakOnboarding()
                onDismissOnboarding()
            }
        )
        return
    }

    AnimatedContent(
        targetState = subScreen,
        transitionSpec = {
            fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing)) togetherWith
                fadeOut(animationSpec = tween(150, easing = FastOutSlowInEasing))
        },
        label = "progress_subscreen_transition"
    ) { activeSubScreen ->
        when (activeSubScreen) {
            ProgressSubScreen.CreatePlan -> {
                CreatePlanScreen(
                    currentBookmarkPage = currentBookmarkPage,
                    onCreateFinishQuran = { days, startPage, dailyReminder ->
                        streakViewModel.createFinishQuranPlan(
                            durationDays = days,
                            startPage = startPage,
                            dailyReminderEnabled = dailyReminder
                        ) {
                            subScreen = ProgressSubScreen.None
                            scope.launch { pagerState.animateScrollToPage(1) }
                        }
                    },
                    onCreateMemorize = { title, surahNumber, versesPerDay, totalVerses, dailyReminder ->
                        streakViewModel.createMemorizePlan(
                            title = title,
                            surahNumber = surahNumber,
                            versesPerDay = versesPerDay,
                            totalVerses = totalVerses,
                            dailyReminderEnabled = dailyReminder
                        ) {
                            subScreen = ProgressSubScreen.None
                            scope.launch { pagerState.animateScrollToPage(1) }
                        }
                    },
                    onCreateWeekly = { title, surahNumber, dayOfWeek, totalWeeks, dailyReminder ->
                        streakViewModel.createWeeklyPlan(
                            title = title,
                            surahNumber = surahNumber,
                            dayOfWeek = dayOfWeek,
                            totalWeeks = totalWeeks,
                            dailyReminderEnabled = dailyReminder
                        ) {
                            subScreen = ProgressSubScreen.None
                            scope.launch { pagerState.animateScrollToPage(1) }
                        }
                    },
                    onBack = { subScreen = ProgressSubScreen.None }
                )
            }

            ProgressSubScreen.CreateCircle -> {
                CreateCircleScreen(
                    generatePasscode = { name -> streakViewModel.generateCirclePasscode(name) },
                    onCreateCircle = { name, colorIndex, passcode ->
                        val creatorName = userProfileName.trim().ifBlank { "You" }
                        streakViewModel.createStreakCircle(
                            name = name,
                            creatorName = creatorName,
                            colorIndex = colorIndex,
                            customPasscode = passcode
                        ) { created ->
                            subScreen = ProgressSubScreen.CircleDetail(created.id)
                        }
                    },
                    onBack = { subScreen = ProgressSubScreen.None }
                )
            }

            ProgressSubScreen.JoinCircle -> {
                JoinCircleScreen(
                    existingCircles = circles,
                    allMembers = allCircleMembers,
                    todayPagesRead = todayPagesRead,
                    onJoinCircle = { passcode ->
                        val memberName = userProfileName.trim().ifBlank { "You" }
                        streakViewModel.joinStreakCircle(
                            passcode = passcode,
                            memberName = memberName
                        ) { success, circleId, message ->
                            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                            if (success && circleId != null) {
                                subScreen = ProgressSubScreen.CircleDetail(circleId)
                            }
                        }
                    },
                    onBack = { subScreen = ProgressSubScreen.None }
                )
            }

            is ProgressSubScreen.CircleDetail -> {
                val targetCircle = circles.firstOrNull { it.id == activeSubScreen.circleId }
                if (targetCircle == null) {
                    subScreen = ProgressSubScreen.None
                } else {
                    val circleMembers = remember(allCircleMembers, targetCircle.id) {
                        allCircleMembers.filter { it.circleId == targetCircle.id }
                    }
                    CircleDetailScreen(
                        circle = targetCircle,
                        members = circleMembers,
                        todayPagesRead = todayPagesRead,
                        onBack = { subScreen = ProgressSubScreen.None },
                        onReadNow = openReaderAtLastPosition,
                        onNudgeMember = { member -> streakViewModel.nudgeCircleMember(member.id) },
                        onDeleteCircle = {
                            streakViewModel.deleteCircle(targetCircle.id)
                            subScreen = ProgressSubScreen.None
                        }
                    )
                }
            }

            ProgressSubScreen.None -> {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    beyondViewportPageCount = 1
                ) { page ->
                    when (page) {
                        0 -> StreakPage(
                            streak = streak,
                            todayLog = todayLog,
                            onReadNow = openReaderAtLastPosition,
                            onStartStreak = {
                                streakViewModel.optInToStreak()
                                openReaderAtLastPosition()
                            },
                            onGoToPlans = {
                                scope.launch { pagerState.animateScrollToPage(1) }
                            },
                            onSelectPage = { targetPage ->
                                scope.launch { pagerState.animateScrollToPage(targetPage) }
                            }
                        )

                        1 -> PlansPage(
                            plans = activePlans,
                            todayLog = todayLog,
                            onCreateNewPlan = { subScreen = ProgressSubScreen.CreatePlan },
                            onOpenQuranPage = { pageNumber, surahNumber ->
                                if (surahNumber != null) {
                                    onOpenSurahReader(surahNumber)
                                } else {
                                    val paraNum = QuranPageMapper.getJuzForPage(pageNumber)
                                    onContinueReading(paraNum, null, null)
                                }
                            },
                            onMarkPlanTodayDone = { planId ->
                                streakViewModel.markPlanTodayDone(planId)
                            },
                            onDeletePlan = { planId ->
                                streakViewModel.deletePlan(planId)
                            },
                            onGoToStreak = {
                                scope.launch { pagerState.animateScrollToPage(0) }
                            },
                            onGoToCircles = {
                                scope.launch { pagerState.animateScrollToPage(2) }
                            },
                            onSelectPage = { targetPage ->
                                scope.launch { pagerState.animateScrollToPage(targetPage) }
                            }
                        )

                        else -> CirclesPage(
                            circles = circles,
                            allMembers = allCircleMembers,
                            todayPagesRead = todayPagesRead,
                            onCircleClick = { circleId ->
                                subScreen = ProgressSubScreen.CircleDetail(circleId)
                            },
                            onOpenCreateCircle = { subScreen = ProgressSubScreen.CreateCircle },
                            onOpenJoinCircle = { subScreen = ProgressSubScreen.JoinCircle },
                            onGoToPlans = {
                                scope.launch { pagerState.animateScrollToPage(1) }
                            },
                            onSelectPage = { targetPage ->
                                scope.launch { pagerState.animateScrollToPage(targetPage) }
                            }
                        )
                    }
                }
            }
        }
    }
}
