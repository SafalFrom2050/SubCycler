package com.subtrack.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subtrack.core.common.currency.MoneyFormatter
import com.subtrack.core.domain.model.PortfolioSummary
import com.subtrack.core.ui.theme.BodyFontFamily
import com.subtrack.core.ui.theme.DisplayHeroFontFamily
import com.subtrack.core.ui.theme.ExpressiveMotion
import com.subtrack.core.ui.theme.HeroMoodPalette
import com.subtrack.core.ui.theme.LocalSubCyclerDarkTheme
import com.subtrack.core.ui.theme.orSnapIfReduced
import com.subtrack.core.ui.theme.rememberReducedMotionEnabled
import kotlin.math.roundToInt

/**
 * Material 3 Expressive v4.0 Hero Bento Card with masked silk-waves backdrop,
 * scroll-parallax off-corner `Clover8` polygon watermark, crystal-clear "Effective Monthly"
 * reconciliation, active/paused pill, and interactive topological morphing savings seal.
 */
@Composable
fun FinancialEfficiencyHud(
    summary: PortfolioSummary,
    currencyCode: String = "USD",
    excludedCurrencyCount: Int = 0,
    incompleteCount: Int = 0,
    scrollProgress: Float = 0f,
    heroMood: HeroMoodPalette? = null,
    onSavingsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val reducedMotion = rememberReducedMotionEnabled()
    val hasSavings = summary.totalAnnualSavings > 0.0
    val stickerInteraction = remember { MutableInteractionSource() }
    val stickerPressed by stickerInteraction.collectIsPressedAsState()
    var stickerWiggleCount by remember { mutableIntStateOf(0) }
    val wiggleRotation = remember { Animatable(-6f) }

    LaunchedEffect(stickerWiggleCount, reducedMotion) {
        if (stickerWiggleCount == 0 || reducedMotion) return@LaunchedEffect
        wiggleRotation.animateTo(10f, ExpressiveMotion.expressiveSpatialFastFloat())
        wiggleRotation.animateTo(-14f, ExpressiveMotion.expressiveSpatialFastFloat())
        wiggleRotation.animateTo(-6f, ExpressiveMotion.expressiveSpatialDefaultFloat())
    }

    val badgeScale by animateFloatAsState(
        targetValue = when {
            !hasSavings -> 0.85f
            stickerPressed -> 0.89f
            else -> 1f
        },
        animationSpec = ExpressiveMotion.expressiveSpatialFastFloat().orSnapIfReduced(reducedMotion),
        label = "savingsBadgeScale"
    )

    val clampedScroll = ExpressiveMotion.clampEffectProgress(scrollProgress)
    val boldBadgePalette = ExpressivePalettes.boldPrimary()
    val heroInkColor by animateColorAsState(
        targetValue = heroMood?.contentColor ?: MaterialTheme.colorScheme.onPrimaryContainer,
        animationSpec = ExpressiveMotion.effectsDefaultColor().orSnapIfReduced(reducedMotion),
        label = "heroInkColor"
    )
    val heroPillColor = if (LocalSubCyclerDarkTheme.current) {
        MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.72f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.88f)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        ExpressiveHeroCardBackdrop(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp)),
            watermarkPolygon = ExpressiveMaterialPolygons.Clover8,
            watermarkRotationDegrees = if (reducedMotion) 0f else clampedScroll * 20f,
            watermarkAlpha = 0.09f * (1f - clampedScroll * 0.6f),
            heroMood = heroMood
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ExpressiveEyebrow(
                        text = if (hasSavings) "EFFECTIVE MONTHLY" else "MONTHLY SPEND",
                        color = heroInkColor.copy(alpha = 0.74f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    ExpressiveMoneyText(
                        formattedAmount = MoneyFormatter.format(summary.totalMonthlyOptimizedCost, currencyCode),
                        heroFontSize = 44.sp,
                        color = heroInkColor
                    )
                    if (hasSavings && summary.totalMonthlyStandardCost > summary.totalMonthlyOptimizedCost) {
                        Spacer(modifier = Modifier.height(3.dp))
                        val fullMonthly = MoneyFormatter.format(summary.totalMonthlyStandardCost, currencyCode)
                        val pctOff = summary.overallSavingsPercentage.roundToInt()
                        Text(
                            text = "Down from $fullMonthly/mo · -$pctOff%",
                            color = heroInkColor.copy(alpha = 0.78f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(7.dp))
                        val spendFraction = (summary.totalMonthlyOptimizedCost / summary.totalMonthlyStandardCost)
                            .toFloat()
                            .coerceIn(0.18f, 0.92f)
                        val spendBarColor = heroInkColor.copy(alpha = 0.70f)
                        val savedBarColor = MaterialTheme.colorScheme.primary
                        Canvas(
                            modifier = Modifier
                                .width(148.dp)
                                .height(5.dp)
                        ) {
                            val gap = 3.dp.toPx()
                            val usable = (size.width - gap).coerceAtLeast(1f)
                            val spendW = usable * spendFraction
                            val savedW = (usable - spendW).coerceAtLeast(size.height)
                            val r = CornerRadius(size.height / 2f, size.height / 2f)
                            drawRoundRect(
                                color = spendBarColor,
                                topLeft = Offset.Zero,
                                size = Size(spendW, size.height),
                                cornerRadius = r
                            )
                            drawRoundRect(
                                color = savedBarColor,
                                topLeft = Offset(spendW + gap, 0f),
                                size = Size(savedW, size.height),
                                cornerRadius = r
                            )
                        }
                    }
                    if (summary.totalSubscriptions > 0) {
                        Spacer(modifier = Modifier.height(10.dp))
                        val completeActiveCount = (summary.activeCount - incompleteCount).coerceAtLeast(0)
                        val statusSummary = buildList {
                            if (completeActiveCount > 0) {
                                add(
                                    when {
                                        excludedCurrencyCount > 0 && summary.blackoutCount == 0 && incompleteCount == 0 ->
                                            "$completeActiveCount active in $currencyCode"
                                        summary.blackoutCount == 0 && incompleteCount == 0 ->
                                            if (completeActiveCount == 1) "1 active service" else "$completeActiveCount active services"
                                        else -> "$completeActiveCount active"
                                    }
                                )
                            }
                            if (summary.blackoutCount > 0) {
                                add("${summary.blackoutCount} paused")
                            }
                            if (incompleteCount > 0) {
                                add("$incompleteCount incomplete")
                            }
                        }.joinToString(" · ").let { base ->
                            if (excludedCurrencyCount > 0 && !base.contains(currencyCode) && base.isNotEmpty()) {
                                "$base ($currencyCode)"
                            } else {
                                base
                            }
                        }
                        if (statusSummary.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = heroPillColor
                            ) {
                                Text(
                                    text = statusSummary,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }

                if (hasSavings) {
                    Spacer(modifier = Modifier.width(12.dp))
                    val formattedAmountOnly = MoneyFormatter.format(
                        summary.totalAnnualSavings,
                        currencyCode,
                        maximumFractionDigits = 0
                    )
                    val stampShape = rememberMorphingPolygonShape(
                        targetPolygon = if (stickerPressed) {
                            ExpressiveMaterialPolygons.Sunny
                        } else {
                            ExpressiveMaterialPolygons.Cookie12
                        }
                    )
                    val amountFontSize = when {
                        formattedAmountOnly.length >= 9 -> 13.5.sp
                        formattedAmountOnly.length >= 7 -> 15.5.sp
                        formattedAmountOnly.length >= 5 -> 17.5.sp
                        else -> 19.5.sp
                    }
                    val sealRingColor = boldBadgePalette.content.copy(alpha = 0.22f)

                    Box(
                        modifier = Modifier
                            .size(98.dp)
                            .graphicsLayer {
                                scaleX = badgeScale
                                scaleY = badgeScale
                                rotationZ = wiggleRotation.value
                            }
                            .expressivePolygonSurface(
                                shape = stampShape,
                                color = boldBadgePalette.container
                            )
                            .drawBehind {
                                val inset = 9.dp.toPx()
                                drawCircle(
                                    color = sealRingColor,
                                    radius = (size.minDimension / 2f) - inset,
                                    style = Stroke(
                                        width = 1.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                                    )
                                )
                            }
                            .semantics {
                                contentDescription = "Estimated annual savings $formattedAmountOnly per year. Open Savings."
                            }
                            .clickable(
                                interactionSource = stickerInteraction,
                                indication = null,
                                role = Role.Button
                            ) {
                                stickerWiggleCount++
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSavingsClick?.invoke()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Text(
                                text = "SAVE",
                                fontFamily = BodyFontFamily,
                                color = boldBadgePalette.content.copy(alpha = 0.78f),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 9.5.sp,
                                lineHeight = 10.sp,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = formattedAmountOnly,
                                fontFamily = DisplayHeroFontFamily,
                                color = boldBadgePalette.content,
                                fontWeight = FontWeight.Black,
                                fontSize = amountFontSize,
                                lineHeight = (amountFontSize.value + 1.5f).sp,
                                letterSpacing = (-0.5).sp,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "per year",
                                fontFamily = BodyFontFamily,
                                color = boldBadgePalette.content.copy(alpha = 0.76f),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 9.5.sp,
                                lineHeight = 10.5.sp,
                                letterSpacing = 0.1.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Scroll-Collapsed Pinned Mini-Ledger Capsule (inspired by BunPod's `FeedNowPlayingCard` collapse):
 * Springs cleanly beneath the Dashboard top bar when the user scrolls past [FinancialEfficiencyHud]
 * so monthly spend and annual savings stay glanceable across large portfolios.
 */
@Composable
fun CompactEfficiencyCapsule(
    summary: PortfolioSummary,
    currencyCode: String,
    onSavingsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isDark = LocalSubCyclerDarkTheme.current
    val hasSavings = summary.totalAnnualSavings > 0.0
    val pctOff = summary.overallSavingsPercentage.roundToInt()

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onSavingsClick?.invoke()
        },
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.55f else 0.75f)
        ),
        shadowElevation = if (isDark) 4.dp else 8.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                ExpressiveMoneyText(
                    formattedAmount = MoneyFormatter.format(summary.totalMonthlyOptimizedCost, currencyCode),
                    heroFontSize = 20.sp,
                    cadenceSuffix = "/mo",
                    color = MaterialTheme.colorScheme.onSurface,
                    elevateCurrencySymbol = false
                )
                if (hasSavings && pctOff > 0) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "-$pctOff%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            if (hasSavings) {
                val formattedSave = MoneyFormatter.format(
                    summary.totalAnnualSavings,
                    currencyCode,
                    maximumFractionDigits = 0
                )
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .expressivePolygonSurface(
                                    shape = ExpressiveCookie12Shape,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                        )
                        Text(
                            text = "Save $formattedSave/yr",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}
