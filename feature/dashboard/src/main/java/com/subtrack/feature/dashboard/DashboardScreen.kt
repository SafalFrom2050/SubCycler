package com.subtrack.feature.dashboard

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.subtrack.core.domain.model.BillingPeriod
import com.subtrack.core.domain.model.CalendarAwareSuggestion
import com.subtrack.core.domain.model.CalendarSuggestionType
import com.subtrack.core.domain.model.PendingSubscriptionAction
import com.subtrack.core.domain.model.PresetLibrary
import com.subtrack.core.domain.model.RegionalPriceCatalog
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.domain.model.SubscriptionActionProgressType
import com.subtrack.core.domain.model.SubscriptionPreset
import com.subtrack.core.ui.components.*
import com.subtrack.core.ui.theme.LocalSubCyclerDarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable
fun DashboardRoute(
    viewModel: DashboardViewModel = hiltViewModel(),
    openSubscriptionId: Long? = null,
    onOpenSubscriptionConsumed: () -> Unit = {},
    pendingAddSubscription: Boolean = false,
    onAddSubscriptionConsumed: () -> Unit = {},
    onNavigateToSavings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onAction(DashboardAction.CalendarPermissionResult(granted))
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val payload = viewModel.buildJsonBackup()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(payload.toByteArray(Charsets.UTF_8))
                    } ?: error("Output stream unavailable")
                }
            }.fold(
                onSuccess = { viewModel.onAction(DashboardAction.SetBackupFeedback("Backup saved locally.")) },
                onFailure = { viewModel.onAction(DashboardAction.SetBackupFeedback("Couldn't save backup file.")) }
            )
        }
    }

    val importJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
                }
            }.fold(
                onSuccess = { raw -> viewModel.onAction(DashboardAction.ImportBackupJson(raw)) },
                onFailure = { viewModel.onAction(DashboardAction.SetBackupFeedback("Couldn't read selected file.")) }
            )
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val csv = viewModel.buildCsvExport()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(csv.toByteArray(Charsets.UTF_8))
                    } ?: error("Output stream unavailable")
                }
            }.fold(
                onSuccess = { viewModel.onAction(DashboardAction.SetBackupFeedback("CSV exported locally.")) },
                onFailure = { viewModel.onAction(DashboardAction.SetBackupFeedback("Couldn't export CSV file.")) }
            )
        }
    }

    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.monitorCalendarWhileForeground()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onAction(DashboardAction.RefreshToday)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DashboardScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onAutoSaveSubscription = viewModel::saveSubscription,
        onRequestCalendarPermission = { calendarPermissionLauncher.launch(Manifest.permission.READ_CALENDAR) },
        onExportJson = { exportJsonLauncher.launch("subcycler-backup-${LocalDate.now()}.json") },
        onImportJson = { importJsonLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
        onExportCsv = { exportCsvLauncher.launch("subcycler-subscriptions-${LocalDate.now()}.csv") },
        onNavigateToSavings = onNavigateToSavings,
        openSubscriptionId = openSubscriptionId,
        onOpenSubscriptionConsumed = onOpenSubscriptionConsumed,
        pendingAddSubscription = pendingAddSubscription,
        onAddSubscriptionConsumed = onAddSubscriptionConsumed,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onAction: (DashboardAction) -> Unit,
    onAutoSaveSubscription: suspend (Subscription) -> Long = { sub ->
        if (sub.id > 0L) {
            onAction(DashboardAction.UpdateSubscription(sub))
            sub.id
        } else {
            onAction(DashboardAction.AddSubscription(sub))
            0L
        }
    },
    onRequestCalendarPermission: () -> Unit = {},
    onExportJson: () -> Unit = {},
    onImportJson: () -> Unit = {},
    onExportCsv: () -> Unit = {},
    onNavigateToSavings: () -> Unit = {},
    openSubscriptionId: Long? = null,
    onOpenSubscriptionConsumed: () -> Unit = {},
    pendingAddSubscription: Boolean = false,
    onAddSubscriptionConsumed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val isDark = LocalSubCyclerDarkTheme.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showAddDialog by remember { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var subscriptionToEdit by remember { mutableStateOf<Subscription?>(null) }
    var editInitialStep by remember { mutableIntStateOf(0) }
    var presetToAdd by remember { mutableStateOf<SubscriptionPreset?>(null) }
    var selectedSubscriptionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var activeBillingDialog by remember { mutableStateOf<PendingSubscriptionAction?>(null) }
    var notificationsEnabled by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }

    val lookupStatuses = uiState.allCycleStatuses.ifEmpty { uiState.cycleStatuses }

    LaunchedEffect(pendingAddSubscription) {
        if (pendingAddSubscription) {
            subscriptionToEdit = null
            editInitialStep = 0
            presetToAdd = null
            showAddDialog = true
            onAddSubscriptionConsumed()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                activeBillingDialog = null
            } else if (event == Lifecycle.Event.ON_RESUME) {
                notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(openSubscriptionId, lookupStatuses, uiState.isLoading) {
        if (openSubscriptionId != null) {
            if (lookupStatuses.any { it.subscription.id == openSubscriptionId }) {
                selectedSubscriptionId = openSubscriptionId
                onOpenSubscriptionConsumed()
            } else if (!uiState.isLoading) {
                // Consume stale deep-links pointing to a deleted subscription
                onOpenSubscriptionConsumed()
            }
        }
    }

    // Only opens CancellationOptionsDialog; actual progress tracking starts ONLY when user taps a provider link!
    val openBillingLinksDialog: (Long, String, SubscriptionActionProgressType) -> Unit = { subId, subName, actionType ->
        selectedSubscriptionId = null
        activeBillingDialog = PendingSubscriptionAction(
            subscriptionId = subId,
            subscriptionName = subName,
            actionType = actionType
        )
    }

    val openSuggestion: (CalendarAwareSuggestion) -> Unit = { suggestion ->
        when (suggestion.identity.suggestionType) {
            CalendarSuggestionType.TRAVEL_REVIEW_CANCELLATION -> {
                openBillingLinksDialog(
                    suggestion.identity.subscriptionId,
                    suggestion.subscriptionName,
                    SubscriptionActionProgressType.UNSUBSCRIBE
                )
            }
            CalendarSuggestionType.HOLIDAY_REVIEW_SUBSCRIPTION -> {
                selectedSubscriptionId = suggestion.identity.subscriptionId
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    ExpressiveBrandWordmark(fontSize = 28.sp)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = androidx.compose.ui.graphics.Color.Transparent
                ),
                actions = {
                    ExpressiveReactiveIconButton(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        onClick = {
                            onAction(DashboardAction.SetBackupFeedback(null))
                            showSettings = true
                        },
                        modifier = Modifier.padding(end = 12.dp),
                        shape = ExpressiveCookie12Shape,
                        containerColor = if (isDark) {
                            MaterialTheme.colorScheme.surfaceContainerHigh
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                        borderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.42f else 0.58f),
                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                        buttonSize = 44.dp,
                        iconSize = 20.dp,
                        motion = ReactiveMotionType.GEAR_SPIN,
                        externalTrigger = if (showSettings) 1 else 0
                    )
                }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 86.dp)
            )
        },
        containerColor = androidx.compose.ui.graphics.Color.Transparent
    ) { innerPadding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(top = 4.dp, bottom = 112.dp + navBarBottom)
        ) {
            // Expressive Hero Bento Card
            item(key = "financial_efficiency_hud", contentType = "hud") {
                FinancialEfficiencyHud(
                    summary = uiState.portfolioSummary,
                    currencyCode = uiState.currencyCode,
                    excludedCurrencyCount = uiState.excludedCurrencyCount,
                    incompleteCount = uiState.incompleteCount,
                    onSavingsClick = onNavigateToSavings,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            if (uiState.excludedCurrencyCount > 0) {
                item(key = "currency_scope_notice", contentType = "notice") {
                    CurrencyScopeNotice(
                        currencyCode = uiState.currencyCode,
                        excludedCurrencyCount = uiState.excludedCurrencyCount,
                        modifier = Modifier.padding(horizontal = 16.dp)
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
                        onUrgentActionClick = { urgentStatus ->
                            selectedSubscriptionId = urgentStatus.subscription.id
                        },
                        onCalendarPrimaryAction = { mergedSuggestion?.let(openSuggestion) },
                        onCalendarDismiss = {
                            mergedSuggestion?.let { onAction(DashboardAction.DismissCalendarSuggestion(it.identity)) }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            } else if (uiState.calendarSuggestion != null) {
                item(key = "calendar_suggestion", contentType = "urgent") {
                    val suggestion = uiState.calendarSuggestion
                    CalendarSuggestionCard(
                        suggestion = suggestion,
                        onPrimaryAction = { openSuggestion(suggestion) },
                        onDismiss = { onAction(DashboardAction.DismissCalendarSuggestion(suggestion.identity)) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Full-bleed Filter & Sort Pill Bar (zero edge clipping!)
            if (uiState.totalSubscriptionCount >= 2 || uiState.selectedFilter != SubscriptionFilter.ALL) {
                item(key = "filter_row", contentType = "filters") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(SubscriptionFilter.entries, key = { it.name }) { filter ->
                            val isSelected = filter == uiState.selectedFilter
                            val chipInteraction = remember { MutableInteractionSource() }
                            val chipPressed by chipInteraction.collectIsPressedAsState()
                            val chipCorner by animateDpAsState(
                                targetValue = if (chipPressed) 11.dp else 18.dp,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "filterChipCorner"
                            )
                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onAction(DashboardAction.SetFilter(filter))
                                },
                                interactionSource = chipInteraction,
                                shape = RoundedCornerShape(chipCorner),
                                color = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainer
                                },
                                border = if (!isSelected) expressiveCardBorder() else null
                            ) {
                                Box(
                                    modifier = Modifier
                                        .height(36.dp)
                                        .padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (filter == SubscriptionFilter.ALL) {
                                            "All · ${uiState.totalSubscriptionCount}"
                                        } else {
                                            filter.displayName
                                        },
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = if (isSelected) {
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        }
                                    )
                                }
                            }
                        }

                        // Compact Sort Pill (matching exact 36dp height & corner-morph feel!)
                        item(key = "sort_pill") {
                            val nextSort = SubscriptionSort.entries[
                                (uiState.selectedSort.ordinal + 1) % SubscriptionSort.entries.size
                            ]
                            val sortInteraction = remember { MutableInteractionSource() }
                            val sortPressed by sortInteraction.collectIsPressedAsState()
                            val sortCorner by animateDpAsState(
                                targetValue = if (sortPressed) 11.dp else 18.dp,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                label = "sortChipCorner"
                            )
                            Surface(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onAction(DashboardAction.SetSort(nextSort))
                                },
                                interactionSource = sortInteraction,
                                shape = RoundedCornerShape(sortCorner),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = expressiveCardBorder(if (isDark) 0.34f else 0.45f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .height(36.dp)
                                        .padding(horizontal = 13.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapVert,
                                        contentDescription = "Change sort order",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = uiState.selectedSort.displayName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Expressive 2-Column Bento Subscription Deck OR Rich Empty State
            if (uiState.cycleStatuses.isEmpty()) {
                item(key = "empty_state", contentType = "empty") {
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        DashboardEmptyState(
                            isFilteredEmpty = uiState.totalSubscriptionCount > 0,
                            currencyCode = uiState.preferredCurrencyCode,
                            onClearFilter = { onAction(DashboardAction.SetFilter(SubscriptionFilter.ALL)) },
                            onQuickAddPreset = { presetName ->
                                val preset = PresetLibrary.presets.firstOrNull { it.name == presetName }
                                if (preset != null) {
                                    val tier = RegionalPriceCatalog.defaultTierFor(preset.name, uiState.preferredCurrencyCode)
                                    if (tier != null) {
                                        val period = tier.billingPeriod
                                        onAction(
                                            DashboardAction.AddSubscription(
                                                Subscription(
                                                    name = preset.name,
                                                    cost = tier.price,
                                                    currency = uiState.preferredCurrencyCode,
                                                    billingPeriod = period,
                                                    category = preset.category,
                                                    startDate = LocalDate.now(),
                                                    strategy = if (period == BillingPeriod.YEARLY) RotationStrategy.CONTINUOUS else preset.suggestedStrategy,
                                                    customActiveDays = preset.suggestedActiveDays,
                                                    customBlackoutDays = preset.suggestedBlackoutDays,
                                                    colorHex = preset.colorHex,
                                                    iconName = preset.iconName,
                                                    isBillingDateConfirmed = false
                                                )
                                            )
                                        )
                                    } else {
                                        // Never relabel USD preset prices into uncatalogued currencies: open sheet prefilled!
                                        subscriptionToEdit = null
                                        editInitialStep = 0
                                        presetToAdd = preset
                                        showAddDialog = true
                                    }
                                }
                            },
                            onAddCustom = {
                                subscriptionToEdit = null
                                editInitialStep = 0
                                presetToAdd = null
                                showAddDialog = true
                            }
                        )
                    }
                }
            } else {
                val rows = uiState.cycleStatuses.chunked(2)
                items(
                    items = rows,
                    key = { row -> row.joinToString("-") { it.subscription.id.toString() } },
                    contentType = { "bento_row" }
                ) { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(IntrinsicSize.Min)
                            .animateItem(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (cycleStatus in rowItems) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                SubscriptionGridItem(
                                    cycleStatus = cycleStatus,
                                    onClick = {
                                        selectedSubscriptionId = cycleStatus.subscription.id
                                    },
                                    modifier = Modifier.fillMaxHeight()
                                )
                            }
                        }
                        if (rowItems.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    // Detail Bottom Sheet when an item is tapped (uses full unfiltered list so deep-links & banners always resolve)
    lookupStatuses.find { it.subscription.id == selectedSubscriptionId }?.let { status ->
        SubscriptionDetailBottomSheet(
            cycleStatus = status,
            onDismiss = { selectedSubscriptionId = null },
            onToggleAutoRenew = {
                onAction(DashboardAction.ToggleAutoRenew(status.subscription.id))
            },
            onStartSubscriptionAction = { actionType ->
                openBillingLinksDialog(status.subscription.id, status.subscription.name, actionType)
            },
            onExtendCycle = {
                onAction(DashboardAction.ExtendActiveCycle(status.subscription.id))
            },
            onResumeToday = {
                onAction(DashboardAction.ResumeToday(status.subscription.id))
            },
            onEdit = {
                subscriptionToEdit = status.subscription
                editInitialStep = 0
                showAddDialog = true
            },
            onEditStep = { step ->
                subscriptionToEdit = status.subscription
                editInitialStep = step
                showAddDialog = true
            },
            onDelete = {
                val deleted = status.subscription
                onAction(DashboardAction.DeleteSubscription(deleted))
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "Deleted ${deleted.name}",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        onAction(DashboardAction.AddSubscription(deleted.copy(id = 0L)))
                    }
                }
            }
        )
    }

    // Provider Billing Links Dialog — persists pending progress ONLY when user opens a real link!
    activeBillingDialog?.let { billingAction ->
        CancellationOptionsDialog(
            subscriptionName = billingAction.subscriptionName,
            actionType = billingAction.actionType,
            onDismiss = { activeBillingDialog = null },
            onOpenLink = { destination ->
                val opened = openCancellationDestination(context, destination)
                if (opened) {
                    onAction(
                        DashboardAction.StartSubscriptionActionProgress(
                            subscriptionId = billingAction.subscriptionId,
                            subscriptionName = billingAction.subscriptionName,
                            actionType = billingAction.actionType
                        )
                    )
                }
                opened
            }
        )
    }

    // Persisted Subscribe / Unsubscribe Recovery Popup
    val pendingToRecover = uiState.pendingSubscriptionAction
    if (pendingToRecover != null && activeBillingDialog == null && selectedSubscriptionId == null) {
        PendingSubscriptionActionDialog(
            pendingAction = pendingToRecover,
            onCompleted = {
                onAction(DashboardAction.ResolvePendingSubscriptionAction(completed = true))
            },
            onCancel = {
                onAction(DashboardAction.ResolvePendingSubscriptionAction(completed = false))
            }
        )
    }

    if (showAddDialog) {
        AddEditSubscriptionDialog(
            initialSubscription = subscriptionToEdit,
            initialPreset = presetToAdd,
            initialStep = editInitialStep,
            defaultCurrencyCode = uiState.preferredCurrencyCode,
            onDismiss = {
                showAddDialog = false
                subscriptionToEdit = null
                editInitialStep = 0
                presetToAdd = null
            },
            onAutoSave = onAutoSaveSubscription
        )
    }

    if (showSettings) {
        CalendarSettingsSheet(
            enabled = uiState.calendarSettings.isEnabled,
            selectedCalendarCount = uiState.calendarSettings.selectedCalendarIds.size,
            accessIssue = uiState.calendarSettings.accessIssue,
            notificationsAvailable = notificationsEnabled,
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
            onDismiss = { showSettings = false },
            currencyCode = uiState.preferredCurrencyCode,
            hasExistingSubscriptions = uiState.totalSubscriptionCount > 0,
            notificationLeadDays = uiState.notificationLeadDays,
            themeMode = uiState.themeMode,
            onCurrencySelected = { onAction(DashboardAction.SetDefaultCurrency(it)) },
            onNotificationLeadDaysSelected = { onAction(DashboardAction.SetNotificationLeadDays(it)) },
            onThemeModeSelected = { onAction(DashboardAction.SetThemeMode(it)) },
            onOpenNotificationSettings = {
                runCatching {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            },
            onExportJson = onExportJson,
            onImportJson = onImportJson,
            onExportCsv = onExportCsv,
            backupMessage = uiState.backupFeedback
        )
    }

    if (uiState.calendarSetupStep == CalendarSetupStep.EXPLAINING) {
        AlertDialog(
            onDismissRequest = { onAction(DashboardAction.CancelCalendarSetup) },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            icon = { Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = {
                Text(
                    "Calendar-aware suggestions",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Text(
                    "Event titles and dates are processed locally on this device. After granting read access, you choose which calendars SubCycler may check."
                )
            },
            dismissButton = {
                TextButton(onClick = { onAction(DashboardAction.CancelCalendarSetup) }) { Text("Cancel") }
            },
            confirmButton = {
                Button(
                    onClick = onRequestCalendarPermission,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) { Text("Continue", fontWeight = FontWeight.Bold) }
            }
        )
    }

    if (uiState.calendarSetupStep == CalendarSetupStep.LOADING_CALENDARS) {
        AlertDialog(
            onDismissRequest = {},
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            title = {
                Text(
                    "Loading calendars",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
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
}

@Composable
private fun DashboardEmptyState(
    isFilteredEmpty: Boolean,
    currencyCode: String,
    onClearFilter: () -> Unit,
    onQuickAddPreset: (String) -> Unit,
    onAddCustom: () -> Unit
) {
    val isDark = LocalSubCyclerDarkTheme.current
    val boldPalette = ExpressivePalettes.boldPrimary()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = if (isDark) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(ExpressiveCookie8Shape)
                    .background(boldPalette.container, ExpressiveCookie8Shape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Autorenew,
                    contentDescription = null,
                    tint = boldPalette.content,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = if (isFilteredEmpty) "No services match this filter" else "Your stack is empty",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = if (isFilteredEmpty) {
                    "Switch back to All to see your subscriptions."
                } else {
                    "Tap a popular service to start tracking in $currencyCode, or add your own."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            if (isFilteredEmpty) {
                FilledTonalButton(
                    onClick = onClearFilter,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text("Show all subscriptions", fontWeight = FontWeight.Bold)
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("Netflix", "Spotify", "ChatGPT Plus").forEach { preset ->
                        Surface(
                            onClick = { onQuickAddPreset(preset) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Box(
                                modifier = Modifier
                                    .heightIn(min = 42.dp)
                                    .padding(horizontal = 8.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "+ ${preset.substringBefore(" ")}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onAddCustom,
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add custom subscription", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}
