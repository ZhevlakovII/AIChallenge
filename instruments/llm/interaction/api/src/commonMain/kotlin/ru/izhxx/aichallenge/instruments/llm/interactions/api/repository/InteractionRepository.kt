package ru.izhxx.aichallenge.instruments.llm.interactions.api.repository

import ru.izhxx.aichallenge.core.foundation.result.AppResult
import ru.izhxx.aichallenge.instruments.llm.config.mcp.model.McpTool
import ru.izhxx.aichallenge.instruments.llm.config.parameters.model.ParametersConfig
import ru.izhxx.aichallenge.instruments.llm.config.provider.model.ProviderConfig
import ru.izhxx.aichallenge.instruments.llm.interactions.api.model.Answer
import ru.izhxx.aichallenge.instruments.llm.interactions.api.model.Message

// TODO(заполнить документацию)
interface InteractionRepository {

    suspend fun sendMessage(
        parametersConfig: ParametersConfig,
        providerConfig: ProviderConfig,
        messages: List<Message>,
        tools: List<McpTool>
    ): AppResult<Answer>
}