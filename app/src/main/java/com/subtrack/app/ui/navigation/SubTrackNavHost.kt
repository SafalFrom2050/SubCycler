package com.subtrack.app.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.subtrack.core.domain.model.AppLauncherIcon
import com.subtrack.core.ui.components.ExpressiveMaterialPolygons
import com.subtrack.core.ui.components.ExpressiveScreenBackdrop
import com.subtrack.core.ui.components.ReactiveIcon
import com.subtrack.core.ui.components.ReactiveMotionType
import com.subtrack.core.ui.components.expressivePolygonSurface
import com.subtrack.core.ui.components.rememberMorphingPolygonShape
import com.subtrack.core.ui.theme.ExpressiveMotion
import com.subtrack.core.ui.theme.LocalSubCyclerDarkTheme
import com.subtrack.core.ui.theme.orSnapIfReduced
import com.subtrack.core.ui.theme.rememberReducedMotionEnabled
import com.subtrack.feature.analytics.AnalyticsRoute
import com.subtrack.feature.dashboard.DashboardRoute
import com.subtrack.feature.onboarding.OnboardingRoute
import com.subtrack.feature.planner.PlannerRoute

@Composable
fun SubTrackApp(
    onRequestNotificationPermission: () -> Unit = {},
    openSubscriptionId: Long? = null,
    onOpenSubscriptionConsumed: () -> Unit = {},
    pendingTransferPayload: String? = null,
    onTransferPayloadConsumed: () -> Unit = {},
    onApplyLauncherIcon: (AppLauncherIcon) -> Unit = {},
    appViewModel: AppViewModel = hiltViewModel()
) {
    val launchState by appViewModel.launchState.collectAsStateWithLifecycle()

    LaunchedEffect(launchState, pendingTransferPayload) {
        if (launchState == AppLaunchState.ONBOARDING && !pendingTransferPayload.isNullOrBlank()) {
            appViewModel.onOnboardingFinished()
        }
    }

    when (launchState) {
        AppLaunchState.LOADING -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
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
            onApplyLauncherIcon = onApplyLauncherIcon
        )
    }
}

@Composable
private fun SubTrackMainNavigation(
    navController: NavHostController = rememberNavController(),
    openSubscriptionId: Long? = null,
    onOpenSubscriptionConsumed: () -> Unit = {},
    pendingTransferPayload: String? = null,
    onTransferPayloadConsumed: () -> Unit = {},
    onApplyLauncherIcon: (AppLauncherIcon) -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val isOnDashboard = currentDestination?.hierarchy?.any {
        it.hasRoute(DashboardDestination::class)
    } != false
    var pendingAddSubscription by remember { mutableStateOf(false) }
    var pendingOpenPairedSwap by remember { mutableStateOf(false) }
    var internalOpenSubId by remember { mutableStateOf<Long?>(null) }
    val effectiveOpenSubId = openSubscriptionId ?: internalOpenSubId
    val isDark = LocalSubCyclerDarkTheme.current

    fun navigateToTab(destination: Any) {
        navController.navigate(destination) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Ensure notification, widget, and transfer deep-links switch back to Subscriptions from Planner/Savings
    LaunchedEffect(effectiveOpenSubId, pendingTransferPayload) {
        if ((effectiveOpenSubId != null || !pendingTransferPayload.isNullOrBlank()) && !isOnDashboard) {
            navigateToTab(DashboardDestination)
        }
    }

    ExpressiveScreenBackdrop {
        // True edge-to-edge scroll-under: content fills full screen behind floating toolbar
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = DashboardDestination,
                modifier = Modifier.fillMaxSize(),
                enterTransition = {
                    fadeIn(animationSpec = tween(200)) +
                        scaleIn(
                            initialScale = 0.96f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(140)) +
                        scaleOut(targetScale = 0.98f, animationSpec = tween(140))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(200)) +
                        scaleIn(
                            initialScale = 0.96f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(140))
                }
            ) {
                composable<DashboardDestination> {
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
                        onNavigateToSavings = { navigateToTab(AnalyticsDestination) },
                        onApplyLauncherIcon = onApplyLauncherIcon
                    )
                }
                composable<PlannerDestination> {
                    PlannerRoute(
                        pendingOpenPairedSwap = pendingOpenPairedSwap,
                        onOpenPairedSwapConsumed = { pendingOpenPairedSwap = false },
                        onOpenSubscription = { subId ->
                            internalOpenSubId = subId
                            navigateToTab(DashboardDestination)
                        }
                    )
                }
                composable<AnalyticsDestination> {
                    AnalyticsRoute(
                        onOpenPairedSwap = {
                            pendingOpenPairedSwap = true
                            navigateToTab(PlannerDestination)
                        }
                    )
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

            // Pixel-Locked M3 Expressive Split Floating Bottom Toolbar (zero horizontal drift across tabs!)
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
                    val dockContainerColor = MaterialTheme.colorScheme.surfaceContainer
                    val dockBorder = BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = if (isDark) 0.7f else 0.85f)
                    )

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(999.dp),
                        color = dockContainerColor,
                        border = dockBorder,
                        shadowElevation = if (isDark) 6.dp else 10.dp,
                        tonalElevation = 0.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 5.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NavigationItems.items.forEach { item ->
                                val isSelected = currentDestination?.hierarchy?.any {
                                    it.hasRoute(item.destination::class)
                                } == true
                                val interactionSource = remember { MutableInteractionSource() }
                                val isPressed by interactionSource.collectIsPressedAsState()
                                var tapTrigger by remember { mutableIntStateOf(0) }
                                val iconMotion = when (item.destination) {
                                    AnalyticsDestination -> ReactiveMotionType.PIGGY_BOUNCE
                                    else -> ReactiveMotionType.POP_BOUNCE
                                }

                                val pillScale by animateFloatAsState(
                                    targetValue = if (isPressed) 0.90f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "navTabScale"
                                )
                                val pillColor by animateColorAsState(
                                    targetValue = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        Color.Transparent
                                    },
                                    animationSpec = tween(200),
                                    label = "navTabBg"
                                )
                                val contentColor by animateColorAsState(
                                    targetValue = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    animationSpec = tween(200),
                                    label = "navTabContent"
                                )

                                Box(
                                    modifier = Modifier
                                        .then(if (isSelected) Modifier.weight(1f) else Modifier.width(44.dp))
                                        .height(48.dp)
                                        .graphicsLayer {
                                            scaleX = pillScale
                                            scaleY = pillScale
                                        }
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(pillColor, RoundedCornerShape(999.dp))
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
                                            if (!isSelected) {
                                                navigateToTab(item.destination)
                                            }
                                        }
                                        .padding(horizontal = if (isSelected) 10.dp else 0.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        ReactiveIcon(
                                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = item.title,
                                            tint = contentColor,
                                            size = 19.dp,
                                            motion = iconMotion,
                                            isPressed = isPressed,
                                            triggerKey = Pair(isSelected, tapTrigger)
                                        )
                                        AnimatedVisibility(
                                            visible = isSelected,
                                            enter = expandHorizontally(
                                                animationSpec = spring(
                                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                                    stiffness = Spring.StiffnessMediumLow
                                                )
                                            ) + fadeIn(animationSpec = tween(160)),
                                            exit = shrinkHorizontally(
                                                animationSpec = tween(150)
                                            ) + fadeOut(animationSpec = tween(110))
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Spacer(modifier = Modifier.width(5.dp))
                                                Text(
                                                    text = item.title,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = contentColor,
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    letterSpacing = (-0.2).sp,
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

                    // Detached 12-Petal Scalloped '+' Action Button with anticipatory press-preview morph (Cookie12 -> Sunny!)
                    val reducedMotion = rememberReducedMotionEnabled()
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
                            .size(56.dp)
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
                                    navigateToTab(DashboardDestination)
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
