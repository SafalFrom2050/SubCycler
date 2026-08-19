package com.subtrack.app.ui.screens.dashboard

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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subtrack.app.data.model.Subscription
import com.subtrack.app.ui.components.*
import com.subtrack.app.ui.theme.Emerald500
import com.subtrack.app.ui.theme.LightSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current

    var showAddDialog by remember { mutableStateOf(false) }
    var subscriptionToEdit by remember { mutableStateOf<Subscription?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SubCycler",
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
                            color = Emerald500.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "AGENTIC 45d",
                                color = Emerald500,
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
                containerColor = Emerald500,
                contentColor = LightSurface
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
            // High Impact Savings Summary Card
            item(key = "savings_summary", contentType = "summary") {
                SavingsSummaryCard(summary = uiState.portfolioSummary)
            }

            // Urgent / Due Next Action Banner
            if (uiState.urgentStatuses.isNotEmpty()) {
                item(key = "urgent_banner", contentType = "urgent") {
                    NextActionBanner(urgentStatuses = uiState.urgentStatuses)
                }
            }

            // Filter Chips
            item(key = "filter_row", contentType = "filters") {
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
                                viewModel.setFilter(filter)
                            },
                            label = {
                                Text(
                                    text = filter.displayName,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Emerald500.copy(alpha = 0.2f),
                                selectedLabelColor = Emerald500
                            )
                        )
                    }
                }
            }

            // Subscriptions List
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
                items(
                    items = uiState.cycleStatuses,
                    key = { it.subscription.id },
                    contentType = { "subscription_card" }
                ) { cycleStatus ->
                    SubscriptionCard(
                        cycleStatus = cycleStatus,
                        onToggleAutoRenew = { viewModel.toggleAutoRenew(cycleStatus.subscription.id) },
                        onEdit = {
                            subscriptionToEdit = cycleStatus.subscription
                            showAddDialog = true
                        },
                        onDelete = { viewModel.deleteSubscription(cycleStatus.subscription) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
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
                    viewModel.updateSubscription(sub)
                } else {
                    viewModel.addSubscription(sub)
                }
                showAddDialog = false
                subscriptionToEdit = null
            }
        )
    }
}
