package ru.izhxx.aichallenge.features.productassistant.impl.data.datasource

import kotlinx.serialization.json.JsonObject

/**
 * Data source for accessing support tickets via MCP
 */
interface TicketMcpDataSource {
    /**
     * Get all support tickets via MCP
     *
     * @param statusFilter Optional status filter
     * @param tagFilter Optional tag filter
     * @return Result containing JsonObject with tickets data
     */
    suspend fun listTickets(
        statusFilter: String? = null,
        tagFilter: String? = null
    ): Result<JsonObject>

    /**
     * Get a specific support ticket by ID via MCP
     *
     * @param ticketId Ticket ID
     * @return Result containing JsonObject with ticket data
     */
    suspend fun getTicket(ticketId: String): Result<JsonObject>

    /**
     * Create a new support ticket via MCP
     *
     * @param title Ticket title
     * @param description Ticket description
     * @param tags Optional list of tags
     * @return Result containing JsonObject with created ticket data
     */
    suspend fun createTicket(
        title: String,
        description: String,
        tags: List<String> = emptyList()
    ): Result<JsonObject>

    /**
     * Update an existing support ticket via MCP
     *
     * @param ticketId Ticket ID
     * @param newStatus Optional new status
     * @param comment Optional comment to add
     * @return Result containing JsonObject with updated ticket data
     */
    suspend fun updateTicket(
        ticketId: String,
        newStatus: String? = null,
        comment: String? = null
    ): Result<JsonObject>
}
