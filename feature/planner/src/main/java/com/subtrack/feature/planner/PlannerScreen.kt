package com.subtrack.feature.planner

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subtrack.core.common.currency.MoneyFormatter
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.ui.components.CycleTimelineView
import com.subtrack.core.ui.components.ExpressiveEyebrow
import com.subtrack.core.ui.components.ExpressiveMaterialPolygons
import com.subtrack.core.ui.components.ExpressivePalettes
import com.subtrack.core.ui.components.ExpressiveReactiveIconButton
import com.subtrack.core.ui.components.ExpressiveScreenTitle
import com.subtrack.core.ui.components.ExpressiveSegmentOption
import com.subtrack.core.ui.components.ExpressiveSegmentedGroup
import com.subtrack.core.ui.components.ExpressiveServiceBadge
import com.subtrack.core.ui.components.ReactiveIcon
import com.subtrack.core.ui.components.ReactiveMotionType
import com.subtrack.core.ui.components.ShapeBloomBackdrop
import com.subtrack.core.ui.components.diagonalHatchPattern
import com.subtrack.core.ui.components.expressiveCardBorder
import com.subtrack.core.ui.components.expressivePolygonSurface
import com.subtrack.core.ui.components.progressiveEdgeFade
import com.subtrack.core.ui.components.rememberMorphingPolygonShape
import com.subtrack.core.ui.theme.DisplayFontFamily
import com.subtrack.core.ui.theme.ExpressiveMotion
import com.subtrack.core.ui.theme.LocalSubCyclerDarkTheme
import com.subtrack.core.ui.theme.orSnapIfReduced
import com.subtrack.core.ui.theme.rememberReducedMotionEnabled
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun PlannerRoute(
    viewModel: PlannerViewModel = hiltViewModel(),
    pendingOpenPairedSwap: Boolean = false,
    onOpenPairedSwapConsumed: () -> Unit = {},
    onOpenSubscription: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(pendingOpenPairedSwap) {
        if (pendingOpenPairedSwap) {
            viewModel.onAction(PlannerAction.SelectTab(PlannerTab.PAIRED_SWAP))
            onOpenPairedSwapConsumed()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onAction(PlannerAction.RefreshToday)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    PlannerScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        onOpenSubscription = onOpenSubscription,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlannerScreen(
    uiState: PlannerUiState,
    onAction: (PlannerAction) -> Unit,
    onOpenSubscription: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isDark = LocalSubCyclerDarkTheme.current
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d, yyyy") }
    var showSuccessSnackbar by remember(uiState.service1?.id, uiState.service2?.id) { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val cardBorder = expressiveCardBorder()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    ExpressiveScreenTitle(text = "Planner")
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .padding(horizontal = 16.dp)
        ) {
            // Connected Material 3 Expressive Segmented Mode Group
            val plannerTabOptions = remember {
                PlannerTab.entries.map { ExpressiveSegmentOption(it, it.displayName) }
            }
            ExpressiveSegmentedGroup(
                options = plannerTabOptions,
                selectedValue = uiState.selectedTab,
                onSelect = { tab -> onAction(PlannerAction.SelectTab(tab)) },
                height = 42.dp,
                fontSize = 12.5.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedContent(
                targetState = uiState.selectedTab,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    fadeIn(tween(180)).togetherWith(fadeOut(tween(120)))
                },
                label = "plannerModeTransition"
            ) { tab ->
                Box(modifier = Modifier.fillMaxSize()) {
                    if (tab == PlannerTab.TIMELINE && uiState.cycleStatuses.isEmpty()) {
                        ShapeBloomBackdrop()
                    }
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .progressiveEdgeFade(bottomFadeHeight = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(bottom = 112.dp + navBarBottom)
                    ) {
                    if (tab == PlannerTab.TIMELINE) {
                        item(key = "date_inspector_card", contentType = "inspector") {
                            val skyPalette = ExpressivePalettes.softSky()
                            val lavenderPalette = ExpressivePalettes.softLavender()
                            val isViewingToday = uiState.selectedTargetDate == uiState.today

                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(26.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                border = cardBorder
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ExpressiveEyebrow(text = "DATE INSPECTOR")
                                        if (!isViewingToday) {
                                            Surface(
                                                onClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    onAction(PlannerAction.ResetToToday)
                                                },
                                                shape = RoundedCornerShape(999.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    text = "Today",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        ExpressiveReactiveIconButton(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                            contentDescription = "Previous week",
                                            onClick = { onAction(PlannerAction.StepDate(-7)) },
                                            shape = CircleShape,
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            iconTint = MaterialTheme.colorScheme.onSurface,
                                            buttonSize = 44.dp,
                                            iconSize = 21.dp,
                                            motion = ReactiveMotionType.NUDGE_LEFT
                                        )

                                        // Tappable date pill opens Calendar DatePicker!
                                        Surface(
                                            onClick = { showDatePicker = true },
                                            shape = RoundedCornerShape(999.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .heightIn(min = 42.dp)
                                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CalendarMonth,
                                                    contentDescription = "Pick any date",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                AnimatedContent(
                                                    targetState = uiState.selectedTargetDate,
                                                    transitionSpec = {
                                                        val dir = if (targetState.isAfter(initialState)) 1 else -1
                                                        (slideInHorizontally(tween(180)) { dir * (it / 3) } + fadeIn(tween(160)))
                                                            .togetherWith(slideOutHorizontally(tween(130)) { -dir * (it / 3) } + fadeOut(tween(110)))
                                                    },
                                                    label = "inspectorDateSlide"
                                                ) { inspectedDate ->
                                                    Text(
                                                        text = inspectedDate.format(dateFormatter),
                                                        fontFamily = DisplayFontFamily,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 14.5.sp,
                                                        letterSpacing = (-0.2).sp,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                            }
                                        }

                                        ExpressiveReactiveIconButton(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = "Next week",
                                            onClick = { onAction(PlannerAction.StepDate(7)) },
                                            shape = CircleShape,
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            iconTint = MaterialTheme.colorScheme.onSurface,
                                            buttonSize = 44.dp,
                                            iconSize = 21.dp,
                                            motion = ReactiveMotionType.NUDGE_RIGHT
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(IntrinsicSize.Min),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight(),
                                            shape = RoundedCornerShape(18.dp),
                                            color = skyPalette.container
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = "Active (${uiState.activeOnSelectedDate.size})",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 11.5.sp,
                                                    color = skyPalette.content
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                if (uiState.activeOnSelectedDate.isEmpty()) {
                                                    Text("None", fontSize = 12.sp, color = skyPalette.content.copy(alpha = 0.7f))
                                                } else {
                                                    FlowRow(
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        uiState.activeOnSelectedDate.forEach { name ->
                                                            InspectorServiceChip(
                                                                serviceName = name,
                                                                chipBg = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.28f else 0.72f),
                                                                textColor = skyPalette.content
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .fillMaxHeight(),
                                            shape = RoundedCornerShape(18.dp),
                                            color = lavenderPalette.container
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = "Paused (${uiState.blackoutOnSelectedDate.size})",
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 11.5.sp,
                                                    color = lavenderPalette.content
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                if (uiState.blackoutOnSelectedDate.isEmpty()) {
                                                    Text("None", fontSize = 12.sp, color = lavenderPalette.content.copy(alpha = 0.7f))
                                                } else {
                                                    FlowRow(
                                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                                    ) {
                                                        uiState.blackoutOnSelectedDate.forEach { name ->
                                                            InspectorServiceChip(
                                                                serviceName = name,
                                                                chipBg = MaterialTheme.colorScheme.surface.copy(alpha = if (isDark) 0.28f else 0.72f),
                                                                textColor = lavenderPalette.content
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (uiState.cycleStatuses.isEmpty()) {
                            item(key = "planner_empty") {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    border = cardBorder
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Tap '+' below to add subscriptions and visualize your cycle timeline.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            items(
                                items = uiState.cycleStatuses,
                                key = { it.subscription.id },
                                contentType = { "timeline_item" }
                            ) { cycleStatus ->
                                CycleTimelineView(
                                    cycleStatus = cycleStatus,
                                    inspectedDate = uiState.selectedTargetDate,
                                    onClick = { onOpenSubscription(cycleStatus.subscription.id) },
                                    modifier = Modifier.animateItem()
                                )
                            }
                        }
                    } else {
                        item(key = "paired_swap_card") {
                            val sagePalette = ExpressivePalettes.softSage()
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(26.dp),
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                border = cardBorder
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Text(
                                        text = "30d / 30d Paired Swap",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = "Alternate two monthly services every 30 days so one stays active while the other rests—cutting combined cost in half.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))

                                    if (uiState.monthlySubscriptions.size < 2) {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(18.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                                        ) {
                                            Text(
                                                text = "Add at least 2 monthly subscriptions (tap '+' below) to build an alternating ~49% swap pair. Yearly plans are excluded automatically.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(14.dp)
                                            )
                                        }
                                    } else {
                                        Text("Service 1 (Active First)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        PlannerServiceDropdown(
                                            subscriptions = uiState.monthlySubscriptions,
                                            selected = uiState.service1,
                                            onSelect = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onAction(PlannerAction.SelectService1(it.id))
                                            }
                                        )

                                        // Kinetic Morphing Baton-Swap Medallion (Cookie9 -> Sunny on press + 180° flip!)
                                        val swapMedallionInteraction = remember { MutableInteractionSource() }
                                        val swapMedallionPressed by swapMedallionInteraction.collectIsPressedAsState()
                                        var swapFlipTrigger by remember { mutableIntStateOf(0) }
                                        val batonShape = rememberMorphingPolygonShape(
                                            targetPolygon = if (swapMedallionPressed) {
                                                ExpressiveMaterialPolygons.Sunny
                                            } else {
                                                ExpressiveMaterialPolygons.Cookie9
                                            }
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            HorizontalDivider(
                                                thickness = 1.dp,
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .expressivePolygonSurface(
                                                        shape = batonShape,
                                                        color = MaterialTheme.colorScheme.primaryContainer,
                                                        borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                                                        borderWidth = 1.dp
                                                    )
                                                    .clickable(
                                                        interactionSource = swapMedallionInteraction,
                                                        indication = null,
                                                        role = Role.Button
                                                    ) {
                                                        swapFlipTrigger++
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        onAction(PlannerAction.SwapServiceOrder)
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                ReactiveIcon(
                                                    imageVector = Icons.Default.SwapVert,
                                                    contentDescription = "Swap service order",
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    size = 19.dp,
                                                    motion = ReactiveMotionType.SWAP_FLIP,
                                                    isPressed = swapMedallionPressed,
                                                    triggerKey = swapFlipTrigger
                                                )
                                            }
                                        }

                                        Text("Service 2 (Active Second)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(5.dp))
                                        PlannerServiceDropdown(
                                            subscriptions = uiState.monthlySubscriptions.filter { it.id != uiState.service1?.id },
                                            selected = uiState.service2,
                                            onSelect = {
                                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                onAction(PlannerAction.SelectService2(it.id))
                                            }
                                        )

                                        if (uiState.service1 != null && uiState.service2 != null && !uiState.hasMixedCurrencies) {
                                            Spacer(modifier = Modifier.height(12.dp))
                                            PairedSwapBatonVisualizer(
                                                service1 = uiState.service1,
                                                service2 = uiState.service2,
                                                swapNonce = swapFlipTrigger
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(18.dp),
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 11.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column {
                                                    Text("Both active", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text("${MoneyFormatter.format(uiState.combinedMonthlyCostBefore, uiState.currencyCode)}/mo", fontFamily = DisplayFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, letterSpacing = (-0.2).sp, color = MaterialTheme.colorScheme.onSurface)
                                                }
                                                Column {
                                                    Text("Alternating", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text("${MoneyFormatter.format(uiState.combinedMonthlyCostAfter, uiState.currencyCode)}/mo", fontFamily = DisplayFontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, letterSpacing = (-0.2).sp, color = MaterialTheme.colorScheme.primary)
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text("Yearly save", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text("${MoneyFormatter.format(uiState.combinedAnnualSavings, uiState.currencyCode)}/yr", fontFamily = DisplayFontFamily, fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = (-0.2).sp, color = MaterialTheme.colorScheme.secondary)
                                                }
                                            }
                                        }

                                        if (uiState.hasMixedCurrencies) {
                                            Text(
                                                text = "Choose services billed in the same currency. SubCycler never guesses exchange rates.",
                                                modifier = Modifier.padding(top = 10.dp),
                                                color = MaterialTheme.colorScheme.error,
                                                fontSize = 11.5.sp
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        val swapBtnInteraction = remember { MutableInteractionSource() }
                                        val swapBtnPressed by swapBtnInteraction.collectIsPressedAsState()
                                        var swapAlignCount by remember { mutableIntStateOf(0) }

                                        Button(
                                            onClick = {
                                                swapAlignCount++
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onAction(PlannerAction.AlignAndActivate)
                                                showSuccessSnackbar = true
                                            },
                                            interactionSource = swapBtnInteraction,
                                            enabled = uiState.service1 != null && uiState.service2 != null && !uiState.hasMixedCurrencies,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(52.dp),
                                            shape = RoundedCornerShape(999.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary
                                            )
                                        ) {
                                            ReactiveIcon(
                                                imageVector = Icons.Default.SyncAlt,
                                                contentDescription = null,
                                                tint = LocalContentColor.current,
                                                size = 18.dp,
                                                motion = ReactiveMotionType.SWAP_FLIP,
                                                isPressed = swapBtnPressed,
                                                triggerKey = Triple(swapAlignCount, uiState.service1?.id, uiState.service2?.id)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Align Swap Schedule", fontWeight = FontWeight.ExtraBold)
                                        }

                                        AnimatedVisibility(
                                            visible = showSuccessSnackbar,
                                            enter = expandVertically(
                                                animationSpec = spring(
                                                    stiffness = Spring.StiffnessMediumLow,
                                                    dampingRatio = Spring.DampingRatioNoBouncy
                                                )
                                            ) + fadeIn(animationSpec = tween(120)),
                                            exit = shrinkVertically() + fadeOut(animationSpec = tween(90))
                                        ) {
                                            Column {
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(16.dp),
                                                    color = sagePalette.container,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(12.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = sagePalette.content, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = "Schedules aligned! Swapping occurs every 30 days.",
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = sagePalette.content
                                                        )
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
                }
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = uiState.selectedTargetDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            onAction(PlannerAction.SetSelectedDate(picked))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Inspect date", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = pickerState, showModeToggle = false)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlannerServiceDropdown(
    subscriptions: List<Subscription>,
    selected: Subscription?,
    onSelect: (Subscription) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected?.let { "${it.name} (${MoneyFormatter.format(it.cost, it.currency)}${it.billingPeriod.shortSuffix})" } ?: "Select a service",
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            leadingIcon = selected?.let { sub ->
                {
                    ExpressiveServiceBadge(
                        serviceName = sub.name,
                        size = 28.dp,
                        fontSize = 10.sp
                    )
                }
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Transparent
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(18.dp)
        ) {
            subscriptions.forEach { sub ->
                DropdownMenuItem(
                    leadingIcon = {
                        ExpressiveServiceBadge(
                            serviceName = sub.name,
                            size = 28.dp,
                            fontSize = 10.sp
                        )
                    },
                    text = {
                        Text(
                            text = "${sub.name} (${MoneyFormatter.format(sub.cost, sub.currency)}${sub.billingPeriod.shortSuffix})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    onClick = {
                        onSelect(sub)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun InspectorServiceChip(
    serviceName: String,
    chipBg: Color,
    textColor: Color
) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = chipBg
    ) {
        Row(
            modifier = Modifier.padding(start = 4.dp, end = 9.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExpressiveServiceBadge(
                serviceName = serviceName,
                size = 20.dp,
                fontSize = 8.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = serviceName,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Visualizes the 60-day interlocking baton pass between Service 1 (Days 1–30 active)
 * and Service 2 (Days 31–60 active) with a dashed Day 30 handoff marker.
 */
@Composable
private fun PairedSwapBatonVisualizer(
    service1: Subscription,
    service2: Subscription,
    swapNonce: Int = 0
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val hatchColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.16f)
    val dashColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    val restBg = MaterialTheme.colorScheme.surfaceContainer

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExpressiveEyebrow(text = "60-DAY BATON PASS", fontSize = 10.sp)
                Text(
                    text = "Day 30 swap",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedContent(
                targetState = Triple(service1, service2, swapNonce),
                transitionSpec = {
                    if (reducedMotion) {
                        fadeIn(tween(0)).togetherWith(fadeOut(tween(0)))
                    } else {
                        (slideInHorizontally(
                            animationSpec = ExpressiveMotion.expressiveSpatialDefaultIntOffset()
                                .orSnapIfReduced(reducedMotion)
                        ) { w -> w / 5 } + fadeIn(tween(180))).togetherWith(
                            slideOutHorizontally(tween(130)) { w -> -w / 5 } + fadeOut(tween(110))
                        )
                    }
                },
                label = "batonTrackSlide"
            ) { (activeFirst, activeSecond, _) ->
            Box(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    // Track 1: Service 1 Active first (Days 1–30), then Rest (Days 31–60)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExpressiveServiceBadge(
                            serviceName = activeFirst.name,
                            size = 26.dp,
                            fontSize = 9.5.sp
                        )
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(26.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "30d Active",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    maxLines = 1
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(restBg)
                                    .diagonalHatchPattern(color = hatchColor, spacing = 6.dp, strokeWidth = 1.dp)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(9.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "30d Rest",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Track 2: Service 2 Rest first (Days 1–30), then Active (Days 31–60)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExpressiveServiceBadge(
                            serviceName = activeSecond.name,
                            size = 26.dp,
                            fontSize = 9.5.sp
                        )
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(26.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(restBg)
                                    .diagonalHatchPattern(color = hatchColor, spacing = 6.dp, strokeWidth = 1.dp)
                                    .border(
                                        1.dp,
                                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                        RoundedCornerShape(9.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "30d Rest",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "30d Active",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Vertical dashed handoff line bisecting the track region at Day 30
                Canvas(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(start = 34.dp)
                ) {
                    val midX = size.width / 2f
                    drawLine(
                        color = dashColor,
                        start = Offset(midX, 0f),
                        end = Offset(midX, size.height),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f), 0f)
                    )
                }
            }
            }
        }
    }
}
