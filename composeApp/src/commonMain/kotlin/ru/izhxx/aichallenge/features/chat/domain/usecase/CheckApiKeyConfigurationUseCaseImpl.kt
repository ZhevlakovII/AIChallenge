package ru.izhxx.aichallenge.features.chat.domain.usecase

import ru.izhxx.aichallenge.domain.repository.ProviderSettingsRepository

/**
 * Реализация юзкейса проверки API ключа
 */
class CheckApiKeyConfigurationUseCaseImpl(
    private val providerSettingsRepository: ProviderSettingsRepository
) : CheckApiKeyConfigurationUseCase {

    override suspend fun invoke(): Boolean = providerSettingsRepository.getApiKey().isNotEmpty()
}
