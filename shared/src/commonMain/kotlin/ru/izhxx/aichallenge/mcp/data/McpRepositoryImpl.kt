package ru.izhxx.aichallenge.mcp.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import ru.izhxx.aichallenge.common.Logger
import ru.izhxx.aichallenge.common.safeApiCall
import ru.izhxx.aichallenge.data.model.github.RepoDTO
import ru.izhxx.aichallenge.data.model.github.toDomain
import ru.izhxx.aichallenge.domain.model.github.Repo
import ru.izhxx.aichallenge.mcp.data.transport.McpWebSocketClient
import ru.izhxx.aichallenge.mcp.domain.model.McpTool
import ru.izhxx.aichallenge.mcp.domain.repository.McpRepository

/**
 * Реализация репозитория MCP.
 */
class McpRepositoryImpl(
    private val transport: McpWebSocketClient,
    private val json: Json
) : McpRepository {

    private val logger = Logger("MCP-Repo")

    override suspend fun listTools(wsUrl: String): Result<List<McpTool>> {
        return safeApiCall(logger) {
            transport.listTools(wsUrl).getOrThrow()
        }.map { result ->
            result.tools.map { dto ->
                McpTool(
                    name = dto.name,
                    description = dto.description,
                    inputSchema = dto.inputSchema?.let { compact(it) }
                )
            }.also { tools ->
                logger.i("Mapped ${tools.size} tools")
            }
        }
    }

    override suspend fun callTool(
        wsUrl: String,
        name: String,
        arguments: JsonElement
    ): Result<JsonElement> {
        return safeApiCall(logger) {
            transport.callTool(wsUrl, name, arguments).getOrThrow()
        }
    }

    override suspend fun callListUserRepos(
        wsUrl: String,
        username: String,
        perPage: Int,
        sort: String
    ): Result<List<Repo>> {
        return safeApiCall(logger) {
            val args: JsonElement = json.parseToJsonElement(
                """
                {
                  "username": ${json.encodeToString(username)},
                  "per_page": $perPage,
                  "sort": ${json.encodeToString(sort)}
                }
                """.trimIndent()
            )
            transport.callTool(wsUrl, "github.list_user_repos", args).getOrThrow()
        }.map { resultEl ->
            decodeRepos(resultEl)
        }
    }

    override suspend fun callListMyRepos(
        wsUrl: String,
        perPage: Int,
        sort: String,
        visibility: String
    ): Result<List<Repo>> {
        return safeApiCall(logger) {
            val args: JsonElement = json.parseToJsonElement(
                """
                {
                  "per_page": $perPage,
                  "sort": ${json.encodeToString(sort)},
                  "visibility": ${json.encodeToString(visibility)}
                }
                """.trimIndent()
            )
            transport.callTool(wsUrl, "github.list_my_repos", args).getOrThrow()
        }.map { resultEl ->
            decodeRepos(resultEl)
        }
    }

    private fun decodeRepos(resultEl: JsonElement): List<Repo> {
        val obj = resultEl as? JsonObject
            ?: error("Invalid tools/call result format: expected object")
        val itemsEl = obj["items"] ?: error("tools/call result missing 'items'")
        val array = itemsEl as? JsonArray
            ?: error("'items' must be array")
        val dtoList: List<RepoDTO> = json.decodeFromJsonElement(array)
        return dtoList.map { it.toDomain() }.also {
            logger.i("Decoded ${it.size} repos")
        }
    }

    private fun compact(element: JsonElement): String {
        // Компактная строка без лишних пробелов
        return json.encodeToString(element)
    }
}
