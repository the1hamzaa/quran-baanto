package com.example.ui.distribution

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.DistributionType
import com.example.data.db.RoomMode
import com.example.domain.ParticipantAllocation
import com.example.ui.components.WaveDivider
import com.example.ui.theme.FrauncesFontFamily
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.QuranBaantoDesignSystem
import com.example.ui.viewmodel.DistributionViewModel

/**
 * EXACT CREATE / EDIT DISTRIBUTION WIZARD & SETUP FORMS
 * Matches reference screenshots:
 * - Create___Room_type (1 of 2)
 * - Create___Distribution_type (2 of 2)
 * - Create___Local_one-time_setup
 * - Create___Local_monthly_setup
 * - Create___Local_amal_setup
 * - Create___Online_amal
 *
 * NO fake/placeholder data added; all lists start clean.
 */
@Composable
fun CreateEditDistributionScreen(
    distributionId: Long?,
    initialType: DistributionType = DistributionType.KHATM,
    initialRoomMode: RoomMode = RoomMode.LOCAL,
    distributionViewModel: DistributionViewModel,
    onSaved: (Long) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = QuranBaantoDesignSystem.colors

    // Wizard navigation steps:
    // 1 = Room Type (Online vs Local)
    // 2 = Distribution Type (Monthly Khatm, One-time, Amal)
    // 3 = Room Detail Setup (Local assignments or Online Amal setup or Online name dialog)
    var step by remember(distributionId) { mutableIntStateOf(if (distributionId != null) 3 else 1) }
    var selectedRoomMode by remember { mutableStateOf(initialRoomMode) }
    var selectedType by remember { mutableStateOf(initialType) }

    // Form inputs
    var roomTitle by remember { mutableStateOf("") }
    var totalTarget by remember { mutableIntStateOf(if (initialType == DistributionType.AMAL) 100 else 30) }

    // Local participant allocations
    val allocations = remember { mutableStateListOf<ParticipantAllocation>() }
    var newPersonName by remember { mutableStateOf("") }
    var newPersonCount by remember { mutableIntStateOf(1) }

    val activeState by distributionViewModel.activeDistributionState.collectAsState()
    var hasLoadedEditData by remember(distributionId) { mutableStateOf(false) }

    LaunchedEffect(distributionId) {
        if (distributionId != null) {
            distributionViewModel.selectDistribution(distributionId)
        }
    }

    LaunchedEffect(distributionId, activeState.distribution, activeState.participants) {
        if (distributionId != null && !hasLoadedEditData && activeState.distribution?.id == distributionId) {
            val dist = activeState.distribution!!
            selectedRoomMode = dist.roomMode
            selectedType = dist.type
            roomTitle = dist.name
            totalTarget = dist.totalTarget
            allocations.clear()

            val sortedParts = activeState.participants.sortedBy { it.displayOrder }
            if (dist.type == DistributionType.ONE_TIME_KHATM && dist.roomMode == RoomMode.LOCAL) {
                var idx = 0
                while (idx < sortedParts.size) {
                    val current = sortedParts[idx]
                    val paras = mutableListOf(current.specificParaNumber ?: current.displayOrder)
                    var nextIdx = idx + 1
                    while (nextIdx < sortedParts.size &&
                        sortedParts[nextIdx].name.equals(current.name, ignoreCase = true)
                    ) {
                        paras.add(sortedParts[nextIdx].specificParaNumber ?: sortedParts[nextIdx].displayOrder)
                        nextIdx++
                    }
                    allocations.add(
                        ParticipantAllocation(
                            id = current.id,
                            name = current.name,
                            count = paras.size,
                            displayOrder = allocations.size + 1,
                            specificParas = paras
                        )
                    )
                    idx = nextIdx
                }
            } else {
                sortedParts.forEachIndexed { idx, p ->
                    allocations.add(
                        ParticipantAllocation(
                            id = p.id,
                            name = p.name,
                            count = p.allocatedCount,
                            displayOrder = idx + 1,
                            specificParas = listOfNotNull(p.specificParaNumber)
                        )
                    )
                }
            }
            step = 3
            hasLoadedEditData = true
        }
    }

    BackHandler {
        when {
            distributionId != null -> onNavigateBack()
            step > 1 -> step -= 1
            else -> onNavigateBack()
        }
    }

    val totalAssigned = allocations.sumOf { it.count }
    val remainingToAssign = (totalTarget - totalAssigned).coerceAtLeast(0)
    val onStep3Back: () -> Unit = {
        if (distributionId != null) onNavigateBack() else step = 2
    }

    AnimatedContent(
        targetState = step,
        transitionSpec = {
            fadeIn(tween(180, easing = FastOutSlowInEasing)) togetherWith
                fadeOut(tween(140, easing = FastOutSlowInEasing))
        },
        label = "create_edit_wizard_step",
        modifier = modifier
    ) { activeStep ->
        when (activeStep) {
            1 -> {
                // STEP 1 OF 2: "Where will it take place?"
                Step1RoomType(
                    selectedMode = selectedRoomMode,
                    onSelectMode = { selectedRoomMode = it },
                    onContinue = { step = 2 },
                    onBack = onNavigateBack
                )
            }
            2 -> {
                // STEP 2 OF 2: "What will you recite?"
                Step2DistributionType(
                    roomMode = selectedRoomMode,
                    selectedType = selectedType,
                    onSelectType = { selectedType = it },
                    onChangeMode = { step = 1 },
                    onContinue = {
                        totalTarget = if (selectedRoomMode == RoomMode.LOCAL && selectedType == DistributionType.AMAL) {
                            100
                        } else if (selectedRoomMode == RoomMode.LOCAL) {
                            30
                        } else {
                            totalTarget
                        }
                        step = 3
                    },
                    onBack = { step = 1 }
                )
            }
            3 -> {
                if (selectedRoomMode == RoomMode.LOCAL) {
                    // Local Setup Form
                    when (selectedType) {
                        DistributionType.ONE_TIME_KHATM -> {
                            LocalOneTimeSetup(
                                title = roomTitle,
                                onTitleChange = { roomTitle = it },
                                allocations = allocations,
                                totalTarget = 30,
                                newPersonName = newPersonName,
                                onNewPersonNameChange = { newPersonName = it },
                                newPersonCount = newPersonCount,
                                onNewPersonCountChange = { newPersonCount = it },
                                onAddPerson = {
                                    if (newPersonName.isNotBlank() && newPersonCount > 0) {
                                        val startPara = totalAssigned + 1
                                        val endPara = (startPara + newPersonCount - 1).coerceAtMost(30)
                                        val paras = (startPara..endPara).toList()
                                        allocations.add(
                                            ParticipantAllocation(
                                                name = newPersonName.trim(),
                                                count = newPersonCount,
                                                specificParas = paras
                                            )
                                        )
                                        newPersonName = ""
                                        newPersonCount = 1
                                    }
                                },
                                onRemoveAllocation = { index ->
                                    if (index in allocations.indices) {
                                        allocations.removeAt(index)
                                    }
                                },
                                onCreateRoom = {
                                    val name = if (roomTitle.isNotBlank()) roomTitle.trim() else "One-Time Khatm"
                                    distributionViewModel.saveDistribution(
                                        name = name,
                                        type = DistributionType.ONE_TIME_KHATM,
                                        roomMode = RoomMode.LOCAL,
                                        totalTarget = 30,
                                        allocations = allocations,
                                        existingId = distributionId
                                    ) { savedId ->
                                        onSaved(savedId)
                                    }
                                },
                                onBack = onStep3Back
                            )
                        }
                        DistributionType.KHATM -> {
                            LocalMonthlySetup(
                                title = roomTitle,
                                onTitleChange = { roomTitle = it },
                                allocations = allocations,
                                totalTarget = 30,
                                newPersonName = newPersonName,
                                onNewPersonNameChange = { newPersonName = it },
                                newPersonCount = newPersonCount,
                                onNewPersonCountChange = { newPersonCount = it },
                                onAddPerson = {
                                    if (newPersonName.isNotBlank() && newPersonCount > 0) {
                                        val startPara = totalAssigned + 1
                                        val endPara = (startPara + newPersonCount - 1).coerceAtMost(30)
                                        val paras = (startPara..endPara).toList()
                                        allocations.add(
                                            ParticipantAllocation(
                                                name = newPersonName.trim(),
                                                count = newPersonCount,
                                                specificParas = paras
                                            )
                                        )
                                        newPersonName = ""
                                        newPersonCount = 1
                                    }
                                },
                                onRemoveAllocation = { index ->
                                    if (index in allocations.indices) {
                                        allocations.removeAt(index)
                                    }
                                },
                                onCreateRoom = {
                                    val name = if (roomTitle.isNotBlank()) roomTitle.trim() else "Monthly Khatm"
                                    distributionViewModel.saveDistribution(
                                        name = name,
                                        type = DistributionType.KHATM,
                                        roomMode = RoomMode.LOCAL,
                                        totalTarget = 30,
                                        allocations = allocations,
                                        existingId = distributionId
                                    ) { savedId ->
                                        onSaved(savedId)
                                    }
                                },
                                onBack = onStep3Back
                            )
                        }
                        DistributionType.AMAL -> {
                            LocalAmalSetup(
                                title = roomTitle,
                                onTitleChange = { roomTitle = it },
                                totalTarget = totalTarget,
                                onTotalTargetChange = { totalTarget = it },
                                allocations = allocations,
                                newPersonName = newPersonName,
                                onNewPersonNameChange = { newPersonName = it },
                                newPersonCount = newPersonCount,
                                onNewPersonCountChange = { newPersonCount = it },
                                onAddPerson = {
                                    if (newPersonName.isNotBlank() && newPersonCount > 0) {
                                        allocations.add(
                                            ParticipantAllocation(
                                                name = newPersonName.trim(),
                                                count = newPersonCount
                                            )
                                        )
                                        newPersonName = ""
                                        newPersonCount = 20.coerceAtMost(remainingToAssign)
                                    }
                                },
                                onRemoveAllocation = { index ->
                                    if (index in allocations.indices) {
                                        allocations.removeAt(index)
                                    }
                                },
                                onCreateRoom = {
                                    val name = if (roomTitle.isNotBlank()) roomTitle.trim() else "Custom Amal"
                                    distributionViewModel.saveDistribution(
                                        name = name,
                                        type = DistributionType.AMAL,
                                        roomMode = RoomMode.LOCAL,
                                        totalTarget = totalTarget,
                                        allocations = allocations,
                                        existingId = distributionId
                                    ) { savedId ->
                                        onSaved(savedId)
                                    }
                                },
                                onBack = onStep3Back
                            )
                        }
                    }
                } else {
                    // Online Room Setup
                    if (selectedType == DistributionType.AMAL) {
                        OnlineAmalSetup(
                            amalName = roomTitle,
                            onAmalNameChange = { roomTitle = it },
                            totalTarget = totalTarget,
                            onTotalTargetChange = { totalTarget = it },
                            onCreateRoom = {
                                val name = if (roomTitle.isNotBlank()) roomTitle.trim() else "Custom Amal"
                                distributionViewModel.saveDistribution(
                                    name = name,
                                    type = DistributionType.AMAL,
                                    roomMode = RoomMode.ONLINE,
                                    totalTarget = totalTarget,
                                    allocations = emptyList(),
                                    existingId = distributionId
                                ) { savedId ->
                                    onSaved(savedId)
                                }
                            },
                            onBack = onStep3Back
                        )
                    } else {
                        OnlineKhatmSetup(
                            title = roomTitle,
                            onTitleChange = { roomTitle = it },
                            type = selectedType,
                            onCreateRoom = {
                                val defaultName = if (selectedType == DistributionType.ONE_TIME_KHATM) "One-Time Khatm" else "Monthly Khatm"
                                val name = if (roomTitle.isNotBlank()) roomTitle.trim() else defaultName
                                distributionViewModel.saveDistribution(
                                    name = name,
                                    type = selectedType,
                                    roomMode = RoomMode.ONLINE,
                                    totalTarget = 30,
                                    allocations = emptyList(),
                                    existingId = distributionId
                                ) { savedId ->
                                    onSaved(savedId)
                                }
                            },
                            onBack = onStep3Back
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// STEP 1: "Where will it take place?" (1 of 2)
// =========================================================================
@Composable
private fun Step1RoomType(
    selectedMode: RoomMode,
    onSelectMode: (RoomMode) -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = QuranBaantoDesignSystem.colors

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        TopRightStepBubble(softNeutral = colors.softNeutral)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 28.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.text
                        )
                    }

                    Text(
                        text = "1 of 2",
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        color = colors.mutedText,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "NEW DISTRIBUTION",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.1.sp,
                    color = colors.mutedText
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Where will it\ntake place?",
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 36.sp,
                    lineHeight = 42.sp,
                    letterSpacing = (-0.5).sp,
                    color = colors.text
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Option: Online room
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelectMode(RoomMode.ONLINE) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OverlappingCirclesVector(
                        modifier = Modifier.size(width = 68.dp, height = 54.dp),
                        colors = colors
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Online room",
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 22.sp,
                            color = colors.text
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Share a code. Friends join from anywhere.",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            color = colors.mutedText
                        )
                    }

                    if (selectedMode == RoomMode.ONLINE) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = colors.pineText,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                WaveDivider(
                    color = colors.gold,
                    strokeWidth = 1.2.dp,
                    amplitude = 2.dp,
                    wavelength = 44.dp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Option: Local room
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelectMode(RoomMode.LOCAL) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ArchWindowVector(
                        modifier = Modifier.size(width = 54.dp, height = 64.dp),
                        outlineColor = colors.gold,
                        strokeWidth = 1.6.dp
                    )

                    Spacer(modifier = Modifier.width(22.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Local room",
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 22.sp,
                            color = colors.text
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "For a masjid or gathering. Paras are claimed in person.",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            color = colors.mutedText
                        )
                    }

                    if (selectedMode == RoomMode.LOCAL) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = colors.pineText,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                WaveDivider(
                    color = colors.rules,
                    strokeWidth = 1.dp,
                    amplitude = 1.5.dp,
                    wavelength = 44.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Continue Button
            Button(
                onClick = onContinue,
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.pine,
                    contentColor = Color(0xFFF1EBDD)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("step1_continue_button")
            ) {
                Text(
                    text = "Continue →",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

// =========================================================================
// STEP 2: "What will you recite?" (2 of 2)
// =========================================================================
@Composable
private fun Step2DistributionType(
    roomMode: RoomMode,
    selectedType: DistributionType,
    onSelectType: (DistributionType) -> Unit,
    onChangeMode: () -> Unit,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = QuranBaantoDesignSystem.colors
    val modeLabel = if (roomMode == RoomMode.ONLINE) "Online room" else "Local room"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        TopRightStepBubble(softNeutral = colors.softNeutral)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 28.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.text
                        )
                    }

                    Text(
                        text = "2 of 2",
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        color = colors.mutedText,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "NEW DISTRIBUTION",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.1.sp,
                    color = colors.mutedText
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "What will you\nrecite?",
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 36.sp,
                    lineHeight = 42.sp,
                    letterSpacing = (-0.5).sp,
                    color = colors.text
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "$modeLabel · ",
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        color = colors.mutedText
                    )
                    Text(
                        text = "change",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        textDecoration = TextDecoration.Underline,
                        color = colors.pineText,
                        modifier = Modifier.clickable { onChangeMode() }
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))

                // Option 1: Monthly Quran khatm
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelectType(DistributionType.KHATM) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BeadedCircleVector(
                        modifier = Modifier.size(60.dp),
                        totalBeads = 30,
                        completedBeads = 0,
                        goldColor = colors.gold,
                        pineColor = colors.pine
                    )

                    Spacer(modifier = Modifier.width(18.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Monthly Quran khatm",
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 21.sp,
                            color = colors.text
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "A new 30-para round every month, same group.",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            color = colors.mutedText
                        )
                    }

                    if (selectedType == DistributionType.KHATM) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = colors.pineText,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                WaveDivider(
                    color = colors.rules,
                    strokeWidth = 1.dp,
                    amplitude = 1.5.dp,
                    wavelength = 44.dp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Option 2: One-time Quran
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelectType(DistributionType.ONE_TIME_KHATM) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ArchWindowVector(
                        modifier = Modifier.size(width = 54.dp, height = 64.dp),
                        outlineColor = colors.gold,
                        strokeWidth = 1.6.dp
                    )

                    Spacer(modifier = Modifier.width(22.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "One-time Quran",
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 21.sp,
                            color = colors.text
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "One 30-para khatm, then the room closes.",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            color = colors.mutedText
                        )
                    }

                    if (selectedType == DistributionType.ONE_TIME_KHATM) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = colors.pineText,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                WaveDivider(
                    color = colors.gold,
                    strokeWidth = 1.2.dp,
                    amplitude = 2.dp,
                    wavelength = 44.dp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Option 3: Amal
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onSelectType(DistributionType.AMAL) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ConcentricCirclesVector(
                        modifier = Modifier.size(60.dp),
                        pineColor = colors.pine,
                        strokeColor = colors.gold
                    )

                    Spacer(modifier = Modifier.width(18.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Amal",
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 21.sp,
                            color = colors.text
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = "Count recitations toward a target, like Yaseen × 100.",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            color = colors.mutedText
                        )
                    }

                    if (selectedType == DistributionType.AMAL) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = colors.pineText,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                WaveDivider(
                    color = colors.rules,
                    strokeWidth = 1.dp,
                    amplitude = 1.5.dp,
                    wavelength = 44.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Create Room / Continue Button
            Button(
                onClick = onContinue,
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.pine,
                    contentColor = Color(0xFFF1EBDD)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("step2_continue_button")
            ) {
                Text(
                    text = "Create room →",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

// =========================================================================
// LOCAL SETUP: One-Time Khatm ("Set up the assignments")
// =========================================================================
@Composable
private fun LocalOneTimeSetup(
    title: String,
    onTitleChange: (String) -> Unit,
    allocations: List<ParticipantAllocation>,
    totalTarget: Int,
    newPersonName: String,
    onNewPersonNameChange: (String) -> Unit,
    newPersonCount: Int,
    onNewPersonCountChange: (Int) -> Unit,
    onAddPerson: () -> Unit,
    onRemoveAllocation: (Int) -> Unit,
    onCreateRoom: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = QuranBaantoDesignSystem.colors
    val totalAssigned = allocations.sumOf { it.count }
    val remaining = (totalTarget - totalAssigned).coerceAtLeast(0)
    val isComplete = totalAssigned == totalTarget

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.text
                        )
                    }

                    Text(
                        text = "LOCAL",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = colors.mutedText
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Set up the\nassignments",
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 36.sp,
                    lineHeight = 42.sp,
                    letterSpacing = (-0.5).sp,
                    color = colors.text
                )

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "ONE-TIME KHATM TITLE",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.1.sp,
                    color = colors.mutedText
                )

                Spacer(modifier = Modifier.height(8.dp))

                BasicTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    textStyle = TextStyle(
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 24.sp,
                        color = colors.text
                    ),
                    cursorBrush = SolidColor(colors.gold),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (title.isEmpty()) {
                            Text(
                                text = "e.g. Khatm for Family",
                                fontFamily = FrauncesFontFamily,
                                fontSize = 24.sp,
                                color = colors.mutedText.copy(alpha = 0.6f)
                            )
                        }
                        innerTextField()
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                WaveDivider(
                    color = colors.gold,
                    strokeWidth = 1.6.dp,
                    amplitude = 2.dp,
                    wavelength = 44.dp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "A descriptive name for your group",
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    color = colors.mutedText
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Progress line: 5 / 30 paras assigned, 25 remaining
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$totalAssigned / $totalTarget",
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 38.sp,
                            color = colors.text
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "paras assigned",
                            fontFamily = InterFontFamily,
                            fontSize = 14.sp,
                            color = colors.mutedText,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Text(
                        text = "$remaining remaining",
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        color = colors.mutedText,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Wavy progress track
                WaveTrack(
                    progress = (totalAssigned.toFloat() / totalTarget.toFloat()).coerceIn(0f, 1f),
                    pineColor = colors.pine,
                    trackColor = colors.rules
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "ADDED SO FAR",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.1.sp,
                    color = colors.mutedText
                )
            }

            // Real participants added so far (clean empty state if none yet)
            if (allocations.isEmpty()) {
                item {
                    Text(
                        text = "No participants added yet. Enter names below to assign portions.",
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        color = colors.mutedText,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                itemsIndexed(allocations) { index, alloc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = alloc.name,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = colors.text
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            val paraRange = if (alloc.specificParas.isNotEmpty()) {
                                "Paras ${alloc.specificParas.first()}–${alloc.specificParas.last()}"
                            } else "${alloc.count} paras"
                            Text(
                                text = "${alloc.count} paras · $paraRange",
                                fontFamily = InterFontFamily,
                                fontSize = 13.sp,
                                color = colors.mutedText
                            )
                        }

                        IconButton(
                            onClick = { onRemoveAllocation(index) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = colors.mutedText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // NAME A PERSON Section
            item {
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "NAME A PERSON",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.1.sp,
                    color = colors.mutedText
                )

                Spacer(modifier = Modifier.height(8.dp))

                BasicTextField(
                    value = newPersonName,
                    onValueChange = onNewPersonNameChange,
                    textStyle = TextStyle(
                        fontFamily = InterFontFamily,
                        fontSize = 16.sp,
                        color = colors.text
                    ),
                    cursorBrush = SolidColor(colors.gold),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (newPersonName.isEmpty()) {
                            Text(
                                text = "Person's name",
                                fontFamily = InterFontFamily,
                                fontSize = 16.sp,
                                color = colors.mutedText.copy(alpha = 0.6f)
                            )
                        }
                        innerTextField()
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                WaveDivider(
                    color = colors.gold,
                    strokeWidth = 1.4.dp,
                    amplitude = 2.dp,
                    wavelength = 44.dp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Quick choice pills: 1 Para, 2 Paras, 3 Paras, 5 Paras, Remaining (X)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 2, 3, 5).forEach { count ->
                        val isSelected = newPersonCount == count
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isSelected) colors.pine else Color.Transparent)
                                .border(1.2.dp, if (isSelected) colors.pine else colors.gold, RoundedCornerShape(18.dp))
                                .clickable { onNewPersonCountChange(count) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "$count ${if (count == 1) "Para" else "Paras"}",
                                fontFamily = InterFontFamily,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                                color = if (isSelected) Color(0xFFF1EBDD) else colors.text
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (remaining > 0) {
                    val isRemainingSelected = newPersonCount == remaining
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isRemainingSelected) colors.pine else Color.Transparent)
                            .border(1.2.dp, if (isRemainingSelected) colors.pine else colors.gold, RoundedCornerShape(18.dp))
                            .clickable { onNewPersonCountChange(remaining) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Remaining ($remaining)",
                            fontFamily = InterFontFamily,
                            fontWeight = if (isRemainingSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                            color = if (isRemainingSelected) Color(0xFFF1EBDD) else colors.text
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Add person to schedule + button (camel/gold pill)
                Button(
                    onClick = onAddPerson,
                    enabled = newPersonName.isNotBlank() && remaining > 0,
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.gold,
                        contentColor = Color(0xFF1B1B19),
                        disabledContainerColor = colors.gold.copy(alpha = 0.35f),
                        disabledContentColor = Color(0xFF1B1B19).copy(alpha = 0.45f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = "Add person to schedule +",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "$totalAssigned / $totalTarget paras named · ${allocations.size} people",
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    color = colors.mutedText
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Create room button
                Button(
                    onClick = onCreateRoom,
                    enabled = isComplete,
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.pine,
                        contentColor = Color(0xFFF1EBDD),
                        disabledContainerColor = colors.softNeutral.copy(alpha = 0.7f),
                        disabledContentColor = colors.mutedText.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "Create room →",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

// =========================================================================
// LOCAL SETUP: Monthly Khatm ("Set up the schedule")
// =========================================================================
@Composable
private fun LocalMonthlySetup(
    title: String,
    onTitleChange: (String) -> Unit,
    allocations: List<ParticipantAllocation>,
    totalTarget: Int,
    newPersonName: String,
    onNewPersonNameChange: (String) -> Unit,
    newPersonCount: Int,
    onNewPersonCountChange: (Int) -> Unit,
    onAddPerson: () -> Unit,
    onRemoveAllocation: (Int) -> Unit,
    onCreateRoom: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = QuranBaantoDesignSystem.colors
    val totalAssigned = allocations.sumOf { it.count }
    val remaining = (totalTarget - totalAssigned).coerceAtLeast(0)
    val isComplete = totalAssigned == totalTarget

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.text
                        )
                    }

                    Text(
                        text = "LOCAL",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = colors.mutedText
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Set up the\nschedule",
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 36.sp,
                    lineHeight = 42.sp,
                    letterSpacing = (-0.5).sp,
                    color = colors.text
                )

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "MONTHLY KHATM TITLE",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.1.sp,
                    color = colors.mutedText
                )

                Spacer(modifier = Modifier.height(8.dp))

                BasicTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    textStyle = TextStyle(
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 24.sp,
                        color = colors.text
                    ),
                    cursorBrush = SolidColor(colors.gold),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (title.isEmpty()) {
                            Text(
                                text = "e.g. Family Khatm",
                                fontFamily = FrauncesFontFamily,
                                fontSize = 24.sp,
                                color = colors.mutedText.copy(alpha = 0.6f)
                            )
                        }
                        innerTextField()
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                WaveDivider(
                    color = colors.gold,
                    strokeWidth = 1.6.dp,
                    amplitude = 2.dp,
                    wavelength = 44.dp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "A descriptive name for your group",
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    color = colors.mutedText
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$totalAssigned / $totalTarget",
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 38.sp,
                            color = colors.text
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "paras assigned",
                            fontFamily = InterFontFamily,
                            fontSize = 14.sp,
                            color = colors.mutedText,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Text(
                        text = "$remaining remaining",
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        color = colors.mutedText,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                WaveTrack(
                    progress = (totalAssigned.toFloat() / totalTarget.toFloat()).coerceIn(0f, 1f),
                    pineColor = colors.pine,
                    trackColor = colors.rules
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "ADDED SO FAR",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.1.sp,
                    color = colors.mutedText
                )
            }

            if (allocations.isEmpty()) {
                item {
                    Text(
                        text = "No participants added yet. Enter names below to assign portions.",
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        color = colors.mutedText,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                itemsIndexed(allocations) { index, alloc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = alloc.name,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = colors.text
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            val paraRange = if (alloc.specificParas.isNotEmpty()) {
                                "Paras ${alloc.specificParas.first()}–${alloc.specificParas.last()}"
                            } else "${alloc.count} paras"
                            Text(
                                text = "${alloc.count} paras · $paraRange",
                                fontFamily = InterFontFamily,
                                fontSize = 13.sp,
                                color = colors.mutedText
                            )
                        }

                        IconButton(
                            onClick = { onRemoveAllocation(index) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = colors.mutedText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "NAME A PERSON",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.1.sp,
                    color = colors.mutedText
                )

                Spacer(modifier = Modifier.height(8.dp))

                BasicTextField(
                    value = newPersonName,
                    onValueChange = onNewPersonNameChange,
                    textStyle = TextStyle(
                        fontFamily = InterFontFamily,
                        fontSize = 16.sp,
                        color = colors.text
                    ),
                    cursorBrush = SolidColor(colors.gold),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (newPersonName.isEmpty()) {
                            Text(
                                text = "Person's name",
                                fontFamily = InterFontFamily,
                                fontSize = 16.sp,
                                color = colors.mutedText.copy(alpha = 0.6f)
                            )
                        }
                        innerTextField()
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                WaveDivider(
                    color = colors.gold,
                    strokeWidth = 1.4.dp,
                    amplitude = 2.dp,
                    wavelength = 44.dp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 2, 3, 5).forEach { count ->
                        val isSelected = newPersonCount == count
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isSelected) colors.pine else Color.Transparent)
                                .border(1.2.dp, if (isSelected) colors.pine else colors.gold, RoundedCornerShape(18.dp))
                                .clickable { onNewPersonCountChange(count) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "$count ${if (count == 1) "Para" else "Paras"}",
                                fontFamily = InterFontFamily,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                                color = if (isSelected) Color(0xFFF1EBDD) else colors.text
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (remaining > 0) {
                    val isRemainingSelected = newPersonCount == remaining
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isRemainingSelected) colors.pine else Color.Transparent)
                            .border(1.2.dp, if (isRemainingSelected) colors.pine else colors.gold, RoundedCornerShape(18.dp))
                            .clickable { onNewPersonCountChange(remaining) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Remaining ($remaining)",
                            fontFamily = InterFontFamily,
                            fontWeight = if (isRemainingSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                            color = if (isRemainingSelected) Color(0xFFF1EBDD) else colors.text
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onAddPerson,
                    enabled = newPersonName.isNotBlank() && remaining > 0,
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.gold,
                        contentColor = Color(0xFF1B1B19),
                        disabledContainerColor = colors.gold.copy(alpha = 0.35f),
                        disabledContentColor = Color(0xFF1B1B19).copy(alpha = 0.45f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = "Add person to schedule +",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "$totalAssigned / $totalTarget paras named · ${allocations.size} people",
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    color = colors.mutedText
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onCreateRoom,
                    enabled = isComplete,
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.pine,
                        contentColor = Color(0xFFF1EBDD),
                        disabledContainerColor = colors.softNeutral.copy(alpha = 0.7f),
                        disabledContentColor = colors.mutedText.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "Create schedule →",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

// =========================================================================
// LOCAL SETUP: Amal List ("Set up the amal list")
// =========================================================================
@Composable
private fun LocalAmalSetup(
    title: String,
    onTitleChange: (String) -> Unit,
    totalTarget: Int,
    onTotalTargetChange: (Int) -> Unit,
    allocations: List<ParticipantAllocation>,
    newPersonName: String,
    onNewPersonNameChange: (String) -> Unit,
    newPersonCount: Int,
    onNewPersonCountChange: (Int) -> Unit,
    onAddPerson: () -> Unit,
    onRemoveAllocation: (Int) -> Unit,
    onCreateRoom: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = QuranBaantoDesignSystem.colors
    val totalAssigned = allocations.sumOf { it.count }
    val remaining = (totalTarget - totalAssigned).coerceAtLeast(0)
    val isComplete = totalAssigned == totalTarget

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.text
                        )
                    }

                    Text(
                        text = "LOCAL",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp,
                        color = colors.mutedText
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Set up the\namal list",
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 36.sp,
                    lineHeight = 42.sp,
                    letterSpacing = (-0.5).sp,
                    color = colors.text
                )

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "AMAL TITLE",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.1.sp,
                    color = colors.mutedText
                )

                Spacer(modifier = Modifier.height(8.dp))

                BasicTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    textStyle = TextStyle(
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 24.sp,
                        color = colors.text
                    ),
                    cursorBrush = SolidColor(colors.gold),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (title.isEmpty()) {
                            Text(
                                text = "e.g. Friday Salawat",
                                fontFamily = FrauncesFontFamily,
                                fontSize = 24.sp,
                                color = colors.mutedText.copy(alpha = 0.6f)
                            )
                        }
                        innerTextField()
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                WaveDivider(
                    color = colors.gold,
                    strokeWidth = 1.6.dp,
                    amplitude = 2.dp,
                    wavelength = 44.dp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "A descriptive name for your group",
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    color = colors.mutedText
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "TOTAL TARGET RECITATIONS",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.1.sp,
                    color = colors.mutedText
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = totalTarget.toString(),
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 44.sp,
                        color = colors.text
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(40, 100, 110, 313).forEach { preset ->
                            val isSelected = totalTarget == preset
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (isSelected) colors.pine else Color.Transparent)
                                    .border(1.2.dp, if (isSelected) colors.pine else colors.gold, RoundedCornerShape(18.dp))
                                    .clickable { onTotalTargetChange(preset) }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = preset.toString(),
                                    fontFamily = InterFontFamily,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = if (isSelected) Color(0xFFF1EBDD) else colors.text
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "$totalAssigned / $totalTarget",
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 38.sp,
                            color = colors.text
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "recitations assigned",
                            fontFamily = InterFontFamily,
                            fontSize = 14.sp,
                            color = colors.mutedText,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Text(
                        text = "$remaining remaining",
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        color = colors.mutedText,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                WaveTrack(
                    progress = (totalAssigned.toFloat() / totalTarget.toFloat()).coerceIn(0f, 1f),
                    pineColor = colors.pine,
                    trackColor = colors.rules
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "ADDED SO FAR",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.1.sp,
                    color = colors.mutedText
                )
            }

            if (allocations.isEmpty()) {
                item {
                    Text(
                        text = "No participants added yet. Enter names below to assign portions.",
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        color = colors.mutedText,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                }
            } else {
                itemsIndexed(allocations) { index, alloc ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = alloc.name,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = colors.text
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Text(
                                text = "${alloc.count} recitations",
                                fontFamily = InterFontFamily,
                                fontSize = 13.sp,
                                color = colors.mutedText
                            )
                        }

                        IconButton(
                            onClick = { onRemoveAllocation(index) },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Remove",
                                tint = colors.mutedText,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "NAME A PERSON",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.1.sp,
                    color = colors.mutedText
                )

                Spacer(modifier = Modifier.height(8.dp))

                BasicTextField(
                    value = newPersonName,
                    onValueChange = onNewPersonNameChange,
                    textStyle = TextStyle(
                        fontFamily = InterFontFamily,
                        fontSize = 16.sp,
                        color = colors.text
                    ),
                    cursorBrush = SolidColor(colors.gold),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (newPersonName.isEmpty()) {
                            Text(
                                text = "Person's name",
                                fontFamily = InterFontFamily,
                                fontSize = 16.sp,
                                color = colors.mutedText.copy(alpha = 0.6f)
                            )
                        }
                        innerTextField()
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                WaveDivider(
                    color = colors.gold,
                    strokeWidth = 1.4.dp,
                    amplitude = 2.dp,
                    wavelength = 44.dp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(10, 20, 25, 50).forEach { count ->
                        val isSelected = newPersonCount == count
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isSelected) colors.pine else Color.Transparent)
                                .border(1.2.dp, if (isSelected) colors.pine else colors.gold, RoundedCornerShape(18.dp))
                                .clickable { onNewPersonCountChange(count) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = count.toString(),
                                fontFamily = InterFontFamily,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                                color = if (isSelected) Color(0xFFF1EBDD) else colors.text
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (remaining > 0) {
                    val isRemainingSelected = newPersonCount == remaining
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (isRemainingSelected) colors.pine else Color.Transparent)
                            .border(1.2.dp, if (isRemainingSelected) colors.pine else colors.gold, RoundedCornerShape(18.dp))
                            .clickable { onNewPersonCountChange(remaining) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Remaining ($remaining)",
                            fontFamily = InterFontFamily,
                            fontWeight = if (isRemainingSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                            color = if (isRemainingSelected) Color(0xFFF1EBDD) else colors.text
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onAddPerson,
                    enabled = newPersonName.isNotBlank() && remaining > 0,
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.gold,
                        contentColor = Color(0xFF1B1B19),
                        disabledContainerColor = colors.gold.copy(alpha = 0.35f),
                        disabledContentColor = Color(0xFF1B1B19).copy(alpha = 0.45f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = "Add person to schedule +",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "$totalAssigned / $totalTarget recitations named · ${allocations.size} people",
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    color = colors.mutedText
                )

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onCreateRoom,
                    enabled = isComplete,
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.pine,
                        contentColor = Color(0xFFF1EBDD),
                        disabledContainerColor = colors.softNeutral.copy(alpha = 0.7f),
                        disabledContentColor = colors.mutedText.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "Create room →",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

// =========================================================================
// ONLINE AMAL SETUP: "Custom Amal target"
// =========================================================================
@Composable
private fun OnlineAmalSetup(
    amalName: String,
    onAmalNameChange: (String) -> Unit,
    totalTarget: Int,
    onTotalTargetChange: (Int) -> Unit,
    onCreateRoom: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = QuranBaantoDesignSystem.colors

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        TopRightStepBubble(softNeutral = colors.softNeutral)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 28.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.text
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ONLINE",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            color = colors.mutedText
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(colors.gold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "SETUP ONLINE ROOM",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.1.sp,
                    color = colors.mutedText
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Custom Amal\ntarget",
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 36.sp,
                    lineHeight = 42.sp,
                    letterSpacing = (-0.5).sp,
                    color = colors.text
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Custom Amal Target Distribution · ",
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        color = colors.mutedText
                    )
                    Text(
                        text = "Change",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        textDecoration = TextDecoration.Underline,
                        color = colors.pineText,
                        modifier = Modifier.clickable { onBack() }
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "Name your room and create it. You'll get a passcode and QR code so members can join from their own phones and pledge their own recitations.",
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = colors.mutedText
                )

                Spacer(modifier = Modifier.height(30.dp))

                Text(
                    text = "Amal name*",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = colors.text
                )

                Spacer(modifier = Modifier.height(6.dp))

                BasicTextField(
                    value = amalName,
                    onValueChange = onAmalNameChange,
                    textStyle = TextStyle(
                        fontFamily = InterFontFamily,
                        fontSize = 18.sp,
                        color = colors.text
                    ),
                    cursorBrush = SolidColor(colors.gold),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (amalName.isEmpty()) {
                            Text(
                                text = "e.g. Friday Salawat",
                                fontFamily = InterFontFamily,
                                fontSize = 18.sp,
                                color = colors.mutedText.copy(alpha = 0.5f)
                            )
                        }
                        innerTextField()
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                WaveDivider(
                    color = colors.gold,
                    strokeWidth = 1.4.dp,
                    amplitude = 2.dp,
                    wavelength = 44.dp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Compulsory · a descriptive name for your group",
                    fontFamily = InterFontFamily,
                    fontSize = 12.sp,
                    color = colors.mutedText
                )

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "Total target recitations",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = colors.text
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = totalTarget.toString(),
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 52.sp,
                    letterSpacing = (-1).sp,
                    color = colors.text
                )

                Spacer(modifier = Modifier.height(6.dp))

                WaveDivider(
                    color = colors.gold,
                    strokeWidth = 1.6.dp,
                    amplitude = 2.dp,
                    wavelength = 44.dp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Presets",
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        color = colors.mutedText
                    )

                    listOf(40, 100, 110, 313).forEach { preset ->
                        val isSelected = totalTarget == preset
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (isSelected) colors.pine else Color.Transparent)
                                .border(1.2.dp, if (isSelected) colors.pine else colors.gold, RoundedCornerShape(18.dp))
                                .clickable { onTotalTargetChange(preset) }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = preset.toString(),
                                fontFamily = InterFontFamily,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp,
                                color = if (isSelected) Color(0xFFF1EBDD) else colors.text
                            )
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                if (amalName.isBlank()) {
                    Text(
                        text = "Room title required",
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp,
                        color = colors.mutedText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Button(
                    onClick = onCreateRoom,
                    enabled = amalName.isNotBlank(),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.pine,
                        contentColor = Color(0xFFF1EBDD),
                        disabledContainerColor = colors.softNeutral.copy(alpha = 0.7f),
                        disabledContentColor = colors.mutedText.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "Create online room →",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

// =========================================================================
// ONLINE KHATM SETUP: One-Time / Monthly Title input
// =========================================================================
@Composable
private fun OnlineKhatmSetup(
    title: String,
    onTitleChange: (String) -> Unit,
    type: DistributionType,
    onCreateRoom: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = QuranBaantoDesignSystem.colors
    val typeName = if (type == DistributionType.ONE_TIME_KHATM) "One-time Quran" else "Monthly Quran khatm"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        TopRightStepBubble(softNeutral = colors.softNeutral)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 28.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.text
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "ONLINE",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            color = colors.mutedText
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(colors.gold)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                Text(
                    text = "SETUP ONLINE ROOM",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.1.sp,
                    color = colors.mutedText
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Name your\nroom",
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 36.sp,
                    lineHeight = 42.sp,
                    letterSpacing = (-0.5).sp,
                    color = colors.text
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "$typeName · 30 paras",
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    color = colors.mutedText
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "You'll receive a room code and QR code. Members can join from their devices and claim their own paras.",
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = colors.mutedText
                )

                Spacer(modifier = Modifier.height(36.dp))

                Text(
                    text = "Room name*",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = colors.text
                )

                Spacer(modifier = Modifier.height(6.dp))

                BasicTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    textStyle = TextStyle(
                        fontFamily = InterFontFamily,
                        fontSize = 18.sp,
                        color = colors.text
                    ),
                    cursorBrush = SolidColor(colors.gold),
                    singleLine = true,
                    decorationBox = { innerTextField ->
                        if (title.isEmpty()) {
                            Text(
                                text = "e.g. Ramadan Khatm",
                                fontFamily = InterFontFamily,
                                fontSize = 18.sp,
                                color = colors.mutedText.copy(alpha = 0.5f)
                            )
                        }
                        innerTextField()
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(6.dp))

                WaveDivider(
                    color = colors.gold,
                    strokeWidth = 1.4.dp,
                    amplitude = 2.dp,
                    wavelength = 44.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                if (title.isBlank()) {
                    Text(
                        text = "Room title required",
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp,
                        color = colors.mutedText,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Button(
                    onClick = onCreateRoom,
                    enabled = title.isNotBlank(),
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.pine,
                        contentColor = Color(0xFFF1EBDD),
                        disabledContainerColor = colors.softNeutral.copy(alpha = 0.7f),
                        disabledContentColor = colors.mutedText.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "Create online room →",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TopRightStepBubble(softNeutral: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        val w = size.width
        val circleCenter = Offset(w - 10.dp.toPx(), 20.dp.toPx())
        val circleRadius = 75.dp.toPx()

        drawCircle(
            color = softNeutral,
            radius = circleRadius,
            center = circleCenter
        )
    }
}
