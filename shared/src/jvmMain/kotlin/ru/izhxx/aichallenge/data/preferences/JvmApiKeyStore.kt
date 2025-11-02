package ru.izhxx.aichallenge.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File
import java.nio.file.Paths

/**
 * JVM-реализация [ApiKeyStore] с использованием DataStore
 */
class JvmApiKeyStore : ApiKeyStore {
    
    private companion object {
        val API_KEY = stringPreferencesKey("api_key")
        private val USER_HOME = System.getProperty("user.home") ?: "."
        private val DATA_STORE_FILE = File(USER_HOME, ".aichallenge/preferences.preferences_pb")
        
        // Создаем директорию, если она не существует
        init {
            DATA_STORE_FILE.parentFile?.mkdirs()
        }
    }
    
    // Создание DataStore
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create {
        Paths.get(DATA_STORE_FILE.absolutePath).toFile()
    }
    
    override suspend fun saveApiKey(apiKey: String) {
        dataStore.edit { preferences ->
            preferences[API_KEY] = apiKey
        }
    }
    
    override suspend fun getApiKey(): String {
        return dataStore.data.map { preferences ->
            preferences[API_KEY].orEmpty()
        }.first()
    }
    
    override fun getApiKeyFlow(): Flow<String> {
        return dataStore.data.map { preferences ->
            preferences[API_KEY].orEmpty()
        }
    }
    
    override suspend fun hasApiKey(): Boolean {
        return getApiKey().isNotBlank()
    }
    
    override suspend fun clearApiKey() {
        dataStore.edit { preferences ->
            preferences.remove(API_KEY)
        }
    }
}
