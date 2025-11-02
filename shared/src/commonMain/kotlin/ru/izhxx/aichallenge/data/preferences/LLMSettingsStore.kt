package ru.izhxx.aichallenge.data.preferences

import ru.izhxx.aichallenge.domain.model.LLMUserSettings

/**
 * Интерфейс хранилища настроек для работы с LLM API
 * Позволяет сохранять и загружать пользовательские настройки LLM
 */
interface LLMSettingsStore {
    
    /**
     * Проверяет, настроен ли API ключ
     * @return true если API ключ настроен, иначе false
     */
    suspend fun hasApiKey(): Boolean
    
    /**
     * Получает API ключ из хранилища
     * @return API ключ или пустую строку, если ключ не был сохранен
     */
    suspend fun getApiKey(): String
    
    /**
     * Проверяет, существуют ли сохраненные настройки LLM
     * @return true если настройки существуют, иначе false
     */
    suspend fun hasSettings(): Boolean
    
    /**
     * Получает настройки LLM
     * @return настройки LLM или значения по умолчанию, если настройки не были сохранены
     */
    suspend fun getSettings(): LLMUserSettings
    
    /**
     * Сохраняет настройки LLM
     * @param settings настройки LLM для сохранения
     */
    suspend fun saveSettings(settings: LLMUserSettings)
    
    /**
     * Сохраняет только API ключ
     * @param apiKey API ключ для сохранения
     */
    suspend fun saveApiKey(apiKey: String)
    
    /**
     * Очищает сохраненные настройки LLM (возвращает к значениям по умолчанию)
     */
    suspend fun clearSettings()
}
