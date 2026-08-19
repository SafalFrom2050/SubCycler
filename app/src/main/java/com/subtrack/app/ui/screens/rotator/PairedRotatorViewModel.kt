package com.subtrack.app.ui.screens.rotator

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subtrack.app.data.model.Subscription
import com.subtrack.app.data.repository.SubscriptionRepository
import com.subtrack.app.di.DefaultDispatcher
import com.subtrack.app.domain.usecase.AlignPairedSubscriptionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

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

sealed interface PairedRotatorAction {
    data class SelectService1(val id: Long) : PairedRotatorAction
    data class SelectService2(val id: Long) : PairedRotatorAction
    object AlignAndActivate : PairedRotatorAction
}

@HiltViewModel
class PairedRotatorViewModel @Inject constructor(
    private val repository: SubscriptionRepository,
    private val alignPairedSubscriptionsUseCase: AlignPairedSubscriptionsUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
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
    }
    .flowOn(defaultDispatcher)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PairedRotatorUiState()
    )

    fun onAction(action: PairedRotatorAction) {
        when (action) {
            is PairedRotatorAction.SelectService1 -> _selectedId1.value = action.id
            is PairedRotatorAction.SelectService2 -> _selectedId2.value = action.id
            is PairedRotatorAction.AlignAndActivate -> {
                val s1 = uiState.value.service1 ?: return
                val s2 = uiState.value.service2 ?: return
                viewModelScope.launch {
                    alignPairedSubscriptionsUseCase(s1.id, s2.id)
                }
            }
        }
    }
}
