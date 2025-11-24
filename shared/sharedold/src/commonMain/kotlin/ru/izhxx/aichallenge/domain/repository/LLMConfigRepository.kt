package ru.izhxx.aichallenge.domain.repository

import kotlinx.coroutines.flow.Flow
import ru.izhxx.aichallenge.domain.model.config.LLMConfig

/**
 * Интерфейс хранилища настроек конфигурации LLM
 * Позволяет сохранять и загружать настройки, связанные с промптами и форматированием ответов
 */
interface LLMConfigRepository {
    /**
     * Поток настроек конфигурации LLM
     */
    val settingsFlow: Flow<LLMConfig>

    /**
     * Получает настройки конфигурации LLM
     * @return настройки или значения по умолчанию, если настройки не были сохранены
     */
    suspend fun getSettings(): LLMConfig
    
    /**
     * Сохраняет настройки конфигурации LLM
     * @param config настройки для сохранения
     */
    suspend fun saveSettings(config: LLMConfig)

    /**
     * Возвращает настройки к значениям по умолчанию
     */
    suspend fun backToDefaultSettings()
}
