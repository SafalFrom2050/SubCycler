package com.subtrack.app.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subtrack.core.domain.model.AppLauncherIcon
import com.subtrack.core.ui.components.ExpressiveBrandWordmark
import com.subtrack.core.ui.components.ExpressiveMaterialPolygons
import com.subtrack.core.ui.components.ExpressiveScreenBackdrop
import com.subtrack.core.ui.components.ReactiveIcon
import com.subtrack.core.ui.components.ReactiveMotionType
import com.subtrack.core.ui.components.SubCyclerSwirlLogo
import com.subtrack.core.ui.components.expressivePolygonSurface
import com.subtrack.core.ui.components.rememberMorphingPolygonShape
import com.subtrack.core.ui.theme.BodyFontFamily
import com.subtrack.core.ui.theme.BrandColorHarmonizer
import com.subtrack.core.ui.theme.ExpressiveMotion
import com.subtrack.core.ui.theme.LocalSubCyclerDarkTheme
import com.subtrack.core.ui.theme.orSnapIfReduced
import com.subtrack.core.ui.theme.rememberReducedMotionEnabled
import com.subtrack.feature.analytics.AnalyticsRoute
import com.subtrack.feature.dashboard.DashboardRoute
import com.subtrack.feature.onboarding.OnboardingRoute
import com.subtrack.feature.planner.PlannerRoute
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun SubTrackApp(
    onRequestNotificationPermission: () -> Unit = {},
    openSubscriptionId: Long? = null,
    onOpenSubscriptionConsumed: () -> Unit = {},
    pendingTransferPayload: String? = null,
    onTransferPayloadConsumed: () -> Unit = {},
    onApplyLauncherIcon: (AppLauncherIcon) -> Unit = {},
    onPreviewNotifications: () -> Unit = {},
    appViewModel: AppViewModel = hiltViewModel()
) {
    val launchState by appViewModel.launchState.collectAsStateWithLifecycle()
    val dockMood by appViewModel.dockMood.collectAsStateWithLifecycle()
    var isDashboardHydrated by remember { mutableStateOf(false) }
    var splashDismissed by remember { mutableStateOf(false) }

    LaunchedEffect(launchState, pendingTransferPayload) {
        if (launchState == AppLaunchState.ONBOARDING && !pendingTransferPayload.isNullOrBlank()) {
            appViewModel.onOnboardingFinished()
        }
    }

    val isUnderlyingContentReady = when (launchState) {
        AppLaunchState.LOADING -> false
        AppLaunchState.ONBOARDING -> dockMood.isLoaded
        AppLaunchState.MAIN_CONTENT -> dockMood.isLoaded && isDashboardHydrated
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (launchState) {
            AppLaunchState.LOADING -> {
                // Keep canvas clean behind the animated brand splash overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                )
            }
            AppLaunchState.ONBOARDING -> {
                OnboardingRoute(
                    onFinished = { requestNotificationPermission ->
                        appViewModel.onOnboardingFinished()
                        if (requestNotificationPermission) {
                            onRequestNotificationPermission()
                        }
                    }
                )
            }
            AppLaunchState.MAIN_CONTENT -> SubTrackMainNavigation(
                openSubscriptionId = openSubscriptionId,
                onOpenSubscriptionConsumed = onOpenSubscriptionConsumed,
                pendingTransferPayload = pendingTransferPayload,
                onTransferPayloadConsumed = onTransferPayloadConsumed,
                onApplyLauncherIcon = onApplyLauncherIcon,
                onPreviewNotifications = onPreviewNotifications,
                dockMood = dockMood,
                isSplashDismissed = splashDismissed,
                onDashboardFirstDataReady = { isDashboardHydrated = true }
            )
        }

        if (!splashDismissed) {
            SubCyclerExpressiveSplashOverlay(
                dockMood = dockMood,
                isContentReady = isUnderlyingContentReady,
                onSplashFinished = { splashDismissed = true }
            )
        }
    }
}

/**
 * Branded Material 3 Expressive Launch Splash:
 * 1. Spins the user's selected launcher swirl icon smoothly while Room + DataStore hydrate underneath.
 * 2. Once hydrated, decelerates cleanly to upright and morphs the 3 petals into the live Stack Mood
 *    swirl colorway with a spring bloom + wordmark reveal.
 * 3. Fades straight into the already-populated Home screen—eliminating any flash of empty state.
 */
@Composable
private fun SubCyclerExpressiveSplashOverlay(
    dockMood: DockMoodState,
    isContentReady: Boolean,
    onSplashFinished: () -> Unit
) {
    val reducedMotion = rememberReducedMotionEnabled()
    val isDark = LocalSubCyclerDarkTheme.current
    val currentReady by rememberUpdatedState(isContentReady)
    val currentDockLoaded by rememberUpdatedState(dockMood.isLoaded)
    val currentOnFinished by rememberUpdatedState(onSplashFinished)

    val rotation = remember { Animatable(0f) }
    val logoScale = remember { Animatable(if (reducedMotion) 1f else 0.86f) }
    val wordmarkAlpha = remember { Animatable(0f) }
    val overlayAlpha = remember { Animatable(1f) }
    val overlayScale = remember { Animatable(1f) }
    var morphedToStackMood by remember { mutableStateOf(false) }

    val launcherSwirl = remember(dockMood.launcherIcon) {
        BrandColorHarmonizer.swirlColorsForLauncherIcon(dockMood.launcherIcon)
    }
    val stackMoodSwirl = remember(dockMood.activeSubscriptions, dockMood.useStackHeroMood, dockMood.launcherIcon) {
        BrandColorHarmonizer.resolveSwirlPetalColors(
            activeSubscriptions = dockMood.activeSubscriptions,
            useStackMood = dockMood.useStackHeroMood,
            fallbackIcon = dockMood.launcherIcon
        )
    }
    val activeSwirlColors = if (morphedToStackMood) stackMoodSwirl else launcherSwirl
    val auraColor by animateColorAsState(
        targetValue = activeSwirlColors.midPetal.copy(alpha = if (isDark) 0.24f else 0.28f),
        animationSpec = ExpressiveMotion.effectsDefaultColor().orSnapIfReduced(reducedMotion),
        label = "splashAuraColor"
    )

    LaunchedEffect(reducedMotion) {
        while (!currentDockLoaded) {
            delay(16L)
        }
        if (reducedMotion) {
            while (!currentReady) {
                delay(24L)
            }
            morphedToStackMood = true
            currentOnFinished()
            return@LaunchedEffect
        }

        // Phase 1: Spring pop-in + crisp 360° material icon rotation while data loads
        val popJob = launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = ExpressiveMotion.expressiveSpatialFastFloat()
            )
        }
        rotation.animateTo(
            targetValue = 360f,
            animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing)
        )
        popJob.join()

        // If Room/DataStore is still hydrating on a cold boot, continue smooth steady spin
        while (!currentReady) {
            rotation.animateTo(
                targetValue = rotation.value + 120f,
                animationSpec = tween(durationMillis = 220, easing = LinearEasing)
            )
        }

        // Phase 2: Settle upright to nearest 360° multiple & morph into live Stack Mood swirl!
        val nextUpright = ((rotation.value / 360f).toInt() + if (rotation.value % 360f > 5f) 1 else 0) * 360f
        morphedToStackMood = true
        val settleSpin = launch {
            if (nextUpright > rotation.value) {
                rotation.animateTo(
                    targetValue = nextUpright,
                    animationSpec = ExpressiveMotion.expressiveSpatialDefaultFloat()
                )
            }
        }
        val bloomPulse = launch {
            logoScale.animateTo(
                targetValue = 1.09f,
                animationSpec = ExpressiveMotion.expressiveSpatialFastFloat()
            )
            logoScale.animateTo(
                targetValue = 1.0f,
                animationSpec = ExpressiveMotion.expressiveSpatialDefaultFloat()
            )
        }
        val revealWordmark = launch {
            wordmarkAlpha.animateTo(
                targetValue = 1f,
                animationSpec = ExpressiveMotion.effectsFast()
            )
        }
        settleSpin.join()
        bloomPulse.join()
        revealWordmark.join()
        delay(220L)

        // Phase 3: Clean lift & dissolve straight into hydrated Home
        val liftJob = launch {
            overlayScale.animateTo(
                targetValue = 1.05f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
            )
        }
        overlayAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing)
        )
        liftJob.join()
        currentOnFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = overlayAlpha.value
                scaleX = overlayScale.value
                scaleY = overlayScale.value
            }
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        ExpressiveScreenBackdrop {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                if (dockMood.isLoaded) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(164.dp)
                                .drawBehind {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(auraColor, Color.Transparent),
                                            center = center,
                                            radius = size.minDimension * 0.52f
                                        )
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            SubCyclerSwirlLogo(
                                colors = activeSwirlColors,
                                size = 96.dp,
                                cornerRadius = 28.dp,
                                rotationDegrees = rotation.value,
                                contentDescriptionText = "SubCycler loading",
                                modifier = Modifier.graphicsLayer {
                                    scaleX = logoScale.value
                                    scaleY = logoScale.value
                                }
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier.graphicsLayer {
                                alpha = wordmarkAlpha.value
                            }
                        ) {
                            ExpressiveBrandWordmark(fontSize = 30.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubTrackMainNavigation(
    openSubscriptionId: Long? = null,
    onOpenSubscriptionConsumed: () -> Unit = {},
    pendingTransferPayload: String? = null,
    onTransferPayloadConsumed: () -> Unit = {},
    onApplyLauncherIcon: (AppLauncherIcon) -> Unit = {},
    onPreviewNotifications: () -> Unit = {},
    dockMood: DockMoodState = DockMoodState(),
    isSplashDismissed: Boolean = true,
    onDashboardFirstDataReady: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val reducedMotion = rememberReducedMotionEnabled()
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var outgoingTabIndex by remember { mutableIntStateOf(-1) }
    var transitionDirection by remember { mutableIntStateOf(1) }
    val transitionProgress = remember { Animatable(1f) }
    var isPlannerMounted by remember { mutableStateOf(false) }
    var isAnalyticsMounted by remember { mutableStateOf(false) }

    val isOnDashboard = selectedTabIndex == 0
    var pendingAddSubscription by remember { mutableStateOf(false) }
    var pendingOpenPairedSwap by remember { mutableStateOf(false) }
    var internalOpenSubId by remember { mutableStateOf<Long?>(null) }
    val effectiveOpenSubId = openSubscriptionId ?: internalOpenSubId
    val isDark = LocalSubCyclerDarkTheme.current

    fun selectTabIndex(targetIndex: Int) {
        val clamped = targetIndex.coerceIn(0, NavigationItems.items.lastIndex)
        if (clamped == 1) isPlannerMounted = true
        if (clamped == 2) isAnalyticsMounted = true
        if (clamped == selectedTabIndex) return
        outgoingTabIndex = selectedTabIndex
        transitionDirection = if (clamped >= selectedTabIndex) 1 else -1
        selectedTabIndex = clamped
    }

    // Gently pre-warm secondary tabs after the launch splash clears so even the very first tap
    // switches immediately via pure GPU layer glide with zero composition stall.
    LaunchedEffect(isSplashDismissed) {
        if (!isSplashDismissed) return@LaunchedEffect
        if (!isPlannerMounted) {
            delay(180L)
            isPlannerMounted = true
        }
        if (!isAnalyticsMounted) {
            delay(180L)
            isAnalyticsMounted = true
        }
    }

    LaunchedEffect(selectedTabIndex, reducedMotion) {
        if (outgoingTabIndex < 0 || outgoingTabIndex == selectedTabIndex) {
            transitionProgress.snapTo(1f)
            outgoingTabIndex = -1
            return@LaunchedEffect
        }
        if (reducedMotion) {
            transitionProgress.snapTo(1f)
            outgoingTabIndex = -1
            return@LaunchedEffect
        }
        transitionProgress.snapTo(0f)
        transitionProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 155, easing = FastOutSlowInEasing)
        )
        outgoingTabIndex = -1
    }

    BackHandler(enabled = selectedTabIndex != 0) {
        selectTabIndex(0)
    }

    // Ensure notification, widget, and transfer deep-links switch back to Subs from Planner/Savings
    LaunchedEffect(effectiveOpenSubId, pendingTransferPayload) {
        if ((effectiveOpenSubId != null || !pendingTransferPayload.isNullOrBlank()) && !isOnDashboard) {
            selectTabIndex(0)
        }
    }

    // Subtle Living Stack Mood harmonizing for the active dock indicator pill when enabled
    val useLivingMoodTint = dockMood.useStackHeroMood && dockMood.activeSubscriptions.isNotEmpty()
    val heroMoodPalette = remember(dockMood.activeSubscriptions, dockMood.useStackHeroMood, dockMood.launcherIcon, isDark) {
        if (useLivingMoodTint) {
            BrandColorHarmonizer.resolveHeroMoodPalette(
                activeSubscriptions = dockMood.activeSubscriptions,
                useStackMood = true,
                isDark = isDark,
                fallbackIcon = dockMood.launcherIcon
            )
        } else {
            null
        }
    }

    val targetIndicatorBg = if (heroMoodPalette != null) {
        lerp(MaterialTheme.colorScheme.primaryContainer, heroMoodPalette.gradientStart, 0.78f)
    } else {
        MaterialTheme.colorScheme.primaryContainer
    }
    val targetIndicatorInk = heroMoodPalette?.contentColor ?: MaterialTheme.colorScheme.onPrimaryContainer

    val animatedIndicatorBg by animateColorAsState(
        targetValue = targetIndicatorBg,
        animationSpec = ExpressiveMotion.effectsFastColor().orSnapIfReduced(reducedMotion),
        label = "dockIndicatorBg"
    )
    val animatedIndicatorInk by animateColorAsState(
        targetValue = targetIndicatorInk,
        animationSpec = ExpressiveMotion.effectsFastColor().orSnapIfReduced(reducedMotion),
        label = "dockIndicatorInk"
    )

    val isTransitioning = outgoingTabIndex >= 0

    ExpressiveScreenBackdrop {
        // True edge-to-edge scroll-under: warm-mounted GPU tab slots keep scroll & ViewModels live
        // inside an isolated stage Box so their z-ordering never competes with the floating dock.
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxSize()) {
                WarmTabSlot(
                    tabIndex = 0,
                    selectedTabIndex = selectedTabIndex,
                    outgoingTabIndex = outgoingTabIndex,
                    isTransitioning = isTransitioning,
                    transitionDirection = transitionDirection,
                    transitionProgressProvider = { transitionProgress.value }
                ) {
                    DashboardRoute(
                        openSubscriptionId = effectiveOpenSubId,
                        onOpenSubscriptionConsumed = {
                            if (openSubscriptionId != null) {
                                onOpenSubscriptionConsumed()
                            }
                            internalOpenSubId = null
                        },
                        pendingTransferPayload = pendingTransferPayload,
                        onTransferPayloadConsumed = onTransferPayloadConsumed,
                        pendingAddSubscription = pendingAddSubscription,
                        onAddSubscriptionConsumed = { pendingAddSubscription = false },
                        onNavigateToSavings = { selectTabIndex(2) },
                        onApplyLauncherIcon = onApplyLauncherIcon,
                        onPreviewNotifications = onPreviewNotifications,
                        onFirstDataReady = onDashboardFirstDataReady
                    )
                }

                if (isPlannerMounted) {
                    WarmTabSlot(
                        tabIndex = 1,
                        selectedTabIndex = selectedTabIndex,
                        outgoingTabIndex = outgoingTabIndex,
                        isTransitioning = isTransitioning,
                        transitionDirection = transitionDirection,
                        transitionProgressProvider = { transitionProgress.value }
                    ) {
                        PlannerRoute(
                            pendingOpenPairedSwap = pendingOpenPairedSwap,
                            onOpenPairedSwapConsumed = { pendingOpenPairedSwap = false },
                            onOpenSubscription = { subId ->
                                internalOpenSubId = subId
                                selectTabIndex(0)
                            }
                        )
                    }
                }

                if (isAnalyticsMounted) {
                    WarmTabSlot(
                        tabIndex = 2,
                        selectedTabIndex = selectedTabIndex,
                        outgoingTabIndex = outgoingTabIndex,
                        isTransitioning = isTransitioning,
                        transitionDirection = transitionDirection,
                        transitionProgressProvider = { transitionProgress.value }
                    ) {
                        AnalyticsRoute(
                            onOpenPairedSwap = {
                                pendingOpenPairedSwap = true
                                selectTabIndex(1)
                            }
                        )
                    }
                }
            }

            // Smooth 4-stop bottom fade scrim so scrolling cards glide cleanly behind the floating dock
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(132.dp)
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.42f to MaterialTheme.colorScheme.background.copy(alpha = 0.56f),
                            0.78f to MaterialTheme.colorScheme.background.copy(alpha = 0.90f),
                            1.0f to MaterialTheme.colorScheme.background.copy(alpha = 0.99f)
                        )
                    )
            )

            // Material 3 Expressive v4.0 Gliding Liquid Indicator Dock + Morphing Scalloped '+' FAB
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .widthIn(max = 368.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlidingLiquidNavDock(
                        items = NavigationItems.items,
                        selectedIndex = selectedTabIndex,
                        indicatorColor = animatedIndicatorBg,
                        indicatorContentColor = animatedIndicatorInk,
                        isDark = isDark,
                        reducedMotion = reducedMotion,
                        onSelectTab = { index -> selectTabIndex(index) },
                        modifier = Modifier.weight(1f)
                    )

                    // Detached 12-Petal Scalloped '+' Action Button with anticipatory press-preview morph (Cookie12 -> Sunny!)
                    val fabInteraction = remember { MutableInteractionSource() }
                    val fabPressed by fabInteraction.collectIsPressedAsState()
                    var fabTapCount by remember { mutableIntStateOf(0) }
                    val fabShape = rememberMorphingPolygonShape(
                        targetPolygon = if (fabPressed) {
                            ExpressiveMaterialPolygons.Sunny
                        } else {
                            ExpressiveMaterialPolygons.Cookie12
                        }
                    )
                    val fabScale by animateFloatAsState(
                        targetValue = if (fabPressed) 0.89f else 1f,
                        animationSpec = ExpressiveMotion.expressiveSpatialFastFloat()
                            .orSnapIfReduced(reducedMotion),
                        label = "detachedFabScale"
                    )
                    val fabCookieRotation by animateFloatAsState(
                        targetValue = if (fabPressed) 18f else 0f,
                        animationSpec = ExpressiveMotion.expressiveSpatialFastFloat()
                            .orSnapIfReduced(reducedMotion),
                        label = "detachedFabRot"
                    )
                    val fabBgColor by animateColorAsState(
                        targetValue = if (isOnDashboard) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        animationSpec = ExpressiveMotion.effectsFastColor()
                            .orSnapIfReduced(reducedMotion),
                        label = "detachedFabBg"
                    )
                    val fabIconColor by animateColorAsState(
                        targetValue = if (isOnDashboard) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        },
                        animationSpec = ExpressiveMotion.effectsFastColor()
                            .orSnapIfReduced(reducedMotion),
                        label = "detachedFabIcon"
                    )

                    val fabShadowColor = Color.Black.copy(alpha = if (isDark) 0.36f else 0.18f)
                    val fabRimColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.38f else 0.28f)
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .drawBehind {
                                // Custom soft radial shadow for Outline.Generic scalloped cookie on all Android APIs
                                val shadowRadius = size.minDimension * 0.64f
                                val shadowCenter = Offset(size.width / 2f, size.height / 2f + 4.dp.toPx())
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            fabShadowColor,
                                            fabShadowColor.copy(alpha = fabShadowColor.alpha * 0.45f),
                                            Color.Transparent
                                        ),
                                        center = shadowCenter,
                                        radius = shadowRadius
                                    ),
                                    radius = shadowRadius,
                                    center = shadowCenter
                                )
                            }
                            .graphicsLayer {
                                scaleX = fabScale
                                scaleY = fabScale
                                rotationZ = fabCookieRotation
                            }
                            .expressivePolygonSurface(
                                shape = fabShape,
                                color = fabBgColor,
                                borderColor = if (!isOnDashboard) fabRimColor else null,
                                borderWidth = 1.dp
                            )
                            .clickable(
                                interactionSource = fabInteraction,
                                indication = null,
                                role = Role.Button
                            ) {
                                fabTapCount++
                                pendingAddSubscription = true
                                if (!isOnDashboard) {
                                    selectTabIndex(0)
                                }
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        ReactiveIcon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Subscription",
                            tint = fabIconColor,
                            size = 26.dp,
                            motion = ReactiveMotionType.PLUS_TWIST,
                            isPressed = fabPressed,
                            triggerKey = fabTapCount
                        )
                    }
                }
            }
        }
    }
}

/**
 * Zero-teardown GPU tab slot: keeps mounted tabs warm (preserving scroll state, ViewModels, and
 * Compose slot tables) while omitting placement when inactive (0 overdraw, 0 hit-testing) and
 * reading [transitionProgressProvider] strictly inside `placeWithLayer` for zero-recomposition slides.
 */
@Composable
private fun WarmTabSlot(
    tabIndex: Int,
    selectedTabIndex: Int,
    outgoingTabIndex: Int,
    isTransitioning: Boolean,
    transitionDirection: Int,
    transitionProgressProvider: () -> Float,
    content: @Composable () -> Unit
) {
    val isSelected = tabIndex == selectedTabIndex
    val isOutgoing = tabIndex == outgoingTabIndex && isTransitioning
    val shouldPlace = isSelected || isOutgoing

    Box(
        modifier = Modifier
            .fillMaxSize()
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    if (shouldPlace) {
                        if (!isTransitioning && isSelected) {
                            placeable.place(x = 0, y = 0, zIndex = 1f)
                        } else {
                            val slidePx = 22.dp.toPx()
                            placeable.placeWithLayer(
                                x = 0,
                                y = 0,
                                zIndex = if (isSelected) 1f else 0f
                            ) {
                                val progress = transitionProgressProvider().coerceIn(0f, 1f)
                                alpha = if (isSelected) {
                                    (0.28f + 0.72f * progress).coerceIn(0f, 1f)
                                } else {
                                    (1f - progress).coerceIn(0f, 1f)
                                }
                                translationX = if (isSelected) {
                                    transitionDirection * slidePx * (1f - progress)
                                } else {
                                    -transitionDirection * slidePx * 0.6f * progress
                                }
                            }
                        }
                    }
                }
            }
    ) {
        content()
    }
}

/**
 * Material 3 Expressive v4.0 Gliding Liquid Indicator Floating Dock:
 * - Always-visible compact labels (`Subs` · `Planner` · `Savings`) eliminate label hide/show lag.
 * - Dual-spring elastic leading/trailing edge indicator glides GPU-only (`drawBehind`) with zero
 *   Compose re-layout churn, stretching fluidly toward the target tab and settling in ~140ms.
 * - Supports direct horizontal thumb scrubbing across all 3 tabs with slot-crossing haptics and
 *   hyperbolic edge overdrag resistance.
 */
@Composable
private fun GlidingLiquidNavDock(
    items: List<NavigationTabItem>,
    selectedIndex: Int,
    indicatorColor: Color,
    indicatorContentColor: Color,
    isDark: Boolean,
    reducedMotion: Boolean,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val tabCount = items.size.coerceAtLeast(1)
    val maxIndex = (tabCount - 1).coerceAtLeast(0)

    val pillLeft = remember { Animatable(selectedIndex.toFloat()) }
    val pillRight = remember { Animatable(selectedIndex.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }
    var dragHoverIndex by remember { mutableIntStateOf(selectedIndex) }
    var pressedTabIndex by remember { mutableIntStateOf(-1) }

    val currentSelectedIndex by rememberUpdatedState(selectedIndex)
    val currentOnSelectTab by rememberUpdatedState(onSelectTab)

    LaunchedEffect(selectedIndex, isDragging, reducedMotion) {
        if (isDragging) return@LaunchedEffect
        val target = selectedIndex.toFloat().coerceIn(0f, maxIndex.toFloat())
        if (reducedMotion) {
            pillLeft.snapTo(target)
            pillRight.snapTo(target)
            return@LaunchedEffect
        }
        val movingRight = target >= (pillLeft.value + pillRight.value) * 0.5f
        val leadingSpec = spring<Float>(
            dampingRatio = 0.72f,
            stiffness = 920f,
            visibilityThreshold = 0.001f
        )
        val trailingSpec = spring<Float>(
            dampingRatio = 0.80f,
            stiffness = 540f,
            visibilityThreshold = 0.001f
        )
        launch {
            pillLeft.animateTo(
                targetValue = target,
                animationSpec = if (movingRight) trailingSpec else leadingSpec
            )
        }
        launch {
            pillRight.animateTo(
                targetValue = target,
                animationSpec = if (movingRight) leadingSpec else trailingSpec
            )
        }
    }

    val isInteracting = isDragging || pressedTabIndex >= 0
    val pillVerticalInset by animateDpAsState(
        targetValue = if (isInteracting) 2.dp else 0.dp,
        animationSpec = ExpressiveMotion.expressiveSpatialFastDp().orSnapIfReduced(reducedMotion),
        label = "dockPillInset"
    )
    val pillCornerRadius by animateDpAsState(
        targetValue = if (isInteracting) 18.dp else 26.dp,
        animationSpec = ExpressiveMotion.expressiveSpatialFastDp().orSnapIfReduced(reducedMotion),
        label = "dockPillCorner"
    )

    val dockContainerColor = MaterialTheme.colorScheme.surfaceContainer
    val dockRimColor = lerp(
        MaterialTheme.colorScheme.outlineVariant,
        indicatorContentColor,
        if (isDark) 0.14f else 0.10f
    ).copy(alpha = if (isDark) 0.58f else 0.76f)
    val indicatorInnerStroke = indicatorContentColor.copy(alpha = if (isDark) 0.14f else 0.10f)

    Surface(
        modifier = modifier.height(60.dp),
        shape = RoundedCornerShape(999.dp),
        color = dockContainerColor,
        border = BorderStroke(1.dp, dockRimColor),
        shadowElevation = if (isDark) 5.dp else 9.dp,
        tonalElevation = 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(5.dp)
                .pointerInput(tabCount, reducedMotion) {
                    var rawDragSlot = currentSelectedIndex.toFloat()
                    var lastTickIndex = currentSelectedIndex
                    detectHorizontalDragGestures(
                        onDragStart = { startOffset ->
                            val slotWidth = (size.width.toFloat() / tabCount).coerceAtLeast(1f)
                            rawDragSlot = ((startOffset.x / slotWidth) - 0.5f)
                                .coerceIn(0f, maxIndex.toFloat())
                            val idx = rawDragSlot.roundToInt().coerceIn(0, maxIndex)
                            isDragging = true
                            dragHoverIndex = idx
                            lastTickIndex = idx
                            scope.launch {
                                pillLeft.snapTo(rawDragSlot)
                                pillRight.snapTo(rawDragSlot)
                            }
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            val slotWidth = (size.width.toFloat() / tabCount).coerceAtLeast(1f)
                            rawDragSlot += dragAmount / slotWidth
                            val dampedSlot = when {
                                rawDragSlot < 0f -> {
                                    val overshoot = -rawDragSlot
                                    -(overshoot * ExpressiveMotion.applyOverdragResistance(overshoot, 6f))
                                        .coerceAtMost(0.10f)
                                }
                                rawDragSlot > maxIndex.toFloat() -> {
                                    val overshoot = rawDragSlot - maxIndex.toFloat()
                                    maxIndex.toFloat() + (overshoot * ExpressiveMotion.applyOverdragResistance(overshoot, 6f))
                                        .coerceAtMost(0.10f)
                                }
                                else -> rawDragSlot
                            }
                            val newIdx = dampedSlot.roundToInt().coerceIn(0, maxIndex)
                            if (newIdx != lastTickIndex) {
                                lastTickIndex = newIdx
                                dragHoverIndex = newIdx
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            scope.launch {
                                pillLeft.snapTo(dampedSlot)
                                pillRight.snapTo(dampedSlot)
                            }
                        },
                        onDragEnd = {
                            val finalIndex = dragHoverIndex.coerceIn(0, maxIndex)
                            isDragging = false
                            if (finalIndex != currentSelectedIndex) {
                                currentOnSelectTab(finalIndex)
                            }
                        },
                        onDragCancel = {
                            isDragging = false
                            dragHoverIndex = currentSelectedIndex
                        }
                    )
                }
                .drawBehind {
                    val slotWidth = size.width / tabCount.toFloat()
                    val leftEdge = minOf(pillLeft.value, pillRight.value)
                    val rightEdge = maxOf(pillLeft.value, pillRight.value)
                    val stretchAmount = abs(rightEdge - leftEdge)
                    val vInsetPx = pillVerticalInset.toPx() + (stretchAmount * 1.4.dp.toPx()).coerceAtMost(2.5.dp.toPx())
                    val minSquishWidth = slotWidth * 0.86f
                    val leftPx = (leftEdge * slotWidth).coerceIn(0f, (size.width - minSquishWidth).coerceAtLeast(0f))
                    val rightPx = ((rightEdge + 1f) * slotWidth).coerceIn(minSquishWidth, size.width)
                    val pillWidth = (rightPx - leftPx).coerceAtLeast(minSquishWidth)
                    val pillHeight = (size.height - vInsetPx * 2f).coerceAtLeast(24.dp.toPx())
                    val radiusPx = pillCornerRadius.toPx().coerceAtMost(pillHeight / 2f)

                    drawRoundRect(
                        color = indicatorColor,
                        topLeft = Offset(leftPx, vInsetPx),
                        size = Size(pillWidth, pillHeight),
                        cornerRadius = CornerRadius(radiusPx, radiusPx)
                    )
                    drawRoundRect(
                        color = indicatorInnerStroke,
                        topLeft = Offset(leftPx + 0.5.dp.toPx(), vInsetPx + 0.5.dp.toPx()),
                        size = Size((pillWidth - 1.dp.toPx()).coerceAtLeast(1f), (pillHeight - 1.dp.toPx()).coerceAtLeast(1f)),
                        cornerRadius = CornerRadius(radiusPx, radiusPx),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = index == selectedIndex
                    val isVisuallyActive = if (isDragging) index == dragHoverIndex else isSelected
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    var tapTrigger by remember { mutableIntStateOf(0) }

                    LaunchedEffect(isPressed) {
                        if (isPressed) {
                            pressedTabIndex = index
                        } else if (pressedTabIndex == index) {
                            pressedTabIndex = -1
                        }
                    }

                    val iconMotion = when (item.destination) {
                        AnalyticsDestination -> ReactiveMotionType.PIGGY_BOUNCE
                        else -> ReactiveMotionType.POP_BOUNCE
                    }

                    val itemScale by animateFloatAsState(
                        targetValue = if (isPressed) 0.92f else 1f,
                        animationSpec = ExpressiveMotion.expressiveSpatialFastFloat()
                            .orSnapIfReduced(reducedMotion),
                        label = "navItemScale"
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (isVisuallyActive) {
                            indicatorContentColor
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        animationSpec = ExpressiveMotion.effectsFastColor()
                            .orSnapIfReduced(reducedMotion),
                        label = "navItemContentColor"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .graphicsLayer {
                                scaleX = itemScale
                                scaleY = itemScale
                            }
                            .semantics {
                                role = Role.Tab
                                selected = isSelected
                            }
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                tapTrigger++
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectTab(index)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            ReactiveIcon(
                                imageVector = if (isVisuallyActive) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title,
                                tint = contentColor,
                                size = 20.dp,
                                motion = iconMotion,
                                isPressed = isPressed,
                                triggerKey = Pair(isVisuallyActive, tapTrigger)
                            )
                            Spacer(modifier = Modifier.height(1.dp))
                            Text(
                                text = item.title,
                                fontFamily = BodyFontFamily,
                                color = contentColor,
                                fontSize = 11.sp,
                                lineHeight = 12.sp,
                                fontWeight = if (isVisuallyActive) FontWeight.ExtraBold else FontWeight.SemiBold,
                                letterSpacing = (-0.1).sp,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
