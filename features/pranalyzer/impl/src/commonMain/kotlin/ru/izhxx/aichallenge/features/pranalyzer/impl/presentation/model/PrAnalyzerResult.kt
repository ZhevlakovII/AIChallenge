package ru.izhxx.aichallenge.features.pranalyzer.impl.presentation.model

/**
 * Results from MVI Executor for PR Analyzer
 */
sealed interface PrAnalyzerResult : MviResult {
    /**
     * Result of URL validation
     */
    data class UrlValidated(
        val url: String,
        val isValid: Boolean
    ) : PrAnalyzerResult

    /**
     * Analysis has started
     */
    data object AnalysisStarted : PrAnalyzerResult

    /**
     * Progress update during analysis
     */
    data class ProgressUpdated(
        val stage: AnalysisStage,
        val progress: Float
    ) : PrAnalyzerResult

    /**
     * Pull Request information has been fetched
     */
    data class PrInfoFetched(
        val prInfo: PrInfoUi
    ) : PrAnalyzerResult

    /**
     * Analysis report has been generated
     */
    data class ReportGenerated(
        val report: AnalysisReportUi
    ) : PrAnalyzerResult

    /**
     * Analysis has failed with an error
     */
    data class AnalysisFailed(
        val error: String
    ) : PrAnalyzerResult

    /**
     * An issue has been expanded/collapsed
     */
    data class IssueExpanded(
        val issueId: String
    ) : PrAnalyzerResult

    /**
     * Error has been cleared
     */
    data object ErrorCleared : PrAnalyzerResult
}
