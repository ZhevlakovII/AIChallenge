package ru.izhxx.aichallenge.features.pranalyzer.impl.presentation.mapper

import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.AnalysisReport
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.CodeIssue
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.DocumentationReference
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.PullRequest
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.Recommendation
import ru.izhxx.aichallenge.features.pranalyzer.impl.presentation.model.AnalysisReportUi
import ru.izhxx.aichallenge.features.pranalyzer.impl.presentation.model.CodeIssueUi
import ru.izhxx.aichallenge.features.pranalyzer.impl.presentation.model.DocumentationRefUi
import ru.izhxx.aichallenge.features.pranalyzer.impl.presentation.model.PrInfoUi
import ru.izhxx.aichallenge.features.pranalyzer.impl.presentation.model.RecommendationUi
import kotlin.random.Random

/**
 * Mapper for converting domain models to UI models
 */
class PrAnalysisUiMapper {
    /**
     * Maps domain PullRequest to UI model
     */
    fun toPrInfoUi(pr: PullRequest): PrInfoUi {
        return PrInfoUi(
            number = pr.number,
            title = pr.title,
            description = pr.description,
            author = pr.author,
            branch = pr.headBranch,
            baseBranch = pr.baseBranch,
            status = pr.state,
            filesChanged = pr.filesChanged,
            linesAdded = pr.additions,
            linesDeleted = pr.deletions,
            createdAt = pr.createdAt,
            updatedAt = pr.updatedAt,
            url = pr.url
        )
    }

    /**
     * Maps domain AnalysisReport to UI model
     */
    fun toAnalysisReportUi(report: AnalysisReport): AnalysisReportUi {
        return AnalysisReportUi(
            summary = report.llmAnalysis.summary,
            overallScore = report.llmAnalysis.overallScore,
            readabilityScore = report.llmAnalysis.readabilityScore,
            maintainabilityScore = report.llmAnalysis.maintainabilityScore,
            securityScore = report.llmAnalysis.securityScore,
            strengths = report.llmAnalysis.strengths,
            weaknesses = report.llmAnalysis.weaknesses,
            issues = report.llmAnalysis.issues.map { toCodeIssueUi(it) },
            recommendations = report.llmAnalysis.recommendations.map { toRecommendationUi(it) },
            documentationRefs = report.llmAnalysis.relevantDocumentation.map { toDocumentationRefUi(it) },
            testCoverageAssessment = report.llmAnalysis.testCoverageAssessment,
            architecturalNotes = report.llmAnalysis.architecturalNotes,
            generatedAt = report.generatedAt
        )
    }

    /**
     * Maps domain CodeIssue to UI model
     */
    fun toCodeIssueUi(issue: CodeIssue): CodeIssueUi {
        return CodeIssueUi(
            id = generateId(issue),
            severity = issue.severity,
            category = issue.category,
            title = issue.title,
            description = issue.description,
            file = issue.file,
            lineNumber = issue.lineNumber,
            codeSnippet = issue.codeSnippet,
            suggestion = issue.suggestion
        )
    }

    /**
     * Maps domain Recommendation to UI model
     */
    fun toRecommendationUi(rec: Recommendation): RecommendationUi {
        return RecommendationUi(
            id = generateId(rec),
            title = rec.title,
            description = rec.description,
            priority = rec.priority,
            rationale = rec.rationale,
            relatedFiles = rec.relatedFiles,
            actionableSteps = rec.actionableSteps
        )
    }

    /**
     * Maps domain DocumentationReference to UI model
     */
    fun toDocumentationRefUi(doc: DocumentationReference): DocumentationRefUi {
        return DocumentationRefUi(
            id = generateId(doc),
            title = doc.title,
            section = doc.section,
            excerpt = doc.summary,
            relevanceScore = doc.relevanceScore,
            url = doc.url
        )
    }

    /**
     * Generates a unique ID for UI models
     * Uses a combination of hashCode and random string for uniqueness
     */
    private fun generateId(obj: Any): String {
        val randomSuffix = Random.nextInt(0, Int.MAX_VALUE).toString(36)
        return "${obj.hashCode()}-$randomSuffix"
    }
}
