package ru.izhxx.aichallenge.features.productassistant.impl.presentation

import ru.izhxx.aichallenge.features.productassistant.impl.presentation.mapper.ProductAssistantUiMapper
import ru.izhxx.aichallenge.features.productassistant.impl.presentation.model.ProductAssistantResult
import ru.izhxx.aichallenge.features.productassistant.impl.presentation.model.ProductAssistantState

/**
 * MVI Reducer for Product Assistant
 * Handles state updates based on results from business logic
 */
class ProductAssistantReducer : MviReducer<ProductAssistantState, ProductAssistantResult> {

    private val mapper = ProductAssistantUiMapper()

    override fun reduce(
        state: ProductAssistantState,
        result: ProductAssistantResult
    ): ProductAssistantState {
        return when (result) {
            is ProductAssistantResult.QueryUpdated -> {
                state.copy(query = result.query)
            }

            is ProductAssistantResult.ModeUpdated -> {
                state.copy(selectedMode = result.mode)
            }

            is ProductAssistantResult.LoadingStarted -> {
                state.copy(
                    isLoading = true,
                    error = null,
                    isInputEnabled = false
                )
            }

            is ProductAssistantResult.AnswerReceived -> {
                state.copy(
                    isLoading = false,
                    response = mapper.toUi(result.response),
                    error = null,
                    isInputEnabled = true
                )
            }

            is ProductAssistantResult.ErrorOccurred -> {
                state.copy(
                    isLoading = false,
                    error = result.message,
                    isInputEnabled = true
                )
            }

            is ProductAssistantResult.ResponseCleared -> {
                state.copy(
                    response = null,
                    error = null
                )
            }

            is ProductAssistantResult.ViewTicketRequested -> {
                // This is handled as an effect, no state change needed
                state
            }

            is ProductAssistantResult.CreateTicketFormVisibilityChanged -> {
                state.copy(
                    showCreateTicketForm = result.isVisible,
                    error = null
                )
            }

            is ProductAssistantResult.TicketFormFieldUpdated -> {
                when (result) {
                    is ProductAssistantResult.TicketFormFieldUpdated.TitleChanged -> {
                        state.copy(ticketTitle = result.title)
                    }

                    is ProductAssistantResult.TicketFormFieldUpdated.DescriptionChanged -> {
                        state.copy(ticketDescription = result.description)
                    }

                    is ProductAssistantResult.TicketFormFieldUpdated.TagsChanged -> {
                        state.copy(ticketTags = result.tags)
                    }
                }
            }

            is ProductAssistantResult.TicketCreationStarted -> {
                state.copy(
                    isLoading = true,
                    error = null,
                    isInputEnabled = false
                )
            }

            is ProductAssistantResult.TicketCreated -> {
                state.copy(
                    isLoading = false,
                    isInputEnabled = true,
                    showCreateTicketForm = false,
                    ticketTitle = "",
                    ticketDescription = "",
                    ticketTags = ""
                )
            }

            is ProductAssistantResult.TicketUpdated -> {
                state.copy(
                    isLoading = false,
                    isInputEnabled = true
                )
            }

            is ProductAssistantResult.TicketOperationFailed -> {
                state.copy(
                    isLoading = false,
                    error = result.message,
                    isInputEnabled = true
                )
            }
        }
    }
}
