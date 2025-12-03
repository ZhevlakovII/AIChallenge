package ru.izhxx.aichallenge.features.productassistant.impl.domain.usecase

import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.DocumentationItem
import ru.izhxx.aichallenge.features.productassistant.impl.domain.repository.ProductAssistantRepository

/**
 * Implementation of SearchFaqUseCase
 */
class SearchFaqUseCaseImpl(
    private val repository: ProductAssistantRepository
) : SearchFaqUseCase {

    override suspend fun invoke(
        query: String,
        maxResults: Int
    ): Result<List<DocumentationItem>> {
        if (query.isBlank()) {
            return Result.failure(IllegalArgumentException("Query cannot be empty"))
        }

        if (maxResults <= 0) {
            return Result.failure(IllegalArgumentException("maxResults must be positive"))
        }

        return repository.searchFaq(query, maxResults)
    }
}
