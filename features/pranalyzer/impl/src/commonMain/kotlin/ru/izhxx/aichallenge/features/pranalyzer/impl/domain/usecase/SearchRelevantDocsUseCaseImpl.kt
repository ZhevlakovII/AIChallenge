package ru.izhxx.aichallenge.features.pranalyzer.impl.domain.usecase

import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.DocumentationSearchResult
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.PrDiff
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.PullRequest
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.repository.PrAnalyzerRepository

/**
 * Implementation of SearchRelevantDocsUseCase
 */
class SearchRelevantDocsUseCaseImpl(
    private val repository: PrAnalyzerRepository
) : SearchRelevantDocsUseCase {

    override suspend fun invoke(
        pullRequest: PullRequest,
        diff: PrDiff,
        maxResults: Int
    ): Result<DocumentationSearchResult> {
        require(maxResults > 0) { "Max results must be positive" }

        // Build search query from PR title, description, and changed files
        val query = buildSearchQuery(pullRequest, diff)

        return repository.searchRelevantDocumentation(query, maxResults)
    }

    /**
     * Builds a search query from PR content
     */
    private fun buildSearchQuery(pullRequest: PullRequest, diff: PrDiff): String {
        val keywords = mutableSetOf<String>()

        // Extract keywords from PR title and description
        keywords.addAll(extractKeywords(pullRequest.title))
        keywords.addAll(extractKeywords(pullRequest.description))

        // Extract keywords from changed file paths
        diff.files.forEach { file ->
            val pathParts = file.filename.split("/", ".", "_", "-")
            keywords.addAll(pathParts.filter { it.length > 2 })
        }

        return keywords.joinToString(" ")
    }

    /**
     * Extracts meaningful keywords from text
     */
    private fun extractKeywords(text: String): List<String> {
        // Simple keyword extraction - can be enhanced with NLP
        val stopWords = setOf("the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for")
        return text
            .split(" ", "\n", ",", ".", ":", ";")
            .map { it.lowercase().trim() }
            .filter { it.length > 2 && it !in stopWords }
            .distinct()
    }
}
