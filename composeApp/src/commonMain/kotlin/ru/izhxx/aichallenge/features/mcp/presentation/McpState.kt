package ru.izhxx.aichallenge.features.mcp.presentation

import ru.izhxx.aichallenge.mcp.domain.model.McpTool

/**
 * Состояние экрана MCP.
 */
data class McpState(
    val url: String = "ws://localhost:3000/mcp",
    val loading: Boolean = false,
    val tools: List<McpTool> = emptyList(),
    val error: String? = null
)
