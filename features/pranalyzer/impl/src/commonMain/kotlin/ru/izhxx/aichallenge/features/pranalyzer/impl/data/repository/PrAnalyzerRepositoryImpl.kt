package ru.izhxx.aichallenge.features.pranalyzer.impl.data.repository

import kotlinx.datetime.Instant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ru.izhxx.aichallenge.features.pranalyzer.impl.data.datasource.LlmAnalysisDataSource
import ru.izhxx.aichallenge.features.pranalyzer.impl.data.datasource.PrMcpDataSource
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.CodeIssue
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.DocumentationSearchResult
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.FileStatus
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.IssueCategory
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.IssueSeverity
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.LlmAnalysis
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.PrDiff
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.PrFile
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.PrState
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.PullRequest
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.Recommendation
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.RecommendationCategory
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.model.RecommendationPriority
import ru.izhxx.aichallenge.features.pranalyzer.impl.domain.repository.PrAnalyzerRepository
import kotlin.time.ExperimentalTime

/**
 * Implementation of PrAnalyzerRepository
 *
 * Aggregates PrMcpDataSource and LlmAnalysisDataSource to provide complete
 * PR analysis functionality. Handles JSON parsing, data transformation,
 * and orchestration of data sources.
 */
@ExperimentalTime
class PrAnalyzerRepositoryImpl(
    private val prMcpDataSource: PrMcpDataSource,
    private val llmAnalysisDataSource: LlmAnalysisDataSource,
    private val json: Json
) : PrAnalyzerRepository {

    override suspend fun fetchPrInfo(
        owner: String,
        repo: String,
        prNumber: Int
    ): Result<PullRequest> = runCatching {
        val prUrl = "https://github.com/$owner/$repo/pull/$prNumber"
        val prInfoJson = prMcpDataSource.getPrInfo(prUrl).getOrThrow()

        mapJsonToPullRequest(prInfoJson, prNumber)
    }

    override suspend fun fetchPrDiff(
        owner: String,
        repo: String,
        prNumber: Int
    ): Result<PrDiff> = runCatching {
        val prUrl = "https://github.com/$owner/$repo/pull/$prNumber"
        val filesJson = prMcpDataSource.getPrFiles(prUrl).getOrThrow()

        mapJsonToPrDiff(filesJson, prNumber)
    }

    override suspend fun fetchFileContent(
        owner: String,
        repo: String,
        filePath: String,
        ref: String
    ): Result<String> = runCatching {
        // For file content, we construct a PR URL with the ref
        // Note: This assumes the MCP tool supports ref parameter
        // If not, we may need to adjust the implementation
        val prUrl = "https://github.com/$owner/$repo/pull/0" // Placeholder, actual implementation may vary
        prMcpDataSource.getFileContent(prUrl, filePath).getOrThrow()
    }

    override suspend fun searchRelevantDocumentation(
        query: String,
        maxResults: Int
    ): Result<DocumentationSearchResult> = runCatching {
        // TODO: Implement RAG integration for documentation search
        // For now, return empty result as a placeholder
        DocumentationSearchResult(
            query = query,
            results = emptyList(),
            totalResults = 0,
            searchDurationMs = 0
        )
    }

    override suspend fun analyzePrWithLlm(
        pullRequest: PullRequest,
        diff: PrDiff,
        documentation: DocumentationSearchResult
    ): Result<LlmAnalysis> = runCatching {
        // Build system prompt for code review
        val systemPrompt = buildCodeReviewSystemPrompt()

        // Build user prompt with PR data
        val userPrompt = buildCodeReviewUserPrompt(pullRequest, diff, documentation)

        // Call LLM
        val llmResponse = llmAnalysisDataSource.analyzeCode(systemPrompt, userPrompt)
            .getOrThrow()

        // Parse JSON response to LlmAnalysis
        parseLlmAnalysisResponse(llmResponse, pullRequest.number)
    }

    /**
     * Maps JSON response from MCP pr.info tool to PullRequest domain model
     */
    private fun mapJsonToPullRequest(prInfoJson: JsonObject, prNumber: Int): PullRequest {
        return PullRequest(
            number = prNumber,
            title = prInfoJson["title"]?.jsonPrimitive?.content ?: "",
            description = prInfoJson["body"]?.jsonPrimitive?.content ?: "",
            author = prInfoJson["user"]?.jsonObject?.get("login")?.jsonPrimitive?.content ?: "unknown",
            baseBranch = prInfoJson["base"]?.jsonObject?.get("ref")?.jsonPrimitive?.content ?: "main",
            headBranch = prInfoJson["head"]?.jsonObject?.get("ref")?.jsonPrimitive?.content ?: "",
            state = mapPrState(prInfoJson["state"]?.jsonPrimitive?.content),
            createdAt = Instant.parse(prInfoJson["created_at"]?.jsonPrimitive?.content ?: "1970-01-01T00:00:00Z"),
            updatedAt = Instant.parse(prInfoJson["updated_at"]?.jsonPrimitive?.content ?: "1970-01-01T00:00:00Z"),
            url = prInfoJson["html_url"]?.jsonPrimitive?.content ?: "",
            filesChanged = prInfoJson["changed_files"]?.jsonPrimitive?.int ?: 0,
            additions = prInfoJson["additions"]?.jsonPrimitive?.int ?: 0,
            deletions = prInfoJson["deletions"]?.jsonPrimitive?.int ?: 0,
            commits = prInfoJson["commits"]?.jsonPrimitive?.int ?: 0
        )
    }

    /**
     * Maps JSON response from MCP pr.files tool to PrDiff domain model
     */
    private fun mapJsonToPrDiff(filesJson: List<JsonObject>, prNumber: Int): PrDiff {
        val files = filesJson.map { fileJson ->
            PrFile(
                filename = fileJson["filename"]?.jsonPrimitive?.content ?: "",
                status = mapFileStatus(fileJson["status"]?.jsonPrimitive?.content),
                additions = fileJson["additions"]?.jsonPrimitive?.int ?: 0,
                deletions = fileJson["deletions"]?.jsonPrimitive?.int ?: 0,
                changes = fileJson["changes"]?.jsonPrimitive?.int ?: 0,
                patch = fileJson["patch"]?.jsonPrimitive?.content,
                blobUrl = fileJson["blob_url"]?.jsonPrimitive?.content ?: "",
                rawUrl = fileJson["raw_url"]?.jsonPrimitive?.content ?: ""
            )
        }

        val totalAdditions = files.sumOf { it.additions }
        val totalDeletions = files.sumOf { it.deletions }

        return PrDiff(
            prNumber = prNumber,
            files = files,
            totalAdditions = totalAdditions,
            totalDeletions = totalDeletions,
            totalChanges = totalAdditions + totalDeletions
        )
    }

    /**
     * Maps PR state string to PrState enum
     */
    private fun mapPrState(state: String?): PrState {
        return when (state?.lowercase()) {
            "open" -> PrState.OPEN
            "closed" -> PrState.CLOSED
            "merged" -> PrState.MERGED
            "draft" -> PrState.DRAFT
            else -> PrState.OPEN
        }
    }

    /**
     * Maps file status string to FileStatus enum
     */
    private fun mapFileStatus(status: String?): FileStatus {
        return when (status?.lowercase()) {
            "added" -> FileStatus.ADDED
            "modified" -> FileStatus.MODIFIED
            "removed" -> FileStatus.REMOVED
            "renamed" -> FileStatus.RENAMED
            "copied" -> FileStatus.COPIED
            "changed" -> FileStatus.CHANGED
            else -> FileStatus.UNCHANGED
        }
    }

    /**
     * Builds system prompt for code review task
     */
    private fun buildCodeReviewSystemPrompt(): String {
        return """
            You are an expert code reviewer specializing in Kotlin Multiplatform, Compose Multiplatform,
            Clean Architecture, and MVI pattern.

            Your task is to analyze Pull Requests and provide comprehensive code reviews with a focus on:
            1. Clean Architecture principles (Data/Domain/Presentation layers)
            2. MVI pattern implementation
            3. Kotlin best practices and idioms
            4. Multiplatform considerations
            5. Code quality, readability, and maintainability
            6. Security concerns
            7. Performance implications
            8. Test coverage

            Respond with a JSON object containing the following structure:
            {
              "summary": "Brief summary of the PR changes",
              "strengths": ["List of positive aspects"],
              "weaknesses": ["List of areas for improvement"],
              "issues": [
                {
                  "severity": "HIGH|MEDIUM|LOW",
                  "category": "Category name",
                  "description": "Issue description",
                  "location": "File:Line",
                  "suggestion": "Suggested fix"
                }
              ],
              "recommendations": [
                {
                  "priority": "HIGH|MEDIUM|LOW",
                  "category": "Category name",
                  "description": "Recommendation description",
                  "rationale": "Why this is important"
                }
              ],
              "quality_scores": {
                "overall": 0-100,
                "readability": 0-100,
                "maintainability": 0-100,
                "security": 0-100
              },
              "test_coverage_assessment": "Assessment of test coverage",
              "architectural_notes": "Notes about architectural decisions"
            }
            Return the answer only following this structure.
        """.trimIndent()
    }

    /**
     * Builds user prompt with PR data for analysis
     */
    private fun buildCodeReviewUserPrompt(
        pullRequest: PullRequest,
        diff: PrDiff,
        documentation: DocumentationSearchResult
    ): String {
        val docContext = if (documentation.results.isNotEmpty()) {
            "\n\n## Relevant Documentation:\n" +
                documentation.results.joinToString("\n\n") { doc ->
                    "- ${doc.title}: ${doc.summary}"
                }
        } else {
            ""
        }

        return """
            # Pull Request Analysis

            ## PR Information
            - Title: ${pullRequest.title}
            - Description: ${pullRequest.description}
            - Author: ${pullRequest.author}
            - Base Branch: ${pullRequest.baseBranch} → Head Branch: ${pullRequest.headBranch}
            - Files Changed: ${pullRequest.filesChanged}
            - Additions: ${pullRequest.additions}, Deletions: ${pullRequest.deletions}
            - Commits: ${pullRequest.commits}

            ## Changed Files
            ${diff.files.joinToString("\n") { file ->
            "- ${file.filename} (${file.status}): +${file.additions} -${file.deletions}"
        }}

            ## Diff Content
            ${diff.files.joinToString("\n\n") { file ->
            """
                ### ${file.filename}
                ${file.patch ?: "No patch available"}
            """.trimIndent()
        }}
            $docContext

            Please analyze this PR and provide a comprehensive code review.
        """.trimIndent()
    }

    /**
     * Parses LLM JSON response to LlmAnalysis domain model
     */
    private fun parseLlmAnalysisResponse(llmResponse: String, prNumber: Int): LlmAnalysis {
        if (llmResponse.first() != '{' && llmResponse.last() != '}') {
            return LlmAnalysis(
                prNumber = prNumber,
                summary = llmResponse,
                strengths = emptyList(),
                weaknesses = emptyList(),
                issues = emptyList(),
                recommendations = emptyList(),
                overallScore = 0,
                readabilityScore = 0,
                maintainabilityScore = 0,
                securityScore = 0,
                testCoverageAssessment = "",
                architecturalNotes = "",
                relevantDocumentation = emptyList() // Will be populated from RAG in future
            )
        }

        val jsonResponse = json.parseToJsonElement(llmResponse).jsonObject

        val issues = jsonResponse["issues"]?.jsonArray?.map { issueJson ->
            val issue = issueJson.jsonObject
            CodeIssue(
                severity = mapIssueSeverity(issue["severity"]?.jsonPrimitive?.content),
                category = mapIssueCategory(issue["category"]?.jsonPrimitive?.content),
                title = issue["title"]?.jsonPrimitive?.content ?: "Untitled Issue",
                description = issue["description"]?.jsonPrimitive?.content ?: "",
                file = issue["file"]?.jsonPrimitive?.content,
                lineNumber = issue["line_number"]?.jsonPrimitive?.int,
                codeSnippet = issue["code_snippet"]?.jsonPrimitive?.content,
                suggestion = issue["suggestion"]?.jsonPrimitive?.content
            )
        } ?: emptyList()

        val recommendations = jsonResponse["recommendations"]?.jsonArray?.map { recJson ->
            val rec = recJson.jsonObject
            Recommendation(
                priority = mapRecommendationPriority(rec["priority"]?.jsonPrimitive?.content),
                category = mapRecommendationCategory(rec["category"]?.jsonPrimitive?.content),
                title = rec["title"]?.jsonPrimitive?.content ?: "Untitled Recommendation",
                description = rec["description"]?.jsonPrimitive?.content ?: "",
                rationale = rec["rationale"]?.jsonPrimitive?.content ?: "",
                relatedFiles = rec["related_files"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                actionableSteps = rec["actionable_steps"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
            )
        } ?: emptyList()

        val qualityScores = jsonResponse["quality_scores"]?.jsonObject
        val overallScore = qualityScores?.get("overall")?.jsonPrimitive?.int ?: 50
        val readabilityScore = qualityScores?.get("readability")?.jsonPrimitive?.int ?: 50
        val maintainabilityScore = qualityScores?.get("maintainability")?.jsonPrimitive?.int ?: 50
        val securityScore = qualityScores?.get("security")?.jsonPrimitive?.int ?: 50

        val strengths = jsonResponse["strengths"]?.jsonArray?.map {
            it.jsonPrimitive.content
        } ?: emptyList()

        val weaknesses = jsonResponse["weaknesses"]?.jsonArray?.map {
            it.jsonPrimitive.content
        } ?: emptyList()

        return LlmAnalysis(
            prNumber = prNumber,
            summary = jsonResponse["summary"]?.jsonPrimitive?.content ?: "",
            strengths = strengths,
            weaknesses = weaknesses,
            issues = issues,
            recommendations = recommendations,
            overallScore = overallScore,
            readabilityScore = readabilityScore,
            maintainabilityScore = maintainabilityScore,
            securityScore = securityScore,
            testCoverageAssessment = jsonResponse["test_coverage_assessment"]?.jsonPrimitive?.content ?: "",
            architecturalNotes = jsonResponse["architectural_notes"]?.jsonPrimitive?.content,
            relevantDocumentation = emptyList() // Will be populated from RAG in future
        )
    }

    /**
     * Maps severity string to IssueSeverity enum
     */
    private fun mapIssueSeverity(severity: String?): IssueSeverity {
        return when (severity?.uppercase()) {
            "CRITICAL" -> IssueSeverity.CRITICAL
            "HIGH" -> IssueSeverity.HIGH
            "MEDIUM" -> IssueSeverity.MEDIUM
            "LOW" -> IssueSeverity.LOW
            "INFO" -> IssueSeverity.INFO
            else -> IssueSeverity.MEDIUM
        }
    }

    /**
     * Maps category string to IssueCategory enum
     */
    private fun mapIssueCategory(category: String?): IssueCategory {
        return when (category?.uppercase()?.replace(" ", "_")) {
            "BUG" -> IssueCategory.BUG
            "SECURITY" -> IssueCategory.SECURITY
            "PERFORMANCE" -> IssueCategory.PERFORMANCE
            "CODE_STYLE", "STYLE" -> IssueCategory.CODE_STYLE
            "MAINTAINABILITY" -> IssueCategory.MAINTAINABILITY
            "DOCUMENTATION", "DOCS" -> IssueCategory.DOCUMENTATION
            "TESTING", "TESTS" -> IssueCategory.TESTING
            "ARCHITECTURE" -> IssueCategory.ARCHITECTURE
            "BEST_PRACTICE", "BEST_PRACTICES" -> IssueCategory.BEST_PRACTICE
            else -> IssueCategory.OTHER
        }
    }

    /**
     * Maps priority string to RecommendationPriority enum
     */
    private fun mapRecommendationPriority(priority: String?): RecommendationPriority {
        return when (priority?.uppercase()?.replace(" ", "_")) {
            "MUST_HAVE", "MUST", "CRITICAL", "HIGH" -> RecommendationPriority.MUST_HAVE
            "SHOULD_HAVE", "SHOULD", "MEDIUM" -> RecommendationPriority.SHOULD_HAVE
            "NICE_TO_HAVE", "NICE", "LOW" -> RecommendationPriority.NICE_TO_HAVE
            "OPTIONAL" -> RecommendationPriority.OPTIONAL
            else -> RecommendationPriority.SHOULD_HAVE
        }
    }

    /**
     * Maps category string to RecommendationCategory enum
     */
    private fun mapRecommendationCategory(category: String?): RecommendationCategory {
        return when (category?.uppercase()?.replace(" ", "_")) {
            "REFACTORING" -> RecommendationCategory.REFACTORING
            "TESTING", "TESTS" -> RecommendationCategory.TESTING
            "DOCUMENTATION", "DOCS" -> RecommendationCategory.DOCUMENTATION
            "ERROR_HANDLING", "ERRORS" -> RecommendationCategory.ERROR_HANDLING
            "PERFORMANCE_OPTIMIZATION", "PERFORMANCE" -> RecommendationCategory.PERFORMANCE_OPTIMIZATION
            "SECURITY_ENHANCEMENT", "SECURITY" -> RecommendationCategory.SECURITY_ENHANCEMENT
            "CODE_ORGANIZATION", "ORGANIZATION" -> RecommendationCategory.CODE_ORGANIZATION
            "API_DESIGN", "API" -> RecommendationCategory.API_DESIGN
            "USER_EXPERIENCE", "UX" -> RecommendationCategory.USER_EXPERIENCE
            "ACCESSIBILITY" -> RecommendationCategory.ACCESSIBILITY
            else -> RecommendationCategory.OTHER
        }
    }
}
