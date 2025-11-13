package ru.izhxx.aichallenge.features.chat.domain.usecase

import ru.izhxx.aichallenge.domain.model.MessageRole
import ru.izhxx.aichallenge.domain.model.message.LLMMessage
import ru.izhxx.aichallenge.domain.model.response.LLMResponse
import ru.izhxx.aichallenge.domain.repository.LLMClientRepository
import ru.izhxx.aichallenge.domain.repository.ProviderSettingsRepository

/**
 * Реализация юзкейса отправки сообщения
 */
class SendMessageUseCaseImpl(
    private val llmClientRepository: LLMClientRepository,
    private val providerSettingsRepository: ProviderSettingsRepository
) : SendMessageUseCase {

    override suspend fun invoke(
        text: String,
        previousMessages: List<LLMMessage>
    ): Result<LLMResponse> {
        // Вызываем перегруженную версию метода без суммаризации
        return invoke(text, previousMessages, null)
    }
    
    override suspend fun invoke(
        text: String,
        previousMessages: List<LLMMessage>,
        summary: String?
    ): Result<LLMResponse> {
        // Проверяем наличие API ключа
        val apiKey = providerSettingsRepository.getApiKey()
        if (apiKey.isEmpty()) {
            return Result.failure(IllegalStateException("API ключ не настроен"))
        }

        // Проверяем, содержит ли последнее сообщение текст, который мы отправляем
        val lastMessage = previousMessages.lastOrNull()
        val messagesForRequest = if (lastMessage?.role == MessageRole.USER && lastMessage.content == text) {
            // Если последнее сообщение уже содержит этот текст, используем сообщения как есть
            previousMessages
        } else {
            // Иначе добавляем новое сообщение пользователя
            val userMessage = LLMMessage(
                role = MessageRole.USER,
                content = text
            )
            previousMessages + userMessage
        }

        // Отправляем запрос через репозиторий с учетом суммаризации
        return llmClientRepository.sendMessagesWithSummary(messagesForRequest, summary)
    }
}
