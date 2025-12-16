package ru.izhxx.aichallenge.tools.llm.completions.api.repository

import ru.izhxx.aichallenge.core.result.AppResult
import ru.izhxx.aichallenge.tools.llm.completions.api.model.Message
import ru.izhxx.aichallenge.tools.llm.completions.api.model.answer.Answer
import ru.izhxx.aichallenge.tools.llm.config.model.ParametersConfig
import ru.izhxx.aichallenge.tools.llm.config.model.ProviderConfig
import ru.izhxx.aichallenge.tools.shared.mcp.model.McpTool

interface CompletionsApiRepository {

    suspend fun sendMessage(
        parametersConfig: ParametersConfig,
        providerConfig: ProviderConfig,
        messages: List<Message>,
        tools: List<McpTool>
    ): AppResult<Answer>
}