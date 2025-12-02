package ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model

/**
 * Represents the complete LLM analysis result for a PR
 */
data class LlmAnalysis(
    val prNumber: Int,
    val summary: String,
    val strengths: List<String>,
    val weaknesses: List<String>,
    val issues: List<CodeIssue>,
    val recommendations: List<Recommendation>,
    val overallScore: Int,
    val readabilityScore: Int,
    val maintainabilityScore: Int,
    val securityScore: Int,
    val testCoverageAssessment: String,
    val architecturalNotes: String?,
    val relevantDocumentation: List<DocumentationReference>
)

/**
 * Represents the quality scores for different aspects of the PR
 */
data class QualityScores(
    val overall: Int,
    val readability: Int,
    val maintainability: Int,
    val security: Int,
    val performance: Int,
    val testability: Int
) {
    init {
        require(overall in 0..100) { "Overall score must be between 0 and 100" }
        require(readability in 0..100) { "Readability score must be between 0 and 100" }
        require(maintainability in 0..100) { "Maintainability score must be between 0 and 100" }
        require(security in 0..100) { "Security score must be between 0 and 100" }
        require(performance in 0..100) { "Performance score must be between 0 and 100" }
        require(testability in 0..100) { "Testability score must be between 0 and 100" }
    }
}
