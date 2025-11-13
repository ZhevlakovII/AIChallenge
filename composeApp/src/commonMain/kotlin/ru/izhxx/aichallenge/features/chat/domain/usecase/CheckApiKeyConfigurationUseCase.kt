package ru.izhxx.aichallenge.features.chat.domain.usecase

/**
 * Юзкейс для проверки настройки API ключа
 */
fun interface CheckApiKeyConfigurationUseCase {
    /**
     * Проверяет, настроен ли API ключ
     * @return true, если API ключ настроен; false - если не настроен
     */
    suspend operator fun invoke(): Boolean
}
