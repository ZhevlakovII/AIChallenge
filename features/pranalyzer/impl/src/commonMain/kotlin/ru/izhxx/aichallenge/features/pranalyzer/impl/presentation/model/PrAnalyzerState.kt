package ru.izhxx.aichallenge.features.pranalyzer.impl.presentation.model

import kotlinx.datetime.Instant
import ru.izhxx.aichallenge.core.ui.mvi.model.MviState
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.IssueCategory
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.IssueSeverity
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.PrState
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.RecommendationPriority

/**
 * UI state for PR Analyzer screen
 */
data class PrAnalyzerState(
    val prUrl: String = "",
    val isLoading: Boolean = false,
    val loadingStage: AnalysisStage = AnalysisStage.IDLE,
    val progress: Float = 0f,
    val prInfo: PrInfoUi? = null,
    val analysisReport: AnalysisReportUi? = null,
    val error: String? = null,
    val expandedIssueIds: Set<String> = emptySet(),
    val isUrlValid: Boolean = false,
    val canAnalyze: Boolean = false
) : MviState

/**
 * Stages of PR analysis pipeline
 */
enum class AnalysisStage {
    IDLE,
    FETCHING_PR_INFO,
    FETCHING_DIFF,
    SEARCHING_DOCS,
    ANALYZING_WITH_LLM,
    GENERATING_REPORT,
    COMPLETED
}

/**
 * UI model for Pull Request information
 */
data class PrInfoUi(
    val number: Int,
    val title: String,
    val description: String,
    val author: String,
    val branch: String,
    val baseBranch: String,
    val status: PrState,
    val filesChanged: Int,
    val linesAdded: Int,
    val linesDeleted: Int,
    val createdAt: Instant,
    val updatedAt: Instant,
    val url: String
)

/**
 * UI model for complete analysis report
 */
data class AnalysisReportUi(
    val summary: String,
    val overallScore: Int,
    val readabilityScore: Int,
    val maintainabilityScore: Int,
    val securityScore: Int,
    val strengths: List<String>,
    val weaknesses: List<String>,
    val issues: List<CodeIssueUi>,
    val recommendations: List<RecommendationUi>,
    val documentationRefs: List<DocumentationRefUi>,
    val testCoverageAssessment: String,
    val architecturalNotes: String?,
    val generatedAt: Instant
)

/**
 * UI model for code issue
 */
data class CodeIssueUi(
    val id: String,
    val severity: IssueSeverity,
    val category: IssueCategory,
    val title: String,
    val description: String,
    val file: String?,
    val lineNumber: Int?,
    val codeSnippet: String?,
    val suggestion: String?
)

/**
 * UI model for recommendation
 */
data class RecommendationUi(
    val id: String,
    val title: String,
    val description: String,
    val priority: RecommendationPriority,
    val rationale: String,
    val relatedFiles: List<String>,
    val actionableSteps: List<String>
)

/**
 * UI model for documentation reference
 */
data class DocumentationRefUi(
    val id: String,
    val title: String,
    val section: String?,
    val excerpt: String,
    val relevanceScore: Double,
    val url: String?
)
