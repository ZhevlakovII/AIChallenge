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
    
    /**
     * Отправляет список сообщений с кастомным системным промптом (игнорируя глобальный из конфигурации).
     * Используется для задач reminder, где system prompt задаётся на уровне задачи.
     *
     * @param systemPrompt системный промпт для текущего запроса
     * @param messages список сообщений (обычно одно user-сообщение)
     * @param summary суммаризация контекста (опционально)
     * @return результат выполнения запроса с ответом LLM
     */
    suspend fun sendMessagesWithCustomSystem(
        systemPrompt: String,
        messages: List<LLMMessage>,
        summary: String? = null
    ): Result<LLMResponse>
}
