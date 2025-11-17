package ru.izhxx.aichallenge.mcp.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import ru.izhxx.aichallenge.common.Logger
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
        return transport.listTools(wsUrl).map { result ->
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

    private fun compact(element: JsonElement): String {
        // Компактная строка без лишних пробелов
        return json.encodeToString(element)
    }
}
