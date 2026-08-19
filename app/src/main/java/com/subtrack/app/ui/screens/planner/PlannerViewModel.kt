package com.subtrack.app.ui.screens.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subtrack.app.data.model.CycleStatus
import com.subtrack.app.data.model.Subscription
import com.subtrack.app.data.repository.SubscriptionRepository
import com.subtrack.app.domain.RotationEngine
import kotlinx.coroutines.flow.*
import java.time.LocalDate

data class PlannerUiState(
    val cycleStatuses: List<CycleStatus> = emptyList(),
    val selectedTargetDate: LocalDate = LocalDate.now(),
    val activeOnSelectedDate: List<String> = emptyList(),
    val blackoutOnSelectedDate: List<String> = emptyList(),
    val isLoading: Boolean = true
)

class PlannerViewModel(
    private val repository: SubscriptionRepository
) : ViewModel() {

    private val _selectedTargetDate = MutableStateFlow(LocalDate.now())
    val selectedTargetDate: StateFlow<LocalDate> = _selectedTargetDate

    val uiState: StateFlow<PlannerUiState> = combine(
        repository.getAllSubscriptions(),
        _selectedTargetDate
    ) { subs, targetDate ->
        val today = LocalDate.now()
        val statuses = subs.map { RotationEngine.calculateCycleStatus(it, today) }

        // Projections for selected date
        val activeList = mutableListOf<String>()
        val blackoutList = mutableListOf<String>()

        subs.forEach { sub ->
            val statusAtDate = RotationEngine.calculateCycleStatus(sub, targetDate)
            if (statusAtDate.phase == com.subtrack.app.data.model.CyclePhase.ACTIVE) {
                activeList.add(sub.name)
            } else {
                blackoutList.add(sub.name)
            }
        }

        PlannerUiState(
            cycleStatuses = statuses,
            selectedTargetDate = targetDate,
            activeOnSelectedDate = activeList,
            blackoutOnSelectedDate = blackoutList,
            isLoading = false
        )
    }
    .flowOn(kotlinx.coroutines.Dispatchers.Default)
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlannerUiState()
    )

    fun setSelectedDate(date: LocalDate) {
        _selectedTargetDate.value = date
    }

    fun stepDate(days: Long) {
        _selectedTargetDate.value = _selectedTargetDate.value.plusDays(days)
    }
}
