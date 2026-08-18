package com.subtrack.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subtrack.app.data.model.*
import com.subtrack.app.ui.theme.*

@Composable
fun SubscriptionCard(
    cycleStatus: CycleStatus,
    onToggleAutoRenew: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) DarkSurface else LightSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                            .size(42.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(brandColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = sub.name.take(2).uppercase(),
                            color = brandColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
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

            Spacer(modifier = Modifier.height(14.dp))

            // Strategy & Phase Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Phase Pill
                val (phaseBg, phaseText, phaseColor) = when (cycleStatus.phase) {
                    CyclePhase.ACTIVE -> Triple(
                        ActiveGreen.copy(alpha = 0.15f),
                        "🟢 ACTIVE (${cycleStatus.daysRemainingInPhase}d left)",
                        ActiveGreen
                    )
                    CyclePhase.BLACKOUT -> Triple(
                        BlackoutIndigo.copy(alpha = 0.15f),
                        "⏸️ BLACKOUT (${cycleStatus.daysRemainingInPhase}d to re-sub)",
                        BlackoutIndigo
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = phaseBg
                ) {
                    Text(
                        text = phaseText,
                        color = phaseColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
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
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar
            val progressColor = if (cycleStatus.phase == CyclePhase.ACTIVE) ActiveGreen else BlackoutIndigo
            LinearProgressIndicator(
                progress = { cycleStatus.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Urgent / Info Next Action Banner
            Spacer(modifier = Modifier.height(12.dp))
            val action = cycleStatus.nextAction
            val actionContainerColor = when (action.urgency) {
                ActionUrgency.ACTION_DUE -> ErrorRose.copy(alpha = 0.12f)
                ActionUrgency.WARNING -> WarningAmber.copy(alpha = 0.12f)
                ActionUrgency.INFO -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            }
            val actionContentColor = when (action.urgency) {
                ActionUrgency.ACTION_DUE -> ErrorRose
                ActionUrgency.WARNING -> WarningAmber
                ActionUrgency.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = actionContainerColor
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = when (action.actionType) {
                            ActionType.CANCEL_AUTO_RENEW -> Icons.Default.NotificationsActive
                            ActionType.RESUBSCRIBE -> Icons.Default.Autorenew
                            ActionType.NO_ACTION -> Icons.Default.CheckCircle
                        },
                        contentDescription = null,
                        tint = actionContentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = action.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = actionContentColor,
                        fontWeight = if (action.urgency != ActionUrgency.INFO) FontWeight.SemiBold else FontWeight.Normal,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Expanded Details & Actions
            AnimatedVisibility(visible = expanded) {
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
                                .clickable { onToggleAutoRenew() }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = sub.isAutoRenewCancelled,
                                onCheckedChange = { onToggleAutoRenew() }
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
