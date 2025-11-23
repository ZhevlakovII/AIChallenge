package ru.izhxx.aichallenge.mcp.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * DTO инструмента MCP (слой Data).
 */
@Serializable
data class McpToolDTO(
    val name: String,
    val description: String? = null,
    val inputSchema: JsonElement? = null
)
