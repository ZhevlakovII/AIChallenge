package ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model

/**
 * Represents a code issue found during PR analysis
 */
data class CodeIssue(
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
 * Severity level of a code issue
 */
enum class IssueSeverity {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW,
    INFO
}

/**
 * Category of a code issue
 */
enum class IssueCategory {
    BUG,
    SECURITY,
    PERFORMANCE,
    CODE_STYLE,
    MAINTAINABILITY,
    DOCUMENTATION,
    TESTING,
    ARCHITECTURE,
    BEST_PRACTICE,
    OTHER
}
