package ru.izhxx.aichallenge.instruments.llm.config.mcp.model

import kotlinx.serialization.json.JsonObject

// TODO(заполнить документацию)
class McpCallFunction(
    val name: String,
    val arguments: JsonObject?,
)