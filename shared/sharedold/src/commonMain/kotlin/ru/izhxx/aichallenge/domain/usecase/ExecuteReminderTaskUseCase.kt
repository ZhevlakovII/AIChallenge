package ru.izhxx.aichallenge.domain.usecase

import ru.izhxx.aichallenge.domain.model.reminder.ReminderResult
import ru.izhxx.aichallenge.domain.model.reminder.ReminderTask

/**
 * UseCase для выполнения одной задачи-напоминания.
 *
 * Выполняет запрос к LLM (с кастомным системным промптом задачи), подготавливает
 * результат для сохранения. Сохранение и уведомление выполняются на уровне движка.
 */
interface ExecuteReminderTaskUseCase {
    /**
     * Выполняет задачу и возвращает результат (успех/ошибка с текстом ответа/сообщением об ошибке).
     */
    suspend operator fun invoke(task: ReminderTask): Result<ReminderResult>
}
