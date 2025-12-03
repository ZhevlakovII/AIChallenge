@file:OptIn(ExperimentalTime::class)

package ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model

import kotlinx.datetime.Instant
import kotlin.time.ExperimentalTime

/**
 * Represents the complete analysis report for a Pull Request
 */
data class AnalysisReport(
    val pullRequest: PullRequest,
    val diff: PrDiff,
    val llmAnalysis: LlmAnalysis,
    val generatedAt: Instant,
    val analysisVersion: String = "1.0.0"
)

/**
 * Represents a summary view of the analysis report
 */
data class AnalysisReportSummary(
    val prNumber: Int,
    val prTitle: String,
    val overallScore: Int,
    val criticalIssuesCount: Int,
    val highIssuesCount: Int,
    val mediumIssuesCount: Int,
    val lowIssuesCount: Int,
    val recommendationsCount: Int,
    val generatedAt: Instant
)

/**
 * Extension function to create a summary from a full report
 */
fun AnalysisReport.toSummary(): AnalysisReportSummary {
    val issuesBySeverity = llmAnalysis.issues.groupBy { it.severity }

    return AnalysisReportSummary(
        prNumber = pullRequest.number,
        prTitle = pullRequest.title,
        overallScore = llmAnalysis.overallScore,
        criticalIssuesCount = issuesBySeverity[IssueSeverity.CRITICAL]?.size ?: 0,
        highIssuesCount = issuesBySeverity[IssueSeverity.HIGH]?.size ?: 0,
        mediumIssuesCount = issuesBySeverity[IssueSeverity.MEDIUM]?.size ?: 0,
        lowIssuesCount = issuesBySeverity[IssueSeverity.LOW]?.size ?: 0,
        recommendationsCount = llmAnalysis.recommendations.size,
        generatedAt = generatedAt
    )
}
