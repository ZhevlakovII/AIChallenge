package ru.izhxx.aichallenge.tools.llm.completions.api.model.answer

import ru.izhxx.aichallenge.tools.llm.completions.api.model.MessageRole
import ru.izhxx.aichallenge.tools.shared.mcp.model.McpCallTool

class AnswerMessage(
    val role: MessageRole,
    val content: String,
    val toolCalls: List<McpCallTool>
)