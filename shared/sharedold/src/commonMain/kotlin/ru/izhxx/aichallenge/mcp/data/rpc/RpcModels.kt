package ru.izhxx.aichallenge.mcp.data.rpc

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Базовые модели JSON-RPC 2.0 для MCP транспорта.
 */

@Serializable
data class RpcRequest(
    val id: Int? = null,
    val method: String,
    val params: JsonElement? = null,
    val jsonrpc: String = "2.0"
)

@Serializable
data class RpcError(
    val code: Int,
    val message: String,
    val data: JsonElement? = null
)

@Serializable
data class RpcResponse(
    val id: Int? = null,
    val result: JsonElement? = null,
    val error: RpcError? = null,
    val jsonrpc: String = "2.0"
)

/**
 * Initialize
 */
@Serializable
data class ClientInfo(
    val name: String,
    val version: String
)

@Serializable
data class InitializeParams(
    val protocolVersion: String,
    val capabilities: JsonElement,
    val clientInfo: ClientInfo
)

@Serializable
data class InitializeResult(
    val serverInfo: JsonElement? = null,
    val capabilities: JsonElement? = null
)
