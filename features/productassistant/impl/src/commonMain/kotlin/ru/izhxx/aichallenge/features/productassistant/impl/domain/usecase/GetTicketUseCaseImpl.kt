@file:OptIn(ExperimentalTime::class)

package ru.izhxx.aichallenge.features.productassistant.impl.domain.usecase

import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.SupportTicket
import ru.izhxx.aichallenge.features.productassistant.impl.domain.repository.ProductAssistantRepository
import kotlin.time.ExperimentalTime

/**
 * Implementation of GetTicketUseCase
 */
class GetTicketUseCaseImpl(
    private val repository: ProductAssistantRepository
) : GetTicketUseCase {

    override suspend fun invoke(ticketId: String): Result<SupportTicket?> {
        if (ticketId.isBlank()) {
            return Result.failure(IllegalArgumentException("Ticket ID cannot be empty"))
        }

        return repository.getTicketById(ticketId)
    }
}
