package com.subtrack.feature.dashboard

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.core.app.NotificationManagerCompat
import com.subtrack.core.domain.model.CalendarAwareSuggestion
import com.subtrack.core.domain.model.CalendarSuggestionType
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.ui.components.*
import com.subtrack.core.ui.theme.OledBlack
import com.subtrack.core.ui.theme.PrimarySky

@Composable
fun DashboardRoute(
    viewModel: DashboardViewModel = hiltViewModel(),
    openSubscriptionId: Long? = null,
    onOpenSubscriptionConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onAction(DashboardAction.CalendarPermissionResult(granted))
    }
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.monitorCalendarWhileForeground()
        }
    }
    DashboardScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onRequestCalendarPermission = { calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR) },
        openSubscriptionId = openSubscriptionId,
        onOpenSubscriptionConsumed = onOpenSubscriptionConsumed,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onAction: (DashboardAction) -> Unit,
    onRequestCalendarPermission: () -> Unit = {},
    openSubscriptionId: Long? = null,
    onOpenSubscriptionConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var subscriptionToEdit by remember { mutableStateOf<Subscription?>(null) }
    var selectedSubscriptionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var cancellationSuggestion by remember { mutableStateOf<CalendarAwareSuggestion?>(null) }

    LaunchedEffect(openSubscriptionId, uiState.cycleStatuses) {
        if (openSubscriptionId != null && uiState.cycleStatuses.any { it.subscription.id == openSubscriptionId }) {
            selectedSubscriptionId = openSubscriptionId
            onOpenSubscriptionConsumed()
        }
    }

    val openSuggestion: (CalendarAwareSuggestion) -> Unit = { suggestion ->
        when (suggestion.identity.suggestionType) {
            CalendarSuggestionType.TRAVEL_REVIEW_CANCELLATION -> cancellationSuggestion = suggestion
            CalendarSuggestionType.HOLIDAY_REVIEW_SUBSCRIPTION -> selectedSubscriptionId = suggestion.identity.subscriptionId
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SubCycler",
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                            color = PrimarySky.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                PrimarySky.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                text = "AGENTIC 45d",
                                color = PrimarySky,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                ),
                actions = {
                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    subscriptionToEdit = null
                    showAddDialog = true
                },
                containerColor = PrimarySky,
                contentColor = OledBlack,
                shape = androidx.compose.foundation.shape.CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Subscription")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Variation 3: Financial Efficiency Bar HUD (Borderless command header)
            item(key = "financial_efficiency_hud", contentType = "hud") {
                FinancialEfficiencyHud(
                    summary = uiState.portfolioSummary,
                    currencyCode = uiState.currencyCode
                )
            }

            if (uiState.excludedCurrencyCount > 0) {
                item(key = "currency_scope_notice", contentType = "notice") {
                    CurrencyScopeNotice(
                        currencyCode = uiState.currencyCode,
                        excludedCurrencyCount = uiState.excludedCurrencyCount
                    )
                }
            }

            // Urgent / Due Next Action Banner
            if (uiState.urgentStatuses.isNotEmpty()) {
                item(key = "urgent_banner", contentType = "urgent") {
                    val topUrgent = uiState.urgentStatuses.first()
                    val mergedSuggestion = uiState.calendarSuggestion?.takeIf {
                        it.identity.subscriptionId == topUrgent.subscription.id
                    }
                    NextActionBanner(
                        urgentStatuses = uiState.urgentStatuses,
                        calendarSuggestion = mergedSuggestion,
                        onCalendarPrimaryAction = { mergedSuggestion?.let(openSuggestion) },
                        onCalendarDismiss = {
                            mergedSuggestion?.let { onAction(DashboardAction.DismissCalendarSuggestion(it.identity)) }
                        }
                    )
                }
            } else if (uiState.calendarSuggestion != null) {
                item(key = "calendar_suggestion", contentType = "urgent") {
                    val suggestion = uiState.calendarSuggestion
                    CalendarSuggestionCard(
                        suggestion = suggestion,
                        onPrimaryAction = { openSuggestion(suggestion) },
                        onDismiss = { onAction(DashboardAction.DismissCalendarSuggestion(suggestion.identity)) }
                    )
                }
            }

            // Section Header & Filter Chips
            item(key = "filter_row", contentType = "filters") {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "App Drawer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = (-0.2).sp
                        )
                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                            color = Color(0xFF141414),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            )
                        ) {
                            Text(
                                text = "${uiState.cycleStatuses.size} services",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(SubscriptionFilter.values(), key = { it.name }) { filter ->
                            val isSelected = filter == uiState.selectedFilter
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onAction(DashboardAction.SetFilter(filter))
                                },
                                label = {
                                    Text(
                                        text = filter.displayName,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                },
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color(0xFF0D0D0D),
                                    selectedContainerColor = PrimarySky.copy(alpha = 0.16f),
                                    selectedLabelColor = PrimarySky,
                                    labelColor = Color(0xFF8E8E93)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) PrimarySky.copy(alpha = 0.85f) else Color(0xFF222222),
                                    borderWidth = 1.dp
                                )
                            )
                        }
                    }
                }
            }

            // Subscriptions 3-Column Grid
            if (uiState.cycleStatuses.isEmpty()) {
                item(key = "empty_state", contentType = "empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No subscriptions in this view",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap '+' to add a subscription and start rotating!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Chunk into rows of 3
                val rows = uiState.cycleStatuses.chunked(3)
                items(
                    items = rows,
                    key = { row -> row.joinToString("-") { it.subscription.id.toString() } },
                    contentType = { "grid_row" }
                ) { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (cycleStatus in rowItems) {
                            Box(modifier = Modifier.weight(1f)) {
                                SubscriptionGridItem(
                                    cycleStatus = cycleStatus,
                                    onClick = {
                                        selectedSubscriptionId = cycleStatus.subscription.id
                                    }
                                )
                            }
                        }
                        // Fill remaining slots in incomplete row
                        val remaining = 3 - rowItems.size
                        for (i in 0 until remaining) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    // Detail Bottom Sheet when an item is tapped
    uiState.cycleStatuses.find { it.subscription.id == selectedSubscriptionId }?.let { status ->
        SubscriptionDetailBottomSheet(
            cycleStatus = status,
            onDismiss = { selectedSubscriptionId = null },
            onToggleAutoRenew = {
                onAction(DashboardAction.ToggleAutoRenew(status.subscription.id))
            },
            onEdit = {
                subscriptionToEdit = status.subscription
                showAddDialog = true
            },
            onDelete = {
                onAction(DashboardAction.DeleteSubscription(status.subscription))
            }
        )
    }

    // Add / Edit Dialog
    if (showAddDialog) {
        AddEditSubscriptionDialog(
            initialSubscription = subscriptionToEdit,
            defaultCurrencyCode = uiState.preferredCurrencyCode,
            onDismiss = {
                showAddDialog = false
                subscriptionToEdit = null
            },
            onSave = { sub ->
                if (subscriptionToEdit != null) {
                    onAction(DashboardAction.UpdateSubscription(sub))
                } else {
                    onAction(DashboardAction.AddSubscription(sub))
                }
                showAddDialog = false
                subscriptionToEdit = null
            }
        )
    }

    if (showSettings) {
        CalendarSettingsSheet(
            enabled = uiState.calendarSettings.isEnabled,
            selectedCalendarCount = uiState.calendarSettings.selectedCalendarIds.size,
            accessIssue = uiState.calendarSettings.accessIssue,
            notificationsAvailable = NotificationManagerCompat.from(context).areNotificationsEnabled(),
            onEnabledChange = { enabled ->
                showSettings = false
                onAction(
                    if (enabled) DashboardAction.BeginCalendarSetup
                    else DashboardAction.DisableCalendarAwareness
                )
            },
            onOpenCalendars = {
                showSettings = false
                onAction(DashboardAction.OpenCalendarSelection)
            },
            onDismiss = { showSettings = false }
        )
    }

    if (uiState.calendarSetupStep == CalendarSetupStep.EXPLAINING) {
        AlertDialog(
            onDismissRequest = { onAction(DashboardAction.CancelCalendarSetup) },
            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
            title = { Text("Calendar-aware suggestions") },
            text = {
                Text(
                    "Event titles and dates are processed locally on this device. After granting read access, you choose which calendars SubCycler may check."
                )
            },
            dismissButton = {
                TextButton(onClick = { onAction(DashboardAction.CancelCalendarSetup) }) { Text("Cancel") }
            },
            confirmButton = {
                TextButton(onClick = onRequestCalendarPermission) { Text("Continue") }
            }
        )
    }

    if (uiState.calendarSetupStep == CalendarSetupStep.LOADING_CALENDARS) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Loading calendars") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("Checking accessible device calendars…")
                }
            },
            confirmButton = {}
        )
    }

    if (uiState.calendarSetupStep == CalendarSetupStep.SELECTING_CALENDARS) {
        CalendarSelectionSheet(
            calendars = uiState.accessibleCalendars,
            initiallySelectedIds = uiState.calendarSelectionSeed,
            onConfirm = { onAction(DashboardAction.ConfirmCalendarSelection(it)) },
            onDismiss = { onAction(DashboardAction.CancelCalendarSetup) }
        )
    }

    cancellationSuggestion?.let { suggestion ->
        CancellationOptionsDialog(
            subscriptionName = suggestion.subscriptionName,
            onDismiss = { cancellationSuggestion = null },
            onOpenLink = { openCancellationDestination(context, it) }
        )
    }
}
