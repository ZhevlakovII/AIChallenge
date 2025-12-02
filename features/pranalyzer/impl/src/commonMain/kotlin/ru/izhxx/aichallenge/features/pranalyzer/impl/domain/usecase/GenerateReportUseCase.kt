package ru.izhxx.aichallenge.features.pranalyzer.impl.domain.usecase

import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.AnalysisReport
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.LlmAnalysis
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.PrDiff
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.PullRequest

/**
 * Use case for generating the final analysis report
 */
interface GenerateReportUseCase {
    /**
     * Generates a complete analysis report from all gathered data
     *
     * @param pullRequest Pull Request information
     * @param diff PR diff data
     * @param llmAnalysis LLM analysis results
     * @return Result containing AnalysisReport or error
     */
    suspend operator fun invoke(
        pullRequest: PullRequest,
        diff: PrDiff,
        llmAnalysis: LlmAnalysis
    ): Result<AnalysisReport>
}
