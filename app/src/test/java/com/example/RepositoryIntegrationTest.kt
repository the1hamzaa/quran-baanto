package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.DistributionRepository
import com.example.data.StreakRepository
import com.example.data.db.AppDatabase
import com.example.data.db.DistributionType
import com.example.data.db.RoomMode
import com.example.data.quran.QuranPageMapper
import com.example.domain.ParticipantAllocation
import com.example.domain.RotationEngine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RepositoryIntegrationTest {

    private lateinit var db: AppDatabase
    private lateinit var distRepo: DistributionRepository
    private lateinit var streakRepo: StreakRepository

    @Before
    fun setup() {
        com.example.data.firebase.FirebaseCoreManager.setCloudDisabledForTesting(true)
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        distRepo = DistributionRepository(db.distributionDao())
        streakRepo = StreakRepository(db.streakDao())
    }

    @After
    fun tearDown() {
        db.close()
        com.example.data.firebase.FirebaseCoreManager.setCloudDisabledForTesting(false)
    }

    @Test
    fun editDistributionPreservesHostPasscodeAndCoAdminRoles() = runTest {
        val roomId = distRepo.saveDistribution(
            name = "Family Monthly",
            type = DistributionType.KHATM,
            roomMode = RoomMode.ONLINE,
            totalTarget = 30,
            allocations = listOf(
                ParticipantAllocation(name = "HostUser", count = 15, displayOrder = 1),
                ParticipantAllocation(name = "ElderUser", count = 15, displayOrder = 2)
            )
        )
        val created = db.distributionDao().getDistributionById(roomId)!!
        assertTrue(created.isHost)
        val originalPasscode = created.roomPasscode!!
        assertTrue(originalPasscode.startsWith("QBM-"))

        // Grant Co-Admin to ElderUser
        distRepo.setMemberAdminRole(roomId, "ElderUser", isGuest = false, isAdmin = true)
        val beforeEdit = db.distributionDao().getParticipantsForDistributionList(roomId)
        val elder = beforeEdit.first { it.name == "ElderUser" }
        assertTrue(elder.isAdmin)

        // Edit distribution title preserving existing participant ids
        distRepo.saveDistribution(
            name = "Family Monthly Updated",
            type = DistributionType.KHATM,
            roomMode = RoomMode.ONLINE,
            totalTarget = 30,
            allocations = beforeEdit.mapIndexed { idx, p ->
                ParticipantAllocation(
                    id = p.id,
                    name = p.name,
                    count = p.allocatedCount,
                    displayOrder = idx + 1
                )
            },
            existingId = roomId
        )

        val afterEditDist = db.distributionDao().getDistributionById(roomId)!!
        val afterEditParts = db.distributionDao().getParticipantsForDistributionList(roomId)
        assertEquals("Family Monthly Updated", afterEditDist.name)
        assertTrue(afterEditDist.isHost)
        assertEquals(originalPasscode, afterEditDist.roomPasscode)
        assertTrue(afterEditParts.first { it.name == "ElderUser" }.isAdmin)
    }

    @Test
    fun joinAndClaimSpecificParasSplitsBlocksAndRotatesAccurately() = runTest {
        val roomId = distRepo.saveDistribution(
            name = "Open Monthly Room",
            type = DistributionType.KHATM,
            roomMode = RoomMode.ONLINE,
            totalTarget = 30,
            allocations = emptyList()
        )
        val dist = db.distributionDao().getDistributionById(roomId)!!

        val success = distRepo.joinAndClaimSpecificParas(
            distributionId = roomId,
            memberName = "Reader",
            selectedParas = listOf(4, 5, 9),
            isGuest = false,
            addedByProfileName = "Reader"
        )
        assertTrue(success)

        val parts = db.distributionDao().getParticipantsForDistributionList(roomId)
        assertEquals(2, parts.size)
        assertEquals(4, parts[0].specificParaNumber)
        assertEquals(2, parts[0].allocatedCount)
        assertEquals(9, parts[1].specificParaNumber)
        assertEquals(1, parts[1].allocatedCount)

        val allocs = parts.map {
            ParticipantAllocation(
                id = it.id,
                name = it.name,
                count = it.allocatedCount,
                displayOrder = it.specificParaNumber ?: it.displayOrder,
                specificParas = if (it.specificParaNumber != null) listOf(it.specificParaNumber) else emptyList()
            )
        }
        val cal = Calendar.getInstance()
        val thisMonth = RotationEngine.calculateAssignments(
            allocs, dist.startYear, dist.startMonth, dist.startYear, dist.startMonth
        ).flatMap { it.assignedParas }
        assertEquals(listOf(4, 5, 9), thisMonth)

        cal.set(dist.startYear, dist.startMonth - 1, 1)
        cal.add(Calendar.MONTH, 1)
        val nextMonth = RotationEngine.calculateAssignments(
            allocs,
            dist.startYear,
            dist.startMonth,
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1
        ).flatMap { it.assignedParas }
        assertEquals(listOf(6, 7, 10), nextMonth)
    }

    @Test
    fun oneTimeKhatmCompleting30ParasAutoCleansAndClosesForever() = runTest {
        val allocs = (1..30).map { i ->
            ParticipantAllocation(
                name = "Member $i",
                count = 1,
                displayOrder = i,
                specificParas = listOf(i)
            )
        }
        val roomId = distRepo.saveDistribution(
            name = "One-Time Khatm",
            type = DistributionType.ONE_TIME_KHATM,
            roomMode = RoomMode.LOCAL,
            totalTarget = 30,
            allocations = allocs
        )

        val participants = db.distributionDao().getParticipantsForDistributionList(roomId)
        assertEquals(30, participants.size)

        for (i in 0 until 29) {
            val res = distRepo.toggleParticipantCompletion(roomId, participants[i].id, 2026, 1)
            assertNotNull(res)
            assertFalse(res!!.autoCleanedUpOneTimeKhatm)
        }
        assertFalse(db.distributionDao().getDistributionById(roomId)!!.isClosedForever)

        val finalRes = distRepo.toggleParticipantCompletion(roomId, participants[29].id, 2026, 1)!!
        assertTrue(finalRes.justCompletedFullDistribution)
        assertTrue(finalRes.autoCleanedUpOneTimeKhatm)

        val closedDist = db.distributionDao().getDistributionById(roomId)!!
        assertTrue(closedDist.isClosedForever)
        assertTrue(closedDist.closedSummaryText?.contains("30/30 Paras Completed") == true)
        assertTrue(db.distributionDao().getParticipantsForDistributionList(roomId).isEmpty())
    }

    @Test
    fun amalReopenIncrementsRoundGeneratesNewPasscodeAndResetsProgress() = runTest {
        val roomId = distRepo.saveDistribution(
            name = "Salawat Circle",
            type = DistributionType.AMAL,
            roomMode = RoomMode.ONLINE,
            totalTarget = 100,
            allocations = listOf(ParticipantAllocation(name = "Host", count = 100, displayOrder = 1))
        )
        val initialDist = db.distributionDao().getDistributionById(roomId)!!
        val initialParticipant = db.distributionDao().getParticipantsForDistributionList(roomId).first()

        distRepo.updateAmalCompletedCount(roomId, initialParticipant.id, 100)
        assertTrue(db.distributionDao().getParticipantsForDistributionList(roomId).first().isCompleted)

        val newPasscode = distRepo.reopenAmalRoom(roomId, keepMembersAndResetTicks = true)
        assertNotNull(newPasscode)
        assertNotEquals(initialDist.roomPasscode, newPasscode)

        val reopenedDist = db.distributionDao().getDistributionById(roomId)!!
        val resetParticipant = db.distributionDao().getParticipantsForDistributionList(roomId).first()
        assertEquals(2, reopenedDist.roundNumber)
        assertFalse(resetParticipant.isCompleted)
        assertEquals(0, resetParticipant.completedCount)
    }

    @Test
    fun quranPageTrackingCountsDistinctPagesAcrossStreakPlanAndCircle() = runTest {
        val planId = streakRepo.createFinishQuranPlan(durationDays = 60, startPage = 1)
        val circleId = streakRepo.createStreakCircle(name = "Family Circle", hostName = "Zaid")

        // Page 2 (Al-Baqarah 2:1)
        assertEquals(2, QuranPageMapper.getPageForAyah(2, 1))
        streakRepo.recordAyahRecitation(
            surahNumber = 2,
            ayahNumber = 1,
            paraNumber = 1,
            surahName = "Al-Baqarah"
        )
        // Same page 2 again (Al-Baqarah 2:2) -> must NOT double count pagesReadCount
        streakRepo.recordAyahRecitation(
            surahNumber = 2,
            ayahNumber = 2,
            paraNumber = 1,
            surahName = "Al-Baqarah"
        )
        // Different page 50 (Ali 'Imran 3:1) -> pagesReadCount increments to 2
        streakRepo.recordAyahRecitation(
            surahNumber = 3,
            ayahNumber = 1,
            paraNumber = 3,
            surahName = "Ali 'Imran"
        )

        val todayLog = streakRepo.recentDailyLogsFlow.first().first()
        val streak = streakRepo.streakFlow.first()!!
        val plan = streakRepo.activePlansFlow.first().first { it.id == planId }
        val circleMember = streakRepo.getMembersForCircle(circleId).first().first()

        assertEquals(2, todayLog.pagesReadCount)
        assertEquals(3, todayLog.ayahsReadCount)
        assertEquals(1, streak.currentStreakDays)
        assertEquals(3, streak.lastReadSurah)
        assertEquals("Ali 'Imran", streak.lastReadSurahName)
        assertEquals(2, plan.totalPagesReadInPlan)
        assertEquals(1, plan.completedDaysCount)
        assertEquals(2, circleMember.pagesReadToday)
        assertTrue(circleMember.hasCompletedToday)
    }

    @Test
    fun maybeLaterDeclinesStreakAndReadingPagesDoesNotForceStartStreak() = runTest {
        streakRepo.dismissStreakOnboarding()
        val declined = streakRepo.streakFlow.first()!!
        assertFalse(declined.isOptedIn)
        assertTrue(declined.hasDismissedStreakOnboarding)

        streakRepo.recordAyahRecitation(
            surahNumber = 2,
            ayahNumber = 1,
            paraNumber = 1,
            surahName = "Al-Baqarah"
        )

        val log = streakRepo.recentDailyLogsFlow.first().first()
        val afterRead = streakRepo.streakFlow.first()!!
        assertEquals(1, log.pagesReadCount)
        assertFalse("Reading pages after Maybe later must not force-start streak", afterRead.isOptedIn)
        assertEquals(0, afterRead.currentStreakDays)
    }
}
