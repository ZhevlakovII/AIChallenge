package ru.izhxx.aichallenge.features.productassistant.impl.domain.usecase

import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.AssistantMode
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.AssistantQuery
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.AssistantResponse

/**
 * Use case for generating an answer using LLM with RAG and MCP context
 */
interface GenerateAnswerUseCase {
    /**
     * Generate an answer for the user query
     *
     * This use case orchestrates:
     * - Searching FAQ (Mode A or C)
     * - Searching tickets (Mode B or C)
     * - Generating LLM response with context
     *
     * @param query User query text
     * @param mode Assistant mode (FAQ_ONLY, TICKET_ANALYSIS, FULL)
     * @param ticketId Optional ticket ID for specific ticket analysis
     * @return Result containing the assistant response
     */
    suspend operator fun invoke(
        query: String,
        mode: AssistantMode = AssistantMode.FULL,
        ticketId: String? = null
    ): Result<AssistantResponse>
}
