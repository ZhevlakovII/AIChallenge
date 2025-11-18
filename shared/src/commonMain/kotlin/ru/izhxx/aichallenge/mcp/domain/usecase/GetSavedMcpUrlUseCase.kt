package ru.izhxx.aichallenge.mcp.domain.usecase

import ru.izhxx.aichallenge.common.SERVER_PORT
import ru.izhxx.aichallenge.domain.repository.McpConfigRepository

/**
 * UseCase для получения сохранённого MCP WebSocket URL.
 * Если URL не сохранён, возвращает значение по умолчанию ws://127.0.0.1:<SERVER_PORT>/mcp.
 */
class GetSavedMcpUrlUseCase(
    private val configRepository: McpConfigRepository
) {
    suspend operator fun invoke(): String {
        return configRepository.getWsUrl() ?: "ws://127.0.0.1:$SERVER_PORT/mcp"
    }
}
