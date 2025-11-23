package ru.izhxx.aichallenge.data.usecase

import ru.izhxx.aichallenge.common.Logger
import ru.izhxx.aichallenge.common.safeApiCall
import ru.izhxx.aichallenge.domain.model.MessageRole
import ru.izhxx.aichallenge.domain.model.message.LLMMessage
import ru.izhxx.aichallenge.domain.model.reminder.ReminderResult
import ru.izhxx.aichallenge.domain.model.reminder.ReminderStatus
import ru.izhxx.aichallenge.domain.model.reminder.ReminderTask
import ru.izhxx.aichallenge.domain.repository.LLMClientRepository
import ru.izhxx.aichallenge.domain.usecase.ExecuteReminderTaskUseCase

/**
 * Реализация UseCase для выполнения одной задачи-напоминания.
 *
 * Отправляет промпт в LLM с кастомным системным промптом (из задачи) и формирует
 * доменную модель результата. Сохранение результата, обновление расписания и
 * уведомления выполняются на уровне движка (ReminderEngine).
 */
class ExecuteReminderTaskUseCaseImpl(
    private val llmClientRepository: LLMClientRepository
) : ExecuteReminderTaskUseCase {

    private val logger = Logger.forClass(this)

    /**
     * Выполняет задачу и возвращает результат.
     *
     * - На успехе: ReminderResult со статусом SUCCESS и текстом ответа.
     * - На ошибке: ReminderResult со статусом ERROR и сообщением об ошибке.
     */
    override suspend fun invoke(task: ReminderTask): Result<ReminderResult> = safeApiCall(logger) {
        val now = System.currentTimeMillis()
        logger.i("Выполнение задачи reminder id=${task.id} name=\"${task.name}\"")

        // Подготовка сообщений: пользовательский промпт уходит как user-сообщение
        val userMessage = LLMMessage(
            role = MessageRole.USER,
            content = task.userPrompt
        )

        val llmResult = llmClientRepository.sendMessagesWithCustomSystem(
            systemPrompt = task.systemPrompt,
            messages = listOf(userMessage),
            summary = null
        )

        llmResult.fold(
            onSuccess = { response ->
                val content = response.choices.firstOrNull()?.rawMessage?.content?.takeIf { it.isNotBlank() }
                    ?: "(пустой ответ LLM)"

                ReminderResult(
                    id = null,
                    taskId = requireNotNull(task.id) { "Task id is required to run task" },
                    runAt = now,
                    status = ReminderStatus.SUCCESS,
                    responseText = content,
                    rawToolTrace = null,
                    errorMessage = null
                )
            },
            onFailure = { error ->
                logger.e("Ошибка выполнения задачи reminder id=${task.id}", error)
                ReminderResult(
                    id = null,
                    taskId = requireNotNull(task.id) { "Task id is required to run task" },
                    runAt = now,
                    status = ReminderStatus.ERROR,
                    responseText = "",
                    rawToolTrace = null,
                    errorMessage = error.message
                )
            }
        )
    }
}
