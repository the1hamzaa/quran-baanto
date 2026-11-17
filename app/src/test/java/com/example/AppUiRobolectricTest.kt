package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.db.DistributionEntity
import com.example.data.db.DistributionType
import com.example.data.db.ParticipantEntity
import com.example.data.db.RoomMode
import com.example.data.preferences.UserPreferencesDataStore
import com.example.ui.distribution.DistributionListScreen
import com.example.ui.home.HomeScreen
import com.example.ui.quran.QuranListScreen
import com.example.ui.streak.JourneyScreen
import com.example.ui.theme.QuranBaantoTheme
import com.example.ui.viewmodel.DistributionViewModel
import com.example.ui.viewmodel.QuranViewModel
import com.example.ui.viewmodel.StreakViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w412dp-h915dp-xxhdpi")
class AppUiRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var app: Application
    private lateinit var db: AppDatabase

    @Before
    fun setup() {
        com.example.data.firebase.FirebaseCoreManager.setCloudDisabledForTesting(true)
        app = ApplicationProvider.getApplicationContext<Context>() as Application
        db = AppDatabase.getDatabase(app)
        runBlocking(Dispatchers.IO) {
            db.clearAllTables()
        }
    }

    @org.junit.After
    fun tearDown() {
        com.example.data.firebase.FirebaseCoreManager.setCloudDisabledForTesting(false)
    }

    @Test
    fun homeScreenEmptyStateShowsRealZeroDataAndNavigatesToStreaks() {
        val distVm = DistributionViewModel(app)
        val quranVm = QuranViewModel(app)
        val streakVm = StreakViewModel(app)
        var navigatedToStreaks = false

        composeRule.setContent {
            QuranBaantoTheme(darkTheme = false) {
                HomeScreen(
                    distributionViewModel = distVm,
                    quranViewModel = quranVm,
                    streakViewModel = streakVm,
                    onNavigateToStreaks = { navigatedToStreaks = true },
                    onOpenSettings = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithText("0/10").assertIsDisplayed()
        composeRule.onNodeWithTag("start_streak_cta").assertIsDisplayed()
        assertTrue(composeRule.onAllNodesWithText("Al-Mulk", substring = true).fetchSemanticsNodes().isEmpty())
        assertTrue(composeRule.onAllNodesWithText("14 days", substring = true).fetchSemanticsNodes().isEmpty())

        composeRule.onNodeWithTag("start_streak_cta").performClick()
        composeRule.waitForIdle()
        assertTrue(navigatedToStreaks)
    }

    @Test
    fun quranListScreenDisplaysBothSurahAndJuzTabsAndSwitches() {
        val quranVm = QuranViewModel(app)

        composeRule.setContent {
            QuranBaantoTheme(darkTheme = false) {
                QuranListScreen(
                    quranViewModel = quranVm,
                    onOpenParaReader = {},
                    onOpenSurahReader = {}
                )
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag("quran_tab_surah").assertIsDisplayed()
        composeRule.onNodeWithTag("quran_tab_juz").assertIsDisplayed()
        composeRule.onNodeWithText("Al-Fatihah").assertIsDisplayed()

        composeRule.onNodeWithTag("quran_tab_juz").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Alif Lam Meem").assertIsDisplayed()
    }

    @Test
    fun distributionListShowsAdminPowersOnlyForOnlineHostOrCoAdmin() {
        val dao = db.distributionDao()
        val (localId, joinedNonAdminOnlineId, hostedOnlineId, coAdminOnlineId) = runBlocking(Dispatchers.IO) {
            UserPreferencesDataStore.getInstance(app).setUserProfileName("Hamza")

        val localId = dao.insertFullDistribution(
            DistributionEntity(
                name = "Local Family Khatm",
                type = DistributionType.KHATM,
                roomMode = RoomMode.LOCAL,
                startYear = 2026,
                startMonth = 1,
                isHost = true
            ),
            listOf(ParticipantEntity(distributionId = 0, name = "Hamza", allocatedCount = 30, displayOrder = 1))
        )

        val joinedNonAdminOnlineId = dao.insertFullDistribution(
            DistributionEntity(
                name = "Joined Non-Admin Room",
                type = DistributionType.AMAL,
                roomMode = RoomMode.ONLINE,
                roomPasscode = "QBA-11112222",
                totalTarget = 100,
                startYear = 2026,
                startMonth = 1,
                isHost = false
            ),
            listOf(ParticipantEntity(distributionId = 0, name = "Hamza", allocatedCount = 20, displayOrder = 1, isAdmin = false))
        )

        val hostedOnlineId = dao.insertFullDistribution(
            DistributionEntity(
                name = "Hosted Online Room",
                type = DistributionType.KHATM,
                roomMode = RoomMode.ONLINE,
                roomPasscode = "QBM-33334444",
                totalTarget = 30,
                startYear = 2026,
                startMonth = 1,
                isHost = true
            ),
            emptyList()
        )

        val coAdminOnlineId = dao.insertFullDistribution(
            DistributionEntity(
                name = "Co-Admin Online Room",
                type = DistributionType.ONE_TIME_KHATM,
                roomMode = RoomMode.ONLINE,
                roomPasscode = "QBO-55556666",
                totalTarget = 30,
                startYear = 2026,
                startMonth = 1,
                isHost = false
            ),
            listOf(
                ParticipantEntity(
                    distributionId = 0,
                    name = "Hamza",
                    allocatedCount = 1,
                    displayOrder = 1,
                    specificParaNumber = 1,
                    isAdmin = true
                )
            )
        )
        Tuple4(localId, joinedNonAdminOnlineId, hostedOnlineId, coAdminOnlineId)
        }

        val distVm = DistributionViewModel(app)

        composeRule.setContent {
            QuranBaantoTheme(darkTheme = false) {
                DistributionListScreen(
                    distributionViewModel = distVm,
                    onCreateNewDistribution = { _, _ -> },
                    onOpenDistributionDetail = {}
                )
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Co-Admin Online Room").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()

        // Rising semicircle action buttons exist & are displayed
        composeRule.onNodeWithTag("rising_join_button").assertIsDisplayed()
        composeRule.onNodeWithTag("rising_create_button").assertIsDisplayed()

        // Admin Powers NEVER shown for Local room or non-admin joined Online room
        composeRule.onNodeWithTag("admin_powers_button_$localId").assertDoesNotExist()
        composeRule.onNodeWithTag("admin_powers_button_$joinedNonAdminOnlineId").assertDoesNotExist()

        // Admin Powers SHOWN for hosted Online room and Co-Admin Online room
        composeRule.onNodeWithTag("admin_powers_button_$hostedOnlineId").assertIsDisplayed()
        composeRule.onNodeWithTag("admin_powers_button_$coAdminOnlineId").assertIsDisplayed()
    }

    @Test
    fun maybeLaterButtonDismissesStartStreakAndKeepsUserOptedOut() {
        val streakVm = StreakViewModel(app)
        var dismissedToHome = false

        composeRule.setContent {
            QuranBaantoTheme(darkTheme = false) {
                JourneyScreen(
                    streakViewModel = streakVm,
                    onContinueReading = { _, _, _ -> },
                    onDismissOnboarding = { dismissedToHome = true }
                )
            }
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Begin a streak.").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("maybe_later_button").assertIsDisplayed()
        composeRule.onNodeWithTag("maybe_later_button").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            dismissedToHome &&
                composeRule.onAllNodesWithText("No streak active").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("streak_page_begin_button").assertIsDisplayed()

        val streak = runBlocking(Dispatchers.IO) { db.streakDao().getStreakDirect() }
        assertTrue(dismissedToHome)
        assertTrue(streak?.hasDismissedStreakOnboarding == true)
        assertFalse(streak?.isOptedIn == true)
    }
}

private data class Tuple4(val a: Long, val b: Long, val c: Long, val d: Long)
