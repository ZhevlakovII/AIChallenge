package ru.izhxx.aichallenge.mcp.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import ru.izhxx.aichallenge.data.model.LlmFunctionDTO
import ru.izhxx.aichallenge.data.model.LlmToolSchemaDTO
import ru.izhxx.aichallenge.mcp.data.model.McpToolDTO
import ru.izhxx.aichallenge.mcp.domain.model.McpTool

/**
 * Маппер инструментов MCP в схемы инструментов LLM (OpenAI-style tools).
 *
 * Переносит название, описание и JSON Schema входных параметров.
 */
class McpToLlmToolsMapper(
    private val json: Json
) {
    /**
     * Преобразует список MCP-инструментов (DTO) в список LLM tools.
     *
     * @param tools список инструментов MCP (DTO)
     * @return список инструментов для передачи в LLM (OpenAI tools)
     */
    fun map(tools: List<McpToolDTO>): List<LlmToolSchemaDTO> {
        return tools.map { dto ->
            LlmToolSchemaDTO(
                function = LlmFunctionDTO(
                    name = dto.name,
                    description = dto.description,
                    parameters = dto.inputSchema?.let { asJsonElement(it) }
                )
            )
        }
    }

    private fun asJsonElement(input: JsonElement): JsonElement = input

    /**
     * Преобразует список доменных MCP-инструментов в список LLM tools.
     *
     * @param tools список доменных инструментов MCP
     * @return список инструментов для передачи в LLM (OpenAI tools)
     */
    fun mapDomain(tools: List<McpTool>): List<LlmToolSchemaDTO> {
        return tools.map { tool ->
            LlmToolSchemaDTO(
                function = LlmFunctionDTO(
                    name = tool.name,
                    description = tool.description,
                    parameters = tool.inputSchema?.let { json.parseToJsonElement(it) }
                )
            )
        }
    }
}
