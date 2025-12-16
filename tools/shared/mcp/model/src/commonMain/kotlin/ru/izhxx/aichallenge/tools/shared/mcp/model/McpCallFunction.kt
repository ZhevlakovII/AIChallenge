package ru.izhxx.aichallenge.tools.shared.mcp.model

import kotlinx.serialization.json.JsonObject

// TODO(заполнить документацию)
class McpCallFunction(
    val name: String,
    val arguments: JsonObject?,
)