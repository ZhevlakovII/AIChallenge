package ru.izhxx.aichallenge.features.rag.domain

import ru.izhxx.aichallenge.rag.docindexer.core.model.DocumentIndex

/**
 * Use case для индексации документов в RAG систему.
 * Сканирует директорию с документацией, создает embeddings и сохраняет индекс.
 */
interface IndexRagDocumentsUseCase {
    /**
     * Индексирует документы из указанной директории.
     *
     * @param inputDir путь к директории с документацией (.md файлы)
     * @param outputPath путь для сохранения JSON индекса
     * @param ollamaUrl URL Ollama сервера (по умолчанию http://localhost:11434)
     * @param model название модели для embeddings (по умолчанию mxbai-embed-large)
     * @param targetTokens желаемый размер чанка в токенах (по умолчанию 400)
     * @param overlapTokens перекрытие чанков в токенах (по умолчанию 80)
     * @param charsPerToken соотношение символов/токен (по умолчанию 3.0 для русского)
     * @param concurrency количество параллельных запросов к Ollama (по умолчанию 4)
     * @param onProgress коллбэк для отображения прогресса (текущий файл, прогресс)
     *
     * @return Result с созданным индексом или ошибкой
     */
    suspend fun indexDocuments(
        inputDir: String,
        outputPath: String,
        ollamaUrl: String = "http://localhost:11434",
        model: String = "mxbai-embed-large",
        targetTokens: Int = 400,
        overlapTokens: Int = 80,
        charsPerToken: Double = 3.0,
        concurrency: Int = 8,
        onProgress: ((currentFile: String, progress: Float) -> Unit)? = null
    ): Result<DocumentIndex>
}
