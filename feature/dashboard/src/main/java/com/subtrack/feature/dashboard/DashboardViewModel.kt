package com.subtrack.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subtrack.core.common.dispatcher.DefaultDispatcher
import com.subtrack.core.domain.model.ActionUrgency
import com.subtrack.core.domain.model.CyclePhase
import com.subtrack.core.domain.model.CycleStatus
import com.subtrack.core.domain.model.PortfolioSummary
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.domain.usecase.GetPortfolioSummaryUseCase
import com.subtrack.core.domain.usecase.GetSubscriptionCycleStatusesUseCase
import com.subtrack.core.domain.usecase.ManageSubscriptionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
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

sealed interface DashboardAction {
    data class SetFilter(val filter: SubscriptionFilter) : DashboardAction
    data class AddSubscription(val subscription: Subscription) : DashboardAction
    data class UpdateSubscription(val subscription: Subscription) : DashboardAction
    data class DeleteSubscription(val subscription: Subscription) : DashboardAction
    data class ToggleAutoRenew(val subscriptionId: Long) : DashboardAction
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getCycleStatusesUseCase: GetSubscriptionCycleStatusesUseCase,
    private val getPortfolioSummaryUseCase: GetPortfolioSummaryUseCase,
    private val manageSubscriptionUseCase: ManageSubscriptionUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _selectedFilter = MutableStateFlow(SubscriptionFilter.ALL)
    val selectedFilter: StateFlow<SubscriptionFilter> = _selectedFilter

    val uiState: StateFlow<DashboardUiState> = combine(
        getCycleStatusesUseCase(),
        _selectedFilter
    ) { statuses, filter ->
        val portfolio = getPortfolioSummaryUseCase(statuses)
        val urgent = statuses.filter { it.nextAction.urgency != ActionUrgency.INFO }

        val filteredStatuses = when (filter) {
            SubscriptionFilter.ALL -> statuses
            SubscriptionFilter.ACTIVE -> statuses.filter { it.phase == CyclePhase.ACTIVE }
            SubscriptionFilter.BLACKOUT -> statuses.filter { it.phase == CyclePhase.BLACKOUT }
            SubscriptionFilter.ROTATING -> statuses.filter { it.subscription.blackoutDays > 0 }
        }

        DashboardUiState(
            cycleStatuses = filteredStatuses,
            urgentStatuses = urgent,
            portfolioSummary = portfolio,
            selectedFilter = filter,
            isLoading = false
        )
    }
    .flowOn(defaultDispatcher)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun onAction(action: DashboardAction) {
        when (action) {
            is DashboardAction.SetFilter -> _selectedFilter.value = action.filter
            is DashboardAction.AddSubscription -> {
                viewModelScope.launch {
                    manageSubscriptionUseCase.addSubscription(action.subscription)
                }
            }
            is DashboardAction.UpdateSubscription -> {
                viewModelScope.launch {
                    manageSubscriptionUseCase.updateSubscription(action.subscription)
                }
            }
            is DashboardAction.DeleteSubscription -> {
                viewModelScope.launch {
                    manageSubscriptionUseCase.deleteSubscription(action.subscription)
                }
            }
            is DashboardAction.ToggleAutoRenew -> {
                viewModelScope.launch {
                    manageSubscriptionUseCase.toggleAutoRenew(action.subscriptionId)
                }
            }
        }
    }
}
