package ru.izhxx.aichallenge.features.productassistant.impl.presentation.model

import ru.izhxx.aichallenge.core.ui.mvi.model.MviIntent
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.AssistantMode

/**
 * User intents for Product Assistant screen
 */
sealed interface ProductAssistantIntent : MviIntent {
    /**
     * User changed the query input
     */
    data class QueryChanged(val query: String) : ProductAssistantIntent

    /**
     * User selected a different assistant mode
     */
    data class ModeChanged(val mode: AssistantMode) : ProductAssistantIntent

    /**
     * User clicked the "Ask" button
     */
    data object AskQuestion : ProductAssistantIntent

    /**
     * User clicked to view ticket details
     */
    data class ViewTicket(val ticketId: String) : ProductAssistantIntent

    /**
     * User clicked to clear the response
     */
    data object ClearResponse : ProductAssistantIntent

    /**
     * User wants to retry after an error
     */
    data object Retry : ProductAssistantIntent

    /**
     * User wants to create a new ticket
     */
    data class CreateTicket(
        val title: String,
        val description: String,
        val tags: List<String> = emptyList()
    ) : ProductAssistantIntent

    /**
     * User wants to update a ticket
     */
    data class UpdateTicket(
        val ticketId: String,
        val newStatus: String? = null,
        val comment: String? = null
    ) : ProductAssistantIntent

    /**
     * User wants to show ticket creation form
     */
    data object ShowCreateTicketForm : ProductAssistantIntent

    /**
     * User wants to hide ticket creation form
     */
    data object HideCreateTicketForm : ProductAssistantIntent

    /**
     * User changed ticket title in creation form
     */
    data class TicketTitleChanged(val title: String) : ProductAssistantIntent

    /**
     * User changed ticket description in creation form
     */
    data class TicketDescriptionChanged(val description: String) : ProductAssistantIntent

    /**
     * User changed ticket tags in creation form
     */
    data class TicketTagsChanged(val tags: String) : ProductAssistantIntent
}
