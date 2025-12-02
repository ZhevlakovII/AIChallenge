@file:OptIn(ExperimentalTime::class)

package ru.izhxx.aichallenge.features.pranalyzer.impl.domain.usecase

import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.AnalysisReport
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.LlmAnalysis
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.PrDiff
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.PullRequest
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Implementation of GenerateReportUseCase
 */
class GenerateReportUseCaseImpl : GenerateReportUseCase {

    override suspend fun invoke(
        pullRequest: PullRequest,
        diff: PrDiff,
        llmAnalysis: LlmAnalysis
    ): Result<AnalysisReport> {
        return runCatching {
            // Validate that all data belongs to the same PR
            require(diff.prNumber == pullRequest.number) {
                "Diff PR number (${diff.prNumber}) does not match Pull Request number (${pullRequest.number})"
            }
            require(llmAnalysis.prNumber == pullRequest.number) {
                "Analysis PR number (${llmAnalysis.prNumber}) does not match Pull Request number (${pullRequest.number})"
            }

            // Create the analysis report
            AnalysisReport(
                pullRequest = pullRequest,
                diff = diff,
                llmAnalysis = llmAnalysis,
                generatedAt = Clock.System.now(),
                analysisVersion = "1.0.0"
            )
        }
    }
}
