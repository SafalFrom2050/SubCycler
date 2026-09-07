package com.subtrack.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subtrack.core.domain.model.RotationStrategy
import com.subtrack.core.domain.model.SubscriptionCategory
import com.subtrack.core.common.currency.MoneyFormatter
import com.subtrack.core.ui.theme.DarkBorder
import com.subtrack.core.ui.theme.DarkSurface
import com.subtrack.core.ui.theme.DarkSurfaceVariant
import com.subtrack.core.ui.theme.OledBlack
import com.subtrack.core.ui.theme.PrimarySky
import com.subtrack.core.ui.theme.TextSecondaryDark
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun OnboardingRoute(
    onFinished: (requestNotificationPermission: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isCompleted) {
        if (uiState.isCompleted) {
            onFinished(uiState.shouldRequestNotificationPermission)
        }
    }

    OnboardingScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        modifier = modifier
    )
}

@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onAction: (OnboardingAction) -> Unit,
    modifier: Modifier = Modifier
) {
    var serviceRequestingDate by remember { mutableStateOf<String?>(null) }
    var showCustomServiceDialog by rememberSaveable { mutableStateOf(false) }
    var showCurrencyDialog by rememberSaveable { mutableStateOf(false) }
    var customServiceSeed by rememberSaveable { mutableStateOf("") }
    var customServiceCategory by rememberSaveable { mutableStateOf(SubscriptionCategory.OTHER) }

    BackHandler(enabled = uiState.step != OnboardingStep.SELECT_SERVICES) {
        onAction(OnboardingAction.Back)
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = OledBlack
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            OnboardingProgress(step = uiState.step)
            Spacer(modifier = Modifier.height(18.dp))

            when (uiState.step) {
                OnboardingStep.SELECT_SERVICES -> ServiceSelectionStep(
                    uiState = uiState,
                    onAction = onAction,
                    onChooseCurrency = { showCurrencyDialog = true },
                    onAddCustomService = { seed, category ->
                        customServiceSeed = seed
                        customServiceCategory = category
                        showCustomServiceDialog = true
                    }
                )
                OnboardingStep.REVIEW_DETAILS -> ServiceReviewStep(
                    uiState = uiState,
                    onAction = onAction,
                    onChooseDate = { serviceRequestingDate = it }
                )
                OnboardingStep.STARTER_PLAN -> StarterPlanStep(
                    uiState = uiState,
                    onAction = onAction
                )
            }
        }
    }

    serviceRequestingDate?.let { serviceName ->
        val draft = uiState.services.firstOrNull { it.preset.name == serviceName }
        if (draft != null) {
            key(serviceName) {
                BillingDatePicker(
                    serviceName = serviceName,
                    initialDate = draft.nextBillingDate,
                    onDismiss = { serviceRequestingDate = null },
                    onDateSelected = { date ->
                        onAction(OnboardingAction.SetNextBillingDate(serviceName, date))
                        serviceRequestingDate = null
                    }
                )
            }
        }
    }

    if (showCustomServiceDialog) {
        CustomServiceDialog(
            initialName = customServiceSeed,
            initialCategory = customServiceCategory,
            currencyCode = uiState.currencyCode,
            onDismiss = { showCustomServiceDialog = false },
            onAdd = { serviceName, category, costInput ->
                onAction(
                    OnboardingAction.AddCustomService(
                        serviceName = serviceName,
                        category = category,
                        costInput = costInput
                    )
                )
                showCustomServiceDialog = false
            }
        )
    }

    if (showCurrencyDialog) {
        CurrencyPickerDialog(
            selectedCurrencyCode = uiState.currencyCode,
            onDismiss = { showCurrencyDialog = false },
            onSelect = { currencyCode ->
                onAction(OnboardingAction.SetCurrency(currencyCode))
                showCurrencyDialog = false
            }
        )
    }
}

@Composable
private fun OnboardingProgress(step: OnboardingStep) {
    val activeStep = step.ordinal
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(OnboardingStep.entries.size) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(if (index <= activeStep) PrimarySky else DarkBorder)
            )
        }
    }
}

@Composable
private fun ServiceSelectionStep(
    uiState: OnboardingUiState,
    onAction: (OnboardingAction) -> Unit,
    onChooseCurrency: () -> Unit,
    onAddCustomService: (String, SubscriptionCategory) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Column(modifier = Modifier.fillMaxSize()) {
        OnboardingHeader(
            eyebrow = "BUILD YOUR STACK",
            title = "What are you paying for?",
            subtitle = "Tap everything that looks familiar. No logins, linking, or detective work."
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { onAction(OnboardingAction.SetSearchQuery(it)) },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search a service") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = onboardingTextFieldColors()
        )

        val searchSeed = uiState.searchQuery.trim()
        val hasExactMatch = uiState.services.any {
            it.preset.name.equals(searchSeed, ignoreCase = true)
        }
        val customButtonText = if (searchSeed.isNotEmpty() && !hasExactMatch) {
            "Add \"$searchSeed\" as custom"
        } else {
            "Add a custom service"
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onChooseCurrency,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text(
                    text = "${MoneyFormatter.symbol(uiState.currencyCode)} ${uiState.currencyCode}",
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = "Choose currency",
                    modifier = Modifier.size(17.dp)
                )
            }
            TextButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onAddCustomService(
                        searchSeed.takeIf { it.isNotEmpty() && !hasExactMatch }.orEmpty(),
                        uiState.selectedCategory.subscriptionCategory ?: SubscriptionCategory.OTHER
                    )
                },
                modifier = Modifier.weight(1f, fill = false),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = customButtonText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            contentPadding = PaddingValues(end = 8.dp)
        ) {
            items(OnboardingCategory.entries, key = { it.name }) { category ->
                val selected = category == uiState.selectedCategory
                FilterChip(
                    selected = selected,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onAction(OnboardingAction.SetCategory(category))
                    },
                    label = {
                        Text(
                            text = category.displayName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    shape = RoundedCornerShape(999.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = DarkSurface,
                        selectedContainerColor = PrimarySky.copy(alpha = 0.14f),
                        labelColor = TextSecondaryDark,
                        selectedLabelColor = PrimarySky
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected,
                        borderColor = DarkBorder,
                        selectedBorderColor = PrimarySky.copy(alpha = 0.7f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (uiState.visibleServices.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No preset found — add it as a custom service above.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(
                    items = uiState.visibleServices,
                    key = { it.preset.name }
                ) { service ->
                    ServiceTile(
                        service = service,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onAction(OnboardingAction.ToggleService(service.preset.name))
                        }
                    )
                }
            }
        }

        uiState.errorMessage?.let { message ->
            Text(
                text = message,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }

        val selectedCount = uiState.selectedServices.size
        Text(
            text = selectionMessage(selectedCount),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp, bottom = 8.dp),
            color = if (selectedCount > 0) PrimarySky else TextSecondaryDark,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        PrimaryOnboardingButton(
            text = if (selectedCount == 0) {
                "Review my stack"
            } else {
                "Review $selectedCount service${if (selectedCount == 1) "" else "s"}"
            },
            enabled = selectedCount > 0,
            onClick = { onAction(OnboardingAction.ContinueToReview) }
        )

        TextButton(
            onClick = { onAction(OnboardingAction.SkipOnboarding) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSaving
        ) {
            Text("I’ll add services later", color = TextSecondaryDark)
        }
    }
}

@Composable
private fun CurrencyPickerDialog(
    selectedCurrencyCode: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    val locale = Locale.getDefault()
    val currencies = remember(selectedCurrencyCode, locale) {
        MoneyFormatter.availableCurrencyCodes(selectedCurrencyCode, locale)
    }
    val filteredCurrencies = remember(query, currencies, locale) {
        val normalizedQuery = query.trim().lowercase(locale)
        if (normalizedQuery.isEmpty()) {
            currencies
        } else {
            currencies.filter { code ->
                code.lowercase(locale).contains(normalizedQuery) ||
                    MoneyFormatter.displayName(code, locale).lowercase(locale).contains(normalizedQuery)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose your currency", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Suggested from your device, but you’re always in control. No exchange-rate conversion is applied.",
                    color = TextSecondaryDark,
                    fontSize = 12.sp
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search code or currency") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = onboardingTextFieldColors()
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    items(filteredCurrencies, key = { it }) { code ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(9.dp))
                                .clickable { onSelect(code) }
                                .padding(horizontal = 10.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = MoneyFormatter.symbol(code, locale),
                                modifier = Modifier.width(38.dp),
                                color = PrimarySky,
                                fontWeight = FontWeight.Bold
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(code, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(
                                    text = MoneyFormatter.displayName(code, locale),
                                    color = TextSecondaryDark,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (code == selectedCurrencyCode) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = PrimarySky,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        containerColor = DarkSurfaceVariant
    )
}

@Composable
private fun CustomServiceDialog(
    initialName: String,
    initialCategory: SubscriptionCategory,
    currencyCode: String,
    onDismiss: () -> Unit,
    onAdd: (String, SubscriptionCategory, String) -> Unit
) {
    var serviceName by rememberSaveable(initialName) { mutableStateOf(initialName) }
    var costInput by rememberSaveable(initialName) { mutableStateOf("") }
    var category by rememberSaveable(initialCategory) { mutableStateOf(initialCategory) }
    val isValid = serviceName.trim().isNotEmpty() &&
        costInput.toDoubleOrNull()?.let { it > 0.0 } == true

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add your own service",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Not in the catalog? Name it, enter what you actually pay, and it joins your stack.",
                    color = TextSecondaryDark,
                    fontSize = 12.sp
                )
                OutlinedTextField(
                    value = serviceName,
                    onValueChange = { serviceName = it.take(60) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Service name") },
                    placeholder = { Text("e.g. My VPN") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = onboardingTextFieldColors()
                )
                OutlinedTextField(
                    value = costInput,
                    onValueChange = { costInput = it.take(12) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Monthly price ($currencyCode)") },
                    prefix = { Text(MoneyFormatter.symbol(currencyCode)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = costInput.isNotEmpty() && costInput.toDoubleOrNull()?.let { it > 0.0 } != true,
                    shape = RoundedCornerShape(10.dp),
                    colors = onboardingTextFieldColors()
                )
                Text(
                    text = "CATEGORY",
                    color = TextSecondaryDark,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp
                )
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    SubscriptionCategory.entries.chunked(4).forEach { categoryRow ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            categoryRow.forEach { option ->
                                FilterChip(
                                    selected = option == category,
                                    onClick = { category = option },
                                    modifier = Modifier.weight(1f),
                                    label = {
                                        Text(
                                            text = customCategoryLabel(option),
                                            modifier = Modifier.fillMaxWidth(),
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            textAlign = TextAlign.Center
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        containerColor = DarkSurface,
                                        selectedContainerColor = PrimarySky.copy(alpha = 0.14f),
                                        labelColor = TextSecondaryDark,
                                        selectedLabelColor = PrimarySky
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = option == category,
                                        borderColor = DarkBorder,
                                        selectedBorderColor = PrimarySky.copy(alpha = 0.7f)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAdd(serviceName.trim(), category, costInput) },
                enabled = isValid
            ) {
                Text("Add to my stack")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        containerColor = DarkSurfaceVariant
    )
}

private fun customCategoryLabel(category: SubscriptionCategory): String = when (category) {
    SubscriptionCategory.STREAMING -> "Video"
    SubscriptionCategory.MUSIC -> "Music"
    SubscriptionCategory.GAMING -> "Gaming"
    SubscriptionCategory.AI_TOOLS -> "AI/Apps"
    SubscriptionCategory.CLOUD -> "Cloud"
    SubscriptionCategory.FITNESS -> "Fitness"
    SubscriptionCategory.NEWS -> "News"
    SubscriptionCategory.OTHER -> "Other"
}

@Composable
private fun ServiceTile(
    service: OnboardingServiceDraft,
    onClick: () -> Unit
) {
    val brandColor = colorFromHex(service.preset.colorHex)
    val borderColor = if (service.isSelected) PrimarySky else DarkBorder
    val backgroundColor = if (service.isSelected) {
        PrimarySky.copy(alpha = 0.13f)
    } else {
        DarkSurface
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.05f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        color = backgroundColor,
        border = BorderStroke(1.dp, borderColor)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (service.isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp)
                        .size(18.dp)
                        .background(PrimarySky, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = OledBlack,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(brandColor, RoundedCornerShape(11.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = service.initials,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
                Spacer(modifier = Modifier.height(7.dp))
                Text(
                    text = service.preset.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ServiceReviewStep(
    uiState: OnboardingUiState,
    onAction: (OnboardingAction) -> Unit,
    onChooseDate: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        BackTitleRow(onBack = { onAction(OnboardingAction.Back) })
        OnboardingHeader(
            eyebrow = "QUICK CHECK",
            title = "Make the estimates yours.",
            subtitle = "Fix what you know. Everything stays editable after setup."
        )

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = DarkSurface,
            border = BorderStroke(1.dp, DarkBorder)
        ) {
            Column(modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp)) {
                Text(
                    text = "MONTHLY PRICE + NEXT BILL",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.7.sp
                )
                Text(
                    text = if (uiState.currencyCode == "USD") {
                        "Prices are editable USD estimates. Leave a date unset if you’re unsure."
                    } else {
                        "Enter the price you actually pay in ${uiState.currencyCode}; USD estimates are left blank."
                    },
                    color = TextSecondaryDark,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(9.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(uiState.selectedServices, key = { it.preset.name }) { service ->
                ReviewServiceCard(
                    service = service,
                    currencyCode = uiState.currencyCode,
                    onCostChanged = {
                        onAction(OnboardingAction.UpdateCost(service.preset.name, it))
                    },
                    onChooseDate = { onChooseDate(service.preset.name) }
                )
            }
        }

        Text(
            text = "✦ You can fine-tune rotation dates later",
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            color = TextSecondaryDark,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )

        uiState.errorMessage?.let { message ->
            Text(
                text = message,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 7.dp),
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }

        PrimaryOnboardingButton(
            text = "Show my starter plan",
            onClick = { onAction(OnboardingAction.ShowStarterPlan) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        SecondaryOnboardingButton(
            text = "Back to services",
            onClick = { onAction(OnboardingAction.Back) }
        )
    }
}

@Composable
private fun ReviewServiceCard(
    service: OnboardingServiceDraft,
    currencyCode: String,
    onCostChanged: (String) -> Unit,
    onChooseDate: () -> Unit
) {
    val brandColor = colorFromHex(service.preset.colorHex)
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d, yyyy") }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(brandColor, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = service.initials,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
                Spacer(modifier = Modifier.width(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = service.preset.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (service.preset.suggestedStrategy == RotationStrategy.CONTINUOUS) {
                            "Continuous plan"
                        } else {
                            "45-day rotation suggested"
                        },
                        color = if (service.preset.suggestedStrategy == RotationStrategy.CONTINUOUS) {
                            TextSecondaryDark
                        } else {
                            PrimarySky
                        },
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(9.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = service.costInput,
                    onValueChange = onCostChanged,
                    modifier = Modifier.weight(0.9f),
                    label = { Text("Price ($currencyCode)") },
                    prefix = { Text(MoneyFormatter.symbol(currencyCode)) },
                    singleLine = true,
                    isError = service.parsedCost == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(9.dp),
                    colors = onboardingTextFieldColors()
                )

                Surface(
                    modifier = Modifier
                        .weight(1.25f)
                        .height(56.dp)
                        .clickable(onClick = onChooseDate),
                    shape = RoundedCornerShape(9.dp),
                    color = DarkSurfaceVariant,
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = PrimarySky,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(7.dp))
                        Column {
                            Text(
                                text = "NEXT BILL",
                                color = TextSecondaryDark,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = service.nextBillingDate?.format(dateFormatter) ?: "Not set",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StarterPlanStep(
    uiState: OnboardingUiState,
    onAction: (OnboardingAction) -> Unit
) {
    val rotatableCount = uiState.selectedServices.count {
        it.preset.suggestedStrategy != RotationStrategy.CONTINUOUS
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(124.dp)
                .background(PrimarySky.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(82.dp)
                    .background(DarkSurface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = PrimarySky,
                    modifier = Modifier.size(34.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(22.dp))

        Text(
            text = "Your stack is ready.",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 29.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.8).sp,
            textAlign = TextAlign.Center
        )
        Text(
            text = "SubCycler built a starter rotation. Nothing is locked in.",
            modifier = Modifier.padding(top = 8.dp, start = 20.dp, end = 20.dp),
            color = TextSecondaryDark,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(18.dp))

        SelectedServiceAvatars(services = uiState.selectedServices)

        Spacer(modifier = Modifier.height(18.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            PayoffCard(
                label = "TRACKED MONTHLY",
                value = MoneyFormatter.format(uiState.monthlyCost, uiState.currencyCode),
                valueColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            PayoffCard(
                label = "POSSIBLE YEARLY SAVE",
                value = MoneyFormatter.format(
                    uiState.potentialAnnualSavings,
                    uiState.currencyCode,
                    maximumFractionDigits = 0
                ),
                valueColor = Color(0xFF4ADE80),
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = if (rotatableCount == 0) {
                "Your selected services start as continuous plans."
            } else {
                "$rotatableCount service${if (rotatableCount == 1) "" else "s"} start with a suggested smart rotation."
            },
            modifier = Modifier.padding(top = 12.dp),
            color = TextSecondaryDark,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )

        Row(
            modifier = Modifier.padding(top = 16.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = Color(0xFF4ADE80),
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Private, offline, and no accounts connected",
                color = TextSecondaryDark,
                fontSize = 11.sp
            )
        }

        uiState.errorMessage?.let { message ->
            Text(
                text = message,
                modifier = Modifier.padding(bottom = 8.dp),
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }

        PrimaryOnboardingButton(
            text = if (uiState.isSaving) "Building your dashboard…" else "Open my dashboard",
            enabled = !uiState.isSaving,
            onClick = { onAction(OnboardingAction.CompleteOnboarding) },
            showProgress = uiState.isSaving
        )

        TextButton(
            onClick = { onAction(OnboardingAction.Back) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isSaving
        ) {
            Text("Edit my answers", color = TextSecondaryDark)
        }
    }
}

@Composable
private fun SelectedServiceAvatars(services: List<OnboardingServiceDraft>) {
    val visibleServices = services.take(5)
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        visibleServices.forEach { service ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(39.dp)
                    .background(colorFromHex(service.preset.colorHex), RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = service.initials,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        if (services.size > visibleServices.size) {
            Box(
                modifier = Modifier
                    .padding(start = 3.dp)
                    .size(39.dp)
                    .background(DarkSurfaceVariant, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+${services.size - visibleServices.size}",
                    color = PrimarySky,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun PayoffCard(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Column(modifier = Modifier.padding(13.dp)) {
            Text(
                text = label,
                color = TextSecondaryDark,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.55.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = 20.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
private fun OnboardingHeader(
    eyebrow: String,
    title: String,
    subtitle: String
) {
    Text(
        text = eyebrow,
        color = PrimarySky,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.3.sp
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = title,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 28.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.7).sp
    )
    Spacer(modifier = Modifier.height(7.dp))
    Text(
        text = subtitle,
        color = TextSecondaryDark,
        fontSize = 13.sp,
        lineHeight = 18.sp
    )
}

@Composable
private fun BackTitleRow(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun PrimaryOnboardingButton(
    text: String,
    enabled: Boolean = true,
    showProgress: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        enabled = enabled,
        shape = RoundedCornerShape(13.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimarySky,
            contentColor = OledBlack,
            disabledContainerColor = Color(0xFF1F2C32),
            disabledContentColor = Color(0xFF71818A)
        )
    ) {
        if (showProgress) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                color = OledBlack,
                strokeWidth = 2.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text = text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
private fun SecondaryOnboardingButton(
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(13.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BillingDatePicker(
    serviceName: String,
    initialDate: LocalDate?,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate?) -> Unit
) {
    val fallbackDate = remember { LocalDate.now().plusDays(30) }
    val initialMillis = (initialDate ?: fallbackDate)
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli()
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val selectedDate = pickerState.selectedDateMillis?.let { millis ->
                        Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                    }
                    onDateSelected(selectedDate)
                }
            ) {
                Text("Use date")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = { onDateSelected(null) }) {
                    Text("Not sure")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        }
    ) {
        Column {
            Text(
                text = "$serviceName next billing date",
                modifier = Modifier.padding(start = 24.dp, top = 18.dp),
                style = MaterialTheme.typography.titleMedium
            )
            HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
            DatePicker(state = pickerState, showModeToggle = false)
        }
    }
}

@Composable
private fun onboardingTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = DarkSurfaceVariant,
    unfocusedContainerColor = DarkSurface,
    focusedBorderColor = PrimarySky,
    unfocusedBorderColor = DarkBorder,
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    focusedLabelColor = PrimarySky,
    unfocusedLabelColor = TextSecondaryDark,
    focusedPlaceholderColor = TextSecondaryDark,
    unfocusedPlaceholderColor = TextSecondaryDark,
    focusedLeadingIconColor = PrimarySky,
    unfocusedLeadingIconColor = TextSecondaryDark
)

private fun selectionMessage(selectedCount: Int): String = when (selectedCount) {
    0 -> "Pick your first service"
    1 -> "1 picked · good start"
    2 -> "2 picked · stack taking shape"
    3 -> "3 picked · nice stack"
    else -> "$selectedCount picked · ready for a quick check"
}

private fun colorFromHex(hex: String): Color {
    return runCatching { Color(android.graphics.Color.parseColor(hex)) }
        .getOrDefault(PrimarySky)
}
