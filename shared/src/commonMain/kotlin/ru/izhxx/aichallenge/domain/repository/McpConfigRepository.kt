package ru.izhxx.aichallenge.domain.repository

/**
 * Репозиторий конфигурации MCP (хранение URL и статуса соединения).
 *
 * Хранение реализуется через DataStore (Preferences).
 */
interface McpConfigRepository {
    /**
     * Возвращает сохранённый WebSocket URL MCP-сервера или null, если он ещё не задан.
     */
    suspend fun getWsUrl(): String?

    /**
     * Сохраняет WebSocket URL MCP-сервера.
     */
    suspend fun setWsUrl(url: String)

    /**
     * Возвращает признак установленного соединения с MCP-сервером.
     * Значение управляется приложением после успешной проверки соединения.
     */
    suspend fun isConnected(): Boolean

    /**
     * Устанавливает признак соединения с MCP-сервером.
     */
    suspend fun setConnected(connected: Boolean)
}
