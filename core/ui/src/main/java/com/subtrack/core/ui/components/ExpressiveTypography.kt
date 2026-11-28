package com.subtrack.core.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subtrack.core.ui.theme.BodyFontFamily
import com.subtrack.core.ui.theme.DisplayFontFamily
import com.subtrack.core.ui.theme.DisplayHeroFontFamily
import com.subtrack.core.ui.theme.ExpressiveMotion
import com.subtrack.core.ui.theme.rememberReducedMotionEnabled

internal data class ParsedMoneyParts(
    val prefix: String,
    val integerPart: String,
    val fractionalPart: String,
    val suffix: String
)

/**
 * Splits a locale-formatted currency string (e.g. "$12.16", "¥6,469", "CA$16.49", "12,99 €")
 * into prefix symbol, integer digits, optional fractional decimal part, and trailing suffix
 * so [ExpressiveMoneyText] can apply Material 3 Expressive split-scale hierarchy safely across
 * every world locale and currency.
 */
internal fun parseFormattedMoney(formattedAmount: String): ParsedMoneyParts {
    val firstDigit = formattedAmount.indexOfFirst { it.isDigit() }
    val lastDigit = formattedAmount.indexOfLast { it.isDigit() }
    if (firstDigit == -1 || lastDigit == -1 || firstDigit > lastDigit) {
        return ParsedMoneyParts(
            prefix = "",
            integerPart = formattedAmount,
            fractionalPart = "",
            suffix = ""
        )
    }

    val prefix = formattedAmount.substring(0, firstDigit)
    val core = formattedAmount.substring(firstDigit, lastDigit + 1)
    val suffix = formattedAmount.substring(lastDigit + 1)

    val lastSepIndex = core.indexOfLast { it == '.' || it == ',' }
    if (lastSepIndex > 0) {
        val trailing = core.substring(lastSepIndex + 1)
        val sepChar = core[lastSepIndex]
        val isLikelyDecimal = trailing.length in 1..2 &&
            trailing.all { it.isDigit() } &&
            core.count { it == sepChar } == 1
        if (isLikelyDecimal) {
            return ParsedMoneyParts(
                prefix = prefix,
                integerPart = core.substring(0, lastSepIndex),
                fractionalPart = core.substring(lastSepIndex),
                suffix = suffix
            )
        }
    }

    return ParsedMoneyParts(
        prefix = prefix,
        integerPart = core,
        fractionalPart = "",
        suffix = suffix
    )
}

/**
 * Material 3 Expressive split-scale currency display:
 * - Sculptural oversized tabular integer digits in [DisplayHeroFontFamily]
 * - Gently elevated, softer currency prefix symbol in [DisplayFontFamily]
 * - Baseline-anchored smaller fractional cents for effortless scan contrast
 * - Optional cadence suffix (e.g. "/yr" or "/mo")
 */
@Composable
fun ExpressiveMoneyText(
    formattedAmount: String,
    modifier: Modifier = Modifier,
    heroFontSize: TextUnit = 44.sp,
    color: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    cadenceSuffix: String = "",
    textAlign: TextAlign? = null,
    elevateCurrencySymbol: Boolean = true
) {
    val parts = parseFormattedMoney(formattedAmount)
    val heroValue = heroFontSize.value
    val symbolSize = (heroValue * 0.58f).coerceAtLeast(12f).sp
    val fractionSize = (heroValue * 0.54f).coerceAtLeast(12f).sp
    val suffixSize = (heroValue * 0.36f).coerceAtLeast(11f).sp
    val heroTracking = (-heroValue * 0.036f).sp

    val styledText = buildAnnotatedString {
        if (parts.prefix.isNotEmpty()) {
            withStyle(
                SpanStyle(
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = symbolSize,
                    color = color.copy(alpha = 0.78f),
                    letterSpacing = (-0.4).sp,
                    baselineShift = if (elevateCurrencySymbol && parts.prefix.length <= 2) {
                        BaselineShift(0.13f)
                    } else {
                        BaselineShift(0.06f)
                    }
                )
            ) {
                append(parts.prefix)
            }
        }
        withStyle(
            SpanStyle(
                fontFamily = DisplayHeroFontFamily,
                fontWeight = FontWeight.Black,
                fontSize = heroFontSize,
                color = color,
                letterSpacing = heroTracking,
                fontFeatureSettings = "tnum"
            )
        ) {
            append(parts.integerPart)
        }
        if (parts.fractionalPart.isNotEmpty()) {
            withStyle(
                SpanStyle(
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = fractionSize,
                    color = color.copy(alpha = 0.76f),
                    letterSpacing = (-0.4).sp,
                    fontFeatureSettings = "tnum"
                )
            ) {
                append(parts.fractionalPart)
            }
        }
        if (parts.suffix.isNotEmpty()) {
            withStyle(
                SpanStyle(
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = symbolSize,
                    color = color.copy(alpha = 0.76f)
                )
            ) {
                append(parts.suffix)
            }
        }
        if (cadenceSuffix.isNotEmpty()) {
            withStyle(
                SpanStyle(
                    fontFamily = BodyFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = suffixSize,
                    color = color.copy(alpha = 0.68f),
                    letterSpacing = 0.sp
                )
            ) {
                append(cadenceSuffix)
            }
        }
    }

    Text(
        text = styledText,
        modifier = modifier,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = textAlign,
        style = TextStyle(
            lineHeight = (heroValue * 1.06f).sp,
            fontFeatureSettings = "tnum"
        )
    )
}

/**
 * Expressive two-tone, weight-contrasted SubCycler wordmark pairing high-optical-size
 * Black "Sub" with warmer Botanical Emerald SemiBold "Cycler".
 */
@Composable
fun ExpressiveBrandWordmark(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 28.sp,
    textAlign: TextAlign? = null
) {
    val sizeValue = fontSize.value
    Text(
        text = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    fontFamily = DisplayHeroFontFamily,
                    fontWeight = FontWeight.Black,
                    fontSize = fontSize,
                    letterSpacing = (-sizeValue * 0.036f).sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
            ) {
                append("Sub")
            }
            withStyle(
                SpanStyle(
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = fontSize,
                    letterSpacing = (-sizeValue * 0.026f).sp,
                    color = MaterialTheme.colorScheme.primary
                )
            ) {
                append("Cycler")
            }
        },
        modifier = modifier,
        maxLines = 1,
        textAlign = textAlign,
        style = TextStyle(lineHeight = (sizeValue * 1.08f).sp)
    )
}

/**
 * Kinetic Top-Bar Brand Header:
 * Pairs SubCycler's botanical swirl brand emblem (which performs a smooth spring rotation
 * whenever [statusTicker] reports a local confirmation) beside a vertical slot-ticker.
 */
@Composable
fun ExpressiveKineticBrandHeader(
    modifier: Modifier = Modifier,
    statusTicker: String? = null,
    swirlColors: com.subtrack.core.ui.theme.SwirlLogoColors = com.subtrack.core.ui.theme.BrandColorHarmonizer.DefaultBotanicalSwirl,
    fontSize: TextUnit = 28.sp
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val spinAngle = remember { Animatable(0f) }

    LaunchedEffect(statusTicker, reducedMotion) {
        if (statusTicker == null) return@LaunchedEffect
        if (!reducedMotion) {
            spinAngle.animateTo(
                targetValue = spinAngle.value + 360f,
                animationSpec = ExpressiveMotion.expressiveSpatialDefaultFloat()
            )
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SubCyclerSwirlLogo(
            colors = swirlColors,
            size = 34.dp,
            cornerRadius = 10.dp,
            rotationDegreesProvider = { spinAngle.value },
            contentDescriptionText = "SubCycler logo"
        )
        Spacer(modifier = Modifier.width(10.dp))
        AnimatedContent(
            targetState = statusTicker,
            transitionSpec = {
                if (reducedMotion) {
                    fadeIn(tween(0)).togetherWith(fadeOut(tween(0)))
                } else {
                    val enteringStatus = targetState != null
                    (slideInVertically(tween(210)) { h -> if (enteringStatus) h else -h } + fadeIn(tween(180)))
                        .togetherWith(
                            slideOutVertically(tween(180)) { h -> if (enteringStatus) -h else h } + fadeOut(tween(140))
                        )
                }
            },
            label = "kineticBrandTicker"
        ) { ticker ->
            if (ticker == null) {
                ExpressiveBrandWordmark(fontSize = fontSize)
            } else {
                Text(
                    text = ticker,
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 21.sp,
                    letterSpacing = (-0.4).sp,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Sculptural Material 3 Expressive screen title for secondary tabs (Planner, Savings),
 * anchored with SubCycler's live mood-harmonized swirl emblem so the top bar stays visually
 * unified across every tab.
 */
@Composable
fun ExpressiveScreenTitle(
    text: String,
    modifier: Modifier = Modifier,
    showBrandIcon: Boolean = true
) {
    val swirlColors = com.subtrack.core.ui.theme.LocalSubCyclerSwirlColors.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showBrandIcon) {
            SubCyclerSwirlLogo(
                colors = swirlColors,
                size = 34.dp,
                cornerRadius = 10.dp,
                contentDescriptionText = "SubCycler logo"
            )
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.headlineLarge,
            fontFamily = DisplayHeroFontFamily,
            fontWeight = FontWeight.Black,
            fontSize = 28.sp,
            letterSpacing = (-0.65).sp,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1
        )
    }
}

/**
 * Shared sculptural `Cookie12` Settings action button matching Home's top bar across every main tab.
 */
@Composable
fun ExpressiveSettingsActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActive: Boolean = false
) {
    val isDark = com.subtrack.core.ui.theme.LocalSubCyclerDarkTheme.current
    ExpressiveReactiveIconButton(
        imageVector = Icons.Default.Settings,
        contentDescription = "Settings",
        onClick = onClick,
        modifier = modifier.padding(end = 12.dp),
        shape = ExpressiveCookie12Shape,
        containerColor = if (isDark) {
            MaterialTheme.colorScheme.surfaceContainerHigh
        } else {
            MaterialTheme.colorScheme.surfaceContainer
        },
        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.42f else 0.58f),
        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
        buttonSize = 44.dp,
        iconSize = 20.dp,
        motion = ReactiveMotionType.GEAR_SPIN,
        externalTrigger = if (isActive) 1 else 0
    )
}

/**
 * Wide-tracked uppercase editorial eyebrow label contrasting against tight display headings.
 */
@Composable
fun ExpressiveEyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    fontSize: TextUnit = 11.sp
) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        fontFamily = BodyFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = fontSize,
        letterSpacing = 1.4.sp,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
