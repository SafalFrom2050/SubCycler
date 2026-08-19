package com.subtrack.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subtrack.core.domain.model.*
import com.subtrack.core.ui.theme.*

@Composable
fun SubscriptionCard(
    cycleStatus: CycleStatus,
    onToggleAutoRenew: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val sub = cycleStatus.subscription
    val isDark = MaterialTheme.colorScheme.background == DarkBg
    val brandColor = try {
        Color(android.graphics.Color.parseColor(sub.colorHex))
    } catch (e: Exception) {
        Emerald500
    }

    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                expanded = !expanded
            },
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
            // Header Row: Brand Accent + Name + Category + Cost
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF141414))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = sub.name.take(2).uppercase(),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = sub.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = sub.category.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Cost display
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$${String.format("%.2f", sub.cost)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (sub.strategy == RotationStrategy.CONTINUOUS) "/month" else "/active mo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Strategy & Phase Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Phase Pill (Clean, without noisy emojis)
                val isPhaseActive = cycleStatus.phase == CyclePhase.ACTIVE
                val phaseBg = if (isPhaseActive) PrimarySky.copy(alpha = 0.10f) else Color(0xFF141414)
                val phaseBorder = if (isPhaseActive) PrimarySky.copy(alpha = 0.25f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                val phaseText = if (isPhaseActive) "ACTIVE · ${cycleStatus.daysRemainingInPhase}d left" else "PAUSED · ${cycleStatus.daysRemainingInPhase}d to re-sub"
                val phaseColor = if (isPhaseActive) PrimarySky else MaterialTheme.colorScheme.onSurfaceVariant

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = phaseBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, phaseBorder)
                ) {
                    Text(
                        text = phaseText,
                        color = phaseColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                // Strategy Pill
                val strategyText = when (sub.strategy) {
                    RotationStrategy.ROTATION_45_DAYS -> "45d (30a/15b)"
                    RotationStrategy.ROTATION_60_DAYS -> "60d (30a/30b)"
                    RotationStrategy.CUSTOM -> "${sub.activeDays}a/${sub.blackoutDays}b"
                    RotationStrategy.CONTINUOUS -> "Continuous"
                }

                Text(
                    text = strategyText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar (Sleek 2dp hairline)
            val progressColor = if (cycleStatus.phase == CyclePhase.ACTIVE) PrimarySky else Color(0xFF3F3F46)
            LinearProgressIndicator(
                progress = { cycleStatus.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(RoundedCornerShape(1.dp)),
                color = progressColor,
                trackColor = Color(0xFF161616)
            )

            // Urgent / Info Next Action Banner (Clean & Quiet)
            Spacer(modifier = Modifier.height(10.dp))
            val action = cycleStatus.nextAction
            val actionContainerColor = when (action.urgency) {
                ActionUrgency.ACTION_DUE -> AlertCrimson.copy(alpha = 0.08f)
                ActionUrgency.WARNING -> WarningAmber.copy(alpha = 0.08f)
                ActionUrgency.INFO -> Color(0xFF121212)
            }
            val actionBorderColor = when (action.urgency) {
                ActionUrgency.ACTION_DUE -> AlertCrimson.copy(alpha = 0.25f)
                ActionUrgency.WARNING -> WarningAmber.copy(alpha = 0.25f)
                ActionUrgency.INFO -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            }
            val actionContentColor = when (action.urgency) {
                ActionUrgency.ACTION_DUE -> AlertCrimson
                ActionUrgency.WARNING -> WarningAmber
                ActionUrgency.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = actionContainerColor,
                border = androidx.compose.foundation.BorderStroke(1.dp, actionBorderColor)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (action.actionType) {
                            ActionType.CANCEL_AUTO_RENEW -> Icons.Default.NotificationsNone
                            ActionType.RESUBSCRIBE -> Icons.Default.Autorenew
                            ActionType.NO_ACTION -> Icons.Default.Check
                        },
                        contentDescription = null,
                        tint = actionContentColor,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = action.description,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = actionContentColor,
                        fontWeight = if (action.urgency != ActionUrgency.INFO) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Expanded Details & Actions
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMediumLow,
                        dampingRatio = Spring.DampingRatioNoBouncy
                    )
                ) + fadeIn(animationSpec = tween(120)),
                exit = shrinkVertically(
                    animationSpec = spring(
                        stiffness = Spring.StiffnessMediumLow,
                        dampingRatio = Spring.DampingRatioNoBouncy
                    )
                ) + fadeOut(animationSpec = tween(90))
            ) {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Savings & Cycle details
                    if (sub.strategy != RotationStrategy.CONTINUOUS) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Annual Cost (Optimized):",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$${String.format("%.2f", cycleStatus.optimizedAnnualCost)} (Saves $${String.format("%.2f", cycleStatus.annualSavings)}/yr)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = ActiveGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    if (sub.notes.isNotEmpty()) {
                        Text(
                            text = "Note: ${sub.notes}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Checkbox to mark Auto-renew cancelled
                    if (cycleStatus.phase == CyclePhase.ACTIVE && sub.strategy != RotationStrategy.CONTINUOUS) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onToggleAutoRenew()
                                }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = sub.isAutoRenewCancelled,
                                onCheckedChange = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onToggleAutoRenew()
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (sub.isAutoRenewCancelled) "✓ Auto-renew cancelled for this cycle" else "Mark auto-renew as cancelled",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = if (sub.isAutoRenewCancelled) ActiveGreen else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Edit & Delete row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = onEdit,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit", fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(
                            onClick = onDelete,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRose),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete", fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
