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
        // Проверяем наличие API ключа
        val apiKey = providerSettingsRepository.getApiKey()
        if (apiKey.isEmpty()) {
            return Result.failure(IllegalStateException("API ключ не настроен"))
        }

        // Формируем сообщение пользователя
        val userMessage = LLMMessage(
            role = MessageRole.USER,
            content = text
        )

        // Отправляем запрос через репозиторий
        return llmClientRepository.sendMessages(previousMessages + userMessage)
    }
}
