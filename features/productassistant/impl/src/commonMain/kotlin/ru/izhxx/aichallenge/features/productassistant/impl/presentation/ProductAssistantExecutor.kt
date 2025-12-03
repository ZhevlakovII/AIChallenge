package ru.izhxx.aichallenge.features.productassistant.impl.presentation

import ru.izhxx.aichallenge.core.ui.mvi.runtime.MviExecutor
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.AssistantMode
import ru.izhxx.aichallenge.features.productassistant.impl.domain.usecase.GenerateAnswerUseCase
import ru.izhxx.aichallenge.features.productassistant.impl.presentation.model.ProductAssistantEffect
import ru.izhxx.aichallenge.features.productassistant.impl.presentation.model.ProductAssistantIntent
import ru.izhxx.aichallenge.features.productassistant.impl.presentation.model.ProductAssistantResult

/**
 * MVI Executor for Product Assistant
 * Handles all side effects and business logic execution
 */
class ProductAssistantExecutor(
    private val generateAnswerUseCase: GenerateAnswerUseCase
) : MviExecutor<ProductAssistantIntent, ProductAssistantResult, ProductAssistantEffect> {

    // Cache for current state values needed for async operations
    private var currentQuery: String = ""
    private var currentMode: AssistantMode = AssistantMode.FULL

    override suspend fun execute(
        intent: ProductAssistantIntent,
        emitResult: suspend (ProductAssistantResult) -> Unit,
        emitEffect: suspend (ProductAssistantEffect) -> Unit
    ) {
        when (intent) {
            is ProductAssistantIntent.QueryChanged -> handleQueryChanged(intent.query, emitResult)
            is ProductAssistantIntent.ModeChanged -> handleModeChanged(intent.mode, emitResult)
            is ProductAssistantIntent.AskQuestion -> handleAskQuestion(emitResult, emitEffect)
            is ProductAssistantIntent.ViewTicket -> handleViewTicket(intent.ticketId, emitResult, emitEffect)
            is ProductAssistantIntent.ClearResponse -> handleClearResponse(emitResult)
            is ProductAssistantIntent.Retry -> handleRetry(emitResult, emitEffect)
        }
    }

    private suspend fun handleQueryChanged(
        query: String,
        emitResult: suspend (ProductAssistantResult) -> Unit
    ) {
        currentQuery = query
        emitResult(ProductAssistantResult.QueryUpdated(query))
    }

    private suspend fun handleModeChanged(
        mode: AssistantMode,
        emitResult: suspend (ProductAssistantResult) -> Unit
    ) {
        currentMode = mode
        emitResult(ProductAssistantResult.ModeUpdated(mode))
    }

    private suspend fun handleAskQuestion(
        emitResult: suspend (ProductAssistantResult) -> Unit,
        emitEffect: suspend (ProductAssistantEffect) -> Unit
    ) {
        if (currentQuery.isBlank()) {
            emitEffect(ProductAssistantEffect.ShowMessage("Введите вопрос"))
            return
        }

        try {
            emitResult(ProductAssistantResult.LoadingStarted)

            val result = generateAnswerUseCase(
                query = currentQuery,
                mode = currentMode,
                ticketId = null
            )

            if (result.isSuccess) {
                val response = result.getOrThrow()
                emitResult(ProductAssistantResult.AnswerReceived(response))
                emitEffect(ProductAssistantEffect.ScrollToResponse)
            } else {
                val error = result.exceptionOrNull()
                emitResult(ProductAssistantResult.ErrorOccurred(
                    error?.message ?: "Произошла неизвестная ошибка"
                ))
                emitEffect(ProductAssistantEffect.ShowMessage("Ошибка: ${error?.message}"))
            }
        } catch (e: Exception) {
            emitResult(ProductAssistantResult.ErrorOccurred(e.message ?: "Неизвестная ошибка"))
            emitEffect(ProductAssistantEffect.ShowMessage("Ошибка: ${e.message}"))
        }
    }

    private suspend fun handleViewTicket(
        ticketId: String,
        emitResult: suspend (ProductAssistantResult) -> Unit,
        emitEffect: suspend (ProductAssistantEffect) -> Unit
    ) {
        emitResult(ProductAssistantResult.ViewTicketRequested(ticketId))
        emitEffect(ProductAssistantEffect.NavigateToTicket(ticketId))
    }

    private suspend fun handleClearResponse(
        emitResult: suspend (ProductAssistantResult) -> Unit
    ) {
        emitResult(ProductAssistantResult.ResponseCleared)
    }

    private suspend fun handleRetry(
        emitResult: suspend (ProductAssistantResult) -> Unit,
        emitEffect: suspend (ProductAssistantEffect) -> Unit
    ) {
        if (currentQuery.isBlank()) {
            emitEffect(ProductAssistantEffect.ShowMessage("Введите вопрос"))
            return
        }

        // Retry is same as asking the question again
        handleAskQuestion(emitResult, emitEffect)
    }
}
