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
        private const val TOOL_UPDATE_TICKET_STATUS = "support.update_ticket_status"
        private const val TOOL_ADD_COMMENT = "support.add_comment"
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
            // If only comment (without status change), use add_comment tool
            when {
                newStatus != null && comment.isNullOrEmpty() -> {
                    val args = buildJsonObject {
                        put("ticket_id", ticketId)
                        put("status", newStatus)
                        comment?.let { put("comment", it) }
                    }

                    mcpRepository.callTool(
                        wsUrl = SUPPORT_SERVER_URL,
                        name = TOOL_UPDATE_TICKET_STATUS,
                        arguments = args
                    ).getOrThrow().jsonObject
                }
                newStatus == null && !comment.isNullOrEmpty() -> {
                    val args = buildJsonObject {
                        put("ticket_id", ticketId)
                        put("author_id", "current_user") // TODO: Get actual user ID
                        put("author_name", "User") // TODO: Get actual user name
                        put("content", comment)
                        put("is_internal", false)
                    }

                    mcpRepository.callTool(
                        wsUrl = SUPPORT_SERVER_URL,
                        name = TOOL_ADD_COMMENT,
                        arguments = args
                    ).getOrThrow().jsonObject
                }
                else -> throw IllegalStateException("Invalid request")
            }
        }
    }
}
