package com.subtrack.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
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
import com.subtrack.core.ui.theme.LocalSubCyclerDarkTheme
import kotlin.math.roundToInt

/**
 * Material 3 Expressive Hero Bento Card with masked silk-waves backdrop,
 * crystal-clear "Effective Monthly" vs full-price reconciliation, active/paused pill,
 * and an interactive 12-petal scalloped savings badge that opens the Savings tab.
 */
@Composable
fun FinancialEfficiencyHud(
    summary: PortfolioSummary,
    currencyCode: String = "USD",
    excludedCurrencyCount: Int = 0,
    incompleteCount: Int = 0,
    onSavingsClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val hasSavings = summary.totalAnnualSavings > 0.0
    val stickerInteraction = remember { MutableInteractionSource() }
    val stickerPressed by stickerInteraction.collectIsPressedAsState()
    var stickerWiggleCount by remember { mutableIntStateOf(0) }
    val wiggleRotation = remember { Animatable(-6f) }

    LaunchedEffect(stickerWiggleCount) {
        if (stickerWiggleCount == 0) return@LaunchedEffect
        wiggleRotation.animateTo(10f, spring(stiffness = Spring.StiffnessHigh))
        wiggleRotation.animateTo(-14f, spring(stiffness = Spring.StiffnessMedium))
        wiggleRotation.animateTo(
            -6f,
            spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    val badgeScale by animateFloatAsState(
        targetValue = when {
            !hasSavings -> 0.85f
            stickerPressed -> 0.89f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "savingsBadgeScale"
    )

    val boldBadgePalette = ExpressivePalettes.boldPrimary()
    val heroPillColor = if (LocalSubCyclerDarkTheme.current) {
        MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.72f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.88f)
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = androidx.compose.ui.graphics.Color.Transparent,
        tonalElevation = 0.dp
    ) {
        ExpressiveHeroCardBackdrop(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
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
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.74f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    ExpressiveMoneyText(
                        formattedAmount = MoneyFormatter.format(summary.totalMonthlyOptimizedCost, currencyCode),
                        heroFontSize = 44.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    if (hasSavings && summary.totalMonthlyStandardCost > summary.totalMonthlyOptimizedCost) {
                        Spacer(modifier = Modifier.height(3.dp))
                        val fullMonthly = MoneyFormatter.format(summary.totalMonthlyStandardCost, currencyCode)
                        val pctOff = summary.overallSavingsPercentage.roundToInt()
                        Text(
                            text = "Down from $fullMonthly/mo · -$pctOff%",
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(7.dp))
                        // Duotone Spend-vs-Saved Micro Proportion Bar
                        val spendFraction = (summary.totalMonthlyOptimizedCost / summary.totalMonthlyStandardCost)
                            .toFloat()
                            .coerceIn(0.18f, 0.92f)
                        val spendBarColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.70f)
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
                    val stampGroove by animateFloatAsState(
                        targetValue = if (stickerPressed) 0.11f else 0.075f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium
                        ),
                        label = "savingsStampGroove"
                    )
                    val stampShape = remember(stampGroove) {
                        ScallopedCookieShape(lobes = 12, grooveDepthFraction = stampGroove)
                    }
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
                            .clip(stampShape)
                            .background(boldBadgePalette.container, stampShape)
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
