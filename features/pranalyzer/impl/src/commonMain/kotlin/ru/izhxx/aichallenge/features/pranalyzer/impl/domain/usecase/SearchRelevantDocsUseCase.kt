package ru.izhxx.aichallenge.features.pranalyzer.impl.domain.usecase

import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.DocumentationSearchResult
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.PrDiff
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.PullRequest

/**
 * Use case for searching relevant documentation based on PR content
 */
interface SearchRelevantDocsUseCase {
    /**
     * Searches for documentation relevant to the Pull Request
     *
     * @param pullRequest Pull Request information
     * @param diff PR diff containing changed files
     * @param maxResults Maximum number of documentation results to return
     * @return Result containing DocumentationSearchResult or error
     */
    suspend operator fun invoke(
        pullRequest: PullRequest,
        diff: PrDiff,
        maxResults: Int = 10
    ): Result<DocumentationSearchResult>
}
