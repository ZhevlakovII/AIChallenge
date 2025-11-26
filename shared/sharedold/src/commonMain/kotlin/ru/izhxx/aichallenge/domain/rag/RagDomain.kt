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
    val maxContextTokens: Int = 1024
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
