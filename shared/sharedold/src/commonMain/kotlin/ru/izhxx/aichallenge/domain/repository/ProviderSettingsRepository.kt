package ru.izhxx.aichallenge.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.izhxx.aichallenge.domain.model.config.ProviderSettings

/**
 * Интерфейс хранилища настроек провайдера LLM API
 * Позволяет сохранять и загружать настройки провайдера LLM (API ключ, URL и т.д.)
 */
interface ProviderSettingsRepository {
    /**
     * Поток настроек провайдера LLM
     */
    val settingsFlow: Flow<ProviderSettings>

    /**
     * Получает API ключ из хранилища
     * @return API ключ или пустую строку, если ключ не был сохранен
     */
    suspend fun getApiKey(): String

    /**
     * Получает настройки провайдера LLM
     * @return настройки провайдера или значения по умолчанию, если настройки не были сохранены
     */
    suspend fun getSettings(): ProviderSettings
    
    /**
     * Сохраняет настройки провайдера LLM
     * @param settings настройки провайдера для сохранения
     */
    suspend fun saveSettings(settings: ProviderSettings)

    /**
     * Возвращает настройки к значениям по умолчанию
     */
    suspend fun backToDefaultSettings()
}
