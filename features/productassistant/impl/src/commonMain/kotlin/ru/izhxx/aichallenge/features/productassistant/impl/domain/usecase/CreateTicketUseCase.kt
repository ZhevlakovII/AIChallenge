package ru.izhxx.aichallenge.features.productassistant.impl.domain.usecase

import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.SupportTicket
import kotlin.time.ExperimentalTime

/**
 * Use case for creating a new support ticket
 */
interface CreateTicketUseCase {
    @OptIn(ExperimentalTime::class)
    suspend operator fun invoke(
        title: String,
        description: String,
        tags: List<String> = emptyList()
    ): Result<SupportTicket>
}
