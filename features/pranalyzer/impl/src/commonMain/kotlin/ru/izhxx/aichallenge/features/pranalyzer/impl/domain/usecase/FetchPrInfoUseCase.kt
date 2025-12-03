package ru.izhxx.aichallenge.features.pranalyzer.impl.domain.usecase

import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.PullRequest

/**
 * Use case for fetching Pull Request information
 */
interface FetchPrInfoUseCase {
    /**
     * Fetches basic information about a Pull Request
     *
     * @param owner Repository owner (user or organization)
     * @param repo Repository name
     * @param prNumber Pull Request number
     * @return Result containing PullRequest data or error
     */
    suspend operator fun invoke(
        owner: String,
        repo: String,
        prNumber: Int
    ): Result<PullRequest>
}
