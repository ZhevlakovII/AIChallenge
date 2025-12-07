package ru.izhxx.aichallenge.features.productassistant.impl.data.datasource

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.put
import ru.izhxx.aichallenge.mcp.domain.repository.McpRepository

/**
 * Implementation of TicketMcpDataSource using MCP client
 */
class TicketMcpDataSourceImpl(
    private val mcpRepository: McpRepository,
) : TicketMcpDataSource {

    companion object {
        private const val SUPPORT_SERVER_URL = "ws://127.0.0.1:9001/mcp"
        private const val TOOL_LIST_TICKETS = "support.list_tickets"
        private const val TOOL_GET_TICKET = "support.get_ticket"
        private const val TOOL_CREATE_TICKET = "support.create_ticket"
        private const val TOOL_UPDATE_TICKET = "support.update_ticket_status"
    }

    override suspend fun listTickets(
        statusFilter: String?,
        tagFilter: String?
    ): Result<JsonObject> {
        return runCatching {
            val args = buildJsonObject {
                if (statusFilter != null) {
                    put("status", statusFilter)
                }
                if (tagFilter != null) {
                    put("tag", tagFilter)
                }
            }

            mcpRepository.callTool(
                wsUrl = SUPPORT_SERVER_URL,
                name = TOOL_LIST_TICKETS,
                arguments = args
            ).getOrThrow().jsonObject
        }
    }

    override suspend fun getTicket(ticketId: String): Result<JsonObject> {
        return runCatching {
            val args = buildJsonObject {
                put("ticket_id", ticketId)
            }

            mcpRepository.callTool(
                wsUrl = SUPPORT_SERVER_URL,
                name = TOOL_GET_TICKET,
                arguments = args
            ).getOrThrow().jsonObject
        }
    }

    override suspend fun createTicket(
        title: String,
        description: String,
        tags: List<String>
    ): Result<JsonObject> {
        return runCatching {
            val args = buildJsonObject {
                put("title", title)
                put("description", description)
                put("user_id", "current_user") // TODO: Get actual user ID
                put("priority", "medium")
                put("tags", buildJsonArray {
                    tags.forEach { tag ->
                        add(tag)
                    }
                })
            }

            mcpRepository.callTool(
                wsUrl = SUPPORT_SERVER_URL,
                name = TOOL_CREATE_TICKET,
                arguments = args
            ).getOrThrow().jsonObject
        }
    }

    override suspend fun updateTicket(
        ticketId: String,
        newStatus: String?,
        comment: String?
    ): Result<JsonObject> {
        return runCatching {
            val args = buildJsonObject {
                put("ticket_id", ticketId)
                newStatus?.let { put("status", it) }
                comment?.let { put("comment", it) }
            }

            mcpRepository.callTool(
                wsUrl = SUPPORT_SERVER_URL,
                name = TOOL_UPDATE_TICKET,
                arguments = args
            ).getOrThrow().jsonObject
        }
    }
}
