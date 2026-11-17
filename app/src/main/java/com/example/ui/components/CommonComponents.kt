package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Fast, zero-path-allocation octagonal number badge for 60fps scrolling in Para & Surah lists.
 */
@Composable
fun OctagonalNumberMedallion(
    number: Int,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    primaryColor: Color = MaterialTheme.colorScheme.primary,
    secondaryColor: Color = MaterialTheme.colorScheme.secondary,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val s = this.size.minDimension
            val boxSize = s * 0.68f
            val offset = (s - boxSize) / 2f
            val topLeft = Offset(offset, offset)
            val rectSize = Size(boxSize, boxSize)
            val corner = CornerRadius(s * 0.08f, s * 0.08f)

            rotate(degrees = 45f, pivot = center) {
                drawRoundRect(
                    color = containerColor.copy(alpha = 0.65f),
                    topLeft = topLeft,
                    size = rectSize,
                    cornerRadius = corner
                )
                drawRoundRect(
                    color = secondaryColor.copy(alpha = 0.55f),
                    topLeft = topLeft,
                    size = rectSize,
                    cornerRadius = corner,
                    style = Stroke(width = 1.1.dp.toPx())
                )
            }

            drawRoundRect(
                color = containerColor.copy(alpha = 0.85f),
                topLeft = topLeft,
                size = rectSize,
                cornerRadius = corner
            )
            drawRoundRect(
                color = primaryColor.copy(alpha = 0.7f),
                topLeft = topLeft,
                size = rectSize,
                cornerRadius = corner,
                style = Stroke(width = 1.2.dp.toPx())
            )
        }

        Text(
            text = number.toString(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = (size.value * 0.31f).sp
            ),
            color = primaryColor,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Subtle horizontal divider with a centered diamond accent.
 */
@Composable
fun ManuscriptOrnamentDivider(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 0.7.dp,
            color = color.copy(alpha = 0.3f)
        )
        Canvas(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .size(8.dp)
        ) {
            rotate(degrees = 45f, pivot = center) {
                drawRect(color = color)
            }
        }
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 0.7.dp,
            color = color.copy(alpha = 0.3f)
        )
    }
}

/**
 * Primary action button with crisp geometry.
 */
@Composable
fun PrimarySanctuaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 13.dp)
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        ),
        contentPadding = contentPadding,
        modifier = modifier.height(48.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Secondary outlined button.
 */
@Composable
fun SecondaryEditorialButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 13.dp)
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) accentColor.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outlineVariant
        ),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = accentColor
        ),
        contentPadding = contentPadding,
        modifier = modifier.height(48.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

data class ParsedAyah(
    val arabicText: String,
    val ayahNumber: Int,
    val arabicDigits: String
)

private val ARABIC_DIGITS = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')

fun Int.toArabicNumerals(): String {
    if (this <= 0) return "٠"
    val str = this.toString()
    val sb = StringBuilder(str.length)
    for (ch in str) {
        if (ch in '0'..'9') {
            sb.append(ARABIC_DIGITS[ch - '0'])
        } else {
            sb.append(ch)
        }
    }
    return sb.toString()
}

/**
 * Draws an authentic Mushaf circular Ayah end medallion with the Arabic verse number
 * strictly centered inside the circle across all Android devices and fonts.
 */
@Composable
fun AyahEndMedallionSymbol(
    arabicDigits: String,
    markerColor: Color,
    fontSizeSp: Int,
    modifier: Modifier = Modifier
) {
    val digitFontSize = when {
        arabicDigits.length >= 3 -> (fontSizeSp * 0.40f).sp
        arabicDigits.length == 2 -> (fontSizeSp * 0.47f).sp
        else -> (fontSizeSp * 0.54f).sp
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val diameter = size.minDimension
            val outerRadius = (diameter / 2f) * 0.86f
            val innerRadius = outerRadius * 0.80f
            val strokeOuter = (diameter * 0.055f).coerceAtLeast(1.1.dp.toPx())
            val strokeInner = (diameter * 0.028f).coerceAtLeast(0.6.dp.toPx())

            drawCircle(
                color = markerColor.copy(alpha = 0.10f),
                radius = outerRadius,
                center = center
            )

            drawCircle(
                color = markerColor,
                radius = outerRadius,
                center = center,
                style = Stroke(width = strokeOuter)
            )

            drawCircle(
                color = markerColor.copy(alpha = 0.5f),
                radius = innerRadius,
                center = center,
                style = Stroke(width = strokeInner)
            )

            val dotRadius = (diameter * 0.045f).coerceAtLeast(1.dp.toPx())
            drawCircle(
                color = markerColor,
                radius = dotRadius,
                center = Offset(center.x, center.y - outerRadius)
            )
            drawCircle(
                color = markerColor,
                radius = dotRadius,
                center = Offset(center.x, center.y + outerRadius)
            )
            drawCircle(
                color = markerColor,
                radius = dotRadius,
                center = Offset(center.x - outerRadius, center.y)
            )
            drawCircle(
                color = markerColor,
                radius = dotRadius,
                center = Offset(center.x + outerRadius, center.y)
            )
        }

        Text(
            text = arabicDigits,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                fontSize = digitFontSize,
                lineHeight = digitFontSize
            ),
            color = markerColor,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

/**
 * Fast, zero-regex Ayah parser for 120 FPS Quran reader list flattening.
 * Strips trailing '۝<digits>' or '﴿<digits>﴾' markers so each Ayah displays
 * its true verse number once inside the proper Arabic ayah medallion.
 */
fun parseAyah(rawVerse: String, fallbackNumber: Int): ParsedAyah {
    val trimmed = rawVerse.trim()

    val ayahSymbolIdx = trimmed.indexOf('۝')
    if (ayahSymbolIdx != -1) {
        val textPart = trimmed.substring(0, ayahSymbolIdx).trim()
        val digitsPart = trimmed.substring(ayahSymbolIdx + 1).trim()
        var parsedNum = 0
        for (ch in digitsPart) {
            val digitVal = when (ch) {
                in '٠'..'٩' -> ch - '٠'
                in '0'..'9' -> ch - '0'
                else -> -1
            }
            if (digitVal >= 0) {
                parsedNum = parsedNum * 10 + digitVal
            }
        }
        val finalNum = if (parsedNum > 0) parsedNum else fallbackNumber
        return ParsedAyah(
            arabicText = textPart,
            ayahNumber = finalNum,
            arabicDigits = finalNum.toArabicNumerals()
        )
    }

    val openIdx = trimmed.lastIndexOf('﴿')
    val closeIdx = trimmed.lastIndexOf('﴾')
    if (openIdx != -1 && closeIdx > openIdx) {
        val textPart = trimmed.substring(0, openIdx).trim()
        val digitsPart = trimmed.substring(openIdx + 1, closeIdx).trim()
        var parsedNum = 0
        for (ch in digitsPart) {
            val digitVal = when (ch) {
                in '٠'..'٩' -> ch - '٠'
                in '0'..'9' -> ch - '0'
                else -> -1
            }
            if (digitVal >= 0) {
                parsedNum = parsedNum * 10 + digitVal
            }
        }
        val finalNum = if (parsedNum > 0) parsedNum else fallbackNumber
        return ParsedAyah(
            arabicText = textPart,
            ayahNumber = finalNum,
            arabicDigits = finalNum.toArabicNumerals()
        )
    }

    return ParsedAyah(
        arabicText = trimmed,
        ayahNumber = fallbackNumber,
        arabicDigits = fallbackNumber.toArabicNumerals()
    )
}
