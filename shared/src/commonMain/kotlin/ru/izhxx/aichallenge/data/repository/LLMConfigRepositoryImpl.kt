package ru.izhxx.aichallenge.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ru.izhxx.aichallenge.domain.model.config.LLMConfig
import ru.izhxx.aichallenge.domain.model.config.ResponseFormat
import ru.izhxx.aichallenge.domain.repository.LLMConfigRepository

class LLMConfigRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : LLMConfigRepository {

    override val settingsFlow = dataStore.data.map { preferences ->
        val defaultSettings = LLMConfig.default()

        val responseFormat = ResponseFormat.getFormat(preferences[responseFormat])

        LLMConfig(
            temperature = preferences[temperature] ?: defaultSettings.temperature,
            responseFormat = responseFormat,
            systemPrompt = preferences[systemPromt] ?: defaultSettings.systemPrompt,
            maxTokens = preferences[maxTokens] ?: defaultSettings.maxTokens,
            topK = preferences[topK] ?: defaultSettings.topK,
            topP = preferences[topP] ?: defaultSettings.topP,
            minP = preferences[minP] ?: defaultSettings.minP,
            topA = preferences[topA] ?: defaultSettings.topA,
            seed = preferences[seed] ?: defaultSettings.seed,
        )
    }

    // Ключи для хранения настроек промта
    private val temperature = doublePreferencesKey("llm_prompt_temperature")
    private val responseFormat = stringPreferencesKey("llm_prompt_response_format")
    private val systemPromt = stringPreferencesKey("llm_prompt_system_prompt")
    private val maxTokens = intPreferencesKey("llm_promt_maxtokens")
    private val topK = intPreferencesKey("llm_promt_topk")
    private val topP = doublePreferencesKey("llm_promt_topp")
    private val minP = doublePreferencesKey("llm_promt_minp")
    private val topA = doublePreferencesKey("llm_promt_topa")
    private val seed = longPreferencesKey("llm_promt_seed")

    /**
     * Получает настройки промпта LLM
     * @return настройки промпта или значения по умолчанию, если настройки не были сохранены
     */
    override suspend fun getSettings(): LLMConfig {
        return settingsFlow.first()
    }

    /**
     * Сохраняет настройки промпта LLM
     * @param config настройки промпта для сохранения
     */
    override suspend fun saveSettings(config: LLMConfig) {
        dataStore.edit { preferences ->
            preferences[temperature] = config.temperature
            preferences[responseFormat] = config.responseFormat.name.lowercase()
            preferences[systemPromt] = config.systemPrompt
            preferences[maxTokens] = config.maxTokens
            preferences[topK] = config.topK
            preferences[topP] = config.topP
            preferences[minP] = config.minP
            preferences[topA] = config.topA
            preferences[seed] = config.seed
        }
    }

    /**
     * Очищает сохраненные настройки промпта (возвращает к значениям по умолчанию)
     */
    override suspend fun backToDefaultSettings() {
        dataStore.edit { preferences ->
            preferences.remove(temperature)
            preferences.remove(responseFormat)
            preferences.remove(systemPromt)
            preferences.remove(maxTokens)
            preferences.remove(topK)
            preferences.remove(topP)
            preferences.remove(minP)
            preferences.remove(topA)
            preferences.remove(seed)
        }
    }
}
