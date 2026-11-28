package com.subtrack.app.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.invisibleToUser
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
import com.subtrack.core.ui.components.RoundedPolygonShape
import com.subtrack.core.ui.components.SubCyclerSwirlLogo
import com.subtrack.core.ui.components.expressivePolygonSurface
import com.subtrack.core.ui.components.rememberMorphingPolygonShape
import com.subtrack.core.ui.theme.BodyFontFamily
import com.subtrack.core.ui.theme.BrandColorHarmonizer
import com.subtrack.core.ui.theme.ExpressiveMotion
import com.subtrack.core.ui.theme.LocalSubCyclerDarkTheme
import com.subtrack.core.ui.theme.launcherAccentTokensFor
import com.subtrack.core.ui.theme.orSnapIfReduced
import com.subtrack.core.ui.theme.rememberReducedMotionEnabled
import com.subtrack.feature.analytics.AnalyticsRoute
import com.subtrack.feature.dashboard.DashboardRoute
import com.subtrack.feature.onboarding.OnboardingRoute
import com.subtrack.feature.planner.PlannerRoute
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
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
    onSplashDismissed: () -> Unit = {},
    appViewModel: AppViewModel = hiltViewModel()
) {
    val launchState by appViewModel.launchState.collectAsStateWithLifecycle()
    val dockMood by appViewModel.dockMood.collectAsStateWithLifecycle()
    var isDashboardHydrated by remember { mutableStateOf(false) }
    var splashRevealStarted by remember { mutableStateOf(false) }
    var splashDismissed by remember { mutableStateOf(false) }
    var isPreviewingWelcome by remember { mutableStateOf(false) }

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
                    isPreviewMode = isPreviewingWelcome,
                    onExitPreview = {
                        isPreviewingWelcome = false
                        appViewModel.onOnboardingFinished()
                    },
                    onFinished = { requestNotificationPermission ->
                        isPreviewingWelcome = false
                        appViewModel.onOnboardingFinished()
                        if (requestNotificationPermission) {
                            onRequestNotificationPermission()
                        }
                    },
                    onApplyLauncherIcon = onApplyLauncherIcon
                )
            }
            AppLaunchState.MAIN_CONTENT -> SubTrackMainNavigation(
                openSubscriptionId = openSubscriptionId,
                onOpenSubscriptionConsumed = onOpenSubscriptionConsumed,
                pendingTransferPayload = pendingTransferPayload,
                onTransferPayloadConsumed = onTransferPayloadConsumed,
                onApplyLauncherIcon = onApplyLauncherIcon,
                onPreviewNotifications = onPreviewNotifications,
                onPreviewWelcome = {
                    isPreviewingWelcome = true
                    appViewModel.openOnboarding()
                },
                dockMood = dockMood,
                isSplashDismissed = splashDismissed,
                isEntranceReady = splashRevealStarted,
                onDashboardFirstDataReady = { isDashboardHydrated = true }
            )
        }

        if (!splashDismissed) {
            SubCyclerExpressiveSplashOverlay(
                dockMood = dockMood,
                isContentReady = isUnderlyingContentReady,
                onSplashExitStart = { splashRevealStarted = true },
                onSplashFinished = {
                    splashRevealStarted = true
                    splashDismissed = true
                    onSplashDismissed()
                }
            )
        }
    }
}

/**
 * Bold Color-Block Material 3 Expressive Launch Splash:
 * 1. Holds the plain canvas until DataStore hydrates `dockMood`, then renders the larger
 *    Color-Block `Cookie12` medallion and swirl emblem cleanly while the underlying screen
 *    finishes its first composition/layout pass so zero main-thread spikes contend with animation.
 * 2. Performs one uninterrupted, GPU-only 360° expressive spin + spring bloom + minimal
 *    `SubCycler` wordmark reveal (zero per-frame recompositions, cached vector paths).
 * 3. Lifts and dissolves smoothly straight into Home's unfurling launch choreography.
 */
@Composable
private fun SubCyclerExpressiveSplashOverlay(
    dockMood: DockMoodState,
    isContentReady: Boolean,
    onSplashExitStart: () -> Unit = {},
    onSplashFinished: () -> Unit
) {
    // Hold the plain canvas until DataStore hydrates dockMood so stageColor and swirl petals
    // initialize directly to the user's selected theme/icon with zero default-green color flash.
    if (!dockMood.isLoaded) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        )
        return
    }

    val reducedMotion = rememberReducedMotionEnabled()
    val isDark = LocalSubCyclerDarkTheme.current
    val currentReady by rememberUpdatedState(isContentReady)
    val currentOnExitStart by rememberUpdatedState(onSplashExitStart)
    val currentOnFinished by rememberUpdatedState(onSplashFinished)

    val rotation = remember { Animatable(0f) }
    val logoScale = remember { Animatable(if (reducedMotion) 1f else 0.88f) }
    val medallionScale = remember { Animatable(if (reducedMotion) 1f else 0.82f) }
    val wordmarkProgress = remember { Animatable(0f) }
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
    val launcherAccent = remember(dockMood.launcherIcon, isDark) {
        launcherAccentTokensFor(dockMood.launcherIcon, isDark)
    }
    val heroMood = remember(dockMood.activeSubscriptions, dockMood.useStackHeroMood, dockMood.launcherIcon, isDark) {
        BrandColorHarmonizer.resolveHeroMoodPalette(
            activeSubscriptions = dockMood.activeSubscriptions,
            useStackMood = dockMood.useStackHeroMood,
            isDark = isDark,
            fallbackIcon = dockMood.launcherIcon
        )
    }
    val activeSwirlColors = if (morphedToStackMood) stackMoodSwirl else launcherSwirl
    // Keep as State<Color> and read exclusively in draw phase so color morphs trigger 0 recompositions.
    val stageColorState = animateColorAsState(
        targetValue = if (morphedToStackMood) heroMood.baseColor else launcherAccent.heroBase,
        animationSpec = ExpressiveMotion.effectsDefaultColor().orSnapIfReduced(reducedMotion),
        label = "splashStageColor"
    )
    val stagePopColorState = animateColorAsState(
        targetValue = if (morphedToStackMood) heroMood.sealColor else launcherAccent.heroPop,
        animationSpec = ExpressiveMotion.effectsDefaultColor().orSnapIfReduced(reducedMotion),
        label = "splashStagePopColor"
    )
    val cookieStageShape = remember { RoundedPolygonShape(ExpressiveMaterialPolygons.Cookie12) }
    val cornerCloverShape = remember { RoundedPolygonShape(ExpressiveMaterialPolygons.Clover8) }
    val cornerPebbleShape = remember { RoundedPolygonShape(ExpressiveMaterialPolygons.Pebble) }
    val cloverAlpha = if (isDark) 0.24f else 0.13f
    val pebbleAlpha = if (isDark) 0.14f else 0.22f

    LaunchedEffect(reducedMotion) {
        if (reducedMotion) {
            while (!currentReady) {
                delay(24L)
            }
            morphedToStackMood = true
            wordmarkProgress.snapTo(1f)
            currentOnExitStart()
            currentOnFinished()
            return@LaunchedEffect
        }

        // Let the underlying Dashboard/Onboarding finish its heavy first composition & layout pass
        // before starting the animation clock, then drain 2 clean VSync frames for locked 90/120Hz.
        withTimeoutOrNull(450L) {
            while (!currentReady) {
                delay(16L)
            }
        }
        withFrameNanos { }
        withFrameNanos { }

        // Phase 1: Single uninterrupted expressive bloom + silky 360° settle + wordmark slide-up
        val spinEasing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
        val medallionJob = launch {
            medallionScale.animateTo(
                targetValue = 1f,
                animationSpec = ExpressiveMotion.expressiveSpatialDefaultFloat()
            )
        }
        val logoJob = launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = ExpressiveMotion.expressiveSpatialFastFloat()
            )
        }
        val spinJob = launch {
            rotation.animateTo(
                targetValue = 360f,
                animationSpec = tween(durationMillis = 440, easing = spinEasing)
            )
        }
        val moodAndWordmarkJob = launch {
            delay(130L)
            morphedToStackMood = true
            wordmarkProgress.animateTo(
                targetValue = 1f,
                animationSpec = ExpressiveMotion.expressiveSpatialFastFloat()
            )
        }
        medallionJob.join()
        logoJob.join()
        spinJob.join()
        moodAndWordmarkJob.join()

        while (!currentReady) {
            delay(16L)
        }
        delay(150L)

        // Phase 2: Crisp lift & dissolve straight into hydrated Home — fire entrance handoff now
        currentOnExitStart()
        val liftJob = launch {
            overlayScale.animateTo(
                targetValue = 1.045f,
                animationSpec = tween(durationMillis = 185, easing = FastOutSlowInEasing)
            )
        }
        overlayAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 185, easing = FastOutSlowInEasing)
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
        // Graphic off-corner Bold Color-Block silhouettes (zero blurry gradient fog)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 84.dp, y = (-68).dp)
                .size(236.dp)
                .graphicsLayer {
                    rotationZ = rotation.value * 0.08f
                    scaleX = medallionScale.value
                    scaleY = medallionScale.value
                }
                .expressivePolygonSurface(
                    shape = cornerCloverShape,
                    color = { stageColorState.value.copy(alpha = cloverAlpha) }
                )
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = (-76).dp, y = 78.dp)
                .size(214.dp)
                .graphicsLayer {
                    rotationZ = 24f - rotation.value * 0.06f
                    scaleX = medallionScale.value
                    scaleY = medallionScale.value
                }
                .expressivePolygonSurface(
                    shape = cornerPebbleShape,
                    color = { stagePopColorState.value.copy(alpha = pebbleAlpha) }
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier.size(206.dp),
                contentAlignment = Alignment.Center
            ) {
                // Larger Flat Color-Block Cookie12 stage medallion + cached Triadic dashed halo
                Box(
                    modifier = Modifier
                        .size(192.dp)
                        .graphicsLayer {
                            scaleX = medallionScale.value
                            scaleY = medallionScale.value
                            rotationZ = -rotation.value * 0.14f
                        }
                        .expressivePolygonSurface(
                            shape = cookieStageShape,
                            color = { stageColorState.value }
                        )
                        .drawWithCache {
                            val inset = 14.dp.toPx()
                            val ringRadius = (size.minDimension / 2f) - inset
                            val dashStroke = Stroke(
                                width = 1.8.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(7.dp.toPx(), 7.dp.toPx()),
                                    0f
                                )
                            )
                            onDrawBehind {
                                drawCircle(
                                    color = stagePopColorState.value.copy(alpha = 0.80f),
                                    radius = ringRadius,
                                    style = dashStroke
                                )
                            }
                        }
                )

                SubCyclerSwirlLogo(
                    colors = activeSwirlColors,
                    size = 118.dp,
                    cornerRadius = 33.dp,
                    rotationDegreesProvider = { rotation.value },
                    contentDescriptionText = "SubCycler loading",
                    modifier = Modifier.graphicsLayer {
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                    }
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Box(
                modifier = Modifier.graphicsLayer {
                    val p = wordmarkProgress.value.coerceIn(0f, 1f)
                    alpha = p
                    translationY = (1f - p) * 12.dp.toPx()
                }
            ) {
                ExpressiveBrandWordmark(fontSize = 34.sp)
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
    onPreviewWelcome: () -> Unit = {},
    dockMood: DockMoodState = DockMoodState(),
    isSplashDismissed: Boolean = true,
    isEntranceReady: Boolean = true,
    onDashboardFirstDataReady: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val reducedMotion = rememberReducedMotionEnabled()
    var selectedTabIndex by rememberSaveable { mutableIntStateOf(0) }
    var tabTransition by remember {
        mutableStateOf(TabTransitionState(activeIndex = selectedTabIndex))
    }
    var isPlannerMounted by remember { mutableStateOf(selectedTabIndex == 1) }
    var isAnalyticsMounted by remember { mutableStateOf(selectedTabIndex == 2) }
    var hasVisitedPlanner by rememberSaveable { mutableStateOf(selectedTabIndex == 1) }
    var hasVisitedSavings by rememberSaveable { mutableStateOf(selectedTabIndex == 2) }

    // One-shot launch choreography for the floating dock and detached '+' Cookie12 FAB
    val skipChromeEntrance = isEntranceReady || reducedMotion
    val dockReveal = remember { Animatable(if (skipChromeEntrance) 1f else 0f) }
    val fabReveal = remember { Animatable(if (skipChromeEntrance) 1f else 0f) }

    LaunchedEffect(isEntranceReady, reducedMotion) {
        if (!isEntranceReady) return@LaunchedEffect
        if (reducedMotion) {
            dockReveal.snapTo(1f)
            fabReveal.snapTo(1f)
            return@LaunchedEffect
        }
        if (dockReveal.value >= 0.99f && fabReveal.value >= 0.99f) return@LaunchedEffect
        launch {
            delay(45L)
            dockReveal.animateTo(1f, ExpressiveMotion.expressiveSpatialDefaultFloat())
        }
        launch {
            delay(90L)
            fabReveal.animateTo(1f, ExpressiveMotion.expressiveSpatialFastFloat())
        }
    }

    val isOnDashboard = selectedTabIndex == 0
    var pendingAddSubscription by remember { mutableStateOf(false) }
    var pendingOpenSettings by remember { mutableStateOf(false) }
    var pendingOpenPairedSwap by remember { mutableStateOf(false) }
    var internalOpenSubId by remember { mutableStateOf<Long?>(null) }
    val effectiveOpenSubId = openSubscriptionId ?: internalOpenSubId
    val isDark = LocalSubCyclerDarkTheme.current
    val latestReducedMotion by rememberUpdatedState(reducedMotion)

    val selectTabIndex: (Int) -> Unit = remember {
        { targetIndex ->
            val clamped = targetIndex.coerceIn(0, NavigationItems.items.lastIndex)
            if (clamped == 1) {
                isPlannerMounted = true
                hasVisitedPlanner = true
            }
            if (clamped == 2) {
                isAnalyticsMounted = true
                hasVisitedSavings = true
            }
            val current = selectedTabIndex
            if (clamped != current) {
                val priorTransition = tabTransition
                val outgoingStartAlpha = if (
                    priorTransition.outgoingIndex >= 0 &&
                    priorTransition.activeIndex == current
                ) {
                    incomingTabAlpha(priorTransition.progress.value)
                } else {
                    1f
                }
                selectedTabIndex = clamped
                // Construct the fresh Animatable(0f) synchronously in the same Compose snapshot as
                // selectedTabIndex so frame 0 never renders with a stale 1f progress value.
                tabTransition = if (latestReducedMotion) {
                    TabTransitionState(
                        activeIndex = clamped,
                        outgoingIndex = -1,
                        direction = if (clamped >= current) 1 else -1,
                        outgoingStartAlpha = 1f,
                        progress = Animatable(1f)
                    )
                } else {
                    TabTransitionState(
                        activeIndex = clamped,
                        outgoingIndex = current,
                        direction = if (clamped >= current) 1 else -1,
                        outgoingStartAlpha = outgoingStartAlpha,
                        progress = Animatable(0f)
                    )
                }
            }
        }
    }

    // Gently pre-warm secondary tabs after the launch splash and Home's initial entrance springs
    // settle so even the very first tap switches immediately via pure GPU layer glide with zero hitch.
    LaunchedEffect(isSplashDismissed) {
        if (!isSplashDismissed) return@LaunchedEffect
        if (!isPlannerMounted) {
            delay(520L)
            isPlannerMounted = true
        }
        if (!isAnalyticsMounted) {
            delay(220L)
            isAnalyticsMounted = true
        }
    }

    LaunchedEffect(tabTransition, reducedMotion) {
        val transition = tabTransition
        if (transition.outgoingIndex < 0 || transition.outgoingIndex == transition.activeIndex) {
            if (transition.progress.value < 1f) {
                transition.progress.snapTo(1f)
            }
            return@LaunchedEffect
        }
        if (reducedMotion) {
            transition.progress.snapTo(1f)
            if (tabTransition === transition) {
                tabTransition = transition.copy(outgoingIndex = -1)
            }
            return@LaunchedEffect
        }
        transition.progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = TAB_TRANSITION_DURATION_MS,
                easing = LinearEasing
            )
        )
        if (tabTransition === transition) {
            tabTransition = transition.copy(outgoingIndex = -1)
        }
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

    // Active dock indicator reads directly from app-wide MaterialTheme (which already reflects
    // Living Stack Mood when enabled or the selected launcher theme when off).
    val animatedIndicatorBg by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.primary,
        animationSpec = ExpressiveMotion.effectsFastColor().orSnapIfReduced(reducedMotion),
        label = "dockIndicatorBg"
    )
    val animatedIndicatorInk by animateColorAsState(
        targetValue = MaterialTheme.colorScheme.onPrimary,
        animationSpec = ExpressiveMotion.effectsFastColor().orSnapIfReduced(reducedMotion),
        label = "dockIndicatorInk"
    )

    val transitionStateProvider: () -> TabTransitionState = remember { { tabTransition } }
    val latestOnOpenSubscriptionConsumed by rememberUpdatedState(onOpenSubscriptionConsumed)
    val latestOpenSubscriptionId by rememberUpdatedState(openSubscriptionId)
    val handleOpenSubscriptionConsumed: () -> Unit = remember {
        {
            if (latestOpenSubscriptionId != null) {
                latestOnOpenSubscriptionConsumed()
            }
            internalOpenSubId = null
        }
    }
    val handleAddSubscriptionConsumed: () -> Unit = remember { { pendingAddSubscription = false } }
    val handleOpenSettings: () -> Unit = remember { { pendingOpenSettings = true } }
    val handleOpenSettingsConsumed: () -> Unit = remember { { pendingOpenSettings = false } }
    val handleOpenPairedSwapConsumed: () -> Unit = remember { { pendingOpenPairedSwap = false } }
    val handleNavigateToSavings: () -> Unit = remember { { selectTabIndex(2) } }
    val handleOpenPairedSwap: () -> Unit = remember {
        {
            pendingOpenPairedSwap = true
            selectTabIndex(1)
        }
    }
    val handleOpenSubscriptionFromPlanner: (Long) -> Unit = remember {
        { subId ->
            internalOpenSubId = subId
            selectTabIndex(0)
        }
    }

    ExpressiveScreenBackdrop {
        // True edge-to-edge scroll-under: warm-mounted GPU tab slots keep scroll & ViewModels live
        // inside an isolated stage composable so switching tabs never recomposes or re-measures tabs.
        Box(modifier = Modifier.fillMaxSize()) {
            SubTrackTabStage(
                transitionStateProvider = transitionStateProvider,
                isPlannerMounted = isPlannerMounted,
                isAnalyticsMounted = isAnalyticsMounted,
                hasVisitedPlanner = hasVisitedPlanner,
                hasVisitedSavings = hasVisitedSavings,
                effectiveOpenSubId = effectiveOpenSubId,
                onOpenSubscriptionConsumed = handleOpenSubscriptionConsumed,
                pendingTransferPayload = pendingTransferPayload,
                onTransferPayloadConsumed = onTransferPayloadConsumed,
                pendingAddSubscription = pendingAddSubscription,
                onAddSubscriptionConsumed = handleAddSubscriptionConsumed,
                pendingOpenSettings = pendingOpenSettings,
                onOpenSettings = handleOpenSettings,
                onOpenSettingsConsumed = handleOpenSettingsConsumed,
                pendingOpenPairedSwap = pendingOpenPairedSwap,
                onOpenPairedSwapConsumed = handleOpenPairedSwapConsumed,
                onNavigateToSavings = handleNavigateToSavings,
                onOpenPairedSwap = handleOpenPairedSwap,
                onOpenSubscriptionFromPlanner = handleOpenSubscriptionFromPlanner,
                onApplyLauncherIcon = onApplyLauncherIcon,
                onPreviewNotifications = onPreviewNotifications,
                onPreviewWelcome = onPreviewWelcome,
                isEntranceReady = isEntranceReady,
                onDashboardFirstDataReady = onDashboardFirstDataReady
            )

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
                        modifier = Modifier
                            .weight(1f)
                            .graphicsLayer {
                                val d = dockReveal.value
                                alpha = ExpressiveMotion.clampEffectProgress(d * 1.5f)
                                translationY = (1f - d) * 26.dp.toPx()
                            }
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
                    val accentTokens = com.subtrack.core.ui.theme.LocalSubCyclerAccentTokens.current
                    val fabPopBg = accentTokens.heroPop
                    val fabPopInk = accentTokens.onHeroPop
                    val fabBgColor by animateColorAsState(
                        targetValue = if (isOnDashboard) {
                            fabPopBg
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        animationSpec = ExpressiveMotion.effectsFastColor()
                            .orSnapIfReduced(reducedMotion),
                        label = "detachedFabBg"
                    )
                    val fabIconColor by animateColorAsState(
                        targetValue = if (isOnDashboard) {
                            fabPopInk
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
                                val f = fabReveal.value
                                val revealScale = 0.72f + 0.28f * f
                                alpha = ExpressiveMotion.clampEffectProgress(f * 1.45f)
                                scaleX = fabScale * revealScale
                                scaleY = fabScale * revealScale
                                rotationZ = fabCookieRotation - (1f - ExpressiveMotion.clampEffectProgress(f)) * 20f
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

private val TabExitEasing = FastOutLinearInEasing
private val TabEnterAlphaEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
private val TabEnterSpatialEasing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1.0f)

internal const val TAB_TRANSITION_DURATION_MS = 220
private const val TAB_OUTGOING_END_PROGRESS = 0.34f
private const val TAB_INCOMING_START_PROGRESS = 0.24f

internal fun outgoingTabAlpha(progress: Float): Float {
    val t = progress.coerceIn(0f, 1f)
    val outT = (t / TAB_OUTGOING_END_PROGRESS).coerceIn(0f, 1f)
    return (1f - TabExitEasing.transform(outT)).coerceIn(0f, 1f)
}

internal fun incomingTabAlpha(progress: Float): Float {
    val t = progress.coerceIn(0f, 1f)
    if (t <= TAB_INCOMING_START_PROGRESS) return 0f
    val inT = ((t - TAB_INCOMING_START_PROGRESS) / (1f - TAB_INCOMING_START_PROGRESS)).coerceIn(0f, 1f)
    return TabEnterAlphaEasing.transform(inT).coerceIn(0f, 1f)
}

internal fun incomingTabSpatial(progress: Float): Float {
    val t = progress.coerceIn(0f, 1f)
    if (t <= TAB_INCOMING_START_PROGRESS) return 0f
    val inT = ((t - TAB_INCOMING_START_PROGRESS) / (1f - TAB_INCOMING_START_PROGRESS)).coerceIn(0f, 1f)
    return TabEnterSpatialEasing.transform(inT).coerceIn(0f, 1f)
}

private data class TabTransitionState(
    val activeIndex: Int,
    val outgoingIndex: Int = -1,
    val direction: Int = 1,
    val outgoingStartAlpha: Float = 1f,
    val progress: Animatable<Float, AnimationVector1D> = Animatable(1f)
)

@Composable
private fun SubTrackTabStage(
    transitionStateProvider: () -> TabTransitionState,
    isPlannerMounted: Boolean,
    isAnalyticsMounted: Boolean,
    hasVisitedPlanner: Boolean,
    hasVisitedSavings: Boolean,
    effectiveOpenSubId: Long?,
    onOpenSubscriptionConsumed: () -> Unit,
    pendingTransferPayload: String?,
    onTransferPayloadConsumed: () -> Unit,
    pendingAddSubscription: Boolean,
    onAddSubscriptionConsumed: () -> Unit,
    pendingOpenSettings: Boolean,
    onOpenSettings: () -> Unit,
    onOpenSettingsConsumed: () -> Unit,
    pendingOpenPairedSwap: Boolean,
    onOpenPairedSwapConsumed: () -> Unit,
    onNavigateToSavings: () -> Unit,
    onOpenPairedSwap: () -> Unit,
    onOpenSubscriptionFromPlanner: (Long) -> Unit,
    onApplyLauncherIcon: (AppLauncherIcon) -> Unit,
    onPreviewNotifications: () -> Unit,
    onPreviewWelcome: () -> Unit,
    isEntranceReady: Boolean,
    onDashboardFirstDataReady: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        WarmTabSlot(
            tabIndex = 0,
            transitionStateProvider = transitionStateProvider
        ) {
            DashboardRoute(
                openSubscriptionId = effectiveOpenSubId,
                onOpenSubscriptionConsumed = onOpenSubscriptionConsumed,
                pendingTransferPayload = pendingTransferPayload,
                onTransferPayloadConsumed = onTransferPayloadConsumed,
                pendingAddSubscription = pendingAddSubscription,
                onAddSubscriptionConsumed = onAddSubscriptionConsumed,
                pendingOpenSettings = pendingOpenSettings,
                onOpenSettingsConsumed = onOpenSettingsConsumed,
                onNavigateToSavings = onNavigateToSavings,
                onApplyLauncherIcon = onApplyLauncherIcon,
                onPreviewNotifications = onPreviewNotifications,
                onPreviewWelcome = onPreviewWelcome,
                isEntranceReady = isEntranceReady,
                onFirstDataReady = onDashboardFirstDataReady
            )
        }

        if (isPlannerMounted) {
            WarmTabSlot(
                tabIndex = 1,
                transitionStateProvider = transitionStateProvider
            ) {
                PlannerRoute(
                    pendingOpenPairedSwap = pendingOpenPairedSwap,
                    onOpenPairedSwapConsumed = onOpenPairedSwapConsumed,
                    isEntranceReady = hasVisitedPlanner,
                    onOpenSettings = onOpenSettings,
                    onOpenSubscription = onOpenSubscriptionFromPlanner
                )
            }
        }

        if (isAnalyticsMounted) {
            WarmTabSlot(
                tabIndex = 2,
                transitionStateProvider = transitionStateProvider
            ) {
                AnalyticsRoute(
                    isEntranceReady = hasVisitedSavings,
                    onOpenSettings = onOpenSettings,
                    onOpenPairedSwap = onOpenPairedSwap
                )
            }
        }
    }
}

/**
 * Zero-teardown GPU tab slot: keeps mounted tabs warm AND continuously placed in a persistent
 * hardware layer so HWUI never discards or re-records a screen's DisplayList on tab switches.
 * Inactive tabs stay at `alpha = 0f` (skipped by HWUI with 0 GPU overdraw), blocked at
 * [PointerEventPass.Initial] (0 touch leakage), and hidden from accessibility.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun WarmTabSlot(
    tabIndex: Int,
    transitionStateProvider: () -> TabTransitionState,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics {
                if (transitionStateProvider().activeIndex != tabIndex) {
                    invisibleToUser()
                }
            }
            .pointerInput(tabIndex) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (transitionStateProvider().activeIndex != tabIndex) {
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
            }
            .layout { measurable, constraints ->
                val placeable = measurable.measure(constraints)
                layout(placeable.width, placeable.height) {
                    val transition = transitionStateProvider()
                    val isSelected = tabIndex == transition.activeIndex
                    val isOutgoing = tabIndex == transition.outgoingIndex &&
                        transition.outgoingIndex != transition.activeIndex
                    val slidePx = 10.dp.toPx()
                    placeable.placeWithLayer(
                        x = 0,
                        y = 0,
                        zIndex = when {
                            isSelected -> 1f
                            isOutgoing -> 0f
                            else -> -1f
                        }
                    ) {
                        when {
                            !isSelected && !isOutgoing -> {
                                alpha = 0f
                                translationX = 0f
                            }
                            transition.outgoingIndex < 0 -> {
                                alpha = if (isSelected) 1f else 0f
                                translationX = 0f
                            }
                            isSelected -> {
                                val t = transition.progress.value.coerceIn(0f, 1f)
                                alpha = incomingTabAlpha(t)
                                translationX = transition.direction * slidePx * (1f - incomingTabSpatial(t))
                            }
                            else -> {
                                val t = transition.progress.value.coerceIn(0f, 1f)
                                alpha = (transition.outgoingStartAlpha * outgoingTabAlpha(t)).coerceIn(0f, 1f)
                                translationX = 0f
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
                    var selectTrigger by remember { mutableIntStateOf(0) }
                    var wasVisuallyActive by remember { mutableStateOf(isVisuallyActive) }

                    LaunchedEffect(isVisuallyActive) {
                        if (isVisuallyActive && !wasVisuallyActive) {
                            selectTrigger++
                        }
                        wasVisuallyActive = isVisuallyActive
                    }

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
                                if (isSelected) {
                                    selectTrigger++
                                }
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
                                triggerKey = selectTrigger
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
