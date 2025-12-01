package ru.izhxx.aichallenge.features.rag.domain

import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.izhxx.aichallenge.rag.docindexer.core.api.Embedder
import ru.izhxx.aichallenge.rag.docindexer.core.impl.CharOverlapChunker
import ru.izhxx.aichallenge.rag.docindexer.core.model.BuildParams
import ru.izhxx.aichallenge.rag.docindexer.core.model.BuildRequest
import ru.izhxx.aichallenge.rag.docindexer.core.model.DocumentIndex
import ru.izhxx.aichallenge.rag.docindexer.core.model.ModelConfig
import ru.izhxx.aichallenge.rag.docindexer.core.pipeline.IndexBuilder
import ru.izhxx.aichallenge.rag.docindexer.fs.FsContentReaderJvm
import ru.izhxx.aichallenge.rag.docindexer.fs.JsonIndexWriter
import ru.izhxx.aichallenge.rag.docindexer.fs.Sha256Hasher
import ru.izhxx.aichallenge.rag.docindexer.ollama.OllamaEmbedder
import java.nio.file.Paths

/**
 * JVM-реализация индексации документов для RAG.
 * Использует модули из rag/doc-indexer.
 */
class IndexRagDocumentsUseCaseImpl(
    private val httpClient: HttpClient
) : IndexRagDocumentsUseCase {

    override suspend fun indexDocuments(
        inputDir: String,
        outputPath: String,
        ollamaUrl: String,
        model: String,
        targetTokens: Int,
        overlapTokens: Int,
        charsPerToken: Double,
        concurrency: Int,
        onProgress: ((currentFile: String, progress: Float) -> Unit)?
    ): Result<DocumentIndex> = runCatching {
        withContext(Dispatchers.Default) {
            // Проверяем что inputDir существует
            val inputPath = Paths.get(inputDir)
            if (!inputPath.toFile().exists() || !inputPath.toFile().isDirectory) {
                throw IllegalArgumentException("Input directory does not exist or is not a directory: $inputDir")
            }

            // Вычисляем maxChars и overlapChars
            val maxChars = (targetTokens * charsPerToken).toInt()
            val overlapChars = (overlapTokens * charsPerToken).toInt()

            // Создаем компоненты индексации
            val reader = FsContentReaderJvm()
            val chunker = CharOverlapChunker()
            val embedder: Embedder = OllamaEmbedder(
                http = httpClient,
                baseUrl = ollamaUrl,
                model = model,
                retries = 3,
                initialBackoffMs = 250
            )
            val hasher = Sha256Hasher()

            // Создаем билдер индекса
            val indexBuilder = IndexBuilder(
                reader = reader,
                chunker = chunker,
                embedder = embedder,
                hasher = hasher
            )

            // Формируем запрос на индексацию
            val request = BuildRequest(
                inputDir = inputDir,
                params = BuildParams(
                    targetTokens = targetTokens,
                    overlapTokens = overlapTokens,
                    charsPerToken = charsPerToken,
                    maxChars = maxChars,
                    overlapChars = overlapChars,
                    concurrency = concurrency
                ),
                model = ModelConfig(
                    provider = "ollama",
                    name = model,
                    endpoint = ollamaUrl,
                    dim = null
                )
            )

            // TODO: Добавить прогресс через onProgress
            // Сейчас IndexBuilder не поддерживает коллбэки прогресса

            // Запускаем индексацию
            val index = indexBuilder.build(request)

            // Сохраняем индекс в JSON
            val writer = JsonIndexWriter(pretty = true)
            writer.write(index, outputPath)

            index
        }
    }
}
