package com.subtrack.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.subtrack.core.ui.R

/**
 * Material 3 Expressive Variable Typography System (100% offline-bundled SIL OFL fonts):
 *
 * 1. [DisplayHeroFontFamily] — Bricolage Grotesque Variable at high optical size (`opsz = 72–84`)
 *    and sculptural slightly-condensed width (`wdth = 90–93`). Delivers dramatic thick-to-thin
 *    stroke contrast and playful calligraphic ink-traps for hero spend numerals, screen titles,
 *    and the SubCycler brand wordmark.
 *
 * 2. [DisplayFontFamily] — Bricolage Grotesque Variable at balanced editorial optical size
 *    (`opsz = 32`, `wdth = 98`). Gives section titles, bento card names, dialog headers, and
 *    service monogram badges warm, unmistakable character without hurting scannability.
 *
 * 3. [BodyFontFamily] — Plus Jakarta Sans Variable (`wght = 400–800`). Clean, contemporary
 *    geometric sans for UI copy, pills, buttons, wide-tracked editorial eyebrows, and inputs.
 *
 * 4. [DisplayExpandedFontFamily] — Google Sans Flex Expanded (`wdth = 125`, `wght = 800`,
 *    `ROND = 35`, SIL OFL v1.1). Wide, openly proportioned Material 3 Expressive display face
 *    for prominent section titles.
 */
@OptIn(ExperimentalTextApi::class)
val DisplayHeroFontFamily = FontFamily(
    Font(
        resId = R.font.bricolage_grotesque_variable,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(500),
            FontVariation.width(94f),
            FontVariation.opticalSizing(64.sp)
        )
    ),
    Font(
        resId = R.font.bricolage_grotesque_variable,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(600),
            FontVariation.width(94f),
            FontVariation.opticalSizing(68.sp)
        )
    ),
    Font(
        resId = R.font.bricolage_grotesque_variable,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(700),
            FontVariation.width(93f),
            FontVariation.opticalSizing(72.sp)
        )
    ),
    Font(
        resId = R.font.bricolage_grotesque_variable,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(800),
            FontVariation.width(92f),
            FontVariation.opticalSizing(76.sp)
        )
    ),
    Font(
        resId = R.font.bricolage_grotesque_variable,
        weight = FontWeight.Black,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(800),
            FontVariation.width(90f),
            FontVariation.opticalSizing(84.sp)
        )
    )
)

@OptIn(ExperimentalTextApi::class)
val DisplayFontFamily = FontFamily(
    Font(
        resId = R.font.bricolage_grotesque_variable,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(400),
            FontVariation.width(98f),
            FontVariation.opticalSizing(24.sp)
        )
    ),
    Font(
        resId = R.font.bricolage_grotesque_variable,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(500),
            FontVariation.width(98f),
            FontVariation.opticalSizing(28.sp)
        )
    ),
    Font(
        resId = R.font.bricolage_grotesque_variable,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(600),
            FontVariation.width(98f),
            FontVariation.opticalSizing(32.sp)
        )
    ),
    Font(
        resId = R.font.bricolage_grotesque_variable,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(700),
            FontVariation.width(97f),
            FontVariation.opticalSizing(36.sp)
        )
    ),
    Font(
        resId = R.font.bricolage_grotesque_variable,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(800),
            FontVariation.width(96f),
            FontVariation.opticalSizing(42.sp)
        )
    ),
    Font(
        resId = R.font.bricolage_grotesque_variable,
        weight = FontWeight.Black,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(800),
            FontVariation.width(94f),
            FontVariation.opticalSizing(48.sp)
        )
    )
)

@OptIn(ExperimentalTextApi::class)
val BodyFontFamily = FontFamily(
    Font(
        resId = R.font.plus_jakarta_sans_variable,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))
    ),
    Font(
        resId = R.font.plus_jakarta_sans_variable,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))
    ),
    Font(
        resId = R.font.plus_jakarta_sans_variable,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))
    ),
    Font(
        resId = R.font.plus_jakarta_sans_variable,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    ),
    Font(
        resId = R.font.plus_jakarta_sans_variable,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800))
    ),
    Font(
        resId = R.font.plus_jakarta_sans_variable,
        weight = FontWeight.Black,
        variationSettings = FontVariation.Settings(FontVariation.weight(800))
    )
)

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

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = DisplayHeroFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 44.sp,
        lineHeight = 48.sp,
        letterSpacing = (-1.5).sp,
        fontFeatureSettings = "tnum"
    ),
    displayMedium = TextStyle(
        fontFamily = DisplayHeroFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = (-1.1).sp,
        fontFeatureSettings = "tnum"
    ),
    displaySmall = TextStyle(
        fontFamily = DisplayHeroFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.9).sp,
        fontFeatureSettings = "tnum"
    ),
    headlineLarge = TextStyle(
        fontFamily = DisplayHeroFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 30.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.9).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplayHeroFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.7).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 22.sp,
        lineHeight = 27.sp,
        letterSpacing = (-0.5).sp
    ),
    titleLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.4).sp
    ),
    titleMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 21.sp,
        letterSpacing = (-0.25).sp
    ),
    titleSmall = TextStyle(
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        letterSpacing = (-0.1).sp
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
        letterSpacing = (-0.1).sp,
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
