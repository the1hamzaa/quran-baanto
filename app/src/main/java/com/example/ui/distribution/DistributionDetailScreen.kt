package com.example.ui.distribution

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.DistributionEntity
import com.example.data.db.DistributionType
import com.example.data.db.ParticipantEntity
import com.example.ui.components.WaveDivider
import com.example.ui.theme.FrauncesFontFamily
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.QuranBaantoDesignSystem
import com.example.ui.viewmodel.DistributionViewModel
import com.example.util.KhatmNotificationHelper
import java.util.Calendar

private enum class OneTimeParaFilter {
    ALL,
    AVAILABLE,
    CLAIMED
}

/**
 * PRODUCTION-GRADE REDESIGNED DISTRIBUTION DETAIL SCREEN
 *
 * Implements exact Organic Modernism designs from reference screenshots:
 * - Online One-Time Khatm: Pine arc header, 30 arch tiles, wave track, claim/complete pills, filters
 * - Online Monthly Khatm: Pine arc header, 220dp beaded circular ring, month switcher, your para
 * - Online Monthly Choose Paras: 30 arch tiles with taken/yours/open states, confirm pill
 * - Online Amal: Large Fraunces recitation counter, wave track, pledge pill, + counter, round reopening
 * - Local Rooms: Exact local designs with back/edit/delete topbar, segmented allocation bar, numbered roster
 * - Admin Powers: ONLY accessible on online rooms created by current user (dist.isOnline && dist.isHost)
 * - Delete Room: Flow connects to DeleteRoomScreen with confirmation text requirement
 * - ZERO mock placeholder names: real database records and empty states only
 */
@Composable
fun DistributionDetailScreen(
    distributionId: Long,
    distributionViewModel: DistributionViewModel,
    onEditDistribution: (Long) -> Unit,
    onOpenParaReader: (Int) -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = QuranBaantoDesignSystem.colors

    val activeState by distributionViewModel.activeDistributionState.collectAsState()
    val savedProfileName by distributionViewModel.userProfileName.collectAsState()

    var showShareDialog by remember { mutableStateOf(false) }
    var showQrCodeDialog by remember { mutableStateOf(false) }
    var showDueReminderDialog by remember { mutableStateOf(false) }
    var activeDueScheduleLabel by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showClaimModal by remember { mutableStateOf(false) }
    var selectedParaForClaim by remember { mutableStateOf<Int?>(null) }
    var showJoinSlotDialog by remember { mutableStateOf(false) }
    var showReopenAmalDialog by remember { mutableStateOf(false) }
    var showAdminPowers by remember { mutableStateOf(false) }
    var isChoosingMonthlyParas by remember { mutableStateOf(false) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        distributionViewModel.completeNotificationOptInPrompt(enabled = granted)
    }

    fun ensureNotificationPermissionRequested() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !KhatmNotificationHelper.hasNotificationPermission(context)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(distributionId) {
        distributionViewModel.selectDistribution(distributionId)
        activeDueScheduleLabel = distributionViewModel.getScheduledDueReminderLabel(distributionId)
    }

    val dist = activeState.distribution

    if (dist == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ArchWindowVector(
                    modifier = Modifier.size(48.dp),
                    outlineColor = colors.gold
                )
                Text(
                    text = "Loading room schedule...",
                    fontFamily = InterFontFamily,
                    fontSize = 14.sp,
                    color = colors.onSurfaceVariant
                )
            }
        }
        return
    }

    // Refresh active reminder label when switching rooms or months
    LaunchedEffect(dist.id, activeState.participants, activeState.selectedYear, activeState.selectedMonth) {
        activeDueScheduleLabel = distributionViewModel.getScheduledDueReminderLabel(dist.id)
    }

    val isCoAdmin = remember(activeState.participants, savedProfileName) {
        val trimmed = savedProfileName.trim()
        trimmed.isNotBlank() && activeState.participants.any {
            it.isAdmin && !it.isGuestEntry && it.name.trim().equals(trimmed, ignoreCase = true)
        }
    }
    val canOpenAdminPowers = dist.isOnline && (dist.isHost || isCoAdmin)

    val detailSubView = when {
        showAdminPowers && canOpenAdminPowers -> "admin"
        showDeleteConfirmation -> "delete"
        isChoosingMonthlyParas && dist.isOnline && dist.type == DistributionType.KHATM -> "choose_paras"
        else -> "main"
    }

    BackHandler(enabled = detailSubView == "main") {
        onBack()
    }

    AnimatedContent(
        targetState = detailSubView,
        transitionSpec = {
            fadeIn(tween(180, easing = FastOutSlowInEasing)) togetherWith
                fadeOut(tween(140, easing = FastOutSlowInEasing))
        },
        label = "distribution_detail_subview",
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) { subView ->
        when (subView) {
            "admin" -> {
                DistributionAdminScreen(
                    distributionId = dist.id,
                    distributionViewModel = distributionViewModel,
                    onBack = { showAdminPowers = false }
                )
            }
            "delete" -> {
                DeleteRoomScreen(
                    roomName = dist.name,
                    memberCount = activeState.participants.size,
                    onConfirmDelete = {
                        distributionViewModel.deleteDistribution(dist.id)
                        onBack()
                    },
                    onCancel = { showDeleteConfirmation = false }
                )
            }
            "choose_paras" -> {
                OnlineMonthlyChooseParasView(
                    dist = dist,
                    activeState = activeState,
                    onConfirm = { chosenParas ->
                        distributionViewModel.joinAndClaimSpecificParas(
                            distributionId = dist.id,
                            memberName = savedProfileName.ifBlank { "You" },
                            selectedParas = chosenParas,
                            isGuest = false
                        )
                        isChoosingMonthlyParas = false
                    },
                    onBack = { isChoosingMonthlyParas = false }
                )
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(colors.background)
                ) {
                    if (dist.isOnline) {
                        OnlineRoomView(
                            dist = dist,
                            activeState = activeState,
                            savedProfileName = savedProfileName,
                            canOpenAdminPowers = canOpenAdminPowers,
                            distributionViewModel = distributionViewModel,
                            onBack = onBack,
                            onShowQrCode = { showQrCodeDialog = true },
                            onOpenDueReminder = {
                                ensureNotificationPermissionRequested()
                                showDueReminderDialog = true
                            },
                            onCopyCode = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Room Code", dist.roomPasscode.orEmpty()))
                                Toast.makeText(context, "Room Code copied", Toast.LENGTH_SHORT).show()
                            },
                            onShareRoom = { showShareDialog = true },
                            onCopyShareText = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Quran Baanto", activeState.shareText))
                                Toast.makeText(context, "Copied schedule to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            onOpenAdminPowers = {
                                if (canOpenAdminPowers) {
                                    showAdminPowers = true
                                }
                            },
                            onClaimOneTimePara = { paraNum ->
                                selectedParaForClaim = paraNum
                                showClaimModal = true
                            },
                            onOpenClaimModal = {
                                selectedParaForClaim = null
                                showClaimModal = true
                            },
                            onOpenJoinSlot = {
                                showJoinSlotDialog = true
                            },
                            onStartChooseParas = {
                                isChoosingMonthlyParas = true
                            },
                            onReopenAmalRound = {
                                showReopenAmalDialog = true
                            },
                            onOpenParaReader = onOpenParaReader
                        )
                    } else {
                        LocalRoomView(
                            dist = dist,
                            activeState = activeState,
                            distributionViewModel = distributionViewModel,
                            onBack = onBack,
                            onEdit = { onEditDistribution(dist.id) },
                            onDelete = { showDeleteConfirmation = true },
                            onShare = { showShareDialog = true },
                            onCopyText = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Quran Baanto", activeState.shareText))
                                Toast.makeText(context, "Copied schedule to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            onOpenParaReader = onOpenParaReader
                        )
                    }
                }
            }
        }
    }

    // Dialog: One-Time Para Claim
    if (showClaimModal && dist.type == DistributionType.ONE_TIME_KHATM) {
        val takenParas = activeState.participants
            .mapNotNull { it.specificParaNumber ?: it.displayOrder }
            .toSet()

        OneTimeParaClaimDialog(
            initialParaNumber = selectedParaForClaim,
            takenParaNumbers = takenParas,
            savedProfileName = savedProfileName,
            onConfirmClaim = { paras, name, isGuest ->
                distributionViewModel.claimMultipleOneTimeParas(
                    distributionId = dist.id,
                    paraNumbers = paras,
                    memberName = name,
                    isGuest = isGuest
                )
                showClaimModal = false
            },
            onDismiss = { showClaimModal = false }
        )
    }

    // Dialog: Join Slot / Pledge (Amal / Monthly)
    if (showJoinSlotDialog) {
        val totalAllocated = activeState.participants.sumOf { it.allocatedCount }
        val remainingCapacity = (dist.totalTarget - totalAllocated).coerceAtLeast(0)

        JoinAndClaimSlotDialog(
            isAmal = dist.type == DistributionType.AMAL,
            remainingCapacity = remainingCapacity,
            savedProfileName = savedProfileName,
            onConfirmClaim = { name, count, isGuest ->
                distributionViewModel.joinAndClaimAllocation(
                    distributionId = dist.id,
                    memberName = name,
                    requestedCount = count,
                    isGuest = isGuest
                )
                showJoinSlotDialog = false
            },
            onDismiss = { showJoinSlotDialog = false }
        )
    }

    // Dialog: QR Code
    if (showQrCodeDialog && !dist.roomPasscode.isNullOrBlank()) {
        RoomQrCodeDialog(
            roomName = dist.name,
            passcode = dist.roomPasscode.orEmpty(),
            typeSubtitle = when (dist.type) {
                DistributionType.KHATM -> "Monthly 30-Para Quran Khatm"
                DistributionType.ONE_TIME_KHATM -> "One-Time 30-Para Quran Khatm"
                DistributionType.AMAL -> "Custom Amal Recitation Target"
            },
            onDismiss = { showQrCodeDialog = false }
        )
    }

    // Dialog: Due Reminder
    if (showDueReminderDialog && dist.isOnline) {
        val userRemainingSummary = distributionViewModel.buildOnlineUserRemainingSummary(
            dist = dist,
            participants = activeState.participants,
            assignments = activeState.assignments,
            year = activeState.selectedYear,
            month = activeState.selectedMonth,
            profileName = savedProfileName
        )
        OnlineRemainingReminderDialog(
            roomName = dist.name,
            memberName = savedProfileName.ifBlank { "You" },
            remainingSummary = userRemainingSummary,
            activeScheduleLabel = activeDueScheduleLabel,
            onSelectPreset = { preset ->
                val label = distributionViewModel.scheduleOnlineReminderForActiveRoom(preset)
                activeDueScheduleLabel = label
                Toast.makeText(context, "Reminder set for $label", Toast.LENGTH_SHORT).show()
                showDueReminderDialog = false
            },
            onCancelScheduled = {
                distributionViewModel.cancelDueReminderForActiveRoom()
                activeDueScheduleLabel = null
                Toast.makeText(context, "Reminder cancelled", Toast.LENGTH_SHORT).show()
                showDueReminderDialog = false
            },
            onDismiss = { showDueReminderDialog = false }
        )
    }

    // Dialog: Share
    if (showShareDialog) {
        ShareDistributionDialog(
            shareText = activeState.shareText,
            roomPasscode = dist.roomPasscode,
            onShowQrCode = if (dist.isOnline) { { showQrCodeDialog = true } } else null,
            onDismiss = { showShareDialog = false }
        )
    }

    // Dialog: Reopen Amal Room Round
    if (showReopenAmalDialog && dist.type == DistributionType.AMAL) {
        ReopenAmalRoomDialog(
            roomName = dist.name,
            currentRound = dist.roundNumber,
            isOnlineRoom = dist.isOnline,
            onConfirmReopen = { keepExisting ->
                distributionViewModel.reopenAmalRoom(dist.id, keepExisting)
                showReopenAmalDialog = false
            },
            onDismiss = { showReopenAmalDialog = false }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ONLINE ROOM VIEW
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OnlineRoomView(
    dist: DistributionEntity,
    activeState: com.example.ui.viewmodel.ActiveDistributionUiState,
    savedProfileName: String,
    canOpenAdminPowers: Boolean,
    distributionViewModel: DistributionViewModel,
    onBack: () -> Unit,
    onShowQrCode: () -> Unit,
    onOpenDueReminder: () -> Unit,
    onCopyCode: () -> Unit,
    onShareRoom: () -> Unit,
    onCopyShareText: () -> Unit,
    onOpenAdminPowers: () -> Unit,
    onClaimOneTimePara: (Int) -> Unit,
    onOpenClaimModal: () -> Unit,
    onOpenJoinSlot: () -> Unit,
    onStartChooseParas: () -> Unit,
    onReopenAmalRound: () -> Unit,
    onOpenParaReader: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 64.dp)
    ) {
        // Architectural Pine Header
        item(key = "online_header") {
            OnlineArchitecturalHeader(
                title = dist.name,
                roomPasscode = dist.roomPasscode,
                isHost = canOpenAdminPowers,
                roleBadgeText = if (dist.isHost) "HOST" else "ADMIN",
                onBack = onBack,
                onShowQrCode = onShowQrCode,
                onOpenDueReminder = onOpenDueReminder,
                onCopyCode = onCopyCode,
                onOpenAdminPowers = if (canOpenAdminPowers) onOpenAdminPowers else null
            )
        }

        // Room Content by Type
        when (dist.type) {
            DistributionType.ONE_TIME_KHATM -> {
                item(key = "online_one_time_content") {
                    OnlineOneTimeContent(
                        dist = dist,
                        participants = activeState.participants,
                        savedProfileName = savedProfileName,
                        distributionViewModel = distributionViewModel,
                        onOpenClaimModal = onOpenClaimModal,
                        onClaimPara = onClaimOneTimePara,
                        onShareRoom = onShareRoom,
                        onCopyShareText = onCopyShareText,
                        onOpenDueReminder = onOpenDueReminder,
                        onOpenParaReader = onOpenParaReader
                    )
                }
            }
            DistributionType.KHATM -> {
                item(key = "online_monthly_content") {
                    OnlineMonthlyContent(
                        dist = dist,
                        activeState = activeState,
                        savedProfileName = savedProfileName,
                        distributionViewModel = distributionViewModel,
                        onStartChooseParas = onStartChooseParas,
                        onShareRoom = onShareRoom,
                        onCopyShareText = onCopyShareText,
                        onOpenDueReminder = onOpenDueReminder,
                        onOpenParaReader = onOpenParaReader
                    )
                }
            }
            DistributionType.AMAL -> {
                item(key = "online_amal_content") {
                    OnlineAmalContent(
                        dist = dist,
                        participants = activeState.participants,
                        savedProfileName = savedProfileName,
                        distributionViewModel = distributionViewModel,
                        onOpenPledge = onOpenJoinSlot,
                        onReopenRound = onReopenAmalRound,
                        onShareRoom = onShareRoom,
                        onCopyShareText = onCopyShareText,
                        onOpenDueReminder = onOpenDueReminder
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ONLINE ONE-TIME KHATM
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OnlineOneTimeContent(
    dist: DistributionEntity,
    participants: List<ParticipantEntity>,
    savedProfileName: String,
    distributionViewModel: DistributionViewModel,
    onOpenClaimModal: () -> Unit,
    onClaimPara: (Int) -> Unit,
    onShareRoom: () -> Unit,
    onCopyShareText: () -> Unit,
    onOpenDueReminder: () -> Unit,
    onOpenParaReader: (Int) -> Unit
) {
    val colors = QuranBaantoDesignSystem.colors

    val claimsByPara = remember(participants) {
        participants.associateBy { it.specificParaNumber ?: it.displayOrder }
    }
    val claimedCount = claimsByPara.size
    val completedCount = participants.count { it.isCompleted }
    val openCount = (30 - claimedCount).coerceAtLeast(0)

    // User's own claimed participant entries (may have claimed 1 or more paras)
    val userClaims = remember(participants, savedProfileName) {
        val trimmed = savedProfileName.trim()
        if (trimmed.isNotBlank()) {
            participants.filter { !it.isGuest && it.name.trim().equals(trimmed, ignoreCase = true) }
        } else {
            participants.filter { !it.isGuest }
        }
    }
    val userClaimIds = remember(userClaims) { userClaims.map { it.id }.toSet() }
    val userParaNumbers = remember(userClaims) {
        userClaims.mapNotNull { it.specificParaNumber ?: it.displayOrder }.sorted()
    }

    var filter by remember { mutableStateOf(OneTimeParaFilter.ALL) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Headline
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "One-Time 30-Para Khatm",
                fontFamily = FrauncesFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = colors.text
            )
            Text(
                text = "$claimedCount of 30 claimed · $completedCount of 30 completed",
                fontFamily = InterFontFamily,
                fontSize = 13.5.sp,
                color = colors.onSurfaceVariant
            )
        }

        // Wave Progress Track
        WaveTrack(
            progress = if (claimedCount > 0) completedCount.toFloat() / 30f else 0f,
            pineColor = colors.pine,
            trackColor = colors.gold.copy(alpha = 0.35f)
        )

        // Primary Action: Claim Para Pill Button
        if (claimedCount < 30) {
            Button(
                onClick = onOpenClaimModal,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.pine,
                    contentColor = Color(0xFFF1EBDD)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("claim_para_button")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Claim para (self or guest)",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "→",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }

        // Secondary Action: Mark my para complete Pill Button + Jump to my para link
        if (userClaims.isNotEmpty()) {
            val isUserDone = userClaims.all { it.isCompleted }
            Button(
                onClick = {
                    val target = userClaims.firstOrNull { it.isCompleted == isUserDone } ?: userClaims.first()
                    if (isUserDone) {
                        userClaims.filter { it.isCompleted }.forEach {
                            distributionViewModel.toggleParticipantCompletion(dist.id, it.id)
                        }
                    } else {
                        val nextPending = userClaims.firstOrNull { !it.isCompleted } ?: target
                        distributionViewModel.toggleParticipantCompletion(dist.id, nextPending.id)
                    }
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.gold,
                    contentColor = if (colors.isDark) Color(0xFF15181C) else Color(0xFF1B1B19)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUserDone) "Marked complete ✓" else "Mark my para complete",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "→",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
            if (userParaNumbers.isNotEmpty()) {
                val firstPara = userParaNumbers.first()
                Text(
                    text = "Jump to my para (Para $firstPara) →",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = colors.pineText,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .clickable { onOpenParaReader(firstPara) }
                        .padding(vertical = 2.dp)
                        .testTag("jump_to_my_para_button")
                )
            }
            Text(
                text = "For your paras, or any you claimed for a guest.",
                fontFamily = InterFontFamily,
                fontSize = 12.sp,
                color = colors.onSurfaceVariant
            )
        }

        // Inline Action Links: Share room · Copy text
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Share room",
                textDecoration = TextDecoration.Underline,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = colors.text,
                modifier = Modifier.clickable { onShareRoom() }
            )
            Text("•", color = colors.gold)
            Text(
                text = "Copy text",
                textDecoration = TextDecoration.Underline,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = colors.text,
                modifier = Modifier.clickable { onCopyShareText() }
            )
        }

        // Notification Prompt Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenDueReminder() }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colors.gold.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = colors.gold,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = "Remind me of my remaining share",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp,
                    color = colors.text
                )
                Text(
                    text = "Alerts only for your remaining paras",
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Schedule Header & Filter Tabs
        Text(
            text = "Schedule · 30 paras",
            fontFamily = FrauncesFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = colors.text
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OneTimeFilterTab(
                title = "All 30",
                selected = filter == OneTimeParaFilter.ALL,
                onClick = { filter = OneTimeParaFilter.ALL }
            )
            OneTimeFilterTab(
                title = "Available $openCount",
                selected = filter == OneTimeParaFilter.AVAILABLE,
                onClick = { filter = OneTimeParaFilter.AVAILABLE }
            )
            OneTimeFilterTab(
                title = "Claimed $claimedCount",
                selected = filter == OneTimeParaFilter.CLAIMED,
                onClick = { filter = OneTimeParaFilter.CLAIMED }
            )
        }

        Text(
            text = "Tap an open para to claim it.",
            fontFamily = InterFontFamily,
            fontSize = 12.5.sp,
            color = colors.onSurfaceVariant
        )

        // 30 Arch Tiles Grid (5 rows of 6)
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (row in 0 until 5) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (col in 1..6) {
                        val para = row * 6 + col
                        val claim = claimsByPara[para]
                        val isUserPara = claim != null && (
                            claim.id in userClaimIds ||
                                (savedProfileName.isNotBlank() && claim.name.trim().equals(savedProfileName.trim(), ignoreCase = true))
                            )

                        val state = when {
                            claim == null -> ParaTileState.OPEN
                            claim.isCompleted -> ParaTileState.COMPLETED
                            isUserPara -> ParaTileState.YOURS
                            else -> ParaTileState.CLAIMED
                        }

                        val visible = when (filter) {
                            OneTimeParaFilter.ALL -> true
                            OneTimeParaFilter.AVAILABLE -> claim == null
                            OneTimeParaFilter.CLAIMED -> claim != null
                        }

                        if (visible) {
                            ArchParaTile(
                                paraNumber = para,
                                state = state,
                                onClick = {
                                    if (claim == null) {
                                        onClaimPara(para)
                                    } else if (isUserPara) {
                                        distributionViewModel.toggleParticipantCompletion(
                                            distributionId = dist.id,
                                            participantId = claim.id
                                        )
                                    }
                                }
                            )
                        } else {
                            // Empty placeholder to maintain grid alignment
                            Spacer(modifier = Modifier.size(width = 46.dp, height = 56.dp))
                        }
                    }
                }
            }
        }

        // Grid Legend
        Text(
            text = "Open: gold line · Claimed: black · Done: pine · Yours: wine",
            fontFamily = InterFontFamily,
            fontSize = 11.5.sp,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
    }
}

@Composable
private fun OneTimeFilterTab(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = QuranBaantoDesignSystem.colors
    Column(
        modifier = Modifier.clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            fontFamily = InterFontFamily,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.5.sp,
            color = if (selected) colors.text else colors.onSurfaceVariant
        )
        if (selected) {
            Spacer(modifier = Modifier.height(2.dp))
            Box(
                modifier = Modifier
                    .width(32.dp)
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(colors.gold)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ONLINE MONTHLY KHATM
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OnlineMonthlyContent(
    dist: DistributionEntity,
    activeState: com.example.ui.viewmodel.ActiveDistributionUiState,
    savedProfileName: String,
    distributionViewModel: DistributionViewModel,
    onStartChooseParas: () -> Unit,
    onShareRoom: () -> Unit,
    onCopyShareText: () -> Unit,
    onOpenDueReminder: () -> Unit,
    onOpenParaReader: (Int) -> Unit
) {
    val colors = QuranBaantoDesignSystem.colors

    val dateInfo = activeState.dateInfo
    val participants = activeState.participants
    val assignments = activeState.assignments

    val currentCal = remember { Calendar.getInstance() }
    val isCurrentMonth = activeState.selectedYear == currentCal.get(Calendar.YEAR) &&
        activeState.selectedMonth == (currentCal.get(Calendar.MONTH) + 1)

    // User's participant entries & assignments for the active month
    val userParticipants = remember(participants, savedProfileName) {
        val trimmed = savedProfileName.trim()
        if (trimmed.isNotBlank()) {
            participants.filter { !it.isGuestEntry && it.name.trim().equals(trimmed, ignoreCase = true) }
        } else {
            participants.filter { !it.isGuestEntry }.take(1)
        }
    }
    val userIds = remember(userParticipants) { userParticipants.map { it.id }.toSet() }

    val userAssignments = remember(assignments, userIds) {
        assignments.filter { it.participantId in userIds }
    }
    val userParasThisMonth = remember(userAssignments) {
        userAssignments.flatMap { it.assignedParas }.distinct().sorted()
    }
    val nextAssignmentsForUser = remember(activeState.nextMonthAssignments, userIds) {
        activeState.nextMonthAssignments.filter { it.participantId in userIds }
    }
    val nextParasForUser = remember(nextAssignmentsForUser) {
        nextAssignmentsForUser.flatMap { it.assignedParas }.distinct().sorted()
    }

    val completedCount = participants.sumOf { member ->
        if (member.isCompletedForMonth(activeState.selectedYear, activeState.selectedMonth)) {
            member.allocatedCount
        } else 0
    }.coerceAtMost(30)

    val userWineIndices = remember(userParasThisMonth) {
        userParasThisMonth.map { it - 1 }.toSet()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Month Switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { distributionViewModel.navigateMonth(-1) },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronLeft,
                    contentDescription = "Previous Month",
                    tint = colors.text
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${dateInfo.gregorianMonthName} ${dateInfo.gregorianYear}",
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = colors.text
                )
                Text(
                    text = "${dateInfo.hijriMonthName} ${dateInfo.hijriYear} AH" +
                        if (isCurrentMonth) " · Current" else "",
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariant
                )
            }

            IconButton(
                onClick = { distributionViewModel.navigateMonth(1) },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Next Month",
                    tint = colors.text
                )
            }
        }

        WaveDivider(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp))

        // 220dp Beaded Circular Ring
        Box(
            modifier = Modifier.size(220.dp),
            contentAlignment = Alignment.Center
        ) {
            BeadedCircleVector(
                modifier = Modifier.fillMaxSize(),
                totalBeads = 30,
                completedBeads = completedCount,
                wineIndices = userWineIndices,
                goldColor = colors.gold,
                pineColor = colors.pine,
                wineColor = colors.wineFill,
                unclaimedColor = if (colors.isDark) Color(0xFF333842) else Color(0xFFCBD5E1)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = completedCount.toString(),
                    fontFamily = FrauncesFontFamily,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.text
                )
                Text(
                    text = "of 30 completed",
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariant
                )
            }
        }

        // YOUR PARA Section
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "YOUR PARA",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = colors.gold,
                letterSpacing = 1.2.sp
            )

            if (userParasThisMonth.isNotEmpty()) {
                val thisMonthLabel = if (userAssignments.size == 1) {
                    userAssignments.first().displayRangeText
                } else {
                    "Paras ${userParasThisMonth.joinToString(", ")}"
                }
                val nextMonthLabel = when {
                    nextAssignmentsForUser.size == 1 -> nextAssignmentsForUser.first().displayRangeText
                    nextParasForUser.isNotEmpty() -> "Paras ${nextParasForUser.joinToString(", ")}"
                    else -> "—"
                }
                val isDone = userParticipants.isNotEmpty() && userParticipants.all {
                    it.isCompletedForMonth(activeState.selectedYear, activeState.selectedMonth)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = thisMonthLabel,
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        color = colors.text,
                        modifier = Modifier.weight(1f)
                    )

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Next month",
                            fontFamily = InterFontFamily,
                            fontSize = 11.5.sp,
                            color = colors.onSurfaceVariant
                        )
                        Text(
                            text = "$nextMonthLabel →",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = colors.pine
                        )
                    }
                }

                Text(
                    text = "Picked once. Rotates automatically every month.",
                    fontFamily = InterFontFamily,
                    fontSize = 12.5.sp,
                    color = colors.onSurfaceVariant
                )

                val firstUserPara = userParasThisMonth.first()
                Text(
                    text = "Jump to my para (Para $firstUserPara) →",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = colors.pineText,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier
                        .clickable { onOpenParaReader(firstUserPara) }
                        .padding(vertical = 2.dp)
                        .testTag("jump_to_my_para_button")
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Solid Gold Pill Button: Mark my para complete
                Button(
                    onClick = {
                        userParticipants.forEach { member ->
                            val memberDone = member.isCompletedForMonth(activeState.selectedYear, activeState.selectedMonth)
                            if (memberDone == isDone) {
                                distributionViewModel.toggleParticipantCompletion(
                                    distributionId = dist.id,
                                    participantId = member.id
                                )
                            }
                        }
                    },
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.gold,
                        contentColor = if (colors.isDark) Color(0xFF15181C) else Color(0xFF1B1B19)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isDone) "Marked complete ✓" else "Mark my para complete",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "→",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            } else {
                // User hasn't claimed paras yet -> "Choose your paras →"
                Button(
                    onClick = onStartChooseParas,
                    shape = CircleShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.pine,
                        contentColor = Color(0xFFF1EBDD)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Choose your paras",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "→",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }

                Text(
                    text = "Pick once. When all 30 are claimed, your paras rotate automatically each month.",
                    fontFamily = InterFontFamily,
                    fontSize = 12.5.sp,
                    color = colors.onSurfaceVariant
                )
            }
        }

        // Inline Action Links
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Share room",
                textDecoration = TextDecoration.Underline,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = colors.text,
                modifier = Modifier.clickable { onShareRoom() }
            )
            Text("•", color = colors.gold)
            Text(
                text = "Copy text",
                textDecoration = TextDecoration.Underline,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = colors.text,
                modifier = Modifier.clickable { onCopyShareText() }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ONLINE MONTHLY: CHOOSE YOUR PARAS VIEW
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OnlineMonthlyChooseParasView(
    dist: DistributionEntity,
    activeState: com.example.ui.viewmodel.ActiveDistributionUiState,
    onConfirm: (List<Int>) -> Unit,
    onBack: () -> Unit
) {
    val colors = QuranBaantoDesignSystem.colors

    BackHandler {
        onBack()
    }

    val takenParas = remember(activeState.assignments) {
        activeState.assignments.flatMap { it.assignedParas }.toSet()
    }
    val claimedCount = takenParas.size
    val openParasCount = (30 - claimedCount).coerceAtLeast(0)

    val selectedParas = remember { mutableStateListOf<Int>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.text
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = "ONLINE",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = colors.gold,
                    letterSpacing = 1.2.sp
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(colors.gold)
                )
            }
        }

        // Caption & Headline
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "${dist.name.uppercase()} · MONTHLY",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = colors.gold,
                letterSpacing = 1.2.sp
            )
            Text(
                text = "Choose your paras",
                fontFamily = FrauncesFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = colors.text
            )
            Text(
                text = "Pick once. When all 30 are claimed, your paras rotate automatically each month.",
                fontFamily = InterFontFamily,
                fontSize = 13.sp,
                color = colors.onSurfaceVariant
            )
        }

        // Wave Progress
        WaveTrack(
            progress = claimedCount.toFloat() / 30f,
            pineColor = colors.pine,
            trackColor = colors.gold.copy(alpha = 0.35f)
        )

        // Status row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$claimedCount of 30 claimed · $openParasCount open",
                fontFamily = InterFontFamily,
                fontSize = 13.sp,
                color = colors.onSurfaceVariant
            )

            Text(
                text = "${selectedParas.size} selected",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp,
                color = colors.wineFill
            )
        }

        // 30 Arch Tiles Grid
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            for (row in 0 until 5) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    for (col in 1..6) {
                        val para = row * 6 + col
                        val isTaken = para in takenParas
                        val isSelected = para in selectedParas

                        val state = when {
                            isSelected -> ParaTileState.YOURS
                            isTaken -> ParaTileState.CLAIMED
                            else -> ParaTileState.OPEN
                        }

                        ArchParaTile(
                            paraNumber = para,
                            state = state,
                            onClick = {
                                if (!isTaken) {
                                    if (isSelected) {
                                        selectedParas.remove(para)
                                    } else {
                                        selectedParas.add(para)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // Legend
        Text(
            text = "Open: gold line · Taken: black · Yours: wine",
            fontFamily = InterFontFamily,
            fontSize = 11.5.sp,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Text(
            text = "Rotation starts once all 30 paras are claimed.",
            fontFamily = InterFontFamily,
            fontSize = 12.sp,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.weight(1f))

        // Confirm Button
        Button(
            onClick = {
                if (selectedParas.isNotEmpty()) {
                    onConfirm(selectedParas.sorted())
                }
            },
            enabled = selectedParas.isNotEmpty(),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.pine,
                contentColor = Color(0xFFF1EBDD),
                disabledContainerColor = colors.pine.copy(alpha = 0.4f),
                disabledContentColor = Color(0xFFF1EBDD).copy(alpha = 0.5f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("confirm_monthly_paras_button")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Confirm ${selectedParas.size} paras",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Text(
                    text = "→",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ONLINE AMAL ROOM
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun OnlineAmalContent(
    dist: DistributionEntity,
    participants: List<ParticipantEntity>,
    savedProfileName: String,
    distributionViewModel: DistributionViewModel,
    onOpenPledge: () -> Unit,
    onReopenRound: () -> Unit,
    onShareRoom: () -> Unit,
    onCopyShareText: () -> Unit,
    onOpenDueReminder: () -> Unit
) {
    val colors = QuranBaantoDesignSystem.colors

    val totalTarget = dist.totalTarget
    val totalPledged = participants.sumOf { it.allocatedCount }
    val remainingPledge = (totalTarget - totalPledged).coerceAtLeast(0)

    val totalRecited = participants.sumOf {
        if (it.isCompleted) it.allocatedCount else it.completedCount.coerceIn(0, it.allocatedCount)
    }

    val isRoundComplete = totalRecited >= totalTarget && totalTarget > 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Section Title & Round Tag
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Custom Amal Target",
                fontFamily = FrauncesFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = colors.text
            )

            Text(
                text = "ROUND ${dist.roundNumber}",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
                color = colors.pine,
                letterSpacing = 1.2.sp
            )
        }

        // Large Recitation Counter
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = totalRecited.toString(),
                fontFamily = FrauncesFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 72.sp,
                color = colors.text,
                lineHeight = 72.sp
            )

            Column(modifier = Modifier.padding(bottom = 10.dp)) {
                Text(
                    text = "of $totalTarget",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = colors.text
                )
                Text(
                    text = "recited",
                    fontFamily = InterFontFamily,
                    fontSize = 14.sp,
                    color = colors.onSurfaceVariant
                )
            }
        }

        // Wave Progress Track
        WaveTrack(
            progress = if (totalTarget > 0) totalRecited.toFloat() / totalTarget.toFloat() else 0f,
            pineColor = colors.pine,
            trackColor = colors.gold.copy(alpha = 0.35f)
        )

        // Summary row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isRoundComplete) "Round ${dist.roundNumber} complete"
                       else "$totalPledged pledged · $remainingPledge left to pledge",
                fontFamily = InterFontFamily,
                fontSize = 13.sp,
                color = colors.onSurfaceVariant
            )

            Text(
                text = "${participants.size} members",
                fontFamily = InterFontFamily,
                fontSize = 13.sp,
                color = colors.onSurfaceVariant
            )
        }

        // Pill Action Button
        if (isRoundComplete) {
            // Reopen for Round X Pill
            Button(
                onClick = onReopenRound,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.gold,
                    contentColor = if (colors.isDark) Color(0xFF15181C) else Color(0xFF1B1B19)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reopen for Round ${dist.roundNumber + 1}",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "→",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
            Text(
                text = "Same members and target. No set-up needed.",
                fontFamily = InterFontFamily,
                fontSize = 12.sp,
                color = colors.onSurfaceVariant
            )
        } else if (remainingPledge > 0) {
            // Pledge recitations Pill
            Button(
                onClick = onOpenPledge,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.pine,
                    contentColor = Color(0xFFF1EBDD)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pledge recitations",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "→",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
            Text(
                text = "For yourself or a guest · $remainingPledge left",
                fontFamily = InterFontFamily,
                fontSize = 12.sp,
                color = colors.onSurfaceVariant
            )
        }

        // Inline Action Links
        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Share room",
                textDecoration = TextDecoration.Underline,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = colors.text,
                modifier = Modifier.clickable { onShareRoom() }
            )
            Text("•", color = colors.gold)
            Text(
                text = "Copy text",
                textDecoration = TextDecoration.Underline,
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = colors.text,
                modifier = Modifier.clickable { onCopyShareText() }
            )
        }

        // Reminder prompt
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenDueReminder() }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(colors.gold.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = colors.gold,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = "Remind me of my remaining share",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp,
                    color = colors.text
                )
                Text(
                    text = "Alerts only for your remaining recitations",
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Member allocations Header
        Text(
            text = if (isRoundComplete) "Final allocations" else "Member allocations",
            fontFamily = FrauncesFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = colors.text
        )

        Text(
            text = "Tap + to count your own. Others show progress only.",
            fontFamily = InterFontFamily,
            fontSize = 12.5.sp,
            color = colors.onSurfaceVariant
        )

        // Participants List or Empty State
        if (participants.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ArchWindowVector(
                        modifier = Modifier.size(44.dp),
                        outlineColor = colors.gold
                    )
                    Text(
                        text = "No member allocations yet.",
                        fontFamily = FrauncesFontFamily,
                        fontSize = 16.sp,
                        color = colors.text
                    )
                    Text(
                        text = "Pledge recitations above to begin this Amal round.",
                        fontFamily = InterFontFamily,
                        fontSize = 12.5.sp,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                participants.forEach { member ->
                    val isUser = member.name.equals(savedProfileName, ignoreCase = true) ||
                        (!member.isGuest && savedProfileName.isBlank())

                    val isUserGuest = member.isGuest && member.addedByProfileName.equals(savedProfileName, ignoreCase = true)
                    val canEditThisMember = isUser || isUserGuest

                    val currentRecited = if (member.isCompleted) member.allocatedCount else member.completedCount

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar circle with Initial
                            val initial = member.name.trim().take(1).uppercase().ifBlank { "M" }
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (canEditThisMember) colors.pine
                                        else if (colors.isDark) Color(0xFF232832)
                                        else Color(0xFFE2DDD2)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initial,
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (canEditThisMember) Color(0xFFF1EBDD) else colors.text
                                )
                            }

                            Column {
                                Text(
                                    text = when {
                                        isUser -> "You"
                                        isUserGuest -> "${member.name} · your guest"
                                        else -> member.name
                                    },
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = colors.text
                                )
                                Text(
                                    text = "$currentRecited of ${member.allocatedCount} recited",
                                    fontFamily = InterFontFamily,
                                    fontSize = 12.5.sp,
                                    color = colors.onSurfaceVariant
                                )
                            }
                        }

                        // Right side controls
                        if (canEditThisMember && !isRoundComplete) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Round "+" Button (increment count)
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(colors.pine)
                                        .clickable {
                                            val newCount = minOf(member.allocatedCount, member.completedCount + 1)
                                            distributionViewModel.updateAmalCompletedCount(
                                                distributionId = dist.id,
                                                participantId = member.id,
                                                newCount = newCount
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Count +1",
                                        tint = Color(0xFFF1EBDD),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Round Checkmark Button (mark 100% complete)
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .border(1.2.dp, colors.gold, CircleShape)
                                        .clickable {
                                            distributionViewModel.toggleParticipantCompletion(
                                                distributionId = dist.id,
                                                participantId = member.id
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Complete",
                                        tint = if (member.isCompleted) colors.pine else colors.gold,
                                        modifier = Modifier.size(17.dp)
                                    )
                                }
                            }
                        } else {
                            // Progress Text for other members
                            Text(
                                text = if (member.isCompleted) "✓ ${member.allocatedCount} / ${member.allocatedCount}"
                                       else "$currentRecited / ${member.allocatedCount}",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = if (member.isCompleted) colors.pine else colors.onSurfaceVariant
                            )
                        }
                    }

                    WaveDivider(modifier = Modifier.fillMaxWidth())
                }
            }
        }

        Text(
            text = "The room completes automatically when everyone is done.",
            fontFamily = InterFontFamily,
            fontSize = 12.sp,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LOCAL ROOM VIEW
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun LocalRoomView(
    dist: DistributionEntity,
    activeState: com.example.ui.viewmodel.ActiveDistributionUiState,
    distributionViewModel: DistributionViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    onCopyText: () -> Unit,
    onOpenParaReader: (Int) -> Unit
) {
    val colors = QuranBaantoDesignSystem.colors

    val dateInfo = activeState.dateInfo
    val participants = activeState.participants
    val assignments = activeState.assignments
    val nextAssignments = activeState.nextMonthAssignments
    val isMonthly = dist.type == DistributionType.KHATM
    val isOneTime = dist.type == DistributionType.ONE_TIME_KHATM
    val isAmal = dist.type == DistributionType.AMAL

    val allocationCounts = remember(participants) {
        participants.map { it.allocatedCount }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 64.dp)
    ) {
        // Top Bar
        item(key = "local_topbar") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.text
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Room",
                            tint = colors.text,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Room",
                            tint = colors.text,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Room Header Info
        item(key = "local_header_info") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (dist.type) {
                            DistributionType.KHATM -> "LOCAL ROOM · MONTHLY KHATM"
                            DistributionType.ONE_TIME_KHATM -> "LOCAL ROOM · ONE-TIME KHATM"
                            DistributionType.AMAL -> "LOCAL ROOM · AMAL"
                        },
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = colors.gold,
                        letterSpacing = 1.2.sp
                    )

                    if (isOneTime) {
                        Text(
                            text = "ONE-TIME",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp,
                            color = colors.pine,
                            letterSpacing = 1.2.sp
                        )
                    }
                }

                Text(
                    text = dist.name,
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                    color = colors.text
                )

                Text(
                    text = if (isAmal) "${participants.size} participants · ${dist.totalTarget} total recitations"
                           else "${participants.size} participants · exactly 30 paras",
                    fontFamily = InterFontFamily,
                    fontSize = 13.5.sp,
                    color = colors.onSurfaceVariant
                )
            }
        }

        // Monthly Switcher (if monthly)
        if (isMonthly) {
            item(key = "local_monthly_switcher") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { distributionViewModel.navigateMonth(-1) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Previous Month",
                                tint = colors.text
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${dateInfo.gregorianMonthName} ${dateInfo.gregorianYear}",
                                fontFamily = FrauncesFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = colors.text
                            )
                            Text(
                                text = "${dateInfo.hijriMonthName} ${dateInfo.hijriYear} AH",
                                fontFamily = InterFontFamily,
                                fontSize = 12.sp,
                                color = colors.onSurfaceVariant
                            )
                        }

                        IconButton(
                            onClick = { distributionViewModel.navigateMonth(1) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Next Month",
                                tint = colors.text
                            )
                        }
                    }

                    WaveDivider(modifier = Modifier.fillMaxWidth())
                }
            }
        }

        // Segmented Allocation Bar & Caption
        item(key = "local_segmented_bar") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = when {
                        isMonthly -> "ROTATING ASSIGNMENTS (30 PARAS)"
                        isOneTime -> "ASSIGNMENTS (30 PARAS)"
                        else -> "MEMBER ALLOCATIONS"
                    },
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = colors.gold,
                    letterSpacing = 1.2.sp
                )

                SegmentedAllocationBar(
                    allocations = allocationCounts,
                    total = dist.totalTarget
                )
            }
        }

        // Participants Roster
        if (participants.isEmpty()) {
            item(key = "local_empty_state") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ArchWindowVector(
                            modifier = Modifier.size(48.dp),
                            outlineColor = colors.gold
                        )
                        Text(
                            text = "No participants added yet.",
                            fontFamily = FrauncesFontFamily,
                            fontSize = 18.sp,
                            color = colors.text
                        )
                        Text(
                            text = "Edit this room to add members and assign portions.",
                            fontFamily = InterFontFamily,
                            fontSize = 13.sp,
                            color = colors.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            itemsIndexed(
                items = participants,
                key = { _, p -> p.id }
            ) { index, participant ->
                val assignment = assignments.firstOrNull { it.participantId == participant.id }
                val startPara = assignment?.startPara ?: (participant.specificParaNumber ?: (index + 1))
                val endPara = assignment?.endPara ?: (((startPara - 1 + participant.allocatedCount - 1) % 30) + 1)
                val nextAssignment = nextAssignments.firstOrNull { it.participantId == participant.id }
                val nextPreviewText = nextAssignment?.displayRangeText ?: run {
                    val ns = ((startPara - 1 + participant.allocatedCount) % 30) + 1
                    val ne = ((ns - 1 + participant.allocatedCount - 1) % 30) + 1
                    if (participant.allocatedCount == 1) "Para $ns" else "Paras $ns–$ne"
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (!isAmal) {
                                Modifier.clickable { onOpenParaReader(startPara) }
                            } else Modifier
                        )
                        .padding(horizontal = 24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Numbered Medallion (1, 2, 3...)
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (colors.isDark) Color(0xFF232832)
                                        else Color(0xFFE2DDD2)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = (index + 1).toString(),
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = colors.text
                                )
                            }

                            Column {
                                Text(
                                    text = participant.name,
                                    fontFamily = FrauncesFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 18.sp,
                                    color = colors.text
                                )

                                Text(
                                    text = when {
                                        isAmal -> "${participant.allocatedCount} recitations"
                                        startPara == endPara -> "Para $startPara · 1"
                                        else -> "Paras $startPara–$endPara · ${participant.allocatedCount}"
                                    },
                                    fontFamily = InterFontFamily,
                                    fontSize = 12.5.sp,
                                    color = colors.pine
                                )
                            }
                        }

                        // Next month rotation preview (for Monthly Khatm) or Read Para CTA
                        if (isMonthly) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "NEXT MONTH",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = colors.onSurfaceVariant,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = nextPreviewText,
                                    fontFamily = FrauncesFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = colors.text
                                )
                            }
                        } else if (isOneTime) {
                            Text(
                                text = "Read →",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = colors.pineText,
                                textDecoration = TextDecoration.Underline
                            )
                        }
                    }

                    WaveDivider(modifier = Modifier.fillMaxWidth())
                }
            }
        }

        // Bottom Action Buttons: Copy text + Share
        item(key = "local_bottom_actions") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Copy text",
                        textDecoration = TextDecoration.Underline,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = colors.text,
                        modifier = Modifier.clickable { onCopyText() }
                    )

                    Button(
                        onClick = onShare,
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.pine,
                            contentColor = Color(0xFFF1EBDD)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Share",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "→",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }
                }

                // If One-Time: Mark room complete button
                if (isOneTime) {
                    Button(
                        onClick = {
                            participants.forEach { p ->
                                distributionViewModel.toggleParticipantCompletion(
                                    distributionId = dist.id,
                                    participantId = p.id
                                )
                            }
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.gold,
                            contentColor = if (colors.isDark) Color(0xFF15181C) else Color(0xFF1B1B19)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Mark room complete",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "→",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                        }
                    }

                    Text(
                        text = "Host only · the room closes when you mark it complete.",
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp,
                        color = colors.onSurfaceVariant
                    )
                }
            }
        }
    }
}
