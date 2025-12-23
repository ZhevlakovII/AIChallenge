package ru.izhxx.aichallenge.tools.shared.mcp.model

/**
 * Тип MCP инструмента (Tool Type).
 * Определяет категорию инструмента для LLM в рамках Model Context Protocol.
 *
 * @property key Строковое представление типа для сериализации.
 *
 * @see McpTool
 */
enum class McpToolType(val key: String) {
    /**
     * Функция (Function Calling).
     * Позволяет LLM вызывать функции с параметрами.
     */
    Function("function");

    companion object {
        /**
         * Парсит строку в [McpToolType].
         *
         * @param statusString Строковое представление типа (регистронезависимо).
         * @return Соответствующий [McpToolType].
         * @throws NoSuchElementException Если тип не найден.
         */
        fun parseType(statusString: String): McpToolType {
            return entries.first { status -> statusString.equals(status.key, ignoreCase = true) }
        }
    }
}
