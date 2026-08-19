package com.subtrack.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.subtrack.core.domain.model.*
import com.subtrack.core.ui.theme.*
import java.time.LocalDate

@Composable
fun AddEditSubscriptionDialog(
    initialSubscription: Subscription? = null,
    onDismiss: () -> Unit,
    onSave: (Subscription) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val focusManager = LocalFocusManager.current

    var name by remember { mutableStateOf(initialSubscription?.name ?: "") }
    var costStr by remember { mutableStateOf(initialSubscription?.cost?.toString() ?: "") }
    var category by remember { mutableStateOf(initialSubscription?.category ?: SubscriptionCategory.STREAMING) }
    var strategy by remember { mutableStateOf(initialSubscription?.strategy ?: RotationStrategy.ROTATION_45_DAYS) }
    var customActiveDaysStr by remember { mutableStateOf(initialSubscription?.customActiveDays?.toString() ?: "30") }
    var customBlackoutDaysStr by remember { mutableStateOf(initialSubscription?.customBlackoutDays?.toString() ?: "15") }
    var colorHex by remember { mutableStateOf(initialSubscription?.colorHex ?: "#10B981") }
    var notes by remember { mutableStateOf(initialSubscription?.notes ?: "") }
    var startDate by remember { mutableStateOf(initialSubscription?.startDate ?: LocalDate.now()) }

    val isEditing = initialSubscription != null

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(20.dp)),
            color = Color(0xFF0D0D0D),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
            ),
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isEditing) "Edit Subscription" else "Add Subscription",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isEditing) "Modify rotation settings and spend" else "Configure 45-day cycling parameters",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                // Scrollable Form Body
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Quick Presets (Monochromatic Luxury Pills)
                    if (!isEditing) {
                        Column {
                            Text(
                                text = "QUICK PRESETS",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PresetLibrary.presets.forEach { preset ->
                                    val isPresetSelected = name == preset.name
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isPresetSelected) PrimarySky.copy(alpha = 0.15f) else Color(0xFF141414),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isPresetSelected) PrimarySky.copy(alpha = 0.8f) else Color(0xFF242424)
                                        ),
                                        modifier = Modifier.clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            name = preset.name
                                            costStr = preset.defaultCost.toString()
                                            category = preset.category
                                            colorHex = preset.colorHex
                                            strategy = preset.suggestedStrategy
                                        }
                                    ) {
                                        Text(
                                            text = preset.name,
                                            color = if (isPresetSelected) PrimarySky else MaterialTheme.colorScheme.onSurface,
                                            fontWeight = if (isPresetSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.sp,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Name & Cost in Row or Stack
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Service Name
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Service Name") },
                            placeholder = { Text("e.g. Netflix") },
                            modifier = Modifier.weight(1.3f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF121212),
                                unfocusedContainerColor = Color(0xFF0F0F0F),
                                focusedBorderColor = PrimarySky,
                                unfocusedBorderColor = Color(0xFF242424),
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedLabelColor = PrimarySky,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) })
                        )

                        // Cost
                        OutlinedTextField(
                            value = costStr,
                            onValueChange = { costStr = it },
                            label = { Text("Cost ($)") },
                            placeholder = { Text("15.49") },
                            modifier = Modifier.weight(0.9f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF121212),
                                unfocusedContainerColor = Color(0xFF0F0F0F),
                                focusedBorderColor = PrimarySky,
                                unfocusedBorderColor = Color(0xFF242424),
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedLabelColor = PrimarySky,
                                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) })
                        )
                    }

                    // Category Selector Row
                    Column {
                        Text(
                            text = "CATEGORY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SubscriptionCategory.values().forEach { cat ->
                                val isSelected = category == cat
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) PrimarySky.copy(alpha = 0.15f) else Color(0xFF121212),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) PrimarySky.copy(alpha = 0.8f) else Color(0xFF222222)
                                    ),
                                    modifier = Modifier.clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        category = cat
                                    }
                                ) {
                                    Text(
                                        text = cat.displayName,
                                        color = if (isSelected) PrimarySky else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Strategy Selector (Compact 2x2 Bento Deck)
                    Column {
                        Text(
                            text = "CYCLE STRATEGY",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Row 1: 45-Day Smart & 60-Day Bi-Monthly
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StrategyBentoCard(
                                    title = "45-Day Smart",
                                    subtitle = "30d active / 15d pause",
                                    badge = "Save 33%",
                                    isSelected = strategy == RotationStrategy.ROTATION_45_DAYS,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        strategy = RotationStrategy.ROTATION_45_DAYS
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                StrategyBentoCard(
                                    title = "60-Day Swap",
                                    subtitle = "30d active / 30d pause",
                                    badge = "Save 50%",
                                    isSelected = strategy == RotationStrategy.ROTATION_60_DAYS,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        strategy = RotationStrategy.ROTATION_60_DAYS
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Row 2: Continuous & Custom
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StrategyBentoCard(
                                    title = "Continuous",
                                    subtitle = "Always active (0% save)",
                                    badge = "Standard",
                                    isSelected = strategy == RotationStrategy.CONTINUOUS,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        strategy = RotationStrategy.CONTINUOUS
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                StrategyBentoCard(
                                    title = "Custom Cycle",
                                    subtitle = "Set custom days",
                                    badge = "Custom",
                                    isSelected = strategy == RotationStrategy.CUSTOM,
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        strategy = RotationStrategy.CUSTOM
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Custom Days fields (if CUSTOM selected)
                    if (strategy == RotationStrategy.CUSTOM) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = customActiveDaysStr,
                                onValueChange = { customActiveDaysStr = it },
                                label = { Text("Active Days") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Right) }),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF121212),
                                    unfocusedContainerColor = Color(0xFF0F0F0F),
                                    focusedBorderColor = PrimarySky,
                                    unfocusedBorderColor = Color(0xFF242424),
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedLabelColor = PrimarySky,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            OutlinedTextField(
                                value = customBlackoutDaysStr,
                                onValueChange = { customBlackoutDaysStr = it },
                                label = { Text("Blackout Days") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color(0xFF121212),
                                    unfocusedContainerColor = Color(0xFF0F0F0F),
                                    focusedBorderColor = PrimarySky,
                                    unfocusedBorderColor = Color(0xFF242424),
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    focusedLabelColor = PrimarySky,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }

                    // Notes Input
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Strategy Notes (Optional)") },
                        placeholder = { Text("e.g. Cancel auto-renew after House of the Dragon") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF121212),
                            unfocusedContainerColor = Color(0xFF0F0F0F),
                            focusedBorderColor = PrimarySky,
                            unfocusedBorderColor = Color(0xFF242424),
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                            focusedLabelColor = PrimarySky,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Primary CTA Save Button
                Button(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        val parsedCost = costStr.toDoubleOrNull() ?: 0.0
                        val parsedActive = customActiveDaysStr.toIntOrNull() ?: 30
                        val parsedBlackout = customBlackoutDaysStr.toIntOrNull() ?: 15

                        val newSubscription = (initialSubscription ?: Subscription(
                            name = name,
                            cost = parsedCost,
                            startDate = startDate
                        )).copy(
                            name = name.ifBlank { "Subscription" },
                            cost = parsedCost,
                            category = category,
                            strategy = strategy,
                            customActiveDays = parsedActive,
                            customBlackoutDays = parsedBlackout,
                            colorHex = colorHex,
                            notes = notes
                        )
                        onSave(newSubscription)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimarySky,
                        contentColor = OledBlack
                    )
                ) {
                    Text(
                        text = if (isEditing) "Update Subscription" else "Add to Rotation Plan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StrategyBentoCard(
    title: String,
    subtitle: String,
    badge: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) PrimarySky.copy(alpha = 0.12f) else Color(0xFF121212),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) PrimarySky.copy(alpha = 0.8f) else Color(0xFF222222)
        ),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (isSelected) PrimarySky else MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isSelected) PrimarySky.copy(alpha = 0.2f) else Color(0xFF1A1A1A)
                ) {
                    Text(
                        text = badge,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) PrimarySky else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
