package ru.izhxx.aichallenge.instances.mcp.secondary

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.call
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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import ru.izhxx.aichallenge.common.Logger
import ru.izhxx.aichallenge.common.SERVER_PORT
import ru.izhxx.aichallenge.mcp.data.model.McpToolDTO
import ru.izhxx.aichallenge.mcp.data.model.ToolsListResult
import ru.izhxx.aichallenge.mcp.data.rpc.InitializeResult
import ru.izhxx.aichallenge.mcp.data.rpc.RpcRequest
import ru.izhxx.aichallenge.mcp.data.rpc.RpcResponse
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Secondary MCP-сервер (второй инстанс) с альтернативным набором инструментов.
 * WS: ws://127.0.0.1:(SERVER_PORT+1)/mcp
 *
 * Включает пересекающееся имя "echo" (для демонстрации политики оркестратора "первый победил"),
 * а также уникальные инструменты:
 * - upper (строка в верхний регистр)
 * - multiply (перемножение a*b)
 * - get_date (сегодняшняя дата, ISO_LOCAL_DATE)
 * - uuid (генерация UUID v4)
 */
fun main() {
    embeddedServer(Netty, port = SERVER_PORT + 1, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val logger = Logger("MCP-Server-Secondary")

    val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    prettyPrint = false
                }
            )
        }
        install(Logging)
    }

    install(WebSockets)

    routing {
        get("/") {
            call.respondText("AIChallenge MCP secondary server is running")
        }

        webSocket("/mcp") {
            val json = Json {
                ignoreUnknownKeys = true
                prettyPrint = false
            }

            logger.i("Client connected to /mcp (secondary)")

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

            logger.i("Client disconnected from /mcp (secondary)")
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
            name = "health_check",
            description = "Состояние MCP сервера (secondary)",
            inputSchema = null
        ),
        McpToolDTO(
            name = "echo",
            description = "Эхо (secondary)",
            inputSchema = json.parseToJsonElement(
                """
                {
                  "type": "object",
                  "title": "Echo input",
                  "description": "Echo back the provided text.",
                  "properties": {
                    "text": { "type": "string", "description": "Text to echo back." }
                  },
                  "required": ["text"],
                  "additionalProperties": false
                }
                """.trimIndent()
            )
        ),
        McpToolDTO(
            name = "upper",
            description = "Преобразует строку в верхний регистр",
            inputSchema = json.parseToJsonElement(
                """
                {
                  "type": "object",
                  "title": "Uppercase string",
                  "properties": {
                    "text": { "type": "string" }
                  },
                  "required": ["text"],
                  "additionalProperties": false
                }
                """.trimIndent()
            )
        ),
        McpToolDTO(
            name = "multiply",
            description = "Перемножение двух чисел (a*b)",
            inputSchema = json.parseToJsonElement(
                """
                {
                  "type": "object",
                  "title": "Multiply two numbers",
                  "properties": {
                    "a": { "type": "number" },
                    "b": { "type": "number" }
                  },
                  "required": ["a","b"],
                  "additionalProperties": false
                }
                """.trimIndent()
            )
        ),
        McpToolDTO(
            name = "get_date",
            description = "Текущая дата (ISO_LOCAL_DATE)",
            inputSchema = null
        ),
        McpToolDTO(
            name = "uuid",
            description = "Генерация случайного UUID v4",
            inputSchema = null
        ),
        McpToolDTO(
            name = "workspace.write_text",
            description = "Запись текста в файл с возможным созданием директорий",
            inputSchema = json.parseToJsonElement(
                """
                {
                  "type": "object",
                  "title": "Write text file",
                  "properties": {
                    "path": { "type": "string", "minLength": 1 },
                    "content": { "type": "string" },
                    "create_dirs": { "type": "boolean", "default": true },
                    "overwrite": { "type": "boolean", "default": true }
                  },
                  "required": ["path","content"],
                  "additionalProperties": false
                }
                """.trimIndent()
            )
        ),
        McpToolDTO(
            name = "chrono.date_range",
            description = "Вычисляет диапазон дат по пресету (today|yesterday|last_7_days)",
            inputSchema = json.parseToJsonElement(
                """
                {
                  "type": "object",
                  "title": "Date range (preset)",
                  "properties": {
                    "preset": { "type": "string", "enum": ["today","yesterday","last_7_days"] },
                    "tz": { "type": "string", "description": "IANA time zone, e.g. Europe/Moscow", "default": "system" }
                  },
                  "required": ["preset"],
                  "additionalProperties": false
                }
                """.trimIndent()
            )
        )
    )
}

private suspend fun DefaultWebSocketServerSession.handleInitialize(
    req: RpcRequest,
    json: Json,
    logger: Logger
) {
    val resultEl: JsonElement = json.encodeToJsonElement(
        InitializeResult(
            serverInfo = buildJsonObject {
                put("name", "AIChallenge-MCP-Secondary")
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
        "health_check" -> {
            val resultEl = buildJsonObject { put("status", kotlinx.serialization.json.JsonPrimitive("ok")) }
            respondResult(json, req.id, resultEl, logger)
        }

        "echo" -> {
            val text = args?.get("text")?.jsonPrimitive?.content
            if (text == null) {
                respondError(json, req.id, -32602, "Invalid params: 'text' is required", logger)
                return
            }
            val resultEl = buildJsonObject {
                put("text", kotlinx.serialization.json.JsonPrimitive(text))
                put("length", kotlinx.serialization.json.JsonPrimitive(text.length))
                put("server", kotlinx.serialization.json.JsonPrimitive("secondary"))
            }
            respondResult(json, req.id, resultEl, logger)
        }

        "upper" -> {
            val text = args?.get("text")?.jsonPrimitive?.content
            if (text == null) {
                respondError(json, req.id, -32602, "Invalid params: 'text' is required", logger)
                return
            }
            val resultEl = buildJsonObject {
                put("value", kotlinx.serialization.json.JsonPrimitive(text.uppercase()))
            }
            respondResult(json, req.id, resultEl, logger)
        }

        "multiply" -> {
            val a = args?.get("a")?.jsonPrimitive?.content?.toDoubleOrNull()
            val b = args?.get("b")?.jsonPrimitive?.content?.toDoubleOrNull()
            if (a == null || b == null) {
                respondError(json, req.id, -32602, "Invalid params: 'a' and 'b' numbers are required", logger)
                return
            }
            val resultEl = buildJsonObject { put("result", kotlinx.serialization.json.JsonPrimitive(a * b)) }
            respondResult(json, req.id, resultEl, logger)
        }

        "get_date" -> {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val resultEl = buildJsonObject { put("date", kotlinx.serialization.json.JsonPrimitive(today)) }
            respondResult(json, req.id, resultEl, logger)
        }

        "uuid" -> {
            val value = UUID.randomUUID().toString()
            val resultEl = buildJsonObject { put("uuid", kotlinx.serialization.json.JsonPrimitive(value)) }
            respondResult(json, req.id, resultEl, logger)
        }

        "workspace.write_text" -> handleWorkspaceWriteText(req, json, args, logger)

        "chrono.date_range" -> {
            val preset = args?.get("preset")?.jsonPrimitive?.content
            val tzStr = args?.get("tz")?.jsonPrimitive?.content
            val zone = runCatching { if (tzStr.isNullOrBlank() || tzStr == "system") java.time.ZoneId.systemDefault() else java.time.ZoneId.of(tzStr) }.getOrDefault(java.time.ZoneId.systemDefault())

            fun startOfDay(d: java.time.LocalDate) = d.atStartOfDay(zone).toInstant()
            val (start, end, label) = when (preset) {
                "today" -> {
                    val today = java.time.LocalDate.now(zone)
                    val s = startOfDay(today)
                    val e = startOfDay(today.plusDays(1)).minusMillis(1)
                    Triple(s, e, today.toString())
                }
                "yesterday" -> {
                    val y = java.time.LocalDate.now(zone).minusDays(1)
                    val s = startOfDay(y)
                    val e = startOfDay(y.plusDays(1)).minusMillis(1)
                    Triple(s, e, y.toString())
                }
                "last_7_days" -> {
                    val endDate = java.time.LocalDate.now(zone)
                    val startDate = endDate.minusDays(6)
                    val s = startOfDay(startDate)
                    val e = startOfDay(endDate.plusDays(1)).minusMillis(1)
                    Triple(s, e, "last_7_days")
                }
                else -> {
                    respondError(json, req.id, -32602, "Invalid params: 'preset' must be one of today|yesterday|last_7_days", logger)
                    return
                }
            }
            val resultEl = buildJsonObject {
                put("start_iso", kotlinx.serialization.json.JsonPrimitive(java.time.format.DateTimeFormatter.ISO_INSTANT.format(start)))
                put("end_iso", kotlinx.serialization.json.JsonPrimitive(java.time.format.DateTimeFormatter.ISO_INSTANT.format(end)))
                put("label", kotlinx.serialization.json.JsonPrimitive(label))
            }
            respondResult(json, req.id, resultEl, logger)
        }

        else -> respondError(json, req.id, -32601, "Unknown tool: $toolName", logger)
    }
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
    val err = ru.izhxx.aichallenge.mcp.data.rpc.RpcError(code = code, message = message)
    val resp = RpcResponse(id = id, error = err)
    val out = json.encodeToString(resp)
    logger.d("=> $out")
    send(Frame.Text(out))
}

private suspend fun DefaultWebSocketServerSession.handleWorkspaceWriteText(
    req: RpcRequest,
    json: Json,
    args: Map<String, JsonElement>?,
    logger: Logger
) {
    val pathStr = args?.get("path")?.jsonPrimitive?.content
    val contentStr = args?.get("content")?.jsonPrimitive?.content
    val createDirs = args?.get("create_dirs")?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: true
    val overwrite = args?.get("overwrite")?.jsonPrimitive?.content?.toBooleanStrictOrNull() ?: true

    if (pathStr.isNullOrBlank() || contentStr == null) {
        respondError(json, req.id, -32602, "Invalid params: 'path' and 'content' are required", logger)
        return
    }

    val p: Path = Paths.get(pathStr).normalize()
    if (createDirs) {
        val parent = p.parent
        if (parent != null) {
            runCatching { Files.createDirectories(parent) }.onFailure {
                respondError(json, req.id, -32001, "Failed to create directories: ${it.message}", logger)
                return
            }
        }
    }
    if (!overwrite && Files.exists(p)) {
        respondError(json, req.id, -32002, "File already exists and overwrite=false: $p", logger)
        return
    }

    val written = runCatching {
        val bytes = contentStr.toByteArray(StandardCharsets.UTF_8)
        Files.write(
            p,
            bytes,
            StandardOpenOption.CREATE,
            if (overwrite) StandardOpenOption.TRUNCATE_EXISTING else StandardOpenOption.CREATE_NEW,
            StandardOpenOption.WRITE
        )
        bytes.size
    }.getOrElse {
        respondError(json, req.id, -32003, "Write failed: ${it.message}", logger)
        return
    }

    val resultEl = buildJsonObject {
        put("path", JsonPrimitive(p.toString()))
        put("bytes_written", JsonPrimitive(written))
    }
    respondResult(json, req.id, resultEl, logger)
}
