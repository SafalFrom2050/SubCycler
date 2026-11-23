package com.subtrack.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subtrack.core.common.currency.MoneyFormatter
import com.subtrack.core.domain.model.AppThemeMode
import com.subtrack.core.domain.model.CalendarAccessIssue
import com.subtrack.core.domain.model.CalendarAwareSuggestion
import com.subtrack.core.domain.model.DeviceCalendar
import com.subtrack.core.ui.theme.LocalSubCyclerDarkTheme

/**
 * Full Offline Settings Sheet (preserves backward-compatible signature & Calendar toggle as
 * the sheet's single Switch so CalendarAwarenessComponentsTest stays 100% compatible).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarSettingsSheet(
    enabled: Boolean,
    selectedCalendarCount: Int,
    accessIssue: CalendarAccessIssue?,
    notificationsAvailable: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onOpenCalendars: () -> Unit,
    onDismiss: () -> Unit,
    currencyCode: String = "USD",
    hasExistingSubscriptions: Boolean = false,
    notificationLeadDays: Int = 3,
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    onCurrencySelected: ((String) -> Unit)? = null,
    onNotificationLeadDaysSelected: ((Int) -> Unit)? = null,
    onThemeModeSelected: ((AppThemeMode) -> Unit)? = null,
    onOpenNotificationSettings: (() -> Unit)? = null,
    onExportJson: (() -> Unit)? = null,
    onImportJson: (() -> Unit)? = null,
    onExportCsv: (() -> Unit)? = null,
    backupMessage: String? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val skyPalette = ExpressivePalettes.softSky()
    val cardBorder = expressiveCardBorder()
    var showCurrencyPicker by remember { mutableStateOf(false) }
    var pendingCurrencyChange by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 1. Calendar-Aware Suggestions Card (Single Switch in sheet!)
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = cardBorder
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(ExpressiveCookie8Shape)
                                .background(skyPalette.container, ExpressiveCookie8Shape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = skyPalette.content,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Calendar-aware suggestions",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                "Use selected device calendars for timely subscription suggestions.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Switch(
                            checked = enabled,
                            onCheckedChange = onEnabledChange
                        )
                    }

                    if (enabled) {
                        Spacer(Modifier.height(14.dp))
                        Surface(
                            onClick = onOpenCalendars,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        "Calendars",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        "$selectedCalendarCount selected",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Change calendars",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            accessIssue?.let {
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

            // 2. Notification Permission & Reminder Lead Time
            if (onNotificationLeadDaysSelected != null || !notificationsAvailable) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = cardBorder
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ExpressiveEyebrow(text = "LOCAL REMINDERS")

                        if (!notificationsAvailable) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.65f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsOff,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(
                                            text = "Notifications are off for SubCycler.",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                    if (onOpenNotificationSettings != null) {
                                        TextButton(onClick = onOpenNotificationSettings) {
                                            Text(
                                                "Enable",
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onErrorContainer
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (onNotificationLeadDaysSelected != null) {
                            Text(
                                text = "Remind me before renewal",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val leadOptions = remember {
                                listOf(1, 3, 5, 7).map { ExpressiveSegmentOption(it, "${it}d") }
                            }
                            ExpressiveSegmentedGroup(
                                options = leadOptions,
                                selectedValue = notificationLeadDays,
                                onSelect = onNotificationLeadDaysSelected,
                                height = 42.dp,
                                fontSize = 12.5.sp
                            )
                        }
                    }
                }
            }

            // 3. Default Currency & Appearance
            if (onCurrencySelected != null || onThemeModeSelected != null) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = cardBorder
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ExpressiveEyebrow(text = "PREFERENCES")

                        if (onCurrencySelected != null) {
                            Surface(
                                onClick = { showCurrencyPicker = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "App currency",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${MoneyFormatter.symbol(currencyCode)} $currencyCode · ${MoneyFormatter.displayName(currencyCode)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Change currency",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        if (onThemeModeSelected != null) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Appearance",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.5.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                val themeOptions = remember {
                                    listOf(
                                        ExpressiveSegmentOption(AppThemeMode.SYSTEM, AppThemeMode.SYSTEM.displayName, Icons.Default.BrightnessAuto),
                                        ExpressiveSegmentOption(AppThemeMode.LIGHT, AppThemeMode.LIGHT.displayName, Icons.Default.LightMode),
                                        ExpressiveSegmentOption(AppThemeMode.DARK, AppThemeMode.DARK.displayName, Icons.Default.DarkMode)
                                    )
                                }
                                ExpressiveSegmentedGroup(
                                    options = themeOptions,
                                    selectedValue = themeMode,
                                    onSelect = onThemeModeSelected,
                                    height = 42.dp,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // 4. Offline Data Portability (JSON Backup/Restore & CSV Export — Zero Permissions)
            if (onExportJson != null || onImportJson != null || onExportCsv != null) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = cardBorder
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ExpressiveEyebrow(text = "OFFLINE BACKUP & EXPORT")
                        Text(
                            text = "Keep a local copy of your subscriptions and earned savings on device storage — no cloud required.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (onExportJson != null) {
                                FilledTonalButton(
                                    onClick = onExportJson,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    shape = RoundedCornerShape(999.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Backup JSON", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            if (onImportJson != null) {
                                FilledTonalButton(
                                    onClick = onImportJson,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    shape = RoundedCornerShape(999.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                        contentColor = MaterialTheme.colorScheme.onSurface
                                    )
                                ) {
                                    Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Restore", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        if (onExportCsv != null) {
                            FilledTonalButton(
                                onClick = onExportCsv,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(999.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                )
                            ) {
                                Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Export spreadsheet (.csv)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        if (!backupMessage.isNullOrBlank()) {
                            Text(
                                text = backupMessage,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCurrencyPicker && onCurrencySelected != null) {
        CurrencyPickerDialog(
            selectedCurrencyCode = currencyCode,
            onDismiss = { showCurrencyPicker = false },
            onSelect = { code ->
                showCurrencyPicker = false
                if (code != currencyCode) {
                    pendingCurrencyChange = code
                }
            }
        )
    }

    pendingCurrencyChange?.let { targetCode ->
        if (onCurrencySelected != null) {
            AlertDialog(
                onDismissRequest = { pendingCurrencyChange = null },
                shape = RoundedCornerShape(28.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                title = {
                    Text(
                        text = "Switch app currency to $targetCode?",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp
                    )
                },
                text = {
                    val symbol = MoneyFormatter.symbol(targetCode)
                    val targetDisplay = if (symbol.equals(targetCode, ignoreCase = true)) {
                        targetCode
                    } else {
                        "$targetCode ($symbol)"
                    }
                    Text(
                        text = if (hasExistingSubscriptions) {
                            "SubCycler works 100% offline and does not convert currencies automatically. All existing subscriptions will switch to $targetDisplay with their current numbers unchanged — please update each service's price manually."
                        } else {
                            "SubCycler works 100% offline and does not convert currencies automatically. Prices are not converted when switching currencies and should be entered or updated manually."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                dismissButton = {
                    TextButton(onClick = { pendingCurrencyChange = null }) {
                        Text("Cancel", fontWeight = FontWeight.SemiBold)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val chosen = targetCode
                            pendingCurrencyChange = null
                            onCurrencySelected(chosen)
                        },
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Switch to $targetCode", fontWeight = FontWeight.ExtraBold)
                    }
                }
            )
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = {
            BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outlineVariant)
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp)
        ) {
            Text(
                text = "Choose calendars",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Only events from calendars you select here are checked. Android's permission itself covers device calendars broadly.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(calendars, key = { it.id }) { calendar ->
                    val selected = calendar.id in selectedIds
                    Surface(
                        onClick = {
                            selectedIds = if (selected) selectedIds - calendar.id else selectedIds + calendar.id
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 8.dp),
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
                                Text(
                                    calendar.displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                if (calendar.accountContext.isNotBlank()) {
                                    Text(
                                        calendar.accountContext,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f)
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilledTonalButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text("Cancel", fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = { onConfirm(selectedIds) },
                    enabled = selectedIds.isNotEmpty(),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Use selected", fontWeight = FontWeight.ExtraBold)
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
    val lavenderPalette = ExpressivePalettes.softLavender()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = lavenderPalette.container
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(ExpressiveClover4Shape)
                        .background(lavenderPalette.selectedContainer, ExpressiveClover4Shape),
                    contentAlignment = Alignment.Center
                ) {
                    ReactiveIcon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = lavenderPalette.content,
                        size = 17.dp,
                        motion = ReactiveMotionType.POP_BOUNCE,
                        triggerKey = suggestion.identity,
                        animateOnFirstComposition = true
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "Calendar suggestion",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = lavenderPalette.content
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                suggestion.message,
                style = MaterialTheme.typography.bodySmall,
                color = lavenderPalette.content.copy(alpha = 0.85f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Dismiss", color = lavenderPalette.content.copy(alpha = 0.72f))
                }
                TextButton(onClick = onPrimaryAction) {
                    Text(suggestion.primaryActionLabel, color = lavenderPalette.content, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
