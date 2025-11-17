package ru.izhxx.aichallenge.mcp.domain.usecase

import ru.izhxx.aichallenge.mcp.domain.model.McpTool
import ru.izhxx.aichallenge.mcp.domain.repository.McpRepository

/**
 * Юзкейс для получения списка инструментов MCP.
 */
class GetMcpToolsUseCase(
    private val repository: McpRepository
) {
    /**
     * Выполняет подключение к MCP и возвращает список инструментов.
     *
     * @param wsUrl WebSocket URL MCP сервера
     */
    suspend operator fun invoke(wsUrl: String): Result<List<McpTool>> {
        return repository.listTools(wsUrl)
    }
}
