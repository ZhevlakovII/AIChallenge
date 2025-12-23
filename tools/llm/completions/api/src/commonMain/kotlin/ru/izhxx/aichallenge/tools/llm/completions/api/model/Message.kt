package ru.izhxx.aichallenge.tools.llm.completions.api.model

/**
 * Модель сообщений для общения с LLM.
 *
 * @property role Роль сообщения, все роли перечислены в [MessageRole]
 * @property content Контент сообщения (текст пользователя, системный промт, ответ LLM)
 */
class Message(
    val role: MessageRole,
    val content: String
)