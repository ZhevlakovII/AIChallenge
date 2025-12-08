package ru.izhxx.aichallenge.features.productassistant.impl.domain.usecase

import kotlinx.datetime.Instant
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.SupportTicket
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.TicketStatus
import ru.izhxx.aichallenge.features.productassistant.impl.domain.repository.ProductAssistantRepository
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Implementation of UpdateTicketUseCase
 */
@OptIn(ExperimentalTime::class)
internal class UpdateTicketUseCaseImpl(
    private val repository: ProductAssistantRepository
) : UpdateTicketUseCase {
    
    override suspend fun invoke(
        ticketId: String,
        newStatus: TicketStatus?,
        comment: String?
    ): Result<SupportTicket> {
        return try {
            // First get the current ticket
            val currentTicket = repository.getTicket(ticketId)
                .getOrElse { return Result.failure(Exception("Ticket not found: $ticketId")) }
            
            // Create updated ticket
            val updatedTicket = currentTicket.copy(
                status = newStatus ?: currentTicket.status,
                updatedAt = getCurrentTimestamp()
            )
            
            // Add comment if provided
            comment?.let { com ->
                repository.updateTicketComment(ticketId, com)
                    .getOrElse { return Result.failure(it) }
            }
            
            // Update ticket status
            repository.updateTicketStatus(ticketId, updatedTicket.status)
                .map { updatedTicket }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun getCurrentTimestamp(): Instant = Clock.System.now()
}
