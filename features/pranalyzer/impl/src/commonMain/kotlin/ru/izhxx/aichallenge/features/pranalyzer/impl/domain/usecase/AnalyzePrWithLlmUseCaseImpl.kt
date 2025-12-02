package ru.izhxx.aichallenge.features.pranalyzer.impl.domain.usecase

import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.DocumentationSearchResult
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.LlmAnalysis
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.PrDiff
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.PullRequest
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.repository.PrAnalyzerRepository

/**
 * Implementation of AnalyzePrWithLlmUseCase
 */
class AnalyzePrWithLlmUseCaseImpl(
    private val repository: PrAnalyzerRepository
) : AnalyzePrWithLlmUseCase {

    override suspend fun invoke(
        pullRequest: PullRequest,
        diff: PrDiff,
        documentation: DocumentationSearchResult
    ): Result<LlmAnalysis> {
        // Validate that diff belongs to the correct PR
        require(diff.prNumber == pullRequest.number) {
            "Diff PR number (${diff.prNumber}) does not match Pull Request number (${pullRequest.number})"
        }

        return repository.analyzePrWithLlm(pullRequest, diff, documentation)
    }
}
