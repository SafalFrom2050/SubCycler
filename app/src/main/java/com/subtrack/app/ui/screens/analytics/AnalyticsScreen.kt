package com.subtrack.app.ui.screens.analytics

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.subtrack.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val haptic = LocalHapticFeedback.current
    val isDark = MaterialTheme.colorScheme.background == DarkBg

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Savings & Financial ROI",
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
            // Multi-Year Compounding Savings Card
            item(key = "multi_year_card", contentType = "card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) DarkSurface else LightSurface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoGraph,
                                contentDescription = null,
                                tint = Emerald500,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Long-Term Compounding Savings",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            SavingsHorizonItem(
                                label = "1 Year",
                                amount = uiState.summary.totalAnnualSavings,
                                subtitle = "Direct savings"
                            )
                            SavingsHorizonItem(
                                label = "3 Years",
                                amount = uiState.threeYearSavings,
                                subtitle = "Accumulated"
                            )
                            SavingsHorizonItem(
                                label = "5 Years",
                                amount = uiState.fiveYearSavings,
                                subtitle = "Compounded"
                            )
                        }
                    }
                }
            }

            // "What-If" Interactive Simulation Slider
            item(key = "simulator_card", contentType = "card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) DarkSurface else LightSurface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "What-If Optimizer Simulator",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Simulate your savings if you apply the 45-day rotation to more of your subscriptions:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Rotation Adoption: ${(uiState.simulatedRotationPct * 100).toInt()}% of services",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Emerald500
                        )

                        Slider(
                            value = uiState.simulatedRotationPct,
                            onValueChange = {
                                if (it != uiState.simulatedRotationPct) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                viewModel.setSimulatedRotationPct(it)
                            },
                            valueRange = 0f..1f,
                            steps = 10,
                            colors = SliderDefaults.colors(
                                thumbColor = Emerald500,
                                activeTrackColor = Emerald500
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Emerald500.copy(alpha = 0.12f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Simulated Annual Savings:",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "$${String.format("%.2f", uiState.simulatedAnnualSavings)}/yr",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = Emerald500
                                )
                            }
                        }
                    }
                }
            }

            // Summary Fact Box
            item(key = "psychology_card", contentType = "card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) DarkSurfaceVariant else LightSurfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "🧠 The Psychology of the 15-Day Blackout",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Most people stay subscribed out of friction and inertia, not continuous daily usage. By setting intentional 15-day blackout breaks between 30-day active binging windows, you eliminate subscription fatigue, let full season drops accumulate, and automatically keep thousands in your pocket.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SavingsHorizonItem(
    label: String,
    amount: Double,
    subtitle: String
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "$${String.format("%.0f", amount)}",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = ActiveGreen
        )
        Text(
            text = subtitle,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
