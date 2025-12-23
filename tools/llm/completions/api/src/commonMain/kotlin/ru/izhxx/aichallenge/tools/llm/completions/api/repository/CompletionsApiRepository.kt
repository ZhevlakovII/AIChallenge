package ru.izhxx.aichallenge.tools.llm.completions.api.repository

import ru.izhxx.aichallenge.core.result.AppResult
import ru.izhxx.aichallenge.tools.llm.completions.api.model.Message
import ru.izhxx.aichallenge.tools.llm.completions.api.model.answer.Answer
import ru.izhxx.aichallenge.tools.llm.config.model.ParametersConfig
import ru.izhxx.aichallenge.tools.llm.config.model.ProviderConfig
import ru.izhxx.aichallenge.tools.shared.mcp.model.McpTool

/**
 * Интерфейс репозитория для взаимодействия с LLM (Large Language Model) API.
 *
 * Назначение:
 * - Предоставляет абстракцию для отправки сообщений в LLM и получения ответов.
 * - Поддерживает конфигурацию провайдера (OpenAI, Anthropic и т.д.) и параметров генерации.
 * - Позволяет передавать инструменты (tools) для function calling.
 *
 * Применение:
 * - Используйте для интеграции с различными LLM провайдерами.
 * - Реализация должна обрабатывать сетевые запросы, сериализацию и обработку ошибок.
 * - Результат возвращается через [AppResult] для типобезопасной обработки ошибок.
 *
 * Правила:
 * - Реализация должна быть платформо-независимой (Kotlin Multiplatform).
 * - Используйте [ParametersConfig] для настройки temperature, max_tokens и других параметров.
 * - Используйте [ProviderConfig] для настройки провайдера (API ключ, base URL, модель).
 *
 * @see Message
 * @see Answer
 * @see ParametersConfig
 * @see ProviderConfig
 * @see McpTool
 */
interface CompletionsApiRepository {

    /**
     * Отправляет сообщения в LLM и возвращает ответ.
     *
     * @param parametersConfig Конфигурация параметров генерации (temperature, max_tokens и т.д.).
     * @param providerConfig Конфигурация провайдера LLM (API ключ, модель, base URL).
     * @param messages История сообщений для отправки в LLM.
     * @param tools Список доступных инструментов для function calling (MCP tools).
     * @return [AppResult] с [Answer] при успехе или ошибкой при неудаче.
     */
    suspend fun sendMessage(
        parametersConfig: ParametersConfig,
        providerConfig: ProviderConfig,
        messages: List<Message>,
        tools: List<McpTool>
    ): AppResult<Answer>
}