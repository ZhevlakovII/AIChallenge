package ru.izhxx.aichallenge.domain.repository

import ru.izhxx.aichallenge.domain.model.reminder.ReminderResult
import ru.izhxx.aichallenge.domain.model.reminder.ReminderTask

/**
 * Репозиторий для работы с задачами-напоминаниями и их результатами.
 *
 * Содержит операции CRUD для задач и методы для сохранения/чтения результатов.
 */
interface ReminderRepository {
    /**
     * Создает новую задачу и возвращает её идентификатор.
     */
    suspend fun createTask(task: ReminderTask): Result<Long>

    /**
     * Обновляет существующую задачу.
     */
    suspend fun updateTask(task: ReminderTask): Result<Unit>

    /**
     * Удаляет задачу по идентификатору.
     */
    suspend fun deleteTask(id: Long): Result<Unit>

    /**
     * Возвращает список всех задач (без наблюдения).
     */
    suspend fun getTasks(): Result<List<ReminderTask>>

    /**
     * Возвращает задачу по идентификатору (или null, если не найдена).
     */
    suspend fun getTask(id: Long): Result<ReminderTask?>

    /**
     * Возвращает список задач, у которых наступило время запуска.
     */
    suspend fun getDueTasks(nowMillis: Long): Result<List<ReminderTask>>

    /**
     * Обновляет расписание задачи: время последнего и следующего запуска.
     */
    suspend fun updateSchedule(taskId: Long, lastRunAt: Long?, nextRunAt: Long?): Result<Unit>

    /**
     * Сохраняет результат выполнения задачи и возвращает идентификатор результата.
     */
    suspend fun saveResult(result: ReminderResult): Result<Long>

    /**
     * Возвращает результаты выполнения конкретной задачи.
     */
    suspend fun getResults(taskId: Long, limit: Int, offset: Int): Result<List<ReminderResult>>

    /**
     * Возвращает последние результаты по всем задачам.
     */
    suspend fun getLatestResults(limit: Int): Result<List<ReminderResult>>
}
