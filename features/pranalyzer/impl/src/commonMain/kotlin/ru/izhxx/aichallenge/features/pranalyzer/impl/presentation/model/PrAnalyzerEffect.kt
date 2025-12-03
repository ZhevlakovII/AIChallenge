package ru.izhxx.aichallenge.features.pranalyzer.impl.presentation.model

import ru.izhxx.aichallenge.core.ui.mvi.model.MviEffect

/**
 * One-time side effects for PR Analyzer screen
 */
sealed interface PrAnalyzerEffect : MviEffect {
    /**
     * Show a message to the user (toast/snackbar)
     */
    data class ShowMessage(val message: String) : PrAnalyzerEffect

    /**
     * Open a URL in the browser
     */
    data class OpenUrl(val url: String) : PrAnalyzerEffect

    /**
     * Export analysis report to a file
     */
    data class ExportReport(
        val content: String,
        val fileName: String
    ) : PrAnalyzerEffect

    /**
     * Navigate back from the screen
     */
    data object NavigateBack : PrAnalyzerEffect
}
