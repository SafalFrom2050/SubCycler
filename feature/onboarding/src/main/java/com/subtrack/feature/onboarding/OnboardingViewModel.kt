package com.subtrack.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.subtrack.core.common.time.TimeProvider
import com.subtrack.core.domain.engine.RotationEngine
import com.subtrack.core.domain.model.PresetLibrary
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.domain.model.SubscriptionCategory
import com.subtrack.core.domain.model.SubscriptionPreset
import com.subtrack.core.domain.repository.UserPreferencesRepository
import com.subtrack.core.domain.usecase.ManageSubscriptionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale
import javax.inject.Inject

enum class OnboardingStep {
    SELECT_SERVICES,
    REVIEW_DETAILS,
    STARTER_PLAN
}

enum class OnboardingCategory(
    val displayName: String,
    val subscriptionCategory: SubscriptionCategory? = null
) {
    POPULAR("Popular"),
    VIDEO("Video", SubscriptionCategory.STREAMING),
    MUSIC("Music", SubscriptionCategory.MUSIC),
    SOFTWARE("Software", SubscriptionCategory.AI_TOOLS),
    GAMING("Gaming", SubscriptionCategory.GAMING),
    CLOUD("Cloud", SubscriptionCategory.CLOUD)
}

data class OnboardingServiceDraft(
    val preset: SubscriptionPreset,
    val initials: String,
    val isPopular: Boolean,
    val isCustom: Boolean = false,
    val isSelected: Boolean = false,
    val costInput: String = String.format(Locale.US, "%.2f", preset.defaultCost),
    val nextBillingDate: LocalDate? = null
) {
    val parsedCost: Double?
        get() = costInput.toDoubleOrNull()?.takeIf { it > 0.0 }
}

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.SELECT_SERVICES,
    val services: List<OnboardingServiceDraft> = OnboardingServiceCatalog.services,
    val searchQuery: String = "",
    val selectedCategory: OnboardingCategory = OnboardingCategory.POPULAR,
    val currencyCode: String = "USD",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val isCompleted: Boolean = false,
    val shouldRequestNotificationPermission: Boolean = false
) {
    val selectedServices: List<OnboardingServiceDraft>
        get() = services.filter { it.isSelected }

    val visibleServices: List<OnboardingServiceDraft>
        get() {
            val normalizedQuery = searchQuery.trim().lowercase(Locale.getDefault())
            return services.filter { service ->
                val matchesCategory = if (normalizedQuery.isNotEmpty()) {
                    true
                } else {
                    when (selectedCategory) {
                        OnboardingCategory.POPULAR -> service.isPopular
                        else -> service.preset.category == selectedCategory.subscriptionCategory
                    }
                }
                val matchesQuery = normalizedQuery.isEmpty() ||
                    service.preset.name.lowercase(Locale.getDefault()).contains(normalizedQuery)
                matchesCategory && matchesQuery
            }
        }

    val monthlyCost: Double
        get() = selectedServices.sumOf { it.parsedCost ?: 0.0 }

    val potentialAnnualSavings: Double
        get() = selectedServices.sumOf { draft ->
            val cost = draft.parsedCost ?: 0.0
            if (draft.preset.suggestedStrategy == RotationStrategy.CONTINUOUS) {
                0.0
            } else {
                val preview = draft.toSubscription(
                    today = LocalDate.of(2026, 1, 1),
                    currency = currencyCode
                )
                RotationEngine.calculateCycleStatus(preview, preview.startDate).annualSavings
            }
        }
}

sealed interface OnboardingAction {
    data class SetSearchQuery(val query: String) : OnboardingAction
    data class SetCategory(val category: OnboardingCategory) : OnboardingAction
    data class SetCurrency(val currencyCode: String) : OnboardingAction
    data class ToggleService(val serviceName: String) : OnboardingAction
    data class AddCustomService(
        val serviceName: String,
        val category: SubscriptionCategory,
        val costInput: String
    ) : OnboardingAction
    data object ContinueToReview : OnboardingAction
    data object Back : OnboardingAction
    data class UpdateCost(val serviceName: String, val costInput: String) : OnboardingAction
    data class SetNextBillingDate(
        val serviceName: String,
        val nextBillingDate: LocalDate?
    ) : OnboardingAction
    data object ShowStarterPlan : OnboardingAction
    data object CompleteOnboarding : OnboardingAction
    data object SkipOnboarding : OnboardingAction
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val manageSubscriptionUseCase: ManageSubscriptionUseCase,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()
    private var hasExplicitCurrencySelection = false

    init {
        loadPreferredCurrency()
    }

    fun onAction(action: OnboardingAction) {
        when (action) {
            is OnboardingAction.SetSearchQuery -> _uiState.update {
                it.copy(searchQuery = action.query)
            }
            is OnboardingAction.SetCategory -> _uiState.update {
                it.copy(selectedCategory = action.category, errorMessage = null)
            }
            is OnboardingAction.SetCurrency -> setCurrency(action.currencyCode)
            is OnboardingAction.ToggleService -> updateService(action.serviceName) { draft ->
                draft.copy(isSelected = !draft.isSelected)
            }
            is OnboardingAction.AddCustomService -> addCustomService(action)
            OnboardingAction.ContinueToReview -> continueToReview()
            OnboardingAction.Back -> goBack()
            is OnboardingAction.UpdateCost -> updateService(action.serviceName) { draft ->
                draft.copy(costInput = action.costInput)
            }
            is OnboardingAction.SetNextBillingDate -> updateService(action.serviceName) { draft ->
                draft.copy(nextBillingDate = action.nextBillingDate)
            }
            OnboardingAction.ShowStarterPlan -> showStarterPlan()
            OnboardingAction.CompleteOnboarding -> completeOnboarding()
            OnboardingAction.SkipOnboarding -> skipOnboarding()
        }
    }

    private fun loadPreferredCurrency() {
        viewModelScope.launch {
            val currencyCode = userPreferencesRepository.userPreferences.first().currency
            if (!hasExplicitCurrencySelection) {
                _uiState.update { state ->
                    state.copy(
                        currencyCode = currencyCode,
                        services = state.services.forCurrency(currencyCode)
                    )
                }
            }
        }
    }

    private fun setCurrency(currencyCode: String) {
        hasExplicitCurrencySelection = true
        val normalizedCode = currencyCode.uppercase(Locale.US)
        _uiState.update { state ->
            state.copy(
                currencyCode = normalizedCode,
                services = state.services.forCurrency(normalizedCode),
                errorMessage = if (state.selectedServices.isNotEmpty()) {
                    "Currency changed to $normalizedCode. Confirm each selected price."
                } else {
                    null
                }
            )
        }
        viewModelScope.launch {
            userPreferencesRepository.setCurrency(normalizedCode)
        }
    }

    private fun addCustomService(action: OnboardingAction.AddCustomService) {
        val serviceName = action.serviceName.trim()
        val cost = action.costInput.toDoubleOrNull()?.takeIf { it > 0.0 }

        if (serviceName.isBlank() || cost == null) {
            _uiState.update {
                it.copy(errorMessage = "Enter a service name and a valid monthly price.")
            }
            return
        }

        _uiState.update { state ->
            val existingService = state.services.firstOrNull {
                it.preset.name.equals(serviceName, ignoreCase = true)
            }
            val updatedServices = if (existingService != null) {
                state.services.map { draft ->
                    if (draft === existingService) {
                        draft.copy(isSelected = true, costInput = action.costInput)
                    } else {
                        draft
                    }
                }
            } else {
                val preset = SubscriptionPreset(
                    name = serviceName,
                    defaultCost = cost,
                    category = action.category,
                    colorHex = "#38BDF8",
                    iconName = action.category.iconName,
                    suggestedStrategy = defaultCustomStrategy(action.category)
                )
                state.services + OnboardingServiceDraft(
                    preset = preset,
                    initials = initialsForName(serviceName),
                    isPopular = false,
                    isCustom = true,
                    isSelected = true,
                    costInput = action.costInput
                )
            }

            state.copy(
                services = updatedServices,
                searchQuery = "",
                errorMessage = null
            )
        }
    }

    private fun updateService(
        serviceName: String,
        transform: (OnboardingServiceDraft) -> OnboardingServiceDraft
    ) {
        _uiState.update { state ->
            state.copy(
                services = state.services.map { draft ->
                    if (draft.preset.name == serviceName) transform(draft) else draft
                },
                errorMessage = null
            )
        }
    }

    private fun continueToReview() {
        _uiState.update { state ->
            if (state.selectedServices.isEmpty()) {
                state.copy(errorMessage = "Choose at least one service to continue.")
            } else {
                state.copy(
                    step = OnboardingStep.REVIEW_DETAILS,
                    searchQuery = "",
                    errorMessage = null
                )
            }
        }
    }

    private fun goBack() {
        _uiState.update { state ->
            state.copy(
                step = when (state.step) {
                    OnboardingStep.SELECT_SERVICES -> OnboardingStep.SELECT_SERVICES
                    OnboardingStep.REVIEW_DETAILS -> OnboardingStep.SELECT_SERVICES
                    OnboardingStep.STARTER_PLAN -> OnboardingStep.REVIEW_DETAILS
                },
                errorMessage = null
            )
        }
    }

    private fun showStarterPlan() {
        _uiState.update { state ->
            val invalidService = state.selectedServices.firstOrNull { it.parsedCost == null }
            if (invalidService != null) {
                state.copy(errorMessage = "Enter a valid monthly price for ${invalidService.preset.name}.")
            } else {
                state.copy(step = OnboardingStep.STARTER_PLAN, errorMessage = null)
            }
        }
    }

    private fun completeOnboarding() {
        if (_uiState.value.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                val state = _uiState.value
                val currency = state.currencyCode
                val today = timeProvider.today()
                val subscriptions = state.selectedServices.map { draft ->
                    draft.toSubscription(today = today, currency = currency)
                }

                userPreferencesRepository.setCurrency(currency)
                manageSubscriptionUseCase.addSubscriptionsIfMissing(subscriptions)
                userPreferencesRepository.setOnboardingCompleted(true)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isCompleted = true,
                        shouldRequestNotificationPermission = subscriptions.isNotEmpty()
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "We couldn't save your starter stack. Please try again."
                    )
                }
            }
        }
    }

    private fun skipOnboarding() {
        if (_uiState.value.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            try {
                userPreferencesRepository.setCurrency(_uiState.value.currencyCode)
                userPreferencesRepository.setOnboardingCompleted(true)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        isCompleted = true,
                        shouldRequestNotificationPermission = false
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "We couldn't finish setup. Please try again."
                    )
                }
            }
        }
    }
}

private fun OnboardingServiceDraft.toSubscription(
    today: LocalDate,
    currency: String
): Subscription {
    val startDate = nextBillingDate?.minusDays(preset.suggestedActiveDays.toLong()) ?: today
    return Subscription(
        name = preset.name,
        cost = parsedCost ?: preset.defaultCost,
        currency = currency,
        category = preset.category,
        startDate = startDate,
        strategy = preset.suggestedStrategy,
        customActiveDays = preset.suggestedActiveDays,
        customBlackoutDays = preset.suggestedBlackoutDays,
        colorHex = preset.colorHex,
        iconName = preset.iconName,
        notes = if (nextBillingDate == null) {
            "Added during onboarding. Next billing date not confirmed."
        } else {
            "Added during onboarding. Next bill: $nextBillingDate."
        }
    )
}

private object OnboardingServiceCatalog {
    private val popularNames = setOf(
        "Netflix",
        "Disney+",
        "Max (HBO)",
        "Amazon Prime Video",
        "YouTube Premium",
        "Spotify",
        "Apple Music",
        "ChatGPT Plus",
        "Claude Pro",
        "Google AI Pro",
        "Notion Plus",
        "Xbox Game Pass Ultimate",
        "PlayStation Plus"
    )

    private val initialsByName = mapOf(
        "Netflix" to "N",
        "Disney+" to "D+",
        "Max (HBO)" to "M",
        "YouTube Premium" to "Y",
        "Spotify" to "S",
        "Apple Music" to "A",
        "Apple TV+" to "TV",
        "Amazon Prime Video" to "P",
        "ChatGPT Plus" to "AI",
        "Claude Pro" to "C",
        "Google AI Plus" to "G+",
        "Google AI Pro" to "GP",
        "Google AI Ultra" to "GU",
        "Notion Plus" to "No",
        "Xbox Game Pass Ultimate" to "X",
        "PlayStation Plus" to "PS",
        "iCloud+" to "iC",
        "Google One" to "G1"
    )

    val services: List<OnboardingServiceDraft> = PresetLibrary.presets.map { preset ->
        OnboardingServiceDraft(
            preset = preset,
            initials = initialsByName[preset.name] ?: initialsForName(preset.name),
            isPopular = preset.name in popularNames
        )
    }
}

private fun initialsForName(name: String): String {
    val words = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        words.isEmpty() -> "?"
        words.size == 1 -> words.first().take(2).uppercase(Locale.US)
        else -> words.take(2).joinToString("") { it.take(1) }.uppercase(Locale.US)
    }
}

private fun defaultCustomStrategy(category: SubscriptionCategory): RotationStrategy = when (category) {
    SubscriptionCategory.STREAMING,
    SubscriptionCategory.GAMING,
    SubscriptionCategory.AI_TOOLS -> RotationStrategy.ROTATION_45_DAYS
    SubscriptionCategory.MUSIC,
    SubscriptionCategory.CLOUD,
    SubscriptionCategory.FITNESS,
    SubscriptionCategory.NEWS,
    SubscriptionCategory.OTHER -> RotationStrategy.CONTINUOUS
}

private fun List<OnboardingServiceDraft>.forCurrency(currencyCode: String): List<OnboardingServiceDraft> =
    map { draft ->
        draft.copy(
            costInput = if (currencyCode == "USD" && !draft.isCustom) {
                String.format(Locale.US, "%.2f", draft.preset.defaultCost)
            } else {
                ""
            }
        )
    }
