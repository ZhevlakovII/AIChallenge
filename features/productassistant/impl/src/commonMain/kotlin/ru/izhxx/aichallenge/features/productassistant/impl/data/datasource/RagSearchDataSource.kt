package ru.izhxx.aichallenge.features.productassistant.impl.data.datasource

import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.DocumentationItem

/**
 * Data source for RAG-based documentation search
 */
interface RagSearchDataSource {
    /**
     * Search documentation using RAG
     *
     * @param query Search query
     * @param maxResults Maximum number of results
     * @return Result containing list of relevant documentation items
     */
    suspend fun searchDocumentation(
        query: String,
        maxResults: Int
    ): Result<List<DocumentationItem>>
}
