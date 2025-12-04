package ru.izhxx.aichallenge.features.productassistant.impl.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import ru.izhxx.aichallenge.core.ui.mvi.runtime.MviReducer
import ru.izhxx.aichallenge.core.ui.mvi.runtime.MviViewModel
import ru.izhxx.aichallenge.features.productassistant.impl.presentation.mapper.ProductAssistantUiMapper
import ru.izhxx.aichallenge.features.productassistant.impl.presentation.model.ProductAssistantEffect
import ru.izhxx.aichallenge.features.productassistant.impl.presentation.model.ProductAssistantIntent
import ru.izhxx.aichallenge.features.productassistant.impl.presentation.model.ProductAssistantResult
import ru.izhxx.aichallenge.features.productassistant.impl.presentation.model.ProductAssistantState

/**
 * MVI ViewModel for Product Assistant screen
 *
 * Orchestrates the MVI pipeline:
 * Intent → Executor → Result/Effect → Reducer → State → UI
 */
class ProductAssistantViewModel(
    private val executor: ProductAssistantExecutor,
    private val mapper: ProductAssistantUiMapper
) : ViewModel(), MviViewModel<ProductAssistantIntent, ProductAssistantResult, ProductAssistantState, ProductAssistantEffect> {

    // Initial state
    private val initialState = ProductAssistantState()

    // Mutable state for internal updates
    private val _state = MutableStateFlow(initialState)

    // Public immutable state for UI observation
    override val state: StateFlow<ProductAssistantState> = _state.asStateFlow()

    // Channel for one-time effects
    private val _effects = Channel<ProductAssistantEffect>(Channel.BUFFERED)

    // Public flow of effects for UI observation
    override val effects: Flow<ProductAssistantEffect> = _effects.receiveAsFlow()

    // Pure reducer function for state transformation
    private val reducer = MviReducer<ProductAssistantState, ProductAssistantResult> { currentState, result ->
        when (result) {
            is ProductAssistantResult.QueryUpdated -> {
                currentState.copy(
                    query = result.query,
                    error = null
                )
            }

            is ProductAssistantResult.ModeUpdated -> {
                currentState.copy(
                    selectedMode = result.mode,
                    error = null
                )
            }

            is ProductAssistantResult.LoadingStarted -> {
                currentState.copy(
                    isLoading = true,
                    error = null,
                    isInputEnabled = false
                )
            }

            is ProductAssistantResult.AnswerReceived -> {
                currentState.copy(
                    isLoading = false,
                    response = mapper.toUi(result.response),
                    error = null,
                    isInputEnabled = true
                )
            }

            is ProductAssistantResult.ErrorOccurred -> {
                currentState.copy(
                    isLoading = false,
                    error = result.message,
                    isInputEnabled = true
                )
            }

            is ProductAssistantResult.ResponseCleared -> {
                currentState.copy(
                    response = null,
                    error = null
                )
            }

            is ProductAssistantResult.ViewTicketRequested -> {
                // No state change needed, handled via effect
                currentState
            }
        }
    }

    /**
     * Accepts user intents and processes them through the MVI pipeline
     */
    override fun accept(intent: ProductAssistantIntent) {
        viewModelScope.launch {
            executor.execute(
                intent = intent,
                emitResult = { result ->
                    // Update state through reducer
                    _state.value = reducer.reduce(_state.value, result)
                },
                emitEffect = { effect ->
                    // Send one-time effect to channel
                    _effects.send(effect)
                }
            )
        }
    }
}
