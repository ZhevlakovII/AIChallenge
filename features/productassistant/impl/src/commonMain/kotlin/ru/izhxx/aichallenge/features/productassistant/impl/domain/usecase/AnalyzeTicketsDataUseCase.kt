package ru.izhxx.aichallenge.features.productassistant.impl.domain.usecase

import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.TicketAnalytics

/**
 * Use case for analyzing ticket data and generating statistics
 */
interface AnalyzeTicketsDataUseCase {
    /**
     * Analyze all tickets and generate aggregated statistics
     *
     * This use case:
     * - Aggregates ticket data by status, tags, time
     * - Calculates common issues and trends
     * - Provides insights for data-driven decisions
     *
     * @param statusFilter Optional filter by status
     * @param tagFilter Optional filter by tag
     * @return Result containing ticket analytics
     */
    suspend operator fun invoke(
        statusFilter: String? = null,
        tagFilter: String? = null
    ): Result<TicketAnalytics>
}
