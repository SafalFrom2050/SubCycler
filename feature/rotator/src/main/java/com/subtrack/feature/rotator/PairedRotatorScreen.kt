package com.subtrack.feature.rotator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subtrack.core.domain.model.Subscription
import com.subtrack.core.ui.theme.*

@Composable
fun PairedRotatorRoute(
    viewModel: PairedRotatorViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PairedRotatorScreen(
        uiState = uiState,
        onAction = viewModel::onAction,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairedRotatorScreen(
    uiState: PairedRotatorUiState,
    onAction: (PairedRotatorAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val isDark = MaterialTheme.colorScheme.background == DarkBg

    var showSuccessSnackbar by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Paired Service Rotator",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 90.dp)
        ) {
            // Explainer Banner
            item(key = "explainer_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SwapHoriz,
                                contentDescription = null,
                                tint = PrimarySky,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Zero-Downtime Alternating Binge",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Pair 2 streaming services to swap every 30 days. When Netflix is active, Disney+ is paused. When Netflix ends, Disney+ activates! You always have fresh content, paying 50% less.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Service Selector Card
            item(key = "service_selector_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Select Two Services to Pair",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Service 1 Dropdown / Picker
                        Text("Service 1 (Active First):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        ServiceDropdown(
                            subscriptions = uiState.subscriptions,
                            selected = uiState.service1,
                            onSelect = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onAction(PairedRotatorAction.SelectService1(it.id))
                            }
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Service 2 Dropdown / Picker
                        Text("Service 2 (Active Second):", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        ServiceDropdown(
                            subscriptions = uiState.subscriptions.filter { it.id != uiState.service1?.id },
                            selected = uiState.service2,
                            onSelect = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onAction(PairedRotatorAction.SelectService2(it.id))
                            }
                        )

                        Spacer(modifier = Modifier.height(20.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        Spacer(modifier = Modifier.height(16.dp))

                        // Financial Impact
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Cost (Simultaneous)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("$${String.format("%.2f", uiState.combinedMonthlyCostBefore)}/mo", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Column {
                                Text("Cost (Alternating)", fontSize = 11.sp, color = PrimarySky)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("$${String.format("%.2f", uiState.combinedMonthlyCostAfter)}/mo", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PrimarySky)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Annual Savings", fontSize = 11.sp, color = PrimarySky)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text("$${String.format("%.2f", uiState.combinedAnnualSavings)}/yr", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = PrimarySky)
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Align Button
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onAction(PairedRotatorAction.AlignAndActivate)
                                showSuccessSnackbar = true
                            },
                            enabled = uiState.service1 != null && uiState.service2 != null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimarySky,
                                contentColor = OledBlack
                            )
                        ) {
                            Icon(Icons.Default.SyncAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Align & Activate 50% Alternating Schedule", fontWeight = FontWeight.Bold)
                        }

                        AnimatedVisibility(
                            visible = showSuccessSnackbar,
                            enter = expandVertically(
                                animationSpec = spring(
                                    stiffness = Spring.StiffnessMediumLow,
                                    dampingRatio = Spring.DampingRatioNoBouncy
                                )
                            ) + fadeIn(animationSpec = tween(120)),
                            exit = shrinkVertically() + fadeOut(animationSpec = tween(90))
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = PrimarySky.copy(alpha = 0.12f),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        PrimarySky.copy(alpha = 0.3f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = PrimarySky, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Schedules synchronized! Swapping occurs every 30 days.",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = PrimarySky
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ServiceDropdown(
    subscriptions: List<Subscription>,
    selected: Subscription?,
    onSelect: (Subscription) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selected?.let { "${it.name} ($${String.format("%.2f", it.cost)}/mo)" } ?: "Select a service",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            subscriptions.forEach { sub ->
                DropdownMenuItem(
                    text = { Text("${sub.name} ($${String.format("%.2f", sub.cost)}/mo)") },
                    onClick = {
                        onSelect(sub)
                        expanded = false
                    }
                )
            }
        }
    }
}
