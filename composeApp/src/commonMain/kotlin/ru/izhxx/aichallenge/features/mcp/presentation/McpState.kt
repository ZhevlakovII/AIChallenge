package ru.izhxx.aichallenge.features.mcp.presentation

import ru.izhxx.aichallenge.mcp.domain.model.McpTool
import ru.izhxx.aichallenge.common.SERVER_PORT

/**
 * Состояние экрана MCP (минимальная поддержка двух серверов).
 */
data class McpState(
    val url1: String = "ws://127.0.0.1:$SERVER_PORT/mcp",
    val url2: String = "ws://127.0.0.1:${SERVER_PORT + 1}/mcp",
    val loading: Boolean = false,
    val connected1: Boolean = false,
    val connected2: Boolean = false,
    // Объединённый список инструментов (distinct по name)
    val tools: List<McpTool> = emptyList(),
    val error: String? = null
)
