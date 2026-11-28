package com.subtrack.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.subtrack.core.ui.R

/**
 * Unified Material 3 Expressive Typography System powered by the 100% offline-bundled
 * SIL OFL v1.1 **Google Sans Flex** superfamily across three purposeful width axes:
 *
 * 1. [DisplayExpandedFontFamily] — Google Sans Flex Expanded (`wdth = 125`, `wght = 800`,
 *    `ROND = 35`). Wide, openly proportioned expressive display face for screen titles
 *    (`Planner`, `Savings`) and prominent section headers (`Your stack`, `Cycles`, `Breakdown`).
 *
 * 2. [DisplayHeroFontFamily] — Google Sans Flex Semi-Wide (`wdth = 112.5`, `wght = 600–800`,
 *    `ROND = 30`). Sculptural, confident numerals and headlines (`$28.15`, `$350`, `SubCycler`
 *    wordmark, Welcome headline) that stay punchy without overflowing longer currencies.
 *
 * 3. [DisplayFontFamily] — Google Sans Flex Semi-Wide + Standard (`wght = 400–800`). Warm,
 *    cohesive character for bento service names, dialog headers, and monogram badges.
 *
 * 4. [BodyFontFamily] — Google Sans Flex Standard (`wdth = 100`, `wght = 400–700`, `ROND = 15`).
 *    Crisp, high-legibility geometric sans for UI copy, chips, buttons, dock labels, and inputs.
 */
val DisplayExpandedFontFamily = FontFamily(
    Font(
        resId = R.font.google_sans_flex_expanded_extrabold,
        weight = FontWeight.Bold
    ),
    Font(
        resId = R.font.google_sans_flex_expanded_extrabold,
        weight = FontWeight.ExtraBold
    ),
    Font(
        resId = R.font.google_sans_flex_expanded_extrabold,
        weight = FontWeight.Black
    )
)

val DisplayHeroFontFamily = FontFamily(
    Font(
        resId = R.font.google_sans_flex_medium,
        weight = FontWeight.Medium
    ),
    Font(
        resId = R.font.google_sans_flex_wide_semibold,
        weight = FontWeight.SemiBold
    ),
    Font(
        resId = R.font.google_sans_flex_wide_bold,
        weight = FontWeight.Bold
    ),
    Font(
        resId = R.font.google_sans_flex_wide_extrabold,
        weight = FontWeight.ExtraBold
    ),
    Font(
        resId = R.font.google_sans_flex_wide_extrabold,
        weight = FontWeight.Black
    )
)

val DisplayFontFamily = FontFamily(
    Font(
        resId = R.font.google_sans_flex_regular,
        weight = FontWeight.Normal
    ),
    Font(
        resId = R.font.google_sans_flex_medium,
        weight = FontWeight.Medium
    ),
    Font(
        resId = R.font.google_sans_flex_wide_semibold,
        weight = FontWeight.SemiBold
    ),
    Font(
        resId = R.font.google_sans_flex_wide_bold,
        weight = FontWeight.Bold
    ),
    Font(
        resId = R.font.google_sans_flex_wide_extrabold,
        weight = FontWeight.ExtraBold
    ),
    Font(
        resId = R.font.google_sans_flex_wide_extrabold,
        weight = FontWeight.Black
    )
)

val BodyFontFamily = FontFamily(
    Font(
        resId = R.font.google_sans_flex_regular,
        weight = FontWeight.Normal
    ),
    Font(
        resId = R.font.google_sans_flex_medium,
        weight = FontWeight.Medium
    ),
    Font(
        resId = R.font.google_sans_flex_semibold,
        weight = FontWeight.SemiBold
    ),
    Font(
        resId = R.font.google_sans_flex_bold,
        weight = FontWeight.Bold
    ),
    Font(
        resId = R.font.google_sans_flex_bold,
        weight = FontWeight.ExtraBold
    ),
    Font(
        resId = R.font.google_sans_flex_bold,
        weight = FontWeight.Black
    )
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = DisplayHeroFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 44.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.9).sp,
        fontFeatureSettings = "tnum"
    ),
    displayMedium = TextStyle(
        fontFamily = DisplayHeroFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.7).sp,
        fontFeatureSettings = "tnum"
    ),
    displaySmall = TextStyle(
        fontFamily = DisplayHeroFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = "tnum"
    ),
    headlineLarge = TextStyle(
        fontFamily = DisplayHeroFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplayHeroFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.25).sp
    ),
    titleLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 21.sp,
        letterSpacing = (-0.1).sp
    ),
    titleSmall = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.5.sp,
        lineHeight = 19.sp
    ),
    bodySmall = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp
    ),
    labelLarge = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum"
    ),
    labelMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum"
    ),
    labelSmall = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.3.sp,
        fontFeatureSettings = "tnum"
    )
)
