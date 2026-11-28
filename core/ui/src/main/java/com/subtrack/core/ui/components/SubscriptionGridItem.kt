package com.subtrack.core.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subtrack.core.common.currency.MoneyFormatter
import com.subtrack.core.domain.model.ActionUrgency
import com.subtrack.core.domain.model.BillingPeriod
import com.subtrack.core.domain.model.CyclePhase
import com.subtrack.core.domain.model.CycleStatus
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.domain.model.SetupMissingItem
import com.subtrack.core.ui.theme.ExpressiveMotion
import com.subtrack.core.ui.theme.LocalSubCyclerDarkTheme
import com.subtrack.core.ui.theme.orSnapIfReduced
import com.subtrack.core.ui.theme.rememberReducedMotionEnabled
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

/**
 * Pure normalized geometry for the unified 2-Zone Wave Vessel grid card:
 * - [hasBreakZone]: true for rotating monthly services (`activeDays > 0 && blackoutDays > 0`).
 * - [activeZoneFraction]: share of card width belonging to the Active window (`0.32..0.80` when rotating, `1.0` otherwise).
 * - [phaseProgress]: progress `0..1` inside the currently active zone (Active, Break, or Setup).
 * - [overallCycleProgress]: monotonic `0..1` progress across the full cycle for accessibility and animation.
 */
data class CycleVesselMetrics(
    val hasBreakZone: Boolean,
    val activeZoneFraction: Float,
    val isActivePhase: Boolean,
    val isIncomplete: Boolean,
    val phaseProgress: Float,
    val overallCycleProgress: Float
)

fun computeCycleVesselMetrics(cycleStatus: CycleStatus): CycleVesselMetrics {
    val sub = cycleStatus.subscription
    if (sub.isIncomplete) {
        val setup = sub.setupCompletionFraction.coerceIn(0.25f, 1f)
        return CycleVesselMetrics(
            hasBreakZone = false,
            activeZoneFraction = 1f,
            isActivePhase = true,
            isIncomplete = true,
            phaseProgress = setup,
            overallCycleProgress = setup
        )
    }

    val isActive = cycleStatus.phase == CyclePhase.ACTIVE
    val phaseProgress = cycleStatus.progressFraction.coerceIn(0f, 1f)
    val effectiveActiveDays = if (cycleStatus.currentCycleNumber == 1) {
        (sub.activeDays + sub.bonusActiveDays.coerceAtLeast(0)).coerceAtLeast(1)
    } else {
        sub.activeDays.coerceAtLeast(1)
    }
    val blackoutDays = sub.blackoutDays.coerceAtLeast(0)
    val hasBreakZone = sub.isRotating && effectiveActiveDays > 0 && blackoutDays > 0

    if (!hasBreakZone) {
        return CycleVesselMetrics(
            hasBreakZone = false,
            activeZoneFraction = 1f,
            isActivePhase = isActive,
            isIncomplete = false,
            phaseProgress = phaseProgress,
            overallCycleProgress = phaseProgress
        )
    }

    val activeZoneFraction = (effectiveActiveDays.toFloat() / (effectiveActiveDays + blackoutDays))
        .coerceIn(0.32f, 0.80f)
    val overallCycleProgress = if (isActive) {
        (phaseProgress * activeZoneFraction).coerceIn(0f, activeZoneFraction)
    } else {
        (activeZoneFraction + phaseProgress * (1f - activeZoneFraction)).coerceIn(activeZoneFraction, 1f)
    }

    return CycleVesselMetrics(
        hasBreakZone = true,
        activeZoneFraction = activeZoneFraction,
        isActivePhase = isActive,
        isIncomplete = false,
        phaseProgress = phaseProgress,
        overallCycleProgress = overallCycleProgress
    )
}

private fun buildVerticalWaveFillPath(
    startX: Float,
    edgeX: Float,
    height: Float,
    amplitudePx: Float,
    cycles: Float = 2.5f,
    phaseRadians: Float = 0.7f
): Path {
    val steps = 40
    val path = Path()
    path.moveTo(startX, 0f)
    path.lineTo(edgeX, 0f)
    for (i in 1..steps) {
        val t = i.toFloat() / steps
        val y = t * height
        val envelope = min(1f, min(t, 1f - t) * 5f)
        val waveX = edgeX + sin(t * (2.0 * PI).toFloat() * cycles + phaseRadians) * amplitudePx * envelope
        path.lineTo(waveX, y)
    }
    path.lineTo(startX, height)
    path.close()
    return path
}

private fun buildVerticalWaveSeamPath(
    edgeX: Float,
    height: Float,
    amplitudePx: Float,
    cycles: Float = 2.5f,
    phaseRadians: Float = 0.7f
): Path {
    val steps = 40
    val path = Path()
    for (i in 0..steps) {
        val t = i.toFloat() / steps
        val y = t * height
        val envelope = min(1f, min(t, 1f - t) * 5f)
        val waveX = edgeX + sin(t * (2.0 * PI).toFloat() * cycles + phaseRadians) * amplitudePx * envelope
        if (i == 0) path.moveTo(waveX, y) else path.lineTo(waveX, y)
    }
    return path
}

/**
 * Roomy Material 3 Expressive v4.1 2-Column Bento Subscription Vessel Card:
 * - Lifecycle-morphing organic badge (`Active`, `UrgentDue`, `Coasting`, `BlackoutRest`, `UnbankedReady`, `Incomplete`).
 * - Unified 2-Zone Wave Vessel: the card background itself encodes the full cycle rhythm
 *   (`[Active liquid wave]` + subtle `45°` hatched `[Break rest zone]` for rotating services),
 *   eliminating duplicate progress bars while gliding seamlessly from Active into Break.
 * - 1-tap scalloped `+¥X · Bank` stamp when a completed break is ready to bank.
 * - Tactile press corner compression (`24dp → 14dp`) on `ExpressiveMotion.expressiveSpatialFast`.
 */
@Composable
fun SubscriptionGridItem(
    cycleStatus: CycleStatus,
    onClick: () -> Unit,
    onBankBreakClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isDark = LocalSubCyclerDarkTheme.current
    val reducedMotion = rememberReducedMotionEnabled()
    val sub = cycleStatus.subscription
    val isIncomplete = sub.isIncomplete
    val isActive = cycleStatus.phase == CyclePhase.ACTIVE
    val isActionDue = !isIncomplete && cycleStatus.nextAction.urgency == ActionUrgency.ACTION_DUE
    val hasUnbankedBreak = !isIncomplete && sub.isRotating && cycleStatus.hasUnbankedCompletedBreak
    val lifecycleState = resolveLifecycleState(cycleStatus)
    val vesselMetrics = remember(cycleStatus) { computeCycleVesselMetrics(cycleStatus) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.955f else 1f,
        animationSpec = ExpressiveMotion.expressiveSpatialFastFloat().orSnapIfReduced(reducedMotion),
        label = "bentoTilePressScale"
    )
    val cardCorner by animateDpAsState(
        targetValue = if (isPressed) 14.dp else 24.dp,
        animationSpec = ExpressiveMotion.expressiveSpatialFastDp().orSnapIfReduced(reducedMotion),
        label = "bentoTileCornerMorph"
    )
    val animatedPhaseProgress by animateFloatAsState(
        targetValue = vesselMetrics.phaseProgress,
        animationSpec = ExpressiveMotion.standardSpatialDefaultFloat().orSnapIfReduced(reducedMotion),
        label = "bentoVesselPhaseProgress"
    )
    val animatedActiveZone by animateFloatAsState(
        targetValue = vesselMetrics.activeZoneFraction,
        animationSpec = ExpressiveMotion.standardSpatialDefaultFloat().orSnapIfReduced(reducedMotion),
        label = "bentoVesselActiveZone"
    )

    val brandPalette = remember(sub.name, sub.colorHex, isDark) {
        com.subtrack.core.ui.theme.BrandColorHarmonizer.harmonizedTonalPalette(
            serviceName = sub.name,
            storedColorHex = sub.colorHex,
            isDark = isDark
        )
    }

    val containerColor = when {
        isIncomplete -> MaterialTheme.colorScheme.surfaceContainer
        isActionDue -> MaterialTheme.colorScheme.errorContainer.copy(alpha = if (isDark) 0.42f else 0.50f)
        !isActive && !vesselMetrics.hasBreakZone -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.34f)
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val cardBorder = if (!isActionDue) expressiveCardBorder() else null

    val activeFillColor = when {
        isIncomplete -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = if (isDark) 0.58f else 0.72f)
        isActionDue -> MaterialTheme.colorScheme.error.copy(alpha = if (isDark) 0.24f else 0.16f)
        else -> brandPalette.accent.copy(alpha = if (isDark) 0.21f else 0.145f)
    }
    val activeCompletedTint = brandPalette.accent.copy(alpha = if (isDark) 0.10f else 0.08f)
    val activeSeamColor = when {
        isIncomplete -> MaterialTheme.colorScheme.outline.copy(alpha = if (isDark) 0.36f else 0.32f)
        isActionDue -> MaterialTheme.colorScheme.error.copy(alpha = if (isDark) 0.78f else 0.68f)
        else -> brandPalette.accent.copy(alpha = if (isDark) 0.84f else 0.72f)
    }
    val breakZoneBaseTint = when {
        !isActive -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = if (isDark) 0.28f else 0.34f)
        else -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = if (isDark) 0.30f else 0.42f)
    }
    val breakFillColor = when {
        isActionDue -> MaterialTheme.colorScheme.error.copy(alpha = if (isDark) 0.26f else 0.18f)
        else -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = if (isDark) 0.54f else 0.54f)
    }
    val breakSeamColor = when {
        isActionDue -> MaterialTheme.colorScheme.error.copy(alpha = if (isDark) 0.78f else 0.68f)
        else -> MaterialTheme.colorScheme.tertiary.copy(alpha = if (isDark) 0.72f else 0.62f)
    }
    val breakHatchColor = if (!isActive) {
        MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = if (isDark) 0.16f else 0.12f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isDark) 0.10f else 0.09f)
    }
    val zoneDividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.48f else 0.56f)

    val isEndingCancelled = !isIncomplete && isActive && cycleStatus.isAutoRenewCancelledForCurrentCycle
    val pillBg = when {
        isIncomplete -> MaterialTheme.colorScheme.surfaceContainerHigh
        isActionDue -> MaterialTheme.colorScheme.errorContainer
        !isActive -> MaterialTheme.colorScheme.tertiaryContainer
        isEndingCancelled -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val pillColor = when {
        isIncomplete -> MaterialTheme.colorScheme.onSurfaceVariant
        isActionDue -> MaterialTheme.colorScheme.onErrorContainer
        !isActive -> MaterialTheme.colorScheme.onTertiaryContainer
        isEndingCancelled -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val pillText = when {
        isIncomplete -> when (sub.missingSetupItems.singleOrNull()) {
            SetupMissingItem.PRICE -> "Needs price"
            SetupMissingItem.BILLING_DATE -> "Needs date"
            SetupMissingItem.CYCLE -> "Needs cycle"
            null -> "Setup · ${sub.setupCompletedSteps}/${sub.setupTotalSteps}"
        }
        isActionDue -> "Due · ${cycleStatus.daysRemainingInPhase}d"
        sub.billingPeriod == BillingPeriod.YEARLY -> "Yearly · ${cycleStatus.daysRemainingInPhase}d"
        isEndingCancelled -> "Ending · ${cycleStatus.daysRemainingInPhase}d"
        isActive -> "${cycleStatus.daysRemainingInPhase}d left"
        else -> "Paused · ${cycleStatus.daysRemainingInPhase}d"
    }

    val cadenceTag = when {
        sub.billingPeriod == BillingPeriod.YEARLY -> ""
        !sub.isStrategyConfirmed -> ""
        sub.strategy == RotationStrategy.ROTATION_45_DAYS -> " · 45d"
        sub.strategy == RotationStrategy.ROTATION_60_DAYS -> " · 60d"
        sub.strategy == RotationStrategy.CUSTOM -> " · ${sub.totalCycleDays}d"
        else -> " · Always"
    }

    val priceSubtitle = if (sub.cost > 0.0) {
        "${MoneyFormatter.format(sub.cost, sub.currency)}${sub.billingPeriod.shortSuffix}$cadenceTag"
    } else {
        "Price not set$cadenceTag"
    }

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(vesselMetrics.overallCycleProgress, 0f..1f)
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        shape = RoundedCornerShape(cardCorner),
        color = containerColor,
        border = cardBorder,
        tonalElevation = 0.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Unified Material 3 Expressive 2-Zone Wave Vessel Background
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                if (w <= 0f || h <= 0f) return@Canvas

                val hasBreak = vesselMetrics.hasBreakZone
                val splitX = if (hasBreak) (w * animatedActiveZone).coerceIn(w * 0.25f, w * 0.85f) else w
                val baseWaveAmp = 3.2.dp.toPx()
                val seamStrokeWidth = 1.65.dp.toPx()
                val hatchSpacing = 9.dp.toPx()
                val hatchStroke = 1.dp.toPx()

                // 1. Right-hand Break rest zone architecture (rotating services only)
                if (hasBreak) {
                    drawRect(
                        color = breakZoneBaseTint,
                        topLeft = Offset(splitX, 0f),
                        size = Size((w - splitX).coerceAtLeast(0f), h)
                    )
                    clipRect(left = splitX, top = 0f, right = w, bottom = h) {
                        var x = splitX - h
                        while (x <= w + h) {
                            drawLine(
                                color = breakHatchColor,
                                start = Offset(x, h),
                                end = Offset(x + h, 0f),
                                strokeWidth = hatchStroke
                            )
                            x += hatchSpacing
                        }
                    }
                }

                // 2. Liquid Wave Fill (either inside Active/Single zone or advancing across Break zone)
                if (!hasBreak || vesselMetrics.isActivePhase) {
                    val minLeadPx = min(14.dp.toPx(), splitX * 0.35f)
                    val edgeX = if (animatedPhaseProgress <= 0f) {
                        0f
                    } else {
                        (minLeadPx + (splitX - minLeadPx) * animatedPhaseProgress).coerceIn(0f, splitX)
                    }
                    if (edgeX > 1f) {
                        val boundaryDamp = ((splitX - edgeX) / 8.dp.toPx()).coerceIn(0f, 1f)
                        val waveAmp = baseWaveAmp * boundaryDamp
                        clipRect(left = 0f, top = 0f, right = splitX, bottom = h) {
                            drawPath(
                                path = buildVerticalWaveFillPath(
                                    startX = 0f,
                                    edgeX = edgeX,
                                    height = h,
                                    amplitudePx = waveAmp
                                ),
                                color = activeFillColor
                            )
                            if (edgeX in 4f..(splitX - 2f)) {
                                drawPath(
                                    path = buildVerticalWaveSeamPath(
                                        edgeX = edgeX,
                                        height = h,
                                        amplitudePx = waveAmp
                                    ),
                                    color = activeSeamColor,
                                    style = Stroke(width = seamStrokeWidth, cap = StrokeCap.Round)
                                )
                            }
                        }
                    }

                    // Subtle dashed Active | Break divider while in Active phase
                    if (hasBreak) {
                        val insetY = 10.dp.toPx().coerceAtMost(h * 0.25f)
                        drawLine(
                            color = zoneDividerColor,
                            start = Offset(splitX, insetY),
                            end = Offset(splitX, h - insetY),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(3.dp.toPx(), 3.5.dp.toPx()),
                                0f
                            )
                        )
                    }
                } else {
                    // Blackout / Break phase: completed Active zone rests in muted brand tint,
                    // while cool-slate liquid wave glides across the right-hand hatched Break zone.
                    drawRect(
                        color = activeCompletedTint,
                        topLeft = Offset.Zero,
                        size = Size(splitX, h)
                    )
                    val breakWidth = (w - splitX).coerceAtLeast(0f)
                    val minBreakLeadPx = min(10.dp.toPx(), breakWidth * 0.30f)
                    val breakEdgeX = if (animatedPhaseProgress <= 0f) {
                        splitX
                    } else {
                        (splitX + minBreakLeadPx + (breakWidth - minBreakLeadPx) * animatedPhaseProgress)
                            .coerceIn(splitX, w)
                    }
                    if (breakEdgeX > splitX + 1f) {
                        val boundaryDamp = ((w - breakEdgeX) / 8.dp.toPx()).coerceIn(0f, 1f)
                        val waveAmp = (2.6.dp.toPx()) * boundaryDamp
                        clipRect(left = splitX, top = 0f, right = w, bottom = h) {
                            drawPath(
                                path = buildVerticalWaveFillPath(
                                    startX = splitX,
                                    edgeX = breakEdgeX,
                                    height = h,
                                    amplitudePx = waveAmp
                                ),
                                color = breakFillColor
                            )
                            if (breakEdgeX in (splitX + 4f)..(w - 2f)) {
                                drawPath(
                                    path = buildVerticalWaveSeamPath(
                                        edgeX = breakEdgeX,
                                        height = h,
                                        amplitudePx = waveAmp
                                    ),
                                    color = breakSeamColor,
                                    style = Stroke(width = seamStrokeWidth, cap = StrokeCap.Round)
                                )
                            }
                        }
                    }

                    // Crisp solid boundary between completed Active zone and active Break zone
                    drawLine(
                        color = zoneDividerColor,
                        start = Offset(splitX, 0f),
                        end = Offset(splitX, h),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 142.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: Lifecycle-Morphing Organic Badge + Status Pill or Scalloped Bank Stamp
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    ExpressiveServiceBadge(
                        serviceName = sub.name,
                        colorHex = sub.colorHex,
                        size = 46.dp,
                        fontSize = 15.sp,
                        isPressed = isPressed,
                        lifecycleState = lifecycleState
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    if (hasUnbankedBreak) {
                        val popAccent = com.subtrack.core.ui.theme.LocalSubCyclerAccentTokens.current
                        val bankText = "+${MoneyFormatter.format(cycleStatus.perBreakSavings, sub.currency, maximumFractionDigits = 0)}"
                        Box(
                            modifier = Modifier
                                .graphicsLayer { rotationZ = -4f }
                                .clip(RoundedCornerShape(999.dp))
                                .background(popAccent.heroPop)
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (onBankBreakClick != null) onBankBreakClick() else onClick()
                                }
                                .padding(horizontal = 9.dp, vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$bankText · Bank",
                                color = popAccent.onHeroPop,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (isActionDue) {
                                Surface(
                                    modifier = Modifier.size(18.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.error
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.PriorityHigh,
                                            contentDescription = "Action Required",
                                            tint = MaterialTheme.colorScheme.onError,
                                            modifier = Modifier.size(11.dp)
                                        )
                                    }
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = pillBg
                            ) {
                                Text(
                                    text = pillText,
                                    color = pillColor,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column {
                    Text(
                        text = sub.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = priceSubtitle,
                        style = MaterialTheme.typography.labelMedium,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
