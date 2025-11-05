package ru.izhxx.aichallenge.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.izhxx.aichallenge.domain.model.llmsettings.LLMPromptSettings

/**
 * Интерфейс хранилища настроек промпта LLM
 * Позволяет сохранять и загружать настройки, связанные с промптами и форматированием ответов
 */
interface LLMPromptSettingsRepository {

    val settingsFlow: Flow<LLMPromptSettings>

    /**
     * Получает настройки промпта LLM
     * @return настройки промпта или значения по умолчанию, если настройки не были сохранены
     */
    suspend fun getSettings(): LLMPromptSettings
    
    /**
     * Сохраняет настройки промпта LLM
     * @param settings настройки промпта для сохранения
     */
    suspend fun saveSettings(settings: LLMPromptSettings)

    /**
     * Очищает сохраненные настройки промпта (возвращает к значениям по умолчанию)
     */
    suspend fun clearSettings()
}
