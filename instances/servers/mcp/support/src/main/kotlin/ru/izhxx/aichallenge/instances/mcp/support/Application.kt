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
    val tags: List<String>
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
            description = "Получить список всех тикетов поддержки",
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
            description = "Получить конкретный тикет поддержки по ID",
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
        put("tags", json.encodeToJsonElement(ticket.tags))
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
