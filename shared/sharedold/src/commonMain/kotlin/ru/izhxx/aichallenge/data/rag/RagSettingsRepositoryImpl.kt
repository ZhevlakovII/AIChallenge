package ru.izhxx.aichallenge.data.rag

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import ru.izhxx.aichallenge.domain.rag.RagSettings
import ru.izhxx.aichallenge.domain.rag.RagSettingsRepository

/**
 * Реализация репозитория настроек RAG на базе DataStore Preferences.
 */
class RagSettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : RagSettingsRepository {

    override val settingsFlow: Flow<RagSettings> = dataStore.data.map { p ->
        RagSettings(
            enabled = p[KEY_ENABLED] ?: false,
            indexPath = p[KEY_INDEX_PATH],
            topK = p[KEY_TOP_K] ?: 4,
            minScore = p[KEY_MIN_SCORE] ?: 0.3,
            maxContextTokens = p[KEY_MAX_CTX_TOKENS] ?: 1024
        )
    }

    override suspend fun getSettings(): RagSettings = settingsFlow.first()

    override suspend fun saveSettings(settings: RagSettings) {
        dataStore.edit { p ->
            p[KEY_ENABLED] = settings.enabled
            if (settings.indexPath.isNullOrBlank()) {
                p.remove(KEY_INDEX_PATH)
            } else {
                p[KEY_INDEX_PATH] = settings.indexPath!!
            }
            p[KEY_TOP_K] = settings.topK
            p[KEY_MIN_SCORE] = settings.minScore
            p[KEY_MAX_CTX_TOKENS] = settings.maxContextTokens
        }
    }

    override suspend fun backToDefaultSettings() {
        dataStore.edit { p ->
            p[KEY_ENABLED] = false
            p.remove(KEY_INDEX_PATH)
            p[KEY_TOP_K] = 4
            p[KEY_MIN_SCORE] = 0.3
            p[KEY_MAX_CTX_TOKENS] = 1024
        }
    }

    private companion object {
        val KEY_ENABLED = booleanPreferencesKey("rag_enabled")
        val KEY_INDEX_PATH = stringPreferencesKey("rag_index_path")
        val KEY_TOP_K = intPreferencesKey("rag_top_k")
        val KEY_MIN_SCORE = doublePreferencesKey("rag_min_score")
        val KEY_MAX_CTX_TOKENS = intPreferencesKey("rag_max_context_tokens")
    }
}
