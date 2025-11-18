package ru.izhxx.aichallenge.features.mcp.presentation

import ru.izhxx.aichallenge.mcp.domain.model.McpTool
import ru.izhxx.aichallenge.common.SERVER_PORT

/**
 * Состояние экрана MCP.
 */
data class McpState(
    val url: String = "ws://127.0.0.1:$SERVER_PORT/mcp",
    val loading: Boolean = false,
    val tools: List<McpTool> = emptyList(),
    val error: String? = null
)
