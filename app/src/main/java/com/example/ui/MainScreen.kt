package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.db.DistributionType
import com.example.data.db.RoomMode
import com.example.data.quran.QuranPageMapper
import com.example.data.quran.ReadMode
import com.example.ui.components.BottomNav
import com.example.ui.distribution.CreateEditDistributionScreen
import com.example.ui.distribution.DistributionDetailScreen
import com.example.ui.distribution.DistributionListScreen
import com.example.ui.home.HomeScreen
import com.example.ui.navigation.BottomNavItem
import com.example.ui.quran.QuranListScreen
import com.example.ui.quran.QuranReaderScreen
import com.example.ui.settings.SettingsSheet
import com.example.ui.streak.JourneyScreen
import com.example.ui.theme.QuranBaantoDesignSystem
import com.example.ui.theme.QuranBaantoTheme
import com.example.ui.viewmodel.DistributionViewModel
import com.example.ui.viewmodel.QuranViewModel
import com.example.ui.viewmodel.StreakViewModel

private val TopLevelTabRoutes = setOf(
    BottomNavItem.Home.route,
    BottomNavItem.Quran.route,
    BottomNavItem.Distributions.route,
    BottomNavItem.Streaks.route
)

@Composable
fun MainScreen() {
    val quranViewModel: QuranViewModel = viewModel()
    val distributionViewModel: DistributionViewModel = viewModel()
    val streakViewModel: StreakViewModel = viewModel()
    val isDarkTheme by quranViewModel.isDarkTheme.collectAsState()
    var showSettingsSheet by remember { mutableStateOf(false) }

    var isDistributionFullscreen by remember { mutableStateOf(false) }
    var isProgressFullscreen by remember { mutableStateOf(false) }
    var isProgressOnCharcoal by remember { mutableStateOf(true) }

    QuranBaantoTheme(darkTheme = isDarkTheme) {
        val colors = QuranBaantoDesignSystem.colors
        val navController = rememberNavController()

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val isTopLevelRoute = currentRoute in TopLevelTabRoutes
        val shouldShowBottomBar = isTopLevelRoute &&
            !(currentRoute == BottomNavItem.Distributions.route && isDistributionFullscreen) &&
            !(currentRoute == BottomNavItem.Streaks.route && isProgressFullscreen)

        val onCharcoalBottomBar = when (currentRoute) {
            BottomNavItem.Home.route -> true
            BottomNavItem.Streaks.route -> isProgressOnCharcoal
            else -> false
        }
        val streakCharcoalBg = if (colors.isDark) colors.background else Color(0xFF181611)
        val bottomBarContainerColor by animateColorAsState(
            targetValue = when (currentRoute) {
                BottomNavItem.Home.route -> colors.deepestSection
                BottomNavItem.Streaks.route -> if (isProgressOnCharcoal) streakCharcoalBg else colors.background
                else -> colors.background
            },
            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
            label = "persistent_bottom_bar_bg"
        )

        val navigateTopLevelTab: (String) -> Unit = { route ->
            if (currentRoute != route) {
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) {
                        saveState = true
                    }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }

        Scaffold(
            bottomBar = {
                AnimatedVisibility(
                    visible = shouldShowBottomBar,
                    enter = fadeIn(animationSpec = tween(180)),
                    exit = fadeOut(animationSpec = tween(130))
                ) {
                    Surface(color = bottomBarContainerColor) {
                        BottomNav(
                            currentRoute = currentRoute ?: BottomNavItem.Home.route,
                            onCharcoal = onCharcoalBottomBar,
                            onNavigate = navigateTopLevelTab
                        )
                    }
                }
            },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = BottomNavItem.Home.route,
                enterTransition = {
                    val targetIsTab = targetState.destination.route in TopLevelTabRoutes
                    val initialIsTab = initialState.destination.route in TopLevelTabRoutes
                    if (targetIsTab && initialIsTab) {
                        fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                            scaleIn(
                                initialScale = 0.985f,
                                animationSpec = tween(220, easing = FastOutSlowInEasing)
                            )
                    } else {
                        slideInHorizontally(
                            initialOffsetX = { it / 6 },
                            animationSpec = tween(260, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(220))
                    }
                },
                exitTransition = {
                    val targetIsTab = targetState.destination.route in TopLevelTabRoutes
                    val initialIsTab = initialState.destination.route in TopLevelTabRoutes
                    if (targetIsTab && initialIsTab) {
                        fadeOut(animationSpec = tween(140, easing = FastOutLinearInEasing))
                    } else {
                        slideOutHorizontally(
                            targetOffsetX = { -it / 8 },
                            animationSpec = tween(220, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(160))
                    }
                },
                popEnterTransition = {
                    val targetIsTab = targetState.destination.route in TopLevelTabRoutes
                    val initialIsTab = initialState.destination.route in TopLevelTabRoutes
                    if (targetIsTab && initialIsTab) {
                        fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                            scaleIn(
                                initialScale = 0.985f,
                                animationSpec = tween(220, easing = FastOutSlowInEasing)
                            )
                    } else {
                        slideInHorizontally(
                            initialOffsetX = { -it / 8 },
                            animationSpec = tween(240, easing = FastOutSlowInEasing)
                        ) + fadeIn(animationSpec = tween(200))
                    }
                },
                popExitTransition = {
                    val targetIsTab = targetState.destination.route in TopLevelTabRoutes
                    val initialIsTab = initialState.destination.route in TopLevelTabRoutes
                    if (targetIsTab && initialIsTab) {
                        fadeOut(animationSpec = tween(140, easing = FastOutLinearInEasing))
                    } else {
                        slideOutHorizontally(
                            targetOffsetX = { it / 6 },
                            animationSpec = tween(240, easing = FastOutSlowInEasing)
                        ) + fadeOut(animationSpec = tween(180))
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                // Home Screen
                composable(BottomNavItem.Home.route) {
                    HomeScreen(
                        distributionViewModel = distributionViewModel,
                        quranViewModel = quranViewModel,
                        streakViewModel = streakViewModel,
                        onNavigateToStreaks = { navigateTopLevelTab(BottomNavItem.Streaks.route) },
                        onCreateNewDistribution = { type ->
                            navController.navigate("create_edit_distribution?type=${type.name}")
                        },
                        onOpenDistributionDetail = { distId ->
                            navController.navigate("distribution_detail/$distId")
                        },
                        onOpenParaReader = { paraNum, targetAyah, targetSurah ->
                            quranViewModel.selectPara(paraNum, targetAyah, targetSurah)
                            navController.navigate("quran_reader_para/$paraNum")
                        },
                        onOpenSurahReader = { surahNum, targetAyah ->
                            quranViewModel.selectSurah(surahNum, targetAyah)
                            navController.navigate("quran_reader_surah/$surahNum")
                        },
                        onOpenSettings = { showSettingsSheet = true }
                    )
                }

                // Qur'an List Screen
                composable(BottomNavItem.Quran.route) {
                    QuranListScreen(
                        quranViewModel = quranViewModel,
                        onOpenParaReader = { paraNum ->
                            if (quranViewModel.selectedParaNumber.value != paraNum || quranViewModel.targetScrollAyah.value == null) {
                                quranViewModel.selectPara(paraNum)
                            }
                            navController.navigate("quran_reader_para/$paraNum")
                        },
                        onOpenSurahReader = { surahNum ->
                            if (quranViewModel.selectedSurahNumber.value != surahNum || quranViewModel.targetScrollAyah.value == null) {
                                quranViewModel.selectSurah(surahNum)
                            }
                            navController.navigate("quran_reader_surah/$surahNum")
                        }
                    )
                }

                // Qur'an Reader Screens (Para and Surah)
                composable(
                    route = "quran_reader_para/{paraNum}",
                    arguments = listOf(navArgument("paraNum") { type = NavType.IntType })
                ) { backStackEntry ->
                    val paraNum = backStackEntry.arguments?.getInt("paraNum") ?: 1
                    LaunchedEffect(paraNum) {
                        if (quranViewModel.selectedParaNumber.value != paraNum) {
                            quranViewModel.selectPara(paraNum)
                        }
                    }
                    QuranReaderScreen(
                        quranViewModel = quranViewModel,
                        onBack = { navController.popBackStack() },
                        onAyahRead = { sNum, aNum, pNum, sName ->
                            streakViewModel.recordAyahRecitation(1, sNum, aNum, pNum, sName)
                        }
                    )
                }

                composable(
                    route = "quran_reader_surah/{surahNum}",
                    arguments = listOf(navArgument("surahNum") { type = NavType.IntType })
                ) { backStackEntry ->
                    val surahNum = backStackEntry.arguments?.getInt("surahNum") ?: 1
                    LaunchedEffect(surahNum) {
                        if (quranViewModel.selectedSurahNumber.value != surahNum) {
                            quranViewModel.selectSurah(surahNum)
                        }
                    }
                    QuranReaderScreen(
                        quranViewModel = quranViewModel,
                        onBack = { navController.popBackStack() },
                        onAyahRead = { sNum, aNum, pNum, sName ->
                            streakViewModel.recordAyahRecitation(1, sNum, aNum, pNum, sName)
                        }
                    )
                }

                // Distributions List Screen
                composable(BottomNavItem.Distributions.route) {
                    DistributionListScreen(
                        distributionViewModel = distributionViewModel,
                        onCreateNewDistribution = { type, mode ->
                            navController.navigate("create_edit_distribution?type=${type.name}&mode=${mode.name}")
                        },
                        onOpenDistributionDetail = { distId ->
                            navController.navigate("distribution_detail/$distId")
                        },
                        onFullscreenOverlayChanged = { isOpen ->
                            isDistributionFullscreen = isOpen
                        }
                    )
                }

                // Streaks & Plans Journey Screen
                composable(BottomNavItem.Streaks.route) {
                    val lastBookmark by quranViewModel.lastReadBookmark.collectAsState()
                    val bookmarkPage = remember(lastBookmark) {
                        val bm = lastBookmark
                        if (bm != null && bm.surahNumber > 0) {
                            QuranPageMapper.getPageForAyah(
                                surahNumber = bm.surahNumber,
                                ayahNumber = bm.ayahNumber.coerceAtLeast(1),
                                paraNumberHint = if (bm.mode == ReadMode.PARA) bm.id else 1
                            )
                        } else {
                            1
                        }
                    }
                    JourneyScreen(
                        streakViewModel = streakViewModel,
                        currentBookmarkPage = bookmarkPage,
                        onContinueReading = { paraNum, surahNum, ayahNum ->
                            quranViewModel.selectPara(
                                paraNumber = paraNum,
                                targetAyah = ayahNum,
                                targetSurah = surahNum
                            )
                            navController.navigate("quran_reader_para/$paraNum")
                        },
                        onOpenSurahReader = { surahNum ->
                            quranViewModel.selectSurah(surahNum)
                            navController.navigate("quran_reader_surah/$surahNum")
                        },
                        onDismissOnboarding = {
                            navigateTopLevelTab(BottomNavItem.Home.route)
                        },
                        onFullscreenOverlayChanged = { isOpen ->
                            isProgressFullscreen = isOpen
                        },
                        onCharcoalStateChanged = { isCharcoal ->
                            isProgressOnCharcoal = isCharcoal
                        }
                    )
                }

                // Distribution Detail Screen
                composable(
                    route = "distribution_detail/{distId}",
                    arguments = listOf(navArgument("distId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val distId = backStackEntry.arguments?.getLong("distId") ?: 0L
                    DistributionDetailScreen(
                        distributionId = distId,
                        distributionViewModel = distributionViewModel,
                        onEditDistribution = { idToEdit ->
                            navController.navigate("create_edit_distribution?distId=$idToEdit")
                        },
                        onOpenParaReader = { paraNum ->
                            quranViewModel.selectPara(paraNum)
                            navController.navigate("quran_reader_para/$paraNum")
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                // Create or Edit Distribution Screen
                composable(
                    route = "create_edit_distribution?distId={distId}&type={type}&mode={mode}",
                    arguments = listOf(
                        navArgument("distId") {
                            type = NavType.LongType
                            defaultValue = -1L
                        },
                        navArgument("type") {
                            type = NavType.StringType
                            defaultValue = "KHATM"
                        },
                        navArgument("mode") {
                            type = NavType.StringType
                            defaultValue = "LOCAL"
                        }
                    )
                ) { backStackEntry ->
                    val rawDistId = backStackEntry.arguments?.getLong("distId") ?: -1L
                    val distId = if (rawDistId == -1L) null else rawDistId
                    val typeString = backStackEntry.arguments?.getString("type") ?: "KHATM"
                    val modeString = backStackEntry.arguments?.getString("mode") ?: "LOCAL"
                    val initialType = try {
                        DistributionType.valueOf(typeString)
                    } catch (e: Exception) {
                        DistributionType.KHATM
                    }
                    val initialMode = try {
                        RoomMode.valueOf(modeString)
                    } catch (e: Exception) {
                        RoomMode.LOCAL
                    }

                    CreateEditDistributionScreen(
                        distributionId = distId,
                        initialType = initialType,
                        initialRoomMode = initialMode,
                        distributionViewModel = distributionViewModel,
                        onSaved = { savedId ->
                            navController.popBackStack()
                            navController.navigate("distribution_detail/$savedId") {
                                launchSingleTop = true
                            }
                        },
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }

            if (showSettingsSheet) {
                SettingsSheet(
                    isDarkTheme = isDarkTheme,
                    onToggleDarkTheme = { quranViewModel.setDarkTheme(it) },
                    onDismiss = { showSettingsSheet = false },
                    quranViewModel = quranViewModel,
                    distributionViewModel = distributionViewModel
                )
            }
        }
    }
}
