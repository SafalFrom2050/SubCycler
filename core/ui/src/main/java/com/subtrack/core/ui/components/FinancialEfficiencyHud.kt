package com.subtrack.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Material 3 Expressive v5.0 Hero Bento Card with Pure Flat Color-Block backdrop,
 * scroll-parallax off-corner `Clover8` polygon watermark, crystal-clear "Effective Monthly"
 * reconciliation, active/paused pill with stack-mood dots, curly spend/savings efficiency wave,
 * and interactive topological morphing savings seal.
 */
@Composable
fun FinancialEfficiencyHud(
    summary: PortfolioSummary,
    currencyCode: String = "USD",
    excludedCurrencyCount: Int = 0,
    incompleteCount: Int = 0,
    scrollProgress: Float = 0f,
    heroMood: HeroMoodPalette? = null,
    isEntranceReady: Boolean = true,
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

    // One-shot launch choreography gated on splash dissolve so the Hero unfurls visibly
    val skipEntrance = isEntranceReady || reducedMotion
    val cardReveal = remember { Animatable(if (skipEntrance) 1f else 0f) }
    val waveReveal = remember { Animatable(if (skipEntrance) 1f else 0f) }
    val sealReveal = remember { Animatable(if (skipEntrance) 1f else 0f) }

    LaunchedEffect(isEntranceReady, reducedMotion) {
        if (!isEntranceReady) return@LaunchedEffect
        if (reducedMotion) {
            cardReveal.snapTo(1f)
            waveReveal.snapTo(1f)
            sealReveal.snapTo(1f)
            return@LaunchedEffect
        }
        if (cardReveal.value >= 0.99f && waveReveal.value >= 0.99f && sealReveal.value >= 0.99f) {
            return@LaunchedEffect
        }
        launch {
            cardReveal.animateTo(1f, ExpressiveMotion.expressiveSpatialDefaultFloat())
        }
        launch {
            delay(60L)
            waveReveal.animateTo(1f, ExpressiveMotion.expressiveSpatialDefaultFloat())
        }
        launch {
            delay(90L)
            sealReveal.animateTo(1f, ExpressiveMotion.expressiveSpatialFastFloat())
        }
    }

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
    val isDark = LocalSubCyclerDarkTheme.current
    val accentTokens = com.subtrack.core.ui.theme.LocalSubCyclerAccentTokens.current
    val heroInkColor by animateColorAsState(
        targetValue = heroMood?.contentColor ?: accentTokens.heroContent,
        animationSpec = ExpressiveMotion.effectsDefaultColor().orSnapIfReduced(reducedMotion),
        label = "heroInkColor"
    )
    val sealBgColor by animateColorAsState(
        targetValue = heroMood?.sealColor ?: accentTokens.heroPop,
        animationSpec = ExpressiveMotion.effectsDefaultColor().orSnapIfReduced(reducedMotion),
        label = "heroSealBgColor"
    )
    val sealInkColor by animateColorAsState(
        targetValue = heroMood?.onSealColor ?: accentTokens.onHeroPop,
        animationSpec = ExpressiveMotion.effectsDefaultColor().orSnapIfReduced(reducedMotion),
        label = "heroSealInkColor"
    )
    val heroPillColor = if (isDark) {
        Color.Black.copy(alpha = 0.28f)
    } else {
        Color.White.copy(alpha = 0.22f)
    }
    val stackDots = heroMood?.stackAccentDots.orEmpty()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                val raw = cardReveal.value
                val clamped = ExpressiveMotion.clampEffectProgress(raw * 1.45f)
                alpha = clamped
                val s = 0.96f + 0.04f * raw
                scaleX = s
                scaleY = s
                translationY = (1f - raw) * 14.dp.toPx()
            },
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        val watermarkSettle = if (reducedMotion) 0f else (1f - ExpressiveMotion.clampEffectProgress(cardReveal.value)) * 14f
        ExpressiveHeroCardBackdrop(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp)),
            watermarkPolygon = ExpressiveMaterialPolygons.Clover8,
            watermarkRotationDegrees = if (reducedMotion) 0f else (clampedScroll * 20f + watermarkSettle),
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
                        Spacer(modifier = Modifier.height(5.dp))
                        val targetSpendFraction = (summary.totalMonthlyOptimizedCost / summary.totalMonthlyStandardCost)
                            .toFloat()
                            .coerceIn(0.22f, 0.88f)
                        val animatedSpendFraction by animateFloatAsState(
                            targetValue = targetSpendFraction,
                            animationSpec = ExpressiveMotion.standardSpatialDefaultFloat().orSnapIfReduced(reducedMotion),
                            label = "heroSpendWaveFraction"
                        )
                        val spendWaveColor = heroInkColor.copy(alpha = 0.90f)
                        val savedBarColor = sealBgColor
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 172.dp)
                                .height(16.dp)
                                .semantics {
                                    contentDescription = "Effective monthly spend reduced by $pctOff percent"
                                }
                        ) {
                            val strokeW = 4.6.dp.toPx()
                            val halfStroke = strokeW / 2f
                            val savedPillH = 5.6.dp.toPx()
                            val visualGap = 6.5.dp.toPx()
                            val amplitude = 3.0.dp.toPx()
                            val wavelength = 21.dp.toPx()
                            val centerY = size.height / 2f

                            val waveVisualEndX = (size.width - visualGap) * animatedSpendFraction
                            val waveStartX = halfStroke
                            val targetWaveEndX = (waveVisualEndX - halfStroke).coerceAtLeast(waveStartX + 1f)
                            val revealRaw = waveReveal.value
                            val waveProgress = ExpressiveMotion.clampEffectProgress(revealRaw * 1.12f)
                            val waveEndX = (waveStartX + (targetWaveEndX - waveStartX) * waveProgress)
                                .coerceAtLeast(waveStartX + 1f)

                            // 1. Sculptural Material 3 Expressive curly Spend wave (draws across smoothly on launch)
                            if (waveProgress > 0.02f) {
                                val wavePath = Path()
                                val stepPx = 1.5f
                                val rampPx = 8.dp.toPx()
                                val phaseShift = (1f - waveProgress) * 1.6f
                                var x = waveStartX
                                var first = true
                                while (x <= waveEndX) {
                                    val distStart = x - waveStartX
                                    val distEnd = waveEndX - x
                                    val envelope = min(1f, min(distStart, distEnd) / rampPx).coerceIn(0f, 1f)
                                    val angle = (distStart / wavelength) * (2.0 * PI).toFloat() + phaseShift
                                    val y = centerY + amplitude * envelope * sin(angle)
                                    if (first) {
                                        wavePath.moveTo(x, y)
                                        first = false
                                    } else {
                                        wavePath.lineTo(x, y)
                                    }
                                    x += stepPx
                                }
                                wavePath.lineTo(waveEndX, centerY)
                                drawPath(
                                    path = wavePath,
                                    color = spendWaveColor,
                                    style = Stroke(
                                        width = strokeW,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }

                            // 2. Matching chunky Triadic-accent Saved pill springs in after the Spend wave lands
                            val savedReveal = ((revealRaw - 0.42f) / 0.58f).coerceIn(0f, 1.06f)
                            if (savedReveal > 0.02f) {
                                val savedStartX = (waveVisualEndX + visualGap).coerceAtMost(size.width - savedPillH)
                                val targetSavedW = (size.width - savedStartX).coerceAtLeast(savedPillH)
                                val animatedSavedW = (targetSavedW * savedReveal).coerceIn(savedPillH * 0.6f, size.width - savedStartX)
                                val savedRadius = CornerRadius(savedPillH / 2f, savedPillH / 2f)
                                drawRoundRect(
                                    color = savedBarColor.copy(alpha = ExpressiveMotion.clampEffectProgress(savedReveal * 1.5f)),
                                    topLeft = Offset(savedStartX, centerY - savedPillH / 2f),
                                    size = Size(animatedSavedW, savedPillH),
                                    cornerRadius = savedRadius
                                )
                            }
                        }
                    }
                    if (summary.totalSubscriptions > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
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
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Text(
                                        text = statusSummary,
                                        color = heroInkColor,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (stackDots.size >= 2) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(3.5.dp)
                                        ) {
                                            stackDots.forEach { dotColor ->
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.5.dp)
                                                        .clip(CircleShape)
                                                        .background(dotColor)
                                                        .border(
                                                            width = 0.75.dp,
                                                            color = Color.White.copy(alpha = 0.55f),
                                                            shape = CircleShape
                                                        )
                                                )
                                            }
                                        }
                                    }
                                }
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
                    val sealRingColor = sealInkColor.copy(alpha = 0.28f)

                    Box(
                        modifier = Modifier
                            .size(98.dp)
                            .graphicsLayer {
                                val sr = sealReveal.value
                                val revealScale = 0.72f + 0.28f * sr
                                val clampedSr = ExpressiveMotion.clampEffectProgress(sr * 1.4f)
                                alpha = clampedSr
                                scaleX = badgeScale * revealScale
                                scaleY = badgeScale * revealScale
                                rotationZ = wiggleRotation.value - (1f - ExpressiveMotion.clampEffectProgress(sr)) * 12f
                            }
                            .expressivePolygonSurface(
                                shape = stampShape,
                                color = sealBgColor
                            )
                            .drawBehind {
                                val inset = 9.dp.toPx()
                                drawCircle(
                                    color = sealRingColor,
                                    radius = (size.minDimension / 2f) - inset,
                                    style = Stroke(
                                        width = 1.1.dp.toPx(),
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
                                color = sealInkColor.copy(alpha = 0.78f),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 9.5.sp,
                                lineHeight = 10.sp,
                                letterSpacing = 1.2.sp
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = formattedAmountOnly,
                                fontFamily = DisplayHeroFontFamily,
                                color = sealInkColor,
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
                                color = sealInkColor.copy(alpha = 0.76f),
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
