package com.meridia.shared.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * Typography transcribed from the demo's type scale.
 *
 * The demo uses two web fonts — Bricolage Grotesque (display) and Instrument Sans
 * (body). Those font files are not yet bundled, so we fall back to the platform
 * sans-serif while keeping the demo's weights, sizes and letter-spacing. To ship the
 * real faces later, drop the .ttf files into
 * `shared/src/commonMain/composeResources/font/` and swap the two families below —
 * no call site changes.
 */
private val DisplayFamily = FontFamily.SansSerif // → Bricolage Grotesque
private val BodyFamily = FontFamily.SansSerif // → Instrument Sans

val MeridiaTypography = Typography(
    // Screen title — demo `h2.title` (26px / 800 / -.02em)
    displaySmall = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 29.sp,
        letterSpacing = (-0.02).em,
    ),
    // Hero / success heading — demo `.hero h3` (22px / 800 / -.02em)
    headlineMedium = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.02).em,
    ),
    // Brand / sheet header — demo `.brand b` (17px display)
    titleLarge = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.01).em,
    ),
    // Card / section header (16px)
    titleMedium = TextStyle(
        fontFamily = DisplayFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
    ),
    // Default body copy (15px)
    bodyLarge = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 23.sp,
    ),
    // Secondary copy — demo `.muted` (13.5px)
    bodyMedium = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 21.sp,
    ),
    // Fine print (12.5px)
    bodySmall = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.5.sp,
        lineHeight = 18.sp,
    ),
    // Button label — demo `.btn` (15px / 700)
    labelLarge = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 18.sp,
    ),
    // Chip label — demo `.chip` (11.5px / 700)
    labelMedium = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.5.sp,
        lineHeight = 14.sp,
    ),
    // Eyebrow — demo `.eyebrow` (11px / 700 / .14em, upper-cased at the call site)
    labelSmall = TextStyle(
        fontFamily = BodyFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.14.em,
    ),
)
