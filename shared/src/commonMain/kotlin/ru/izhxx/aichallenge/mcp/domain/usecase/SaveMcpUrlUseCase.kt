package ru.izhxx.aichallenge.mcp.domain.usecase

import ru.izhxx.aichallenge.domain.repository.McpConfigRepository

/**
 * UseCase для сохранения WebSocket URL MCP-сервера в настройках.
 */
class SaveMcpUrlUseCase(
    private val configRepository: McpConfigRepository
) {
    /**
     * Сохраняет URL и сбрасывает флаг connected=false (потребуется повторная проверка).
     */
    suspend operator fun invoke(url: String) {
        configRepository.setWsUrl(url)
        configRepository.setConnected(false)
    }
}
