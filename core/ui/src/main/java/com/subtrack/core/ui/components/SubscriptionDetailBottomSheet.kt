package com.subtrack.core.ui.components

import com.subtrack.core.common.currency.MoneyFormatter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subtrack.core.domain.model.ActionType
import com.subtrack.core.domain.model.ActionUrgency
import com.subtrack.core.domain.model.CyclePhase
import com.subtrack.core.domain.model.CycleStatus
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.ui.theme.AlertCrimson
import com.subtrack.core.ui.theme.ErrorRose
import com.subtrack.core.ui.theme.PrimarySky
import com.subtrack.core.ui.theme.WarningAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionDetailBottomSheet(
    cycleStatus: CycleStatus,
    onDismiss: () -> Unit,
    onToggleAutoRenew: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val sub = cycleStatus.subscription
    val isActive = cycleStatus.phase == CyclePhase.ACTIVE
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0F0F0F),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = Color(0xFF333333),
                modifier = Modifier.padding(vertical = 10.dp)
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header: Monogram + Name + Category + Price
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
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF181818))
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = sub.name.take(2).uppercase(),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = sub.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
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
                        text = MoneyFormatter.format(sub.cost, sub.currency),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (sub.strategy == RotationStrategy.CONTINUOUS) "/month" else "/active mo",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Phase Status Pill & Strategy Label
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val phaseBg = if (isActive) PrimarySky.copy(alpha = 0.10f) else Color(0xFF161616)
                val phaseBorder = if (isActive) PrimarySky.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                val phaseText = if (isActive) "ACTIVE · ${cycleStatus.daysRemainingInPhase} days left" else "PAUSED · ${cycleStatus.daysRemainingInPhase} days to re-sub"
                val phaseColor = if (isActive) PrimarySky else MaterialTheme.colorScheme.onSurfaceVariant

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
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }

                val strategyText = when (sub.strategy) {
                    RotationStrategy.ROTATION_45_DAYS -> "45d Cycle (30a/15b)"
                    RotationStrategy.ROTATION_60_DAYS -> "60d Cycle (30a/30b)"
                    RotationStrategy.CUSTOM -> "${sub.activeDays}a/${sub.blackoutDays}b"
                    RotationStrategy.CONTINUOUS -> "Continuous"
                }

                Text(
                    text = strategyText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Progress Bar
            val progressColor = if (isActive) PrimarySky else Color(0xFF3F3F46)
            LinearProgressIndicator(
                progress = { cycleStatus.progressFraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = progressColor,
                trackColor = Color(0xFF1E1E22)
            )

            // Urgent / Info Next Action Banner
            val action = cycleStatus.nextAction
            if (action.actionType != ActionType.NO_ACTION) {
                Spacer(modifier = Modifier.height(14.dp))
                val isDue = action.urgency == ActionUrgency.ACTION_DUE
                val containerColor = if (isDue) AlertCrimson.copy(alpha = 0.08f) else Color(0xFF141414)
                val borderColor = if (isDue) AlertCrimson.copy(alpha = 0.3f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                val contentColor = if (isDue) AlertCrimson else MaterialTheme.colorScheme.onSurfaceVariant

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = containerColor,
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (action.actionType) {
                                ActionType.CANCEL_AUTO_RENEW -> Icons.Default.NotificationsNone
                                ActionType.RESUBSCRIBE -> Icons.Default.Autorenew
                                ActionType.NO_ACTION -> Icons.Default.Check
                            },
                            contentDescription = null,
                            tint = contentColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = action.description,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                            color = contentColor,
                            fontWeight = if (isDue) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(12.dp))

            // Financial impact
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
                        text = "${MoneyFormatter.format(cycleStatus.optimizedAnnualCost, sub.currency)} (Saves ${MoneyFormatter.format(cycleStatus.annualSavings, sub.currency)}/yr)",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimarySky
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
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
            if (isActive && sub.strategy != RotationStrategy.CONTINUOUS) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
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
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = PrimarySky,
                            checkmarkColor = Color.Black
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (sub.isAutoRenewCancelled) "✓ Auto-renew cancelled for this cycle" else "Mark auto-renew as cancelled",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (sub.isAutoRenewCancelled) PrimarySky else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons (Edit & Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onEdit()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    )
                ) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit", fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = {
                        onDismiss()
                        onDelete()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRose),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        ErrorRose.copy(alpha = 0.35f)
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Delete", fontSize = 13.sp)
                }
            }
        }
    }
}
