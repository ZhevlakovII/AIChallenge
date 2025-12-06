package ru.izhxx.aichallenge.instruments.llm.interactions.api.model.answer

import ru.izhxx.aichallenge.instruments.llm.config.mcp.model.McpCallTool
import ru.izhxx.aichallenge.instruments.llm.interactions.api.model.MessageRole

class AnswerMessage(
    val role: MessageRole,
    val content: String,
    val toolCalls: List<McpCallTool>
)