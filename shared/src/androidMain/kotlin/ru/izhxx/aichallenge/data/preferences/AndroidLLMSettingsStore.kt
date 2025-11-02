package ru.izhxx.aichallenge.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ru.izhxx.aichallenge.domain.model.LLMUserSettings

/**
 * Android-реализация [LLMSettingsStore] с использованием DataStore
 * для хранения настроек LLM
 */
class AndroidLLMSettingsStore(private val context: Context) : LLMSettingsStore {

    private companion object {
        // Ключи для хранения настроек
        private val API_KEY = stringPreferencesKey("llm_api_key")
        private val API_URL = stringPreferencesKey("llm_api_url")
        private val MODEL = stringPreferencesKey("llm_model")
        private val TEMPERATURE = doublePreferencesKey("llm_temperature")
        private val OPENAI_PROJECT = stringPreferencesKey("llm_openai_project")

        // Extension property для создания DataStore
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
            name = "llm_settings_datastore"
        )
    }

    /**
     * Проверяет, настроен ли API ключ
     */
    override suspend fun hasApiKey(): Boolean {
        return getApiKey().isNotEmpty()
    }

    /**
     * Получает API ключ из хранилища
     */
    override suspend fun getApiKey(): String {
        return context.dataStore.data.map { preferences ->
            preferences[API_KEY].orEmpty()
        }.first()
    }

    /**
     * Проверяет, существуют ли сохраненные настройки LLM
     */
    override suspend fun hasSettings(): Boolean {
        // Считаем, что настройки существуют, если есть хотя бы один ключ
        val settings = getSettings()
        return settings.apiKey.isNotEmpty() || 
               settings.apiUrl.isNotEmpty() ||
               settings.model.isNotEmpty() ||
               settings.temperature in 0.0..1.0 ||
               settings.openaiProject.isNotEmpty()
    }

    /**
     * Получает настройки LLM
     */
    override suspend fun getSettings(): LLMUserSettings {
        return context.dataStore.data.map { preferences ->
            val defaultSettings = LLMUserSettings()
            
            LLMUserSettings(
                apiKey = preferences[API_KEY].orEmpty(),
                apiUrl = preferences[API_URL] ?: defaultSettings.apiUrl,
                model = preferences[MODEL] ?: defaultSettings.model,
                temperature = preferences[TEMPERATURE] ?: defaultSettings.temperature,
                openaiProject = preferences[OPENAI_PROJECT] ?: defaultSettings.openaiProject
            )
        }.first()
    }

    /**
     * Сохраняет настройки LLM
     */
    override suspend fun saveSettings(settings: LLMUserSettings) {
        context.dataStore.edit { preferences ->
            preferences[API_KEY] = settings.apiKey
            preferences[API_URL] = settings.apiUrl
            preferences[MODEL] = settings.model
            preferences[TEMPERATURE] = settings.temperature
            preferences[OPENAI_PROJECT] = settings.openaiProject
        }
    }

    /**
     * Сохраняет только API ключ
     */
    override suspend fun saveApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[API_KEY] = apiKey
        }
    }

    /**
     * Очищает сохраненные настройки LLM
     */
    override suspend fun clearSettings() {
        context.dataStore.edit { preferences ->
            preferences.remove(API_KEY)
            preferences.remove(API_URL)
            preferences.remove(MODEL)
            preferences.remove(TEMPERATURE)
            preferences.remove(OPENAI_PROJECT)
        }
    }

    /**
     * Получает поток настроек LLM для наблюдения за изменениями
     */
    fun getSettingsFlow(): Flow<LLMUserSettings> {
        return context.dataStore.data.map { preferences ->
            val defaultSettings = LLMUserSettings()
            
            LLMUserSettings(
                apiKey = preferences[API_KEY].orEmpty(),
                apiUrl = preferences[API_URL] ?: defaultSettings.apiUrl,
                model = preferences[MODEL] ?: defaultSettings.model,
                temperature = preferences[TEMPERATURE] ?: defaultSettings.temperature,
                openaiProject = preferences[OPENAI_PROJECT] ?: defaultSettings.openaiProject
            )
        }
    }
}
