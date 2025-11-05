package ru.izhxx.aichallenge.domain.repository

import ru.izhxx.aichallenge.domain.model.ParsedResponse
import ru.izhxx.aichallenge.domain.model.openai.ChatMessage

/**
 * Репозиторий для работы с LLM клиентом
 */
interface LLMClientRepository {
    /**
     * Отправляет сообщение без истории
     * @param messages сообщения пользователя
     * @return результат выполнения запроса с разобранным ответом
     */
    suspend fun sendMessage(messages: List<ChatMessage>): Result<ParsedResponse>
}
