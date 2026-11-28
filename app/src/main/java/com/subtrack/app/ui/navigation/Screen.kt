package com.subtrack.app.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
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
            title = "Subs",
            selectedIcon = Icons.Rounded.GridView,
            unselectedIcon = Icons.Outlined.GridView
        ),
        NavigationTabItem(
            destination = PlannerDestination,
            title = "Planner",
            selectedIcon = Icons.Rounded.CalendarMonth,
            unselectedIcon = Icons.Outlined.CalendarMonth
        ),
        NavigationTabItem(
            destination = AnalyticsDestination,
            title = "Savings",
            selectedIcon = Icons.Rounded.Savings,
            unselectedIcon = Icons.Outlined.Savings
        )
    )

    fun indexFor(destination: NavDestination?): Int {
        if (destination == null) return 0
        val idx = items.indexOfFirst { tab ->
            destination.hierarchy.any { it.hasRoute(tab.destination::class) }
        }
        return if (idx >= 0) idx else 0
    }
}
