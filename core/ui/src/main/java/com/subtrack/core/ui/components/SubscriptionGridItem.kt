package com.subtrack.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subtrack.core.domain.model.ActionUrgency
import com.subtrack.core.domain.model.CyclePhase
import com.subtrack.core.domain.model.CycleStatus
import com.subtrack.core.ui.theme.AlertCrimson
import com.subtrack.core.ui.theme.PrimarySky

@Composable
fun SubscriptionGridItem(
    cycleStatus: CycleStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val sub = cycleStatus.subscription
    val isActive = cycleStatus.phase == CyclePhase.ACTIVE
    val isActionDue = cycleStatus.nextAction.urgency == ActionUrgency.ACTION_DUE

    val cardBorderColor = if (isActionDue) {
        AlertCrimson.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF0D0D0D)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            // Top-right Alert Badge if action due
            if (isActionDue) {
                Surface(
                    modifier = Modifier
                        .size(16.dp)
                        .align(Alignment.TopEnd),
                    shape = CircleShape,
                    color = AlertCrimson
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PriorityHigh,
                            contentDescription = "Action Required",
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(2.dp))

                // Centered App Monogram Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF181818))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = sub.name.take(2).uppercase(),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Service Name (Concise single-line)
                Text(
                    text = sub.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )

                // Cost
                Text(
                    text = "$${String.format("%.2f", sub.cost)}/mo",
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Status Pill
                val pillBg = if (isActive) PrimarySky.copy(alpha = 0.10f) else Color(0xFF161616)
                val pillBorder = if (isActive) PrimarySky.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                val pillText = if (isActive) "${cycleStatus.daysRemainingInPhase}d left" else "Paused · ${cycleStatus.daysRemainingInPhase}d"
                val pillColor = if (isActive) PrimarySky else MaterialTheme.colorScheme.onSurfaceVariant

                Surface(
                    shape = RoundedCornerShape(5.dp),
                    color = pillBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, pillBorder)
                ) {
                    Text(
                        text = pillText,
                        color = pillColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Hairline Progress (2dp)
                val progressColor = if (isActive) PrimarySky else Color(0xFF3F3F46)
                LinearProgressIndicator(
                    progress = { cycleStatus.progressFraction },
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(2.dp)
                        .clip(RoundedCornerShape(1.dp)),
                    color = progressColor,
                    trackColor = Color(0xFF1C1C1E)
                )
            }
        }
    }
}
