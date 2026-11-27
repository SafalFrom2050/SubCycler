package com.subtrack.feature.dashboard

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.subtrack.core.common.currency.MoneyFormatter
import com.subtrack.core.domain.model.BillingPeriod
import com.subtrack.core.domain.model.CalendarAwareSuggestion
import com.subtrack.core.domain.model.CalendarSuggestionType
import com.subtrack.core.domain.model.CyclePhase
import com.subtrack.core.domain.model.PendingSubscriptionAction
import com.subtrack.core.domain.model.PresetLibrary
import com.subtrack.core.domain.model.RegionalPriceCatalog
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.domain.model.SubscriptionActionProgressType
import com.subtrack.core.domain.model.SubscriptionPreset
import com.subtrack.core.ui.components.AddEditSubscriptionDialog
import com.subtrack.core.ui.components.CalendarSelectionSheet
import com.subtrack.core.ui.components.CalendarSettingsSheet
import com.subtrack.core.ui.components.CalendarSuggestionCard
import com.subtrack.core.ui.components.CancellationOptionsDialog
import com.subtrack.core.ui.components.CheckInDeckItem
import com.subtrack.core.ui.components.CompactEfficiencyCapsule
import com.subtrack.core.ui.components.CurrencyScopeNotice
import com.subtrack.core.ui.components.CycleCheckInDeck
import com.subtrack.core.ui.components.ExpressiveCookie12Shape
import com.subtrack.core.ui.components.ExpressiveCookie8Shape
import com.subtrack.core.ui.components.ExpressiveKineticBrandHeader
import com.subtrack.core.ui.components.ExpressivePalettes
import com.subtrack.core.ui.components.ExpressiveReactiveIconButton
import com.subtrack.core.ui.components.ExpressiveRemovalCollapse
import com.subtrack.core.ui.components.ExpressiveSnackHost
import com.subtrack.core.ui.components.FannedBadgeStack
import com.subtrack.core.ui.components.FinancialEfficiencyHud
import com.subtrack.core.ui.components.ImportPreviewSheet
import com.subtrack.core.ui.components.ImportStackSheet
import com.subtrack.core.ui.components.NextActionBanner
import com.subtrack.core.ui.components.PendingSubscriptionActionDialog
import com.subtrack.core.ui.components.ReactiveMotionType
import com.subtrack.core.ui.components.ShapeBloomBackdrop
import com.subtrack.core.ui.components.ShareStackSheet
import com.subtrack.core.ui.components.SubscriptionDetailBottomSheet
import com.subtrack.core.ui.components.SubscriptionGridItem
import com.subtrack.core.ui.components.buildCycleCheckInItems
import com.subtrack.core.ui.components.buildImportPreviewItems
import com.subtrack.core.ui.components.expressiveCardBorder
import com.subtrack.core.ui.components.expressivePolygonSurface
import com.subtrack.core.ui.components.openCancellationDestination
import com.subtrack.core.ui.components.progressiveEdgeFade
import com.subtrack.core.ui.components.rememberExpressiveSnackHostState
import com.subtrack.core.ui.theme.DisplayFontFamily
import com.subtrack.core.ui.theme.ExpressiveMotion
import com.subtrack.core.ui.theme.LocalSubCyclerDarkTheme
import com.subtrack.core.ui.theme.orSnapIfReduced
import com.subtrack.core.ui.theme.rememberReducedMotionEnabled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate

@Composable
fun DashboardRoute(
    viewModel: DashboardViewModel = hiltViewModel(),
    openSubscriptionId: Long? = null,
    onOpenSubscriptionConsumed: () -> Unit = {},
    pendingTransferPayload: String? = null,
    onTransferPayloadConsumed: () -> Unit = {},
    pendingAddSubscription: Boolean = false,
    onAddSubscriptionConsumed: () -> Unit = {},
    onNavigateToSavings: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingSaveIncludeHistory by remember { mutableStateOf(true) }

    LaunchedEffect(pendingTransferPayload) {
        val payload = pendingTransferPayload ?: return@LaunchedEffect
        viewModel.onAction(DashboardAction.InspectIncomingTransfer(payload))
        onTransferPayloadConsumed()
    }

    val calendarPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        viewModel.onAction(DashboardAction.CalendarPermissionResult(granted))
    }

    val saveBackupFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                val payload = viewModel.buildBackupFilePayload(pendingSaveIncludeHistory)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        stream.write(payload.toByteArray(Charsets.UTF_8))
                    } ?: error("Output stream unavailable")
                }
            }.fold(
                onSuccess = { viewModel.onAction(DashboardAction.SetBackupFeedback("Backup file saved to device.")) },
                onFailure = { viewModel.onAction(DashboardAction.SetBackupFeedback("Couldn't save backup file.")) }
            )
        }
    }

    val openBackupFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val maxBytes = 512 * 1024
                        val buffer = ByteArray(4096)
                        val out = ByteArrayOutputStream()
                        var total = 0
                        while (true) {
                            val read = stream.read(buffer)
                            if (read <= 0) break
                            total += read
                            require(total <= maxBytes) { "Selected file is too large." }
                            out.write(buffer, 0, read)
                        }
                        out.toString(Charsets.UTF_8.name())
                    }.orEmpty()
                }
            }.fold(
                onSuccess = { raw -> viewModel.onAction(DashboardAction.InspectIncomingTransfer(raw)) },
                onFailure = { viewModel.onAction(DashboardAction.SetBackupFeedback("Couldn't read selected file.")) }
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
        onShareTransferLink = { includeHistory ->
            scope.launch {
                runCatching {
                    val shareMessage = viewModel.buildShareMessage(includeHistory)
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "SubCycler stack")
                        putExtra(Intent.EXTRA_TEXT, shareMessage)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share SubCycler stack"))
                }.onFailure {
                    viewModel.onAction(DashboardAction.SetBackupFeedback("No app available to share."))
                }
            }
        },
        onCopyTransferLink = { includeHistory ->
            scope.launch {
                runCatching {
                    val link = viewModel.buildTransferLink(includeHistory)
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    clipboard?.setPrimaryClip(ClipData.newPlainText("SubCycler transfer link", link))
                }.fold(
                    onSuccess = { viewModel.onAction(DashboardAction.SetBackupFeedback("Transfer link copied.")) },
                    onFailure = { viewModel.onAction(DashboardAction.SetBackupFeedback("Couldn't copy transfer link.")) }
                )
            }
        },
        onShareBackupFile = { includeHistory ->
            scope.launch {
                runCatching {
                    val payload = viewModel.buildBackupFilePayload(includeHistory)
                    val uri = withContext(Dispatchers.IO) {
                        val cacheDir = File(context.cacheDir, "shared_backups").apply {
                            mkdirs()
                            listFiles()?.forEach { it.delete() }
                        }
                        val file = File(cacheDir, "subcycler-backup-${LocalDate.now()}.subcycler")
                        file.writeText(payload, Charsets.UTF_8)
                        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                    }
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "application/octet-stream"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, "SubCycler backup")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share SubCycler backup"))
                }.onFailure {
                    viewModel.onAction(DashboardAction.SetBackupFeedback("Couldn't share backup file."))
                }
            }
        },
        onSaveBackupFile = { includeHistory ->
            pendingSaveIncludeHistory = includeHistory
            saveBackupFileLauncher.launch("subcycler-backup-${LocalDate.now()}.subcycler")
        },
        onPickBackupFile = {
            openBackupFileLauncher.launch(arrayOf("application/octet-stream", "text/plain", "*/*"))
        },
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
    onShareTransferLink: (includePersonalHistory: Boolean) -> Unit = {},
    onCopyTransferLink: (includePersonalHistory: Boolean) -> Unit = {},
    onShareBackupFile: (includePersonalHistory: Boolean) -> Unit = {},
    onSaveBackupFile: (includePersonalHistory: Boolean) -> Unit = {},
    onPickBackupFile: () -> Unit = {},
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
    val reducedMotion = rememberReducedMotionEnabled()
    val snackHostState = rememberExpressiveSnackHostState()
    val scope = rememberCoroutineScope()
    val feedListState = rememberLazyListState()
    val filterRowState = rememberLazyListState()

    var showAddDialog by remember { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var showShareSheet by rememberSaveable { mutableStateOf(false) }
    var showImportSheet by rememberSaveable { mutableStateOf(false) }
    var subscriptionToEdit by remember { mutableStateOf<Subscription?>(null) }
    var editInitialStep by remember { mutableIntStateOf(0) }
    var presetToAdd by remember { mutableStateOf<SubscriptionPreset?>(null) }
    var selectedSubscriptionId by rememberSaveable { mutableStateOf<Long?>(null) }
    var activeBillingDialog by remember { mutableStateOf<PendingSubscriptionAction?>(null) }
    var notificationsEnabled by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }
    var brandTickerText by remember { mutableStateOf<String?>(null) }
    var brandTickerNonce by remember { mutableIntStateOf(0) }
    var hideCheckInDeck by rememberSaveable { mutableStateOf(false) }
    val pendingRemovalIds = remember { mutableStateListOf<Long>() }

    fun pulseBrandTicker(message: String) {
        brandTickerText = message
        brandTickerNonce++
    }

    LaunchedEffect(brandTickerNonce) {
        if (brandTickerText == null) return@LaunchedEffect
        delay(1500L)
        brandTickerText = null
    }

    // Surface offline backup feedback as a 3D stacked pill + brand ticker pulse
    LaunchedEffect(uiState.backupFeedback) {
        val msg = uiState.backupFeedback ?: return@LaunchedEffect
        snackHostState.show(
            message = msg,
            icon = Icons.Default.DownloadDone,
            dedupeKey = "backup_$msg"
        )
        pulseBrandTicker("Synced locally")
    }

    // Derive continuous scroll collapse progress (0f..1f) for Hero watermark parallax + pinned Mini-Ledger Capsule
    val heroCollapseProgress by remember {
        derivedStateOf {
            when {
                feedListState.firstVisibleItemIndex > 0 -> 1f
                else -> (feedListState.firstVisibleItemScrollOffset / 340f).coerceIn(0f, 1f)
            }
        }
    }
    val showCollapsedCapsule by remember {
        derivedStateOf {
            feedListState.firstVisibleItemIndex > 0 && uiState.totalSubscriptionCount > 0
        }
    }

    // Comfort-band 25%..75% horizontal auto-scroll whenever the selected filter chip changes
    LaunchedEffect(uiState.selectedFilter) {
        val targetIndex = SubscriptionFilter.entries.indexOf(uiState.selectedFilter)
        val layoutInfo = filterRowState.layoutInfo
        val itemInfo = layoutInfo.visibleItemsInfo.firstOrNull { it.index == targetIndex }
        if (itemInfo != null) {
            val delta = ExpressiveMotion.computeComfortBandScrollDelta(
                itemStartPx = itemInfo.offset,
                itemSizePx = itemInfo.size,
                viewportStartPx = layoutInfo.viewportStartOffset,
                viewportEndPx = layoutInfo.viewportEndOffset
            )
            if (delta != null) {
                filterRowState.animateScrollBy(
                    value = delta.toFloat(),
                    animationSpec = ExpressiveMotion.standardSpatialFastFloat()
                        .orSnapIfReduced(reducedMotion)
                )
            }
        }
    }

    val lookupStatuses = uiState.allCycleStatuses.ifEmpty { uiState.cycleStatuses }
    val checkInItems = remember(lookupStatuses) { buildCycleCheckInItems(lookupStatuses) }
    val showCheckInDeck = !hideCheckInDeck && (
        checkInItems.size >= 2 ||
            checkInItems.any { it is CheckInDeckItem.BankCompletedBreak }
        )

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
                onOpenSubscriptionConsumed()
            }
        }
    }

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

    val filterCounts = remember(lookupStatuses) {
        mapOf(
            SubscriptionFilter.ALL to lookupStatuses.size,
            SubscriptionFilter.ACTIVE to lookupStatuses.count { it.phase == CyclePhase.ACTIVE },
            SubscriptionFilter.BLACKOUT to lookupStatuses.count { it.phase == CyclePhase.BLACKOUT },
            SubscriptionFilter.ROTATING to lookupStatuses.count { it.subscription.isRotating }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    ExpressiveKineticBrandHeader(
                        statusTicker = brandTickerText,
                        fontSize = 28.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
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
        containerColor = Color.Transparent
    ) { innerPadding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            // Subliminal ambient ShapeBloomBackdrop behind empty portfolio states
            if (uiState.cycleStatuses.isEmpty()) {
                ShapeBloomBackdrop()
            }

            LazyColumn(
                state = feedListState,
                modifier = Modifier
                    .fillMaxSize()
                    .progressiveEdgeFade(
                        topFadeHeight = if (showCollapsedCapsule) 28.dp else 0.dp,
                        bottomFadeHeight = 28.dp
                    ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 112.dp + navBarBottom)
            ) {
                // 1. Expressive Hero Bento Card with scroll-parallax watermark
                item(key = "financial_efficiency_hud", contentType = "hud") {
                    FinancialEfficiencyHud(
                        summary = uiState.portfolioSummary,
                        currencyCode = uiState.currencyCode,
                        excludedCurrencyCount = uiState.excludedCurrencyCount,
                        incompleteCount = uiState.incompleteCount,
                        scrollProgress = heroCollapseProgress,
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

                // 2. Inline Cycle Check-In Deck (when finished breaks or 2+ incomplete drafts await action)
                if (showCheckInDeck) {
                    item(key = "cycle_checkin_deck", contentType = "checkin") {
                        CycleCheckInDeck(
                            items = checkInItems,
                            onPrimaryAction = { item ->
                                when (item) {
                                    is CheckInDeckItem.BankCompletedBreak -> {
                                        onAction(DashboardAction.ResumeToday(item.cycleStatus.subscription.id))
                                        pulseBrandTicker("Banked ${item.formattedSavings}")
                                        snackHostState.show(
                                            message = "Banked ${item.formattedSavings} from ${item.serviceName}",
                                            serviceNameForBadge = item.serviceName
                                        )
                                    }
                                    is CheckInDeckItem.FinishIncompleteSetup -> {
                                        subscriptionToEdit = item.cycleStatus.subscription
                                        editInitialStep = item.cycleStatus.subscription.firstMissingSetupStepIndex
                                        showAddDialog = true
                                    }
                                }
                            },
                            onDismissDeck = { hideCheckInDeck = true },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                // 3. Urgent / Due Next Action Banner (Dual-Layer Curtain-Contrast Vessel)
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

                // 4. Comfort-Band Morphing Filter Strip (12dp idle squircle → 22dp selected stadium pill!)
                if (uiState.totalSubscriptionCount >= 2 || uiState.selectedFilter != SubscriptionFilter.ALL) {
                    item(key = "filter_row", contentType = "filters") {
                        LazyRow(
                            state = filterRowState,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(SubscriptionFilter.entries, key = { it.name }) { filter ->
                                val isSelected = filter == uiState.selectedFilter
                                val count = filterCounts[filter] ?: 0
                                val chipInteraction = remember { MutableInteractionSource() }
                                val chipPressed by chipInteraction.collectIsPressedAsState()
                                val chipCorner by animateDpAsState(
                                    targetValue = when {
                                        chipPressed -> 10.dp
                                        isSelected -> 22.dp
                                        else -> 12.dp
                                    },
                                    animationSpec = ExpressiveMotion.standardSpatialFastDp()
                                        .orSnapIfReduced(reducedMotion),
                                    label = "filterChipCorner"
                                )
                                val chipBg by animateColorAsState(
                                    targetValue = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainer
                                    },
                                    animationSpec = ExpressiveMotion.effectsFastColor()
                                        .orSnapIfReduced(reducedMotion),
                                    label = "filterChipBg"
                                )
                                val chipFg by animateColorAsState(
                                    targetValue = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    animationSpec = ExpressiveMotion.effectsFastColor()
                                        .orSnapIfReduced(reducedMotion),
                                    label = "filterChipFg"
                                )

                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onAction(DashboardAction.SetFilter(filter))
                                    },
                                    interactionSource = chipInteraction,
                                    shape = RoundedCornerShape(chipCorner),
                                    color = chipBg,
                                    border = if (!isSelected) expressiveCardBorder() else null
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .height(38.dp)
                                            .padding(start = 10.dp, end = 14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (count > 0 || filter == SubscriptionFilter.ALL) {
                                            Box(
                                                modifier = Modifier
                                                    .height(22.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (isSelected) {
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                                        } else {
                                                            MaterialTheme.colorScheme.surfaceContainerHigh
                                                        }
                                                    )
                                                    .padding(horizontal = 6.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "$count",
                                                    fontFamily = DisplayFontFamily,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 11.sp,
                                                    color = chipFg
                                                )
                                            }
                                        }
                                        Text(
                                            text = filter.displayName,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.5.sp,
                                            color = chipFg
                                        )
                                    }
                                }
                            }

                            // Compact Sort Pill (matching 38dp height & corner compression)
                            item(key = "sort_pill") {
                                val nextSort = SubscriptionSort.entries[
                                    (uiState.selectedSort.ordinal + 1) % SubscriptionSort.entries.size
                                ]
                                val sortInteraction = remember { MutableInteractionSource() }
                                val sortPressed by sortInteraction.collectIsPressedAsState()
                                val sortCorner by animateDpAsState(
                                    targetValue = if (sortPressed) 10.dp else 18.dp,
                                    animationSpec = ExpressiveMotion.standardSpatialFastDp()
                                        .orSnapIfReduced(reducedMotion),
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
                                            .height(38.dp)
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

                    // Fanned miniature badge preview when filtering Paused or Rotating views
                    if ((uiState.selectedFilter == SubscriptionFilter.BLACKOUT ||
                            uiState.selectedFilter == SubscriptionFilter.ROTATING) &&
                        uiState.cycleStatuses.size >= 2
                    ) {
                        item(key = "filter_fanned_summary", contentType = "filter_summary") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FannedBadgeStack(
                                    serviceNames = uiState.cycleStatuses.map { it.subscription.name },
                                    maxVisible = 4,
                                    badgeSize = 28.dp,
                                    step = 18.dp,
                                    ringWidth = 2.dp
                                )
                                Text(
                                    text = "${uiState.cycleStatuses.size} ${uiState.selectedFilter.displayName.lowercase()} services",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // 5. Expressive 2-Column Bento Subscription Deck OR Rich Empty State
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
                                            snackHostState.show(
                                                message = "Added ${preset.name}",
                                                serviceNameForBadge = preset.name
                                            )
                                            pulseBrandTicker("Added ${preset.name}")
                                        } else {
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
                    itemsIndexed(
                        items = rows,
                        key = { _, row -> row.joinToString("-") { it.subscription.id.toString() } },
                        contentType = { _, _ -> "bento_row" }
                    ) { _, rowItems ->
                        val rowIsFullyRemoving = rowItems.all { it.subscription.id in pendingRemovalIds }
                        ExpressiveRemovalCollapse(
                            removing = rowIsFullyRemoving,
                            onRemoved = {
                                rowItems.forEach { status ->
                                    if (status.subscription.id in pendingRemovalIds) {
                                        pendingRemovalIds.remove(status.subscription.id)
                                        onAction(DashboardAction.DeleteSubscription(status.subscription))
                                    }
                                }
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(IntrinsicSize.Min)
                                    .animateItem(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                for (cycleStatus in rowItems) {
                                    val isCardRemoving = !rowIsFullyRemoving && cycleStatus.subscription.id in pendingRemovalIds
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                    ) {
                                        ExpressiveRemovalCollapse(
                                            removing = isCardRemoving,
                                            onRemoved = {
                                                if (cycleStatus.subscription.id in pendingRemovalIds) {
                                                    pendingRemovalIds.remove(cycleStatus.subscription.id)
                                                    onAction(DashboardAction.DeleteSubscription(cycleStatus.subscription))
                                                }
                                            },
                                            modifier = Modifier.fillMaxHeight()
                                        ) {
                                            SubscriptionGridItem(
                                                cycleStatus = cycleStatus,
                                                onClick = {
                                                    selectedSubscriptionId = cycleStatus.subscription.id
                                                },
                                                onBankBreakClick = {
                                                    val saved = MoneyFormatter.format(
                                                        cycleStatus.perBreakSavings,
                                                        cycleStatus.subscription.currency,
                                                        maximumFractionDigits = 0
                                                    )
                                                    onAction(DashboardAction.ResumeToday(cycleStatus.subscription.id))
                                                    pulseBrandTicker("Banked $saved")
                                                    snackHostState.show(
                                                        message = "Banked $saved from ${cycleStatus.subscription.name}",
                                                        serviceNameForBadge = cycleStatus.subscription.name
                                                    )
                                                },
                                                modifier = Modifier.fillMaxHeight()
                                            )
                                        }
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

            // Pinned Scroll-Collapsed Mini-Ledger Capsule (springs in once Hero scrolls off-screen!)
            AnimatedVisibility(
                visible = showCollapsedCapsule,
                enter = slideInVertically(
                    animationSpec = ExpressiveMotion.expressiveSpatialDefaultIntOffset()
                        .orSnapIfReduced(reducedMotion)
                ) { -it } + fadeIn(),
                exit = slideOutVertically(
                    animationSpec = ExpressiveMotion.standardSpatialFast()
                ) { -it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                CompactEfficiencyCapsule(
                    summary = uiState.portfolioSummary,
                    currencyCode = uiState.currencyCode,
                    onSavingsClick = onNavigateToSavings
                )
            }

            // 3D Stacked Floating Stadium Snack Host hovering above the Split Floating Toolbar
            ExpressiveSnackHost(
                hostState = snackHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 86.dp)
            )
        }
    }

    // Detail Bottom Sheet (Cycle Time-Machine Wave Scrubber + Asymmetric 1+(2x2) Command Deck)
    lookupStatuses.find { it.subscription.id == selectedSubscriptionId }?.let { status ->
        SubscriptionDetailBottomSheet(
            cycleStatus = status,
            onDismiss = { selectedSubscriptionId = null },
            onToggleAutoRenew = {
                onAction(DashboardAction.ToggleAutoRenew(status.subscription.id))
                val nextMsg = if (status.isAutoRenewCancelledForCurrentCycle) {
                    "Cancel reminders re-enabled"
                } else {
                    "Auto-renew marked cancelled"
                }
                snackHostState.show(
                    message = nextMsg,
                    serviceNameForBadge = status.subscription.name,
                    dedupeKey = "autorenew_${status.subscription.id}"
                )
            },
            onStartSubscriptionAction = { actionType ->
                openBillingLinksDialog(status.subscription.id, status.subscription.name, actionType)
            },
            onExtendCycle = {
                onAction(DashboardAction.ExtendActiveCycle(status.subscription.id))
                snackHostState.show(
                    message = "Skipped break (+${status.subscription.activeDays}d)",
                    serviceNameForBadge = status.subscription.name
                )
            },
            onResumeToday = {
                onAction(DashboardAction.ResumeToday(status.subscription.id))
                pulseBrandTicker("Cycle restarted")
                snackHostState.show(
                    message = "${status.subscription.name} resumed today",
                    serviceNameForBadge = status.subscription.name
                )
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
                if (deleted.id !in pendingRemovalIds) {
                    pendingRemovalIds.add(deleted.id)
                }
                snackHostState.show(
                    message = "Deleted ${deleted.name}",
                    actionLabel = "Undo",
                    serviceNameForBadge = deleted.name,
                    icon = Icons.Default.DeleteOutline,
                    durationMillis = 4500L,
                    dedupeKey = "delete_${deleted.id}",
                    onAction = {
                        if (deleted.id in pendingRemovalIds) {
                            pendingRemovalIds.remove(deleted.id)
                        } else {
                            onAction(DashboardAction.AddSubscription(deleted.copy(id = 0L)))
                        }
                        pulseBrandTicker("Restored ${deleted.name}")
                    }
                )
            }
        )
    }

    // Provider Billing Links Dialog
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
                pulseBrandTicker("Updated cycle")
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
            onAutoSave = { sub ->
                val savedId = onAutoSaveSubscription(sub)
                pulseBrandTicker("Saved locally")
                savedId
            }
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
            onCurrencySelected = {
                onAction(DashboardAction.SetDefaultCurrency(it))
                snackHostState.show(
                    message = "Switched app currency to $it",
                    icon = Icons.Default.Check,
                    dedupeKey = "currency_$it"
                )
            },
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
            onShareStack = {
                showSettings = false
                showShareSheet = true
            },
            onImportStack = {
                showSettings = false
                showImportSheet = true
            },
            backupMessage = uiState.backupFeedback
        )
    }

    if (showShareSheet) {
        val currentSubscriptions = remember(lookupStatuses) {
            lookupStatuses.map { it.subscription }
        }
        ShareStackSheet(
            subscriptions = currentSubscriptions,
            onShareTransferLink = { includeHistory ->
                showShareSheet = false
                onShareTransferLink(includeHistory)
            },
            onCopyTransferLink = { includeHistory ->
                showShareSheet = false
                onCopyTransferLink(includeHistory)
            },
            onShareBackupFile = { includeHistory ->
                showShareSheet = false
                onShareBackupFile(includeHistory)
            },
            onSaveBackupFile = { includeHistory ->
                showShareSheet = false
                onSaveBackupFile(includeHistory)
            },
            onDismiss = { showShareSheet = false }
        )
    }

    if (showImportSheet) {
        ImportStackSheet(
            onInspectTransferText = { raw ->
                showImportSheet = false
                onAction(DashboardAction.InspectIncomingTransfer(raw))
            },
            onPickBackupFile = {
                showImportSheet = false
                onPickBackupFile()
            },
            onDismiss = { showImportSheet = false }
        )
    }

    uiState.pendingImportSubscriptions?.let { incoming ->
        val existing = remember(lookupStatuses) { lookupStatuses.map { it.subscription } }
        val previewItems = remember(incoming, existing) {
            buildImportPreviewItems(incoming = incoming, existing = existing)
        }
        ImportPreviewSheet(
            items = previewItems,
            onConfirmImport = {
                onAction(DashboardAction.ConfirmPendingImport)
                pulseBrandTicker("Stack imported")
            },
            onDismiss = { onAction(DashboardAction.DismissPendingImport) }
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
                    .expressivePolygonSurface(
                        shape = ExpressiveCookie8Shape,
                        color = boldPalette.container
                    ),
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
