package ru.izhxx.aichallenge.features.mcp.presentation

/**
 * События экрана MCP (MVI Intent).
 */
sealed interface McpEvent {
    data class UrlChanged(val value: String) : McpEvent
    data object Load : McpEvent
}
