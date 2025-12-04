package ru.izhxx.aichallenge.features.productassistant.impl.presentation.model

import ru.izhxx.aichallenge.core.ui.mvi.model.MviResult
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.AssistantMode
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.AssistantResponse

/**
 * Results from business logic operations
 */
sealed interface ProductAssistantResult : MviResult {
    /**
     * Query input changed
     */
    data class QueryUpdated(val query: String) : ProductAssistantResult

    /**
     * Mode selection changed
     */
    data class ModeUpdated(val mode: AssistantMode) : ProductAssistantResult

    /**
     * Started loading answer
     */
    data object LoadingStarted : ProductAssistantResult

    /**
     * Successfully received answer
     */
    data class AnswerReceived(val response: AssistantResponse) : ProductAssistantResult

    /**
     * Error occurred
     */
    data class ErrorOccurred(val message: String) : ProductAssistantResult

    /**
     * Response cleared
     */
    data object ResponseCleared : ProductAssistantResult

    /**
     * Ticket view requested
     */
    data class ViewTicketRequested(val ticketId: String) : ProductAssistantResult
}
