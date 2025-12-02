package ru.izhxx.aichallenge.features.settings.state

import ru.izhxx.aichallenge.domain.model.config.ResponseFormat
import ru.izhxx.aichallenge.domain.rag.CutoffMode
import ru.izhxx.aichallenge.domain.rag.RerankMode

/**
 * Состояние для экрана настроек
 */
data class SettingsState(
    // Настройки провайдера
    val apiKey: String = "",
    val apiUrl: String = "",
    val model: String = "",

    // Настройки промпта
    val temperature: String = "0.7",
    val maxTokens: String = "4096",
    val responseFormat: ResponseFormat = ResponseFormat.MARKDOWN,
    val systemPrompt: String = "",

    // Расширенные настройки генерации
    val topK: String = "40",
    val topP: String = "0.95",
    val minP: String = "0.05",
    val topA: String = "0.0",
    val seed: String = "0",

    // Фича-флаг: включить function calling (MCP tools)
    val enableMcpToolCalling: Boolean = false,

    // ===== RAG настройки =====
    val ragEnabled: Boolean = false,
    val ragIndexPath: String = "",
    val ragTopK: String = "4",
    val ragMinScore: String = "0.3",
    val ragMaxContextTokens: String = "1024",

    // ===== RAG rerank (2-й этап) =====
    val ragRerankMode: RerankMode = RerankMode.None,
    val ragCandidateK: String = "16",
    val ragMmrLambda: String = "0.5",
    val ragCutoffMode: CutoffMode = CutoffMode.Quantile,
    // для Static
    val ragMinRerankScore: String = "",
    // для Quantile
    val ragQuantileQ: String = "0.2",
    // для ZScore
    val ragZScore: String = "-0.5",

    // ===== RAG Indexing =====
    val docsDirectory: String = "",
    val isIndexing: Boolean = false,
    val indexingProgress: String? = null,
    val indexingSuccess: String? = null,
    val indexingError: String? = null,

    // Состояние UI
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val error: String? = null
)
