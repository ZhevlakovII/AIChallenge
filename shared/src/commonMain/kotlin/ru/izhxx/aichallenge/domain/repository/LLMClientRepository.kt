package ru.izhxx.aichallenge.domain.repository

import ru.izhxx.aichallenge.domain.model.message.LLMMessage
import ru.izhxx.aichallenge.domain.model.response.LLMResponse

/**
 * Репозиторий для работы с LLM клиентом
 */
interface LLMClientRepository {
    /**
     * Отправляет список сообщений
     * @param messages список сообщений
     * @return результат выполнения запроса с ответом LLM
     */
    suspend fun sendMessages(messages: List<LLMMessage>): Result<LLMResponse>
    
    /**
     * Отправляет список сообщений с учетом суммаризации
     * @param messages список сообщений
     * @param summary суммаризация предыдущей истории диалога (может быть null)
     * @return результат выполнения запроса с ответом LLM
     */
    suspend fun sendMessagesWithSummary(
        messages: List<LLMMessage>, 
        summary: String?
    ): Result<LLMResponse>
}
