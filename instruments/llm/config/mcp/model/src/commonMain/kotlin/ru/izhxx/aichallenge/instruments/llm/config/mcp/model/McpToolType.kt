package ru.izhxx.aichallenge.instruments.llm.config.mcp.model

// TODO(заполнить документацию)
enum class McpToolType(val key: String) {
    Function("function");

    companion object {
        fun parseType(statusString: String): McpToolType {
            return entries.first { status -> statusString.lowercase() == status.key.lowercase() }
        }
    }
}
