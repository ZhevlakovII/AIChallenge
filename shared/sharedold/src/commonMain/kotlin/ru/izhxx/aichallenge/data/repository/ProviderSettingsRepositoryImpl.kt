package ru.izhxx.aichallenge.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ru.izhxx.aichallenge.domain.model.config.ProviderSettings
import ru.izhxx.aichallenge.domain.repository.ProviderSettingsRepository

class ProviderSettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : ProviderSettingsRepository {

    override val settingsFlow = dataStore.data.map { preferences ->
        val defaultSettings = ProviderSettings.default()

        ProviderSettings(
            apiKey = preferences[apiKey].orEmpty(),
            apiUrl = preferences[apiUrl] ?: defaultSettings.apiUrl,
            model = preferences[model] ?: defaultSettings.model,
        )
    }

    // Ключи для хранения настроек провайдера
    private val apiKey = stringPreferencesKey("llm_provider_api_key")
    private val apiUrl = stringPreferencesKey("llm_provider_api_url")
    private val model = stringPreferencesKey("llm_provider_model")

    /**
     * Получает API ключ из хранилища
     * @return API ключ или пустую строку, если ключ не был сохранен
     */
    override suspend fun getApiKey(): String {
        return settingsFlow.first().apiKey
    }

    /**
     * Получает настройки провайдера LLM
     * @return настройки провайдера или значения по умолчанию, если настройки не были сохранены
     */
    override suspend fun getSettings(): ProviderSettings {
        return settingsFlow.first()
    }

    /**
     * Сохраняет настройки провайдера LLM
     * @param settings настройки провайдера для сохранения
     */
    override suspend fun saveSettings(settings: ProviderSettings) {
        dataStore.edit { preferences ->
            preferences[apiKey] = settings.apiKey
            preferences[apiUrl] = settings.apiUrl
            preferences[model] = settings.model
        }
    }

    /**
     * Очищает сохраненные настройки провайдера (возвращает к значениям по умолчанию)
     */
    override suspend fun backToDefaultSettings() {
        dataStore.edit { preferences ->
            preferences.remove(apiKey)
            preferences.remove(apiUrl)
            preferences.remove(model)
        }
    }
}