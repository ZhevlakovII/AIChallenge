package ru.izhxx.aichallenge.mcp.domain.repository

import ru.izhxx.aichallenge.mcp.domain.model.McpTool

/**
 * Репозиторий для взаимодействия с MCP-сервером.
 */
interface McpRepository {
    /**
     * Устанавливает MCP-соединение и возвращает список доступных инструментов.
     *
     * @param wsUrl WebSocket URL MCP сервера (например, ws://localhost:3000/mcp)
     */
    suspend fun listTools(wsUrl: String): Result<List<McpTool>>
}
