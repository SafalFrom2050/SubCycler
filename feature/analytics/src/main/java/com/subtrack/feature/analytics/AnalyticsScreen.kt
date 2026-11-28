package com.subtrack.feature.analytics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.ui.components.CurrencyScopeNotice
import com.subtrack.core.ui.components.ExpressiveClover4Shape
import com.subtrack.core.ui.components.ExpressiveCookie12Shape
import com.subtrack.core.ui.components.ExpressiveCookie8Shape
import com.subtrack.core.ui.components.ExpressiveEyebrow
import com.subtrack.core.ui.components.ExpressiveHeroCardBackdrop
import com.subtrack.core.ui.components.ExpressiveMoneyText
import com.subtrack.core.ui.components.ExpressivePalettes
import com.subtrack.core.ui.components.ExpressiveScreenTitle
import com.subtrack.core.ui.components.ExpressiveSectionRow
import com.subtrack.core.ui.components.ExpressiveSectionStack
import com.subtrack.core.ui.components.ExpressiveServiceBadge
import com.subtrack.core.ui.components.ExpressiveSettingsActionButton
import com.subtrack.core.ui.components.ExpressiveTonalPalette
import com.subtrack.core.ui.components.ShapeBloomBackdrop
import com.subtrack.core.ui.components.expressiveCardBorder
import com.subtrack.core.ui.components.expressivePolygonSurface
import com.subtrack.core.ui.components.progressiveEdgeFade
import com.subtrack.core.ui.theme.DisplayFontFamily
import com.subtrack.core.ui.theme.ExpressiveMotion
import com.subtrack.core.ui.theme.LocalSubCyclerDarkTheme
import com.subtrack.core.ui.theme.orSnapIfReduced
import com.subtrack.core.ui.theme.rememberReducedMotionEnabled
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun AnalyticsRoute(
    viewModel: AnalyticsViewModel = hiltViewModel(),
    isEntranceReady: Boolean = true,
    onOpenSettings: (() -> Unit)? = null,
    onOpenPairedSwap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onAction(AnalyticsAction.RefreshToday)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    AnalyticsScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        isEntranceReady = isEntranceReady,
        onOpenSettings = onOpenSettings,
        onOpenPairedSwap = onOpenPairedSwap,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    uiState: AnalyticsUiState,
    onAction: (AnalyticsAction) -> Unit,
    isEntranceReady: Boolean = true,
    onOpenSettings: (() -> Unit)? = null,
    onOpenPairedSwap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isDark = LocalSubCyclerDarkTheme.current
    val sagePalette = ExpressivePalettes.softSage()
    val skyPalette = ExpressivePalettes.softSky()
    val lavenderPalette = ExpressivePalettes.softLavender()
    val boldPalette = ExpressivePalettes.boldPrimary()
    val neutralPalette = ExpressiveTonalPalette(
        container = MaterialTheme.colorScheme.surfaceContainer,
        selectedContainer = MaterialTheme.colorScheme.surfaceContainerHigh,
        content = MaterialTheme.colorScheme.onSurface,
        accent = MaterialTheme.colorScheme.onSurfaceVariant,
        border = MaterialTheme.colorScheme.outlineVariant
    )
    val cardBorder = expressiveCardBorder()
    val heroPillBg = if (isDark) {
        MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.72f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.88f)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    ExpressiveScreenTitle(text = "Savings")
                },
                actions = {
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
        val reducedMotion = rememberReducedMotionEnabled()
        val skipEntrance = isEntranceReady || reducedMotion
        val heroReveal = remember { Animatable(if (skipEntrance) 1f else 0f) }
        val arcDrawReveal = remember { Animatable(if (skipEntrance) 1f else 0f) }

        LaunchedEffect(isEntranceReady, reducedMotion) {
            if (!isEntranceReady) return@LaunchedEffect
            if (reducedMotion) {
                heroReveal.snapTo(1f)
                arcDrawReveal.snapTo(1f)
                return@LaunchedEffect
            }
            if (heroReveal.value >= 0.99f && arcDrawReveal.value >= 0.99f) return@LaunchedEffect
            launch {
                heroReveal.animateTo(1f, ExpressiveMotion.expressiveSpatialDefaultFloat())
            }
            launch {
                delay(70L)
                arcDrawReveal.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = 0.82f,
                        stiffness = 280f,
                        visibilityThreshold = 0.001f
                    )
                )
            }
        }
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
        ) {
            ShapeBloomBackdrop()
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .progressiveEdgeFade(bottomFadeHeight = 28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 120.dp + navBarBottom)
            ) {
            if (uiState.excludedCurrencyCount > 0) {
                item(key = "currency_scope_notice", contentType = "notice") {
                    CurrencyScopeNotice(
                        currencyCode = uiState.currencyCode,
                        excludedCurrencyCount = uiState.excludedCurrencyCount
                    )
                }
            }

            if (uiState.summary.totalSubscriptions == 0) {
                item(key = "savings_empty", contentType = "empty") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = cardBorder
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
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
                                text = "See your 5-year savings",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap '+' below to add your subscriptions. Rotating streaming & AI tools on 45-day cycles cuts annual costs by ~32%.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // 1. Pure Flat Color-Block 5-Year Hero Card with Triadic Growth Curve
                item(key = "hero_savings", contentType = "hero") {
                    val hasSavings = uiState.fiveYearSavings > 0.0
                    val savingsPct = uiState.summary.overallSavingsPercentage.roundToInt()
                    val rotatingCount = uiState.rotatingContributions.size
                    val accentTokens = com.subtrack.core.ui.theme.LocalSubCyclerAccentTokens.current
                    val savingsHeroInk = accentTokens.heroContent
                    val savingsHeroPillBg = if (isDark) {
                        Color.Black.copy(alpha = 0.28f)
                    } else {
                        Color.White.copy(alpha = 0.22f)
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
                                .clip(RoundedCornerShape(28.dp))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 20.dp)
                            ) {
                                ExpressiveEyebrow(
                                    text = "PROJECTED 5-YEAR SAVINGS",
                                    color = savingsHeroInk.copy(alpha = 0.80f)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        ExpressiveMoneyText(
                                            formattedAmount = MoneyFormatter.format(
                                                uiState.fiveYearSavings,
                                                uiState.currencyCode,
                                                maximumFractionDigits = 0
                                            ),
                                            heroFontSize = 46.sp,
                                            color = savingsHeroInk
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Surface(
                                            shape = RoundedCornerShape(999.dp),
                                            color = savingsHeroPillBg
                                        ) {
                                            Text(
                                                text = if (hasSavings) {
                                                    "$savingsPct% cut · $rotatingCount rotating"
                                                } else {
                                                    "All always-on · ${uiState.summary.totalSubscriptions} tracked"
                                                },
                                                color = savingsHeroInk,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(10.dp))
                                    if (hasSavings) {
                                        CompoundingGrowthArc(
                                            strokeColor = accentTokens.heroPop,
                                            nodeColor = savingsHeroInk,
                                            drawProgress = arcDrawReveal.value,
                                            modifier = Modifier
                                                .width(96.dp)
                                                .height(74.dp)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(90.dp)
                                                .expressivePolygonSurface(
                                                    shape = ExpressiveCookie12Shape,
                                                    color = accentTokens.heroPop
                                                )
                                                .clickable {
                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    onOpenPairedSwap()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center,
                                                modifier = Modifier.padding(horizontal = 8.dp)
                                            ) {
                                                Text(
                                                    text = "ROTATE",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    letterSpacing = 1.sp,
                                                    color = accentTokens.onHeroPop.copy(alpha = 0.78f)
                                                )
                                                Text(
                                                    text = "-32%",
                                                    fontFamily = DisplayFontFamily,
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Black,
                                                    letterSpacing = (-0.5).sp,
                                                    color = accentTokens.onHeroPop
                                                )
                                                Text(
                                                    text = "possible",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = accentTokens.onHeroPop.copy(alpha = 0.78f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Stepped Horizon Bento Row: EARNED / 1 YEAR / 3 YEARS
                item(key = "horizon_row", contentType = "card") {
                    val hasEarned = uiState.totalEarnedSavings > 0.0
                    val earnedFraction = if (uiState.summary.totalAnnualSavings > 0.0) {
                        (uiState.totalEarnedSavings / uiState.summary.totalAnnualSavings).toFloat().coerceIn(0f, 1f)
                    } else 0f

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SavingsHorizonCard(
                            label = if (uiState.totalCompletedBreaks > 0) "EARNED (${uiState.totalCompletedBreaks})" else "EARNED",
                            amount = uiState.totalEarnedSavings,
                            currencyCode = uiState.currencyCode,
                            palette = if (hasEarned) boldPalette else neutralPalette,
                            progressFraction = if (hasEarned) earnedFraction.coerceAtLeast(0.18f) else 0.08f,
                            isEntranceReady = isEntranceReady,
                            entranceIndex = 0,
                            caption = if (!hasEarned) "Bank 1st break" else null,
                            border = if (hasEarned) null else cardBorder,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        SavingsHorizonCard(
                            label = "1 YEAR",
                            amount = uiState.summary.totalAnnualSavings,
                            currencyCode = uiState.currencyCode,
                            palette = ExpressivePalettes.softOcean(),
                            progressFraction = 0.28f,
                            isEntranceReady = isEntranceReady,
                            entranceIndex = 1,
                            border = cardBorder,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                        SavingsHorizonCard(
                            label = "3 YEARS",
                            amount = uiState.threeYearSavings,
                            currencyCode = uiState.currencyCode,
                            palette = lavenderPalette,
                            progressFraction = 0.64f,
                            isEntranceReady = isEntranceReady,
                            entranceIndex = 2,
                            border = cardBorder,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                        )
                    }
                }

                // 3. Per-Service Connected Vessel Savings Stack (28dp outer / 4dp inner seams + proportional tonal vessel!)
                if (uiState.rotatingContributions.isNotEmpty()) {
                    item(key = "savings_breakdown", contentType = "card") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ExpressiveEyebrow(
                                    text = "SAVINGS BREAKDOWN",
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "${MoneyFormatter.format(uiState.summary.totalAnnualSavings, uiState.currencyCode, maximumFractionDigits = 0)}/yr",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            ExpressiveSectionStack {
                                uiState.rotatingContributions.forEachIndexed { index, item ->
                                    val sub = item.subscription
                                    val subPalette = remember(sub.name, sub.colorHex, isDark) {
                                        com.subtrack.core.ui.theme.BrandColorHarmonizer.harmonizedTonalPalette(
                                            serviceName = sub.name,
                                            storedColorHex = sub.colorHex,
                                            isDark = isDark
                                        )
                                    }
                                    val vesselColor = subPalette.accent.copy(
                                        alpha = if (isDark) 0.19f else 0.13f
                                    )
                                    val vesselSeam = subPalette.accent.copy(
                                        alpha = if (isDark) 0.72f else 0.62f
                                    )
                                    val rowSweep = remember(sub.id) { Animatable(if (skipEntrance) 1f else 0f) }
                                    LaunchedEffect(isEntranceReady, reducedMotion, sub.id) {
                                        if (!isEntranceReady) return@LaunchedEffect
                                        if (reducedMotion) {
                                            rowSweep.snapTo(1f)
                                        } else if (rowSweep.value < 0.99f) {
                                            delay(80L + index.coerceAtMost(5) * 55L)
                                            rowSweep.animateTo(1f, ExpressiveMotion.expressiveSpatialDefaultFloat())
                                        }
                                    }
                                    val animatedShare by animateFloatAsState(
                                        targetValue = item.shareFraction.coerceIn(0.06f, 1f),
                                        animationSpec = ExpressiveMotion.expressiveSpatialDefaultFloat()
                                            .orSnapIfReduced(reducedMotion),
                                        label = "breakdownShare"
                                    )
                                    val cycleNote = when (sub.strategy) {
                                        RotationStrategy.ROTATION_45_DAYS -> "30d on · 15d off"
                                        RotationStrategy.ROTATION_60_DAYS -> "30d on · 30d off"
                                        RotationStrategy.CUSTOM -> "${sub.activeDays}d on · ${sub.blackoutDays}d off"
                                        else -> "Rotating"
                                    }

                                    ExpressiveSectionRow(
                                        index = index,
                                        count = uiState.rotatingContributions.size,
                                        outerRadius = 24.dp,
                                        innerRadius = 4.dp
                                    ) {
                                        // Proportional horizontal brand-tinted vessel sweeping behind the service row
                                        Canvas(modifier = Modifier.matchParentSize()) {
                                            val fillW = (size.width * animatedShare * rowSweep.value.coerceIn(0f, 1.03f))
                                                .coerceIn(0f, size.width)
                                            drawRect(
                                                color = vesselColor,
                                                topLeft = Offset.Zero,
                                                size = androidx.compose.ui.geometry.Size(fillW, size.height)
                                            )
                                            if (fillW in 4f..(size.width - 4f)) {
                                                drawLine(
                                                    color = vesselSeam,
                                                    start = Offset(fillW, 0f),
                                                    end = Offset(fillW, size.height),
                                                    strokeWidth = 1.5.dp.toPx()
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            ExpressiveServiceBadge(
                                                serviceName = sub.name,
                                                colorHex = sub.colorHex,
                                                size = 38.dp,
                                                fontSize = 12.sp
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = sub.name,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "$cycleNote · ${(item.shareFraction * 100).roundToInt()}% of savings",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "${MoneyFormatter.format(item.annualSavings, sub.currency, maximumFractionDigits = 0)}/yr",
                                                fontFamily = DisplayFontFamily,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 15.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 4. Contradiction-Free What-If Optimizer Card (or Actionable All-Rotating Celebration Card)
                item(key = "simulator_card", contentType = "card") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(26.dp),
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        border = cardBorder
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            if (uiState.opportunities.isEmpty()) {
                                val allRotating = uiState.monthlySubscriptionCount > 0
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .expressivePolygonSurface(
                                                shape = ExpressiveClover4Shape,
                                                color = sagePalette.container
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (allRotating) Icons.Default.Check else Icons.Default.Autorenew,
                                            contentDescription = null,
                                            tint = sagePalette.content,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (allRotating) "All monthly plans rotating" else "What-If Simulator",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (allRotating) {
                                                "Push savings up to ~49% by alternating two services every 30 days."
                                            } else {
                                                "Add a monthly subscription to simulate 45-day rotation savings."
                                            },
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (allRotating && uiState.monthlySubscriptionCount >= 2) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    FilledTonalButton(
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onOpenPairedSwap()
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp),
                                        shape = RoundedCornerShape(999.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    ) {
                                        Text("Try Paired Swap", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = "What-If Simulator",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "See your total annual savings if you also rotate your continuous monthly plans on 45-day cycles.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Rotate continuous plans",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = skyPalette.container
                                    ) {
                                        Text(
                                            text = "${(uiState.simulatedRotationPct * 100).roundToInt()}%",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            color = skyPalette.content,
                                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Slider(
                                    value = uiState.simulatedRotationPct,
                                    onValueChange = { rawValue ->
                                        val snapped = (rawValue * 20f).roundToInt() / 20f
                                        if (snapped != uiState.simulatedRotationPct) {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            onAction(AnalyticsAction.SetSimulatedRotationPct(snapped))
                                        }
                                    },
                                    valueRange = 0f..1f,
                                    steps = 0,
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                                    )
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = sagePalette.container,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Projected annual total",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = sagePalette.content
                                            )
                                            if (uiState.additionalPotentialAnnualSavings > 0.5) {
                                                Text(
                                                    text = "+${MoneyFormatter.format(uiState.additionalPotentialAnnualSavings, uiState.currencyCode, maximumFractionDigits = 0)}/yr extra",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = sagePalette.content.copy(alpha = 0.78f)
                                                )
                                            }
                                        }
                                        ExpressiveMoneyText(
                                            formattedAmount = MoneyFormatter.format(uiState.simulatedAnnualSavings, uiState.currencyCode, maximumFractionDigits = 0),
                                            heroFontSize = 22.sp,
                                            cadenceSuffix = "/yr",
                                            color = sagePalette.content,
                                            elevateCurrencySymbol = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Actionable Opportunity Connected Stack (1-Tap "Use 45d" for Continuous Monthly plans)
                if (uiState.opportunities.isNotEmpty()) {
                    item(key = "opportunities_card", contentType = "card") {
                        ExpressiveSectionStack(eyebrow = "SAVINGS OPPORTUNITIES") {
                            uiState.opportunities.forEachIndexed { index, opportunity ->
                                val sub = opportunity.subscription
                                ExpressiveSectionRow(
                                    index = index,
                                    count = uiState.opportunities.size,
                                    outerRadius = 24.dp,
                                    innerRadius = 4.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 13.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            ExpressiveServiceBadge(
                                                serviceName = sub.name,
                                                size = 40.dp,
                                                fontSize = 13.sp
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = sub.name,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "+${MoneyFormatter.format(opportunity.estimatedAnnualSavings, sub.currency, maximumFractionDigits = 0)}/yr on 45d cycle",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        FilledTonalButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onAction(AnalyticsAction.SwitchToSmartRotation(sub.id))
                                            },
                                            shape = RoundedCornerShape(999.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                            colors = ButtonDefaults.filledTonalButtonColors(
                                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        ) {
                                            Text("Use 45d", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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

/**
 * Botanical compounding trajectory curve inside the 5-Year Savings Hero card
 * showing 1Y -> 3Y -> 5Y growth nodes.
 */
@Composable
private fun CompoundingGrowthArc(
    strokeColor: Color,
    nodeColor: Color,
    drawProgress: Float = 1f,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val p0 = Offset(w * 0.08f, h * 0.84f)
        val p1 = Offset(w * 0.50f, h * 0.52f)
        val p2 = Offset(w * 0.88f, h * 0.16f)
        val t = ExpressiveMotion.clampEffectProgress(drawProgress)

        val curvePath = Path().apply {
            moveTo(p0.x, p0.y)
            cubicTo(
                w * 0.28f, h * 0.78f,
                w * 0.36f, h * 0.58f,
                p1.x, p1.y
            )
            cubicTo(
                w * 0.66f, h * 0.44f,
                w * 0.76f, h * 0.24f,
                p2.x, p2.y
            )
        }

        val fillPath = Path().apply {
            addPath(curvePath)
            lineTo(p2.x, h * 0.92f)
            lineTo(p0.x, h * 0.92f)
            close()
        }

        // Smooth left-to-right growth curve sweep on first Savings visit
        clipRect(left = 0f, top = 0f, right = w * t, bottom = h) {
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        strokeColor.copy(alpha = 0.26f),
                        strokeColor.copy(alpha = 0.02f)
                    )
                )
            )
            drawPath(
                path = curvePath,
                color = strokeColor.copy(alpha = 0.90f),
                style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // Sequential milestone dot pops (1Y -> 3Y -> 5Y glowing peak)
        val d0 = (t / 0.25f).coerceIn(0f, 1f)
        val d1 = ((t - 0.38f) / 0.25f).coerceIn(0f, 1f)
        val d2 = ((t - 0.72f) / 0.28f).coerceIn(0f, 1f)
        if (d0 > 0.02f) drawCircle(color = nodeColor.copy(alpha = 0.68f * d0), radius = 3.2.dp.toPx() * d0, center = p0)
        if (d1 > 0.02f) drawCircle(color = nodeColor.copy(alpha = 0.84f * d1), radius = 3.8.dp.toPx() * d1, center = p1)
        if (d2 > 0.02f) {
            val peakPop = (d2 * (if (drawProgress > 1f) 1.12f else 1f)).coerceAtMost(1.18f)
            drawCircle(color = strokeColor.copy(alpha = 0.30f * d2), radius = 9.5.dp.toPx() * peakPop, center = p2)
            drawCircle(color = strokeColor.copy(alpha = d2), radius = 5.dp.toPx() * peakPop, center = p2)
        }
    }
}

@Composable
private fun SavingsHorizonCard(
    label: String,
    amount: Double,
    currencyCode: String,
    palette: ExpressiveTonalPalette,
    progressFraction: Float,
    modifier: Modifier = Modifier,
    isEntranceReady: Boolean = true,
    entranceIndex: Int = 0,
    caption: String? = null,
    border: BorderStroke? = null
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val skip = isEntranceReady || reducedMotion
    val barReveal = remember { Animatable(if (skip) 1f else 0f) }
    LaunchedEffect(isEntranceReady, reducedMotion) {
        if (!isEntranceReady) return@LaunchedEffect
        if (reducedMotion) {
            barReveal.snapTo(1f)
        } else if (barReveal.value < 0.99f) {
            delay(50L + entranceIndex * 45L)
            barReveal.animateTo(1f, ExpressiveMotion.expressiveSpatialDefaultFloat())
        }
    }
    val animatedFill = (progressFraction.coerceIn(0.08f, 1f) * barReveal.value.coerceIn(0f, 1.04f))
        .coerceIn(0.01f, 1f)

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = palette.container,
        border = border
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                ExpressiveEyebrow(
                    text = label,
                    color = palette.content.copy(alpha = 0.76f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                ExpressiveMoneyText(
                    formattedAmount = MoneyFormatter.format(amount, currencyCode, maximumFractionDigits = 0),
                    heroFontSize = 21.sp,
                    color = palette.content,
                    elevateCurrencySymbol = false
                )
                if (caption != null) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = caption,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Medium,
                        color = palette.content.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.5.dp)
                    .clip(CircleShape)
                    .background(palette.content.copy(alpha = 0.14f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedFill)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(palette.accent)
                )
            }
        }
    }
}
