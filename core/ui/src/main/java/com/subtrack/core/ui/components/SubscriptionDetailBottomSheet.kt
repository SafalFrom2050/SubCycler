package com.subtrack.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.subtrack.core.ui.theme.DisplayFontFamily
import com.subtrack.core.ui.theme.DisplayHeroFontFamily
import com.subtrack.core.ui.theme.ExpressiveMotion
import com.subtrack.core.ui.theme.LocalSubCyclerDarkTheme
import com.subtrack.core.ui.theme.orSnapIfReduced
import com.subtrack.core.ui.theme.rememberReducedMotionEnabled
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * Pure domain-view projection for a finger-scrubbed point inside the Cycle Time-Machine wave.
 */
data class CycleScrubPreview(
    val dayIndexInCycle: Int,
    val totalCycleDays: Int,
    val previewDate: LocalDate,
    val isInBreakZone: Boolean,
    val countdownLabel: String,
    val detailCaption: String
)

private val scrubDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE, MMM d")

/**
 * Computes the date, phase, countdown label, and status caption at any normalized
 * position [scrubFraction] (`0f..1f`) across a subscription's full rotation cycle.
 */
fun computeCycleScrubPreview(
    cycleStatus: CycleStatus,
    scrubFraction: Float
): CycleScrubPreview {
    val sub = cycleStatus.subscription
    val isRotating = sub.isRotating && sub.blackoutDays > 0
    val activeDays = if (sub.billingPeriod == BillingPeriod.YEARLY) {
        365
    } else {
        (sub.activeDays + sub.bonusActiveDays).coerceAtLeast(1)
    }
    val breakDays = if (isRotating) sub.blackoutDays.coerceAtLeast(0) else 0
    val totalDays = (activeDays + breakDays).coerceAtLeast(1)
    val clamped = scrubFraction.coerceIn(0f, 1f)
    val dayIndex = (clamped * totalDays).roundToInt().coerceIn(0, totalDays)

    val cycleStartDate = if (cycleStatus.phase == CyclePhase.ACTIVE) {
        cycleStatus.phaseStartDate
    } else {
        cycleStatus.phaseStartDate.minusDays(activeDays.toLong())
    }
    val previewDate = cycleStartDate.plusDays(dayIndex.toLong())
    val formattedDate = previewDate.format(scrubDateFormatter)

    val inBreak = isRotating && dayIndex >= activeDays
    return if (!inBreak) {
        val remainingActive = (activeDays - dayIndex).coerceAtLeast(0)
        val caption = when {
            isRotating && remainingActive == 0 -> "$formattedDate · Turn off auto-renew · Break begins"
            isRotating && remainingActive <= 3 -> "$formattedDate · Cancel window ($remainingActive d to break)"
            else -> "$formattedDate · Active day ${(dayIndex + 1).coerceAtMost(activeDays)} of $activeDays"
        }
        CycleScrubPreview(
            dayIndexInCycle = dayIndex,
            totalCycleDays = totalDays,
            previewDate = previewDate,
            isInBreakZone = false,
            countdownLabel = if (remainingActive == 0 && isRotating) "Swap day" else "${remainingActive}d left",
            detailCaption = caption
        )
    } else {
        val breakDay = (dayIndex - activeDays + 1).coerceIn(1, breakDays.coerceAtLeast(1))
        val breakRemaining = (totalDays - dayIndex).coerceAtLeast(0)
        val perBreakFormatted = MoneyFormatter.format(
            cycleStatus.perBreakSavings,
            sub.currency,
            maximumFractionDigits = 0
        )
        val caption = if (breakRemaining == 0) {
            "$formattedDate · Break complete · Ready to bank ~$perBreakFormatted"
        } else {
            "$formattedDate · Day $breakDay of ${breakDays}d break · Unbilled rest"
        }
        CycleScrubPreview(
            dayIndexInCycle = dayIndex,
            totalCycleDays = totalDays,
            previewDate = previewDate,
            isInBreakZone = true,
            countdownLabel = if (breakRemaining == 0) "+$perBreakFormatted" else "-${breakRemaining}d rest",
            detailCaption = caption
        )
    }
}

/**
 * Resolves the semantic lifecycle badge state for a [CycleStatus].
 */
fun resolveLifecycleState(cycleStatus: CycleStatus): ServiceBadgeLifecycleState {
    val sub = cycleStatus.subscription
    return when {
        sub.isIncomplete -> ServiceBadgeLifecycleState.INCOMPLETE
        sub.isRotating && cycleStatus.hasUnbankedCompletedBreak -> ServiceBadgeLifecycleState.UNBANKED_READY
        cycleStatus.phase == CyclePhase.BLACKOUT -> ServiceBadgeLifecycleState.BLACKOUT_REST
        cycleStatus.nextAction.urgency == ActionUrgency.ACTION_DUE -> ServiceBadgeLifecycleState.URGENT_DUE
        cycleStatus.isAutoRenewCancelledForCurrentCycle -> ServiceBadgeLifecycleState.COASTING_CANCELLED
        else -> ServiceBadgeLifecycleState.ACTIVE
    }
}

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
    val reducedMotion = rememberReducedMotionEnabled()
    val cardBorder = expressiveCardBorder()
    val sub = cycleStatus.subscription
    val brandPalette = remember(sub.name, sub.colorHex, isDark) {
        com.subtrack.core.ui.theme.BrandColorHarmonizer.harmonizedTonalPalette(
            serviceName = sub.name,
            storedColorHex = sub.colorHex,
            isDark = isDark
        )
    }
    val isIncomplete = sub.isIncomplete
    val isActive = cycleStatus.phase == CyclePhase.ACTIVE
    val isYearly = sub.billingPeriod == BillingPeriod.YEARLY
    val isRotating = sub.isRotating
    val isCancelledForCycle = cycleStatus.isAutoRenewCancelledForCurrentCycle
    val lifecycleState = resolveLifecycleState(cycleStatus)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showInlineDeleteConfirm by remember { mutableStateOf(false) }

    // Interactive Cycle Time-Machine scrub state
    var scrubFraction by remember { mutableStateOf<Float?>(null) }
    var isScrubbing by remember { mutableStateOf(false) }
    var lastHapticDay by remember { mutableIntStateOf(-1) }
    var lastInBreakZone by remember { mutableStateOf(false) }
    var scrubberWidthPx by remember { mutableFloatStateOf(1f) }

    val scrubPreview = remember(cycleStatus, scrubFraction) {
        scrubFraction?.let { computeCycleScrubPreview(cycleStatus, it) }
    }

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
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp)
        ) {
            // 1. Editorial Header: Lifecycle-Morphing Badge + Category Eyebrow + Name + Price
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
                        colorHex = sub.colorHex,
                        size = 54.dp,
                        fontSize = 17.sp,
                        lifecycleState = lifecycleState
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        ExpressiveEyebrow(
                            text = sub.category.displayName.uppercase(),
                            color = if (isActive && !isIncomplete) brandPalette.accent else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = sub.name,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 21.sp,
                            letterSpacing = (-0.4).sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

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

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Unified Architectural Status Card + Monumental Countdown + Interactive Cycle Time-Machine Wave
            val action = cycleStatus.nextAction
            val isDue = !isIncomplete && action.urgency == ActionUrgency.ACTION_DUE
            val statusSurfaceColor = when {
                isActive && !isDue && !isIncomplete -> androidx.compose.ui.graphics.lerp(
                    MaterialTheme.colorScheme.surfaceContainer,
                    brandPalette.container,
                    if (isDark) 0.36f else 0.38f
                )
                else -> MaterialTheme.colorScheme.surfaceContainer
            }
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                color = statusSurfaceColor,
                border = cardBorder
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val dotColor = when {
                            scrubPreview != null && scrubPreview.isInBreakZone -> MaterialTheme.colorScheme.tertiary
                            scrubPreview != null -> MaterialTheme.colorScheme.primary
                            isIncomplete -> MaterialTheme.colorScheme.primary
                            isDue -> MaterialTheme.colorScheme.error
                            isActive -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.tertiary
                        }
                        val phaseBadgeLabel = when {
                            scrubPreview != null && scrubPreview.isInBreakZone -> "PREVIEW · BREAK"
                            scrubPreview != null -> "PREVIEW · ACTIVE"
                            isIncomplete -> "INCOMPLETE · ${sub.setupCompletedSteps}/${sub.setupTotalSteps}"
                            isYearly -> "ANNUAL PLAN"
                            isCancelledForCycle && isActive -> "COASTING TO BREAK"
                            isActive -> "ACTIVE WINDOW"
                            else -> "PAUSED BREAK"
                        }

                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = if (scrubPreview != null) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            }
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
                                    text = phaseBadgeLabel,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                )
                            }
                        }

                        val strategyText = when {
                            isYearly -> "Yearly"
                            !sub.isStrategyConfirmed -> "Cycle unset"
                            sub.strategy == RotationStrategy.ROTATION_45_DAYS -> "30d on · 15d off"
                            sub.strategy == RotationStrategy.ROTATION_60_DAYS -> "30d on · 30d off"
                            sub.strategy == RotationStrategy.CUSTOM -> "${sub.activeDays}d on · ${sub.blackoutDays}d off"
                            else -> "Always on"
                        }
                        Text(
                            text = strategyText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (isIncomplete) {
                        Spacer(modifier = Modifier.height(14.dp))
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
                            text = "Add price, date, and cycle to track.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.5.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Spacer(modifier = Modifier.height(10.dp))

                        // Monumental right-aligned tabular countdown (BunPod PlayerProgress inspiration!)
                        val restingCountdown = when {
                            isActive -> "${cycleStatus.daysRemainingInPhase}d left"
                            else -> "-${cycleStatus.daysRemainingInPhase}d rest"
                        }
                        val displayCountdown = scrubPreview?.countdownLabel ?: restingCountdown
                        val countdownColor by animateColorAsState(
                            targetValue = when {
                                scrubPreview != null && scrubPreview.isInBreakZone -> MaterialTheme.colorScheme.tertiary
                                isDue && scrubPreview == null -> MaterialTheme.colorScheme.error
                                !isActive && scrubPreview == null -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            animationSpec = ExpressiveMotion.effectsFastColor().orSnapIfReduced(reducedMotion),
                            label = "countdownColor"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = scrubPreview?.detailCaption ?: action.description.ifBlank {
                                    if (isActive) "Active billing cycle" else "Unbilled blackout break"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 12.sp,
                                fontWeight = if (scrubPreview != null) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (scrubPreview != null) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 10.dp, bottom = 4.dp)
                            )

                            Text(
                                text = displayCountdown,
                                fontFamily = DisplayHeroFontFamily,
                                fontWeight = FontWeight.Black,
                                fontSize = 36.sp,
                                lineHeight = 38.sp,
                                letterSpacing = (-1.2).sp,
                                color = countdownColor,
                                maxLines = 1,
                                style = MaterialTheme.typography.displaySmall.copy(
                                    fontFeatureSettings = "tnum"
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Interactive "Cycle Time-Machine" Wavy Scrubber!
                        val progressColor = when {
                            isDue -> MaterialTheme.colorScheme.error
                            isActive -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.tertiary
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .onSizeChanged { scrubberWidthPx = it.width.toFloat().coerceAtLeast(1f) }
                                .pointerInput(cycleStatus.subscription.id, sub.activeDays, sub.blackoutDays) {
                                    detectHorizontalDragGestures(
                                        onDragStart = { offset ->
                                            isScrubbing = true
                                            val frac = (offset.x / scrubberWidthPx).coerceIn(0f, 1f)
                                            scrubFraction = frac
                                            val preview = computeCycleScrubPreview(cycleStatus, frac)
                                            lastHapticDay = preview.dayIndexInCycle
                                            lastInBreakZone = preview.isInBreakZone
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        },
                                        onHorizontalDrag = { change, _ ->
                                            change.consume()
                                            val frac = (change.position.x / scrubberWidthPx).coerceIn(0f, 1f)
                                            scrubFraction = frac
                                            val preview = computeCycleScrubPreview(cycleStatus, frac)
                                            val hitBoundary = preview.isInBreakZone != lastInBreakZone ||
                                                frac <= 0.005f || frac >= 0.995f
                                            if (hitBoundary && preview.dayIndexInCycle != lastHapticDay) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            } else if (preview.dayIndexInCycle != lastHapticDay) {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            }
                                            lastHapticDay = preview.dayIndexInCycle
                                            lastInBreakZone = preview.isInBreakZone
                                        },
                                        onDragEnd = {
                                            isScrubbing = false
                                            scrubFraction = null
                                            lastHapticDay = -1
                                        },
                                        onDragCancel = {
                                            isScrubbing = false
                                            scrubFraction = null
                                            lastHapticDay = -1
                                        }
                                    )
                                }
                                .pointerInput(cycleStatus.subscription.id) {
                                    detectTapGestures(
                                        onPress = { offset ->
                                            isScrubbing = true
                                            val frac = (offset.x / scrubberWidthPx).coerceIn(0f, 1f)
                                            scrubFraction = frac
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            tryAwaitRelease()
                                            isScrubbing = false
                                            scrubFraction = null
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            WavyProgressIndicator(
                                progress = cycleStatus.progressFraction,
                                activeColor = progressColor,
                                animateWave = isActive && !isScrubbing,
                                activeDays = if (isRotating) sub.activeDays else 0,
                                blackoutDays = if (isRotating) sub.blackoutDays else 0,
                                isActivePhase = isActive,
                                grabFraction = if (isScrubbing) 1f else 0f,
                                scrubTotalProgress = scrubFraction
                            )
                        }

                        if (isDue && scrubPreview == null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NotificationsActive,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = action.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        if (isRotating && sub.cost > 0.0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(
                                thickness = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                            )
                            Spacer(modifier = Modifier.height(11.dp))
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

            Spacer(modifier = Modifier.height(14.dp))

            // 3. Asymmetric 50/50 Bento Command Matrix (inspired by BunPod's PlayerControls!)
            val isUrgentCancel = !isIncomplete && isRotating && isActive && !isCancelledForCycle && cycleStatus.daysRemainingInPhase <= 7
            val isPausedForResub = !isIncomplete && isRotating && !isActive
            val hasUnbankedBreak = !isIncomplete && isRotating && cycleStatus.hasUnbankedCompletedBreak
            val canSkipBreak = !isIncomplete && isRotating && isActive && !isCancelledForCycle && cycleStatus.daysRemainingInPhase <= 10
            val canMarkActiveEarly = !isIncomplete && isRotating && !isActive && !hasUnbankedBreak

            val heroTitle = when {
                isIncomplete -> "Finish setup"
                hasUnbankedBreak -> {
                    val saved = MoneyFormatter.format(cycleStatus.perBreakSavings, sub.currency, maximumFractionDigits = 0)
                    "Bank ~$saved"
                }
                isUrgentCancel -> "Cancel renew"
                isPausedForResub -> "Re-subscribe"
                isCancelledForCycle && isActive -> "Coasting"
                isRotating -> "Manage / Cancel"
                else -> "Manage billing"
            }
            val heroSubtitle = when {
                isIncomplete -> "Missing details"
                hasUnbankedBreak -> "Restart active cycle"
                isUrgentCancel -> "Billing portal"
                isPausedForResub -> "Start fresh cycle"
                isCancelledForCycle && isActive -> "Reminders paused"
                else -> "Billing portal"
            }
            val heroIcon = when {
                isIncomplete -> Icons.Default.Tune
                hasUnbankedBreak -> Icons.Default.CheckCircle
                isCancelledForCycle && isActive -> Icons.Default.Check
                else -> Icons.AutoMirrored.Filled.OpenInNew
            }
            val isHeroBoldPrimary = isIncomplete || hasUnbankedBreak || isUrgentCancel || isPausedForResub

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(152.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // LEFT 50%: Primary Superellipse Hero Lifecycle Tile
                HeroCommandTile(
                    title = heroTitle,
                    subtitle = heroSubtitle,
                    icon = heroIcon,
                    boldPrimary = isHeroBoldPrimary,
                    isCoastingSeal = isCancelledForCycle && isActive && !hasUnbankedBreak && !isIncomplete,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        when {
                            isIncomplete -> {
                                onDismiss()
                                onEditStep(sub.firstMissingSetupStepIndex)
                            }
                            hasUnbankedBreak -> {
                                onResumeToday()
                                onDismiss()
                            }
                            isPausedForResub -> {
                                onStartSubscriptionAction(SubscriptionActionProgressType.SUBSCRIBE)
                            }
                            else -> {
                                onStartSubscriptionAction(SubscriptionActionProgressType.UNSUBSCRIBE)
                            }
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )

                // RIGHT 50%: Tight 2×2 Tactile Sub-Grid (6dp inner seams, >= 48dp targets)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Top-Start: Anticipatory Press-Preview Auto-Renew Seal Toggle (or Resume Active)
                        if (!isIncomplete && isActive && isRotating) {
                            AnticipatoryToggleTile(
                                checked = isCancelledForCycle,
                                label = if (isCancelledForCycle) "Cancelled" else "Mark off",
                                icon = if (isCancelledForCycle) Icons.Default.NotificationsOff else Icons.Default.Check,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onToggleAutoRenew()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        } else if (canMarkActiveEarly) {
                            val canBank = cycleStatus.phaseStartDate.isBefore(LocalDate.now())
                            SubCommandTile(
                                label = if (canBank) "Bank now" else "Resume",
                                icon = Icons.Default.PlayArrow,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onResumeToday()
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        } else {
                            SubCommandTile(
                                label = "Cycle",
                                icon = Icons.Default.Autorenew,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDismiss()
                                    onEditStep(1)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }

                        // Top-End: Skip Break (+30d) when within 10d, else jump directly to Cycle Studio
                        if (canSkipBreak) {
                            SubCommandTile(
                                label = "+${sub.activeDays}d",
                                icon = Icons.Default.FastForward,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onExtendCycle()
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        } else {
                            SubCommandTile(
                                label = "Schedule",
                                icon = Icons.Default.Tune,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onDismiss()
                                    onEditStep(1)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Bottom-Start: Edit
                        SubCommandTile(
                            label = "Edit",
                            icon = Icons.Default.Edit,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDismiss()
                                onEdit()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )

                        // Bottom-End: Delete (morphs toward Gem on press, expands inline confirm bar!)
                        SubCommandTile(
                            label = "Delete",
                            icon = Icons.Default.DeleteOutline,
                            containerColor = if (showInlineDeleteConfirm) {
                                MaterialTheme.colorScheme.errorContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            contentColor = if (showInlineDeleteConfirm) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showInlineDeleteConfirm = !showInlineDeleteConfirm
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }
            }

            // Inline Spring-Expanding Delete Confirmation (zero jarring AlertDialog jump!)
            AnimatedVisibility(
                visible = showInlineDeleteConfirm,
                enter = expandVertically(
                    animationSpec = ExpressiveMotion.expressiveSpatialDefaultIntSize()
                        .orSnapIfReduced(reducedMotion)
                ) + fadeIn(),
                exit = shrinkVertically(
                    animationSpec = ExpressiveMotion.standardSpatialFast()
                ) + fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = if (isDark) 0.55f else 0.72f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Delete ${sub.name}?",
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Stops tracking and cancels local reminders.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilledTonalButton(
                                onClick = { showInlineDeleteConfirm = false },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp),
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Text("Keep", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                            }
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showInlineDeleteConfirm = false
                                    onDelete()
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp),
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            ) {
                                Text("Confirm delete", fontWeight = FontWeight.ExtraBold, fontSize = 12.5.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCommandTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    boldPrimary: Boolean,
    isCoastingSeal: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = LocalSubCyclerDarkTheme.current
    val reducedMotion = rememberReducedMotionEnabled()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val corner by animateDpAsState(
        targetValue = if (isPressed) 14.dp else 28.dp,
        animationSpec = ExpressiveMotion.expressiveSpatialFastDp().orSnapIfReduced(reducedMotion),
        label = "heroTileCorner"
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = ExpressiveMotion.expressiveSpatialFastFloat().orSnapIfReduced(reducedMotion),
        label = "heroTileScale"
    )
    val bgColor = if (boldPrimary) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val fgColor = if (boldPrimary) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onPrimaryContainer
    }
    val iconShape = rememberMorphingPolygonShape(
        targetPolygon = when {
            isPressed -> ExpressiveMaterialPolygons.Sunny
            isCoastingSeal -> ExpressiveMaterialPolygons.Cookie12
            else -> ExpressiveMaterialPolygons.Circle
        }
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        shape = RoundedCornerShape(corner),
        color = bgColor,
        border = if (!boldPrimary && isDark) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.34f))
        } else {
            null
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .expressivePolygonSurface(
                        shape = iconShape,
                        color = fgColor.copy(alpha = if (boldPrimary) 0.16f else 0.14f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                ReactiveIcon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = fgColor,
                    size = 22.dp,
                    motion = ReactiveMotionType.NUDGE_RIGHT,
                    isPressed = isPressed
                )
            }
            Column {
                Text(
                    text = title,
                    fontFamily = DisplayFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    lineHeight = 20.sp,
                    letterSpacing = (-0.3).sp,
                    color = fgColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = fgColor.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * BunPod `FavButton`-inspired Anticipatory Press-Preview Tile:
 * - While pressed down, the tile's internal polygon seal IMMEDIATELY springs toward the
 *   toggled destination (`Circle ↔ Gem`), while the background color waits until release!
 */
@Composable
private fun AnticipatoryToggleTile(
    checked: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val previewChecked = if (isPressed && !reducedMotion) !checked else checked

    val corner by animateDpAsState(
        targetValue = if (isPressed) 12.dp else 20.dp,
        animationSpec = ExpressiveMotion.expressiveSpatialFastDp().orSnapIfReduced(reducedMotion),
        label = "toggleTileCorner"
    )
    // Color tracks the committed state (changes on release, never prematurely on press!)
    val bgColor by animateColorAsState(
        targetValue = if (checked) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        animationSpec = ExpressiveMotion.effectsFastColor().orSnapIfReduced(reducedMotion),
        label = "toggleTileBg"
    )
    val fgColor by animateColorAsState(
        targetValue = if (checked) {
            MaterialTheme.colorScheme.onPrimaryContainer
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = ExpressiveMotion.effectsFastColor().orSnapIfReduced(reducedMotion),
        label = "toggleTileFg"
    )

    // Shape previews the toggled destination while pressed!
    val sealShape = rememberMorphingPolygonShape(
        targetPolygon = if (previewChecked) ExpressiveMaterialPolygons.Gem else ExpressiveMaterialPolygons.Circle
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier,
        shape = RoundedCornerShape(corner),
        color = bgColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .expressivePolygonSurface(
                        shape = sealShape,
                        color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (checked) MaterialTheme.colorScheme.onPrimary else fgColor,
                    modifier = Modifier.size(15.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = fgColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SubCommandTile(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    contentColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val corner by animateDpAsState(
        targetValue = if (isPressed) 12.dp else 20.dp,
        animationSpec = ExpressiveMotion.expressiveSpatialFastDp().orSnapIfReduced(reducedMotion),
        label = "subTileCorner"
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = ExpressiveMotion.expressiveSpatialFastFloat().orSnapIfReduced(reducedMotion),
        label = "subTileScale"
    )

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        shape = RoundedCornerShape(corner),
        color = containerColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            ReactiveIcon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                size = 19.dp,
                motion = ReactiveMotionType.POP_BOUNCE,
                isPressed = isPressed
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
