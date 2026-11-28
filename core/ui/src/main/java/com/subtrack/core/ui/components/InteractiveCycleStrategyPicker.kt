package com.subtrack.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subtrack.core.common.currency.MoneyFormatter
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.ui.theme.DisplayFontFamily
import com.subtrack.core.ui.theme.LocalSubCyclerDarkTheme
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

private data class RhythmSegment(
    val isActive: Boolean,
    val days: Int
)

/**
 * Minimal, neutral-first Cycle Strategy picker:
 * - Calm monochrome segmented controls (`Always`, `Smart`, `Swap`, `Custom`)
 * - Clean proportional rhythm bar without media play/pause icons or legend clutter
 * - Break slider & active-window stepper stay tucked away until `Custom` is selected
 */
@Composable
fun InteractiveCycleStrategyPicker(
    strategy: RotationStrategy,
    activeDays: Int,
    blackoutDays: Int,
    monthlyCost: Double?,
    currencyCode: String,
    onStrategyChanged: (strategy: RotationStrategy, activeDays: Int, blackoutDays: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isDark = LocalSubCyclerDarkTheme.current
    val cardBorder = expressiveCardBorder()

    val neutralSelectedBg = if (isDark) Color(0xFF34393E) else Color(0xFFFFFFFF)
    val neutralSelectedFg = MaterialTheme.colorScheme.onSurface
    val neutralSelectedBorder = BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.24f else 0.16f)
    )
    val activeBarBg = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDark) 0.88f else 0.84f)
    val activeBarFg = MaterialTheme.colorScheme.surface

    val effectiveActive = when (strategy) {
        RotationStrategy.CUSTOM -> activeDays.coerceIn(7, 90)
        else -> strategy.activeDays
    }
    val effectiveBreak = when (strategy) {
        RotationStrategy.CUSTOM -> blackoutDays.coerceAtLeast(0)
        else -> strategy.blackoutDays
    }
    val totalCycleDays = (effectiveActive + effectiveBreak).coerceAtLeast(1)
    val isRotating = effectiveBreak > 0

    // Exact RotationEngine formula so preview always matches Home & Savings
    val billsPerYear = if (isRotating) 365.0 / totalCycleDays else 12.0
    val savingsRatio = if (isRotating) max(0.0, 1.0 - (billsPerYear / 12.0)) else 0.0
    val savingsPct = (savingsRatio * 100.0).roundToInt()
    val estimatedAnnualSavings = monthlyCost?.takeIf { it > 0.0 }?.let { (it * 12.0) * savingsRatio }

    val sliderMax = max(45, effectiveBreak).toFloat()
    var lastHapticDay by remember(strategy) { mutableIntStateOf(effectiveBreak) }
    var ribbonBounceTick by remember { mutableIntStateOf(0) }

    val ribbonScale by animateFloatAsState(
        targetValue = if (ribbonBounceTick % 2 == 0) 1f else 0.992f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "ribbonBounce"
    )

    // Build proportional 90-day visual preview segments
    val segments = remember(effectiveActive, effectiveBreak) {
        if (effectiveBreak <= 0) {
            listOf(RhythmSegment(isActive = true, days = 90))
        } else {
            val result = mutableListOf<RhythmSegment>()
            var remaining = 90
            var nextActive = true
            while (remaining > 0 && result.size < 8) {
                val chunk = minOf(remaining, if (nextActive) effectiveActive else effectiveBreak)
                if (chunk > 0) {
                    result.add(RhythmSegment(isActive = nextActive, days = chunk))
                    remaining -= chunk
                }
                nextActive = !nextActive
            }
            result
        }
    }

    Column(modifier = modifier) {
        ExpressiveEyebrow(text = "CYCLE")
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = cardBorder
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Calm Neutral Segmented Mode Group (Always · Smart · Swap · Custom)
                val presetOptions = remember {
                    listOf(
                        ExpressiveSegmentOption(RotationStrategy.CONTINUOUS, "Always"),
                        ExpressiveSegmentOption(RotationStrategy.ROTATION_45_DAYS, "Smart"),
                        ExpressiveSegmentOption(RotationStrategy.ROTATION_60_DAYS, "Swap"),
                        ExpressiveSegmentOption(RotationStrategy.CUSTOM, "Custom")
                    )
                }
                ExpressiveSegmentedGroup(
                    options = presetOptions,
                    selectedValue = strategy,
                    onSelect = { itemStrategy ->
                        ribbonBounceTick++
                        when (itemStrategy) {
                            RotationStrategy.CONTINUOUS ->
                                onStrategyChanged(RotationStrategy.CONTINUOUS, 30, 0)
                            RotationStrategy.ROTATION_45_DAYS ->
                                onStrategyChanged(RotationStrategy.ROTATION_45_DAYS, 30, 15)
                            RotationStrategy.ROTATION_60_DAYS ->
                                onStrategyChanged(RotationStrategy.ROTATION_60_DAYS, 30, 30)
                            RotationStrategy.CUSTOM -> {
                                val seedBreak = if (effectiveBreak == 0) 10 else effectiveBreak
                                onStrategyChanged(RotationStrategy.CUSTOM, effectiveActive, seedBreak)
                            }
                        }
                    },
                    height = 40.dp,
                    fontSize = 12.sp,
                    selectedContainerColor = neutralSelectedBg,
                    selectedContentColor = neutralSelectedFg,
                    selectedBorder = neutralSelectedBorder
                )

                // 2. Concise Rhythm Summary + Savings Highlight (only when rotating)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isRotating) {
                                "${effectiveActive}d on · ${effectiveBreak}d off"
                            } else {
                                "Always on"
                            },
                            fontFamily = DisplayFontFamily,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.5.sp,
                            letterSpacing = (-0.3).sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val billsLabel = if (isRotating) {
                            val formattedBills = String.format(Locale.US, "%.1f", billsPerYear)
                                .removeSuffix(".0")
                            "~$formattedBills bills/yr"
                        } else {
                            "12 bills/yr"
                        }
                        Text(
                            text = billsLabel,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (isRotating) {
                        Spacer(modifier = Modifier.width(8.dp))
                        val badgeText = if (estimatedAnnualSavings != null && estimatedAnnualSavings >= 1.0) {
                            "Save ${MoneyFormatter.format(estimatedAnnualSavings, currencyCode, maximumFractionDigits = 0)}/yr"
                        } else {
                            "-$savingsPct%/yr"
                        }
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f)
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                // 3. Minimal Proportional 90-Day Ribbon (shown only when rotating, zero play/pause icons or legend!)
                AnimatedVisibility(
                    visible = isRotating,
                    enter = expandVertically(tween(180)) + fadeIn(tween(150)),
                    exit = shrinkVertically(tween(140)) + fadeOut(tween(110))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .graphicsLayer {
                                scaleX = ribbonScale
                                scaleY = ribbonScale
                            }
                            .clip(RoundedCornerShape(12.dp)),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        segments.forEach { segment ->
                            val animatedWeight by animateFloatAsState(
                                targetValue = segment.days.toFloat().coerceAtLeast(1f),
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                label = "segmentWeight"
                            )
                            val showFullLabel = segment.days >= 24
                            val showCompactLabel = segment.days in 12..23
                            val hatchColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.14f)

                            Box(
                                modifier = Modifier
                                    .weight(animatedWeight)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (segment.isActive) {
                                            activeBarBg
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                        }
                                    )
                                    .then(
                                        if (!segment.isActive) {
                                            Modifier
                                                .diagonalHatchPattern(color = hatchColor, spacing = 7.dp, strokeWidth = 1.dp)
                                                .border(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
                                                    RoundedCornerShape(10.dp)
                                                )
                                        } else {
                                            Modifier
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                val labelText = when {
                                    showFullLabel && segment.isActive -> "${segment.days}d on"
                                    showFullLabel && !segment.isActive -> "${segment.days}d off"
                                    showCompactLabel -> "${segment.days}d"
                                    else -> null
                                }
                                if (labelText != null) {
                                    Text(
                                        text = labelText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (segment.isActive) {
                                            activeBarFg
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // 4. Custom Controls (Break Slider + Active Window Stepper — revealed ONLY when Custom is selected!)
                AnimatedVisibility(
                    visible = strategy == RotationStrategy.CUSTOM,
                    enter = expandVertically(tween(180)) + fadeIn(tween(160)),
                    exit = shrinkVertically(tween(140)) + fadeOut(tween(120))
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Break length",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (effectiveBreak == 0) "None" else "${effectiveBreak}d",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Slider(
                                value = effectiveBreak.toFloat(),
                                onValueChange = { raw ->
                                    val snapped = raw.roundToInt().coerceAtLeast(0)
                                    if (snapped != lastHapticDay) {
                                        lastHapticDay = snapped
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onStrategyChanged(RotationStrategy.CUSTOM, effectiveActive, snapped)
                                    }
                                },
                                valueRange = 0f..sliderMax,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.onSurface,
                                    activeTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                )
                            )

                            val marks = listOf(
                                0 to "0d",
                                15 to "15d",
                                30 to "30d",
                                sliderMax.toInt() to "${sliderMax.toInt()}d"
                            )
                            Layout(
                                modifier = Modifier.fillMaxWidth(),
                                content = {
                                    marks.forEach { (days, text) ->
                                        val isHit = effectiveBreak == days
                                        Text(
                                            text = text,
                                            fontSize = 11.sp,
                                            fontWeight = if (isHit) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isHit) {
                                                MaterialTheme.colorScheme.onSurface
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    lastHapticDay = days
                                                    onStrategyChanged(RotationStrategy.CUSTOM, effectiveActive, days)
                                                }
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            ) { measurables, constraints ->
                                val loose = constraints.copy(minWidth = 0)
                                val placeables = measurables.map { it.measure(loose) }
                                val height = placeables.maxOfOrNull { it.height } ?: 0
                                val totalW = constraints.maxWidth
                                val thumbInset = 10.dp.roundToPx()
                                val trackW = (totalW - 2 * thumbInset).coerceAtLeast(1)

                                layout(totalW, height) {
                                    placeables.forEachIndexed { idx, placeable ->
                                        val days = marks[idx].first
                                        val frac = (days / sliderMax).coerceIn(0f, 1f)
                                        val idealX = when (idx) {
                                            0 -> 0
                                            marks.lastIndex -> totalW - placeable.width
                                            else -> (thumbInset + (trackW * frac) - placeable.width / 2f).roundToInt()
                                        }
                                        val clampedX = idealX.coerceIn(0, (totalW - placeable.width).coerceAtLeast(0))
                                        placeable.placeRelative(clampedX, (height - placeable.height) / 2)
                                    }
                                }
                            }
                        }

                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Active window",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    onClick = {
                                        val next = (effectiveActive - 5).coerceAtLeast(7)
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onStrategyChanged(RotationStrategy.CUSTOM, next, effectiveBreak)
                                    },
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Remove,
                                            contentDescription = "Decrease active days",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }

                                Text(
                                    text = "${effectiveActive}d",
                                    fontFamily = DisplayFontFamily,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Surface(
                                    onClick = {
                                        val next = (effectiveActive + 5).coerceAtMost(90)
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onStrategyChanged(RotationStrategy.CUSTOM, next, effectiveBreak)
                                    },
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Increase active days",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
