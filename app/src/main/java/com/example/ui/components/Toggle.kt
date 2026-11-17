package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.ui.theme.QuranBaantoDesignSystem

/**
 * TOGGLE COMPONENT
 * The toggle is a 40x22 pine pill with a cream knob.
 * Has a 48dp touch target for accessibility.
 */
@Composable
fun Toggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = QuranBaantoDesignSystem.colors
    val interactionSource = remember { MutableInteractionSource() }

    // Knob offset animation: 40dp width, 22dp height, knob 16dp.
    // Left padding = 3dp, right padding = 3dp.
    // Left position = 3dp, right position = (40 - 16 - 3) = 21dp.
    val knobOffset by animateDpAsState(
        targetValue = if (checked) 21.dp else 3.dp,
        animationSpec = tween(durationMillis = 200),
        label = "knobOffset"
    )

    val pillBackground = if (checked) colors.pine else colors.softNeutral
    val pillBorder = if (checked) null else colors.mutedText.copy(alpha = 0.4f)
    val knobColor = if (colors.isDark) Color(0xFFF1EBDD) else Color(0xFFF6F0E4)

    Box(
        modifier = modifier
            .size(48.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                onCheckedChange(!checked)
            }
            .semantics { role = Role.Switch },
        contentAlignment = Alignment.Center
    ) {
        // 40x22 pill
        Box(
            modifier = Modifier
                .size(width = 40.dp, height = 22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(pillBackground)
                .then(
                    if (pillBorder != null) {
                        Modifier.border(1.dp, pillBorder, RoundedCornerShape(11.dp))
                    } else Modifier
                ),
            contentAlignment = Alignment.CenterStart
        ) {
            // Cream knob
            Box(
                modifier = Modifier
                    .offset(x = knobOffset)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(knobColor)
            )
        }
    }
}
