package com.subtrack.app.ui.screens.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subtrack.app.data.model.Subscription
import com.subtrack.app.data.repository.SubscriptionRepository
import com.subtrack.app.domain.PortfolioSummary
import com.subtrack.app.domain.RotationEngine
import kotlinx.coroutines.flow.*
import java.time.LocalDate

data class AnalyticsUiState(
    val subscriptions: List<Subscription> = emptyList(),
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
    val simulatedRotationPct: Float = 0.5f, // Slider 0% to 100%
    val simulatedAnnualSavings: Double = 0.0,
    val isLoading: Boolean = true
)

class AnalyticsViewModel(
    private val repository: SubscriptionRepository
) : ViewModel() {

    private val _simulatedRotationPct = MutableStateFlow(0.75f)

    val uiState: StateFlow<AnalyticsUiState> = combine(
        repository.getAllSubscriptions(),
        _simulatedRotationPct
    ) { subs, simPct ->
        val today = LocalDate.now()
        val statuses = subs.map { RotationEngine.calculateCycleStatus(it, today) }
        val summary = RotationEngine.calculatePortfolioSavings(statuses)

        // Simulation: If simPct of all subs used 45-day rotation (saving 33.3%)
        val totalStandardAnnual = subs.sumOf { it.cost * 12.0 }
        val simulatedAnnualSavings = totalStandardAnnual * simPct * (15.0 / 45.0)

        AnalyticsUiState(
            subscriptions = subs,
            summary = summary,
            threeYearSavings = summary.totalAnnualSavings * 3.0,
            fiveYearSavings = summary.totalAnnualSavings * 5.0,
            simulatedRotationPct = simPct,
            simulatedAnnualSavings = simulatedAnnualSavings,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AnalyticsUiState()
    )

    fun setSimulatedRotationPct(pct: Float) {
        _simulatedRotationPct.value = pct
    }
}
