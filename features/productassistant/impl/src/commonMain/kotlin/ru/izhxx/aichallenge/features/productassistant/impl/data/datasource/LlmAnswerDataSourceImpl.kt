package ru.izhxx.aichallenge.features.productassistant.impl.data.datasource

import ru.izhxx.aichallenge.domain.model.MessageRole
import ru.izhxx.aichallenge.domain.model.message.LLMMessage
import ru.izhxx.aichallenge.domain.repository.LLMClientRepository

/**
 * Implementation of LlmAnswerDataSource using LLMClientRepository
 */
class LlmAnswerDataSourceImpl(
    private val llmClientRepository: LLMClientRepository
) : LlmAnswerDataSource {

    override suspend fun generateAnswer(
        systemPrompt: String,
        userPrompt: String
    ): Result<String> {
        return runCatching {
            val messages = listOf(
                LLMMessage(
                    role = MessageRole.USER,
                    content = userPrompt
                )
            )

            // Use LLMClientRepository with custom system prompt
            // This method ignores global system prompt from config and uses our custom one
            val llmResponse = llmClientRepository.sendMessagesWithCustomSystem(
                systemPrompt = systemPrompt,
                messages = messages,
                summary = null
            ).getOrThrow()

            llmResponse.choices.first().rawMessage.content.ifBlank {
                throw IllegalStateException("LLM returned empty response")
            }
        }
    }
}
