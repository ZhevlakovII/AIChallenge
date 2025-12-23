package ru.izhxx.aichallenge.tools.shared.mcp.model

/**
 * Модель MCP Tool для передачи в LLM API (Model Context Protocol).
 *
 * Назначение:
 * - Представляет инструмент (tool), который LLM может использовать через function calling.
 * - Используется в интеграции с MCP (Model Context Protocol) для расширения возможностей LLM.
 * - Позволяет LLM вызывать функции и получать структурированные ответы.
 *
 * Структура:
 * - [type] - тип инструмента (обычно "function" для function calling).
 * - [function] - описание функции (имя, параметры, описание).
 *
 * Применение:
 * - Передавайте список McpTool в [CompletionsApiRepository.sendMessage].
 * - LLM может запросить вызов функции, указав имя и параметры.
 * - Приложение должно выполнить вызов и вернуть результат в следующем сообщении.
 *
 * Правила:
 * - Описание функции должно быть четким для корректной работы LLM.
 * - Параметры функции должны быть описаны в формате JSON Schema.
 *
 * Пример:
 * ```kotlin
 * val weatherTool = McpTool(
 *     type = McpToolType.Function,
 *     function = McpFunction(
 *         name = "get_weather",
 *         description = "Get current weather for a location",
 *         parameters = mapOf(
 *             "location" to "City name",
 *             "units" to "Temperature units (celsius or fahrenheit)"
 *         )
 *     )
 * )
 *
 * // Отправка в LLM:
 * repository.sendMessage(
 *     parametersConfig = parametersConfig,
 *     providerConfig = providerConfig,
 *     messages = messages,
 *     tools = listOf(weatherTool)
 * )
 * ```
 *
 * @property type Тип инструмента MCP.
 * @property function Описание функции для вызова.
 *
 * @see McpToolType
 * @see McpFunction
 */
class McpTool(
    val type: McpToolType,
    val function: McpFunction
)