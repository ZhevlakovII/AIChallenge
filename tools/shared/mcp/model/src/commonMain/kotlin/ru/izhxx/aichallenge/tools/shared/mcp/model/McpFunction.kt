package ru.izhxx.aichallenge.tools.shared.mcp.model

import kotlinx.serialization.json.JsonObject

/**
 * Описание функции для MCP Tool (часть [McpTool]).
 * Определяет доступную функцию, которую LLM может вызвать через function calling.
 *
 * @property name Уникальное имя функции (используется для идентификации при вызове).
 * @property description Описание назначения функции для LLM (должно быть четким и информативным).
 * @property parameters Схема параметров функции в формате JSON Schema (null, если параметров нет).
 *
 * @see McpTool
 * @see McpCallFunction
 */
class McpFunction(
    val name: String,
    val description: String,
    val parameters: JsonObject?,
)