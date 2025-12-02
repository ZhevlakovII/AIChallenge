package ru.izhxx.aichallenge.features.pranalyzer.impl.data.datasource

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import ru.izhxx.aichallenge.mcp.domain.repository.McpRepository
import ru.izhxx.aichallenge.mcp.orchestrator.McpRouter

/**
 * Implementation of PrMcpDataSource using existing McpRepository
 *
 * This data source uses MCP tools to fetch PR information from GitHub.
 * It parses GitHub PR URLs and calls appropriate MCP tools.
 */
class PrMcpDataSourceImpl(
    private val mcpRepository: McpRepository,
    private val mcpRouter: McpRouter,
    private val json: Json
) : PrMcpDataSource {

    override suspend fun getPrInfo(prUrl: String): Result<JsonObject> = runCatching {
        val (owner, repo, prNumber) = parsePrUrl(prUrl)
        val mcpServerUrl = mcpRouter.resolve("pr.info").orEmpty()

        // Build arguments for pr.info MCP tool
        val args = buildJsonObject {
            put("owner", owner)
            put("repo", repo)
            put("pull_number", prNumber)
        }

        // Call MCP tool
        val result = mcpRepository.callTool(mcpServerUrl, "pr.info", args)
            .getOrThrow()

        // Return as JsonObject
        result.jsonObject
    }

    override suspend fun getPrDiff(prUrl: String): Result<String> = runCatching {
        val (owner, repo, prNumber) = parsePrUrl(prUrl)
        val mcpServerUrl = mcpRouter.resolve("pr.diff").orEmpty()

        // Build arguments for pr.diff MCP tool
        val args = buildJsonObject {
            put("owner", owner)
            put("repo", repo)
            put("pull_number", prNumber)
        }

        // Call MCP tool
        val result = mcpRepository.callTool(mcpServerUrl, "pr.diff", args)
            .getOrThrow()

        // Extract diff string from result
        result.jsonObject["diff"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("No diff found in MCP response")
    }

    override suspend fun getPrFiles(prUrl: String): Result<List<JsonObject>> = runCatching {
        val (owner, repo, prNumber) = parsePrUrl(prUrl)
        val mcpServerUrl = mcpRouter.resolve("pr.files").orEmpty()

        // Build arguments for pr.files MCP tool
        val args = buildJsonObject {
            put("owner", owner)
            put("repo", repo)
            put("pull_number", prNumber)
        }

        // Call MCP tool
        val result = mcpRepository.callTool(mcpServerUrl, "pr.files", args)
            .getOrThrow()

        // Extract files array from result
        result.jsonObject["files"]?.jsonArray?.map { it.jsonObject }
            ?: throw IllegalStateException("No files found in MCP response")
    }

    override suspend fun getFileContent(prUrl: String, filePath: String): Result<String> = runCatching {
        val (owner, repo, prNumber) = parsePrUrl(prUrl)
        val mcpServerUrl = mcpRouter.resolve("pr.file_content").orEmpty()

        // Build arguments for pr.file_content MCP tool
        val args = buildJsonObject {
            put("owner", owner)
            put("repo", repo)
            put("pull_number", prNumber)
            put("path", filePath)
        }

        // Call MCP tool
        val result = mcpRepository.callTool(mcpServerUrl, "pr.file_content", args)
            .getOrThrow()

        // Extract content string from result
        result.jsonObject["content"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("No content found in MCP response for file: $filePath")
    }

    /**
     * Parses GitHub PR URL to extract owner, repo, and PR number
     *
     * Expected format: https://github.com/owner/repo/pull/123
     *
     * @param prUrl GitHub PR URL
     * @return Triple of (owner, repo, prNumber)
     * @throws IllegalArgumentException if URL format is invalid
     */
    private fun parsePrUrl(prUrl: String): Triple<String, String, Int> {
        val regex = Regex("https://github\\.com/([^/]+)/([^/]+)/pull/(\\d+)")
        val matchResult = regex.find(prUrl)
            ?: throw IllegalArgumentException("Invalid GitHub PR URL format. Expected: https://github.com/owner/repo/pull/123")

        val (owner, repo, prNumberStr) = matchResult.destructured
        val prNumber = prNumberStr.toIntOrNull()
            ?: throw IllegalArgumentException("Invalid PR number: $prNumberStr")

        return Triple(owner, repo, prNumber)
    }
}
