package com.subtrack.app.ui.screens.rotator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subtrack.app.data.model.RotationStrategy
import com.subtrack.app.data.model.Subscription
import com.subtrack.app.data.repository.SubscriptionRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

data class PairedRotatorUiState(
    val subscriptions: List<Subscription> = emptyList(),
    val service1: Subscription? = null,
    val service2: Subscription? = null,
    val combinedMonthlyCostBefore: Double = 0.0,
    val combinedMonthlyCostAfter: Double = 0.0,
    val combinedAnnualSavings: Double = 0.0,
    val isAligned: Boolean = false,
    val isLoading: Boolean = true
)

class PairedRotatorViewModel(
    private val repository: SubscriptionRepository
) : ViewModel() {

    private val _selectedId1 = MutableStateFlow<Long?>(null)
    private val _selectedId2 = MutableStateFlow<Long?>(null)

    val uiState: StateFlow<PairedRotatorUiState> = combine(
        repository.getAllSubscriptions(),
        _selectedId1,
        _selectedId2
    ) { subs, id1, id2 ->
        // Default to first two streaming services if not selected
        val s1 = subs.find { it.id == id1 } ?: subs.firstOrNull()
        val s2 = subs.find { it.id == id2 } ?: subs.getOrNull(1)

        val costBefore = (s1?.cost ?: 0.0) + (s2?.cost ?: 0.0)
        // In alternating 30d/30d mode, you pay for 1 service each month
        val costAfter = if (s1 != null && s2 != null) (s1.cost + s2.cost) / 2.0 else costBefore
        val annualSavings = (costBefore - costAfter) * 12.0

        PairedRotatorUiState(
            subscriptions = subs,
            service1 = s1,
            service2 = s2,
            combinedMonthlyCostBefore = costBefore,
            combinedMonthlyCostAfter = costAfter,
            combinedAnnualSavings = annualSavings,
            isLoading = false
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PairedRotatorUiState()
    )

    fun selectService1(id: Long) {
        _selectedId1.value = id
    }

    fun selectService2(id: Long) {
        _selectedId2.value = id
    }

    fun alignAndActivateAlternatingRotation() {
        val s1 = uiState.value.service1 ?: return
        val s2 = uiState.value.service2 ?: return

        viewModelScope.launch {
            val today = LocalDate.now()
            // S1 active now (30d active + 30d blackout)
            val updatedS1 = s1.copy(
                startDate = today,
                strategy = RotationStrategy.ROTATION_60_DAYS,
                customActiveDays = 30,
                customBlackoutDays = 30,
                pairedWithSubscriptionId = s2.id
            )

            // S2 blackout now, activates in 30 days
            val updatedS2 = s2.copy(
                startDate = today.minusDays(30), // Effectively puts S2 in blackout phase for first 30 days
                strategy = RotationStrategy.ROTATION_60_DAYS,
                customActiveDays = 30,
                customBlackoutDays = 30,
                pairedWithSubscriptionId = s1.id
            )

            repository.updateSubscription(updatedS1)
            repository.updateSubscription(updatedS2)
        }
    }
}
