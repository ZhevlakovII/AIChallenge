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
}
