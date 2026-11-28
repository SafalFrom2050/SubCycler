package com.subtrack.feature.planner

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
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
import com.subtrack.core.ui.components.ExpressiveCookie12Shape
import com.subtrack.core.ui.components.ExpressiveCookie8Shape
import com.subtrack.core.ui.components.ExpressiveEyebrow
import com.subtrack.core.ui.components.ExpressiveHeroCardBackdrop
import com.subtrack.core.ui.components.ExpressiveMaterialPolygons
import com.subtrack.core.ui.components.ExpressiveMoneyText
import com.subtrack.core.ui.components.ExpressivePalettes
import com.subtrack.core.ui.components.ExpressiveReactiveIconButton
import com.subtrack.core.ui.components.ExpressiveScreenTitle
import com.subtrack.core.ui.components.ExpressiveSectionTitle
import com.subtrack.core.ui.components.ExpressiveSegmentOption
import com.subtrack.core.ui.components.ExpressiveSegmentedGroup
import com.subtrack.core.ui.components.ExpressiveServiceBadge
import com.subtrack.core.ui.components.ExpressiveSettingsActionButton
import com.subtrack.core.ui.components.ReactiveIcon
import com.subtrack.core.ui.components.ReactiveMotionType
import com.subtrack.core.ui.components.ShapeBloomBackdrop
import com.subtrack.core.ui.components.diagonalHatchPattern
import com.subtrack.core.ui.components.expressiveCardBorder
import com.subtrack.core.ui.components.expressivePolygonSurface
import com.subtrack.core.ui.components.progressiveEdgeFade
import com.subtrack.core.ui.components.rememberMorphingPolygonShape
import com.subtrack.core.ui.theme.BodyFontFamily
import com.subtrack.core.ui.theme.BrandColorHarmonizer
import com.subtrack.core.ui.theme.DisplayFontFamily
import com.subtrack.core.ui.theme.DisplayHeroFontFamily
import com.subtrack.core.ui.theme.ExpressiveMotion
import com.subtrack.core.ui.theme.LocalSubCyclerAccentTokens
import com.subtrack.core.ui.theme.LocalSubCyclerDarkTheme
import com.subtrack.core.ui.theme.orSnapIfReduced
import com.subtrack.core.ui.theme.rememberReducedMotionEnabled
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun PlannerRoute(
    viewModel: PlannerViewModel = hiltViewModel(),
    pendingOpenPairedSwap: Boolean = false,
    onOpenPairedSwapConsumed: () -> Unit = {},
    isEntranceReady: Boolean = true,
    onOpenSettings: (() -> Unit)? = null,
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
        isEntranceReady = isEntranceReady,
        onOpenSettings = onOpenSettings,
        onOpenSubscription = onOpenSubscription,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlannerScreen(
    uiState: PlannerUiState,
    onAction: (PlannerAction) -> Unit,
    isEntranceReady: Boolean = true,
    onOpenSettings: (() -> Unit)? = null,
    onOpenSubscription: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isDark = LocalSubCyclerDarkTheme.current
    val accentTokens = LocalSubCyclerAccentTokens.current
    val reducedMotion = rememberReducedMotionEnabled()
    val headlineDateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }
    val yearFormatter = remember { DateTimeFormatter.ofPattern("yyyy") }
    var showSuccessSnackbar by remember(uiState.service1?.id, uiState.service2?.id) { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    val cardBorder = expressiveCardBorder()

    val isViewingToday = uiState.selectedTargetDate == uiState.today
    val dayOffsetFromToday = ChronoUnit.DAYS.between(uiState.today, uiState.selectedTargetDate)

    // One-shot springy first-visit entrance choreography for Planner's Hero & controls
    val skipEntrance = isEntranceReady || reducedMotion
    val heroReveal = remember { Animatable(if (skipEntrance) 1f else 0f) }

    LaunchedEffect(isEntranceReady, reducedMotion) {
        if (!isEntranceReady) return@LaunchedEffect
        if (reducedMotion) {
            heroReveal.snapTo(1f)
            return@LaunchedEffect
        }
        if (heroReveal.value >= 0.99f) return@LaunchedEffect
        heroReveal.animateTo(1f, ExpressiveMotion.expressiveSpatialDefaultFloat())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    ExpressiveScreenTitle(text = "Planner")
                },
                actions = {
                    AnimatedVisibility(
                        visible = uiState.selectedTab == PlannerTab.TIMELINE && !isViewingToday,
                        enter = fadeIn(tween(150)),
                        exit = fadeOut(tween(120))
                    ) {
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onAction(PlannerAction.ResetToToday)
                            },
                            shape = RoundedCornerShape(999.dp),
                            color = accentTokens.heroPop,
                            modifier = Modifier.padding(end = if (onOpenSettings != null) 8.dp else 14.dp)
                        ) {
                            Text(
                                text = "Today",
                                fontFamily = BodyFontFamily,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = accentTokens.onHeroPop,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                    if (onOpenSettings != null) {
                        ExpressiveSettingsActionButton(onClick = onOpenSettings)
                    }
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
            // Connected Material 3 Expressive Segmented Mode Group with minimal icons
            val plannerTabOptions = remember {
                listOf(
                    ExpressiveSegmentOption(PlannerTab.TIMELINE, "Timeline", Icons.Default.CalendarMonth),
                    ExpressiveSegmentOption(PlannerTab.PAIRED_SWAP, "Paired Swap", Icons.Default.SyncAlt)
                )
            }
            ExpressiveSegmentedGroup(
                options = plannerTabOptions,
                selectedValue = uiState.selectedTab,
                onSelect = { tab -> onAction(PlannerAction.SelectTab(tab)) },
                height = 44.dp,
                fontSize = 12.5.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            AnimatedContent(
                targetState = uiState.selectedTab,
                modifier = Modifier.weight(1f),
                transitionSpec = {
                    fadeIn(tween(170)).togetherWith(fadeOut(tween(110)))
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
                        contentPadding = PaddingValues(bottom = 118.dp + navBarBottom)
                    ) {
                        if (tab == PlannerTab.TIMELINE) {
                            // 1. Pure Flat Color-Block Rotation Forecast Hero Card
                            item(key = "date_inspector_hero", contentType = "hero") {
                                PlannerDateHeroCard(
                                    uiState = uiState,
                                    isViewingToday = isViewingToday,
                                    dayOffsetFromToday = dayOffsetFromToday,
                                    headlineDateFormatter = headlineDateFormatter,
                                    yearFormatter = yearFormatter,
                                    heroReveal = heroReveal,
                                    onStepDate = { days -> onAction(PlannerAction.StepDate(days)) },
                                    onJumpOffset = { offset ->
                                        if (offset == 0L) {
                                            onAction(PlannerAction.ResetToToday)
                                        } else {
                                            onAction(PlannerAction.SetSelectedDate(uiState.today.plusDays(offset)))
                                        }
                                    },
                                    onOpenDatePicker = { showDatePicker = true }
                                )
                            }

                            if (uiState.cycleStatuses.isEmpty()) {
                                item(key = "planner_empty", contentType = "empty") {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(28.dp),
                                        color = MaterialTheme.colorScheme.surfaceContainer,
                                        border = cardBorder
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 24.dp, vertical = 32.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .expressivePolygonSurface(
                                                        shape = ExpressiveCookie8Shape,
                                                        color = accentTokens.heroPop
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CalendarMonth,
                                                    contentDescription = null,
                                                    tint = accentTokens.onHeroPop,
                                                    modifier = Modifier.size(25.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(14.dp))
                                            Text(
                                                text = "Map your rotation windows",
                                                fontFamily = DisplayFontFamily,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 18.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                textAlign = TextAlign.Center
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = "Tap '+' below to add subscriptions and forecast active & rest periods across the year.",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            } else {
                                item(key = "timeline_section_header", contentType = "section_header") {
                                    ExpressiveSectionTitle(
                                        text = "Cycles",
                                        fontSize = 22.sp,
                                        modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 6.dp)
                                    )
                                }

                                itemsIndexed(
                                    items = uiState.cycleStatuses,
                                    key = { _, status -> status.subscription.id },
                                    contentType = { _, _ -> "timeline_item" }
                                ) { index, cycleStatus ->
                                    CycleTimelineView(
                                        cycleStatus = cycleStatus,
                                        inspectedDate = uiState.selectedTargetDate,
                                        isEntranceReady = isEntranceReady,
                                        entranceIndex = index,
                                        onClick = { onOpenSubscription(cycleStatus.subscription.id) },
                                        modifier = Modifier.animateItem()
                                    )
                                }
                            }
                        } else {
                            // 2. Redesigned Pure Flat Color-Block Paired Swap Experience
                            item(key = "paired_swap_hero", contentType = "swap_hero") {
                                PairedSwapHeroCard(
                                    uiState = uiState,
                                    heroReveal = heroReveal
                                )
                            }

                            item(key = "paired_swap_builder", contentType = "swap_builder") {
                                val sagePalette = ExpressivePalettes.softSage()
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(26.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    border = cardBorder
                                ) {
                                    Column(modifier = Modifier.padding(18.dp)) {
                                        if (uiState.monthlySubscriptions.size < 2) {
                                            Surface(
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(18.dp),
                                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                                            ) {
                                                Text(
                                                    text = "Add at least 2 monthly subscriptions (tap '+' below) to pair them into an alternating 30d / 30d schedule. Yearly plans are excluded automatically.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(14.dp)
                                                )
                                            }
                                        } else {
                                            ExpressiveEyebrow(text = "SWAP PAIR")
                                            Spacer(modifier = Modifier.height(12.dp))

                                            PlannerServiceCardPicker(
                                                slotLabel = "DAYS 1–30",
                                                subscriptions = uiState.monthlySubscriptions,
                                                selected = uiState.service1,
                                                onSelect = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    onAction(PlannerAction.SelectService1(it.id))
                                                }
                                            )

                                            // Kinetic Morphing Baton-Swap Medallion (Triadic Pop Cookie9 -> Sunny + 180° flip)
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
                                                    .padding(vertical = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                HorizontalDivider(
                                                    thickness = 1.dp,
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .expressivePolygonSurface(
                                                            shape = batonShape,
                                                            color = accentTokens.heroPop
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
                                                        tint = accentTokens.onHeroPop,
                                                        size = 20.dp,
                                                        motion = ReactiveMotionType.SWAP_FLIP,
                                                        isPressed = swapMedallionPressed,
                                                        triggerKey = swapFlipTrigger
                                                    )
                                                }
                                            }

                                            PlannerServiceCardPicker(
                                                slotLabel = "DAYS 31–60",
                                                subscriptions = uiState.monthlySubscriptions.filter { it.id != uiState.service1?.id },
                                                selected = uiState.service2,
                                                onSelect = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    onAction(PlannerAction.SelectService2(it.id))
                                                }
                                            )

                                            if (uiState.service1 != null && uiState.service2 != null && !uiState.hasMixedCurrencies) {
                                                Spacer(modifier = Modifier.height(14.dp))
                                                PairedSwapBatonVisualizer(
                                                    service1 = uiState.service1,
                                                    service2 = uiState.service2,
                                                    swapNonce = swapFlipTrigger
                                                )
                                            }

                                            if (uiState.hasMixedCurrencies) {
                                                Text(
                                                    text = "Choose services billed in the same currency. SubCycler never guesses exchange rates.",
                                                    modifier = Modifier.padding(top = 10.dp),
                                                    color = MaterialTheme.colorScheme.error,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(14.dp))

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
                                                    .height(54.dp),
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
                                                Text(
                                                    text = "Align Schedule",
                                                    fontFamily = DisplayFontFamily,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 15.sp
                                                )
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
                                                            Icon(
                                                                Icons.Default.CheckCircle,
                                                                contentDescription = null,
                                                                tint = sagePalette.content,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                            Spacer(modifier = Modifier.width(8.dp))
                                                            Text(
                                                                text = "Aligned · swaps every 30 days",
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlannerDateHeroCard(
    uiState: PlannerUiState,
    isViewingToday: Boolean,
    dayOffsetFromToday: Long,
    headlineDateFormatter: DateTimeFormatter,
    yearFormatter: DateTimeFormatter,
    heroReveal: Animatable<Float, *>,
    onStepDate: (Long) -> Unit,
    onJumpOffset: (Long) -> Unit,
    onOpenDatePicker: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val isDark = LocalSubCyclerDarkTheme.current
    val accentTokens = LocalSubCyclerAccentTokens.current
    val heroInk = accentTokens.heroContent
    val frostedBg = if (isDark) Color.Black.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.20f)
    val frostedChipBg = if (isDark) Color.Black.copy(alpha = 0.24f) else Color.White.copy(alpha = 0.18f)

    val deltaBadgeText = when {
        dayOffsetFromToday == 0L -> null
        dayOffsetFromToday > 0L -> "+${dayOffsetFromToday}D"
        else -> "-${abs(dayOffsetFromToday)}D"
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                val r = heroReveal.value
                val s = 0.96f + 0.04f * r
                alpha = ExpressiveMotion.clampEffectProgress(r * 1.45f)
                scaleX = s
                scaleY = s
                translationY = (1f - r) * 14.dp.toPx()
            },
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent
    ) {
        ExpressiveHeroCardBackdrop(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp)),
            watermarkPolygon = ExpressiveMaterialPolygons.Clover8
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 18.dp)
            ) {
                // Minimal Eyebrow + offset pill only when away from today (no duplicate TODAY label)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExpressiveEyebrow(
                        text = "FORECAST",
                        color = heroInk.copy(alpha = 0.78f)
                    )
                    if (deltaBadgeText != null) {
                        Surface(
                            shape = RoundedCornerShape(999.dp),
                            color = accentTokens.heroPop
                        ) {
                            Text(
                                text = deltaBadgeText,
                                fontFamily = BodyFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.8.sp,
                                color = accentTokens.onHeroPop,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Interactive Date Scrubber Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExpressiveReactiveIconButton(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Previous week",
                        onClick = { onStepDate(-7L) },
                        shape = CircleShape,
                        containerColor = frostedBg,
                        iconTint = heroInk,
                        buttonSize = 42.dp,
                        iconSize = 21.dp,
                        motion = ReactiveMotionType.NUDGE_LEFT
                    )

                    Surface(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onOpenDatePicker()
                        },
                        shape = RoundedCornerShape(999.dp),
                        color = frostedBg
                    ) {
                        Row(
                            modifier = Modifier
                                .heightIn(min = 42.dp)
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = "Pick any date",
                                tint = accentTokens.heroPop,
                                modifier = Modifier.size(17.dp)
                            )
                            AnimatedContent(
                                targetState = uiState.selectedTargetDate,
                                transitionSpec = {
                                    val dir = if (targetState.isAfter(initialState)) 1 else -1
                                    (slideInHorizontally(tween(180)) { dir * (it / 3) } + fadeIn(tween(160)))
                                        .togetherWith(slideOutHorizontally(tween(130)) { -dir * (it / 3) } + fadeOut(tween(110)))
                                },
                                label = "inspectorDateSlide"
                            ) { inspectedDate ->
                                Row(
                                    verticalAlignment = Alignment.Bottom,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        text = inspectedDate.format(headlineDateFormatter),
                                        fontFamily = DisplayHeroFontFamily,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp,
                                        letterSpacing = (-0.4).sp,
                                        color = heroInk
                                    )
                                    Text(
                                        text = inspectedDate.format(yearFormatter),
                                        fontFamily = BodyFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = heroInk.copy(alpha = 0.70f),
                                        modifier = Modifier.padding(bottom = 1.dp)
                                    )
                                }
                            }
                        }
                    }

                    ExpressiveReactiveIconButton(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Next week",
                        onClick = { onStepDate(7L) },
                        shape = CircleShape,
                        containerColor = frostedBg,
                        iconTint = heroInk,
                        buttonSize = 42.dp,
                        iconSize = 21.dp,
                        motion = ReactiveMotionType.NUDGE_RIGHT
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quick-Jump Horizon Strip (Today / +15d / +30d / +45d)
                val quickHorizons = remember {
                    listOf(0L to "Today", 15L to "+15d", 30L to "+30d", 45L to "+45d")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    quickHorizons.forEach { (offsetDays, label) ->
                        val isSelectedOffset = dayOffsetFromToday == offsetDays
                        Surface(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onJumpOffset(offsetDays)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(999.dp),
                            color = if (isSelectedOffset) accentTokens.heroPop else frostedChipBg
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontFamily = BodyFontFamily,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSelectedOffset) accentTokens.onHeroPop else heroInk.copy(alpha = 0.88f)
                                )
                            }
                        }
                    }
                }

                // Compact Live Stack Snapshot (zero empty dead-space boxes!)
                if (uiState.cycleStatuses.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = frostedBg
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(accentTokens.heroPop)
                                    )
                                    Text(
                                        text = "${uiState.activeSubsOnSelectedDate.size} Active",
                                        fontFamily = DisplayFontFamily,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = heroInk
                                    )
                                }
                                if (uiState.blackoutSubsOnSelectedDate.isNotEmpty()) {
                                    Text(
                                        text = "${uiState.blackoutSubsOnSelectedDate.size} on break",
                                        fontFamily = BodyFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.5.sp,
                                        color = heroInk.copy(alpha = 0.78f)
                                    )
                                }
                            }

                            if (uiState.activeSubsOnSelectedDate.isNotEmpty()) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    uiState.activeSubsOnSelectedDate.forEach { sub ->
                                        InspectorServiceChip(
                                            serviceName = sub.name,
                                            colorHex = sub.colorHex,
                                            chipBg = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.18f),
                                            textColor = heroInk
                                        )
                                    }
                                }
                            }

                            if (uiState.blackoutSubsOnSelectedDate.isNotEmpty()) {
                                HorizontalDivider(
                                    thickness = 0.75.dp,
                                    color = heroInk.copy(alpha = 0.14f)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "BREAK",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        letterSpacing = 0.8.sp,
                                        color = accentTokens.heroPop
                                    )
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        uiState.blackoutSubsOnSelectedDate.forEach { sub ->
                                            InspectorServiceChip(
                                                serviceName = sub.name,
                                                colorHex = sub.colorHex,
                                                chipBg = accentTokens.heroPop.copy(alpha = 0.22f),
                                                textColor = heroInk
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

@Composable
private fun PairedSwapHeroCard(
    uiState: PlannerUiState,
    heroReveal: Animatable<Float, *>
) {
    val isDark = LocalSubCyclerDarkTheme.current
    val accentTokens = LocalSubCyclerAccentTokens.current
    val heroInk = accentTokens.heroContent
    val frostedPillBg = if (isDark) Color.Black.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.22f)

    val hasValidPair = uiState.service1 != null && uiState.service2 != null && !uiState.hasMixedCurrencies
    val hasSavings = hasValidPair && uiState.combinedAnnualSavings > 0.5
    val pctCut = if (hasValidPair && uiState.combinedMonthlyCostBefore > 0.0) {
        ((1.0 - uiState.combinedMonthlyCostAfter / uiState.combinedMonthlyCostBefore) * 100.0).roundToInt()
    } else {
        49
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                val r = heroReveal.value
                val s = 0.96f + 0.04f * r
                alpha = ExpressiveMotion.clampEffectProgress(r * 1.45f)
                scaleX = s
                scaleY = s
                translationY = (1f - r) * 14.dp.toPx()
            },
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent
    ) {
        ExpressiveHeroCardBackdrop(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp)),
            watermarkPolygon = ExpressiveMaterialPolygons.Clover8
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    ExpressiveEyebrow(
                        text = "PAIRED SWAP",
                        color = heroInk.copy(alpha = 0.78f)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    if (hasValidPair) {
                        ExpressiveMoneyText(
                            formattedAmount = MoneyFormatter.format(uiState.combinedMonthlyCostAfter, uiState.currencyCode),
                            heroFontSize = 40.sp,
                            cadenceSuffix = "/mo",
                            color = heroInk
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        val bothText = MoneyFormatter.format(uiState.combinedMonthlyCostBefore, uiState.currencyCode)
                        Text(
                            text = "Down from $bothText · -$pctCut%",
                            color = heroInk.copy(alpha = 0.80f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "Cut 2 plans in half",
                            fontFamily = DisplayHeroFontFamily,
                            fontWeight = FontWeight.Black,
                            fontSize = 24.sp,
                            letterSpacing = (-0.5).sp,
                            color = heroInk
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = frostedPillBg
                    ) {
                        Text(
                            text = "30d / 30d alternating",
                            color = heroInk,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)
                        )
                    }
                }

                if (hasSavings) {
                    Spacer(modifier = Modifier.width(12.dp))
                    val formattedSave = MoneyFormatter.format(
                        uiState.combinedAnnualSavings,
                        uiState.currencyCode,
                        maximumFractionDigits = 0
                    )
                    val amountFontSize = when {
                        formattedSave.length >= 9 -> 13.sp
                        formattedSave.length >= 7 -> 15.sp
                        formattedSave.length >= 5 -> 17.sp
                        else -> 19.sp
                    }
                    val ringColor = accentTokens.onHeroPop.copy(alpha = 0.28f)
                    Box(
                        modifier = Modifier
                            .size(94.dp)
                            .expressivePolygonSurface(
                                shape = ExpressiveCookie12Shape,
                                color = accentTokens.heroPop
                            )
                            .drawWithCache {
                                val inset = 9.dp.toPx()
                                val radius = (size.minDimension / 2f) - inset
                                val stroke = Stroke(
                                    width = 1.1.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                                )
                                onDrawBehind {
                                    drawCircle(color = ringColor, radius = radius, style = stroke)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 10.dp)
                        ) {
                            Text(
                                text = "SAVE",
                                fontFamily = BodyFontFamily,
                                color = accentTokens.onHeroPop.copy(alpha = 0.78f),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 9.sp,
                                lineHeight = 10.sp,
                                letterSpacing = 1.1.sp
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = formattedSave,
                                fontFamily = DisplayHeroFontFamily,
                                color = accentTokens.onHeroPop,
                                fontWeight = FontWeight.Black,
                                fontSize = amountFontSize,
                                lineHeight = (amountFontSize.value + 1.5f).sp,
                                letterSpacing = (-0.4).sp,
                                maxLines = 1
                            )
                            Text(
                                text = "per year",
                                fontFamily = BodyFontFamily,
                                color = accentTokens.onHeroPop.copy(alpha = 0.78f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp,
                                lineHeight = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlannerServiceCardPicker(
    slotLabel: String,
    subscriptions: List<Subscription>,
    selected: Subscription?,
    onSelect: (Subscription) -> Unit
) {
    val isDark = LocalSubCyclerDarkTheme.current
    var expanded by remember { mutableStateOf(false) }
    val brandPalette = remember(selected?.name, selected?.colorHex, isDark) {
        selected?.let {
            BrandColorHarmonizer.harmonizedTonalPalette(
                serviceName = it.name,
                storedColorHex = it.colorHex,
                isDark = isDark
            )
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        Surface(
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            border = BorderStroke(
                width = if (expanded) 1.5.dp else 1.dp,
                color = if (expanded) {
                    brandPalette?.accent ?: MaterialTheme.colorScheme.primary
                } else {
                    (brandPalette?.accent ?: MaterialTheme.colorScheme.outlineVariant).copy(alpha = if (isDark) 0.30f else 0.24f)
                }
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(34.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(brandPalette?.accent ?: MaterialTheme.colorScheme.outlineVariant)
                )

                if (selected != null) {
                    ExpressiveServiceBadge(
                        serviceName = selected.name,
                        colorHex = selected.colorHex,
                        size = 38.dp,
                        fontSize = 12.sp
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = slotLabel,
                        fontFamily = BodyFontFamily,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.9.sp,
                        color = brandPalette?.accent ?: MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = selected?.name ?: "Select a monthly service",
                        fontFamily = DisplayFontFamily,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.5.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (selected != null) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Text(
                            text = "${MoneyFormatter.format(selected.cost, selected.currency)}${selected.billingPeriod.shortSuffix}",
                            fontFamily = DisplayFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Choose service",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            shape = RoundedCornerShape(20.dp)
        ) {
            subscriptions.forEach { sub ->
                DropdownMenuItem(
                    leadingIcon = {
                        ExpressiveServiceBadge(
                            serviceName = sub.name,
                            colorHex = sub.colorHex,
                            size = 30.dp,
                            fontSize = 10.sp
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = sub.name,
                                fontFamily = DisplayFontFamily,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${MoneyFormatter.format(sub.cost, sub.currency)}${sub.billingPeriod.shortSuffix}",
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
    colorHex: String?,
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
                colorHex = colorHex,
                size = 20.dp,
                fontSize = 8.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = serviceName,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Brand-colored 60-day interlocking baton pass between Service 1 (Days 1–30 active)
 * and Service 2 (Days 31–60 active) with a crisp Triadic Day 30 handoff divider.
 */
@Composable
private fun PairedSwapBatonVisualizer(
    service1: Subscription,
    service2: Subscription,
    swapNonce: Int = 0
) {
    val isDark = LocalSubCyclerDarkTheme.current
    val accentTokens = LocalSubCyclerAccentTokens.current
    val reducedMotion = rememberReducedMotionEnabled()
    val hatchColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (isDark) 0.16f else 0.13f)
    val dashColor = accentTokens.heroPop.copy(alpha = 0.85f)
    val restBg = MaterialTheme.colorScheme.surfaceContainer

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExpressiveEyebrow(text = "60-DAY CYCLE", fontSize = 10.sp)
                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = accentTokens.heroPop
                ) {
                    Text(
                        text = "DAY 30",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.6.sp,
                        color = accentTokens.onHeroPop,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
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
                val palette1 = remember(activeFirst.name, activeFirst.colorHex, isDark) {
                    BrandColorHarmonizer.harmonizedTonalPalette(
                        serviceName = activeFirst.name,
                        storedColorHex = activeFirst.colorHex,
                        isDark = isDark
                    )
                }
                val palette2 = remember(activeSecond.name, activeSecond.colorHex, isDark) {
                    BrandColorHarmonizer.harmonizedTonalPalette(
                        serviceName = activeSecond.name,
                        storedColorHex = activeSecond.colorHex,
                        isDark = isDark
                    )
                }

                Box(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Track 1: Service 1 vivid brand Active (Days 1–30), then Hatched Rest (Days 31–60)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ExpressiveServiceBadge(
                                serviceName = activeFirst.name,
                                colorHex = activeFirst.colorHex,
                                size = 28.dp,
                                fontSize = 9.5.sp
                            )
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(palette1.container),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Active",
                                        fontFamily = DisplayFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = palette1.content,
                                        maxLines = 1
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(restBg)
                                        .diagonalHatchPattern(color = hatchColor, spacing = 6.dp, strokeWidth = 1.dp)
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                                            RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Rest",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                            }
                        }

                        // Track 2: Service 2 Hatched Rest (Days 1–30), then vivid brand Active (Days 31–60)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ExpressiveServiceBadge(
                                serviceName = activeSecond.name,
                                colorHex = activeSecond.colorHex,
                                size = 28.dp,
                                fontSize = 9.5.sp
                            )
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(30.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(restBg)
                                        .diagonalHatchPattern(color = hatchColor, spacing = 6.dp, strokeWidth = 1.dp)
                                        .border(
                                            1.dp,
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
                                            RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Rest",
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
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(palette2.container),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Active",
                                        fontFamily = DisplayFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = palette2.content,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Vertical Triadic dashed handoff line bisecting the track region at Day 30
                    Canvas(
                        modifier = Modifier
                            .matchParentSize()
                            .padding(start = 36.dp)
                    ) {
                        val midX = size.width / 2f
                        drawLine(
                            color = dashColor,
                            start = Offset(midX, 0f),
                            end = Offset(midX, size.height),
                            strokeWidth = 1.8.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f), 0f)
                        )
                    }
                }
            }
        }
    }
}
