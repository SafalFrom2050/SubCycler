package com.subtrack.feature.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subtrack.core.domain.model.CycleStatus
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.ui.components.*
import com.subtrack.core.ui.theme.OledBlack
import com.subtrack.core.ui.theme.PrimarySky

@Composable
fun DashboardRoute(
    viewModel: DashboardViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DashboardScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    uiState: DashboardUiState,
    onAction: (DashboardAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    var showAddDialog by remember { mutableStateOf(false) }
    var subscriptionToEdit by remember { mutableStateOf<Subscription?>(null) }
    var selectedStatusForDetail by remember { mutableStateOf<CycleStatus?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SubCycler",
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                            color = PrimarySky.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                PrimarySky.copy(alpha = 0.3f)
                            )
                        ) {
                            Text(
                                text = "AGENTIC 45d",
                                color = PrimarySky,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    subscriptionToEdit = null
                    showAddDialog = true
                },
                containerColor = PrimarySky,
                contentColor = OledBlack,
                shape = androidx.compose.foundation.shape.CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 3.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Subscription")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Variation 3: Financial Efficiency Bar HUD (Borderless command header)
            item(key = "financial_efficiency_hud", contentType = "hud") {
                FinancialEfficiencyHud(summary = uiState.portfolioSummary)
            }

            // Urgent / Due Next Action Banner
            if (uiState.urgentStatuses.isNotEmpty()) {
                item(key = "urgent_banner", contentType = "urgent") {
                    NextActionBanner(urgentStatuses = uiState.urgentStatuses)
                }
            }

            // Section Header & Filter Chips
            item(key = "filter_row", contentType = "filters") {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "App Drawer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            letterSpacing = (-0.2).sp
                        )
                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                            color = Color(0xFF141414),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                            )
                        ) {
                            Text(
                                text = "${uiState.cycleStatuses.size} services",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(SubscriptionFilter.values(), key = { it.name }) { filter ->
                            val isSelected = filter == uiState.selectedFilter
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onAction(DashboardAction.SetFilter(filter))
                                },
                                label = {
                                    Text(
                                        text = filter.displayName,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp
                                    )
                                },
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color(0xFF0D0D0D),
                                    selectedContainerColor = PrimarySky.copy(alpha = 0.16f),
                                    selectedLabelColor = PrimarySky,
                                    labelColor = Color(0xFF8E8E93)
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = isSelected,
                                    borderColor = if (isSelected) PrimarySky.copy(alpha = 0.85f) else Color(0xFF222222),
                                    borderWidth = 1.dp
                                )
                            )
                        }
                    }
                }
            }

            // Subscriptions 3-Column Grid
            if (uiState.cycleStatuses.isEmpty()) {
                item(key = "empty_state", contentType = "empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No subscriptions in this view",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tap '+' to add a subscription and start rotating!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Chunk into rows of 3
                val rows = uiState.cycleStatuses.chunked(3)
                items(
                    items = rows,
                    key = { row -> row.joinToString("-") { it.subscription.id.toString() } },
                    contentType = { "grid_row" }
                ) { rowItems ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateItem(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (cycleStatus in rowItems) {
                            Box(modifier = Modifier.weight(1f)) {
                                SubscriptionGridItem(
                                    cycleStatus = cycleStatus,
                                    onClick = {
                                        selectedStatusForDetail = cycleStatus
                                    }
                                )
                            }
                        }
                        // Fill remaining slots in incomplete row
                        val remaining = 3 - rowItems.size
                        for (i in 0 until remaining) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }

    // Detail Bottom Sheet when an item is tapped
    selectedStatusForDetail?.let { status ->
        SubscriptionDetailBottomSheet(
            cycleStatus = status,
            onDismiss = { selectedStatusForDetail = null },
            onToggleAutoRenew = {
                onAction(DashboardAction.ToggleAutoRenew(status.subscription.id))
                // Refresh local status reference
                selectedStatusForDetail = uiState.cycleStatuses.find { it.subscription.id == status.subscription.id }
            },
            onEdit = {
                subscriptionToEdit = status.subscription
                showAddDialog = true
            },
            onDelete = {
                onAction(DashboardAction.DeleteSubscription(status.subscription))
            }
        )
    }

    // Add / Edit Dialog
    if (showAddDialog) {
        AddEditSubscriptionDialog(
            initialSubscription = subscriptionToEdit,
            onDismiss = {
                showAddDialog = false
                subscriptionToEdit = null
            },
            onSave = { sub ->
                if (subscriptionToEdit != null) {
                    onAction(DashboardAction.UpdateSubscription(sub))
                } else {
                    onAction(DashboardAction.AddSubscription(sub))
                }
                showAddDialog = false
                subscriptionToEdit = null
            }
        )
    }
}
