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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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

/**
 * Roomy Material 3 Expressive v4.0 2-Column Bento Subscription Vessel Card:
 * - Lifecycle-morphing organic badge (`Active`, `UrgentDue`, `Coasting`, `BlackoutRest`, `UnbankedReady`, `Incomplete`).
 * - Whisper-soft 70/30 ambient horizontal vessel fill (`0..progressFraction`) with fine `45°` rest-hatching during Blackout.
 * - 1-tap scalloped `Cookie7` `+¥X · Bank` stamp when a completed break is ready to bank.
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
    val vesselProgress by animateFloatAsState(
        targetValue = if (isIncomplete) 0f else cycleStatus.progressFraction.coerceIn(0f, 1f),
        animationSpec = ExpressiveMotion.standardSpatialDefaultFloat().orSnapIfReduced(reducedMotion),
        label = "bentoVesselFill"
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
        isActionDue -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
        !isActive -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.34f)
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val cardBorder = if (!isActionDue) expressiveCardBorder() else null

    val vesselFillColor = when {
        isActionDue -> MaterialTheme.colorScheme.error.copy(alpha = if (isDark) 0.16f else 0.11f)
        !isActive -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = if (isDark) 0.30f else 0.34f)
        else -> brandPalette.container.copy(alpha = if (isDark) 0.38f else 0.44f)
    }
    val vesselSeamColor = when {
        isActionDue -> MaterialTheme.colorScheme.error.copy(alpha = 0.28f)
        !isActive -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.26f)
        else -> brandPalette.accent.copy(alpha = if (isDark) 0.32f else 0.24f)
    }
    val restHatchColor = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = if (isDark) 0.12f else 0.08f)

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
            // Whisper-soft 70/30 Ambient Horizon Vessel Fill (0..progressFraction)
            if (!isIncomplete && vesselProgress > 0.02f) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val fillWidth = (size.width * vesselProgress).coerceIn(0f, size.width)
                    drawRect(
                        color = vesselFillColor,
                        topLeft = Offset.Zero,
                        size = Size(fillWidth, size.height)
                    )
                    if (!isActive) {
                        clipRect(left = 0f, top = 0f, right = fillWidth, bottom = size.height) {
                            val step = 11.dp.toPx()
                            var x = -size.height
                            while (x <= fillWidth + size.height) {
                                drawLine(
                                    color = restHatchColor,
                                    start = Offset(x, size.height),
                                    end = Offset(x + size.height, 0f),
                                    strokeWidth = 1.dp.toPx()
                                )
                                x += step
                            }
                        }
                    }
                    if (fillWidth in 4f..(size.width - 4f)) {
                        drawLine(
                            color = vesselSeamColor,
                            start = Offset(fillWidth, 0f),
                            end = Offset(fillWidth, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
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
                            val bankText = "+${MoneyFormatter.format(cycleStatus.perBreakSavings, sub.currency, maximumFractionDigits = 0)}"
                            Box(
                                modifier = Modifier
                                    .graphicsLayer { rotationZ = -4f }
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(MaterialTheme.colorScheme.primary)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (onBankBreakClick != null) onBankBreakClick() else onClick()
                                    }
                                    .padding(horizontal = 9.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$bankText · Bank",
                                    color = MaterialTheme.colorScheme.onPrimary,
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

                    Spacer(modifier = Modifier.height(14.dp))

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

                Spacer(modifier = Modifier.height(12.dp))

                // Sculpted Mini Cycle-Rhythm Track or Setup Progress Bar
                if (isIncomplete) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(sub.setupCompletionFraction.coerceIn(0.12f, 1f))
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.outline)
                        )
                    }
                } else {
                    CycleRhythmTrack(
                        progress = cycleStatus.progressFraction,
                        isActivePhase = isActive,
                        activeDays = if (sub.isRotating) sub.activeDays else 0,
                        blackoutDays = if (sub.isRotating) sub.blackoutDays else 0,
                        activeColor = if (isActionDue) MaterialTheme.colorScheme.error else brandPalette.accent,
                        pausedColor = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}
