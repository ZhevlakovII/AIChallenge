package ru.izhxx.aichallenge.features.pranalyzer.impl.data.datasource

import ru.izhxx.aichallenge.domain.model.MessageRole
import ru.izhxx.aichallenge.domain.model.message.LLMMessage
import ru.izhxx.aichallenge.domain.repository.LLMClientRepository

/**
 * Implementation of LlmAnalysisDataSource using existing LLMClientRepository
 *
 * This data source uses the LLM client to perform code analysis with custom prompts.
 * It leverages the sendMessagesWithCustomSystem method to provide specific instructions
 * for code review tasks.
 */
class LlmAnalysisDataSourceImpl(
    private val llmClientRepository: LLMClientRepository
) : LlmAnalysisDataSource {

    override suspend fun analyzeCode(
        systemPrompt: String,
        userPrompt: String
    ): Result<String> = runCatching {
        // Create user message with the analysis request
        val messages = listOf(
            LLMMessage(
                role = MessageRole.USER,
                content = userPrompt
            )
        )

        // Send to LLM with custom system prompt
        val response = llmClientRepository.sendMessagesWithCustomSystem(
            systemPrompt = systemPrompt,
            messages = messages,
            summary = null
        ).getOrThrow()

        // Extract the content from the first choice
        val content = response.choices.firstOrNull()?.rawMessage?.content
            ?: throw IllegalStateException("Empty response from LLM")

        content
    }
}
