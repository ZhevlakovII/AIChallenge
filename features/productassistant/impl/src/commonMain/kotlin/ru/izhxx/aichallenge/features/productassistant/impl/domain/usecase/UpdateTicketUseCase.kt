package ru.izhxx.aichallenge.features.productassistant.impl.domain.usecase

import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.SupportTicket
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.TicketStatus
import kotlin.time.ExperimentalTime

/**
 * Use case for updating an existing support ticket
 */
@OptIn(ExperimentalTime::class)
interface UpdateTicketUseCase {
    suspend operator fun invoke(
        ticketId: String,
        newStatus: TicketStatus? = null,
        comment: String? = null
    ): Result<SupportTicket>
}
