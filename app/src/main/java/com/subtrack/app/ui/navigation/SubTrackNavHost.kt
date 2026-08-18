package com.subtrack.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Screen.items.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (currentRoute != screen.route) {
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
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
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
            modifier = Modifier.padding(innerPadding)
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
