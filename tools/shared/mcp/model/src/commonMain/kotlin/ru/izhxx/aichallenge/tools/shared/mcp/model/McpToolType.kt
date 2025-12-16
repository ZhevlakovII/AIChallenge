package ru.izhxx.aichallenge.tools.shared.mcp.model

// TODO(заполнить документацию)
enum class McpToolType(val key: String) {
    Function("function");

    companion object {
        fun parseType(statusString: String): McpToolType {
            return entries.first { status -> statusString.equals(status.key, ignoreCase = true) }
        }
    }
}
