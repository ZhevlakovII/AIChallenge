package ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model

/**
 * Represents a recommendation for improving the PR
 */
data class Recommendation(
    val priority: RecommendationPriority,
    val category: RecommendationCategory,
    val title: String,
    val description: String,
    val rationale: String,
    val relatedFiles: List<String>,
    val actionableSteps: List<String>
)

/**
 * Priority level of a recommendation
 */
enum class RecommendationPriority {
    MUST_HAVE,
    SHOULD_HAVE,
    NICE_TO_HAVE,
    OPTIONAL
}

/**
 * Category of a recommendation
 */
enum class RecommendationCategory {
    REFACTORING,
    TESTING,
    DOCUMENTATION,
    ERROR_HANDLING,
    PERFORMANCE_OPTIMIZATION,
    SECURITY_ENHANCEMENT,
    CODE_ORGANIZATION,
    API_DESIGN,
    USER_EXPERIENCE,
    ACCESSIBILITY,
    OTHER
}
