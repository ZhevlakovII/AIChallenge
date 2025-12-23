package ru.izhxx.aichallenge.tools.llm.completions.api.model.answer

import ru.izhxx.aichallenge.tools.llm.completions.api.model.MessageRole
import ru.izhxx.aichallenge.tools.shared.mcp.model.McpCallTool

/**
 * Сообщение-ответ от LLM (часть [Choice]).
 * Содержит текстовый ответ и опциональные вызовы функций (tool calls).
 *
 * @property role Роль сообщения (обычно [MessageRole.Assistant]).
 * @property content Текстовое содержимое ответа LLM.
 * @property toolCalls Список запросов на вызов функций (MCP tools), если LLM запросила их выполнение.
 *
 * @see Choice
 * @see MessageRole
 * @see McpCallTool
 */
class AnswerMessage(
    val role: MessageRole,
    val content: String,
    val toolCalls: List<McpCallTool>
)