package ru.izhxx.aichallenge.features.productassistant.impl.presentation

import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.AssistantMode
import ru.izhxx.aichallenge.features.productassistant.impl.domain.model.TicketStatus
import ru.izhxx.aichallenge.features.productassistant.impl.domain.usecase.CreateTicketUseCase
import ru.izhxx.aichallenge.features.productassistant.impl.domain.usecase.GenerateAnswerUseCase
import ru.izhxx.aichallenge.features.productassistant.impl.domain.usecase.UpdateTicketUseCase
import ru.izhxx.aichallenge.features.productassistant.impl.presentation.model.ProductAssistantEffect
import ru.izhxx.aichallenge.features.productassistant.impl.presentation.model.ProductAssistantIntent
import ru.izhxx.aichallenge.features.productassistant.impl.presentation.model.ProductAssistantResult
import kotlin.time.ExperimentalTime

/**
 * MVI Executor for Product Assistant
 * Handles all side effects and business logic execution
 */
class ProductAssistantExecutor(
    private val generateAnswerUseCase: GenerateAnswerUseCase,
    private val createTicketUseCase: CreateTicketUseCase,
    private val updateTicketUseCase: UpdateTicketUseCase
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
            is ProductAssistantIntent.CreateTicket -> handleCreateTicket(intent.title, intent.description, intent.tags, emitResult, emitEffect)
            ProductAssistantIntent.HideCreateTicketForm -> handleHideCreateTicketForm(emitResult, emitEffect)
            ProductAssistantIntent.ShowCreateTicketForm -> handleShowCreateTicketForm(emitResult, emitEffect)
            is ProductAssistantIntent.UpdateTicket -> handleUpdateTicket(intent.ticketId, intent.newStatus, intent.comment, emitResult, emitEffect)
            is ProductAssistantIntent.TicketTitleChanged -> handleTicketTitleChanged(intent.title, emitResult)
            is ProductAssistantIntent.TicketDescriptionChanged -> handleTicketDescriptionChanged(intent.description, emitResult)
            is ProductAssistantIntent.TicketTagsChanged -> handleTicketTagsChanged(intent.tags, emitResult)
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

    @OptIn(ExperimentalTime::class)
    private suspend fun handleCreateTicket(
        title: String,
        description: String,
        tags: List<String>,
        emitResult: suspend (ProductAssistantResult) -> Unit,
        emitEffect: suspend (ProductAssistantEffect) -> Unit
    ) {
        if (title.isBlank()) {
            emitEffect(ProductAssistantEffect.ShowMessage("Введите заголовок тикета"))
            return
        }
        if (description.isBlank()) {
            emitEffect(ProductAssistantEffect.ShowMessage("Введите описание тикета"))
            return
        }

        try {
            emitResult(ProductAssistantResult.TicketCreationStarted)

            val result = createTicketUseCase(title, description, tags)

            if (result.isSuccess) {
                val ticket = result.getOrThrow()
                emitResult(ProductAssistantResult.TicketCreated(ticket.id))
                emitEffect(ProductAssistantEffect.ShowMessage("Тикет #${ticket.id.take(8)} успешно создан"))
                emitEffect(ProductAssistantEffect.ClearTicketForm)
                emitEffect(ProductAssistantEffect.HideTicketCreationForm)
            } else {
                val error = result.exceptionOrNull()
                emitResult(ProductAssistantResult.TicketOperationFailed(
                    error?.message ?: "Не удалось создать тикет"
                ))
                emitEffect(ProductAssistantEffect.ShowMessage("Ошибка создания тикета: ${error?.message}"))
            }
        } catch (e: Exception) {
            emitResult(ProductAssistantResult.TicketOperationFailed(e.message ?: "Неизвестная ошибка"))
            emitEffect(ProductAssistantEffect.ShowMessage("Ошибка создания тикета: ${e.message}"))
        }
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun handleUpdateTicket(
        ticketId: String,
        newStatus: String?,
        comment: String?,
        emitResult: suspend (ProductAssistantResult) -> Unit,
        emitEffect: suspend (ProductAssistantEffect) -> Unit
    ) {
        if (ticketId.isBlank()) {
            emitEffect(ProductAssistantEffect.ShowMessage("ID тикета не может быть пустым"))
            return
        }

        if (newStatus.isNullOrBlank() && comment.isNullOrBlank()) {
            emitEffect(ProductAssistantEffect.ShowMessage("Укажите новый статус или комментарий"))
            return
        }

        try {
            val statusEnum = if (!newStatus.isNullOrBlank()) {
                TicketStatus.fromString(newStatus)
            } else null

            val result = updateTicketUseCase(ticketId, statusEnum, comment)

            if (result.isSuccess) {
                val ticket = result.getOrThrow()
                emitResult(ProductAssistantResult.TicketUpdated(ticket.id))
                val message = if (!newStatus.isNullOrBlank() && !comment.isNullOrBlank()) {
                    "Тикет #${ticket.id.take(8)} обновлен: статус изменен на $newStatus, добавлен комментарий"
                } else if (!newStatus.isNullOrBlank()) {
                    "Тикет #${ticket.id.take(8)}: статус изменен на $newStatus"
                } else {
                    "Тикет #${ticket.id.take(8)}: добавлен комментарий"
                }
                emitEffect(ProductAssistantEffect.ShowMessage(message))
            } else {
                val error = result.exceptionOrNull()
                emitResult(ProductAssistantResult.TicketOperationFailed(
                    error?.message ?: "Не удалось обновить тикет"
                ))
                emitEffect(ProductAssistantEffect.ShowMessage("Ошибка обновления тикета: ${error?.message}"))
            }
        } catch (e: Exception) {
            emitResult(ProductAssistantResult.TicketOperationFailed(e.message ?: "Неизвестная ошибка"))
            emitEffect(ProductAssistantEffect.ShowMessage("Ошибка обновления тикета: ${e.message}"))
        }
    }

    private suspend fun handleShowCreateTicketForm(
        emitResult: suspend (ProductAssistantResult) -> Unit,
        emitEffect: suspend (ProductAssistantEffect) -> Unit
    ) {
        emitResult(ProductAssistantResult.CreateTicketFormVisibilityChanged(true))
        emitEffect(ProductAssistantEffect.ShowTicketCreationForm)
        emitEffect(ProductAssistantEffect.FocusOnTicketTitle)
    }

    private suspend fun handleHideCreateTicketForm(
        emitResult: suspend (ProductAssistantResult) -> Unit,
        emitEffect: suspend (ProductAssistantEffect) -> Unit
    ) {
        emitResult(ProductAssistantResult.CreateTicketFormVisibilityChanged(false))
        emitEffect(ProductAssistantEffect.HideTicketCreationForm)
        emitEffect(ProductAssistantEffect.ClearTicketForm)
    }

    private suspend fun handleTicketTitleChanged(
        title: String,
        emitResult: suspend (ProductAssistantResult) -> Unit
    ) {
        emitResult(ProductAssistantResult.TicketFormFieldUpdated.TitleChanged(title))
    }

    private suspend fun handleTicketDescriptionChanged(
        description: String,
        emitResult: suspend (ProductAssistantResult) -> Unit
    ) {
        emitResult(ProductAssistantResult.TicketFormFieldUpdated.DescriptionChanged(description))
    }

    private suspend fun handleTicketTagsChanged(
        tags: String,
        emitResult: suspend (ProductAssistantResult) -> Unit
    ) {
        emitResult(ProductAssistantResult.TicketFormFieldUpdated.TagsChanged(tags))
    }
}
