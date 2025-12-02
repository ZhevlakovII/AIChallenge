package ru.izhxx.aichallenge.features.pranalyzer.impl.presentation.model

import ru.izhxx.aichallenge.core.ui.mvi.model.MviIntent

/**
 * User intents for PR Analyzer screen
 */
sealed interface PrAnalyzerIntent : MviIntent {
    /**
     * User changed the PR URL input
     */
    data class PrUrlChanged(val url: String) : PrAnalyzerIntent

    /**
     * User clicked the "Analyze" button
     */
    data object StartAnalysis : PrAnalyzerIntent

    /**
     * User wants to retry analysis after an error
     */
    data object RetryAnalysis : PrAnalyzerIntent

    /**
     * User clicked to expand/collapse an issue details
     */
    data class ExpandIssue(val issueId: String) : PrAnalyzerIntent

    /**
     * User clicked to open documentation link
     */
    data class OpenDocumentation(val docUrl: String) : PrAnalyzerIntent

    /**
     * User clicked to export the analysis report
     */
    data object ExportReport : PrAnalyzerIntent
}
