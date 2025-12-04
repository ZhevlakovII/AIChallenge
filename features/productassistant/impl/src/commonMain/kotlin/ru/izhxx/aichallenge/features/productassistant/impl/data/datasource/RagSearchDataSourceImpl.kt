package ru.izhxx.aichallenge.features.productassistant.impl.data.datasource

import ru.izhxx.aichallenge.data.rag.RagSearchPipeline
import ru.izhxx.aichallenge.domain.rag.RagEmbedder
import ru.izhxx.aichallenge.domain.rag.RagIndexRepository
import ru.izhxx.aichallenge.domain.rag.RagRetriever
import ru.izhxx.aichallenge.domain.rag.RagSettingsRepository
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.DocumentationCategory
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.DocumentationItem

/**
 * Implementation of RagSearchDataSource using RAG pipeline
 */
class RagSearchDataSourceImpl(
    private val ragSettingsRepository: RagSettingsRepository,
    private val ragIndexRepository: RagIndexRepository,
    private val ragEmbedder: RagEmbedder,
    private val ragRetriever: RagRetriever
) : RagSearchDataSource {

    override suspend fun searchDocumentation(
        query: String,
        maxResults: Int
    ): Result<List<DocumentationItem>> {
        return runCatching {
            val ragSettings = ragSettingsRepository.getSettings()

            // Check if RAG is enabled
            if (!ragSettings.enabled) {
                return Result.success(emptyList())
            }

            // Load or get current index
            val index = ragIndexRepository.getCurrentIndex() ?: run {
                val idxPath = ragSettings.indexPath
                if (idxPath.isNullOrBlank()) {
                    return Result.success(emptyList())
                }
                ragIndexRepository.loadIndex(idxPath).getOrElse {
                    return Result.success(emptyList())
                }
            }

            // Create RAG search pipeline
            val pipeline = RagSearchPipeline(
                embedder = ragEmbedder,
                retriever = ragRetriever
            )

            // Retrieve relevant chunks
            val chunks = pipeline.retrieveChunks(
                questionText = query,
                index = index,
                settings = ragSettings
            )

            // Convert chunks to DocumentationItem
            chunks.take(maxResults).map { chunk ->
                DocumentationItem(
                    question = extractQuestion(chunk.text),
                    answer = chunk.text,
                    category = DocumentationCategory.GENERAL,
                    keywords = extractKeywords(chunk.text),
                    relevanceScore = chunk.score
                )
            }
        }
    }

    private fun extractQuestion(text: String): String {
        // Try to extract question from text
        val lines = text.lines()
        val questionLine = lines.firstOrNull { it.contains("?") || it.startsWith("#") }
        return questionLine?.trim()?.removePrefix("#")?.trim() ?: text.take(100)
    }

    private fun extractKeywords(text: String): List<String> {
        // Simple keyword extraction
        val words = text.lowercase()
            .split(Regex("\\s+"))
            .filter { it.length > 3 }
            .distinct()
        return words.take(10)
    }
}
