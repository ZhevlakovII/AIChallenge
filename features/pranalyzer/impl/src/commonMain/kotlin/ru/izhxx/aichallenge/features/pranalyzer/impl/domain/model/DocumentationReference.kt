package ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model

/**
 * Represents a reference to project documentation relevant to the PR
 */
data class DocumentationReference(
    val title: String,
    val url: String?,
    val filePath: String?,
    val relevanceScore: Double,
    val summary: String,
    val keywords: List<String>,
    val section: String?
)

/**
 * Represents a search result from documentation search
 */
data class DocumentationSearchResult(
    val query: String,
    val results: List<DocumentationReference>,
    val totalResults: Int,
    val searchDurationMs: Long
)
