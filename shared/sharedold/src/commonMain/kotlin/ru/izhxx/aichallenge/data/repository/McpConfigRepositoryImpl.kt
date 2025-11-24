package ru.izhxx.aichallenge.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ru.izhxx.aichallenge.di.DataStoreProvider
import ru.izhxx.aichallenge.domain.repository.McpConfigRepository

/**
 * Реализация репозитория конфигурации MCP на базе DataStore (Preferences).
 *
 * Хранит:
 * - wsUrl: адрес WebSocket MCP-сервера
 * - connected: флаг успешной проверки подключения
 */
class McpConfigRepositoryImpl : McpConfigRepository {

    private val dataStore: DataStore<Preferences> =
        DataStoreProvider.providePreferencesDataStore("mcp_config.preferences_pb")

    private val KEY_WS_URL = stringPreferencesKey("mcp_ws_url")
    private val KEY_CONNECTED = booleanPreferencesKey("mcp_connected")

    override suspend fun getWsUrl(): String? {
        return dataStore.data.map { it[KEY_WS_URL] }.first()
    }

    override suspend fun setWsUrl(url: String) {
        dataStore.edit { prefs ->
            prefs[KEY_WS_URL] = url
        }
    }

    override suspend fun isConnected(): Boolean {
        return dataStore.data.map { it[KEY_CONNECTED] ?: false }.first()
    }

    override suspend fun setConnected(connected: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_CONNECTED] = connected
        }
    }
}
