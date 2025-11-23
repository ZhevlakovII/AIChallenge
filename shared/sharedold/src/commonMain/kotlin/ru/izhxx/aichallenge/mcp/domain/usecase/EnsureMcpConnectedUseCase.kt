package ru.izhxx.aichallenge.mcp.domain.usecase

import ru.izhxx.aichallenge.domain.repository.McpConfigRepository
import ru.izhxx.aichallenge.mcp.domain.repository.McpRepository

/**
 * UseCase для проверки и установки соединения с MCP-сервером.
 *
 * Выполняет вызов listTools(wsUrl). Если успешно — помечает состояние connected=true в репозитории конфигурации.
 * Если неуспешно — connected=false и возвращает ошибку.
 */
class EnsureMcpConnectedUseCase(
    private val mcpRepository: McpRepository,
    private val configRepository: McpConfigRepository
) {
    /**
     * Проверяет соединение с MCP. Использует переданный URL.
     *
     * @param wsUrl WebSocket URL MCP-сервера
     * @return Result<Unit> — успех/ошибка соединения
     */
    suspend operator fun invoke(wsUrl: String): Result<Unit> {
        return mcpRepository.listTools(wsUrl).map {
            // Соединение успешно — сохраняем признак
            // (список инструментов можем игнорировать здесь, он нужен вызывающей стороне)
            Unit
        }.onSuccess {
            configRepository.setConnected(true)
        }.onFailure {
            configRepository.setConnected(false)
        }
    }
}
