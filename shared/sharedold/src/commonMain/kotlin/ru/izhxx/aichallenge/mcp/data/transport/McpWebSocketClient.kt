package ru.izhxx.aichallenge.mcp.data.transport

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.put
import ru.izhxx.aichallenge.common.Logger
import ru.izhxx.aichallenge.mcp.data.model.ToolsListResult
import ru.izhxx.aichallenge.mcp.data.rpc.ClientInfo
import ru.izhxx.aichallenge.mcp.data.rpc.InitializeParams
import ru.izhxx.aichallenge.mcp.data.rpc.InitializeResult
import ru.izhxx.aichallenge.mcp.data.rpc.RpcRequest
import ru.izhxx.aichallenge.mcp.data.rpc.RpcResponse

/**
 * Минимальный WebSocket клиент MCP (JSON-RPC 2.0) для получения списка инструментов.
 */
class McpWebSocketClient(
    private val httpClient: HttpClient,
    private val json: Json
) {
    private val logger = Logger("MCP")

    /**
     * Устанавливает соединение с MCP-сервером и возвращает результат вызова tools/list.
     *
     * Последовательность:
     * 1) initialize (id=1)
     * 2) notifications/initialized (notification)
     * 3) tools/list (id=2)
     */
    suspend fun listTools(wsUrl: String): Result<ToolsListResult> = runCatching {
        logger.i("Connecting to MCP server: $wsUrl")
        var initReceived = false
        var toolsReceived: ToolsListResult? = null

        httpClient.webSocket(urlString = wsUrl) {
            // 1) initialize
            val initParams = InitializeParams(
                protocolVersion = "2024-11-05",
                capabilities = buildJsonObject { },
                clientInfo = ClientInfo(name = "AIChallenge", version = "1.0.0")
            )
            val initReq = RpcRequest(
                id = 1,
                method = "initialize",
                params = json.encodeToJsonElement(initParams)
            )
            val initText = json.encodeToString(initReq)
            logger.d("-> initialize: $initText")
            send(Frame.Text(initText))

            // 2) initialized notification (без id)
            val initializedNotif = RpcRequest(
                id = null,
                method = "notifications/initialized",
                params = buildJsonObject { }
            )
            val initializedText = json.encodeToString(initializedNotif)
            logger.d("-> initialized (notification): $initializedText")
            send(Frame.Text(initializedText))

            // 3) tools/list
            val toolsReq = RpcRequest(
                id = 2,
                method = "tools/list",
                params = null
            )
            val toolsText = json.encodeToString(toolsReq)
            logger.d("-> tools/list: $toolsText")
            send(Frame.Text(toolsText))

            // Ждём ответы на id=1 и id=2
            withTimeout(15_000) {
                while (!(initReceived && toolsReceived != null)) {
                    val frame = incoming.receive()
                    val text = (frame as? Frame.Text)?.readText() ?: continue
                    logger.d("<- $text")

                    val resp = kotlin.runCatching { json.decodeFromString(RpcResponse.serializer(), text) }.getOrNull()
                        ?: continue

                    if (resp.id == 1) {
                        // initialize result
                        if (resp.error != null) {
                            error("MCP initialize error: ${resp.error.code} ${resp.error.message}")
                        }
                        val resultEl: JsonElement = resp.result ?: error("Empty initialize result")
                        // парсим для валидации структуры, данные не используются далее
                        json.decodeFromJsonElement<InitializeResult>(resultEl)
                        initReceived = true
                        logger.i("initialize ok")
                    } else if (resp.id == 2) {
                        if (resp.error != null) {
                            error("MCP tools/list error: ${resp.error.code} ${resp.error.message}")
                        }
                        val resultEl: JsonElement = resp.result ?: error("Empty tools/list result")
                        toolsReceived = json.decodeFromJsonElement<ToolsListResult>(resultEl)
                        logger.i("tools/list ok: ${toolsReceived?.tools?.size} tools")
                    }
                }
            }
        }

        return@runCatching toolsReceived ?: ToolsListResult(emptyList())
    }

    /**
     * Вызывает инструмент MCP "tools/call" и возвращает JSON-результат.
     *
     * Последовательность:
     * 1) initialize (id=1)
     * 2) notifications/initialized (notification)
     * 3) tools/call (id=3)
     *
     * @param wsUrl WebSocket URL MCP
     * @param name имя инструмента (например, "github.list_user_repos")
     * @param args аргументы инструмента (JSON-объект) или null
     */
    suspend fun callTool(
        wsUrl: String,
        name: String,
        args: JsonElement?
    ): Result<JsonElement> = runCatching {
        logger.i("Connecting to MCP server for tools/call: $wsUrl, tool=$name")
        var initReceived = false
        var callResult: JsonElement? = null

        httpClient.webSocket(urlString = wsUrl) {
            // 1) initialize
            val initParams = InitializeParams(
                protocolVersion = "2024-11-05",
                capabilities = buildJsonObject { },
                clientInfo = ClientInfo(name = "AIChallenge", version = "1.0.0")
            )
            val initReq = RpcRequest(
                id = 1,
                method = "initialize",
                params = json.encodeToJsonElement(initParams)
            )
            val initText = json.encodeToString(initReq)
            logger.d("-> initialize: $initText")
            send(Frame.Text(initText))

            // 2) initialized notification
            val initializedNotif = RpcRequest(
                id = null,
                method = "notifications/initialized",
                params = buildJsonObject { }
            )
            val initializedText = json.encodeToString(initializedNotif)
            logger.d("-> initialized (notification): $initializedText")
            send(Frame.Text(initializedText))

            // 3) tools/call
            val params = buildJsonObject {
                put("name", name)
                if (args != null) {
                    put("arguments", args)
                }
            }
            val callReq = RpcRequest(
                id = 3,
                method = "tools/call",
                params = params
            )
            val callText = json.encodeToString(callReq)
            logger.d("-> tools/call: $callText")
            send(Frame.Text(callText))

            // Ждём ответы initialize и tools/call
            withTimeout(20_000) {
                while (!(initReceived && callResult != null)) {
                    val frame = incoming.receive()
                    val text = (frame as? Frame.Text)?.readText() ?: continue
                    logger.d("<- $text")

                    val resp = kotlin.runCatching { json.decodeFromString(RpcResponse.serializer(), text) }.getOrNull()
                        ?: continue

                    if (resp.id == 1) {
                        if (resp.error != null) {
                            error("MCP initialize error: ${resp.error.code} ${resp.error.message}")
                        }
                        val resultEl: JsonElement = resp.result ?: error("Empty initialize result")
                        json.decodeFromJsonElement<InitializeResult>(resultEl)
                        initReceived = true
                        logger.i("initialize ok")
                    } else if (resp.id == 3) {
                        if (resp.error != null) {
                            error("MCP tools/call error: ${resp.error.code} ${resp.error.message}")
                        }
                        callResult = resp.result ?: error("Empty tools/call result")
                        logger.i("tools/call ok")
                    }
                }
            }
        }

        return@runCatching callResult!!
    }
}
