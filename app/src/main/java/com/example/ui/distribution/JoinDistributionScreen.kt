package com.example.ui.distribution

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.QrCodeScanner
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.db.DistributionEntity
import com.example.data.db.DistributionType
import com.example.ui.components.WaveDivider
import com.example.ui.theme.FrauncesFontFamily
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.QuranBaantoDesignSystem
import com.example.ui.viewmodel.DistributionViewModel

/**
 * EXACT JOIN DISTRIBUTION SCREEN
 * Matches Join_distribution_acccae47.png and Join_distribution___dark_6b0ef56d.png precisely.
 */
@Composable
fun JoinDistributionScreen(
    distributionViewModel: DistributionViewModel,
    onJoined: (Long) -> Unit,
    onBack: () -> Unit,
    onScanQrCode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    androidx.activity.compose.BackHandler(enabled = true) { onBack() }
    val colors = QuranBaantoDesignSystem.colors
    val distributions by distributionViewModel.distributions.collectAsState()
    val userProfileName by distributionViewModel.userProfileName.collectAsState()

    var codeInput by remember { mutableStateOf("") }
    val normalizedCode = codeInput.trim().uppercase()

    // Find if the room is locally known or has matching passcode format
    val matchedLocalRoom = remember(normalizedCode, distributions) {
        if (normalizedCode.length >= 6) {
            distributions.firstOrNull { it.roomPasscode?.equals(normalizedCode, ignoreCase = true) == true }
        } else null
    }

    val isValidCodeFormat = normalizedCode.length >= 6 && (
        normalizedCode.startsWith("QBO-") ||
        normalizedCode.startsWith("QBM-") ||
        normalizedCode.startsWith("QBA-") ||
        normalizedCode.contains("-")
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Soft neutral circle cropped at top right
        TopRightJoinBubble(softNeutral = colors.softNeutral)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 28.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Back arrow
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

                Spacer(modifier = Modifier.height(36.dp))

                Text(
                    text = "JOIN",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.1.sp,
                    color = colors.mutedText
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Enter the\nroom code",
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 36.sp,
                    lineHeight = 42.sp,
                    letterSpacing = (-0.5).sp,
                    color = colors.text
                )

                Spacer(modifier = Modifier.height(36.dp))

                // Big bold code input with wave underline
                BasicTextField(
                    value = codeInput,
                    onValueChange = { codeInput = it.uppercase() },
                    textStyle = TextStyle(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        letterSpacing = 2.sp,
                        color = colors.text
                    ),
                    cursorBrush = SolidColor(colors.gold),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("join_room_code_input")
                )

                Spacer(modifier = Modifier.height(6.dp))

                WaveDivider(
                    color = colors.gold,
                    strokeWidth = 1.6.dp,
                    amplitude = 2.dp,
                    wavelength = 44.dp,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Codes are not case-sensitive.",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = colors.mutedText
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Scan QR code instead button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onScanQrCode() }
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .border(1.2.dp, colors.gold, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Scan QR Code",
                            tint = colors.text,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Text(
                        text = "Scan a QR code instead",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        textDecoration = TextDecoration.Underline,
                        color = colors.text
                    )
                }

                // ROOM FOUND Section
                if (matchedLocalRoom != null || isValidCodeFormat) {
                    Spacer(modifier = Modifier.height(40.dp))

                    Text(
                        text = "ROOM FOUND",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        letterSpacing = 1.1.sp,
                        color = colors.mutedText
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    val previewTitle = matchedLocalRoom?.name ?: when {
                        normalizedCode.startsWith("QBO-") -> "One-Time Khatm Room"
                        normalizedCode.startsWith("QBM-") -> "Monthly Khatm Room"
                        normalizedCode.startsWith("QBA-") -> "Amal Recitation Room"
                        else -> "Shared Room ($normalizedCode)"
                    }

                    val previewType = when (matchedLocalRoom?.type) {
                        DistributionType.AMAL -> "Online · Amal recitation"
                        DistributionType.KHATM -> "Online · Monthly khatm"
                        else -> "Online · One-time khatm"
                    }

                    val previewStatus = if (matchedLocalRoom != null) {
                        "${matchedLocalRoom.totalTarget} portions target"
                    } else "Ready to join"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ArchWindowVector(
                            modifier = Modifier.size(width = 44.dp, height = 52.dp),
                            outlineColor = colors.gold,
                            strokeWidth = 1.4.dp
                        )

                        Spacer(modifier = Modifier.width(18.dp))

                        Column {
                            Text(
                                text = previewTitle,
                                fontFamily = FrauncesFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 20.sp,
                                color = colors.text
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = previewType,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                                color = colors.mutedText
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = previewStatus,
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp,
                                color = colors.mutedText
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    WaveDivider(
                        color = colors.rules,
                        strokeWidth = 1.dp,
                        amplitude = 1.5.dp,
                        wavelength = 40.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Bottom Action
            Button(
                onClick = {
                    if (isValidCodeFormat || matchedLocalRoom != null) {
                        distributionViewModel.joinRoomByPasscode(
                            rawInput = normalizedCode,
                            customRoomTitle = null,
                            userNameToSave = userProfileName
                        ) { joinedId ->
                            if (joinedId != null) {
                                onJoined(joinedId)
                            }
                        }
                    }
                },
                enabled = isValidCodeFormat || matchedLocalRoom != null,
                shape = RoundedCornerShape(26.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.pine,
                    contentColor = Color(0xFFF1EBDD),
                    disabledContainerColor = colors.pine.copy(alpha = 0.35f),
                    disabledContentColor = Color(0xFFF1EBDD).copy(alpha = 0.45f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("join_room_submit_button")
            ) {
                Text(
                    text = "Join room →",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
private fun TopRightJoinBubble(softNeutral: Color) {
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
