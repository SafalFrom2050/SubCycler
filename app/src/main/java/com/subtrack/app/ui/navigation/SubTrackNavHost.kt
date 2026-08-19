package com.subtrack.app.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.subtrack.app.data.repository.SubscriptionRepository
import com.subtrack.app.ui.screens.analytics.AnalyticsScreen
import com.subtrack.app.ui.screens.analytics.AnalyticsViewModel
import com.subtrack.app.ui.screens.dashboard.DashboardScreen
import com.subtrack.app.ui.screens.dashboard.DashboardViewModel
import com.subtrack.app.ui.screens.planner.PlannerScreen
import com.subtrack.app.ui.screens.planner.PlannerViewModel
import com.subtrack.app.ui.screens.rotator.PairedRotatorScreen
import com.subtrack.app.ui.screens.rotator.PairedRotatorViewModel
import com.subtrack.app.ui.theme.Emerald500

@Composable
fun SubTrackApp(
    repository: SubscriptionRepository,
    navController: NavHostController = rememberNavController()
) {
    val haptic = LocalHapticFeedback.current

    val dashboardViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
        DashboardViewModel(repository)
    }
    val plannerViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
        PlannerViewModel(repository)
    }
    val pairedRotatorViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
        PairedRotatorViewModel(repository)
    }
    val analyticsViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
        AnalyticsViewModel(repository)
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var isNavBarExpanded by rememberSaveable { mutableStateOf(true) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (available.y < -10f) {
                    if (isNavBarExpanded) isNavBarExpanded = false
                } else if (available.y > 10f) {
                    if (!isNavBarExpanded) isNavBarExpanded = true
                }
                return Offset.Zero
            }
        }
    }

    LaunchedEffect(currentRoute) {
        isNavBarExpanded = true
    }

    val navBarHeight by animateDpAsState(
        targetValue = if (isNavBarExpanded) 80.dp else 54.dp,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioNoBouncy
        ),
        label = "navBarHeight"
    )

    Scaffold(
        modifier = Modifier.nestedScroll(nestedScrollConnection),
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(navBarHeight),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Screen.items.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = selected,
                        alwaysShowLabel = isNavBarExpanded,
                        onClick = {
                            if (currentRoute != screen.route) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.title
                            )
                        },
                        label = if (isNavBarExpanded) {
                            {
                                Text(
                                    text = screen.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            }
                        } else null,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Emerald500,
                            selectedTextColor = Emerald500,
                            indicatorColor = Emerald500.copy(alpha = 0.15f)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(top = innerPadding.calculateTopPadding()),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(viewModel = dashboardViewModel)
            }
            composable(Screen.Planner.route) {
                PlannerScreen(viewModel = plannerViewModel)
            }
            composable(Screen.Rotator.route) {
                PairedRotatorScreen(viewModel = pairedRotatorViewModel)
            }
            composable(Screen.Analytics.route) {
                AnalyticsScreen(viewModel = analyticsViewModel)
            }
        }
    }
}
