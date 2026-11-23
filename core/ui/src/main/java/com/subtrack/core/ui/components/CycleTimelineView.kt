package com.subtrack.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subtrack.core.common.currency.MoneyFormatter
import com.subtrack.core.domain.engine.RotationEngine
import com.subtrack.core.domain.model.BillingPeriod
import com.subtrack.core.domain.model.CyclePhase
import com.subtrack.core.domain.model.CycleStatus
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.ui.theme.LocalSubCyclerDarkTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun CycleTimelineView(
    cycleStatus: CycleStatus,
    inspectedDate: LocalDate? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val sub = cycleStatus.subscription
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d")
    val isActiveNow = cycleStatus.phase == CyclePhase.ACTIVE
    val isYearly = sub.billingPeriod == BillingPeriod.YEARLY
    val phasePalette = if (isActiveNow) ExpressivePalettes.softSky() else ExpressivePalettes.softLavender()
    val cardBorder = expressiveCardBorder()

    val cardContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
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
                        size = 42.dp,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = sub.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = when {
                                isYearly -> "Annual 365d cycle"
                                sub.strategy == RotationStrategy.CONTINUOUS -> "Always on · 30d"
                                sub.strategy == RotationStrategy.ROTATION_45_DAYS -> "45d cycle · 30d on / 15d off"
                                sub.strategy == RotationStrategy.ROTATION_60_DAYS -> "60d swap · 30d on / 30d off"
                                else -> "${sub.activeDays}d on · ${sub.blackoutDays}d off"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = phasePalette.container
                ) {
                    Text(
                        text = if (isActiveNow) "Active" else "Paused",
                        color = phasePalette.content,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Full-bleed LazyRow prevents hard mid-character edge clipping
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = cycleStatus.timelineBlocks,
                    key = { "${it.startDate}_${it.phase}" }
                ) { block ->
                    val isCurrent = block.isCurrent
                    val isInspected = inspectedDate != null &&
                        !inspectedDate.isBefore(block.startDate) &&
                        inspectedDate.isBefore(block.endDate)
                    val blockPalette = if (block.phase == CyclePhase.ACTIVE) {
                        ExpressivePalettes.softSky()
                    } else {
                        ExpressivePalettes.softLavender()
                    }

                    val blockDays = ChronoUnit.DAYS.between(block.startDate, block.endDate).toInt().coerceAtLeast(1)
                    // Proportional duration widths: 30d active looks visibly wider than a 7d or 15d break!
                    val blockWidth = when {
                        isYearly -> 178.dp
                        else -> (blockDays * 4.2f + 64f).dp.coerceIn(112.dp, 190.dp)
                    }
                    val isPausedBlock = block.phase == CyclePhase.BLACKOUT
                    val blockCorner = RoundedCornerShape(16.dp)

                    Box(
                        modifier = Modifier
                            .width(blockWidth)
                            .clip(blockCorner)
                            .background(
                                if (isCurrent || isInspected) blockPalette.selectedContainer else blockPalette.container
                            )
                            .then(
                                if (isPausedBlock) {
                                    Modifier.diagonalHatchPattern(
                                        color = blockPalette.content.copy(alpha = 0.08f),
                                        spacing = 8.dp,
                                        strokeWidth = 1.dp
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .then(
                                if (isInspected && !isCurrent) {
                                    Modifier.border(
                                        1.8.dp,
                                        MaterialTheme.colorScheme.primary,
                                        blockCorner
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (block.phase == CyclePhase.ACTIVE) "ACTIVE" else "PAUSED",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.6.sp,
                                    color = blockPalette.content
                                )
                                when {
                                    isInspected && !isCurrent -> Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                    ) {
                                        Text(
                                            text = "DATE",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                    isCurrent -> Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = blockPalette.accent.copy(alpha = 0.18f)
                                    ) {
                                        Text(
                                            text = "NOW",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Black,
                                            color = blockPalette.accent,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "${block.startDate.format(dateFormatter)} – ${block.endDate.format(dateFormatter)}",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = blockPalette.content,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = when {
                                    isYearly -> "365d annual"
                                    block.phase == CyclePhase.ACTIVE -> "${blockDays}d active"
                                    else -> "${blockDays}d break"
                                },
                                fontSize = 11.sp,
                                color = blockPalette.content.copy(alpha = 0.78f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            val formattedEnd = RotationEngine.formatReadableDate(cycleStatus.phaseEndDate)
            val footerText = when {
                isYearly -> "Annual renewal on $formattedEnd."
                !sub.isRotating -> "Continuous monthly renewal on $formattedEnd."
                cycleStatus.phase == CyclePhase.ACTIVE && cycleStatus.isAutoRenewCancelledForCurrentCycle ->
                    "Auto-renew cancelled · ${sub.blackoutDays}-day pause begins $formattedEnd."
                cycleStatus.phase == CyclePhase.ACTIVE ->
                    "Turn off auto-renew before $formattedEnd."
                else ->
                    "This break saves ~${MoneyFormatter.format(cycleStatus.perBreakSavings, sub.currency)}. Re-subscribe on $formattedEnd."
            }
            Text(
                text = footerText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = cardBorder,
            tonalElevation = 0.dp,
            content = cardContent
        )
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = cardBorder,
            tonalElevation = 0.dp,
            content = cardContent
        )
    }
}
