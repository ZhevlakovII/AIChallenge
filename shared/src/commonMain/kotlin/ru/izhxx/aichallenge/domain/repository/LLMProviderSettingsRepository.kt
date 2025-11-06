package ru.izhxx.aichallenge.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.izhxx.aichallenge.domain.model.llmsettings.LLMProviderSettings

/**
 * Интерфейс хранилища настроек провайдера LLM API
 * Позволяет сохранять и загружать настройки провайдера LLM (API ключ, URL и т.д.)
 */
interface LLMProviderSettingsRepository {

    val settingsFlow: Flow<LLMProviderSettings>

    /**
     * Получает API ключ из хранилища
     * @return API ключ или пустую строку, если ключ не был сохранен
     */
    suspend fun getApiKey(): String

    /**
     * Получает настройки провайдера LLM
     * @return настройки провайдера или значения по умолчанию, если настройки не были сохранены
     */
    suspend fun getSettings(): LLMProviderSettings
    
    /**
     * Сохраняет настройки провайдера LLM
     * @param settings настройки провайдера для сохранения
     */
    suspend fun saveSettings(settings: LLMProviderSettings)

    /**
     * Очищает сохраненные настройки провайдера (возвращает к значениям по умолчанию)
     */
    suspend fun clearSettings()
}
