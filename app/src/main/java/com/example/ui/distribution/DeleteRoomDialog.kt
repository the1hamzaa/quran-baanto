package com.example.ui.distribution

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.ui.components.WaveDivider
import com.example.ui.theme.FrauncesFontFamily
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.QuranBaantoDesignSystem

/**
 * EXACT DELETE ROOM SCREEN / DIALOG
 * Matches Delete_room_2724f973.png and Delete_room___dark_bfa9963c.png precisely.
 */
@Composable
fun DeleteRoomScreen(
    roomName: String,
    memberCount: Int,
    onConfirmDelete: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    androidx.activity.compose.BackHandler(enabled = true) { onCancel() }
    val colors = QuranBaantoDesignSystem.colors
    var deleteInput by remember { mutableStateOf("") }
    val isConfirmed = deleteInput.trim().equals("DELETE", ignoreCase = true)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Large wine circle with "!" exclamation mark cropped at top right
        TopRightExclamationBubble(wineColor = colors.wineFill)

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
                    onClick = onCancel,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.text
                    )
                }

                Spacer(modifier = Modifier.height(48.dp))

                // Title
                Text(
                    text = "Delete this\nroom?",
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 38.sp,
                    lineHeight = 44.sp,
                    letterSpacing = (-0.5).sp,
                    color = colors.text
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Description
                val membersText = if (memberCount > 0) " for all $memberCount members" else ""
                Text(
                    text = "This permanently removes $roomName and its roster records$membersText.",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = colors.mutedText
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "This can't be undone.",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = colors.wineText
                )

                Spacer(modifier = Modifier.height(40.dp))

                // TYPE DELETE TO CONFIRM
                Text(
                    text = "TYPE DELETE TO CONFIRM",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    letterSpacing = 1.1.sp,
                    color = colors.mutedText
                )

                Spacer(modifier = Modifier.height(10.dp))

                BasicTextField(
                    value = deleteInput,
                    onValueChange = { deleteInput = it },
                    textStyle = TextStyle(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        letterSpacing = 2.sp,
                        color = colors.text
                    ),
                    cursorBrush = SolidColor(colors.gold),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("delete_room_confirm_field")
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Gold wave underline
                WaveDivider(
                    color = colors.gold,
                    strokeWidth = 1.6.dp,
                    amplitude = 2.dp,
                    wavelength = 44.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Bottom Actions
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Delete button
                Button(
                    onClick = onConfirmDelete,
                    enabled = isConfirmed,
                    shape = RoundedCornerShape(26.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.wineFill,
                        contentColor = Color(0xFFF1EBDD),
                        disabledContainerColor = colors.wineFill.copy(alpha = 0.35f),
                        disabledContentColor = Color(0xFFF1EBDD).copy(alpha = 0.45f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("confirm_delete_room_button")
                ) {
                    Text(
                        text = "Delete room →",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cancel link
                Text(
                    text = "Cancel",
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 15.sp,
                    textDecoration = TextDecoration.Underline,
                    color = colors.pineText,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onCancel() }
                        .padding(8.dp)
                )
            }
        }
    }
}

@Composable
private fun TopRightExclamationBubble(wineColor: Color) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        val w = size.width
        val circleCenter = Offset(w - 20.dp.toPx(), 45.dp.toPx())
        val circleRadius = 70.dp.toPx()

        drawCircle(
            color = wineColor,
            radius = circleRadius,
            center = circleCenter
        )
    }

    // Centered exclamation mark inside the bubble
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 65.dp, top = 50.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Text(
            text = "!",
            fontFamily = FrauncesFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 42.sp,
            color = Color(0xFFF1EBDD)
        )
    }
}
