package ru.izhxx.aichallenge.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ru.izhxx.aichallenge.domain.model.ResponseFormat
import ru.izhxx.aichallenge.domain.model.llmsettings.LLMPromptSettings
import ru.izhxx.aichallenge.domain.repository.LLMPromptSettingsRepository

class LLMPromptSettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : LLMPromptSettingsRepository {

    override val settingsFlow = dataStore.data.map { preferences ->
        val defaultSettings = LLMPromptSettings()

        val responseFormatString = preferences[responseFormat] ?: "xml"
        val responseFormat = try {
            ResponseFormat.valueOf(responseFormatString.uppercase())
        } catch (e: Exception) {
            ResponseFormat.XML
        }

        LLMPromptSettings(
            temperature = preferences[temperature] ?: defaultSettings.temperature,
            responseFormat = responseFormat,
            systemPrompt = preferences[systemPromt] ?: defaultSettings.systemPrompt
        )
    }

    // Ключи для хранения настроек промта
    private val temperature = doublePreferencesKey("llm_prompt_temperature")
    private val responseFormat = stringPreferencesKey("llm_prompt_response_format")
    private val systemPromt = stringPreferencesKey("llm_prompt_system_prompt")

    /**
     * Получает настройки промпта LLM
     * @return настройки промпта или значения по умолчанию, если настройки не были сохранены
     */
    override suspend fun getSettings(): LLMPromptSettings {
        return settingsFlow.first()
    }

    /**
     * Сохраняет настройки промпта LLM
     * @param settings настройки промпта для сохранения
     */
    override suspend fun saveSettings(settings: LLMPromptSettings) {
        dataStore.edit { preferences ->
            preferences[temperature] = settings.temperature
            preferences[responseFormat] = settings.responseFormat.name.lowercase()
            preferences[systemPromt] = settings.systemPrompt
        }
    }

    /**
     * Очищает сохраненные настройки промпта (возвращает к значениям по умолчанию)
     */
    override suspend fun clearSettings() {
        dataStore.edit { preferences ->
            preferences.remove(temperature)
            preferences.remove(responseFormat)
            preferences.remove(systemPromt)
        }
    }
}