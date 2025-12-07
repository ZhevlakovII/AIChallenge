package ru.izhxx.aichallenge.features.pranalyzer.impl.presentation

import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.usecase.AnalyzePrWithLlmUseCase
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.usecase.FetchPrDiffUseCase
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.usecase.FetchPrInfoUseCase
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.usecase.GenerateReportUseCase
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.usecase.SearchRelevantDocsUseCase
import ru.izhxx.aichallenge.features.pranalyzer.impl.presentation.mapper.PrAnalysisUiMapper
import ru.izhxx.aichallenge.features.pranalyzer.impl.presentation.model.AnalysisStage
import ru.izhxx.aichallenge.features.pranalyzer.impl.presentation.model.PrAnalyzerEffect
import ru.izhxx.aichallenge.features.pranalyzer.impl.presentation.model.PrAnalyzerIntent
import ru.izhxx.aichallenge.features.pranalyzer.impl.presentation.model.PrAnalyzerResult
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * MVI Executor for PR Analyzer
 * Handles all side effects and business logic execution
 */
@ExperimentalTime
class PrAnalyzerExecutor(
    private val fetchPrInfoUseCase: FetchPrInfoUseCase,
    private val fetchPrDiffUseCase: FetchPrDiffUseCase,
    private val searchRelevantDocsUseCase: SearchRelevantDocsUseCase,
    private val analyzePrWithLlmUseCase: AnalyzePrWithLlmUseCase,
    private val generateReportUseCase: GenerateReportUseCase,
    private val mapper: PrAnalysisUiMapper
) {

    // Cache for current PR URL parts
    private var currentOwner: String? = null
    private var currentRepo: String? = null
    private var currentPrNumber: Int? = null

    suspend fun execute(
        intent: PrAnalyzerIntent,
        emitResult: suspend (PrAnalyzerResult) -> Unit,
        emitEffect: suspend (PrAnalyzerEffect) -> Unit
    ) {
        when (intent) {
            is PrAnalyzerIntent.PrUrlChanged -> handlePrUrlChanged(intent.url, emitResult)
            is PrAnalyzerIntent.StartAnalysis -> handleStartAnalysis(emitResult, emitEffect)
            is PrAnalyzerIntent.RetryAnalysis -> handleRetryAnalysis(emitResult, emitEffect)
            is PrAnalyzerIntent.ExpandIssue -> handleExpandIssue(intent.issueId, emitResult)
            is PrAnalyzerIntent.OpenDocumentation -> handleOpenDocumentation(intent.docUrl, emitEffect)
            is PrAnalyzerIntent.ExportReport -> handleExportReport(emitEffect)
        }
    }

    /**
     * Handles PR URL input change with validation
     */
    private suspend fun handlePrUrlChanged(
        url: String,
        emitResult: suspend (PrAnalyzerResult) -> Unit
    ) {
        val isValid = validatePrUrl(url)

        // Parse URL if valid
        if (isValid) {
            val urlParts = parsePrUrl(url)
            currentOwner = urlParts?.owner
            currentRepo = urlParts?.repo
            currentPrNumber = urlParts?.prNumber
        } else {
            currentOwner = null
            currentRepo = null
            currentPrNumber = null
        }

        emitResult(PrAnalyzerResult.UrlValidated(url = url, isValid = isValid))
    }

    /**
     * Handles the complete analysis pipeline
     * Executes all 5 stages with progress tracking
     */
    private suspend fun handleStartAnalysis(
        emitResult: suspend (PrAnalyzerResult) -> Unit,
        emitEffect: suspend (PrAnalyzerEffect) -> Unit
    ) {
        // Validate we have URL parts
        val owner = currentOwner ?: return
        val repo = currentRepo ?: return
        val prNumber = currentPrNumber ?: return

        try {
            // Clear previous state
            emitResult(PrAnalyzerResult.AnalysisStarted)

            // Stage 1: Fetch PR Info (0-20%)
            emitResult(PrAnalyzerResult.ProgressUpdated(AnalysisStage.FETCHING_PR_INFO, 0.05f))
            val prResult = fetchPrInfoUseCase(owner, repo, prNumber)
            val pullRequest = prResult.getOrElse {
                emitResult(PrAnalyzerResult.AnalysisFailed("Failed to fetch PR info: ${it.message}"))
                return
            }

            val prInfoUi = mapper.toPrInfoUi(pullRequest)
            emitResult(PrAnalyzerResult.PrInfoFetched(prInfoUi))
            emitResult(PrAnalyzerResult.ProgressUpdated(AnalysisStage.FETCHING_PR_INFO, 0.2f))

            // Stage 2: Fetch PR Diff (20-40%)
            emitResult(PrAnalyzerResult.ProgressUpdated(AnalysisStage.FETCHING_DIFF, 0.25f))
            val diffResult = fetchPrDiffUseCase(owner, repo, prNumber)
            val diff = diffResult.getOrElse {
                emitResult(PrAnalyzerResult.AnalysisFailed("Failed to fetch PR diff: ${it.message}"))
                return
            }
            emitResult(PrAnalyzerResult.ProgressUpdated(AnalysisStage.FETCHING_DIFF, 0.4f))

            // Stage 3: Search Relevant Documentation (40-60%)
            emitResult(PrAnalyzerResult.ProgressUpdated(AnalysisStage.SEARCHING_DOCS, 0.45f))
            val docsResult = searchRelevantDocsUseCase(pullRequest, diff, maxResults = 10)
            val documentation = docsResult.getOrElse {
                emitResult(PrAnalyzerResult.AnalysisFailed("Failed to search documentation: ${it.message}"))
                return
            }
            emitResult(PrAnalyzerResult.ProgressUpdated(AnalysisStage.SEARCHING_DOCS, 0.6f))

            // Stage 4: Analyze with LLM (60-90%)
            emitResult(PrAnalyzerResult.ProgressUpdated(AnalysisStage.ANALYZING_WITH_LLM, 0.65f))
            val analysisResult = analyzePrWithLlmUseCase(pullRequest, diff, documentation)
            val llmAnalysis = analysisResult.getOrElse {
                emitResult(PrAnalyzerResult.AnalysisFailed("Failed to analyze with LLM: ${it.message}"))
                return
            }
            emitResult(PrAnalyzerResult.ProgressUpdated(AnalysisStage.ANALYZING_WITH_LLM, 0.9f))

            // Stage 5: Generate Final Report (90-100%)
            emitResult(PrAnalyzerResult.ProgressUpdated(AnalysisStage.GENERATING_REPORT, 0.92f))
            val reportResult = generateReportUseCase(pullRequest, diff, llmAnalysis)
            val report = reportResult.getOrElse {
                emitResult(PrAnalyzerResult.AnalysisFailed("Failed to generate report: ${it.message}"))
                return
            }

            val reportUi = mapper.toAnalysisReportUi(report)
            emitResult(PrAnalyzerResult.ProgressUpdated(AnalysisStage.GENERATING_REPORT, 0.98f))

            // Complete
            emitResult(PrAnalyzerResult.ReportGenerated(reportUi))
            emitResult(PrAnalyzerResult.ProgressUpdated(AnalysisStage.COMPLETED, 1.0f))
            emitEffect(PrAnalyzerEffect.ShowMessage("Analysis completed successfully!"))

        } catch (e: Exception) {
            emitResult(PrAnalyzerResult.AnalysisFailed("Unexpected error: ${e.message}"))
            emitEffect(PrAnalyzerEffect.ShowMessage("Analysis failed: ${e.message}"))
        }
    }

    /**
     * Handles retry after error - clears error and restarts analysis
     */
    private suspend fun handleRetryAnalysis(
        emitResult: suspend (PrAnalyzerResult) -> Unit,
        emitEffect: suspend (PrAnalyzerEffect) -> Unit
    ) {
        emitResult(PrAnalyzerResult.ErrorCleared)
        handleStartAnalysis(emitResult, emitEffect)
    }

    /**
     * Toggles issue expansion state
     */
    private suspend fun handleExpandIssue(
        issueId: String,
        emitResult: suspend (PrAnalyzerResult) -> Unit
    ) {
        emitResult(PrAnalyzerResult.IssueExpanded(issueId))
    }

    /**
     * Opens documentation URL in browser
     */
    private suspend fun handleOpenDocumentation(
        docUrl: String,
        emitEffect: suspend (PrAnalyzerEffect) -> Unit
    ) {
        emitEffect(PrAnalyzerEffect.OpenUrl(docUrl))
    }

    /**
     * Exports the analysis report
     */
    private suspend fun handleExportReport(
        emitEffect: suspend (PrAnalyzerEffect) -> Unit
    ) {
        val timestamp = Clock.System.now().toString().replace(":", "-")
        val fileName = "pr-analysis-report-$timestamp.md"

        // This would need access to the current report from state
        // For now, emit a placeholder - in real implementation,
        // executor would need state access or receive report as parameter
        emitEffect(
            PrAnalyzerEffect.ExportReport(
                content = "# PR Analysis Report\n\nReport content would be here...",
                fileName = fileName
            )
        )
        emitEffect(PrAnalyzerEffect.ShowMessage("Report exported to $fileName"))
    }

    /**
     * Validates GitHub PR URL format
     * Expected format: https://github.com/{owner}/{repo}/pull/{number}
     */
    private fun validatePrUrl(url: String): Boolean {
        val pattern = """^https://github\.com/[\w-]+/[\w-]+/pull/\d+/?$""".toRegex()
        return pattern.matches(url.trim())
    }

    /**
     * Parses GitHub PR URL into components
     */
    private fun parsePrUrl(url: String): PrUrlParts? {
        val pattern = """^https://github\.com/([\w-]+)/([\w-]+)/pull/(\d+)/?$""".toRegex()
        val matchResult = pattern.find(url.trim()) ?: return null

        val (owner, repo, prNumberStr) = matchResult.destructured
        val prNumber = prNumberStr.toIntOrNull() ?: return null

        return PrUrlParts(owner, repo, prNumber)
    }

    /**
     * Data class for parsed PR URL parts
     */
    private data class PrUrlParts(
        val owner: String,
        val repo: String,
        val prNumber: Int
    )
}
