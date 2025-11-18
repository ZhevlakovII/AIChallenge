package ru.izhxx.aichallenge

// Ktor HTTP client for GitHub API calls
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
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
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
 * Точка входа Ktor-сервера и минимальная реализация MCP по WebSocket.
 *
 * Поддерживаемые методы JSON-RPC:
 * - "initialize" (id != null)          -> RpcResponse с InitializeResult
 * - "notifications/initialized" (notif) -> без ответа
 * - "tools/list" (id != null)           -> RpcResponse с ToolsListResult
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

    // HTTP клиент для вызовов GitHub API
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

            // Предзаготовленный список инструментов
            val tools = listOf(
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
                          "properties": {
                            "text": { "type": "string" }
                          },
                          "required": ["text"]
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
                          "properties": {
                            "a": { "type": "number" },
                            "b": { "type": "number" }
                          },
                          "required": ["a","b"]
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
                          "properties": {
                            "username": { "type": "string" },
                            "per_page": { "type": "integer", "minimum": 1, "maximum": 100, "default": 20 },
                            "sort": { "type": "string", "enum": ["created","updated","pushed","full_name"], "default": "updated" }
                          },
                          "required": ["username"]
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
                          "properties": {
                            "per_page": { "type": "integer", "minimum": 1, "maximum": 100, "default": 20 },
                            "sort": { "type": "string", "enum": ["created","updated","pushed","full_name"], "default": "updated" },
                            "visibility": { "type": "string", "enum": ["all","public","private"], "default": "all" }
                          }
                        }
                        """.trimIndent()
                    )
                )
            )

            // Основной цикл чтения JSON-RPC сообщений
            for (frame in incoming) {
                val text = (frame as? Frame.Text)?.readText() ?: continue
                logger.d("<= $text")

                val req = runCatching { json.decodeFromString(RpcRequest.serializer(), text) }.getOrNull()
                if (req == null) {
                    // Невалидный JSON-RPC — игнорируем
                    continue
                }

                when (req.method) {
                    "initialize" -> {
                        // Ответ: InitializeResult (serverInfo/capabilities как пустые объекты)
                        val resultEl: JsonElement = json.encodeToJsonElement(
                            InitializeResult(
                                serverInfo = buildJsonObject {
                                    put("name", "AIChallenge-MCP")
                                    put("version", "1.0.0")
                                },
                                capabilities = buildJsonObject { }
                            )
                        )
                        val resp = RpcResponse(
                            id = req.id,
                            result = resultEl
                        )
                        val out = json.encodeToString(resp)
                        logger.d("=> $out")
                        send(Frame.Text(out))
                    }

                    "notifications/initialized" -> {
                        // Уведомление — ответа не требуется
                        logger.i("notifications/initialized received")
                    }

                    "tools/list" -> {
                        val resultEl: JsonElement = json.encodeToJsonElement(
                            ToolsListResult(tools = tools)
                        )
                        val resp = RpcResponse(
                            id = req.id,
                            result = resultEl
                        )
                        val out = json.encodeToString(resp)
                        logger.d("=> $out")
                        send(Frame.Text(out))
                    }
                    "tools/call" -> {
                        val params = req.params?.jsonObject
                        val toolName = params?.get("name")?.jsonPrimitive?.content
                        val args = params?.get("arguments")?.jsonObject

                        suspend fun respondError(code: Int, message: String): Unit {
                            val err = RpcError(code = code, message = message)
                            val resp = RpcResponse(id = req.id, error = err)
                            val out = json.encodeToString(resp)
                            logger.d("=> $out")
                            send(Frame.Text(out))
                        }

                        if (toolName == null) {
                            respondError(-32602, "Invalid params: 'name' is required")
                        } else {
                            when (toolName) {
                                "github.list_user_repos" -> {
                                    val username = args?.get("username")?.jsonPrimitive?.content
                                    val perPage = args?.get("per_page")?.jsonPrimitive?.intOrNull ?: 20
                                    val sort = args?.get("sort")?.jsonPrimitive?.content ?: "updated"

                                    if (username.isNullOrBlank()) {
                                        respondError(-32602, "Invalid params: 'username' is required")
                                    } else {
                                        val url = "https://api.github.com/users/${username}/repos"
                                        val response: HttpResponse = httpClient.get(url) {
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
                                            val resp = RpcResponse(id = req.id, result = resultEl)
                                            val out = json.encodeToString(resp)
                                            logger.d("=> $out")
                                            send(Frame.Text(out))
                                        } else {
                                            val body = runCatching { response.bodyAsText() }.getOrDefault("")
                                            respondError(response.status.value, "GitHub API error (${response.status.value}): $body")
                                        }
                                    }
                                }
                                "github.list_my_repos" -> {
                                    val perPage = args?.get("per_page")?.jsonPrimitive?.intOrNull ?: 20
                                    val sort = args?.get("sort")?.jsonPrimitive?.content ?: "updated"
                                    val visibility = args?.get("visibility")?.jsonPrimitive?.content ?: "all"

                                    val token = System.getenv("GITHUB_TOKEN")?.trim().orEmpty()
                                    if (token.isEmpty()) {
                                        respondError(-32000, "GitHub token not configured on server (GITHUB_TOKEN)")
                                    } else {
                                        val url = "https://api.github.com/user/repos"
                                        val response: HttpResponse = httpClient.get(url) {
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
                                            val resp = RpcResponse(id = req.id, result = resultEl)
                                            val out = json.encodeToString(resp)
                                            logger.d("=> $out")
                                            send(Frame.Text(out))
                                        } else {
                                            val body = runCatching { response.bodyAsText() }.getOrDefault("")
                                            respondError(response.status.value, "GitHub API error (${response.status.value}): $body")
                                        }
                                    }
                                }
                                else -> {
                                    respondError(-32601, "Unknown tool: $toolName")
                                }
                            }
                        }
                    }

                    else -> {
                        // Метод не поддерживается
                        val err = RpcError(
                            code = -32601,
                            message = "Method not found: ${req.method}"
                        )
                        val resp = RpcResponse(
                            id = req.id,
                            error = err
                        )
                        val out = json.encodeToString(resp)
                        logger.d("=> $out")
                        send(Frame.Text(out))
                    }
                }
            }

            logger.i("Client disconnected from /mcp")
        }
    }
}
