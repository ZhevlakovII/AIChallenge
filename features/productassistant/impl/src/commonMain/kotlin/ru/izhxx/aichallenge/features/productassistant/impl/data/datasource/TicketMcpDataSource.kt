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
}
