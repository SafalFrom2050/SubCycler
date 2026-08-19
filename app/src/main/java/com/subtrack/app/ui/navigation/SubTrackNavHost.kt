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
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.draw.drawBehind
import com.subtrack.feature.analytics.AnalyticsRoute
import com.subtrack.feature.dashboard.DashboardRoute
import com.subtrack.feature.planner.PlannerRoute
import com.subtrack.feature.rotator.PairedRotatorRoute
import com.subtrack.core.ui.theme.PrimarySky

@Composable
fun SubTrackApp(
    navController: NavHostController = rememberNavController()
) {
    val haptic = LocalHapticFeedback.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

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

    LaunchedEffect(currentDestination) {
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
            val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            NavigationBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(navBarHeight)
                    .drawBehind {
                        drawLine(
                            color = outlineColor,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    },
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                NavigationItems.items.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.hasRoute(item.destination::class) } == true
                    NavigationBarItem(
                        selected = selected,
                        alwaysShowLabel = isNavBarExpanded,
                        onClick = {
                            if (!selected) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                navController.navigate(item.destination) {
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
                                imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title
                            )
                        },
                        label = if (isNavBarExpanded) {
                            {
                                Text(
                                    text = item.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            }
                        } else null,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = PrimarySky,
                            selectedTextColor = PrimarySky,
                            indicatorColor = PrimarySky.copy(alpha = 0.15f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = DashboardDestination,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            composable<DashboardDestination> {
                DashboardRoute()
            }
            composable<PlannerDestination> {
                PlannerRoute()
            }
            composable<RotatorDestination> {
                PairedRotatorRoute()
            }
            composable<AnalyticsDestination> {
                AnalyticsRoute()
            }
        }
    }
}
