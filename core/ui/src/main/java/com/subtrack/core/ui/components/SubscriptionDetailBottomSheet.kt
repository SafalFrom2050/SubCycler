package com.subtrack.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.subtrack.core.domain.model.ActionType
import com.subtrack.core.domain.model.ActionUrgency
import com.subtrack.core.domain.model.BillingPeriod
import com.subtrack.core.domain.model.CyclePhase
import com.subtrack.core.domain.model.CycleStatus
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.domain.model.SubscriptionActionProgressType
import com.subtrack.core.ui.theme.LocalSubCyclerDarkTheme
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionDetailBottomSheet(
    cycleStatus: CycleStatus,
    onDismiss: () -> Unit,
    onToggleAutoRenew: () -> Unit,
    onStartSubscriptionAction: (SubscriptionActionProgressType) -> Unit = {},
    onExtendCycle: () -> Unit = {},
    onResumeToday: () -> Unit = {},
    onEdit: () -> Unit,
    onEditStep: (Int) -> Unit = { onEdit() },
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isDark = LocalSubCyclerDarkTheme.current
    val cardBorder = expressiveCardBorder()
    val sub = cycleStatus.subscription
    val isIncomplete = sub.isIncomplete
    val isActive = cycleStatus.phase == CyclePhase.ACTIVE
    val isYearly = sub.billingPeriod == BillingPeriod.YEARLY
    val isRotating = sub.isRotating
    val isCancelledForCycle = cycleStatus.isAutoRenewCancelledForCurrentCycle
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.padding(vertical = 10.dp)
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header: Expressive Organic Badge + Name + Category + Price
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    ExpressiveServiceBadge(
                        serviceName = sub.name,
                        size = 54.dp,
                        fontSize = 17.sp
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = sub.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 21.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = sub.category.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    if (sub.cost > 0.0) {
                        ExpressiveMoneyText(
                            formattedAmount = MoneyFormatter.format(sub.cost, sub.currency),
                            heroFontSize = 24.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            elevateCurrencySymbol = false
                        )
                        Text(
                            text = when {
                                isYearly -> "/year"
                                !isRotating -> "/month"
                                else -> "/active mo"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Price not set",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "incomplete",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Unified Architectural Neutral Status, Timeline & Annual Savings Card (True 70/30 Restraint!)
            val action = cycleStatus.nextAction
            val isDue = !isIncomplete && action.urgency == ActionUrgency.ACTION_DUE
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = cardBorder
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val dotColor = when {
                            isIncomplete -> MaterialTheme.colorScheme.primary
                            isDue -> MaterialTheme.colorScheme.error
                            isActive -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        val phaseText = when {
                            isIncomplete -> "INCOMPLETE · ${sub.setupCompletedSteps}/${sub.setupTotalSteps}"
                            isYearly -> "ANNUAL · ${cycleStatus.daysRemainingInPhase}d left"
                            isActive -> "ACTIVE · ${cycleStatus.daysRemainingInPhase}d left"
                            else -> "PAUSED · ${cycleStatus.daysRemainingInPhase}d to re-sub"
                        }

                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(dotColor)
                                )
                                Spacer(modifier = Modifier.width(7.dp))
                                Text(
                                    text = phaseText,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        val strategyText = when {
                            isYearly -> "Yearly plan"
                            !sub.isStrategyConfirmed -> "Cycle not set"
                            sub.strategy == RotationStrategy.ROTATION_45_DAYS -> "30d on · 15d off"
                            sub.strategy == RotationStrategy.ROTATION_60_DAYS -> "30d on · 30d off"
                            sub.strategy == RotationStrategy.CUSTOM -> "${sub.activeDays}d on · ${sub.blackoutDays}d off"
                            else -> "Always on"
                        }

                        Text(
                            text = strategyText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isIncomplete) {
                        val missingLabel = sub.missingSetupItems.joinToString(", ") { it.label }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(sub.setupCompletionFraction)
                                    .fillMaxHeight()
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Missing: $missingLabel",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Finish these details to unlock countdown tracking, savings math, and quiet local reminders.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val progressColor = if (isActive) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        WavyProgressIndicator(
                            progress = cycleStatus.progressFraction,
                            activeColor = progressColor,
                            animateWave = isActive,
                            activeDays = if (isRotating) sub.activeDays else 0,
                            blackoutDays = if (isRotating) sub.blackoutDays else 0,
                            isActivePhase = isActive
                        )

                        if (action.actionType != ActionType.NO_ACTION || isCancelledForCycle) {
                            Spacer(modifier = Modifier.height(12.dp))
                            if (isDue) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(9.dp))
                                        Text(
                                            text = action.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when {
                                            isCancelledForCycle && isActive -> Icons.Default.CheckCircle
                                            action.actionType == ActionType.CANCEL_AUTO_RENEW -> Icons.Default.NotificationsNone
                                            action.actionType == ActionType.RESUBSCRIBE -> Icons.Default.Autorenew
                                            else -> Icons.Default.Check
                                        },
                                        contentDescription = null,
                                        tint = if (isCancelledForCycle && isActive) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = action.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 12.5.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        if (isRotating && sub.cost > 0.0) {
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Estimated annual cost",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (sub.completedBreaksCount > 0) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${sub.completedBreaksCount} break${if (sub.completedBreaksCount == 1) "" else "s"} completed · ${MoneyFormatter.format(sub.earnedSavingsTotal, sub.currency, maximumFractionDigits = 0)} earned",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = MoneyFormatter.format(cycleStatus.optimizedAnnualCost, sub.currency),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Saves ${MoneyFormatter.format(cycleStatus.annualSavings, sub.currency, maximumFractionDigits = 0)}/yr",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Watchlist / Notes Card
            if (sub.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = cardBorder
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        ExpressiveEyebrow(text = "WATCHLIST · NOTES")
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = sub.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Contextual Cycle Actions (hidden while incomplete so user isn't shown noisy cycle actions yet)
            if (!isIncomplete && isRotating && cycleStatus.hasUnbankedCompletedBreak) {
                Spacer(modifier = Modifier.height(14.dp))
                val formattedBreakSave = MoneyFormatter.format(
                    cycleStatus.perBreakSavings,
                    sub.currency,
                    maximumFractionDigits = 0
                )
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onResumeToday()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        "Break finished · Bank ~$formattedBreakSave & restart",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            } else if (!isIncomplete && isRotating && isActive && !isCancelledForCycle && cycleStatus.daysRemainingInPhase <= 10) {
                Spacer(modifier = Modifier.height(14.dp))
                FilledTonalButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onExtendCycle()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("Skip upcoming break (+${sub.activeDays}d)", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary CTA: "Finish setup" when incomplete, else contextual Billing Portal CTA
            val isUrgentCancel = !isIncomplete && isRotating && isActive && !isCancelledForCycle && cycleStatus.daysRemainingInPhase <= 7
            val isPausedForResub = !isIncomplete && isRotating && !isActive
            val primaryBtnInteraction = remember { MutableInteractionSource() }
            val primaryBtnPressed by primaryBtnInteraction.collectIsPressedAsState()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when {
                    isIncomplete -> {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onDismiss()
                                onEditStep(sub.firstMissingSetupStepIndex)
                            },
                            interactionSource = primaryBtnInteraction,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text("Finish setup", fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                        }
                    }
                    isUrgentCancel -> {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onStartSubscriptionAction(SubscriptionActionProgressType.UNSUBSCRIBE)
                            },
                            interactionSource = primaryBtnInteraction,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            ReactiveIcon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                size = 16.dp,
                                motion = ReactiveMotionType.NUDGE_RIGHT,
                                isPressed = primaryBtnPressed
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cancel auto-renew", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    isPausedForResub -> {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onStartSubscriptionAction(SubscriptionActionProgressType.SUBSCRIBE)
                            },
                            interactionSource = primaryBtnInteraction,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            ReactiveIcon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                size = 16.dp,
                                motion = ReactiveMotionType.NUDGE_RIGHT,
                                isPressed = primaryBtnPressed
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Re-subscribe", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                    else -> {
                        val btnCorner by animateDpAsState(
                            targetValue = if (primaryBtnPressed) 16.dp else 26.dp,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "manageBtnCorner"
                        )
                        FilledTonalButton(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onStartSubscriptionAction(SubscriptionActionProgressType.UNSUBSCRIBE)
                            },
                            interactionSource = primaryBtnInteraction,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            shape = RoundedCornerShape(btnCorner),
                            border = if (isDark) {
                                BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.34f))
                            } else null,
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        ) {
                            ReactiveIcon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                size = 16.dp,
                                motion = ReactiveMotionType.NUDGE_RIGHT,
                                isPressed = primaryBtnPressed
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isRotating) "Manage / Cancel" else "Manage billing",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            if (!isIncomplete && isActive && isRotating) {
                Spacer(modifier = Modifier.height(10.dp))
                val toggleInteraction = remember { MutableInteractionSource() }
                val togglePressed by toggleInteraction.collectIsPressedAsState()
                val toggleCorner by animateDpAsState(
                    targetValue = if (togglePressed) 13.dp else 18.dp,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "autoRenewCorner"
                )
                val toggleBg by animateColorAsState(
                    targetValue = if (isCancelledForCycle) {
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isDark) 0.72f else 0.78f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                    animationSpec = tween(180),
                    label = "autoRenewBg"
                )
                val badgeScale by animateFloatAsState(
                    targetValue = if (isCancelledForCycle) 1.06f else 0.94f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    ),
                    label = "autoRenewCheckScale"
                )
                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggleAutoRenew()
                    },
                    interactionSource = toggleInteraction,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(toggleCorner),
                    color = toggleBg,
                    border = if (isCancelledForCycle) {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                    } else {
                        cardBorder
                    }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .graphicsLayer {
                                    scaleX = badgeScale
                                    scaleY = badgeScale
                                }
                                .clip(if (isCancelledForCycle) ExpressiveCookie8Shape else CircleShape)
                                .background(
                                    if (isCancelledForCycle) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    },
                                    if (isCancelledForCycle) ExpressiveCookie8Shape else CircleShape
                                )
                                .then(
                                    if (!isCancelledForCycle) {
                                        Modifier.border(
                                            1.5.dp,
                                            MaterialTheme.colorScheme.outlineVariant,
                                            CircleShape
                                        )
                                    } else {
                                        Modifier
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCancelledForCycle) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Auto-renew cancelled",
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isCancelledForCycle) "Auto-renew cancelled" else "Mark auto-renew as cancelled",
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isCancelledForCycle) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                            Text(
                                text = if (isCancelledForCycle) {
                                    "Cancel reminders paused for this cycle"
                                } else {
                                    "Stops cancel reminders for this cycle"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                color = if (isCancelledForCycle) {
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            } else if (!isIncomplete && !isActive && isRotating) {
                val canBankSavings = cycleStatus.phaseStartDate.isBefore(LocalDate.now())
                Spacer(modifier = Modifier.height(6.dp))
                TextButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onResumeToday()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (canBankSavings) {
                            "Already re-subscribed? Mark active & bank savings"
                        } else {
                            "Already re-subscribed? Mark active"
                        },
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Secondary Filled Tonal Pill Buttons (Edit & Delete with confirmation)
            val editInteraction = remember { MutableInteractionSource() }
            val editPressed by editInteraction.collectIsPressedAsState()
            val deleteInteraction = remember { MutableInteractionSource() }
            val deletePressed by deleteInteraction.collectIsPressedAsState()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalButton(
                    onClick = {
                        onDismiss()
                        onEdit()
                    },
                    interactionSource = editInteraction,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    ReactiveIcon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onSurface,
                        size = 15.dp,
                        motion = ReactiveMotionType.POP_BOUNCE,
                        isPressed = editPressed
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                FilledTonalButton(
                    onClick = { showDeleteConfirm = true },
                    interactionSource = deleteInteraction,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    ReactiveIcon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        size = 15.dp,
                        motion = ReactiveMotionType.BELL_RING,
                        isPressed = deletePressed
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            title = {
                Text(
                    text = "Delete ${sub.name}?",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Text(
                    text = "This removes ${sub.name} and its local reminders from SubCycler. You can undo right after deleting.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDismiss()
                        onDelete()
                    },
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Delete", fontWeight = FontWeight.ExtraBold)
                }
            }
        )
    }
}
