package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.QuranBaantoDesignSystem

data class NavTabItem(
    val route: String,
    val label: String
)

val MainNavTabs = listOf(
    NavTabItem(route = "home", label = "Home"),
    NavTabItem(route = "quran", label = "Quran"),
    NavTabItem(route = "distributions", label = "Distributions"),
    NavTabItem(route = "streaks", label = "Progress")
)

/**
 * EXACT BOTTOM NAVIGATION:
 * Text only, no icons (Home, Quran, Distributions, Progress).
 * The active tab is bold with a true 44x22dp semicircle rising from the bottom edge.
 */
@Composable
fun BottomNav(
    currentRoute: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
    onCharcoal: Boolean = (currentRoute == "home"),
    tabs: List<NavTabItem> = MainNavTabs
) {
    val colors = QuranBaantoDesignSystem.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
        contentAlignment = Alignment.BottomCenter
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.Bottom
        ) {
            tabs.forEach { tab ->
                val isActive = currentRoute == tab.route
                val interactionSource = remember { MutableInteractionSource() }

                val textColor = if (onCharcoal) {
                    if (isActive) Color(0xFFF1EBDD) else Color(0xFFF1EBDD).copy(alpha = 0.70f)
                } else {
                    if (isActive) colors.text else colors.mutedText
                }

                val semicircleColor = if (onCharcoal) colors.gold else colors.pine

                Column(
                    modifier = Modifier
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            onNavigate(tab.route)
                        }
                        .testTag("bottom_nav_${tab.route}")
                        .semantics { role = Role.Tab },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = tab.label,
                        fontFamily = InterFontFamily,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp,
                        color = textColor,
                        modifier = Modifier.padding(bottom = if (isActive) 6.dp else 28.dp)
                    )

                    if (isActive) {
                        Spacer(
                            modifier = Modifier
                                .size(width = 44.dp, height = 22.dp)
                                .drawWithCache {
                                    val path = Path().apply {
                                        arcTo(
                                            rect = Rect(0f, 0f, size.width, size.height * 2f),
                                            startAngleDegrees = 180f,
                                            sweepAngleDegrees = 180f,
                                            forceMoveTo = false
                                        )
                                        close()
                                    }
                                    onDrawBehind {
                                        drawPath(path = path, color = semicircleColor)
                                    }
                                }
                        )
                    }
                }
            }
        }
    }
}
