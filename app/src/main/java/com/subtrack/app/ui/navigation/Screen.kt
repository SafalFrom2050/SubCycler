package com.subtrack.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

@Serializable
object DashboardDestination

@Serializable
object PlannerDestination

@Serializable
object RotatorDestination

@Serializable
object AnalyticsDestination

data class NavigationTabItem(
    val destination: Any,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

object NavigationItems {
    val items = listOf(
        NavigationTabItem(
            destination = DashboardDestination,
            title = "Subscriptions",
            selectedIcon = Icons.AutoMirrored.Filled.ListAlt,
            unselectedIcon = Icons.AutoMirrored.Outlined.ListAlt
        ),
        NavigationTabItem(
            destination = PlannerDestination,
            title = "45d Planner",
            selectedIcon = Icons.Filled.CalendarMonth,
            unselectedIcon = Icons.Outlined.CalendarMonth
        ),
        NavigationTabItem(
            destination = RotatorDestination,
            title = "Paired Swap",
            selectedIcon = Icons.Filled.SwapHoriz,
            unselectedIcon = Icons.Outlined.SwapHoriz
        ),
        NavigationTabItem(
            destination = AnalyticsDestination,
            title = "Savings & ROI",
            selectedIcon = Icons.Filled.Savings,
            unselectedIcon = Icons.Outlined.Savings
        )
    )
}
