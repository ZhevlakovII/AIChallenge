@file:OptIn(ExperimentalTime::class)

package ru.izhxx.aichallenge.features.productassistant.impl.domain.usecase

import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.SupportTicket
import ru.izhxx.aichallenge.features.productassistant.impl.domain.repository.ProductAssistantRepository
import kotlin.time.ExperimentalTime

/**
 * Implementation of ListTicketsUseCase
 */
class ListTicketsUseCaseImpl(
    private val repository: ProductAssistantRepository
) : ListTicketsUseCase {

    override suspend fun invoke(
        statusFilter: String?,
        tagFilter: String?
    ): Result<List<SupportTicket>> {
        return repository.getAllTickets(statusFilter, tagFilter)
    }
}
