package ru.izhxx.aichallenge

import io.ktor.server.application.Application
import io.ktor.server.application.call
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
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
