package com.example.ui.distribution

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.DistributionType
import com.example.data.db.ParticipantEntity
import com.example.ui.components.WaveDivider
import com.example.ui.theme.FrauncesFontFamily
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.QuranBaantoDesignSystem
import com.example.ui.viewmodel.DistributionViewModel
import java.util.Calendar

private data class AdminGroupedMember(
    val key: String,
    val displayName: String,
    val isGuest: Boolean,
    val isAdmin: Boolean,
    val slots: List<ParticipantEntity>,
    val portionSummary: String,
    val statusText: String,
    val isCompleted: Boolean
)

/**
 * ADMIN POWERS SCREEN
 * Accessible ONLY for Online Rooms created by the current user (Host) or where the current user
 * has been granted Co-Admin privileges by the creator.
 */
@Composable
fun DistributionAdminScreen(
    distributionId: Long,
    distributionViewModel: DistributionViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = QuranBaantoDesignSystem.colors

    val distributions by distributionViewModel.distributions.collectAsState()
    val participantsByDistribution by distributionViewModel.participantsByDistribution.collectAsState()
    val userProfileName by distributionViewModel.userProfileName.collectAsState()

    val dist = distributions.firstOrNull { it.id == distributionId }
    val participants = participantsByDistribution[distributionId].orEmpty()

    var showDeleteConfirmScreen by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }

    BackHandler(enabled = true) {
        if (showDeleteConfirmScreen) {
            showDeleteConfirmScreen = false
        } else {
            onBack()
        }
    }

    if (dist == null) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(colors.background),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Session not found",
                fontFamily = FrauncesFontFamily,
                color = colors.text
            )
        }
        return
    }

    AnimatedContent(
        targetState = showDeleteConfirmScreen,
        transitionSpec = {
            fadeIn(tween(180, easing = FastOutSlowInEasing)) togetherWith
                fadeOut(tween(140, easing = FastOutSlowInEasing))
        },
        label = "admin_delete_transition"
    ) { isConfirmingDelete ->
        if (isConfirmingDelete) {
            val uniqueMemberCount = participants
                .map { "${it.name.trim().lowercase()}|${it.isGuestEntry}" }
                .distinct()
                .size
            DeleteRoomScreen(
                roomName = dist.name,
                memberCount = uniqueMemberCount,
                onConfirmDelete = {
                    distributionViewModel.deleteDistribution(dist.id)
                    showDeleteConfirmScreen = false
                    onBack()
                },
                onCancel = { showDeleteConfirmScreen = false }
            )
        } else {
            val passcode = dist.roomPasscode ?: ""
            val currentCal = remember { Calendar.getInstance() }
            val currentYear = currentCal.get(Calendar.YEAR)
            val currentMonth = currentCal.get(Calendar.MONTH) + 1

            val currentAssignments = remember(dist, participants, currentYear, currentMonth) {
                if (dist.type == DistributionType.KHATM) {
                    distributionViewModel.computeAssignmentsForDistribution(
                        distribution = dist,
                        participants = participants,
                        targetYear = currentYear,
                        targetMonth = currentMonth
                    )
                } else {
                    emptyList()
                }
            }

            // Group participants cleanly so each person appears ONCE in the member roster
            val groupedMembers = remember(dist, participants, currentAssignments, currentYear, currentMonth) {
                val assignmentsByParticipantId = currentAssignments.associateBy { it.participantId }
                val linked = linkedMapOf<String, MutableList<ParticipantEntity>>()
                for (p in participants) {
                    val key = "${p.name.trim().lowercase()}|${p.isGuestEntry}"
                    linked.getOrPut(key) { mutableListOf() }.add(p)
                }

                linked.entries.map { (key, slots) ->
                    val primary = slots.first()
                    val displayName = primary.name.trim() + if (primary.isGuestEntry) " (Guest)" else ""
                    val isAdmin = slots.any { it.isAdmin }

                    val (portionSummary, statusText, isAllDone) = when (dist.type) {
                        DistributionType.ONE_TIME_KHATM -> {
                            val paras = slots.mapNotNull { it.specificParaNumber ?: it.displayOrder.takeIf { d -> d in 1..30 } }
                                .distinct()
                                .sorted()
                            val label = when {
                                paras.size == 1 -> "Para ${paras.first()}"
                                paras.isNotEmpty() -> "Paras ${paras.joinToString(", ")}"
                                else -> "${slots.sumOf { it.allocatedCount }} paras"
                            }
                            val doneCount = slots.count { it.isCompleted }
                            val allDone = slots.isNotEmpty() && doneCount == slots.size
                            val status = when {
                                allDone -> "$label · Completed ✓"
                                doneCount > 0 -> "$label · $doneCount/${slots.size} done"
                                else -> "$label · Pending"
                            }
                            Triple(label, status, allDone)
                        }

                        DistributionType.KHATM -> {
                            val memberParas = slots
                                .flatMap { slot -> assignmentsByParticipantId[slot.id]?.assignedParas.orEmpty() }
                                .distinct()
                                .sorted()
                            val label = when {
                                memberParas.size == 1 -> "Para ${memberParas.first()}"
                                memberParas.size > 1 -> {
                                    // Show compact range if contiguous, else comma list
                                    val isContiguous = memberParas.zipWithNext().all { (a, b) -> b == a + 1 }
                                    if (isContiguous) "Paras ${memberParas.first()}–${memberParas.last()}"
                                    else "Paras ${memberParas.joinToString(", ")}"
                                }
                                else -> "${slots.sumOf { it.allocatedCount }} paras"
                            }
                            val doneCount = slots.count { it.isCompletedForMonth(currentYear, currentMonth) }
                            val allDone = slots.isNotEmpty() && doneCount == slots.size
                            val status = if (allDone) {
                                "$label · Completed ✓"
                            } else {
                                "$label · Pending this month"
                            }
                            Triple(label, status, allDone)
                        }

                        DistributionType.AMAL -> {
                            val totalPledged = slots.sumOf { it.allocatedCount }
                            val totalRecited = slots.sumOf { if (it.isCompleted) it.allocatedCount else it.completedCount.coerceIn(0, it.allocatedCount) }
                            val allDone = slots.isNotEmpty() && slots.all { it.isCompleted }
                            val label = "$totalPledged recitations"
                            val status = if (allDone) {
                                "$totalPledged pledged · Completed ✓"
                            } else {
                                "$totalPledged pledged · $totalRecited recited"
                            }
                            Triple(label, status, allDone)
                        }
                    }

                    AdminGroupedMember(
                        key = key,
                        displayName = displayName,
                        isGuest = primary.isGuestEntry,
                        isAdmin = isAdmin,
                        slots = slots,
                        portionSummary = portionSummary,
                        statusText = statusText,
                        isCompleted = isAllDone
                    )
                }
            }

            Box(
                modifier = modifier
                    .fillMaxSize()
                    .background(colors.background)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.statusBars)
                        .padding(horizontal = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Top Bar: Back arrow and HOST / ADMIN pill badge
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

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (dist.isHost) colors.gold else colors.pine)
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (dist.isHost) "HOST" else "ADMIN",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp,
                                    color = if (dist.isHost) Color(0xFF1B1B19) else Color(0xFFF1EBDD)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "Admin powers",
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 36.sp,
                            letterSpacing = (-0.5).sp,
                            color = colors.text
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ONLINE ROOM · $passcode",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                letterSpacing = 1.1.sp,
                                color = colors.mutedText
                            )

                            Text(
                                text = "ROUND ${dist.roundNumber}",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.1.sp,
                                color = colors.pineText
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = dist.name,
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 28.sp,
                            letterSpacing = (-0.3).sp,
                            color = colors.text
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        val totalLabel = if (dist.type == DistributionType.AMAL) {
                            "${dist.totalTarget} total recitations"
                        } else "30 total portions"

                        Text(
                            text = "${groupedMembers.size} registered ${if (groupedMembers.size == 1) "member" else "members"} · $totalLabel",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            color = colors.mutedText
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Action buttons: Copy code, QR code
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        val clip = ClipData.newPlainText("Room Passcode", passcode)
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        cm?.setPrimaryClip(clip)
                                        Toast.makeText(context, "Room code copied to clipboard", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .border(1.2.dp, colors.gold, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy code",
                                        tint = colors.text,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Text(
                                    text = "Copy code",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    textDecoration = TextDecoration.Underline,
                                    color = colors.pineText
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { showQrDialog = true }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .border(1.2.dp, colors.gold, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.QrCode2,
                                        contentDescription = "QR code",
                                        tint = colors.text,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Text(
                                    text = "QR code",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    textDecoration = TextDecoration.Underline,
                                    color = colors.pineText
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        WaveDivider(
                            color = colors.rules,
                            strokeWidth = 1.2.dp,
                            amplitude = 2.dp,
                            wavelength = 44.dp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Member Roster Section
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Member roster",
                                fontFamily = FrauncesFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 24.sp,
                                color = colors.text
                            )

                            Text(
                                text = "${groupedMembers.size} ${if (groupedMembers.size == 1) "member" else "members"}",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                color = colors.mutedText
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = if (dist.isHost) {
                                "View portions, nudge pending readers, grant co-admin access, or remove slots."
                            } else {
                                "View portions, nudge pending readers, or remove inactive slots."
                            },
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            color = colors.mutedText
                        )

                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    if (groupedMembers.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 36.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                ArchWindowVector(
                                    modifier = Modifier.size(width = 64.dp, height = 76.dp),
                                    outlineColor = colors.gold,
                                    strokeWidth = 1.6.dp
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                Text(
                                    text = "No participants in this room yet.",
                                    fontFamily = FrauncesFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 20.sp,
                                    color = colors.text
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "Share the room code so members can join.",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = colors.mutedText
                                )
                            }
                        }
                    } else {
                        items(groupedMembers, key = { it.key }) { memberGroup ->
                            val cleanSelfName = userProfileName.trim()
                            val isSelf = cleanSelfName.isNotBlank() &&
                                memberGroup.slots.any { it.name.trim().equals(cleanSelfName, ignoreCase = true) && !it.isGuestEntry }
                            val isHostMember = dist.isHost && (
                                isSelf || (cleanSelfName.isBlank() && memberGroup.slots.any { it.displayOrder == 1 && !it.isGuestEntry })
                                )

                            AdminMemberRow(
                                group = memberGroup,
                                isHostMember = isHostMember,
                                isSelf = isSelf,
                                viewerIsHost = dist.isHost,
                                onNudge = {
                                    val cleanMemberName = memberGroup.slots.first().name.trim()
                                    val (postedNotif, nudgeMsg) = distributionViewModel.nudgeParticipant(
                                        distributionId = dist.id,
                                        roomName = dist.name,
                                        memberName = cleanMemberName,
                                        portionLabel = memberGroup.portionSummary
                                    )
                                    val toastText = if (postedNotif) {
                                        "Nudge notification sent for $cleanMemberName (${memberGroup.portionSummary})"
                                    } else {
                                        "Share nudge for $cleanMemberName (${memberGroup.portionSummary})"
                                    }
                                    Toast.makeText(context, toastText, Toast.LENGTH_SHORT).show()

                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, nudgeMsg)
                                    }
                                    try {
                                        context.startActivity(Intent.createChooser(shareIntent, "Send Nudge"))
                                    } catch (_: Exception) {
                                    }
                                },
                                onToggleAdmin = {
                                    val cleanMemberName = memberGroup.slots.first().name.trim()
                                    val newAdmin = !memberGroup.isAdmin
                                    distributionViewModel.setMemberAdminRole(
                                        distributionId = dist.id,
                                        memberName = cleanMemberName,
                                        isGuest = memberGroup.isGuest,
                                        isAdmin = newAdmin
                                    )
                                    Toast.makeText(
                                        context,
                                        if (newAdmin) "$cleanMemberName now has Admin powers" else "Removed Admin powers from $cleanMemberName",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onRemove = {
                                    memberGroup.slots.forEach { slot ->
                                        distributionViewModel.removeParticipantClaim(slot.id)
                                    }
                                }
                            )
                        }
                    }

                    // Danger Zone Section (Delete room session — available to Host)
                    if (dist.isHost) {
                        item {
                            Spacer(modifier = Modifier.height(14.dp))

                            WaveDivider(
                                color = colors.rules,
                                strokeWidth = 1.2.dp,
                                amplitude = 2.dp,
                                wavelength = 44.dp,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "DANGER ZONE",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                letterSpacing = 1.1.sp,
                                color = colors.wineText
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Session controls",
                                fontFamily = FrauncesFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 24.sp,
                                color = colors.text
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Delete room session",
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = colors.text
                                    )

                                    Spacer(modifier = Modifier.height(3.dp))

                                    Text(
                                        text = "Permanently removes this room and its roster records.",
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 13.sp,
                                        color = colors.mutedText
                                    )
                                }

                                Button(
                                    onClick = { showDeleteConfirmScreen = true },
                                    shape = RoundedCornerShape(24.dp),
                                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.wineFill,
                                        contentColor = Color(0xFFF1EBDD)
                                    ),
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 96.dp, minHeight = 48.dp)
                                        .testTag("admin_delete_room_button")
                                ) {
                                    Text(
                                        text = "Delete",
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    } else {
                        item {
                            Spacer(modifier = Modifier.height(36.dp))
                        }
                    }
                }
            }
        }
    }

    if (showQrDialog) {
        RoomQrCodeDialog(
            roomName = dist.name,
            passcode = dist.roomPasscode ?: "",
            typeSubtitle = "Online Room Passcode",
            onDismiss = { showQrDialog = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdminMemberRow(
    group: AdminGroupedMember,
    isHostMember: Boolean,
    isSelf: Boolean,
    viewerIsHost: Boolean,
    onNudge: () -> Unit,
    onToggleAdmin: () -> Unit,
    onRemove: () -> Unit
) {
    val colors = QuranBaantoDesignSystem.colors
    val initial = group.displayName.trim().firstOrNull()?.uppercase() ?: "M"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isHostMember -> colors.pine
                            group.isAdmin -> colors.gold.copy(alpha = 0.25f)
                            else -> colors.softNeutral
                        }
                    )
                    .then(
                        if (group.isAdmin && !isHostMember) {
                            Modifier.border(1.2.dp, colors.gold, CircleShape)
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isHostMember) Color(0xFFF1EBDD) else colors.text
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isSelf) "${group.displayName} · you" else group.displayName,
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = colors.text
                    )

                    if (group.isAdmin && !isHostMember) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.gold.copy(alpha = 0.2f))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Admin",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = colors.text
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = group.statusText,
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = if (group.isCompleted) colors.pineText else colors.mutedText
                )
            }

            if (isHostMember) {
                Text(
                    text = "Host",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = colors.mutedText
                )
            }
        }

        if (!isHostMember) {
            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                modifier = Modifier.padding(start = 60.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Nudge button next to Remove on all non-host members (especially uncompleted)
                if (!group.isCompleted) {
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 40.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.pine)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onNudge() }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("admin_nudge_${group.slots.first().name.trim()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = "Nudge ${group.displayName}",
                                tint = Color(0xFFF1EBDD),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Nudge",
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = Color(0xFFF1EBDD)
                            )
                        }
                    }
                }

                // Host can assign/revoke Admin powers for family members (not guest placeholders)
                if (viewerIsHost && !group.isGuest) {
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minHeight = 40.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .border(
                                1.2.dp,
                                if (group.isAdmin) colors.gold else colors.rules,
                                RoundedCornerShape(18.dp)
                            )
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { onToggleAdmin() }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("admin_toggle_role_${group.slots.first().name.trim()}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (group.isAdmin) "Admin ✓" else "Make admin",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = if (group.isAdmin) colors.pineText else colors.text
                        )
                    }
                }

                // Remove button
                Box(
                    modifier = Modifier
                        .defaultMinSize(minHeight = 40.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.2.dp, colors.wine, RoundedCornerShape(18.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onRemove() }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .testTag("admin_remove_${group.slots.first().name.trim()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Remove",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = colors.wineText
                    )
                }
            }
        }
    }
}
