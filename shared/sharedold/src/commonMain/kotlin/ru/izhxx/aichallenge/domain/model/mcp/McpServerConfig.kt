package ru.izhxx.aichallenge.domain.model.mcp


/**
 * Конфигурация отдельного MCP-сервера.
 *
 * [id] — стабильный идентификатор (UUID/произвольный), чтобы ссылаться на сервер в хранилище.
 * [name] — удобочитаемое имя (для UI).
 * [url] — WebSocket URL (например, ws://127.0.0.1:8080/mcp).
 * [tags] — произвольные метки (например, "primary","secondary","external").
 * [toolPrefixes] — подсказка для оркестратора (какие префиксы инструментов, например "github.", "notes.").
 */
data class McpServerConfig(
    val id: String,
    val name: String,
    val url: String,
    val tags: Set<String> = emptySet(),
    val toolPrefixes: Set<String> = emptySet()
)
