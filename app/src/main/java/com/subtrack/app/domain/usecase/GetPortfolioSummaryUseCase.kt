package com.subtrack.app.domain.usecase

import com.subtrack.app.data.model.CycleStatus
import com.subtrack.app.domain.PortfolioSummary
import com.subtrack.app.domain.RotationEngine

/**
 * Use case to compute high-level portfolio metrics and financial savings summary.
 */
class GetPortfolioSummaryUseCase {
    operator fun invoke(statuses: List<CycleStatus>): PortfolioSummary {
        return RotationEngine.calculatePortfolioSavings(statuses)
    }
}
