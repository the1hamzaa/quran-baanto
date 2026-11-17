package com.example.ui.quran

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.quran.ParaInfo
import com.example.data.quran.QuranPageMapper
import com.example.data.quran.SurahInfo
import com.example.ui.components.WaveDivider
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.FrauncesFontFamily
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.QuranBaantoDesignSystem
import com.example.ui.viewmodel.QuranViewModel

/**
 * QURAN LIST SCREEN (Organic Modernism Design)
 * Matches Quran_1x_75efb64f.png and Quran___dark_1x_390e32ef.png precisely.
 *
 * - Top-left architectural pine circular motif with "Quran" title and Surah / Juz tabs.
 * - Open search field with gold wave underline.
 * - Continuous flowing vertical gold wave timeline connecting all circular item medallions.
 * - Medallions: Gold outline by default, Wine filled for the active last-read bookmark.
 * - English Surah/Juz names in Fraunces, verse counts in Inter, Arabic calligraphy in Amiri in pine.
 * - Integrated BottomNav with active "Quran" tab.
 */
@Composable
fun QuranListScreen(
    quranViewModel: QuranViewModel,
    onOpenParaReader: (Int) -> Unit,
    onOpenSurahReader: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = QuranBaantoDesignSystem.colors
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Surah, 1 = Juz
    var searchQuery by remember { mutableStateOf("") }

    val allSurahs = quranViewModel.surahList
    val allParas = quranViewModel.paraList
    val lastReadBookmark by quranViewModel.lastReadBookmark.collectAsState()

    // Filter surahs
    val filteredSurahs = remember(searchQuery, allSurahs) {
        if (searchQuery.isBlank()) {
            allSurahs
        } else {
            val q = searchQuery.trim().lowercase()
            allSurahs.filter {
                it.number.toString() == q ||
                    it.transliteration.lowercase().contains(q) ||
                    it.arabicName.contains(q) ||
                    it.englishMeaning.lowercase().contains(q)
            }
        }
    }

    // Filter juzs
    val filteredParas = remember(searchQuery, allParas) {
        if (searchQuery.isBlank()) {
            allParas
        } else {
            val q = searchQuery.trim().lowercase()
            allParas.filter {
                it.number.toString() == q ||
                    it.transliteration.lowercase().contains(q) ||
                    it.arabicTitle.contains(q) ||
                    it.surahRange.lowercase().contains(q)
            }
        }
    }

    // Determine current bookmark targets
    val bookmarkedSurahNumber = lastReadBookmark?.surahNumber ?: 2
    val bookmarkedJuzNumber = lastReadBookmark?.let { bm ->
        val page = QuranPageMapper.getPageForAyah(bm.surahNumber, bm.ayahNumber, bm.id)
        QuranPageMapper.getJuzForPage(page)
    } ?: 1

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Top-left organic pine shape with gold arc
        TopLeftQuranHeaderArc(isDark = colors.isDark)

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Upper Area: Header, Tabs, and Search
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(top = 16.dp)
            ) {
                // Header Title & Tabs over the top-left pine arc
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp)
                ) {
                    Text(
                        text = "Quran",
                        fontFamily = FrauncesFontFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 44.sp,
                        letterSpacing = (-0.5).sp,
                        color = Color(0xFFF6F0E4),
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Surah & Juz Tabs
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(22.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Surah Tab
                        Column(
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { selectedTab = 0 }
                                .padding(vertical = 4.dp)
                                .testTag("quran_tab_surah")
                        ) {
                            Text(
                                text = "Surah",
                                fontFamily = InterFontFamily,
                                fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 16.sp,
                                color = if (selectedTab == 0) Color(0xFFF6F0E4) else Color(0xFFF6F0E4).copy(alpha = 0.78f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (selectedTab == 0) {
                                Box(
                                    modifier = Modifier
                                        .width(38.dp)
                                        .height(2.5.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF6F0E4))
                                )
                            } else {
                                Spacer(modifier = Modifier.height(2.5.dp))
                            }
                        }

                        // Juz Tab
                        Column(
                            horizontalAlignment = Alignment.Start,
                            modifier = Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { selectedTab = 1 }
                                .padding(vertical = 4.dp)
                                .testTag("quran_tab_juz")
                        ) {
                            Text(
                                text = "Juz",
                                fontFamily = InterFontFamily,
                                fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 16.sp,
                                color = if (selectedTab == 1) Color(0xFFF6F0E4) else Color(0xFFF6F0E4).copy(alpha = 0.78f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            if (selectedTab == 1) {
                                Box(
                                    modifier = Modifier
                                        .width(26.dp)
                                        .height(2.5.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF6F0E4))
                                )
                            } else {
                                Spacer(modifier = Modifier.height(2.5.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(26.dp))

                // Search Bar with Gold Wave Underline (no box container)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = colors.text,
                            modifier = Modifier.size(22.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = if (selectedTab == 0) "Search surah, juz or ayah" else "Search juz, surah or meaning",
                                    fontFamily = InterFontFamily,
                                    fontSize = 15.sp,
                                    color = colors.mutedText
                                )
                            }

                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                textStyle = TextStyle(
                                    fontFamily = InterFontFamily,
                                    fontSize = 15.sp,
                                    color = colors.text
                                ),
                                cursorBrush = SolidColor(colors.gold),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("quran_search_field")
                            )
                        }
                    }

                    // Gold flowing wave underline
                    WaveDivider(
                        color = colors.gold,
                        strokeWidth = 1.4.dp,
                        amplitude = 2.dp,
                        wavelength = 44.dp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
            }

            // Continuous Wavy Timeline List
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (selectedTab == 0) {
                    // Surah List
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(
                            items = filteredSurahs,
                            key = { _, surah -> "surah_${surah.number}" }
                        ) { index, surah ->
                            val isBookmark = surah.number == bookmarkedSurahNumber
                            SurahTimelineRow(
                                surah = surah,
                                isBookmark = isBookmark,
                                rowIndex = index,
                                onClick = { onOpenSurahReader(surah.number) }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                } else {
                    // Juz List
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        itemsIndexed(
                            items = filteredParas,
                            key = { _, para -> "para_${para.number}" }
                        ) { index, para ->
                            val isBookmark = para.number == bookmarkedJuzNumber
                            JuzTimelineRow(
                                para = para,
                                isBookmark = isBookmark,
                                rowIndex = index,
                                onClick = { onOpenParaReader(para.number) }
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

/**
 * Top-left decorative pine organic circular shape with gold arc
 */
@Composable
private fun TopLeftQuranHeaderArc(isDark: Boolean) {
    val pineColor = if (isDark) Color(0xFF234D40) else Color(0xFF1F3D36)
    val goldArcColor = Color(0xFFC5A77D).copy(alpha = 0.65f)
    Spacer(
        modifier = Modifier
            .fillMaxWidth()
            .height(242.dp)
            .drawWithCache {
                val circleCenter = Offset(14.dp.toPx(), 18.dp.toPx())
                val circleRadius = 242.dp.toPx()
                val ringStroke = 1.6.dp.toPx()

                onDrawBehind {
                    drawCircle(
                        color = pineColor,
                        radius = circleRadius,
                        center = circleCenter
                    )
                    drawCircle(
                        color = goldArcColor,
                        radius = circleRadius * 0.88f,
                        center = circleCenter,
                        style = Stroke(width = ringStroke)
                    )
                }
            }
    )
}

/**
 * Single Surah item row along the continuous flowing vertical gold wave timeline
 */
@Composable
private fun SurahTimelineRow(
    surah: SurahInfo,
    isBookmark: Boolean,
    rowIndex: Int,
    onClick: () -> Unit
) {
    val colors = QuranBaantoDesignSystem.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(end = 28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Continuous Gold Wavy Line & Medallion Container
        Box(
            modifier = Modifier
                .width(66.dp)
                .height(78.dp),
            contentAlignment = Alignment.Center
        ) {
            val sign = if (rowIndex % 2 == 0) 1f else -1f
            Spacer(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val w = size.width
                        val h = size.height
                        val centerX = w * 0.52f
                        val waveAmp = 7.dp.toPx()
                        val strokePx = 1.6.dp.toPx()

                        val path = Path().apply {
                            moveTo(centerX, 0f)
                            cubicTo(
                                centerX - waveAmp * sign, h * 0.35f,
                                centerX + waveAmp * sign, h * 0.65f,
                                centerX, h
                            )
                        }

                        onDrawBehind {
                            drawPath(
                                path = path,
                                color = Color(0xFFC5A77D),
                                style = Stroke(width = strokePx, cap = StrokeCap.Round)
                            )
                        }
                    }
            )

            // Circle Medallion (Wine filled for bookmark, Gold outline otherwise)
            val medallionBg = if (isBookmark) {
                if (colors.isDark) Color(0xFF8A3A50) else Color(0xFF642B3A)
            } else {
                colors.background
            }
            val numColor = if (isBookmark) {
                Color(0xFFF1EBDD)
            } else {
                colors.text
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(medallionBg)
                    .then(
                        if (!isBookmark) {
                            Modifier.border(1.6.dp, colors.gold, CircleShape)
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = surah.number.toString(),
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = numColor
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // English Title and Verses count
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = surah.transliteration,
                fontFamily = FrauncesFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 22.sp,
                letterSpacing = (-0.2).sp,
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${surah.versesCount} verses",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                color = colors.mutedText,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Arabic Calligraphy in Pine
        Text(
            text = surah.arabicName,
            fontFamily = AmiriFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 28.sp,
            color = colors.pineText,
            maxLines = 1
        )
    }
}

/**
 * Single Juz item row along the continuous flowing vertical gold wave timeline
 */
@Composable
private fun JuzTimelineRow(
    para: ParaInfo,
    isBookmark: Boolean,
    rowIndex: Int,
    onClick: () -> Unit
) {
    val colors = QuranBaantoDesignSystem.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(78.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(end = 28.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Continuous Gold Wavy Line & Medallion Container
        Box(
            modifier = Modifier
                .width(66.dp)
                .height(78.dp),
            contentAlignment = Alignment.Center
        ) {
            val sign = if (rowIndex % 2 == 0) 1f else -1f
            Spacer(
                modifier = Modifier
                    .fillMaxSize()
                    .drawWithCache {
                        val w = size.width
                        val h = size.height
                        val centerX = w * 0.52f
                        val waveAmp = 7.dp.toPx()
                        val strokePx = 1.6.dp.toPx()

                        val path = Path().apply {
                            moveTo(centerX, 0f)
                            cubicTo(
                                centerX - waveAmp * sign, h * 0.35f,
                                centerX + waveAmp * sign, h * 0.65f,
                                centerX, h
                            )
                        }

                        onDrawBehind {
                            drawPath(
                                path = path,
                                color = Color(0xFFC5A77D),
                                style = Stroke(width = strokePx, cap = StrokeCap.Round)
                            )
                        }
                    }
            )

            // Circle Medallion (Wine filled for bookmark, Gold outline otherwise)
            val medallionBg = if (isBookmark) {
                if (colors.isDark) Color(0xFF8A3A50) else Color(0xFF642B3A)
            } else {
                colors.background
            }
            val numColor = if (isBookmark) {
                Color(0xFFF1EBDD)
            } else {
                colors.text
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(medallionBg)
                    .then(
                        if (!isBookmark) {
                            Modifier.border(1.6.dp, colors.gold, CircleShape)
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = para.number.toString(),
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = numColor
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // English Title and Surah Range
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = para.transliteration,
                fontFamily = FrauncesFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 21.sp,
                letterSpacing = (-0.2).sp,
                color = colors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Juz ${para.number} • ${para.surahRange}",
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                color = colors.mutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Arabic Calligraphy in Pine
        Text(
            text = para.arabicTitle,
            fontFamily = AmiriFontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 28.sp,
            color = colors.pineText,
            maxLines = 1
        )
    }
}
