package ru.izhxx.aichallenge.features.pranalyzer.impl.domain.usecase

import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.PrDiff

/**
 * Use case for fetching Pull Request diff
 */
interface FetchPrDiffUseCase {
    /**
     * Fetches the complete diff for a Pull Request
     *
     * @param owner Repository owner (user or organization)
     * @param repo Repository name
     * @param prNumber Pull Request number
     * @return Result containing PrDiff data or error
     */
    suspend operator fun invoke(
        owner: String,
        repo: String,
        prNumber: Int
    ): Result<PrDiff>
}
