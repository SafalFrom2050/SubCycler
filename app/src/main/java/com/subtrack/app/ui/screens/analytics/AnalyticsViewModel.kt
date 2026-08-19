package com.subtrack.app.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subtrack.app.di.DefaultDispatcher
import com.subtrack.app.domain.PortfolioSummary
import com.subtrack.app.domain.usecase.GetPortfolioSummaryUseCase
import com.subtrack.app.domain.usecase.GetSubscriptionCycleStatusesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class AnalyticsUiState(
    val summary: PortfolioSummary = PortfolioSummary(
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
    val threeYearSavings: Double = 0.0,
    val fiveYearSavings: Double = 0.0,
    val simulatedRotationPct: Float = 0.75f, // Slider 0% to 100%
    val simulatedAnnualSavings: Double = 0.0,
    val isLoading: Boolean = true
)

sealed interface AnalyticsAction {
    data class SetSimulatedRotationPct(val pct: Float) : AnalyticsAction
}

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val getCycleStatusesUseCase: GetSubscriptionCycleStatusesUseCase,
    private val getPortfolioSummaryUseCase: GetPortfolioSummaryUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _simulatedRotationPct = MutableStateFlow(0.75f)

    val uiState: StateFlow<AnalyticsUiState> = combine(
        getCycleStatusesUseCase(),
        _simulatedRotationPct
    ) { statuses, simPct ->
        val summary = getPortfolioSummaryUseCase(statuses)

        // Simulation: If simPct of all subs used 45-day rotation (saving 33.3%)
        val totalStandardAnnual = statuses.sumOf { it.subscription.cost * 12.0 }
        val simulatedAnnualSavings = totalStandardAnnual * simPct * (15.0 / 45.0)

        AnalyticsUiState(
            summary = summary,
            threeYearSavings = summary.totalAnnualSavings * 3.0,
            fiveYearSavings = summary.totalAnnualSavings * 5.0,
            simulatedRotationPct = simPct,
            simulatedAnnualSavings = simulatedAnnualSavings,
            isLoading = false
        )
    }
    .flowOn(defaultDispatcher)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsUiState()
    )

    fun onAction(action: AnalyticsAction) {
        when (action) {
            is AnalyticsAction.SetSimulatedRotationPct -> _simulatedRotationPct.value = action.pct
        }
    }
}
