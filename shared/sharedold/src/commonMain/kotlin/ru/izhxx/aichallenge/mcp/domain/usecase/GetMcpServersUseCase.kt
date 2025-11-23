package ru.izhxx.aichallenge.mcp.domain.usecase

import ru.izhxx.aichallenge.domain.model.mcp.McpServerConfig
import ru.izhxx.aichallenge.domain.repository.McpServersRepository

/**
 * Возвращает список сохранённых MCP-серверов.
 */
class GetMcpServersUseCase(
    private val repository: McpServersRepository
) {
    suspend operator fun invoke(): List<McpServerConfig> = repository.getServers()
}
