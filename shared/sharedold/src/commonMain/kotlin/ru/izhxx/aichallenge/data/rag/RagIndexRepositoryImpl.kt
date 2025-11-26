package ru.izhxx.aichallenge.data.rag

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import ru.izhxx.aichallenge.domain.rag.RagIndexRepository
import ru.izhxx.aichallenge.rag.docindexer.core.model.DocumentIndex

/**
 * Реализация репозитория индекса документов.
 * Читает JSON-файл индекса (выход doc-indexer), кэширует его в памяти и валидирует размерности.
 */
class RagIndexRepositoryImpl(
    private val json: Json
) : RagIndexRepository {

    private val current = MutableStateFlow<DocumentIndex?>(null)
    override val currentIndexFlow: Flow<DocumentIndex?> = current.asStateFlow()

    override suspend fun getCurrentIndex(): DocumentIndex? = current.value

    override suspend fun loadIndex(path: String): Result<DocumentIndex> = runCatching {
        val text = readFileTextCompat(path) ?: error("Не удалось прочитать индекс по пути: $path")
        val idx = json.decodeFromString<DocumentIndex>(text)

        // Валидация размерности эмбеддингов
        val someChunk = idx.documents.asSequence().flatMap { it.chunks.asSequence() }.firstOrNull()
            ?: error("Индекс пуст (нет чанков)")
        val firstDim = someChunk.embedding.size
        val modelDim = idx.model.dim
        if (modelDim != null && modelDim != firstDim) {
            error("Несовпадение размерности эмбеддингов: index.model.dim=$modelDim, фактический=$firstDim")
        }
        // Проверим, что у всех чанков одинаковая длина вектора
        val bad = idx.documents.any { d -> d.chunks.any { it.embedding.size != firstDim } }
        if (bad) error("В индексе обнаружены чанки с различной длиной эмбеддингов")

        current.value = idx
        idx
    }
}
