package com.subtrack.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.EventRepeat
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subtrack.core.common.currency.MoneyFormatter
import com.subtrack.core.domain.engine.RotationEngine
import com.subtrack.core.domain.model.ActionUrgency
import com.subtrack.core.domain.model.BillingPeriod
import com.subtrack.core.domain.model.CyclePhase
import com.subtrack.core.domain.model.CycleStatus
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.ui.theme.BodyFontFamily
import com.subtrack.core.ui.theme.BrandColorHarmonizer
import com.subtrack.core.ui.theme.DisplayFontFamily
import com.subtrack.core.ui.theme.ExpressiveMotion
import com.subtrack.core.ui.theme.LocalSubCyclerAccentTokens
import com.subtrack.core.ui.theme.LocalSubCyclerDarkTheme
import com.subtrack.core.ui.theme.rememberReducedMotionEnabled
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Bold Color-Block Material 3 Expressive Cycle Timeline Card:
 * - Harmonized true-hue brand colors for Active windows (matching Home's 2-zone vessels & Savings)
 * - Crisp `45°` hatched Rest blocks with per-break savings reward pills
 * - Live animated progress bar inside the current `NOW` phase window
 * - Automatic horizontal scroll tracking whenever [inspectedDate] scrubs across future/past cycles
 */
@Composable
fun CycleTimelineView(
    cycleStatus: CycleStatus,
    inspectedDate: LocalDate? = null,
    onClick: (() -> Unit)? = null,
    isEntranceReady: Boolean = true,
    entranceIndex: Int = -1,
    modifier: Modifier = Modifier
) {
    val sub = cycleStatus.subscription
    val isDark = LocalSubCyclerDarkTheme.current
    val accentTokens = LocalSubCyclerAccentTokens.current
    val reducedMotion = rememberReducedMotionEnabled()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d") }
    val listState = rememberLazyListState()

    val isActiveNow = cycleStatus.phase == CyclePhase.ACTIVE
    val isYearly = sub.billingPeriod == BillingPeriod.YEARLY
    val cardBorder = expressiveCardBorder()
    val brandPalette = remember(sub.name, sub.colorHex, isDark) {
        BrandColorHarmonizer.harmonizedTonalPalette(
            serviceName = sub.name,
            storedColorHex = sub.colorHex,
            isDark = isDark
        )
    }

    val lifecycleState = remember(cycleStatus) {
        when {
            sub.isIncomplete -> ServiceBadgeLifecycleState.INCOMPLETE
            sub.isRotating && cycleStatus.hasUnbankedCompletedBreak -> ServiceBadgeLifecycleState.UNBANKED_READY
            cycleStatus.phase == CyclePhase.BLACKOUT -> ServiceBadgeLifecycleState.BLACKOUT_REST
            cycleStatus.nextAction.urgency == ActionUrgency.ACTION_DUE -> ServiceBadgeLifecycleState.URGENT_DUE
            cycleStatus.isAutoRenewCancelledForCurrentCycle -> ServiceBadgeLifecycleState.COASTING_CANCELLED
            else -> ServiceBadgeLifecycleState.ACTIVE
        }
    }

    // One-shot staggered entrance & live progress bar sweep on first visit to Planner
    val shouldAnimateEntrance = !reducedMotion && entranceIndex in 0..5
    val skipEntrance = isEntranceReady || !shouldAnimateEntrance
    val cardReveal = remember { Animatable(if (skipEntrance) 1f else 0f) }
    val progressSweep = remember { Animatable(if (skipEntrance) 1f else 0f) }

    LaunchedEffect(isEntranceReady, reducedMotion) {
        if (!isEntranceReady) return@LaunchedEffect
        if (reducedMotion || !shouldAnimateEntrance) {
            cardReveal.snapTo(1f)
            progressSweep.snapTo(1f)
            return@LaunchedEffect
        }
        if (cardReveal.value >= 0.99f && progressSweep.value >= 0.99f) return@LaunchedEffect
        val staggerMs = entranceIndex.coerceIn(0, 5) * 45L
        launch {
            delay(55L + staggerMs)
            cardReveal.animateTo(1f, ExpressiveMotion.expressiveSpatialDefaultFloat())
        }
        launch {
            delay(110L + staggerMs)
            progressSweep.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.80f,
                    stiffness = 300f,
                    visibilityThreshold = 0.001f
                )
            )
        }
    }

    // Automatically glide the timeline row to bring the inspected date's phase block into view
    LaunchedEffect(inspectedDate, cycleStatus.timelineBlocks) {
        val target = inspectedDate ?: return@LaunchedEffect
        val idx = cycleStatus.timelineBlocks.indexOfFirst { block ->
            !target.isBefore(block.startDate) && target.isBefore(block.endDate)
        }
        if (idx >= 0) {
            if (reducedMotion) {
                listState.scrollToItem(idx)
            } else {
                listState.animateScrollToItem(idx)
            }
        }
    }

    val statusPillBg = when {
        !isActiveNow -> accentTokens.heroPop
        else -> brandPalette.accent.copy(alpha = if (isDark) 0.22f else 0.14f)
    }
    val statusPillInk = when {
        !isActiveNow -> accentTokens.onHeroPop
        else -> if (isDark) brandPalette.accent else brandPalette.container
    }
    val statusPillDot = if (!isActiveNow) accentTokens.onHeroPop else brandPalette.accent
    val statusPillLabel = when {
        !isActiveNow -> "${cycleStatus.daysRemainingInPhase}d break"
        cycleStatus.daysRemainingInPhase <= 0 -> "Due today"
        else -> "${cycleStatus.daysRemainingInPhase}d left"
    }

    val formattedPerBreakSave = remember(cycleStatus.perBreakSavings, sub.currency) {
        if (sub.isRotating && cycleStatus.perBreakSavings > 0.5) {
            MoneyFormatter.format(cycleStatus.perBreakSavings, sub.currency, maximumFractionDigits = 0)
        } else {
            null
        }
    }

    val cardContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 14.dp)
        ) {
            // 1. Brand Header Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    ExpressiveServiceBadge(
                        serviceName = sub.name,
                        colorHex = sub.colorHex,
                        size = 42.dp,
                        fontSize = 13.sp,
                        lifecycleState = lifecycleState
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = sub.name,
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            letterSpacing = (-0.2).sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = when {
                                isYearly -> "Annual 365d · ${MoneyFormatter.format(sub.cost, sub.currency)}/yr"
                                sub.strategy == RotationStrategy.CONTINUOUS -> "Always on · ${MoneyFormatter.format(sub.cost, sub.currency)}/mo"
                                sub.strategy == RotationStrategy.ROTATION_45_DAYS -> "45d rhythm · 30d on / 15d off"
                                sub.strategy == RotationStrategy.ROTATION_60_DAYS -> "60d swap · 30d on / 30d off"
                                else -> "${sub.activeDays}d on · ${sub.blackoutDays}d off"
                            },
                            fontFamily = BodyFontFamily,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = statusPillBg
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(statusPillDot)
                        )
                        Text(
                            text = statusPillLabel,
                            color = statusPillInk,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 2. Proportional Brand-Tinted Horizontal Cycle Track
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                itemsIndexed(
                    items = cycleStatus.timelineBlocks,
                    key = { _, block -> "${block.startDate}_${block.phase}" }
                ) { _, block ->
                    val isCurrent = block.isCurrent
                    val isInspected = inspectedDate != null &&
                        !inspectedDate.isBefore(block.startDate) &&
                        inspectedDate.isBefore(block.endDate)
                    val isActiveBlock = block.phase == CyclePhase.ACTIVE
                    val isPausedBlock = !isActiveBlock

                    val blockDays = ChronoUnit.DAYS.between(block.startDate, block.endDate).toInt().coerceAtLeast(1)
                    val blockWidth = when {
                        isYearly -> 188.dp
                        else -> (blockDays * 4.4f + 70f).dp.coerceIn(124.dp, 204.dp)
                    }
                    val blockCorner = RoundedCornerShape(18.dp)

                    val blockBg = when {
                        isActiveBlock && (isCurrent || isInspected) ->
                            brandPalette.accent.copy(alpha = if (isDark) 0.24f else 0.16f)
                        isActiveBlock ->
                            brandPalette.accent.copy(alpha = if (isDark) 0.11f else 0.08f)
                        isPausedBlock && (isCurrent || isInspected) ->
                            MaterialTheme.colorScheme.surfaceContainerHighest
                        else ->
                            MaterialTheme.colorScheme.surfaceContainerHigh
                    }

                    val blockBorderColor = when {
                        isInspected && !isCurrent -> if (isDark) accentTokens.heroPop else MaterialTheme.colorScheme.primary
                        isActiveBlock && isCurrent -> brandPalette.accent.copy(alpha = if (isDark) 0.72f else 0.55f)
                        isActiveBlock -> brandPalette.accent.copy(alpha = if (isDark) 0.22f else 0.20f)
                        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.45f else 0.55f)
                    }
                    val blockBorderWidth = when {
                        isInspected && !isCurrent -> 2.dp
                        isCurrent -> 1.4.dp
                        else -> 1.dp
                    }

                    val phaseEyebrowColor = when {
                        isActiveBlock -> if (isDark) brandPalette.accent else brandPalette.container
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    val hatchColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isDark) 0.12f else 0.10f)
                    val progressTrackBg = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.09f)
                    val progressFillColor = if (isActiveBlock) brandPalette.accent else accentTokens.heroPop

                    Box(
                        modifier = Modifier
                            .width(blockWidth)
                            .clip(blockCorner)
                            .background(blockBg)
                            .then(
                                if (isPausedBlock) {
                                    Modifier.diagonalHatchPattern(
                                        color = hatchColor,
                                        spacing = 8.dp,
                                        strokeWidth = 1.dp
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .border(blockBorderWidth, blockBorderColor, blockCorner)
                            .padding(horizontal = 12.dp, vertical = 11.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isActiveBlock) brandPalette.accent
                                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                            )
                                    )
                                    Text(
                                        text = if (isActiveBlock) "ACTIVE" else "BREAK",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.8.sp,
                                        color = phaseEyebrowColor
                                    )
                                }

                                when {
                                    isInspected && !isCurrent -> Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = accentTokens.heroPop
                                    ) {
                                        Text(
                                            text = "DATE",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.5.sp,
                                            color = accentTokens.onHeroPop,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    isCurrent -> Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = if (isActiveBlock) brandPalette.container else accentTokens.heroPop
                                    ) {
                                        Text(
                                            text = "NOW",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            letterSpacing = 0.5.sp,
                                            color = if (isActiveBlock) brandPalette.content else accentTokens.onHeroPop,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    isPausedBlock && formattedPerBreakSave != null -> Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = accentTokens.heroPop.copy(alpha = if (isDark) 0.22f else 0.90f)
                                    ) {
                                        Text(
                                            text = "+$formattedPerBreakSave",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isDark) accentTokens.heroPop else accentTokens.onHeroPop,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(7.dp))

                            Text(
                                text = "${block.startDate.format(dateFormatter)} – ${block.endDate.format(dateFormatter)}",
                                fontFamily = DisplayFontFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.2).sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = when {
                                    isYearly -> "365d annual window"
                                    isActiveBlock -> "${blockDays}d active"
                                    else -> "${blockDays}d rest"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            if (isCurrent) {
                                Spacer(modifier = Modifier.height(8.dp))
                                val targetProgress = cycleStatus.progressFraction.coerceIn(0.06f, 1f)
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.5.dp)
                                ) {
                                    val r = CornerRadius(size.height / 2f, size.height / 2f)
                                    drawRoundRect(
                                        color = progressTrackBg,
                                        topLeft = Offset.Zero,
                                        size = size,
                                        cornerRadius = r
                                    )
                                    val sweep = ExpressiveMotion.clampEffectProgress(progressSweep.value)
                                    val fillW = (size.width * targetProgress * sweep).coerceAtLeast(size.height)
                                    drawRoundRect(
                                        color = progressFillColor,
                                        topLeft = Offset.Zero,
                                        size = Size(fillW.coerceAtMost(size.width), size.height),
                                        cornerRadius = r
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.75.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Action Cue Footer
            val formattedEnd = RotationEngine.formatReadableDate(cycleStatus.phaseEndDate)
            val footerIcon = when {
                !sub.isRotating || isYearly -> Icons.Default.EventRepeat
                cycleStatus.phase == CyclePhase.ACTIVE -> Icons.Default.NotificationsNone
                else -> Icons.Default.Autorenew
            }
            val footerText = when {
                isYearly -> "Annual renewal on $formattedEnd"
                !sub.isRotating -> "Continuous monthly renewal on $formattedEnd"
                cycleStatus.phase == CyclePhase.ACTIVE && cycleStatus.isAutoRenewCancelledForCurrentCycle ->
                    "Auto-renew off · ${sub.blackoutDays}d break starts $formattedEnd"
                cycleStatus.phase == CyclePhase.ACTIVE ->
                    "Turn off auto-renew before $formattedEnd"
                else ->
                    "Saves ~${MoneyFormatter.format(cycleStatus.perBreakSavings, sub.currency)} · Resume on $formattedEnd"
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    Icon(
                        imageVector = footerIcon,
                        contentDescription = null,
                        tint = if (isActiveNow) brandPalette.accent else accentTokens.heroPop,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = footerText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (onClick != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }

    val entranceModifier = modifier
        .fillMaxWidth()
        .graphicsLayer {
            val r = cardReveal.value
            val s = 0.96f + 0.04f * r
            alpha = ExpressiveMotion.clampEffectProgress(r * 1.45f)
            scaleX = s
            scaleY = s
            translationY = (1f - r) * 14.dp.toPx()
        }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = entranceModifier,
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = cardBorder,
            tonalElevation = 0.dp,
            content = cardContent
        )
    } else {
        Surface(
            modifier = entranceModifier,
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = cardBorder,
            tonalElevation = 0.dp,
            content = cardContent
        )
    }
}
