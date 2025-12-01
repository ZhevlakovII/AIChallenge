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
import ru.izhxx.aichallenge.domain.rag.RerankSettings
import ru.izhxx.aichallenge.domain.rag.RerankMode
import ru.izhxx.aichallenge.domain.rag.CutoffMode

/**
 * Реализация репозитория настроек RAG на базе DataStore Preferences.
 */
class RagSettingsRepositoryImpl(
    private val dataStore: DataStore<Preferences>
) : RagSettingsRepository {

    override val settingsFlow: Flow<RagSettings> = dataStore.data.map { p ->
        val mode = p[KEY_RERANK_MODE]?.let { runCatching { RerankMode.valueOf(it) }.getOrNull() } ?: RerankMode.None
        val cutoff = p[KEY_RERANK_CUTOFF_MODE]?.let { runCatching { CutoffMode.valueOf(it) }.getOrNull() } ?: CutoffMode.Quantile
        val rerank = RerankSettings(
            mode = mode,
            candidateK = p[KEY_RERANK_CANDIDATE_K] ?: 16,
            mmrLambda = p[KEY_RERANK_MMR_LAMBDA] ?: 0.5,
            cutoffMode = cutoff,
            minRerankScore = p[KEY_RERANK_MIN_SCORE],
            quantileQ = p[KEY_RERANK_QUANTILE_Q] ?: 0.2,
            zScore = p[KEY_RERANK_Z_SCORE] ?: -0.5
        )
        RagSettings(
            enabled = p[KEY_ENABLED] ?: false,
            indexPath = p[KEY_INDEX_PATH],
            topK = p[KEY_TOP_K] ?: 4,
            minScore = p[KEY_MIN_SCORE] ?: 0.3,
            maxContextTokens = p[KEY_MAX_CTX_TOKENS] ?: 1024,
            rerank = rerank
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

            // Реранк настройки
            p[KEY_RERANK_MODE] = settings.rerank.mode.name
            p[KEY_RERANK_CANDIDATE_K] = settings.rerank.candidateK
            p[KEY_RERANK_MMR_LAMBDA] = settings.rerank.mmrLambda
            p[KEY_RERANK_CUTOFF_MODE] = settings.rerank.cutoffMode.name
            val minScore = settings.rerank.minRerankScore
            if (minScore == null) {
                p.remove(KEY_RERANK_MIN_SCORE)
            } else {
                p[KEY_RERANK_MIN_SCORE] = minScore
            }
            p[KEY_RERANK_QUANTILE_Q] = settings.rerank.quantileQ
            p[KEY_RERANK_Z_SCORE] = settings.rerank.zScore
        }
    }

    override suspend fun backToDefaultSettings() {
        dataStore.edit { p ->
            p[KEY_ENABLED] = false
            p.remove(KEY_INDEX_PATH)
            p[KEY_TOP_K] = 4
            p[KEY_MIN_SCORE] = 0.3
            p[KEY_MAX_CTX_TOKENS] = 1024

            // Реранк дефолты
            p[KEY_RERANK_MODE] = RerankMode.None.name
            p[KEY_RERANK_CANDIDATE_K] = 16
            p[KEY_RERANK_MMR_LAMBDA] = 0.5
            p[KEY_RERANK_CUTOFF_MODE] = CutoffMode.Quantile.name
            p.remove(KEY_RERANK_MIN_SCORE)
            p[KEY_RERANK_QUANTILE_Q] = 0.2
            p[KEY_RERANK_Z_SCORE] = -0.5
        }
    }

    private companion object {
        val KEY_ENABLED = booleanPreferencesKey("rag_enabled")
        val KEY_INDEX_PATH = stringPreferencesKey("rag_index_path")
        val KEY_TOP_K = intPreferencesKey("rag_top_k")
        val KEY_MIN_SCORE = doublePreferencesKey("rag_min_score")
        val KEY_MAX_CTX_TOKENS = intPreferencesKey("rag_max_context_tokens")

        // Реранк настройки
        val KEY_RERANK_MODE = stringPreferencesKey("rag_rerank_mode")
        val KEY_RERANK_CANDIDATE_K = intPreferencesKey("rag_rerank_candidate_k")
        val KEY_RERANK_MMR_LAMBDA = doublePreferencesKey("rag_rerank_mmr_lambda")
        val KEY_RERANK_CUTOFF_MODE = stringPreferencesKey("rag_rerank_cutoff_mode")
        val KEY_RERANK_MIN_SCORE = doublePreferencesKey("rag_rerank_min_score")
        val KEY_RERANK_QUANTILE_Q = doublePreferencesKey("rag_rerank_quantile_q")
        val KEY_RERANK_Z_SCORE = doublePreferencesKey("rag_rerank_z_score")
    }
}
