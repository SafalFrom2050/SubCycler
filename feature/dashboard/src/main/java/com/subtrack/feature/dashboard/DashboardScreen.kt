package com.subtrack.feature.dashboard

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import com.subtrack.core.domain.model.AppLauncherIcon
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
import com.subtrack.core.ui.components.ReactiveIcon
import com.subtrack.core.ui.components.ReactiveMotionType
import com.subtrack.core.ui.components.ShapeBloomBackdrop
import com.subtrack.core.ui.components.ShareStackSheet
import com.subtrack.core.ui.components.SubscriptionDetailBottomSheet
import com.subtrack.core.ui.components.SubscriptionGridItem
import com.subtrack.core.ui.components.buildCycleCheckInItems
import com.subtrack.core.ui.components.buildImportPreviewItems
import com.subtrack.core.ui.components.decodeQrFromBitmap
import com.subtrack.core.ui.components.expressiveCardBorder
import com.subtrack.core.ui.components.expressivePolygonSurface
import com.subtrack.core.ui.components.openCancellationDestination
import com.subtrack.core.ui.components.progressiveEdgeFade
import com.subtrack.core.ui.components.rememberExpressiveSnackHostState
import com.subtrack.core.ui.components.renderStackShareCardBitmap
import com.subtrack.core.ui.theme.DisplayFontFamily
import com.subtrack.core.ui.theme.ExpressiveMotion
import com.subtrack.core.ui.theme.LocalSubCyclerDarkTheme
import com.subtrack.core.ui.theme.orSnapIfReduced
import com.subtrack.core.ui.theme.rememberReducedMotionEnabled
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
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
    pendingOpenSettings: Boolean = false,
    onOpenSettingsConsumed: () -> Unit = {},
    onNavigateToSavings: () -> Unit = {},
    onApplyLauncherIcon: (AppLauncherIcon) -> Unit = {},
    onPreviewNotifications: () -> Unit = {},
    isEntranceReady: Boolean = true,
    onFirstDataReady: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var pendingSaveIncludeHistory by remember { mutableStateOf(true) }

    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading) {
            onFirstDataReady()
        }
    }

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

    val pickCardImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val decoded = withContext(Dispatchers.Default) {
                runCatching {
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                    val maxDim = maxOf(bounds.outWidth, bounds.outHeight).coerceAtLeast(1)
                    var sample = 1
                    while (maxDim / sample > 1600) sample *= 2
                    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
                    val bmp = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
                        ?: return@runCatching null
                    try {
                        decodeQrFromBitmap(bmp)
                    } finally {
                        bmp.recycle()
                    }
                }.getOrNull()
            }
            if (!decoded.isNullOrBlank()) {
                viewModel.onAction(DashboardAction.InspectIncomingTransfer(decoded))
            } else {
                viewModel.onAction(
                    DashboardAction.SetBackupFeedback("Couldn't find a SubCycler QR code in that image.")
                )
            }
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
        onShareStackCard = { includeHistory ->
            scope.launch {
                runCatching {
                    val subs = viewModel.getSubscriptionsSnapshot()
                    val link = viewModel.buildTransferLink(includeHistory)
                    val caption = viewModel.buildHumanShareCaption(
                        includePersonalHistory = includeHistory,
                        footerHint = "Scan the QR or share this card into SubCycler to import."
                    )
                    val uri = withContext(Dispatchers.Default) {
                        val cardBitmap = renderStackShareCardBitmap(
                            context = context,
                            subscriptions = subs,
                            transferLink = link,
                            includePersonalHistory = includeHistory,
                            useStackMood = uiState.useStackHeroMood
                        )
                        try {
                            val cacheDir = File(context.cacheDir, "shared_backups").apply {
                                mkdirs()
                                listFiles()?.forEach { it.delete() }
                            }
                            val file = File(cacheDir, "subcycler-stack-card-${LocalDate.now()}.png")
                            file.outputStream().use { out ->
                                cardBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                            }
                            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        } finally {
                            cardBitmap.recycle()
                        }
                    }
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "image/png"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        putExtra(Intent.EXTRA_SUBJECT, "My SubCycler Stack")
                        putExtra(Intent.EXTRA_TEXT, caption)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share SubCycler Stack Card"))
                }.onFailure {
                    viewModel.onAction(DashboardAction.SetBackupFeedback("Couldn't create Stack Card image."))
                }
            }
        },
        onShareTransferLink = { includeHistory ->
            scope.launch {
                runCatching {
                    val shareMessage = viewModel.buildShareMessage(includeHistory)
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "SubCycler stack")
                        putExtra(Intent.EXTRA_TEXT, shareMessage)
                    }
                    context.startActivity(Intent.createChooser(sendIntent, "Share SubCycler link"))
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
                    onSuccess = { viewModel.onAction(DashboardAction.SetBackupFeedback("Short transfer link copied.")) },
                    onFailure = { viewModel.onAction(DashboardAction.SetBackupFeedback("Couldn't copy transfer link.")) }
                )
            }
        },
        onShareBackupFile = { includeHistory ->
            scope.launch {
                runCatching {
                    val payload = viewModel.buildBackupFilePayload(includeHistory)
                    val cleanCaption = viewModel.buildHumanShareCaption(
                        includePersonalHistory = includeHistory,
                        footerHint = "Tap the attached .subcycler file to open in SubCycler."
                    )
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
                        putExtra(Intent.EXTRA_TEXT, cleanCaption)
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
        onPickCardImage = { pickCardImageLauncher.launch("image/*") },
        onPickBackupFile = {
            openBackupFileLauncher.launch(arrayOf("application/octet-stream", "text/plain", "*/*"))
        },
        onNavigateToSavings = onNavigateToSavings,
        openSubscriptionId = openSubscriptionId,
        onOpenSubscriptionConsumed = onOpenSubscriptionConsumed,
        pendingAddSubscription = pendingAddSubscription,
        onAddSubscriptionConsumed = onAddSubscriptionConsumed,
        pendingOpenSettings = pendingOpenSettings,
        onOpenSettingsConsumed = onOpenSettingsConsumed,
        onApplyLauncherIcon = onApplyLauncherIcon,
        onPreviewNotifications = onPreviewNotifications,
        isEntranceReady = isEntranceReady,
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
    onShareStackCard: (includePersonalHistory: Boolean) -> Unit = {},
    onShareTransferLink: (includePersonalHistory: Boolean) -> Unit = {},
    onCopyTransferLink: (includePersonalHistory: Boolean) -> Unit = {},
    onShareBackupFile: (includePersonalHistory: Boolean) -> Unit = {},
    onSaveBackupFile: (includePersonalHistory: Boolean) -> Unit = {},
    onPickCardImage: () -> Unit = {},
    onPickBackupFile: () -> Unit = {},
    onNavigateToSavings: () -> Unit = {},
    openSubscriptionId: Long? = null,
    onOpenSubscriptionConsumed: () -> Unit = {},
    pendingAddSubscription: Boolean = false,
    onAddSubscriptionConsumed: () -> Unit = {},
    pendingOpenSettings: Boolean = false,
    onOpenSettingsConsumed: () -> Unit = {},
    onApplyLauncherIcon: (AppLauncherIcon) -> Unit = {},
    onPreviewNotifications: () -> Unit = {},
    isEntranceReady: Boolean = true,
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

    LaunchedEffect(pendingOpenSettings) {
        if (pendingOpenSettings) {
            onAction(DashboardAction.SetBackupFeedback(null))
            showSettings = true
            onOpenSettingsConsumed()
        }
    }
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
    var savedDuringEditSession by remember { mutableStateOf(false) }
    var hideCheckInDeck by rememberSaveable { mutableStateOf(false) }
    val pendingRemovalIds = remember { mutableStateListOf<Long>() }

    fun pulseBrandTicker(message: String) {
        brandTickerText = message
        brandTickerNonce++
    }

    LaunchedEffect(brandTickerNonce) {
        if (brandTickerText == null) return@LaunchedEffect
        delay(2000L)
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

    val filters = SubscriptionFilter.entries
    val showFilterBar = uiState.totalSubscriptionCount >= 2 || uiState.selectedFilter != SubscriptionFilter.ALL
    val filterPagerState = rememberPagerState(
        initialPage = uiState.selectedFilter.ordinal,
        pageCount = { filters.size }
    )
    val latestSelectedFilter by rememberUpdatedState(uiState.selectedFilter)

    // Sync horizontal swipe page settlement -> ViewModel filter + subtle haptic tick
    LaunchedEffect(filterPagerState) {
        snapshotFlow { filterPagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                val swipedFilter = filters[page]
                if (swipedFilter != latestSelectedFilter) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onAction(DashboardAction.SetFilter(swipedFilter))
                }
            }
    }

    // Sync external filter resets (e.g., "Show all subscriptions") -> HorizontalPager smoothly
    LaunchedEffect(uiState.selectedFilter) {
        val targetPage = uiState.selectedFilter.ordinal
        if (filterPagerState.currentPage != targetPage && !filterPagerState.isScrollInProgress) {
            if (reducedMotion) {
                filterPagerState.scrollToPage(targetPage)
            } else {
                filterPagerState.animateScrollToPage(
                    page = targetPage,
                    animationSpec = ExpressiveMotion.standardSpatialDefaultFloat()
                )
            }
        }
    }

    // Comfort-band 25%..75% horizontal auto-scroll whenever the active filter chip changes (tap or swipe)
    LaunchedEffect(filterPagerState.currentPage) {
        val targetIndex = filterPagerState.currentPage
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

    val activeSubscriptions = remember(lookupStatuses) {
        lookupStatuses
            .filter { it.phase == CyclePhase.ACTIVE }
            .map { it.subscription }
    }
    val swirlColors = remember(activeSubscriptions, uiState.useStackHeroMood, uiState.launcherIcon) {
        com.subtrack.core.ui.theme.BrandColorHarmonizer.resolveSwirlPetalColors(
            activeSubscriptions = activeSubscriptions,
            useStackMood = uiState.useStackHeroMood,
            fallbackIcon = uiState.launcherIcon
        )
    }
    val heroMood = remember(activeSubscriptions, uiState.useStackHeroMood, uiState.launcherIcon, isDark) {
        com.subtrack.core.ui.theme.BrandColorHarmonizer.resolveHeroMoodPalette(
            activeSubscriptions = activeSubscriptions,
            useStackMood = uiState.useStackHeroMood,
            isDark = isDark,
            fallbackIcon = uiState.launcherIcon
        )
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
                        swirlColors = swirlColors,
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
        BoxWithConstraints(
            modifier = modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            // Fill the horizontal swipe target down to the top of the floating bottom nav dock
            // even when only 1 row of Bento cards (or an empty filter tab) is displayed.
            val pagerMinHeight = (maxHeight - 274.dp - 84.dp - navBarBottom).coerceAtLeast(360.dp)

            // Subliminal ambient ShapeBloomBackdrop behind empty portfolio states (never during initial load)
            if (!uiState.isLoading && uiState.cycleStatuses.isEmpty()) {
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
                        heroMood = heroMood,
                        isEntranceReady = isEntranceReady,
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

                // 4. Comfort-Band Morphing Filter Strip (synchronized live with HorizontalPager swipes!)
                if (showFilterBar) {
                    item(key = "filter_row", contentType = "filters") {
                        val visualSelectedFilter = filters[filterPagerState.currentPage]
                        val skipChipEntrance = isEntranceReady || reducedMotion
                        val chipStripReveal = remember {
                            androidx.compose.animation.core.Animatable(if (skipChipEntrance) 1f else 0f)
                        }
                        LaunchedEffect(isEntranceReady, reducedMotion) {
                            if (!isEntranceReady) return@LaunchedEffect
                            if (reducedMotion) {
                                chipStripReveal.snapTo(1f)
                            } else if (chipStripReveal.value < 0.99f) {
                                kotlinx.coroutines.delay(50L)
                                chipStripReveal.animateTo(1f, ExpressiveMotion.expressiveSpatialDefaultFloat())
                            }
                        }
                        LazyRow(
                            state = filterRowState,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    val r = chipStripReveal.value
                                    alpha = ExpressiveMotion.clampEffectProgress(r * 1.45f)
                                    translationY = (1f - r) * 10.dp.toPx()
                                }
                        ) {
                            items(filters, key = { it.name }) { filter ->
                                val isSelected = filter == visualSelectedFilter
                                val count = filterCounts[filter] ?: 0
                                val chipInteraction = remember { MutableInteractionSource() }
                                val chipPressed by chipInteraction.collectIsPressedAsState()
                                var chipAnimNonce by remember { mutableIntStateOf(0) }
                                LaunchedEffect(isSelected, isEntranceReady) {
                                    if (isSelected && isEntranceReady) chipAnimNonce++
                                }
                                val filterIcon = when (filter) {
                                    SubscriptionFilter.ALL -> if (isSelected) Icons.Rounded.GridView else Icons.Outlined.GridView
                                    SubscriptionFilter.ACTIVE -> Icons.Rounded.PlayArrow
                                    SubscriptionFilter.BLACKOUT -> Icons.Rounded.Pause
                                    SubscriptionFilter.ROTATING -> Icons.Rounded.Autorenew
                                }
                                val filterMotion = when (filter) {
                                    SubscriptionFilter.ALL -> ReactiveMotionType.POP_BOUNCE
                                    SubscriptionFilter.ACTIVE -> ReactiveMotionType.NUDGE_RIGHT
                                    SubscriptionFilter.BLACKOUT -> ReactiveMotionType.PAUSE_SETTLE
                                    SubscriptionFilter.ROTATING -> ReactiveMotionType.CYCLE_SPIN
                                }
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
                                val chipAccentPop = heroMood.sealColor
                                val chipOnPop = heroMood.onSealColor
                                val chipBg by animateColorAsState(
                                    targetValue = when {
                                        isSelected && isDark -> chipAccentPop
                                        isSelected -> MaterialTheme.colorScheme.onSurface
                                        else -> MaterialTheme.colorScheme.surfaceContainer
                                    },
                                    animationSpec = ExpressiveMotion.effectsFastColor()
                                        .orSnapIfReduced(reducedMotion),
                                    label = "filterChipBg"
                                )
                                val chipFg by animateColorAsState(
                                    targetValue = when {
                                        isSelected && isDark -> chipOnPop
                                        isSelected -> chipAccentPop
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    animationSpec = ExpressiveMotion.effectsFastColor()
                                        .orSnapIfReduced(reducedMotion),
                                    label = "filterChipFg"
                                )

                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        if (isSelected) chipAnimNonce++
                                        onAction(DashboardAction.SetFilter(filter))
                                        scope.launch {
                                            if (reducedMotion) {
                                                filterPagerState.scrollToPage(filter.ordinal)
                                            } else {
                                                filterPagerState.animateScrollToPage(
                                                    page = filter.ordinal,
                                                    animationSpec = ExpressiveMotion.standardSpatialDefaultFloat()
                                                )
                                            }
                                        }
                                    },
                                    interactionSource = chipInteraction,
                                    shape = RoundedCornerShape(chipCorner),
                                    color = chipBg,
                                    border = if (!isSelected) expressiveCardBorder() else null
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .height(38.dp)
                                            .padding(start = 11.dp, end = if (count > 0 || filter == SubscriptionFilter.ALL) 9.dp else 13.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        ReactiveIcon(
                                            imageVector = filterIcon,
                                            contentDescription = null,
                                            tint = chipFg,
                                            size = 15.dp,
                                            motion = filterMotion,
                                            isPressed = chipPressed,
                                            triggerKey = chipAnimNonce
                                        )
                                        Text(
                                            text = filter.displayName,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 12.5.sp,
                                            color = chipFg
                                        )
                                        if (count > 0 || filter == SubscriptionFilter.ALL) {
                                            Box(
                                                modifier = Modifier
                                                    .height(20.dp)
                                                    .clip(RoundedCornerShape(999.dp))
                                                    .background(
                                                        if (isSelected) {
                                                            chipFg.copy(alpha = 0.16f)
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
                                                    fontSize = 10.5.sp,
                                                    color = chipFg
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Compact Sort Pill (matching 38dp height, corner compression, and 180° flip icon)
                            item(key = "sort_pill") {
                                val nextSort = SubscriptionSort.entries[
                                    (uiState.selectedSort.ordinal + 1) % SubscriptionSort.entries.size
                                ]
                                val sortInteraction = remember { MutableInteractionSource() }
                                val sortPressed by sortInteraction.collectIsPressedAsState()
                                var sortTapCount by remember { mutableIntStateOf(0) }
                                val sortCorner by animateDpAsState(
                                    targetValue = if (sortPressed) 10.dp else 18.dp,
                                    animationSpec = ExpressiveMotion.standardSpatialFastDp()
                                        .orSnapIfReduced(reducedMotion),
                                    label = "sortChipCorner"
                                )
                                Surface(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        sortTapCount++
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
                                        ReactiveIcon(
                                            imageVector = Icons.Default.SwapVert,
                                            contentDescription = "Change sort order",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            size = 15.dp,
                                            motion = ReactiveMotionType.SWAP_FLIP,
                                            isPressed = sortPressed,
                                            triggerKey = sortTapCount
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

                // 5. Slide-to-Switch HorizontalPager across All · Active · Paused · Rotating
                item(key = "subscription_filter_pager", contentType = "filter_pager") {
                    HorizontalPager(
                        state = filterPagerState,
                        userScrollEnabled = showFilterBar,
                        pageSpacing = 12.dp,
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = pagerMinHeight)
                    ) { pageIndex ->
                        val pageFilter = filters[pageIndex]
                        val pageStatuses = remember(lookupStatuses, pageFilter, uiState.selectedSort) {
                            filterAndSortCycleStatuses(lookupStatuses, pageFilter, uiState.selectedSort)
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .defaultMinSize(minHeight = pagerMinHeight),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if ((pageFilter == SubscriptionFilter.BLACKOUT ||
                                    pageFilter == SubscriptionFilter.ROTATING) &&
                                pageStatuses.size >= 2
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    FannedBadgeStack(
                                        serviceNames = pageStatuses.map { it.subscription.name },
                                        serviceColors = pageStatuses.map { it.subscription.colorHex },
                                        maxVisible = 4,
                                        badgeSize = 28.dp,
                                        step = 18.dp,
                                        ringWidth = 2.dp
                                    )
                                    Text(
                                        text = "${pageStatuses.size} ${pageFilter.displayName.lowercase()} services",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            if (pageStatuses.isEmpty()) {
                                if (!uiState.isLoading) {
                                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                                        DashboardEmptyState(
                                        isFilteredEmpty = uiState.totalSubscriptionCount > 0,
                                        activeFilter = pageFilter,
                                        currencyCode = uiState.preferredCurrencyCode,
                                        onClearFilter = {
                                            onAction(DashboardAction.SetFilter(SubscriptionFilter.ALL))
                                            scope.launch {
                                                if (reducedMotion) {
                                                    filterPagerState.scrollToPage(0)
                                                } else {
                                                    filterPagerState.animateScrollToPage(
                                                        page = 0,
                                                        animationSpec = ExpressiveMotion.standardSpatialDefaultFloat()
                                                    )
                                                }
                                            }
                                        },
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
                                val rows = pageStatuses.chunked(2)
                                rows.forEachIndexed { rowIdx, rowItems ->
                                    key(rowItems.joinToString("-") { it.subscription.id.toString() }) {
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
                                                    .height(IntrinsicSize.Min),
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                rowItems.forEachIndexed { colIdx, cycleStatus ->
                                                    val isCardRemoving = !rowIsFullyRemoving && cycleStatus.subscription.id in pendingRemovalIds
                                                    val gridIndex = if (pageIndex == 0) rowIdx * 2 + colIdx else -1
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
                                                                isEntranceReady = isEntranceReady,
                                                                entranceIndex = gridIndex,
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
                val hadSaves = savedDuringEditSession
                savedDuringEditSession = false
                showAddDialog = false
                subscriptionToEdit = null
                editInitialStep = 0
                presetToAdd = null
                if (hadSaves) {
                    pulseBrandTicker("Saved locally")
                }
            },
            onAutoSave = { sub ->
                val savedId = onAutoSaveSubscription(sub)
                savedDuringEditSession = true
                pulseBrandTicker("Saved locally")
                savedId
            }
        )
    }

    if (showSettings) {
        val recommendedIcon = remember(activeSubscriptions) {
            if (activeSubscriptions.isNotEmpty()) {
                com.subtrack.core.ui.theme.BrandColorHarmonizer.recommendedLauncherIcon(activeSubscriptions)
            } else {
                null
            }
        }
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
            useStackHeroMood = uiState.useStackHeroMood,
            selectedLauncherIcon = uiState.launcherIcon,
            recommendedLauncherIcon = recommendedIcon,
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
            onUseStackHeroMoodChanged = { enabled ->
                onAction(DashboardAction.SetUseStackHeroMood(enabled))
            },
            onLauncherIconSelected = { icon ->
                onAction(DashboardAction.SetLauncherIcon(icon))
                onApplyLauncherIcon(icon)
                snackHostState.show(
                    message = "App icon set to ${icon.displayName}",
                    icon = Icons.Default.Check,
                    dedupeKey = "launcher_icon_${icon.name}"
                )
            },
            onOpenNotificationSettings = {
                runCatching {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            },
            onPreviewNotifications = {
                onPreviewNotifications()
                snackHostState.show(
                    message = "Sample alerts sent — pull down your notification shade",
                    icon = Icons.Default.Check,
                    dedupeKey = "preview_notif"
                )
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
            useStackHeroMood = uiState.useStackHeroMood,
            onShareStackCard = { includeHistory ->
                showShareSheet = false
                onShareStackCard(includeHistory)
            },
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
            onPickCardImage = {
                showImportSheet = false
                onPickCardImage()
            },
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
    activeFilter: SubscriptionFilter = SubscriptionFilter.ALL,
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
                text = if (isFilteredEmpty) {
                    when (activeFilter) {
                        SubscriptionFilter.ACTIVE -> "No active subscriptions"
                        SubscriptionFilter.BLACKOUT -> "No paused subscriptions"
                        SubscriptionFilter.ROTATING -> "No rotating subscriptions"
                        SubscriptionFilter.ALL -> "No services match this filter"
                    }
                } else {
                    "Your stack is empty"
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            if (!isFilteredEmpty) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Pick a service or add your own.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (isFilteredEmpty) {
                FilledTonalButton(
                    onClick = onClearFilter,
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text("Show all", fontWeight = FontWeight.Bold)
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
