@file:OptIn(ExperimentalTime::class)

package ru.izhxx.aichallenge.features.productassistant.impl.domain.usecase

import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.SupportTicket
import kotlin.time.ExperimentalTime

/**
 * Use case for listing all support tickets
 */
interface ListTicketsUseCase {
    /**
     * Get all support tickets with optional filters
     *
     * @param statusFilter Optional status filter
     * @param tagFilter Optional tag filter
     * @return Result containing list of support tickets
     */
    suspend operator fun invoke(
        statusFilter: String? = null,
        tagFilter: String? = null
    ): Result<List<SupportTicket>>
}
