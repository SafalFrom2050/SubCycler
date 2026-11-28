package com.subtrack.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Autorenew
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
 * Minimal, Uniform-Proportion Bold Color-Block Material 3 Expressive Cycle Timeline Card:
 * - Uniform `156dp × 90dp` snapping horizontal cycle blocks (zero jagged height/width steps)
 * - Harmonized true-hue brand colors for Active windows + `45°` hatched Break windows with `+¥X` rewards
 * - Live animated progress bar tucked cleanly inside the `NOW` block's fixed height
 * - Minimal text: zero redundant renewal footers on continuous/yearly plans
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
    val snapFling = rememberSnapFlingBehavior(
        lazyListState = listState,
        snapPosition = SnapPosition.Start
    )

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

    // Only rotating plans surface a terse actionable cue; Always-on/Yearly cards stay minimal.
    val showRotatingFooter = sub.isRotating && !isYearly

    val cardContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 15.dp, bottom = if (showRotatingFooter) 13.dp else 15.dp)
        ) {
            // 1. Minimal Brand Header Row
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
                        size = 40.dp,
                        fontSize = 12.5.sp,
                        lifecycleState = lifecycleState
                    )
                    Spacer(modifier = Modifier.width(11.dp))
                    Column {
                        Text(
                            text = sub.name,
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.5.sp,
                            letterSpacing = (-0.2).sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = when {
                                isYearly -> "${MoneyFormatter.format(sub.cost, sub.currency)}/yr"
                                sub.strategy == RotationStrategy.CONTINUOUS ->
                                    "${MoneyFormatter.format(sub.cost, sub.currency)}/mo · Always"
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

            Spacer(modifier = Modifier.height(13.dp))

            // 2. Uniform-Proportion Snapping Horizontal Cycle Track (156dp × 90dp every tile)
            LazyRow(
                state = listState,
                flingBehavior = snapFling,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
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
                            .width(156.dp)
                            .height(90.dp)
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
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
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
                                        fontSize = 9.5.sp,
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
                                            fontSize = 8.5.sp,
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
                                            fontSize = 8.5.sp,
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
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (isDark) accentTokens.heroPop else accentTokens.onHeroPop,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Column {
                                Text(
                                    text = "${block.startDate.format(dateFormatter)} – ${block.endDate.format(dateFormatter)}",
                                    fontFamily = DisplayFontFamily,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.2).sp,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isYearly) "365d" else "${blockDays}d",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }

                            if (isCurrent) {
                                val targetProgress = cycleStatus.progressFraction.coerceIn(0.06f, 1f)
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
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
                            } else {
                                Spacer(modifier = Modifier.height(4.dp))
                            }
                        }
                    }
                }
            }

            // 3. Terse Action Footer — Rotating Services Only (zero noise on Always-on/Yearly plans)
            if (showRotatingFooter) {
                Spacer(modifier = Modifier.height(11.dp))
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.75.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.24f)
                )
                Spacer(modifier = Modifier.height(9.dp))

                val shortEnd = cycleStatus.phaseEndDate.format(dateFormatter)
                val footerIcon = if (cycleStatus.phase == CyclePhase.ACTIVE) {
                    Icons.Default.NotificationsNone
                } else {
                    Icons.Default.Autorenew
                }
                val footerText = when {
                    cycleStatus.phase == CyclePhase.ACTIVE && cycleStatus.isAutoRenewCancelledForCurrentCycle ->
                        "Break starts $shortEnd"
                    cycleStatus.phase == CyclePhase.ACTIVE ->
                        "Cancel by $shortEnd"
                    formattedPerBreakSave != null ->
                        "Resume $shortEnd · +$formattedPerBreakSave"
                    else ->
                        "Resume $shortEnd"
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = footerIcon,
                            contentDescription = null,
                            tint = if (isActiveNow) brandPalette.accent else accentTokens.heroPop,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = footerText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (onClick != null) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.50f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
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
