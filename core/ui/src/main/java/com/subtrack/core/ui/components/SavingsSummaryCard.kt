package com.subtrack.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subtrack.core.domain.model.PortfolioSummary
import com.subtrack.core.ui.theme.*

@Composable
fun SavingsSummaryCard(
    summary: PortfolioSummary,
    modifier: Modifier = Modifier
) {
    val gradient = Brush.linearGradient(
        colors = listOf(
            Emerald700,
            Emerald500
        )
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(20.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "PROJECTED ANNUAL SAVINGS",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Emerald100,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$${String.format("%.2f", summary.totalAnnualSavings)}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = LightSurface
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = LightSurface.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                                contentDescription = null,
                                tint = LightSurface,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "-${String.format("%.1f", summary.overallSavingsPercentage)}%",
                                color = LightSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = LightSurface.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(14.dp))

                // 3 Column breakdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Optimized Spend",
                            fontSize = 11.sp,
                            color = Emerald100
                        )
                        Text(
                            text = "$${String.format("%.2f", summary.totalMonthlyOptimizedCost)}/mo",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightSurface
                        )
                    }

                    Column {
                        Text(
                            text = "Standard Spend",
                            fontSize = 11.sp,
                            color = Emerald100
                        )
                        Text(
                            text = "$${String.format("%.2f", summary.totalMonthlyStandardCost)}/mo",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = LightSurface.copy(alpha = 0.8f)
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Active / Blackout",
                            fontSize = 11.sp,
                            color = Emerald100
                        )
                        Text(
                            text = "${summary.activeCount} active · ${summary.blackoutCount} paused",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightSurface
                        )
                    }
                }
            }
        }
    }
}
