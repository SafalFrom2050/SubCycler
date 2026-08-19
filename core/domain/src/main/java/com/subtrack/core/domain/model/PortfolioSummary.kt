package com.subtrack.core.domain.model

data class PortfolioSummary(
    val totalSubscriptions: Int,
    val activeCount: Int,
    val blackoutCount: Int,
    val totalStandardAnnualCost: Double,
    val totalOptimizedAnnualCost: Double,
    val totalAnnualSavings: Double,
    val totalMonthlyStandardCost: Double,
    val totalMonthlyOptimizedCost: Double,
    val totalMonthlySavings: Double,
    val overallSavingsPercentage: Double,
    val urgentActionsCount: Int
)
