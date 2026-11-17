package com.example.ui.distribution

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.PrimarySanctuaryButton
import com.example.ui.components.SecondaryEditorialButton
import com.example.ui.theme.QuranBaantoDesignSystem
import com.example.util.KhatmNotificationHelper
import com.example.util.RoomQrCodeHelper

/**
 * First-come, first-served Para claim dialog for One-Time Qur'an Khatm.
 * Supports claiming for Yourself OR claiming on behalf of an elderly family member / Guest (e.g. Grandma).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OneTimeParaClaimDialog(
    initialParaNumber: Int?,
    takenParaNumbers: Set<Int>,
    savedProfileName: String,
    onConfirmClaim: (selectedParas: List<Int>, memberName: String, isGuest: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var isGuest by remember { mutableStateOf(false) }
    var selfName by remember(savedProfileName) { mutableStateOf(savedProfileName) }
    var guestName by remember { mutableStateOf("") }
    var hasAttemptedSubmit by remember { mutableStateOf(false) }

    val selectedParas = remember(initialParaNumber) {
        mutableStateListOf<Int>().apply {
            if (initialParaNumber != null && initialParaNumber !in takenParaNumbers) {
                add(initialParaNumber)
            } else {
                val firstFree = (1..30).firstOrNull { it !in takenParaNumbers }
                if (firstFree != null) add(firstFree)
            }
        }
    }

    val activeName = if (isGuest) guestName.trim() else selfName.trim()
    val isNameError = hasAttemptedSubmit && activeName.isBlank()
    val isParasError = hasAttemptedSubmit && selectedParas.isEmpty()
    val canConfirm = activeName.isNotEmpty() && selectedParas.isNotEmpty()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp)
                .widthIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Claim Para to Recite",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "First-come, first-served • Pick for yourself or a guest",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Surface(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Myself vs Guest (Grandma / Family) Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ClaimIdentityTab(
                        title = "For Myself",
                        icon = Icons.Default.Person,
                        selected = !isGuest,
                        onClick = { isGuest = false },
                        modifier = Modifier.weight(1f)
                    )
                    ClaimIdentityTab(
                        title = "For Guest / Family",
                        icon = Icons.Default.Favorite,
                        selected = isGuest,
                        onClick = { isGuest = true },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = if (isGuest) guestName else selfName,
                    onValueChange = {
                        if (isGuest) guestName = it else selfName = it
                        if (it.isNotBlank()) hasAttemptedSubmit = false
                    },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (isGuest) "Guest / Family Member Name" else "Your Name")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("*", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.ExtraBold)
                        }
                    },
                    placeholder = {
                        Text(if (isGuest) "e.g. Grandma, Dadi, Nani (Compulsory)" else "e.g. Hamza (Compulsory)")
                    },
                    isError = isNameError,
                    supportingText = {
                        if (isNameError) {
                            Text(
                                text = "Name is compulsory to claim a Para",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        } else {
                            Text(
                                text = "Compulsory * • Name recorded on the Khatm roster",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    trailingIcon = {
                        if (isNameError) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Name required",
                                tint = MaterialTheme.colorScheme.error
                            )
                        } else if (activeName.isNotBlank()) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Valid",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = if (isNameError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
                        errorBorderColor = MaterialTheme.colorScheme.error,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                if (isParasError) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Please select at least 1 available Para to claim",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                Text(
                    text = "Select Available Para(s) (${selectedParas.size} selected) *",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isParasError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (para in 1..30) {
                        val isTaken = para in takenParaNumbers
                        val isSelected = para in selectedParas

                        Surface(
                            onClick = {
                                if (!isTaken) {
                                    if (isSelected) {
                                        selectedParas.remove(para)
                                    } else {
                                        selectedParas.add(para)
                                    }
                                }
                            },
                            enabled = !isTaken,
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                isTaken -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                isSelected -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(
                                width = 1.dp,
                                color = when {
                                    isTaken -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    isSelected -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.outlineVariant
                                }
                            ),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$para",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    ),
                                    color = when {
                                        isTaken -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                        isSelected -> MaterialTheme.colorScheme.onPrimary
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SecondaryEditorialButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    PrimarySanctuaryButton(
                        text = if (selectedParas.size <= 1) "Claim Para" else "Claim ${selectedParas.size} Paras",
                        icon = Icons.Default.Check,
                        onClick = {
                            if (!canConfirm) {
                                hasAttemptedSubmit = true
                            } else {
                                onConfirmClaim(selectedParas.toList(), activeName, isGuest)
                            }
                        },
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }
        }
    }
}

/**
 * Dialog for claiming a monthly Para count (in Online Monthly Khatm) or Recitation count (in Online Amal)
 * for Yourself or for a Guest (e.g. Grandma).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun JoinAndClaimSlotDialog(
    isAmal: Boolean,
    remainingCapacity: Int,
    savedProfileName: String,
    onConfirmClaim: (memberName: String, count: Int, isGuest: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var isGuest by remember { mutableStateOf(false) }
    var selfName by remember(savedProfileName) { mutableStateOf(savedProfileName) }
    var guestName by remember { mutableStateOf("") }
    var hasAttemptedSubmit by remember { mutableStateOf(false) }
    var countText by remember(isAmal, remainingCapacity) {
        val defaultVal = if (isAmal) minOf(10, remainingCapacity.coerceAtLeast(1)) else 1
        mutableStateOf(defaultVal.toString())
    }

    val parsedCount = (countText.toIntOrNull() ?: 0).coerceIn(0, remainingCapacity.coerceAtLeast(1))
    val activeName = if (isGuest) guestName.trim() else selfName.trim()
    val isNameError = hasAttemptedSubmit && activeName.isBlank()
    val isCountError = hasAttemptedSubmit && (parsedCount < 1 || parsedCount > remainingCapacity)
    val canConfirm = activeName.isNotEmpty() && parsedCount in 1..remainingCapacity

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isAmal) "Pledge Amal Recitations" else "Claim Monthly Para(s)",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ClaimIdentityTab(
                        title = "For Myself",
                        icon = Icons.Default.Person,
                        selected = !isGuest,
                        onClick = { isGuest = false },
                        modifier = Modifier.weight(1f)
                    )
                    ClaimIdentityTab(
                        title = "For Guest",
                        icon = Icons.Default.Favorite,
                        selected = isGuest,
                        onClick = { isGuest = true },
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = if (isGuest) guestName else selfName,
                    onValueChange = {
                        if (isGuest) guestName = it else selfName = it
                        if (it.isNotBlank()) hasAttemptedSubmit = false
                    },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (isGuest) "Guest / Family Name" else "Your Name")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("*", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.ExtraBold)
                        }
                    },
                    placeholder = {
                        Text(if (isGuest) "e.g. Grandma, Dadi, Nani (Compulsory)" else "e.g. Hamza (Compulsory)")
                    },
                    isError = isNameError,
                    supportingText = {
                        if (isNameError) {
                            Text(
                                text = "Name is compulsory to claim",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        } else {
                            Text(
                                text = "Compulsory * • Name recorded on the room roster",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    trailingIcon = {
                        if (isNameError) {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Name required",
                                tint = MaterialTheme.colorScheme.error
                            )
                        } else if (activeName.isNotBlank()) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Valid",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = if (isNameError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
                        errorBorderColor = MaterialTheme.colorScheme.error,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                OutlinedTextField(
                    value = countText,
                    onValueChange = {
                        countText = it.filter { ch -> ch.isDigit() }
                        if (it.isNotBlank()) hasAttemptedSubmit = false
                    },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                if (isAmal) "Recitations to Pledge"
                                else "Paras per Month"
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("*", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.ExtraBold)
                        }
                    },
                    isError = isCountError,
                    supportingText = {
                        if (isCountError) {
                            Text(
                                text = "Must be between 1 and $remainingCapacity ($remainingCapacity remaining)",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                            )
                        } else {
                            Text(
                                text = "Compulsory * • Up to $remainingCapacity remaining",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = if (isCountError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant,
                        errorBorderColor = MaterialTheme.colorScheme.error,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                val presets = if (isAmal) {
                    listOf(5, 10, 20, 25, 50, remainingCapacity).distinct().filter { it in 1..remainingCapacity }
                } else {
                    listOf(1, 2, 3, 5, 6).filter { it in 1..remainingCapacity }
                }

                if (presets.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presets.forEach { preset ->
                            FilterChip(
                                selected = parsedCount == preset,
                                onClick = { countText = preset.toString() },
                                label = {
                                    Text(
                                        if (preset == remainingCapacity && isAmal) "All ($preset)" else "+$preset"
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SecondaryEditorialButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    PrimarySanctuaryButton(
                        text = "Confirm",
                        icon = Icons.Default.Check,
                        onClick = {
                            if (!canConfirm) {
                                hasAttemptedSubmit = true
                            } else {
                                onConfirmClaim(activeName, parsedCount, isGuest)
                            }
                        },
                        modifier = Modifier.weight(1.2f)
                    )
                }
            }
        }
    }
}

/**
 * Dialog to Reopen a Custom Amal Room for a new round with the same Amal settings.
 * If it is an Online Room, generates a brand-new unique Room Passcode to share!
 */
@Composable
fun ReopenAmalRoomDialog(
    roomName: String,
    currentRound: Int,
    isOnlineRoom: Boolean,
    onConfirmReopen: (keepExistingMembers: Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var keepExistingMembers by remember { mutableStateOf(!isOnlineRoom) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Reopen \"$roomName\"?",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = if (isOnlineRoom) {
                        "Start Round ${currentRound + 1} with the exact same Amal target. A brand-new unique Room Passcode will be generated so you can share it with the same or new people!"
                    } else {
                        "Start Round ${currentRound + 1} with the exact same Amal target without having to recreate the distribution."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        onClick = { keepExistingMembers = false },
                        shape = RoundedCornerShape(14.dp),
                        color = if (!keepExistingMembers) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                        else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (!keepExistingMembers) 1.5.dp else 1.dp,
                            color = if (!keepExistingMembers) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Start Fresh from 0 Claims",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Clears old pledges so old or new members can join and claim from the start.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        onClick = { keepExistingMembers = true },
                        shape = RoundedCornerShape(14.dp),
                        color = if (keepExistingMembers) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                        else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            width = if (keepExistingMembers) 1.5.dp else 1.dp,
                            color = if (keepExistingMembers) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Keep Current Members & Reset Ticks",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Keeps the current member list and resets all completion checkmarks for the new round.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SecondaryEditorialButton(
                        text = "Cancel",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    PrimarySanctuaryButton(
                        text = "Reopen Round ${currentRound + 1}",
                        icon = Icons.Default.Refresh,
                        onClick = { onConfirmReopen(keepExistingMembers) },
                        modifier = Modifier.weight(1.3f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ClaimIdentityTab(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = modifier.height(44.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                ),
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Beautiful pop-out container displaying a scannable QR Code + Passcode for an Online Room,
 * with one-tap "Share QR Image" and "Copy Code" actions.
 */
@Composable
fun RoomQrCodeDialog(
    roomName: String,
    passcode: String,
    typeSubtitle: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val qrBitmap = remember(passcode, roomName) {
        RoomQrCodeHelper.generateQrCodeBitmap(
            passcode = passcode,
            roomName = roomName,
            sizePx = 640
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.3.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.6f)),
            tonalElevation = 8.dp,
            shadowElevation = 18.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 24.dp)
                .widthIn(max = 440.dp)
                .testTag("room_qr_code_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ROOM QR INVITE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    fontSize = 10.sp
                                ),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Surface(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close QR Code",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = roomName,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 21.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = typeSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                // Ornamental Manuscript Frame housing the QR Code
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFFFDFBF7),
                    border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)),
                    tonalElevation = 2.dp,
                    modifier = Modifier.size(236.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(14.dp)
                    ) {
                        if (qrBitmap != null) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "Online Room QR Code for $passcode",
                                modifier = Modifier
                                    .size(208.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                        } else {
                            Text(
                                text = passcode,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Passcode Pill Container
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "PASSCODE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.8.sp,
                                    fontSize = 10.sp
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = passcode,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Surface(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Room Passcode", passcode))
                                Toast.makeText(context, "Passcode copied", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy Passcode",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Copy",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SecondaryEditorialButton(
                        text = "Close",
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    )
                    PrimarySanctuaryButton(
                        text = "Share QR Image",
                        icon = Icons.Default.Share,
                        onClick = {
                            RoomQrCodeHelper.shareRoomQrCodeImage(
                                context = context,
                                roomName = roomName,
                                passcode = passcode,
                                typeSubtitle = typeSubtitle
                            )
                        },
                        modifier = Modifier
                            .weight(1.35f)
                            .testTag("share_qr_image_button")
                    )
                }
            }
        }
    }
}

@Composable
fun OnlineRemainingReminderDialog(
    roomName: String,
    memberName: String,
    remainingSummary: String,
    activeScheduleLabel: String?,
    onSelectPreset: (KhatmNotificationHelper.ReminderPreset) -> Unit,
    onCancelScheduled: () -> Unit,
    onDismiss: () -> Unit
) {
    val goldAccent = QuranBaantoDesignSystem.colors.gold
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.3.dp, goldAccent.copy(alpha = 0.6f)),
            tonalElevation = 8.dp,
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .widthIn(max = 480.dp)
                .testTag("schedule_due_reminder_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(goldAccent.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = goldAccent,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "My Remaining Portion Reminder",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Personal reminder for \"$roomName\"",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                }

                // Highlight the user's specific remaining Paras / Amals
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, goldAccent.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "YOUR REMAINING ASSIGNMENT (${memberName.ifBlank { "My Profile" }.uppercase()})",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.7.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = remainingSummary,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                if (!activeScheduleLabel.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Scheduled: $activeScheduleLabel",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Surface(
                                onClick = onCancelScheduled,
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Text(
                                    text = "Clear",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                KhatmNotificationHelper.ReminderPreset.entries.forEach { preset ->
                    Surface(
                        onClick = { onSelectPreset(preset) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = preset.label,
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = preset.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                SecondaryEditorialButton(
                    text = "Done",
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

