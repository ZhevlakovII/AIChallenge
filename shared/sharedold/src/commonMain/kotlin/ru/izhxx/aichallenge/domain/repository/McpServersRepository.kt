package ru.izhxx.aichallenge.domain.repository

import ru.izhxx.aichallenge.domain.model.mcp.McpServerConfig

/**
 * Репозиторий для хранения и получения списка MCP-серверов (множественная конфигурация).
 * Не заменяет McpConfigRepository (одиночный URL) для обратной совместимости.
 */
interface McpServersRepository {
    /**
     * Возвращает сохранённый список MCP-серверов.
     */
    suspend fun getServers(): List<McpServerConfig>

    /**
     * Сохраняет список MCP-серверов, полностью заменяя существующий.
     */
    suspend fun saveServers(servers: List<McpServerConfig>)
}
