package com.subtrack.app.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subtrack.app.data.model.CyclePhase
import com.subtrack.app.data.model.CycleStatus
import com.subtrack.app.data.model.Subscription
import com.subtrack.app.data.repository.SubscriptionRepository
import com.subtrack.app.domain.PortfolioSummary
import com.subtrack.app.domain.RotationEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DashboardUiState(
    val subscriptions: List<Subscription> = emptyList(),
    val cycleStatuses: List<CycleStatus> = emptyList(),
    val urgentStatuses: List<CycleStatus> = emptyList(),
    val portfolioSummary: PortfolioSummary = PortfolioSummary(
        totalSubscriptions = 0,
        activeCount = 0,
        blackoutCount = 0,
        totalStandardAnnualCost = 0.0,
        totalOptimizedAnnualCost = 0.0,
        totalAnnualSavings = 0.0,
        totalMonthlyStandardCost = 0.0,
        totalMonthlyOptimizedCost = 0.0,
        totalMonthlySavings = 0.0,
        overallSavingsPercentage = 0.0,
        urgentActionsCount = 0
    ),
    val selectedFilter: SubscriptionFilter = SubscriptionFilter.ALL,
    val isLoading: Boolean = true
)

enum class SubscriptionFilter(val displayName: String) {
    ALL("All"),
    ACTIVE("Active Now"),
    BLACKOUT("In Blackout"),
    ROTATING("45d Rotators")
}

class DashboardViewModel(
    private val repository: SubscriptionRepository
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(SubscriptionFilter.ALL)
    val selectedFilter: StateFlow<SubscriptionFilter> = _selectedFilter

    val uiState: StateFlow<DashboardUiState> = combine(
        repository.getAllSubscriptions(),
        _selectedFilter
    ) { subs, filter ->
        val today = LocalDate.now()
        val statuses = subs.map { RotationEngine.calculateCycleStatus(it, today) }
        val portfolio = RotationEngine.calculatePortfolioSavings(statuses)
        val urgent = statuses.filter { it.nextAction.urgency != com.subtrack.app.data.model.ActionUrgency.INFO }

        val filteredStatuses = when (filter) {
            SubscriptionFilter.ALL -> statuses
            SubscriptionFilter.ACTIVE -> statuses.filter { it.phase == CyclePhase.ACTIVE }
            SubscriptionFilter.BLACKOUT -> statuses.filter { it.phase == CyclePhase.BLACKOUT }
            SubscriptionFilter.ROTATING -> statuses.filter { it.subscription.blackoutDays > 0 }
        }

        DashboardUiState(
            subscriptions = subs,
            cycleStatuses = filteredStatuses,
            urgentStatuses = urgent,
            portfolioSummary = portfolio,
            selectedFilter = filter,
            isLoading = false
        )
    }
    .flowOn(kotlinx.coroutines.Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun setFilter(filter: SubscriptionFilter) {
        _selectedFilter.value = filter
    }

    fun addSubscription(subscription: Subscription) {
        viewModelScope.launch {
            repository.addSubscription(subscription)
        }
    }

    fun updateSubscription(subscription: Subscription) {
        viewModelScope.launch {
            repository.updateSubscription(subscription)
        }
    }

    fun deleteSubscription(subscription: Subscription) {
        viewModelScope.launch {
            repository.deleteSubscription(subscription)
        }
    }

    fun toggleAutoRenew(subscriptionId: Long) {
        viewModelScope.launch {
            repository.toggleAutoRenewCancelled(subscriptionId)
        }
    }
}
