package com.subtrack.core.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.subtrack.core.ui.theme.LocalSubCyclerDarkTheme

/**
 * Roomy Material 3 Expressive 2-Column Bento Subscription Card.
 * Features light-mode architectural hairline contrast, organic monogram badge,
 * full readable service name, price + cycle cadence subtitle, and subtle cycle progress bar.
 */
@Composable
fun SubscriptionGridItem(
    cycleStatus: CycleStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isDark = LocalSubCyclerDarkTheme.current
    val sub = cycleStatus.subscription
    val isIncomplete = sub.isIncomplete
    val isActive = cycleStatus.phase == CyclePhase.ACTIVE
    val isActionDue = !isIncomplete && cycleStatus.nextAction.urgency == ActionUrgency.ACTION_DUE

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bentoTilePressScale"
    )
    val cardCorner by animateDpAsState(
        targetValue = if (isPressed) 15.dp else 24.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bentoTileCornerMorph"
    )

    val containerColor = when {
        isIncomplete -> MaterialTheme.colorScheme.surfaceContainer
        isActionDue -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)
        !isActive -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.38f)
        else -> MaterialTheme.colorScheme.surfaceContainer
    }
    val cardBorder = if (!isActionDue) expressiveCardBorder() else null

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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                // Top Row: Organic Service Badge + Status Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    ExpressiveServiceBadge(
                        serviceName = sub.name,
                        size = 46.dp,
                        fontSize = 15.sp,
                        isPressed = isPressed
                    )
                    Spacer(modifier = Modifier.width(6.dp))
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

            // Sculpted Mini Cycle-Rhythm Track (shows Active + Hatched Break cadence!) or Setup Progress Bar
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
                    activeColor = if (isActionDue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    pausedColor = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}
