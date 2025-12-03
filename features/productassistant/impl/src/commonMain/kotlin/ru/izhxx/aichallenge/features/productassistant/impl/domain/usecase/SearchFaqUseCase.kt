package ru.izhxx.aichallenge.features.productassistant.impl.domain.usecase

import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.DocumentationItem

/**
 * Use case for searching FAQ documentation
 */
interface SearchFaqUseCase {
    /**
     * Search for relevant FAQ items
     *
     * @param query Search query
     * @param maxResults Maximum number of results
     * @return Result containing list of relevant documentation items
     */
    suspend operator fun invoke(
        query: String,
        maxResults: Int = 5
    ): Result<List<DocumentationItem>>
}
