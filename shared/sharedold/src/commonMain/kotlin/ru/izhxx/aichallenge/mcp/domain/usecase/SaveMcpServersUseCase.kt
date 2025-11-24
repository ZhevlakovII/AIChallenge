package ru.izhxx.aichallenge.mcp.domain.usecase

import ru.izhxx.aichallenge.domain.model.mcp.McpServerConfig
import ru.izhxx.aichallenge.domain.repository.McpServersRepository

/**
 * Сохраняет список MCP-серверов.
 */
class SaveMcpServersUseCase(
    private val repository: McpServersRepository
) {
    suspend operator fun invoke(servers: List<McpServerConfig>) {
        repository.saveServers(servers)
    }
}
