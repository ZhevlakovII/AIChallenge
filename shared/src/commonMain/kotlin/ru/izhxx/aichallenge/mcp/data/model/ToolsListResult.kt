package ru.izhxx.aichallenge.mcp.data.model

import kotlinx.serialization.Serializable

/**
 * Результат ответа метода MCP "tools/list".
 */
@Serializable
data class ToolsListResult(
    val tools: List<McpToolDTO> = emptyList()
)
