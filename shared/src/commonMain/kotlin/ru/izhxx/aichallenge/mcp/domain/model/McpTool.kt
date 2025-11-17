package ru.izhxx.aichallenge.mcp.domain.model

/**
 * Доменная модель инструмента MCP.
 *
 * @property name Имя инструмента
 * @property description Описание инструмента (опционально)
 * @property inputSchema JSON-схема входных параметров инструмента в компактном текстовом виде (опционально)
 */
data class McpTool(
    val name: String,
    val description: String?,
    val inputSchema: String?
)
