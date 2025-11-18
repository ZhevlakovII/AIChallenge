package ru.izhxx.aichallenge.features.mcp.presentation

/**
 * События экрана MCP (MVI Intent).
 */
sealed interface McpEvent {
    /**
     * Пользователь изменил URL MCP WebSocket.
     */
    data class UrlChanged(val value: String) : McpEvent

    /**
     * Сохранить текущий URL в настройки.
     */
    data object SaveUrl : McpEvent

    /**
     * Проверить соединение с MCP-сервером (и обновить флаг connected).
     */
    data object CheckConnection : McpEvent

    /**
     * Загрузить инструменты (внутри выполняет проверку соединения).
     */
    data object Load : McpEvent
}
