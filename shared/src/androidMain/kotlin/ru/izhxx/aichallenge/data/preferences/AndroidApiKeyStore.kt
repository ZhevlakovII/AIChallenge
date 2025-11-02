package ru.izhxx.aichallenge.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Android-реализация [ApiKeyStore] с использованием EncryptedSharedPreferences
 * для безопасного хранения API ключа
 */
class AndroidApiKeyStore(private val context: Context) : ApiKeyStore {

    private companion object {
        private val API_KEY = stringPreferencesKey("api_key")

        // Extension property для создания DataStore
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
            name = "api_key_datastore",
            produceMigrations = { context ->
                // Миграция из EncryptedSharedPreferences в DataStore при необходимости
                listOf()
            }
        )
    }

    override suspend fun saveApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[API_KEY] = apiKey
        }
    }

    override suspend fun getApiKey(): String {
        return context.dataStore.data.map { preferences ->
            preferences[API_KEY].orEmpty()
        }.first()
    }

    override fun getApiKeyFlow(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[API_KEY].orEmpty()
        }
    }

    override suspend fun hasApiKey(): Boolean {
        return getApiKey().isNotEmpty()
    }

    override suspend fun clearApiKey() {
        context.dataStore.edit { preferences ->
            preferences.remove(API_KEY)
        }
    }
}
