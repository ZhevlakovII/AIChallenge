package ru.izhxx.aichallenge.instruments.llm.config.mcp.model

import kotlinx.serialization.json.JsonObject

// TODO(заполнить документацию)
class McpFunction(
    val name: String,
    val description: String,
    val parameters: JsonObject?,
)