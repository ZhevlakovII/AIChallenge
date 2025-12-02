package ru.izhxx.aichallenge.features.pranalyzer.impl.domain.usecase

import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.PrDiff
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.repository.PrAnalyzerRepository

/**
 * Implementation of FetchPrDiffUseCase
 */
class FetchPrDiffUseCaseImpl(
    private val repository: PrAnalyzerRepository
) : FetchPrDiffUseCase {

    override suspend fun invoke(
        owner: String,
        repo: String,
        prNumber: Int
    ): Result<PrDiff> {
        require(owner.isNotBlank()) { "Owner cannot be blank" }
        require(repo.isNotBlank()) { "Repository name cannot be blank" }
        require(prNumber > 0) { "PR number must be positive" }

        return repository.fetchPrDiff(owner, repo, prNumber)
    }
}
