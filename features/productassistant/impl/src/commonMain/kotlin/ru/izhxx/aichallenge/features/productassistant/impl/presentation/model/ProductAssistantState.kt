package ru.izhxx.aichallenge.features.productassistant.impl.presentation.model

import ru.izhxx.aichallenge.core.ui.mvi.model.MviState
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.AssistantMode

/**
 * UI state for Product Assistant screen
 */
data class ProductAssistantState(
    val query: String = "",
    val selectedMode: AssistantMode = AssistantMode.FULL,
    val isLoading: Boolean = false,
    val response: AssistantResponseUi? = null,
    val error: String? = null,
    val isInputEnabled: Boolean = true
) : MviState

/**
 * UI model for assistant response
 */
data class AssistantResponseUi(
    val answer: String,
    val mode: AssistantMode,
    val relatedTickets: List<TicketUi>,
    val relatedDocumentation: List<FaqItemUi>,
    val confidence: Double,
    val sources: List<SourceUi>
)

/**
 * UI model for a ticket
 */
data class TicketUi(
    val id: String,
    val title: String,
    val description: String,
    val status: String,
    val statusColor: Long,
    val tags: List<String>,
    val createdAt: String
)

/**
 * UI model for FAQ item
 */
data class FaqItemUi(
    val question: String,
    val answer: String,
    val category: String,
    val relevanceScore: Double
)

/**
 * UI model for response source
 */
data class SourceUi(
    val type: String,
    val typeDisplayName: String,
    val reference: String,
    val excerpt: String?
)
