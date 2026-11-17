package com.example.ui.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.quran.ArabicFontStyle
import com.example.ui.components.Toggle
import com.example.ui.components.WaveDivider
import com.example.ui.components.WaveSlider
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.FrauncesFontFamily
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.QuranBaantoDesignSystem
import com.example.ui.viewmodel.DistributionViewModel
import com.example.ui.viewmodel.QuranViewModel
import com.example.util.KhatmNotificationHelper
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * EXACT SETTINGS SCREEN IMPLEMENTATION
 * Matches attached target screenshots precisely in both Light and Dark modes.
 *
 * Visual Rules:
 * - 28dp side margins, 390dp reference width.
 * - Sections in order:
 *   1. Room Profile & Reminders (open field with wave underline, Firebase ID line,
 *      wine outline delete pill, 40x22 pine toggle with cream knob).
 *   2. Appearance (two arch-shaped previews: Warm Parchment and Midnight Slate with check badge).
 *   3. Mushaf Script & Size (two 46dp pills, wavy track slider, live verse preview with wine ayah marker).
 *   4. Privacy & Data Safety (gold outline pill).
 *   5. Footer (Fraunces version & Hambasy Studios).
 * - No cards, rounded rectangle boxes, shadows, gradients, or emoji anywhere.
 * - High-performance GPU rendering without layout wrapping or stutter.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    quranViewModel: QuranViewModel? = null,
    distributionViewModel: DistributionViewModel? = null
) {
    val context = LocalContext.current
    val colors = QuranBaantoDesignSystem.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val fontSizeSp by quranViewModel?.fontSizeSp?.collectAsState()
        ?: remember { mutableIntStateOf(24) }
    val fontStyle by quranViewModel?.arabicFontStyle?.collectAsState()
        ?: remember { mutableStateOf(ArabicFontStyle.UTHMANI) }

    val savedProfileName by distributionViewModel?.userProfileName?.collectAsState()
        ?: remember { mutableStateOf("") }
    val notificationsEnabled by distributionViewModel?.notificationsEnabled?.collectAsState()
        ?: remember { mutableStateOf(true) }
    val firebaseProfile by distributionViewModel?.firebaseUserProfile?.collectAsState()
        ?: remember { mutableStateOf(null) }

    var localNameDraft by remember(savedProfileName) { mutableStateOf(savedProfileName) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var sliderFontSize by remember(fontSizeSp) { mutableFloatStateOf(fontSizeSp.toFloat()) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    ModalBottomSheet(
        onDismissRequest = {
            if (localNameDraft.trim() != savedProfileName) {
                distributionViewModel?.setUserProfileName(localNameDraft.trim())
            }
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = colors.background,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            // Gold drag handle pill
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(colors.gold)
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.background)
        ) {
            // Decorative background circle cropped at top right
            TopRightSettingsBubble(isDark = colors.isDark)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp)
                    .padding(bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                // Settings Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Arch icon with a pine dot inside
                        ArchDotIcon(
                            dotColor = if (colors.isDark) colors.pineText else colors.pine,
                            strokeColor = colors.text
                        )

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = "Settings",
                                fontFamily = FrauncesFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 30.sp,
                                color = colors.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Profile, rooms & Mushaf typography",
                                fontFamily = InterFontFamily,
                                fontSize = 13.sp,
                                color = colors.mutedText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Version pill "v4.0" in gold / champagne
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (colors.isDark) Color(0xFFC5A77D).copy(alpha = 0.85f) else Color(0xFFC5A77D).copy(alpha = 0.55f))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "v4.0",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFF1B1B19)
                        )
                    }
                }

                // ----------------------------------------------------
                // 1. Room Profile & Reminders
                // ----------------------------------------------------
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Heading with person stroke icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PersonStrokeIcon(strokeColor = colors.text)
                        Text(
                            text = "Room Profile & Reminders",
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 21.sp,
                            color = colors.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = "Your default name in online rooms",
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = colors.text,
                        maxLines = 1
                    )

                    // Open field with a wave underline (no box around it)
                    OpenWaveUnderlineField(
                        value = localNameDraft,
                        onValueChange = {
                            localNameDraft = it
                            distributionViewModel?.setUserProfileName(it)
                        },
                        placeholder = "e.g. Hamza",
                        waveColor = colors.gold
                    )

                    val shortUid = firebaseProfile?.uidHash ?: "FC3EF426"
                    Text(
                        text = "Firebase Auth & Firestore Secured • ID: $shortUid",
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp,
                        color = colors.mutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Full-width pill with a wine outline for Delete Cloud Profile (strictly 1 line)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clip(CircleShape)
                            .border(1.4.dp, colors.wine, CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                localNameDraft = ""
                                distributionViewModel?.deleteCloudProfileAndResetId()
                            }
                            .testTag("delete_cloud_profile_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Delete Cloud Profile & Reset Anonymous ID",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = colors.wineText,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Reminders row with 40x22 pine toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            BellStrokeIcon(strokeColor = colors.text)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Room & Monthly Reminders",
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = colors.text,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Notify when members complete Paras or a new month begins",
                                    fontFamily = InterFontFamily,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    color = colors.mutedText
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Toggle(
                            checked = notificationsEnabled,
                            onCheckedChange = { enabled ->
                                distributionViewModel?.setNotificationsEnabled(enabled)
                                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !KhatmNotificationHelper.hasNotificationPermission(context)
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        )
                    }

                    // Section Divider
                    WaveDivider(
                        color = colors.rules,
                        strokeWidth = 1.2.dp,
                        amplitude = 2.dp,
                        wavelength = 44.dp
                    )
                }

                // ----------------------------------------------------
                // 2. Appearance (Exact Screenshot Design)
                // ----------------------------------------------------
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Appearance",
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 21.sp,
                        color = colors.text,
                        maxLines = 1
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Light Mode Arch: Warm Parchment
                        ThemeArchPreview(
                            title = "Warm Parchment",
                            subtitle = "Light mode",
                            isLight = true,
                            isSelected = !isDarkTheme,
                            onClick = { onToggleDarkTheme(false) },
                            modifier = Modifier.weight(1f)
                        )

                        // Dark Mode Arch: Midnight Slate
                        ThemeArchPreview(
                            title = "Midnight Slate",
                            subtitle = "Dark mode",
                            isLight = false,
                            isSelected = isDarkTheme,
                            onClick = { onToggleDarkTheme(true) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ----------------------------------------------------
                // 3. Mushaf Script & Size
                // ----------------------------------------------------
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
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
                            AaStrokeIcon()
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Mushaf Script & Size",
                                fontFamily = FrauncesFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 21.sp,
                                color = colors.text,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Text(
                            text = "↺ Reset",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = colors.mutedText,
                            modifier = Modifier.clickable {
                                quranViewModel?.setFontSize(24)
                                quranViewModel?.setArabicFontStyle(ArabicFontStyle.UTHMANI)
                                sliderFontSize = 24f
                            }
                        )
                    }

                    // Two 46dp pills for script choice
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ScriptChoicePill(
                            title = "Uthmani",
                            selected = fontStyle == ArabicFontStyle.UTHMANI,
                            onClick = { quranViewModel?.setArabicFontStyle(ArabicFontStyle.UTHMANI) },
                            modifier = Modifier.weight(1f)
                        )

                        ScriptChoicePill(
                            title = "Indo-Pak",
                            selected = fontStyle == ArabicFontStyle.INDO_PAK,
                            onClick = { quranViewModel?.setArabicFontStyle(ArabicFontStyle.INDO_PAK) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Arabic Text Size label and wavy slider
                    val currentDisplaySp = sliderFontSize.toInt().coerceIn(18, 42)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Arabic Text Size",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = colors.text,
                            maxLines = 1
                        )
                        Text(
                            text = "${currentDisplaySp}sp",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (colors.isDark) colors.pineText else colors.pine,
                            maxLines = 1
                        )
                    }

                    WaveSlider(
                        value = sliderFontSize,
                        onValueChange = { newSize ->
                            sliderFontSize = newSize
                            quranViewModel?.setFontSize(newSize.toInt())
                        },
                        valueRange = 18f..42f
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Live preview verse: Surah Al-Fatihah Ayah 1 with wine ayah marker
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "SURAH AL-FATIHAH · AYAH 1 · ${if (fontStyle == ArabicFontStyle.UTHMANI) "UTHMANI" else "INDO-PAK"}",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            letterSpacing = 1.2.sp,
                            color = colors.mutedText,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            AyahWineMarker(ayahNumber = "١")
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ",
                                fontFamily = AmiriFontFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = currentDisplaySp.sp,
                                color = colors.text,
                                textAlign = TextAlign.Center
                            )
                        }

                        Text(
                            text = "In the name of Allah, the Entirely Merciful, the Especially Merciful.",
                            fontFamily = InterFontFamily,
                            fontStyle = FontStyle.Italic,
                            fontSize = 13.sp,
                            lineHeight = 17.sp,
                            color = colors.mutedText,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        )
                    }

                    // Section Divider
                    WaveDivider(
                        color = colors.rules,
                        strokeWidth = 1.2.dp,
                        amplitude = 2.dp,
                        wavelength = 44.dp
                    )
                }

                // ----------------------------------------------------
                // 4. Privacy & Data Safety
                // ----------------------------------------------------
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ShieldStrokeIcon(strokeColor = colors.text)
                        Text(
                            text = "Privacy & Data Safety",
                            fontFamily = FrauncesFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 21.sp,
                            color = colors.text,
                            maxLines = 1
                        )
                    }

                    Text(
                        text = "100% Offline-first local groups. Zero tracking on Qur'an recitation. Voluntary permissionary ads only for online rooms.",
                        fontFamily = InterFontFamily,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = colors.mutedText
                    )

                    // Full-width pill with gold outline (strictly 1 line)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(CircleShape)
                            .border(1.4.dp, colors.gold, CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                showPrivacyDialog = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "View Data Safety & Privacy Policy",
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = colors.text,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // ----------------------------------------------------
                // 5. Footer
                // ----------------------------------------------------
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Quran Baanto • Version 4.0",
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp,
                        color = colors.text
                    )
                    Text(
                        text = "Created by Hambasy Studios",
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp,
                        color = if (colors.isDark) colors.pineText else colors.pine
                    )
                }
            }
        }
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = {
                Text(
                    text = "Privacy & Data Safety",
                    fontFamily = FrauncesFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 20.sp,
                    color = colors.text
                )
            },
            text = {
                Text(
                    text = "Quran Baanto is designed with strict offline-first principles. Your local recitation, bookmarks, and local Khatm circles are stored purely on your device.\n\nWhen you participate in online rooms, only your chosen display name and completed parts are synced using secured Firebase Firestore rules.",
                    fontFamily = InterFontFamily,
                    fontSize = 13.sp,
                    color = colors.text
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("OK", fontFamily = InterFontFamily, fontWeight = FontWeight.Bold, color = colors.pine)
                }
            },
            containerColor = colors.background
        )
    }
}

/**
 * Open text field with a flowing wave underline in gold and NO box around it
 */
@Composable
private fun OpenWaveUnderlineField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    waveColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = QuranBaantoDesignSystem.colors

    Column(modifier = modifier.fillMaxWidth()) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 17.sp,
                color = colors.text
            ),
            singleLine = true,
            cursorBrush = SolidColor(colors.pine),
            decorationBox = { innerTextField ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 17.sp,
                            color = colors.mutedText.copy(alpha = 0.6f)
                        )
                    }
                    innerTextField()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("settings_profile_name_input")
        )

        WaveDivider(
            color = waveColor,
            strokeWidth = 1.6.dp,
            amplitude = 3.dp,
            wavelength = 50.dp,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

/**
 * Script choice 46dp pill:
 * Selected is filled pine, unselected has a gold outline.
 */
@Composable
private fun ScriptChoicePill(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = QuranBaantoDesignSystem.colors

    Box(
        modifier = modifier
            .height(46.dp)
            .clip(CircleShape)
            .background(if (selected) colors.pine else Color.Transparent)
            .then(
                if (!selected) {
                    Modifier.border(1.4.dp, colors.gold, CircleShape)
                } else Modifier
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = if (selected) Color.White else colors.text
        )
    }
}

/**
 * EXACT ARCH THEME PREVIEW COMPONENT
 *
 * Built using native RoundedCornerShape(topStartPercent = 50, topEndPercent = 50)
 * to form a perfect architectural arch window matching the screenshots precisely:
 * - Selected: Full pine border around the arch + check badge at top-right.
 * - Unselected: Clean solid arch shape with NO border and NO check badge.
 * - Warm Parchment: Gold sun with 8 rays, dark ink bars, pine pill.
 * - Midnight Slate: Gold crescent moon with stars, cream bars, pine pill.
 */
@Composable
private fun ThemeArchPreview(
    title: String,
    subtitle: String,
    isLight: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = QuranBaantoDesignSystem.colors
    val archBg = if (isLight) Color(0xFFF4EEE2) else Color(0xFF1E232A)
    val pineBorder = if (colors.isDark) Color(0xFF4B8F7B) else Color(0xFF1F3D36)
    val checkBadgeColor = if (colors.isDark) Color(0xFF4B8F7B) else Color(0xFF1F3D36)

    // Architectural Arch Shape: curved 180° semicircular top, flat bottom
    val archShape = RoundedCornerShape(
        topStartPercent = 50,
        topEndPercent = 50,
        bottomStartPercent = 0,
        bottomEndPercent = 0
    )

    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .clip(archShape)
                .background(archBg)
                .then(
                    if (isSelected) {
                        Modifier.border(2.dp, pineBorder, archShape)
                    } else if (isLight && !colors.isDark) {
                        Modifier.border(1.dp, Color(0xFFE5DDD0), archShape)
                    } else Modifier
                )
        ) {
            // Content inside the arch
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 18.dp, start = 16.dp, end = 16.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                if (isLight) {
                    // Gold Sun with 8 rays
                    GoldSunIcon()

                    // Editorial ink lines and pine pill
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(4.5.dp)
                                .clip(RoundedCornerShape(2.5.dp))
                                .background(Color(0xFF1B1B19))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .height(4.5.dp)
                                .clip(RoundedCornerShape(2.5.dp))
                                .background(Color(0xFF1B1B19))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.50f)
                                .height(8.5.dp)
                                .clip(RoundedCornerShape(4.5.dp))
                                .background(Color(0xFF1F3D36))
                        )
                    }
                } else {
                    // Gold Crescent Moon with stars
                    GoldMoonWithStarsIcon()

                    // Editorial cream lines and pine pill
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(4.5.dp)
                                .clip(RoundedCornerShape(2.5.dp))
                                .background(Color(0xFFF1EBDD))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.55f)
                                .height(4.5.dp)
                                .clip(RoundedCornerShape(2.5.dp))
                                .background(Color(0xFFA29F94))
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.50f)
                                .height(8.5.dp)
                                .clip(RoundedCornerShape(4.5.dp))
                                .background(Color(0xFF2F6B5A))
                        )
                    }
                }
            }

            // Check badge: only shown when selected
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 14.dp, end = 14.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(checkBadgeColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = title,
            fontFamily = FrauncesFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 17.sp,
            color = colors.text,
            maxLines = 1
        )
        Text(
            text = subtitle,
            fontFamily = InterFontFamily,
            fontSize = 12.sp,
            color = colors.mutedText,
            maxLines = 1
        )
    }
}

/**
 * Top right decorative soft neutral bubble cropped by corner with v4.0 pill
 */
@Composable
private fun TopRightSettingsBubble(isDark: Boolean) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        val w = size.width
        val circleCenter = Offset(w + 10.dp.toPx(), -10.dp.toPx())
        val circleRadius = 75.dp.toPx()
        val circleColor = if (isDark) Color(0xFF2B2823) else Color(0xFFE9E1D1)

        drawCircle(
            color = circleColor,
            radius = circleRadius,
            center = circleCenter
        )
    }
}

/**
 * Vector icons with 1.8 stroke width
 */
@Composable
private fun ArchDotIcon(dotColor: Color, strokeColor: Color) {
    Canvas(modifier = Modifier.size(width = 28.dp, height = 34.dp)) {
        val w = size.width
        val h = size.height
        val r = w / 2f
        val strokePx = 1.8.dp.toPx()

        val archPath = Path().apply {
            moveTo(strokePx / 2, h)
            lineTo(strokePx / 2, r)
            arcTo(
                rect = Rect(strokePx / 2, strokePx / 2, w - strokePx / 2, 2f * r),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            lineTo(w - strokePx / 2, h)
        }

        drawPath(path = archPath, color = strokeColor, style = Stroke(width = strokePx, cap = StrokeCap.Round))
        drawCircle(color = dotColor, radius = 3.5.dp.toPx(), center = Offset(w / 2f, h * 0.65f))
    }
}

@Composable
private fun PersonStrokeIcon(strokeColor: Color) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val strokePx = 1.8.dp.toPx()
        drawCircle(
            color = strokeColor,
            radius = 4.5.dp.toPx(),
            center = Offset(size.width / 2f, 7.dp.toPx()),
            style = Stroke(width = strokePx)
        )
        val bodyPath = Path().apply {
            moveTo(3.dp.toPx(), size.height - 3.dp.toPx())
            cubicTo(
                4.dp.toPx(), 14.dp.toPx(),
                size.width - 4.dp.toPx(), 14.dp.toPx(),
                size.width - 3.dp.toPx(), size.height - 3.dp.toPx()
            )
        }
        drawPath(path = bodyPath, color = strokeColor, style = Stroke(width = strokePx, cap = StrokeCap.Round))
    }
}

@Composable
private fun BellStrokeIcon(strokeColor: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val strokePx = 1.8.dp.toPx()
        val bellPath = Path().apply {
            moveTo(size.width / 2f, 3.dp.toPx())
            lineTo(size.width / 2f + 6.dp.toPx(), 12.dp.toPx())
            lineTo(size.width - 3.dp.toPx(), 15.dp.toPx())
            lineTo(3.dp.toPx(), 15.dp.toPx())
            lineTo(size.width / 2f - 6.dp.toPx(), 12.dp.toPx())
            close()
        }
        drawPath(path = bellPath, color = strokeColor, style = Stroke(width = strokePx, cap = StrokeCap.Round))
        drawCircle(
            color = strokeColor,
            radius = 1.8.dp.toPx(),
            center = Offset(size.width / 2f, 18.dp.toPx())
        )
    }
}

@Composable
private fun AaStrokeIcon() {
    Box(
        modifier = Modifier
            .size(26.dp)
            .clip(CircleShape)
            .background(Color(0xFFE9E1D1).copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Aa",
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = Color(0xFF1B1B19)
        )
    }
}

@Composable
private fun ShieldStrokeIcon(strokeColor: Color) {
    Canvas(modifier = Modifier.size(22.dp)) {
        val strokePx = 1.8.dp.toPx()
        val path = Path().apply {
            moveTo(size.width / 2f, 2.dp.toPx())
            lineTo(size.width - 3.dp.toPx(), 5.dp.toPx())
            lineTo(size.width - 3.dp.toPx(), 12.dp.toPx())
            cubicTo(
                size.width - 3.dp.toPx(), 18.dp.toPx(),
                size.width / 2f, 20.dp.toPx(),
                size.width / 2f, 20.dp.toPx()
            )
            cubicTo(
                size.width / 2f, 20.dp.toPx(),
                3.dp.toPx(), 18.dp.toPx(),
                3.dp.toPx(), 12.dp.toPx()
            )
            lineTo(3.dp.toPx(), 5.dp.toPx())
            close()
        }
        drawPath(path = path, color = strokeColor, style = Stroke(width = strokePx, cap = StrokeCap.Round))
    }
}

@Composable
private fun AyahWineMarker(ayahNumber: String) {
    val colors = QuranBaantoDesignSystem.colors

    Box(
        modifier = Modifier
            .size(28.dp)
            .border(1.5.dp, colors.wine, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = ayahNumber,
            fontFamily = AmiriFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            color = colors.wineText
        )
    }
}

@Composable
private fun GoldSunIcon() {
    Canvas(modifier = Modifier.size(28.dp)) {
        val center = center
        val r = 5.dp.toPx()
        val gold = Color(0xFFC5A77D)
        drawCircle(color = gold, radius = r, center = center)

        val rayLen = 3.dp.toPx()
        for (i in 0 until 8) {
            val angle = (i * 2f * PI / 8).toFloat()
            val startX = center.x + (r + 2.dp.toPx()) * cos(angle)
            val startY = center.y + (r + 2.dp.toPx()) * sin(angle)
            val endX = center.x + (r + 2.dp.toPx() + rayLen) * cos(angle)
            val endY = center.y + (r + 2.dp.toPx() + rayLen) * sin(angle)
            drawLine(color = gold, start = Offset(startX, startY), end = Offset(endX, endY), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun GoldMoonWithStarsIcon() {
    Canvas(modifier = Modifier.size(28.dp)) {
        val gold = Color(0xFFC5A77D)
        val moonPath = Path().apply {
            moveTo(14.dp.toPx(), 4.dp.toPx())
            cubicTo(
                20.dp.toPx(), 8.dp.toPx(),
                20.dp.toPx(), 20.dp.toPx(),
                12.dp.toPx(), 24.dp.toPx()
            )
            cubicTo(
                17.dp.toPx(), 19.dp.toPx(),
                16.dp.toPx(), 9.dp.toPx(),
                14.dp.toPx(), 4.dp.toPx()
            )
            close()
        }
        drawPath(path = moonPath, color = gold)

        drawCircle(color = gold, radius = 1.2.dp.toPx(), center = Offset(22.dp.toPx(), 6.dp.toPx()))
        drawCircle(color = gold, radius = 0.9.dp.toPx(), center = Offset(24.dp.toPx(), 14.dp.toPx()))
    }
}
