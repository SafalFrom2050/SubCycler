package com.subtrack.core.domain.usecase

import com.subtrack.core.domain.engine.RotationEngine
import com.subtrack.core.domain.model.CycleStatus
import com.subtrack.core.domain.model.PortfolioSummary

/**
 * Use case to compute high-level portfolio metrics and financial savings summary.
 */
class GetPortfolioSummaryUseCase {
    operator fun invoke(statuses: List<CycleStatus>): PortfolioSummary {
        return RotationEngine.calculatePortfolioSavings(statuses)
    }
}
