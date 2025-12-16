package ru.izhxx.aichallenge.tools.llm.completions.api.model.answer

import ru.izhxx.aichallenge.tools.llm.completions.api.model.Usage

/**
 * Модель ответа от LLM API.
 *
 * Назначение:
 * - Представляет полный ответ от LLM провайдера (OpenAI, Anthropic и т.д.).
 * - Содержит сгенерированные варианты ответа ([choices]) и метаданные запроса.
 *
 * Структура:
 * - [id] - уникальный идентификатор ответа от API провайдера.
 * - [createdAt] - временная метка создания ответа (Unix timestamp).
 * - [model] - имя использованной модели (например, "gpt-4", "claude-3-opus").
 * - [choices] - список сгенерированных вариантов ответа (обычно один элемент).
 * - [usage] - информация об использовании токенов (опциональна, зависит от провайдера).
 *
 * Правила:
 * - Ответ может содержать несколько [choices], но обычно используется первый.
 * - [usage] может быть null для провайдеров, не предоставляющих информацию об использовании.
 *
 * Пример:
 * ```kotlin
 * val answer = Answer(
 *     id = "chatcmpl-123",
 *     createdAt = 1234567890L,
 *     model = "gpt-4",
 *     choices = listOf(
 *         Choice(
 *             index = 0,
 *             message = AnswerMessage(role = MessageRole.Assistant, content = "Hello!")
 *         )
 *     ),
 *     usage = Usage(promptTokens = 10, completionTokens = 5, totalTokens = 15)
 * )
 * ```
 *
 * @property id Уникальный идентификатор ответа.
 * @property createdAt Unix timestamp создания ответа.
 * @property model Имя использованной модели.
 * @property choices Список вариантов ответа от LLM.
 * @property usage Информация об использовании токенов (опционально).
 *
 * @see Choice
 * @see Usage
 */
class Answer(
    val id: String,
    val createdAt: Long,
    val model: String,
    val choices: List<Choice>,
    val usage: Usage?
)