package ru.izhxx.aichallenge.features.productassistant.impl.domain.repository

import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.AssistantQuery
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.AssistantResponse
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.DocumentationItem
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.SupportTicket
import kotlin.time.ExperimentalTime

/**
 * Repository for Product Assistant feature
 *
 * Provides access to:
 * - Support tickets (via MCP)
 * - Documentation/FAQ (via RAG/local files)
 * - LLM-based answer generation
 */
@OptIn(ExperimentalTime::class)
interface ProductAssistantRepository {

    /**
     * Search FAQ documentation for relevant information
     *
     * @param query Search query
     * @param maxResults Maximum number of results to return
     * @return Result containing list of relevant documentation items
     */
    suspend fun searchFaq(
        query: String,
        maxResults: Int = 5
    ): Result<List<DocumentationItem>>

    /**
     * Get all support tickets
     *
     * @param statusFilter Optional status filter
     * @param tagFilter Optional tag filter
     * @return Result containing list of support tickets
     */
    suspend fun getAllTickets(
        statusFilter: String? = null,
        tagFilter: String? = null
    ): Result<List<SupportTicket>>

    /**
     * Get a specific support ticket by ID
     *
     * @param ticketId Ticket ID
     * @return Result containing the support ticket or null if not found
     */
    suspend fun getTicketById(ticketId: String): Result<SupportTicket?>

    /**
     * Search for relevant tickets based on query
     *
     * @param query Search query
     * @param maxResults Maximum number of results
     * @return Result containing list of relevant tickets
     */
    suspend fun searchTickets(
        query: String,
        maxResults: Int = 3
    ): Result<List<SupportTicket>>

    /**
     * Generate answer using LLM with provided context
     *
     * @param query User query
     * @param faqContext FAQ documentation context
     * @param ticketContext Support ticket context
     * @return Result containing the assistant response
     */
    suspend fun generateAnswer(
        query: AssistantQuery,
        faqContext: List<DocumentationItem>,
        ticketContext: List<SupportTicket>
    ): Result<AssistantResponse>
}
