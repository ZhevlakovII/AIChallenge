@file:OptIn(ExperimentalTime::class)

package ru.izhxx.aichallenge.features.productassistant.impl.domain.usecase

import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.SupportTicket
import kotlin.time.ExperimentalTime

/**
 * Use case for getting a support ticket by ID
 */
interface GetTicketUseCase {
    /**
     * Get a specific support ticket by ID
     *
     * @param ticketId Ticket ID
     * @return Result containing the support ticket or null if not found
     */
    suspend operator fun invoke(ticketId: String): Result<SupportTicket?>
}
