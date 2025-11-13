package ru.izhxx.aichallenge.features.chat.domain.usecase

import ru.izhxx.aichallenge.domain.model.message.LLMMessage
import ru.izhxx.aichallenge.domain.model.response.LLMResponse

/**
 * Юзкейс для отправки сообщения в LLM
 */
fun interface SendMessageUseCase {
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
}
