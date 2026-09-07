package com.subtrack.core.ui.components

import com.subtrack.core.common.currency.MoneyFormatter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subtrack.core.domain.model.CyclePhase
import com.subtrack.core.domain.model.CycleStatus
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.ui.theme.*
import java.time.format.DateTimeFormatter

@Composable
fun CycleTimelineView(
    cycleStatus: CycleStatus,
    modifier: Modifier = Modifier
) {
    val sub = cycleStatus.subscription
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d")

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Subscription Header & Current Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = sub.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (sub.strategy == RotationStrategy.CONTINUOUS) "Continuous 30d Cycle" else "45-Day Cycle (30d Active + 15d Blackout)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Current Phase Badge
                val currentBadgeColor = if (cycleStatus.phase == CyclePhase.ACTIVE) PrimarySky else CyberIndigo
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = currentBadgeColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = if (cycleStatus.phase == CyclePhase.ACTIVE) "NOW: Active" else "NOW: Blackout",
                        color = currentBadgeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Horizontal Scrollable Timeline Blocks
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                cycleStatus.timelineBlocks.forEachIndexed { index, block ->
                    val isCurrent = block.isCurrent
                    val blockColor = if (block.phase == CyclePhase.ACTIVE) PrimarySky else CyberIndigoLight
                    val blockBg = if (block.phase == CyclePhase.ACTIVE) {
                        PrimarySky.copy(alpha = 0.12f)
                    } else {
                        CyberIndigo.copy(alpha = 0.12f)
                    }

                    Box(
                        modifier = Modifier
                            .width(130.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(blockBg)
                            .then(
                                if (isCurrent) Modifier.border(1.dp, blockColor, RoundedCornerShape(10.dp))
                                else Modifier.border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                            )
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (block.phase == CyclePhase.ACTIVE) "ACTIVE" else "BLACKOUT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = blockColor
                                )
                                if (isCurrent) {
                                    Text(
                                        text = "CURRENT",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = blockColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "${block.startDate.format(dateFormatter)} - ${block.endDate.format(dateFormatter)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = if (block.phase == CyclePhase.ACTIVE) "30 Days Active" else "15 Days Blackout",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action guidance footer
            val footerText = when (cycleStatus.phase) {
                CyclePhase.ACTIVE -> "💡 Tip: Auto-renew should be turned off before ${cycleStatus.phaseEndDate.format(dateFormatter)}."
                CyclePhase.BLACKOUT -> "💡 Tip: Blackout saves you ~${MoneyFormatter.format(sub.cost * 0.50, sub.currency)} per rotation. Resubscribe on ${cycleStatus.phaseEndDate.format(dateFormatter)}."
            }
            Text(
                text = footerText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}
