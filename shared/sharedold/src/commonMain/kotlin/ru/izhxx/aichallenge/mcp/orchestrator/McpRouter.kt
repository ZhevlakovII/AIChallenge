package ru.izhxx.aichallenge.mcp.orchestrator

import ru.izhxx.aichallenge.domain.model.mcp.McpServerConfig

/**
 * Простой роутер инструментов MCP: сопоставляет имя инструмента серверу (wsUrl).
 * Политика: первый встретившийся инструмент побеждает (при конфликтах имен).
 */
interface McpRouter {
    /**
     * Перестраивает реестр (toolName -> wsUrl) на основе списка серверов.
     * Возвращает построенный реестр.
     */
    suspend fun rebuildRegistry(servers: List<McpServerConfig>): Map<String, String>

    /**
     * Находит toolName в wsUrl зарегистрированного сервера. Возвращает null, если не найден.
     */
    suspend fun resolve(toolName: String): String?

    /**
     * Текущий слепок реестра.
     */
    suspend fun registry(): Map<String, String>
}
