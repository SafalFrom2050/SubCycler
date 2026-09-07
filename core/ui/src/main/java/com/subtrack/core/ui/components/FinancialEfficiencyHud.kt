package com.subtrack.core.ui.components

import com.subtrack.core.common.currency.MoneyFormatter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.subtrack.core.domain.model.PortfolioSummary
import com.subtrack.core.ui.theme.PrimarySky

@Composable
fun FinancialEfficiencyHud(
    summary: PortfolioSummary,
    currencyCode: String = "USD",
    modifier: Modifier = Modifier
) {
    val efficiencyFraction = if (summary.totalMonthlyStandardCost > 0) {
        (summary.totalMonthlyOptimizedCost / summary.totalMonthlyStandardCost).coerceIn(0.0, 1.0).toFloat()
    } else {
        1.0f
    }
    val monthlySaved = if (summary.totalAnnualSavings > 0) summary.totalAnnualSavings / 12.0 else 0.0

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        // Label + ROI Pill Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "PROJECTED ANNUAL SAVINGS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = Color(0xFF121212),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimarySky.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.TrendingDown,
                        contentDescription = null,
                        tint = PrimarySky,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "-${String.format("%.1f", summary.overallSavingsPercentage)}%",
                        color = PrimarySky,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Large Stark Annual Hero Number
        Text(
            text = MoneyFormatter.format(summary.totalAnnualSavings, currencyCode),
            fontSize = 34.sp,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = (-0.5).sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Financial Efficiency Dual-Segment Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF27272A)) // Muted zinc background (Savings segment)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction = efficiencyFraction)
                    .clip(RoundedCornerShape(2.dp))
                    .background(PrimarySky) // Sky Blue active spend
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Micro-metrics Underneath Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(PrimarySky)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "${MoneyFormatter.format(summary.totalMonthlyOptimizedCost, currencyCode)}/mo Active",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(PrimarySky.copy(alpha = 0.7f))
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "${MoneyFormatter.format(monthlySaved, currencyCode)}/mo Saved",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = PrimarySky
                )
            }

            Text(
                text = "${summary.activeCount} Active · ${summary.blackoutCount} Paused",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
