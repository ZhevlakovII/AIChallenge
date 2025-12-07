package ru.izhxx.aichallenge.features.productassistant.impl.presentation.model

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

    /**
     * Ticket creation form visibility changed
     */
    data class CreateTicketFormVisibilityChanged(val isVisible: Boolean) : ProductAssistantResult

    /**
     * Ticket form field updated
     */
    sealed interface TicketFormFieldUpdated : ProductAssistantResult {
        data class TitleChanged(val title: String) : TicketFormFieldUpdated
        data class DescriptionChanged(val description: String) : TicketFormFieldUpdated
        data class TagsChanged(val tags: String) : TicketFormFieldUpdated
    }

    /**
     * Ticket creation started
     */
    data object TicketCreationStarted : ProductAssistantResult

    /**
     * Ticket creation completed successfully
     */
    data class TicketCreated(val ticketId: String) : ProductAssistantResult

    /**
     * Ticket update completed successfully
     */
    data class TicketUpdated(val ticketId: String) : ProductAssistantResult

    /**
     * Ticket operation failed
     */
    data class TicketOperationFailed(val message: String) : ProductAssistantResult
}
