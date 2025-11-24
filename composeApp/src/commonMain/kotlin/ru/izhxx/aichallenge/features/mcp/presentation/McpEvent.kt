package ru.izhxx.aichallenge.features.mcp.presentation

/**
 * События экрана MCP (MVI Intent).
 *
 * Поддержка двух MCP-серверов (минимальный UI без динамического списка).
 */
sealed interface McpEvent {
    // Server #1
    data class Url1Changed(val value: String) : McpEvent
    data object SaveServers : McpEvent
    data object CheckConnections : McpEvent
    data object LoadToolsUnion : McpEvent

    // Server #2
    data class Url2Changed(val value: String) : McpEvent
}
