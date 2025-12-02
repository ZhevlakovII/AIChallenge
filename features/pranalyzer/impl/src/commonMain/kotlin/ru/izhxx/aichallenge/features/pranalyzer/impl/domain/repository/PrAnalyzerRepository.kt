package ru.izhxx.aichallenge.features.pranalyzer.impl.domain.repository

import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.DocumentationSearchResult
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.LlmAnalysis
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.PrDiff
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.PullRequest

/**
 * Repository interface for PR Analyzer feature
 * Provides access to GitHub API, LLM services, and documentation search
 */
interface PrAnalyzerRepository {

    /**
     * Fetches basic information about a Pull Request
     *
     * @param owner Repository owner (user or organization)
     * @param repo Repository name
     * @param prNumber Pull Request number
     * @return Result containing PullRequest data or error
     */
    suspend fun fetchPrInfo(
        owner: String,
        repo: String,
        prNumber: Int
    ): Result<PullRequest>

    /**
     * Fetches the complete diff for a Pull Request
     *
     * @param owner Repository owner (user or organization)
     * @param repo Repository name
     * @param prNumber Pull Request number
     * @return Result containing PrDiff data or error
     */
    suspend fun fetchPrDiff(
        owner: String,
        repo: String,
        prNumber: Int
    ): Result<PrDiff>

    /**
     * Fetches the content of a specific file from the repository
     *
     * @param owner Repository owner (user or organization)
     * @param repo Repository name
     * @param filePath Path to the file
     * @param ref Git reference (branch, tag, or commit SHA)
     * @return Result containing file content as String or error
     */
    suspend fun fetchFileContent(
        owner: String,
        repo: String,
        filePath: String,
        ref: String
    ): Result<String>

    /**
     * Searches for relevant documentation based on PR content
     *
     * @param query Search query derived from PR content
     * @param maxResults Maximum number of results to return
     * @return Result containing DocumentationSearchResult or error
     */
    suspend fun searchRelevantDocumentation(
        query: String,
        maxResults: Int = 10
    ): Result<DocumentationSearchResult>

    /**
     * Analyzes the Pull Request using LLM
     *
     * @param pullRequest Pull Request information
     * @param diff Complete diff of the PR
     * @param documentation Relevant documentation references
     * @return Result containing LlmAnalysis or error
     */
    suspend fun analyzePrWithLlm(
        pullRequest: PullRequest,
        diff: PrDiff,
        documentation: DocumentationSearchResult
    ): Result<LlmAnalysis>
}
