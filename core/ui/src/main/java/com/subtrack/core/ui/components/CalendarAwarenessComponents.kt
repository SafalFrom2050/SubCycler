package com.subtrack.core.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.subtrack.core.domain.model.CalendarAccessIssue
import com.subtrack.core.domain.model.CalendarAwareSuggestion
import com.subtrack.core.domain.model.DeviceCalendar
import com.subtrack.core.ui.theme.PrimarySky

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarSettingsSheet(
    enabled: Boolean,
    selectedCalendarCount: Int,
    accessIssue: CalendarAccessIssue?,
    notificationsAvailable: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onOpenCalendars: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("Settings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Calendar-aware suggestions", fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Use selected device calendars for timely subscription suggestions.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(16.dp))
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange
                )
            }

            if (enabled) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onOpenCalendars)
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Calendars", fontWeight = FontWeight.Medium)
                        Text(
                            "$selectedCalendarCount selected",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text("Change", color = PrimarySky, style = MaterialTheme.typography.labelLarge)
                }
                if (!notificationsAvailable) {
                    Text(
                        "Notifications are off. Calendar suggestions will still appear in SubCycler.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            accessIssue?.let {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = when (it) {
                        CalendarAccessIssue.PERMISSION_DENIED -> "Calendar permission was not granted. The feature remains off."
                        CalendarAccessIssue.PERMISSION_REVOKED -> "Calendar access was removed. The feature was turned off."
                        CalendarAccessIssue.NO_ACCESSIBLE_CALENDARS -> "No selected device calendars are currently accessible."
                        CalendarAccessIssue.PROVIDER_UNAVAILABLE -> "Calendar data is currently unavailable. The feature was turned off."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarSelectionSheet(
    calendars: List<DeviceCalendar>,
    initiallySelectedIds: Set<Long>,
    onConfirm: (Set<Long>) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedIds by remember(calendars, initiallySelectedIds) {
        mutableStateOf(initiallySelectedIds)
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text("Choose calendars", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Only events from calendars you select here are checked. Android's permission itself covers device calendars broadly.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                items(calendars, key = { it.id }) { calendar ->
                    val selected = calendar.id in selectedIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedIds = if (selected) selectedIds - calendar.id else selectedIds + calendar.id
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selected,
                            onCheckedChange = { checked ->
                                selectedIds = if (checked) selectedIds + calendar.id else selectedIds - calendar.id
                            }
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(calendar.displayName, fontWeight = FontWeight.Medium)
                            if (calendar.accountContext.isNotBlank()) {
                                Text(
                                    calendar.accountContext,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(
                    onClick = { onConfirm(selectedIds) },
                    enabled = selectedIds.isNotEmpty(),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Use selected")
                }
            }
        }
    }
}

@Composable
fun CalendarSuggestionCard(
    suggestion: CalendarAwareSuggestion,
    onPrimaryAction: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = PrimarySky)
                Spacer(Modifier.width(10.dp))
                Text("Calendar suggestion", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text(suggestion.message, style = MaterialTheme.typography.bodySmall)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) { Text("Dismiss") }
                TextButton(onClick = onPrimaryAction) { Text(suggestion.primaryActionLabel) }
            }
        }
    }
}
