package ru.izhxx.aichallenge.features.pranalyzer.impl.domain.usecase

import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.PullRequest
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.repository.PrAnalyzerRepository

/**
 * Implementation of FetchPrInfoUseCase
 */
class FetchPrInfoUseCaseImpl(
    private val repository: PrAnalyzerRepository
) : FetchPrInfoUseCase {

    override suspend fun invoke(
        owner: String,
        repo: String,
        prNumber: Int
    ): Result<PullRequest> {
        require(owner.isNotBlank()) { "Owner cannot be blank" }
        require(repo.isNotBlank()) { "Repository name cannot be blank" }
        require(prNumber > 0) { "PR number must be positive" }

        return repository.fetchPrInfo(owner, repo, prNumber)
    }
}
