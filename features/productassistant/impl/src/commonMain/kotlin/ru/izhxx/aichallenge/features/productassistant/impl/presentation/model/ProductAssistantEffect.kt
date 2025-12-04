package ru.izhxx.aichallenge.features.productassistant.impl.presentation.model

import ru.izhxx.aichallenge.core.ui.mvi.model.MviEffect

/**
 * One-time side effects for Product Assistant screen
 */
sealed interface ProductAssistantEffect : MviEffect {
    /**
     * Show a message to the user (toast/snackbar)
     */
    data class ShowMessage(val message: String) : ProductAssistantEffect

    /**
     * Navigate to ticket details
     */
    data class NavigateToTicket(val ticketId: String) : ProductAssistantEffect

    /**
     * Scroll to response
     */
    data object ScrollToResponse : ProductAssistantEffect
}
