package ru.izhxx.aichallenge.features.pranalyzer.impl.domain.usecase

import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.DocumentationSearchResult
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.LlmAnalysis
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.PrDiff
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.PullRequest

/**
 * Use case for analyzing PR with LLM
 */
interface AnalyzePrWithLlmUseCase {
    /**
     * Analyzes the Pull Request using LLM
     *
     * @param pullRequest Pull Request information
     * @param diff Complete diff of the PR
     * @param documentation Relevant documentation references
     * @return Result containing LlmAnalysis or error
     */
    suspend operator fun invoke(
        pullRequest: PullRequest,
        diff: PrDiff,
        documentation: DocumentationSearchResult
    ): Result<LlmAnalysis>
}
