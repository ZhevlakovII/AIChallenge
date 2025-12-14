@file:OptIn(ExperimentalTime::class)

package ru.izhxx.aichallenge.features.productassistant.impl.domain.usecase

import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.AssistantMode
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.AssistantQuery
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.AssistantResponse
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.DocumentationItem
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.SupportTicket
import ru.izhxx.aichallenge.features.productassistant.impl.domain.repository.ProductAssistantRepository
import kotlin.time.ExperimentalTime

/**
 * Implementation of GenerateAnswerUseCase
 */
class GenerateAnswerUseCaseImpl(
    private val repository: ProductAssistantRepository
) : GenerateAnswerUseCase {

    override suspend fun invoke(
        query: String,
        mode: AssistantMode,
        ticketId: String?
    ): Result<AssistantResponse> {
        if (query.isBlank()) {
            return Result.failure(IllegalArgumentException("Query cannot be empty"))
        }

        val assistantQuery = AssistantQuery(
            text = query,
            mode = mode,
            ticketId = ticketId
        )

        // Gather context based on mode
        val faqContext = mutableListOf<DocumentationItem>()
        val ticketContext = mutableListOf<SupportTicket>()

        when (mode) {
            AssistantMode.FAQ_ONLY -> {
                // Mode A: Only search FAQ
                val faqResult = repository.searchFaq(query, maxResults = 5)
                if (faqResult.isSuccess) {
                    faqContext.addAll(faqResult.getOrThrow())
                }
            }

            AssistantMode.TICKET_ANALYSIS -> {
                // Mode B: Analyze ticket(s)
                if (!ticketId.isNullOrBlank()) {
                    val ticketResult = repository.getTicketById(ticketId)
                    if (ticketResult.isSuccess) {
                        ticketResult.getOrNull()?.let { ticketContext.add(it) }
                    }
                } else {
                    // Search for relevant tickets
                    val ticketsResult = repository.searchTickets(query, maxResults = 3)
                    if (ticketsResult.isSuccess) {
                        ticketContext.addAll(ticketsResult.getOrThrow())
                    }
                }
            }

            AssistantMode.FULL -> {
                // Mode C: Full mode - both FAQ and tickets
                val faqResult = repository.searchFaq(query, maxResults = 5)
                if (faqResult.isSuccess) {
                    faqContext.addAll(faqResult.getOrThrow())
                }

                val ticketsResult = repository.searchTickets(query, maxResults = 3)
                if (ticketsResult.isSuccess) {
                    ticketContext.addAll(ticketsResult.getOrThrow())
                }

                // If specific ticket ID provided, also add it
                if (!ticketId.isNullOrBlank()) {
                    val ticketResult = repository.getTicketById(ticketId)
                    if (ticketResult.isSuccess) {
                        ticketResult.getOrNull()?.let { ticket ->
                            if (!ticketContext.any { it.id == ticket.id }) {
                                ticketContext.add(ticket)
                            }
                        }
                    }
                }
            }

            AssistantMode.ANALYTICS -> {
                // Mode D: Data Analytics - get all tickets for statistical analysis
                // LLM will analyze aggregated data instead of individual tickets
                val allTicketsResult = repository.getAllTickets()
                if (allTicketsResult.isSuccess) {
                    ticketContext.addAll(allTicketsResult.getOrThrow())
                }
            }
        }

        // Generate answer with collected context
        return repository.generateAnswer(assistantQuery, faqContext, ticketContext)
    }
}
