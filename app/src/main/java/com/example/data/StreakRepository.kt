package com.example.data

import com.example.data.db.DailyLogEntity
import com.example.data.db.PlanType
import com.example.data.db.ReadingPlanEntity
import com.example.data.db.StreakCircleEntity
import com.example.data.db.StreakCircleMemberEntity
import com.example.data.db.StreakDao
import com.example.data.db.StreakEntity
import com.example.data.quran.QuranPageMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.random.Random

sealed interface FreezeShieldAdResult {
    data class Progress(val adsWatched: Int, val adsNeeded: Int = 3) : FreezeShieldAdResult
    data object ShieldEarned : FreezeShieldAdResult
    data class CooldownActive(val daysRemaining: Long) : FreezeShieldAdResult
}

class StreakRepository(private val dao: StreakDao) {

    val streakFlow: Flow<StreakEntity?> = dao.getStreak()
    val activePlansFlow: Flow<List<ReadingPlanEntity>> = dao.getActivePlans()
    val allPlansFlow: Flow<List<ReadingPlanEntity>> = dao.getAllPlans()
    val streakCirclesFlow: Flow<List<StreakCircleEntity>> = dao.getAllCircles()
    val allCircleMembersFlow: Flow<List<StreakCircleMemberEntity>> = dao.getAllCircleMembers()
    val recentDailyLogsFlow: Flow<List<DailyLogEntity>> = dao.getRecentLogs(30)

    fun getTodayKey(): String = LocalDate.now().toString()
    fun getYesterdayKey(): String = LocalDate.now().minusDays(1).toString()

    suspend fun getOrCreateStreak(): StreakEntity {
        val existing = dao.getStreakDirect()
        if (existing != null) return existing
        val defaultStreak = StreakEntity(id = 1L)
        dao.insertOrUpdateStreak(defaultStreak)
        return defaultStreak
    }

    /**
     * Opts the user into the daily streak and habit journey.
     * Starts with today's daily log initialized.
     */
    suspend fun optInToStreak(dailyTargetAyahs: Int = 10): StreakEntity {
        val current = getOrCreateStreak()
        val today = getTodayKey()
        val updated = current.copy(
            isOptedIn = true,
            hasDismissedStreakOnboarding = true,
            consecutiveMissedDays = 0
        )
        dao.insertOrUpdateStreak(updated)

        val todayLog = dao.getDailyLogDirect(today)
        if (todayLog == null) {
            dao.insertOrUpdateDailyLog(
                DailyLogEntity(
                    dateKey = today,
                    ayahsReadCount = 0,
                    targetAyahsCount = dailyTargetAyahs,
                    pagesReadCount = 0,
                    targetPagesCount = 10
                )
            )
        }
        return updated
    }

    suspend fun dismissStreakOnboarding() {
        val current = getOrCreateStreak()
        dao.insertOrUpdateStreak(
            current.copy(
                isOptedIn = false,
                hasDismissedStreakOnboarding = true
            )
        )
    }

    suspend fun reopenStreakOnboarding() {
        val current = getOrCreateStreak()
        if (!current.isOptedIn && current.hasDismissedStreakOnboarding) {
            dao.insertOrUpdateStreak(current.copy(hasDismissedStreakOnboarding = false))
        }
    }

    /**
     * Evaluates daily streak continuity, rollover, and daily reset of circle members' today status.
     */
    suspend fun evaluateStreakRollover(): StreakEntity {
        val streak = getOrCreateStreak()
        val today = LocalDate.now()
        val todayKey = today.toString()

        // Reset circle member daily counters if their lastCompletedDate is not today
        val allCircles = dao.getAllCircles().firstOrNull().orEmpty()
        for (circle in allCircles) {
            val members = dao.getMembersForCircleDirect(circle.id)
            for (member in members) {
                if (member.lastCompletedDate != todayKey && (member.hasCompletedToday || member.pagesReadToday > 0)) {
                    dao.updateMember(
                        member.copy(
                            hasCompletedToday = false,
                            pagesReadToday = 0
                        )
                    )
                }
            }
        }

        if (!streak.isOptedIn) return streak
        val lastDateStr = streak.lastCompletedDate

        if (lastDateStr.isBlank()) {
            return streak
        }

        val lastDate = try {
            LocalDate.parse(lastDateStr)
        } catch (_: Exception) {
            return streak
        }

        val daysBetween = ChronoUnit.DAYS.between(lastDate, today)

        if (daysBetween <= 1) {
            return streak
        }

        if (daysBetween == 2L) {
            val canUseShield = streak.availableFreezeShields > 0 && canUseFreezeShieldCooldown(streak.lastFreezeShieldUsedDate)
            if (canUseShield) {
                val updated = streak.copy(
                    availableFreezeShields = (streak.availableFreezeShields - 1).coerceAtLeast(0),
                    lastFreezeShieldUsedDate = getYesterdayKey(),
                    consecutiveMissedDays = 1
                )
                dao.insertOrUpdateStreak(updated)
                dao.insertOrUpdateDailyLog(
                    DailyLogEntity(
                        dateKey = getYesterdayKey(),
                        hasMetGoal = true,
                        freezeShieldUsed = true,
                        completedTimestamp = System.currentTimeMillis()
                    )
                )
                return updated
            } else {
                val updated = streak.copy(consecutiveMissedDays = 1)
                dao.insertOrUpdateStreak(updated)
                return updated
            }
        }

        val resetStreak = streak.copy(
            currentStreakDays = 0,
            consecutiveMissedDays = daysBetween.toInt()
        )
        dao.insertOrUpdateStreak(resetStreak)
        return resetStreak
    }

    fun canUseFreezeShieldCooldown(lastUsedDateStr: String): Boolean {
        if (lastUsedDateStr.isBlank()) return true
        val lastUsed = try {
            LocalDate.parse(lastUsedDateStr)
        } catch (_: Exception) {
            return true
        }
        val daysSinceLastUse = ChronoUnit.DAYS.between(lastUsed, LocalDate.now())
        return daysSinceLastUse >= 7
    }

    fun getDaysUntilNextFreezeShieldAvailable(lastUsedDateStr: String): Long {
        if (lastUsedDateStr.isBlank()) return 0
        val lastUsed = try {
            LocalDate.parse(lastUsedDateStr)
        } catch (_: Exception) {
            return 0
        }
        val daysSinceLastUse = ChronoUnit.DAYS.between(lastUsed, LocalDate.now())
        return (7 - daysSinceLastUse).coerceAtLeast(0)
    }

    suspend fun recordAdWatchedForFreezeShield(): FreezeShieldAdResult {
        val streak = getOrCreateStreak()
        val cooldownDays = getDaysUntilNextFreezeShieldAvailable(streak.lastFreezeShieldUsedDate)
        if (cooldownDays > 0) {
            return FreezeShieldAdResult.CooldownActive(cooldownDays)
        }

        if (streak.availableFreezeShields >= 1) {
            return FreezeShieldAdResult.ShieldEarned
        }

        val nextCount = streak.pendingAdViewsForNextShield + 1
        return if (nextCount >= 3) {
            val updated = streak.copy(
                availableFreezeShields = 1,
                pendingAdViewsForNextShield = 0
            )
            dao.insertOrUpdateStreak(updated)
            FreezeShieldAdResult.ShieldEarned
        } else {
            val updated = streak.copy(pendingAdViewsForNextShield = nextCount)
            dao.insertOrUpdateStreak(updated)
            FreezeShieldAdResult.Progress(adsWatched = nextCount, adsNeeded = 3)
        }
    }

    /**
     * Automatically called from QuranReaderScreen as the user recites Ayahs & reads pages.
     * Accurately tracks distinct Madinah Mushaf pages read today, distinct Ayahs read today,
     * persists last bookmark position, and syncs live across Home, Streak, Plans, and Circles.
     */
    suspend fun recordAyahRecitation(
        ayahsIncrement: Int = 1,
        surahNumber: Int,
        ayahNumber: Int,
        paraNumber: Int,
        surahName: String,
        explicitPageNumber: Int? = null
    ): Boolean {
        val today = getTodayKey()
        val streak = getOrCreateStreak()
        val pageNumber = (explicitPageNumber ?: QuranPageMapper.getPageForAyah(surahNumber, ayahNumber, paraNumber))
            .coerceIn(1, 604)

        // Persist last read position on streak
        val updatedStreakPosition = streak.copy(
            lastReadSurah = surahNumber,
            lastReadAyah = ayahNumber,
            lastReadPara = paraNumber,
            lastReadSurahName = surahName,
            lastReadTimestamp = System.currentTimeMillis()
        )
        dao.insertOrUpdateStreak(updatedStreakPosition)

        val currentLog = dao.getDailyLogDirect(today) ?: DailyLogEntity(
            dateKey = today,
            ayahsReadCount = 0,
            targetAyahsCount = 10,
            pagesReadCount = 0,
            targetPagesCount = 10
        )

        // Distinct Mushaf pages visited today
        val existingPages = currentLog.readPagesCsv
            .split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .toCollection(LinkedHashSet())
        val isNewPageToday = existingPages.add(pageNumber)
        val newPagesCount = existingPages.size
        val newPagesCsv = existingPages.joinToString(",")

        // Distinct Ayahs read today so scrolling back and forth doesn't artificially inflate counts
        val ayahKey = "$surahNumber:$ayahNumber"
        val existingAyahKeys = currentLog.readAyahKeysCsv
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toCollection(LinkedHashSet())
        val isNewAyahToday = existingAyahKeys.add(ayahKey)
        val newAyahCount = if (isNewAyahToday) {
            maxOf(currentLog.ayahsReadCount + ayahsIncrement, existingAyahKeys.size)
        } else {
            currentLog.ayahsReadCount
        }
        // Keep stored ayah keys bounded cleanly
        val newAyahKeysCsv = existingAyahKeys.toList().takeLast(300).joinToString(",")

        val wasAlreadyMet = currentLog.hasMetGoal
        // Completing at least 1 Mushaf page today counts as completing today's streak!
        val isNowMet = newPagesCount >= 1 || newAyahCount >= currentLog.targetAyahsCount

        val updatedLog = currentLog.copy(
            ayahsReadCount = newAyahCount,
            pagesReadCount = newPagesCount,
            readPagesCsv = newPagesCsv,
            readAyahKeysCsv = newAyahKeysCsv,
            hasMetGoal = isNowMet || wasAlreadyMet,
            completedTimestamp = if (!wasAlreadyMet && isNowMet) System.currentTimeMillis() else currentLog.completedTimestamp
        )
        dao.insertOrUpdateDailyLog(updatedLog)

        var justAchievedGoal = false
        var effectiveStreakDays = streak.currentStreakDays
        val hasExplicitlyDeclinedStreak = !streak.isOptedIn && streak.hasDismissedStreakOnboarding

        if (!wasAlreadyMet && isNowMet && !hasExplicitlyDeclinedStreak) {
            justAchievedGoal = true
            val alreadyIncrementedToday = streak.lastCompletedDate == today
            val nextStreak = if (alreadyIncrementedToday) {
                streak.currentStreakDays.coerceAtLeast(1)
            } else {
                streak.currentStreakDays + 1
            }
            effectiveStreakDays = nextStreak
            val maxStreak = nextStreak.coerceAtLeast(streak.longestStreakDays)
            val updatedStreak = updatedStreakPosition.copy(
                isOptedIn = true,
                hasDismissedStreakOnboarding = true,
                currentStreakDays = nextStreak,
                longestStreakDays = maxStreak,
                lastCompletedDate = today,
                consecutiveMissedDays = 0
            )
            dao.insertOrUpdateStreak(updatedStreak)
        }

        // Update active reading plans (increment completedDaysCount once per day, and track pages read in Finish Quran plans)
        val activePlans = dao.getActivePlans().firstOrNull().orEmpty()
        for (plan in activePlans) {
            val isFinishPlan = plan.planType == PlanType.FINISH_QURAN || plan.planType == PlanType.PARA_RANGE_TARGET
            val isSameSurahMemorize = plan.planType == PlanType.MEMORIZE &&
                (plan.targetSurahNumber == null || plan.targetSurahNumber == surahNumber)

            var updatedPlan = plan
            var changed = false

            if (isFinishPlan && isNewPageToday) {
                updatedPlan = updatedPlan.copy(totalPagesReadInPlan = updatedPlan.totalPagesReadInPlan + 1)
                changed = true
            }

            if (updatedPlan.lastProgressDate != today && (isFinishPlan || isSameSurahMemorize)) {
                val nextDays = updatedPlan.completedDaysCount + 1
                val isPlanDone = nextDays >= updatedPlan.totalDaysCount
                updatedPlan = updatedPlan.copy(
                    completedDaysCount = nextDays,
                    lastProgressDate = today,
                    isCompleted = isPlanDone
                )
                changed = true
            }

            if (changed) {
                dao.updatePlan(updatedPlan)
            }
        }

        // Sync live pagesReadToday & completion across ALL Streak Circles for the current user
        val allCircles = dao.getAllCircles().firstOrNull().orEmpty()
        for (circle in allCircles) {
            val members = dao.getMembersForCircleDirect(circle.id)
            val currentUserMember = members.firstOrNull { it.isCurrentUser } ?: members.firstOrNull { it.isHost }
            if (currentUserMember != null) {
                val didUpdate = currentUserMember.pagesReadToday != newPagesCount ||
                    !currentUserMember.hasCompletedToday ||
                    currentUserMember.lastCompletedDate != today ||
                    currentUserMember.currentStreakDays != effectiveStreakDays

                if (didUpdate) {
                    dao.updateMember(
                        currentUserMember.copy(
                            hasCompletedToday = newPagesCount > 0 || isNowMet,
                            pagesReadToday = newPagesCount,
                            currentStreakDays = effectiveStreakDays.coerceAtLeast(1),
                            lastCompletedDate = today,
                            isCurrentUser = true
                        )
                    )
                }
            }
            if (justAchievedGoal && circle.lastEvaluatedDate != today) {
                dao.updateCircle(
                    circle.copy(
                        collectiveStreakDays = (circle.collectiveStreakDays + 1).coerceAtLeast(1),
                        lastEvaluatedDate = today
                    )
                )
            }
        }

        return justAchievedGoal
    }

    // --- Reading Plans ---
    suspend fun createFinishQuranPlan(
        durationDays: Int,
        startPage: Int = 1,
        dailyReminderEnabled: Boolean = true
    ): Long {
        val cleanDays = durationDays.coerceIn(7, 365)
        val dailyPages = ceil(604.0 / cleanDays).toInt().coerceAtLeast(1)
        val today = getTodayKey()
        val plan = ReadingPlanEntity(
            title = "Finish the Quran",
            planType = PlanType.FINISH_QURAN,
            dailyTargetAyahs = dailyPages * 15,
            dailyTargetPages = dailyPages,
            startPage = startPage.coerceIn(1, 604),
            totalPagesReadInPlan = 0,
            dailyReminderEnabled = dailyReminderEnabled,
            startDate = today,
            targetCompletionDate = LocalDate.now().plusDays(cleanDays.toLong()).toString(),
            totalDaysCount = cleanDays,
            completedDaysCount = 0,
            isActive = true
        )
        val planId = dao.insertPlan(plan)
        // Update today's log page target to match
        val currentLog = dao.getDailyLogDirect(today)
        if (currentLog != null) {
            dao.insertOrUpdateDailyLog(currentLog.copy(targetPagesCount = dailyPages))
        }
        return planId
    }

    suspend fun createMemorizePlan(
        title: String,
        surahNumber: Int?,
        versesPerDay: Int,
        totalVerses: Int,
        dailyReminderEnabled: Boolean = true
    ): Long {
        val perDay = versesPerDay.coerceIn(1, 50)
        val totalDays = ceil(totalVerses.toDouble() / perDay).toInt().coerceAtLeast(1)
        val today = getTodayKey()
        val plan = ReadingPlanEntity(
            title = title.trim().ifBlank { "Memorize Surah" },
            planType = PlanType.MEMORIZE,
            dailyTargetAyahs = perDay,
            dailyTargetPages = 1,
            targetSurahNumber = surahNumber,
            dailyReminderEnabled = dailyReminderEnabled,
            startDate = today,
            targetCompletionDate = LocalDate.now().plusDays(totalDays.toLong()).toString(),
            totalDaysCount = totalDays,
            completedDaysCount = 0,
            isActive = true
        )
        return dao.insertPlan(plan)
    }

    suspend fun createWeeklyPlan(
        title: String,
        surahNumber: Int?,
        dayOfWeek: Int, // 1=Mon .. 7=Sun
        totalWeeks: Int = 52,
        dailyReminderEnabled: Boolean = true
    ): Long {
        val cleanWeeks = totalWeeks.coerceIn(4, 104)
        val today = getTodayKey()
        val plan = ReadingPlanEntity(
            title = title.trim().ifBlank { "Weekly Recitation" },
            planType = PlanType.WEEKLY,
            dailyTargetAyahs = 10,
            dailyTargetPages = 1,
            targetSurahNumber = surahNumber,
            weeklyDayOfWeek = dayOfWeek.coerceIn(1, 7),
            dailyReminderEnabled = dailyReminderEnabled,
            startDate = today,
            targetCompletionDate = LocalDate.now().plusWeeks(cleanWeeks.toLong()).toString(),
            totalDaysCount = cleanWeeks,
            completedDaysCount = 0,
            isActive = true
        )
        return dao.insertPlan(plan)
    }

    suspend fun createReadingPlan(
        title: String,
        planType: PlanType,
        dailyTargetAyahs: Int,
        totalDays: Int,
        targetParasCsv: String = "",
        targetSurahNumber: Int? = null
    ): Long {
        val today = getTodayKey()
        val plan = ReadingPlanEntity(
            title = title.trim(),
            planType = planType,
            dailyTargetAyahs = dailyTargetAyahs,
            dailyTargetPages = if (planType == PlanType.FINISH_QURAN) ceil(604.0 / totalDays.coerceAtLeast(1)).toInt() else 10,
            targetParasCsv = targetParasCsv,
            targetSurahNumber = targetSurahNumber,
            startDate = today,
            targetCompletionDate = LocalDate.now().plusDays(totalDays.toLong()).toString(),
            totalDaysCount = totalDays,
            completedDaysCount = 0,
            isActive = true
        )
        val planId = dao.insertPlan(plan)
        optInToStreak(dailyTargetAyahs = dailyTargetAyahs)
        return planId
    }

    suspend fun markPlanTodayDone(planId: Long) {
        val plan = dao.getPlanByIdDirect(planId) ?: return
        val today = getTodayKey()
        if (plan.lastProgressDate == today) return
        val nextDays = plan.completedDaysCount + 1
        val isDone = nextDays >= plan.totalDaysCount
        dao.updatePlan(
            plan.copy(
                completedDaysCount = nextDays,
                lastProgressDate = today,
                isCompleted = isDone
            )
        )
    }

    suspend fun deletePlan(planId: Long) {
        dao.deletePlanById(planId)
    }

    // --- Streak Circles (Group Streak Rooms) ---
    suspend fun createStreakCircle(
        name: String,
        hostName: String,
        colorIndex: Int = 0,
        customPasscode: String? = null
    ): Long {
        val cleanName = name.trim().ifBlank { "Reading Circle" }
        val passcode = customPasscode?.trim()?.uppercase()?.takeIf { it.isNotBlank() }
            ?: generateCirclePasscode(cleanName)
        val today = getTodayKey()
        val todayLog = dao.getDailyLogDirect(today)
        val todayPages = todayLog?.pagesReadCount ?: 0
        val hasReadToday = todayPages > 0 || (todayLog?.hasMetGoal == true)
        val streak = getOrCreateStreak()

        val displayHostName = hostName.trim().ifBlank { "You" }
        val circle = StreakCircleEntity(
            name = cleanName,
            roomPasscode = passcode,
            colorIndex = colorIndex.coerceIn(0, 3),
            hostName = displayHostName,
            isOnline = true,
            collectiveStreakDays = if (hasReadToday) 1 else 0,
            lastEvaluatedDate = if (hasReadToday) today else ""
        )
        val circleId = dao.insertCircle(circle)
        dao.insertMember(
            StreakCircleMemberEntity(
                circleId = circleId,
                memberName = displayHostName,
                currentStreakDays = streak.currentStreakDays,
                pagesReadToday = todayPages,
                hasCompletedToday = hasReadToday,
                lastCompletedDate = if (hasReadToday) today else "",
                isHost = true,
                isCurrentUser = true
            )
        )
        return circleId
    }

    fun generateCirclePasscode(name: String): String {
        val letters = name.uppercase().filter { it in 'A'..'Z' }.take(3).padEnd(3, 'C')
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val suffix = (1..4).map { chars[Random.nextInt(chars.length)] }.joinToString("")
        return "$letters-$suffix"
    }

    suspend fun joinStreakCircle(passcode: String, memberName: String): Long? {
        val cleanCode = passcode.trim().uppercase()
        if (cleanCode.isBlank()) return null
        val today = getTodayKey()
        val todayLog = dao.getDailyLogDirect(today)
        val todayPages = todayLog?.pagesReadCount ?: 0
        val hasReadToday = todayPages > 0 || (todayLog?.hasMetGoal == true)
        val streak = getOrCreateStreak()

        val existing = dao.getCircleByPasscode(cleanCode)
        val circleId = existing?.id ?: run {
            val newCircle = StreakCircleEntity(
                name = "Circle $cleanCode",
                roomPasscode = cleanCode,
                colorIndex = 0,
                hostName = memberName.trim().ifBlank { "Companion" },
                isOnline = true,
                collectiveStreakDays = if (hasReadToday) 1 else 0
            )
            dao.insertCircle(newCircle)
        }

        val cleanMemberName = memberName.trim().ifBlank { "You" }
        val members = dao.getMembersForCircleDirect(circleId)
        val existingUser = members.firstOrNull {
            it.isCurrentUser || it.memberName.equals(cleanMemberName, ignoreCase = true)
        }
        if (existingUser == null) {
            dao.insertMember(
                StreakCircleMemberEntity(
                    circleId = circleId,
                    memberName = cleanMemberName,
                    currentStreakDays = streak.currentStreakDays,
                    pagesReadToday = todayPages,
                    hasCompletedToday = hasReadToday,
                    lastCompletedDate = if (hasReadToday) today else "",
                    isHost = existing == null,
                    isCurrentUser = true
                )
            )
        } else {
            dao.updateMember(
                existingUser.copy(
                    pagesReadToday = todayPages,
                    hasCompletedToday = hasReadToday,
                    lastCompletedDate = if (hasReadToday) today else existingUser.lastCompletedDate,
                    isCurrentUser = true
                )
            )
        }
        return circleId
    }

    suspend fun nudgeCircleMember(memberId: Long) {
        val member = dao.getMemberByIdDirect(memberId) ?: return
        dao.updateMember(member.copy(lastNudgedTimestamp = System.currentTimeMillis()))
    }

    suspend fun deleteCircle(circleId: Long) {
        dao.deleteCircleById(circleId)
    }

    fun getMembersForCircle(circleId: Long): Flow<List<StreakCircleMemberEntity>> =
        dao.getMembersForCircle(circleId)

    fun formatStreakCircleShareText(
        circle: StreakCircleEntity,
        members: List<StreakCircleMemberEntity>
    ): String {
        val sb = StringBuilder()
        sb.append("${circle.name} · Reading Circle\n")
        sb.append("Invite Code: ${circle.roomPasscode}\n\n")
        members.forEach { m ->
            val label = if (m.isCurrentUser) "${m.memberName} (You)" else m.memberName
            val status = if (m.hasCompletedToday) "${m.pagesReadToday} pages today · Done" else "Not yet today"
            sb.append("• $label: $status\n")
        }
        sb.append("\nJoin my reading circle on Quran Baanto using invite code: ${circle.roomPasscode}")
        return sb.toString().trim()
    }
}
