package ru.izhxx.aichallenge.tools.shared.mcp.model

/**
 * Запрос на вызов MCP инструмента от LLM (часть [AnswerMessage]).
 * Представляет tool call, который LLM запросила выполнить в ответе.
 * Приложение должно обработать этот вызов и вернуть результат в следующем сообщении.
 *
 * @property type Тип инструмента (обычно [McpToolType.Function]).
 * @property function Детали вызываемой функции (имя и параметры).
 *
 * @see McpTool
 * @see McpCallFunction
 * @see McpToolType
 */
class McpCallTool(
    val type: McpToolType,
    val function: McpCallFunction
)
