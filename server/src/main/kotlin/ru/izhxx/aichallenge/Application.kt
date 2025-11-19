package ru.izhxx.aichallenge

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
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
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import ru.izhxx.aichallenge.common.Logger
import ru.izhxx.aichallenge.common.SERVER_PORT
import ru.izhxx.aichallenge.mcp.data.model.McpToolDTO
import ru.izhxx.aichallenge.mcp.data.model.ToolsListResult
import ru.izhxx.aichallenge.mcp.data.rpc.InitializeResult
import ru.izhxx.aichallenge.mcp.data.rpc.RpcError
import ru.izhxx.aichallenge.mcp.data.rpc.RpcRequest
import ru.izhxx.aichallenge.mcp.data.rpc.RpcResponse

/**
 * Точка входа Ktor-сервера и минимальная реализация MCP по WebSocket (JSON-RPC 2.0).
 *
 * Поддерживаемые RPC-методы:
 * - initialize
 * - notifications/initialized
 * - tools/list
 * - tools/call (health_check, get_time, echo, sum, github.list_user_repos, github.list_my_repos)
 */
fun main() {
    embeddedServer(Netty, port = SERVER_PORT, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

/**
 * Конфигурация приложения:
 * - Включение WebSockets
 * - Маршруты: GET / (ping), WS /mcp (MCP протокол)
 */
fun Application.module() {
    val logger = Logger("MCP-Server")

    // HTTP клиент для GitHub API
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
        // Простой ping для проверки живости HTTP-сервера
        get("/") {
            call.respondText("AIChallenge MCP server is running")
        }

        // MCP WebSocket endpoint
        webSocket("/mcp") {
            val json = Json {
                ignoreUnknownKeys = true
                prettyPrint = false
            }

            logger.i("Client connected to /mcp")

            // Сборка описаний инструментов вынесена в отдельную функцию
            val tools = buildMcpTools(json)

            // Основной цикл чтения JSON-RPC сообщений
            for (frame in incoming) {
                val text = (frame as? Frame.Text)?.readText() ?: continue
                logger.d("<= $text")

                val req = runCatching { json.decodeFromString(RpcRequest.serializer(), text) }.getOrNull()
                if (req == null) {
                    // Невалидный JSON — игнорируем
                    continue
                }

                // Делегируем обработку RPC-метода
                handleRpcRequest(
                    session = this,
                    req = req,
                    json = json,
                    tools = tools,
                    http = httpClient,
                    logger = logger
                )
            }

            logger.i("Client disconnected from /mcp")
        }
    }
}

/**
 * Обработчик входящих JSON-RPC запросов.
 */
private suspend fun handleRpcRequest(
    session: DefaultWebSocketServerSession,
    req: RpcRequest,
    json: Json,
    tools: List<McpToolDTO>,
    http: HttpClient,
    logger: Logger
) {
    when (req.method) {
        "initialize" -> session.handleInitialize(req, json, logger)
        "notifications/initialized" -> {
            // Уведомление — ответа не требуется
            logger.i("notifications/initialized received")
        }
        "tools/list" -> session.handleToolsList(req, json, tools, logger)
        "tools/call" -> session.handleToolsCall(req, json, http, logger)
        else -> {
            // Метод не поддерживается
            session.respondError(json, req.id, -32601, "Method not found: ${req.method}", logger)
        }
    }
}

/**
 * Возвращает список инструментов MCP.
 */
private fun buildMcpTools(json: Json): List<McpToolDTO> {
    return listOf(
        McpToolDTO(
            name = "health_check",
            description = "Состояние MCP сервера",
            inputSchema = null
        ),
        McpToolDTO(
            name = "get_time",
            description = "Текущее время",
            inputSchema = null
        ),
        McpToolDTO(
            name = "echo",
            description = "Эхо",
            inputSchema = json.parseToJsonElement(
                """
                {
                  "type": "object",
                  "title": "Echo input",
                  "description": "Echo back the provided text.",
                  "properties": {
                    "text": {
                      "type": "string",
                      "description": "Text to echo back."
                    }
                  },
                  "required": ["text"],
                  "additionalProperties": false
                }
                """.trimIndent()
            )
        ),
        McpToolDTO(
            name = "sum",
            description = "Сумма двух чисел",
            inputSchema = json.parseToJsonElement(
                """
                {
                  "type": "object",
                  "title": "Sum two numbers",
                  "description": "Calculate the sum of two numbers.",
                  "properties": {
                    "a": {
                      "type": "number",
                      "description": "First addend."
                    },
                    "b": {
                      "type": "number",
                      "description": "Second addend."
                    }
                  },
                  "required": ["a","b"],
                  "additionalProperties": false
                }
                """.trimIndent()
            )
        ),
        // GitHub: публичные репозитории пользователя
        McpToolDTO(
            name = "github.list_user_repos",
            description = "Список публичных репозиториев указанного пользователя GitHub",
            inputSchema = json.parseToJsonElement(
                """
                {
                  "type": "object",
                  "title": "List public repositories of a GitHub user",
                  "description": "Fetch a paginated list of public repositories for the given GitHub username.",
                  "properties": {
                    "username": {
                      "type": "string",
                      "minLength": 1,
                      "description": "GitHub username whose public repositories will be listed."
                    },
                    "per_page": {
                      "type": "integer",
                      "minimum": 1,
                      "maximum": 100,
                      "default": 20,
                      "description": "Number of repositories to return per page (1-100)."
                    },
                    "sort": {
                      "type": "string",
                      "enum": ["created","updated","pushed","full_name"],
                      "default": "updated",
                      "description": "Sorting criteria for repositories."
                    }
                  },
                  "required": ["username"],
                  "additionalProperties": false
                }
                """.trimIndent()
            )
        ),
        // GitHub: репозитории аутентифицированного пользователя (требуется GITHUB_TOKEN)
        McpToolDTO(
            name = "github.list_my_repos",
            description = "Список репозиториев аутентифицированного пользователя GitHub (при наличии GITHUB_TOKEN на сервере)",
            inputSchema = json.parseToJsonElement(
                """
                {
                  "type": "object",
                  "title": "List repositories of the authenticated user",
                  "description": "Fetch a paginated list of repositories for the authenticated user using the configured token.",
                  "properties": {
                    "per_page": {
                      "type": "integer",
                      "minimum": 1,
                      "maximum": 100,
                      "default": 20,
                      "description": "Number of repositories to return per page (1-100)."
                    },
                    "sort": {
                      "type": "string",
                      "enum": ["created","updated","pushed","full_name"],
                      "default": "updated",
                      "description": "Sorting criteria for repositories."
                    },
                    "visibility": {
                      "type": "string",
                      "enum": ["all","public","private"],
                      "default": "all",
                      "description": "Visibility filter for repositories."
                    }
                  },
                  "additionalProperties": false
                }
                """.trimIndent()
            )
        )
    )
}

/**
 * Обработчик initialize.
 */
private suspend fun DefaultWebSocketServerSession.handleInitialize(
    req: RpcRequest,
    json: Json,
    logger: Logger
) {
    val resultEl: JsonElement = json.encodeToJsonElement(
        InitializeResult(
            serverInfo = buildJsonObject {
                put("name", "AIChallenge-MCP")
                put("version", "1.0.0")
            },
            capabilities = buildJsonObject { }
        )
    )
    respondResult(json, req.id, resultEl, logger)
}

/**
 * Обработчик tools/list.
 */
private suspend fun DefaultWebSocketServerSession.handleToolsList(
    req: RpcRequest,
    json: Json,
    tools: List<McpToolDTO>,
    logger: Logger
) {
    val resultEl: JsonElement = json.encodeToJsonElement(
        ToolsListResult(tools = tools)
    )
    respondResult(json, req.id, resultEl, logger)
}

/**
 * Обработчик tools/call.
 */
private suspend fun DefaultWebSocketServerSession.handleToolsCall(
    req: RpcRequest,
    json: Json,
    http: HttpClient,
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
        "health_check" -> handleHealthCheck(req, json, logger)
        "get_time" -> handleGetTime(req, json, logger)
        "echo" -> handleEcho(req, json, args, logger)
        "sum" -> handleSum(req, json, args, logger)
        "github.list_user_repos" -> handleGithubListUserRepos(req, json, args, http, logger)
        "github.list_my_repos" -> handleGithubListMyRepos(req, json, args, http, logger)
        else -> respondError(json, req.id, -32601, "Unknown tool: $toolName", logger)
    }
}

/**
 * Инструмент: health_check — возвращает статус сервера.
 */
private suspend fun DefaultWebSocketServerSession.handleHealthCheck(
    req: RpcRequest,
    json: Json,
    logger: Logger
) {
    val resultEl = buildJsonObject {
        put("status", kotlinx.serialization.json.JsonPrimitive("ok"))
    }
    respondResult(json, req.id, resultEl, logger)
}

/**
 * Инструмент: get_time — возвращает текущее время в ISO-8601 (UTC).
 */
private suspend fun DefaultWebSocketServerSession.handleGetTime(
    req: RpcRequest,
    json: Json,
    logger: Logger
) {
    val now = Instant.now()
    val iso = DateTimeFormatter.ISO_INSTANT.format(now)
    val resultEl = buildJsonObject {
        put("iso", kotlinx.serialization.json.JsonPrimitive(iso))
    }
    respondResult(json, req.id, resultEl, logger)
}

/**
 * Инструмент: echo — эхо-ответ с длиной текста.
 */
private suspend fun DefaultWebSocketServerSession.handleEcho(
    req: RpcRequest,
    json: Json,
    args: Map<String, JsonElement>?,
    logger: Logger
) {
    val text = args?.get("text")?.jsonPrimitive?.content
    if (text == null) {
        respondError(json, req.id, -32602, "Invalid params: 'text' is required", logger)
        return
    }
    val resultEl = buildJsonObject {
        put("text", kotlinx.serialization.json.JsonPrimitive(text))
        put("length", kotlinx.serialization.json.JsonPrimitive(text.length))
    }
    respondResult(json, req.id, resultEl, logger)
}

/**
 * Инструмент: sum — сумма двух чисел.
 */
private suspend fun DefaultWebSocketServerSession.handleSum(
    req: RpcRequest,
    json: Json,
    args: Map<String, JsonElement>?,
    logger: Logger
) {
    val aStr = args?.get("a")?.jsonPrimitive?.content
    val bStr = args?.get("b")?.jsonPrimitive?.content
    val a = aStr?.toDoubleOrNull()
    val b = bStr?.toDoubleOrNull()
    if (a == null || b == null) {
        respondError(json, req.id, -32602, "Invalid params: 'a' and 'b' numbers are required", logger)
        return
    }
    val resultEl = buildJsonObject {
        put("result", kotlinx.serialization.json.JsonPrimitive(a + b))
    }
    respondResult(json, req.id, resultEl, logger)
}

/**
 * Инструмент: github.list_user_repos — список публичных репозиториев пользователя.
 */
private suspend fun DefaultWebSocketServerSession.handleGithubListUserRepos(
    req: RpcRequest,
    json: Json,
    args: Map<String, JsonElement>?,
    http: HttpClient,
    logger: Logger
) {
    val username = args?.get("username")?.jsonPrimitive?.content
    val perPage = args?.get("per_page")?.jsonPrimitive?.intOrNull ?: 20
    val sort = args?.get("sort")?.jsonPrimitive?.content ?: "updated"

    if (username.isNullOrBlank()) {
        respondError(json, req.id, -32602, "Invalid params: 'username' is required", logger)
        return
    }

    val url = "https://api.github.com/users/${username}/repos"
    val response: HttpResponse = http.get(url) {
        parameter("per_page", perPage)
        parameter("sort", sort)
        header("Accept", "application/vnd.github+json")
        header("X-GitHub-Api-Version", "2022-11-28")
    }

    if (response.status.isSuccess()) {
        val body = response.bodyAsText()
        val arrayEl = runCatching { json.parseToJsonElement(body) }.getOrNull()
        val resultEl = buildJsonObject {
            put("items", arrayEl ?: json.parseToJsonElement("[]"))
        }
        respondResult(json, req.id, resultEl, logger)
    } else {
        val body = runCatching { response.bodyAsText() }.getOrDefault("")
        respondError(json, req.id, response.status.value, "GitHub API error (${response.status.value}): $body", logger)
    }
}

/**
 * Инструмент: github.list_my_repos — список репозиториев аутентифицированного пользователя.
 * Требует настройки переменной окружения GITHUB_TOKEN.
 */
private suspend fun DefaultWebSocketServerSession.handleGithubListMyRepos(
    req: RpcRequest,
    json: Json,
    args: Map<String, JsonElement>?,
    http: HttpClient,
    logger: Logger
) {
    val perPage = args?.get("per_page")?.jsonPrimitive?.intOrNull ?: 20
    val sort = args?.get("sort")?.jsonPrimitive?.content ?: "updated"
    val visibility = args?.get("visibility")?.jsonPrimitive?.content ?: "all"

    val token = System.getenv("GITHUB_TOKEN")?.trim().orEmpty()
    if (token.isEmpty()) {
        respondError(json, req.id, -32000, "GitHub token not configured on server (GITHUB_TOKEN)", logger)
        return
    }

    val url = "https://api.github.com/user/repos"
    val response: HttpResponse = http.get(url) {
        parameter("per_page", perPage)
        parameter("sort", sort)
        parameter("visibility", visibility)
        header("Accept", "application/vnd.github+json")
        header("X-GitHub-Api-Version", "2022-11-28")
        header("Authorization", "Bearer $token")
    }

    if (response.status.isSuccess()) {
        val body = response.bodyAsText()
        val arrayEl = runCatching { json.parseToJsonElement(body) }.getOrNull()
        val resultEl = buildJsonObject {
            put("items", arrayEl ?: json.parseToJsonElement("[]"))
        }
        respondResult(json, req.id, resultEl, logger)
    } else {
        val body = runCatching { response.bodyAsText() }.getOrDefault("")
        respondError(json, req.id, response.status.value, "GitHub API error (${response.status.value}): $body", logger)
    }
}

/**
 * Хелпер для отправки успешного ответа JSON-RPC.
 */
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

/**
 * Хелпер для отправки ошибки JSON-RPC.
 */
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
