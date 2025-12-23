package ru.izhxx.aichallenge.tools.shared.mcp.model

import kotlinx.serialization.json.JsonObject

/**
 * Запрос на вызов функции от LLM (часть [McpCallTool]).
 * Представляет конкретный вызов функции, который LLM запросила выполнить.
 *
 * @property name Имя вызываемой функции (должно соответствовать [McpFunction.name]).
 * @property arguments Аргументы функции в формате JSON (может быть null, если функция без параметров).
 *
 * @see McpCallTool
 * @see McpFunction
 */
class McpCallFunction(
    val name: String,
    val arguments: JsonObject?,
)