package com.subtrack.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.Serializable

@Serializable
object DashboardDestination

@Serializable
object PlannerDestination

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
            title = "Planner",
            selectedIcon = Icons.Filled.CalendarMonth,
            unselectedIcon = Icons.Outlined.CalendarMonth
        ),
        NavigationTabItem(
            destination = AnalyticsDestination,
            title = "Savings",
            selectedIcon = Icons.Filled.Savings,
            unselectedIcon = Icons.Outlined.Savings
        )
    )
}
