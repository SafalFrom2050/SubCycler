package com.subtrack.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.subtrack.app.data.model.*
import com.subtrack.app.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSubscriptionDialog(
    initialSubscription: Subscription? = null,
    onDismiss: () -> Unit,
    onSave: (Subscription) -> Unit
) {
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
                .fillMaxHeight(0.9f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "Edit Subscription" else "Add Subscription",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Scrollable Form
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Presets Carousel (if not editing)
                    if (!isEditing) {
                        Text(
                            text = "Quick Presets",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PresetLibrary.presets.forEach { preset ->
                                val presetColor = try {
                                    Color(android.graphics.Color.parseColor(preset.colorHex))
                                } catch (e: Exception) {
                                    Emerald500
                                }
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = presetColor.copy(alpha = 0.12f),
                                    border = CardDefaults.outlinedCardBorder().copy(
                                        brush = androidx.compose.ui.graphics.SolidColor(presetColor.copy(alpha = 0.3f))
                                    ),
                                    modifier = Modifier.clickable {
                                        name = preset.name
                                        costStr = preset.defaultCost.toString()
                                        category = preset.category
                                        colorHex = preset.colorHex
                                        strategy = preset.suggestedStrategy
                                    }
                                ) {
                                    Text(
                                        text = preset.name,
                                        color = presetColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Name Input
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Service Name") },
                        placeholder = { Text("e.g. Netflix, Spotify") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Cost Input
                    OutlinedTextField(
                        value = costStr,
                        onValueChange = { costStr = it },
                        label = { Text("Monthly Cost ($)") },
                        placeholder = { Text("15.49") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Strategy Selector
                    Text(
                        text = "Cycle & Rotation Strategy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        RotationStrategy.values().forEach { strat ->
                            val isSelected = strategy == strat
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Emerald500.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isSelected) CardDefaults.outlinedCardBorder().copy(
                                    brush = androidx.compose.ui.graphics.SolidColor(Emerald500)
                                ) else null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { strategy = strat }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { strategy = strat },
                                        colors = RadioButtonDefaults.colors(selectedColor = Emerald500)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = strat.displayName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isSelected) Emerald500 else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = strat.description,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
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
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = customBlackoutDaysStr,
                                onValueChange = { customBlackoutDaysStr = it },
                                label = { Text("Blackout Days") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // Notes Input
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes (Optional)") },
                        placeholder = { Text("e.g. Cancel after Stranger Things") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Save Button
                Button(
                    onClick = {
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
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald500)
                ) {
                    Text(
                        text = if (isEditing) "Update Subscription" else "Add to Rotation Plan",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = LightSurface
                    )
                }
            }
        }
    }
}
