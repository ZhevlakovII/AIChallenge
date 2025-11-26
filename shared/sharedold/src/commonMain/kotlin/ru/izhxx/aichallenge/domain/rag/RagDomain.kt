package ru.izhxx.aichallenge.domain.rag

import kotlinx.coroutines.flow.Flow
import ru.izhxx.aichallenge.rag.docindexer.core.model.DocumentIndex

/**
 * Режим работы чата (для будущего расширения COMPARE).
 * В итерации 1 используется только BASELINE/RAG через RagSettings.enabled.
 */
enum class ChatMode {
    BASELINE,
    RAG,
    COMPARE
}

/**
 * Режим второго этапа (после первичного kNN).
 */
enum class RerankMode {
    None,
    MMR,
    LLM // локальный LLM-as-reranker через Ollama (опционально)
}

/**
 * Режим отсечения нерелевантных результатов после второго этапа.
 */
enum class CutoffMode {
    Static,   // статический порог (minRerankScore), применим прежде всего для LLM/cross-encoder
    Quantile, // отсечение нижней квантили по score (напр. q=0.2)
    ZScore    // нормализация и отрезание по z >= threshold
}

/**
 * Настройки второго этапа ранжирования/фильтрации.
 */
data class RerankSettings(
    val mode: RerankMode = RerankMode.None,
    val candidateK: Int = 16,           // число кандидатов с 1-го этапа для rerank
    val mmrLambda: Double = 0.5,        // баланс релевантность/диверсификация для MMR
    val cutoffMode: CutoffMode = CutoffMode.Quantile,
    val minRerankScore: Double? = null, // для Static (актуально для LLM-score); для MMR можно не использовать
    val quantileQ: Double = 0.2,        // для Quantile: отбросить нижние q
    val zScore: Double = -0.5           // для ZScore: отрезать по z >= zScore
)

/**
 * Настройки RAG.
 * - enabled: включает/выключает RAG-пайплайн
 * - indexPath: путь к JSON-индексу, созданному doc-indexer'ом
 * - topK: количество ближайших чанков
 * - minScore: порог косинусного сходства
 * - maxContextTokens: лимит токенов на контекст (объединённые чанки)
 */
data class RagSettings(
    val enabled: Boolean = false,
    val indexPath: String? = null,
    val topK: Int = 4,
    val minScore: Double = 0.3,
    val maxContextTokens: Int = 1024,
    val rerank: RerankSettings = RerankSettings()
)

/**
 * Результат ретривала одного чанка.
 */
data class RetrievedChunk(
    val docId: String,
    val path: String,
    val chunkIndex: Int,
    val score: Double,
    val text: String
)

/**
 * Репозиторий настроек RAG.
 */
interface RagSettingsRepository {
    val settingsFlow: Flow<RagSettings>
    suspend fun getSettings(): RagSettings
    suspend fun saveSettings(settings: RagSettings)
    suspend fun backToDefaultSettings()
}

/**
 * Репозиторий индекса документов (в памяти).
 */
interface RagIndexRepository {
    /** Текущий загруженный индекс (или null, если не загружен). */
    val currentIndexFlow: Flow<DocumentIndex?>

    /** Возвращает текущее значение индекса. */
    suspend fun getCurrentIndex(): DocumentIndex?

    /**
     * Загружает и кэширует индекс из указанного пути (JSON).
     * Возвращает Result с объектом индекса либо ошибку.
     */
    suspend fun loadIndex(path: String): Result<DocumentIndex>
}

/**
 * Интерфейс рантайм-эмбеддера для текста вопроса.
 * Обертка над конкретной реализацией (например, OllamaEmbedder).
 */
interface RagEmbedder {
    suspend fun embed(text: String): List<Double>
}

/**
 * Ретривер, который ищет релевантные чанки и собирает контекст.
 */
interface RagRetriever {
    /**
     * Ищет topK чанков по косинусной близости с порогом minScore.
     */
    suspend fun retrieve(
        questionEmbedding: List<Double>,
        index: DocumentIndex,
        topK: Int,
        minScore: Double
    ): List<RetrievedChunk>

    /**
     * Формирует текстовый контекст с учетом лимита токенов.
     * Использует параметры индекса (charsPerToken) для грубой оценки бюджета.
     */
    fun buildContext(
        chunks: List<RetrievedChunk>,
        index: DocumentIndex,
        maxTokens: Int
    ): String
}

/**
 * Второй этап: интерфейс реранкера.
 */
interface RagReranker {
    suspend fun rerank(
        questionEmbedding: List<Double>,
        candidates: List<RetrievedChunk>,
        index: DocumentIndex,
        settings: RerankSettings
    ): List<RetrievedChunk>
}
