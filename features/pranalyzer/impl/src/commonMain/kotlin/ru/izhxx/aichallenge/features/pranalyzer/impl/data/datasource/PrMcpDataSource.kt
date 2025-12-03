package ru.izhxx.aichallenge.features.pranalyzer.impl.data.datasource

import kotlinx.serialization.json.JsonObject

/**
 * Data source for accessing PR information via MCP (Model Context Protocol)
 *
 * Provides methods to interact with GitHub PR data through MCP tools:
 * - pr.info - Get basic PR information
 * - pr.diff - Get full PR diff
 * - pr.files - Get list of changed files
 * - pr.file_content - Get content of a specific file
 */
interface PrMcpDataSource {

    /**
     * Fetches basic information about a Pull Request using MCP pr.info tool
     *
     * @param prUrl Full GitHub PR URL (e.g., https://github.com/owner/repo/pull/123)
     * @return Result containing JsonObject with PR info or error
     */
    suspend fun getPrInfo(prUrl: String): Result<JsonObject>

    /**
     * Fetches the complete diff for a Pull Request using MCP pr.diff tool
     *
     * @param prUrl Full GitHub PR URL
     * @return Result containing diff as String or error
     */
    suspend fun getPrDiff(prUrl: String): Result<String>

    /**
     * Fetches the list of files changed in a Pull Request using MCP pr.files tool
     *
     * @param prUrl Full GitHub PR URL
     * @return Result containing List of JsonObjects representing files or error
     */
    suspend fun getPrFiles(prUrl: String): Result<List<JsonObject>>

    /**
     * Fetches the content of a specific file from a Pull Request using MCP pr.file_content tool
     *
     * @param prUrl Full GitHub PR URL
     * @param filePath Path to the file in the repository
     * @return Result containing file content as String or error
     */
    suspend fun getFileContent(prUrl: String, filePath: String): Result<String>
}
