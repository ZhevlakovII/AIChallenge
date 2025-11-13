package ru.izhxx.aichallenge.features.chat.domain.usecase

import ru.izhxx.aichallenge.domain.model.message.LLMMessage
import ru.izhxx.aichallenge.domain.model.response.LLMResponse

/**
 * Юзкейс для отправки сообщения в LLM
 */
interface SendMessageUseCase {
    /**
     * Отправляет сообщение пользователя в LLM и возвращает ответ
     * @param text текст сообщения пользователя
     * @param previousMessages предыдущие сообщения для контекста
     * @return результат с ответом LLM или ошибкой
     */
    suspend operator fun invoke(
        text: String,
        previousMessages: List<LLMMessage>
    ): Result<LLMResponse>
    
    /**
     * Отправляет сообщение пользователя в LLM с учетом суммаризации и возвращает ответ
     * @param text текст сообщения пользователя
     * @param previousMessages предыдущие сообщения для контекста
     * @param summary суммаризация предыдущей истории диалога (может быть null)
     * @return результат с ответом LLM или ошибкой
     */
    suspend operator fun invoke(
        text: String,
        previousMessages: List<LLMMessage>,
        summary: String?
    ): Result<LLMResponse>
}
