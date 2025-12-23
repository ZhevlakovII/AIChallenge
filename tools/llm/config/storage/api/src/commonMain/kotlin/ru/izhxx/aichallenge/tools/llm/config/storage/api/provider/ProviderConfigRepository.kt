package ru.izhxx.aichallenge.tools.llm.config.storage.api.provider

import ru.izhxx.aichallenge.core.url.Url
import ru.izhxx.aichallenge.tools.llm.config.model.ProviderConfig

/**
 * Интерфейс репозитория для управления конфигурацией LLM провайдера.
 *
 * Назначение:
 * - Предоставляет абстракцию для хранения и получения настроек LLM провайдера.
 * - Позволяет обновлять отдельные параметры или всю конфигурацию целиком.
 * - Используется для персистентного хранения настроек провайдера (API ключ, модель, URL).
 *
 * Применение:
 * - Используйте для сохранения и загрузки пользовательских настроек провайдера.
 * - Реализация обычно использует DataStore или базу данных для персистентности.
 * - Все операции асинхронные через suspend функции.
 *
 * Правила:
 * - Конфигурация должна сохраняться между запусками приложения.
 * - При первом запуске должны использоваться значения по умолчанию.
 * - Все методы update должны сохранять изменения немедленно.
 *
 * Пример:
 * ```kotlin
 * class SettingsViewModel(
 *     private val repository: ProviderConfigRepository
 * ) {
 *     suspend fun loadSettings() {
 *         val config = repository.getConfig()
 *         // Отобразить настройки в UI
 *     }
 *
 *     suspend fun changeModel(newModel: String) {
 *         repository.updateModel(newModel)
 *     }
 * }
 * ```
 *
 * @see ProviderConfig
 */
interface ProviderConfigRepository {

    /**
     * Получает текущую конфигурацию провайдера.
     *
     * @return Текущая конфигурация провайдера.
     */
    suspend fun getConfig(): ProviderConfig

    /**
     * Обновляет API URL провайдера.
     *
     * @param url Новый API URL.
     */
    suspend fun updateApiUrl(url: Url)

    /**
     * Обновляет имя модели LLM.
     *
     * @param model Новое имя модели (например, "gpt-4", "claude-3-opus").
     */
    suspend fun updateModel(model: String)

    /**
     * Обновляет API ключ провайдера.
     *
     * @param key Новый API ключ.
     */
    suspend fun updateApiKey(key: String)

    /**
     * Обновляет всю конфигурацию провайдера целиком.
     *
     * @param config Новая конфигурация провайдера.
     */
    suspend fun updateConfig(config: ProviderConfig)
}