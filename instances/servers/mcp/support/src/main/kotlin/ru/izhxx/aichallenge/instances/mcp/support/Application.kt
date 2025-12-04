package ru.izhxx.aichallenge.instances.mcp.support

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.websocket.DefaultWebSocketServerSession
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import ru.izhxx.aichallenge.common.Logger
import ru.izhxx.aichallenge.mcp.data.model.McpToolDTO
import ru.izhxx.aichallenge.mcp.data.model.ToolsListResult
import ru.izhxx.aichallenge.mcp.data.rpc.InitializeResult
import ru.izhxx.aichallenge.mcp.data.rpc.RpcError
import ru.izhxx.aichallenge.mcp.data.rpc.RpcRequest
import ru.izhxx.aichallenge.mcp.data.rpc.RpcResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Support Tickets MCP Server
 * WS: ws://127.0.0.1:9001/mcp
 *
 * Provides access to support tickets data:
 * - support.list_tickets - Get all support tickets
 * - support.get_ticket - Get a specific ticket by ID
 */

private const val SUPPORT_SERVER_PORT = 9001

@Serializable
data class SupportTicket(
    val id: String,
    val userId: String,
    val title: String,
    val description: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
    val assignedTo: String? = null,
    val comments: List<TicketComment> = emptyList(),
    val tags: List<String>
)

@Serializable
data class TicketComment(
    val id: String,
    val authorId: String,
    val authorName: String,
    val content: String,
    val createdAt: String,
    val isInternal: Boolean = false
)

@Serializable
data class TicketsData(
    val tickets: List<SupportTicket>
)

fun main() {
    embeddedServer(Netty, port = SUPPORT_SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val logger = Logger("MCP-Server-Support")

    install(WebSockets)

    routing {
        get("/") {
            call.respondText("AIChallenge Support MCP server is running")
        }

        webSocket("/mcp") {
            val json = Json {
                ignoreUnknownKeys = true
                prettyPrint = false
            }

            logger.i("Client connected to /mcp (support)")

            val tools = buildMcpTools(json)

            for (frame in incoming) {
                val text = (frame as? Frame.Text)?.readText() ?: continue
                logger.d("<= $text")

                val req = runCatching { json.decodeFromString(RpcRequest.serializer(), text) }.getOrNull()
                if (req == null) continue

                handleRpcRequest(
                    session = this,
                    req = req,
                    json = json,
                    tools = tools,
                    logger = logger
                )
            }

            logger.i("Client disconnected from /mcp (support)")
        }
    }
}

private suspend fun handleRpcRequest(
    session: DefaultWebSocketServerSession,
    req: RpcRequest,
    json: Json,
    tools: List<McpToolDTO>,
    logger: Logger
) {
    when (req.method) {
        "initialize" -> session.handleInitialize(req, json, logger)
        "notifications/initialized" -> logger.i("notifications/initialized received")
        "tools/list" -> session.handleToolsList(req, json, tools, logger)
        "tools/call" -> session.handleToolsCall(req, json, logger)
        else -> session.respondError(json, req.id, -32601, "Method not found: ${req.method}", logger)
    }
}

private fun buildMcpTools(json: Json): List<McpToolDTO> {
    return listOf(
        McpToolDTO(
            name = "support.list_tickets",
            description = "Получить список тикетов поддержки с фильтрацией. Использовать когда: - Нужно получить общую картину по обращениям - Поиск тикетов по статусу (open, in_progress, resolved) - Фильтрация по типам проблем через теги - Анализ загруженности поддержки Возвращает podstawную информацию о тикетах. Для детального просмотра использовать support.get_ticket.",
            inputSchema = json.parseToJsonElement(
                """
                {
                  "type": "object",
                  "title": "List all support tickets",
                  "description": "Returns all support tickets with their details.",
                  "properties": {
                    "status": {
                      "type": "string",
                      "description": "Filter tickets by status (open, in_progress, resolved)",
                      "enum": ["open", "in_progress", "resolved"]
                    },
                    "tag": {
                      "type": "string",
                      "description": "Filter tickets by tag (auth, network, settings, etc.)"
                    }
                  },
                  "additionalProperties": false
                }
                """.trimIndent()
            )
        ),
        McpToolDTO(
            name = "support.get_ticket",
            description = "Получить полную информацию по конкретному тикету. Использовать когда: - Нужно изучить детали проблемы - Анализ истории комментариев и действий - Проверка текущего статуса и ответственного - Подготовка к ответу пользователю Включает все комментарии, историю изменений и технические детали.",
            inputSchema = json.parseToJsonElement(
                """
                {
                  "type": "object",
                  "title": "Get support ticket by ID",
                  "description": "Returns detailed information about a specific support ticket.",
                  "properties": {
                    "ticket_id": {
                      "type": "string",
                      "description": "Unique ticket ID (UUID format)"
                    }
                  },
                  "required": ["ticket_id"],
                  "additionalProperties": false
                }
                """.trimIndent()
            )
        ),
        McpToolDTO(
            name = "support.create_ticket",
            description = "Создать новый тикет поддержки. Использовать когда: - Пользователь сообщает о новой проблеме - Сообщение не соответствует существующему тикету - Нужна отслеживаемая регистрация обращения Не использовать для простых вопросов или когда проблема уже решается в существующем тикете.",
            inputSchema = json.parseToJsonElement(
                """
                {
                  "type": "object",
                  "title": "Create support ticket",
                  "description": "Creates a new support ticket with the provided information.",
                  "properties": {
                    "title": {
                      "type": "string",
                      "description": "Brief summary of the issue"
                    },
                    "description": {
                      "type": "string",
                      "description": "Detailed description of the problem or question"
                    },
                    "user_id": {
                      "type": "string",
                      "description": "Unique identifier of the user reporting the issue"
                    },
                    "tags": {
                      "type": "array",
                      "items": {
                        "type": "string"
                      },
                      "description": "Array of tags to categorize the ticket (e.g., ['auth', 'api-key', 'openai'])"
                    },
                    "priority": {
                      "type": "string",
                      "enum": ["low", "medium", "high"],
                      "description": "Priority level of the ticket (default: medium)"
                    }
                  },
                  "required": ["title", "description", "user_id"],
                  "additionalProperties": false
                }
                """.trimIndent()
            )
        ),
        McpToolDTO(
            name = "support.update_ticket_status",
            description = "Обновить статус тикета поддержки. Использовать когда: - Статус тикета изменился в ходе решения проблемы - Тикет решен и нужно изменить статус на 'resolved' - Тикет закрыт и нужно установить 'closed' - Нужно вернуться к работе над тикетом ('in_progress') Всегда добавлять комментарий explaining причину изменения статуса.",
            inputSchema = json.parseToJsonElement(
                """
                {
                  "type": "object",
                  "title": "Update ticket status",
                  "description": "Updates the status of a support ticket with optional comment.",
                  "properties": {
                    "ticket_id": {
                      "type": "string",
                      "description": "Unique ticket ID (UUID format)"
                    },
                    "status": {
                      "type": "string",
                      "enum": ["open", "in_progress", "resolved", "closed"],
                      "description": "New status for the ticket"
                    },
                    "comment": {
                      "type": "string",
                      "description": "Optional comment explaining the status change"
                    }
                  },
                  "required": ["ticket_id", "status"],
                  "additionalProperties": false
                }
                """.trimIndent()
            )
        ),
        McpToolDTO(
            name = "support.add_comment",
            description = "Добавить комментарий к тикету поддержки. Использовать когда: - Есть дополнительная информация по проблеме - Нужен статус обновления работы - Ответственный хочет задать уточняющие вопросы - Есть результаты диагностики или решения Внутренние комментарии (is_internal: true) для коммуникации внутри команды поддержки.",
            inputSchema = json.parseToJsonElement(
                """
                {
                  "type": "object",
                  "title": "Add comment to ticket",
                  "description": "Adds a comment to an existing support ticket.",
                  "properties": {
                    "ticket_id": {
                      "type": "string",
                      "description": "Unique ticket ID (UUID format)"
                    },
                    "author_id": {
                      "type": "string",
                      "description": "Unique identifier of the comment author"
                    },
                    "author_name": {
                      "type": "string",
                      "description": "Display name of the comment author"
                    },
                    "content": {
                      "type": "string",
                      "description": "Content of the comment"
                    },
                    "is_internal": {
                      "type": "boolean",
                      "description": "Whether this is an internal comment (visible only to support team)",
                      "default": false
                    }
                  },
                  "required": ["ticket_id", "author_id", "author_name", "content"],
                  "additionalProperties": false
                }
                """.trimIndent()
            )
        )
    )
}

private fun loadTicketsData(): Result<TicketsData> {
    return runCatching {
        fun findRepoRoot(start: Path = Paths.get("").toAbsolutePath().normalize()): Path? {
            var cur = start
            repeat(20) {
                val dotGit = cur.resolve(".git")
                val gradlew = cur.resolve("gradlew")
                val settings = cur.resolve("settings.gradle.kts")
                if (Files.isDirectory(dotGit) || Files.exists(gradlew) || Files.exists(settings)) return cur
                cur = cur.parent ?: return null
            }
            return null
        }

        val root = findRepoRoot() ?: Paths.get("").toAbsolutePath().normalize()
        val ticketsFile = root.resolve("docs/support-tickets.json")

        if (!Files.exists(ticketsFile) || !Files.isRegularFile(ticketsFile)) {
            throw IllegalStateException("Support tickets file not found: ${ticketsFile.toAbsolutePath()}")
        }

        val content = Files.readAllBytes(ticketsFile)
        val jsonString = String(content, StandardCharsets.UTF_8)
        val json = Json { ignoreUnknownKeys = true }
        json.decodeFromString<TicketsData>(jsonString)
    }
}

private fun saveTicketsData(ticketsData: TicketsData): Result<Unit> {
    return runCatching {
        fun findRepoRoot(start: Path = Paths.get("").toAbsolutePath().normalize()): Path? {
            var cur = start
            repeat(20) {
                val dotGit = cur.resolve(".git")
                val gradlew = cur.resolve("gradlew")
                val settings = cur.resolve("settings.gradle.kts")
                if (Files.isDirectory(dotGit) || Files.exists(gradlew) || Files.exists(settings)) return cur
                cur = cur.parent ?: return null
            }
            return null
        }

        val root = findRepoRoot() ?: Paths.get("").toAbsolutePath().normalize()
        val ticketsFile = root.resolve("docs/support-tickets.json")

        val json = Json { 
            ignoreUnknownKeys = true
            prettyPrint = true
        }
        val jsonString = json.encodeToString(TicketsData.serializer(), ticketsData)
        
        Files.write(ticketsFile, jsonString.toByteArray(StandardCharsets.UTF_8))
    }
}

private suspend fun DefaultWebSocketServerSession.handleInitialize(
    req: RpcRequest,
    json: Json,
    logger: Logger
) {
    val resultEl: JsonElement = json.encodeToJsonElement(
        InitializeResult(
            serverInfo = buildJsonObject {
                put("name", "AIChallenge-MCP-Support")
                put("version", "1.0.0")
            },
            capabilities = buildJsonObject { }
        )
    )
    respondResult(json, req.id, resultEl, logger)
}

private suspend fun DefaultWebSocketServerSession.handleToolsList(
    req: RpcRequest,
    json: Json,
    tools: List<McpToolDTO>,
    logger: Logger
) {
    val resultEl: JsonElement = json.encodeToJsonElement(ToolsListResult(tools = tools))
    respondResult(json, req.id, resultEl, logger)
}

private suspend fun DefaultWebSocketServerSession.handleToolsCall(
    req: RpcRequest,
    json: Json,
    logger: Logger
) {
    val params = req.params?.jsonObject
    val toolName = params?.get("name")?.jsonPrimitive?.content
    val args = params?.get("arguments")?.jsonObject

    if (toolName == null) {
        respondError(json, req.id, -32602, "Invalid params: 'name' is required", logger)
        return
    }

    when (toolName) {
        "support.list_tickets" -> handleListTickets(req, json, args, logger)
        "support.get_ticket" -> handleGetTicket(req, json, args, logger)
        "support.create_ticket" -> handleCreateTicket(req, json, args, logger)
        "support.update_ticket_status" -> handleUpdateTicketStatus(req, json, args, logger)
        "support.add_comment" -> handleAddComment(req, json, args, logger)
        else -> respondError(json, req.id, -32601, "Unknown tool: $toolName", logger)
    }
}

private suspend fun DefaultWebSocketServerSession.handleListTickets(
    req: RpcRequest,
    json: Json,
    args: Map<String, JsonElement>?,
    logger: Logger
) {
    val statusFilter = args?.get("status")?.jsonPrimitive?.content
    val tagFilter = args?.get("tag")?.jsonPrimitive?.content

    val ticketsResult = loadTicketsData()
    if (ticketsResult.isFailure) {
        respondError(json, req.id, -32000, "Failed to load tickets data: ${ticketsResult.exceptionOrNull()?.message}", logger)
        return
    }

    var tickets = ticketsResult.getOrThrow().tickets

    // Apply filters
    if (!statusFilter.isNullOrBlank()) {
        tickets = tickets.filter { it.status.equals(statusFilter, ignoreCase = true) }
    }

    if (!tagFilter.isNullOrBlank()) {
        tickets = tickets.filter { ticket ->
            ticket.tags.any { it.equals(tagFilter, ignoreCase = true) }
        }
    }

    val ticketsArray = buildJsonArray {
        tickets.forEach { ticket ->
            add(buildJsonObject {
                put("id", JsonPrimitive(ticket.id))
                put("userId", JsonPrimitive(ticket.userId))
                put("title", JsonPrimitive(ticket.title))
                put("description", JsonPrimitive(ticket.description))
                put("status", JsonPrimitive(ticket.status))
                put("createdAt", JsonPrimitive(ticket.createdAt))
                put("updatedAt", JsonPrimitive(ticket.updatedAt))
                ticket.assignedTo?.let { put("assignedTo", JsonPrimitive(it)) }
                put("comments", json.encodeToJsonElement(ticket.comments))
                put("tags", json.encodeToJsonElement(ticket.tags))
            })
        }
    }

    val resultEl = buildJsonObject {
        put("tickets", ticketsArray)
        put("count", JsonPrimitive(tickets.size))
        if (statusFilter != null) put("filtered_by_status", JsonPrimitive(statusFilter))
        if (tagFilter != null) put("filtered_by_tag", JsonPrimitive(tagFilter))
    }

    respondResult(json, req.id, resultEl, logger)
}

private suspend fun DefaultWebSocketServerSession.handleGetTicket(
    req: RpcRequest,
    json: Json,
    args: Map<String, JsonElement>?,
    logger: Logger
) {
    val ticketId = args?.get("ticket_id")?.jsonPrimitive?.content

    if (ticketId.isNullOrBlank()) {
        respondError(json, req.id, -32602, "Invalid params: 'ticket_id' is required", logger)
        return
    }

    val ticketsResult = loadTicketsData()
    if (ticketsResult.isFailure) {
        respondError(json, req.id, -32000, "Failed to load tickets data: ${ticketsResult.exceptionOrNull()?.message}", logger)
        return
    }

    val ticket = ticketsResult.getOrThrow().tickets.find { it.id == ticketId }

    if (ticket == null) {
        respondError(json, req.id, -32001, "Ticket not found: $ticketId", logger)
        return
    }

    val resultEl = buildJsonObject {
        put("id", JsonPrimitive(ticket.id))
        put("userId", JsonPrimitive(ticket.userId))
        put("title", JsonPrimitive(ticket.title))
        put("description", JsonPrimitive(ticket.description))
        put("status", JsonPrimitive(ticket.status))
        put("createdAt", JsonPrimitive(ticket.createdAt))
        put("updatedAt", JsonPrimitive(ticket.updatedAt))
        ticket.assignedTo?.let { put("assignedTo", JsonPrimitive(it)) }
        put("comments", json.encodeToJsonElement(ticket.comments))
        put("tags", json.encodeToJsonElement(ticket.tags))
    }

    respondResult(json, req.id, resultEl, logger)
}

private suspend fun DefaultWebSocketServerSession.handleCreateTicket(
    req: RpcRequest,
    json: Json,
    args: Map<String, JsonElement>?,
    logger: Logger
) {
    val title = args?.get("title")?.jsonPrimitive?.content
    val description = args?.get("description")?.jsonPrimitive?.content
    val userId = args?.get("user_id")?.jsonPrimitive?.content
    val priority = args?.get("priority")?.jsonPrimitive?.content ?: "medium"
    
    // Parse tags array
    val tags = args?.get("tags")?.jsonArray?.map { 
        it.jsonPrimitive.content 
    } ?: emptyList()

    // Validate required fields
    if (title.isNullOrBlank()) {
        respondError(json, req.id, -32602, "Invalid params: 'title' is required", logger)
        return
    }
    if (description.isNullOrBlank()) {
        respondError(json, req.id, -32602, "Invalid params: 'description' is required", logger)
        return
    }
    if (userId.isNullOrBlank()) {
        respondError(json, req.id, -32602, "Invalid params: 'user_id' is required", logger)
        return
    }
    if (!listOf("low", "medium", "high").contains(priority)) {
        respondError(json, req.id, -32602, "Invalid params: 'priority' must be one of: low, medium, high", logger)
        return
    }

    // Load current tickets data
    val ticketsResult = loadTicketsData()
    if (ticketsResult.isFailure) {
        respondError(json, req.id, -32000, "Failed to load tickets data: ${ticketsResult.exceptionOrNull()?.message}", logger)
        return
    }

    val currentData = ticketsResult.getOrThrow()
    val timestamp = Instant.now().toString()
    
    // Create new ticket
    val newTicket = SupportTicket(
        id = UUID.randomUUID().toString(),
        userId = userId,
        title = title.trim(),
        description = description.trim(),
        status = "open",
        createdAt = timestamp,
        updatedAt = timestamp,
        assignedTo = null,
        comments = emptyList(),
        tags = tags
    )

    // Add to tickets list
    val updatedTickets = currentData.tickets + newTicket
    val updatedData = TicketsData(updatedTickets)

    // Save updated data
    val saveResult = saveTicketsData(updatedData)
    if (saveResult.isFailure) {
        respondError(json, req.id, -32000, "Failed to save ticket data: ${saveResult.exceptionOrNull()?.message}", logger)
        return
    }

    // Return created ticket
    val resultEl = buildJsonObject {
        put("id", JsonPrimitive(newTicket.id))
        put("userId", JsonPrimitive(newTicket.userId))
        put("title", JsonPrimitive(newTicket.title))
        put("description", JsonPrimitive(newTicket.description))
        put("status", JsonPrimitive(newTicket.status))
        put("priority", JsonPrimitive(priority))
        put("createdAt", JsonPrimitive(newTicket.createdAt))
        put("updatedAt", JsonPrimitive(newTicket.updatedAt))
        newTicket.assignedTo?.let { put("assignedTo", JsonPrimitive(it)) }
        put("comments", json.encodeToJsonElement(newTicket.comments))
        put("tags", json.encodeToJsonElement(newTicket.tags))
    }

    respondResult(json, req.id, resultEl, logger)
}

private suspend fun DefaultWebSocketServerSession.handleUpdateTicketStatus(
    req: RpcRequest,
    json: Json,
    args: Map<String, JsonElement>?,
    logger: Logger
) {
    val ticketId = args?.get("ticket_id")?.jsonPrimitive?.content
    val newStatus = args?.get("status")?.jsonPrimitive?.content
    val comment = args?.get("comment")?.jsonPrimitive?.content

    // Validate required fields
    if (ticketId.isNullOrBlank()) {
        respondError(json, req.id, -32602, "Invalid params: 'ticket_id' is required", logger)
        return
    }
    if (newStatus.isNullOrBlank()) {
        respondError(json, req.id, -32602, "Invalid params: 'status' is required", logger)
        return
    }
    if (!listOf("open", "in_progress", "resolved", "closed").contains(newStatus)) {
        respondError(json, req.id, -32602, "Invalid params: 'status' must be one of: open, in_progress, resolved, closed", logger)
        return
    }

    // Load current tickets data
    val ticketsResult = loadTicketsData()
    if (ticketsResult.isFailure) {
        respondError(json, req.id, -32000, "Failed to load tickets data: ${ticketsResult.exceptionOrNull()?.message}", logger)
        return
    }

    val currentData = ticketsResult.getOrThrow()
    
    // Find the ticket
    val ticketIndex = currentData.tickets.indexOfFirst { it.id == ticketId }
    if (ticketIndex == -1) {
        respondError(json, req.id, -32001, "Ticket not found: $ticketId", logger)
        return
    }

    val oldTicket = currentData.tickets[ticketIndex]
    val timestamp = Instant.now().toString()

    // Create system comment if status changed
    val updatedComments = if (oldTicket.status != newStatus && !comment.isNullOrBlank()) {
        val systemComment = TicketComment(
            id = UUID.randomUUID().toString(),
            authorId = "system",
            authorName = "System",
            content = "Статус изменен с '${oldTicket.status}' на '$newStatus': $comment",
            createdAt = timestamp,
            isInternal = false
        )
        oldTicket.comments + systemComment
    } else {
        oldTicket.comments
    }

    // Update ticket
    val updatedTicket = oldTicket.copy(
        status = newStatus,
        updatedAt = timestamp,
        comments = updatedComments
    )

    // Update tickets list
    val updatedTickets = currentData.tickets.toMutableList()
    updatedTickets[ticketIndex] = updatedTicket
    val updatedData = TicketsData(updatedTickets)

    // Save updated data
    val saveResult = saveTicketsData(updatedData)
    if (saveResult.isFailure) {
        respondError(json, req.id, -32000, "Failed to save ticket data: ${saveResult.exceptionOrNull()?.message}", logger)
        return
    }

    // Return updated ticket
    val resultEl = buildJsonObject {
        put("id", JsonPrimitive(updatedTicket.id))
        put("userId", JsonPrimitive(updatedTicket.userId))
        put("title", JsonPrimitive(updatedTicket.title))
        put("description", JsonPrimitive(updatedTicket.description))
        put("status", JsonPrimitive(updatedTicket.status))
        put("previous_status", JsonPrimitive(oldTicket.status))
        put("createdAt", JsonPrimitive(updatedTicket.createdAt))
        put("updatedAt", JsonPrimitive(updatedTicket.updatedAt))
        updatedTicket.assignedTo?.let { put("assignedTo", JsonPrimitive(it)) }
        put("comments", json.encodeToJsonElement(updatedTicket.comments))
        put("tags", json.encodeToJsonElement(updatedTicket.tags))
    }

    respondResult(json, req.id, resultEl, logger)
}

private suspend fun DefaultWebSocketServerSession.handleAddComment(
    req: RpcRequest,
    json: Json,
    args: Map<String, JsonElement>?,
    logger: Logger
) {
    val ticketId = args?.get("ticket_id")?.jsonPrimitive?.content
    val authorId = args?.get("author_id")?.jsonPrimitive?.content
    val authorName = args?.get("author_name")?.jsonPrimitive?.content
    val content = args?.get("content")?.jsonPrimitive?.content
    val isInternal = args?.get("is_internal")?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: false

    // Validate required fields
    if (ticketId.isNullOrBlank()) {
        respondError(json, req.id, -32602, "Invalid params: 'ticket_id' is required", logger)
        return
    }
    if (authorId.isNullOrBlank()) {
        respondError(json, req.id, -32602, "Invalid params: 'author_id' is required", logger)
        return
    }
    if (authorName.isNullOrBlank()) {
        respondError(json, req.id, -32602, "Invalid params: 'author_name' is required", logger)
        return
    }
    if (content.isNullOrBlank()) {
        respondError(json, req.id, -32602, "Invalid params: 'content' is required", logger)
        return
    }

    // Load current tickets data
    val ticketsResult = loadTicketsData()
    if (ticketsResult.isFailure) {
        respondError(json, req.id, -32000, "Failed to load tickets data: ${ticketsResult.exceptionOrNull()?.message}", logger)
        return
    }

    val currentData = ticketsResult.getOrThrow()
    
    // Find the ticket
    val ticketIndex = currentData.tickets.indexOfFirst { it.id == ticketId }
    if (ticketIndex == -1) {
        respondError(json, req.id, -32001, "Ticket not found: $ticketId", logger)
        return
    }

    val oldTicket = currentData.tickets[ticketIndex]
    val timestamp = Instant.now().toString()

    // Create new comment
    val newComment = TicketComment(
        id = UUID.randomUUID().toString(),
        authorId = authorId.trim(),
        authorName = authorName.trim(),
        content = content.trim(),
        createdAt = timestamp,
        isInternal = isInternal
    )

    // Update ticket with new comment
    val updatedTicket = oldTicket.copy(
        updatedAt = timestamp,
        comments = oldTicket.comments + newComment
    )

    // Update tickets list
    val updatedTickets = currentData.tickets.toMutableList()
    updatedTickets[ticketIndex] = updatedTicket
    val updatedData = TicketsData(updatedTickets)

    // Save updated data
    val saveResult = saveTicketsData(updatedData)
    if (saveResult.isFailure) {
        respondError(json, req.id, -32000, "Failed to save ticket data: ${saveResult.exceptionOrNull()?.message}", logger)
        return
    }

    // Return the new comment
    val resultEl = buildJsonObject {
        put("id", JsonPrimitive(newComment.id))
        put("ticket_id", JsonPrimitive(ticketId))
        put("author_id", JsonPrimitive(newComment.authorId))
        put("author_name", JsonPrimitive(newComment.authorName))
        put("content", JsonPrimitive(newComment.content))
        put("created_at", JsonPrimitive(newComment.createdAt))
        put("is_internal", JsonPrimitive(newComment.isInternal))
        put("ticket_updated_at", JsonPrimitive(updatedTicket.updatedAt))
    }

    respondResult(json, req.id, resultEl, logger)
}

private suspend fun DefaultWebSocketServerSession.respondResult(
    json: Json,
    id: Int?,
    result: JsonElement,
    logger: Logger
) {
    val resp = RpcResponse(id = id, result = result)
    val out = json.encodeToString(resp)
    logger.d("=> $out")
    send(Frame.Text(out))
}

private suspend fun DefaultWebSocketServerSession.respondError(
    json: Json,
    id: Int?,
    code: Int,
    message: String,
    logger: Logger
) {
    val err = RpcError(code = code, message = message)
    val resp = RpcResponse(id = id, error = err)
    val out = json.encodeToString(resp)
    logger.d("=> $out")
    send(Frame.Text(out))
}
