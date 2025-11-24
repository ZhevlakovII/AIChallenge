package ru.izhxx.aichallenge.data.repository

import ru.izhxx.aichallenge.common.Logger
import ru.izhxx.aichallenge.common.safeApiCall
import ru.izhxx.aichallenge.data.database.AppDatabase
import ru.izhxx.aichallenge.data.mapper.toDomain
import ru.izhxx.aichallenge.data.mapper.toEntity
import ru.izhxx.aichallenge.domain.model.reminder.ReminderResult
import ru.izhxx.aichallenge.domain.model.reminder.ReminderTask
import ru.izhxx.aichallenge.domain.repository.ReminderRepository

/**
 * Реализация репозитория для работы с задачами-напоминаниями и их результатами.
 */
class ReminderRepositoryImpl(
    private val database: AppDatabase
) : ReminderRepository {

    private val logger = Logger.forClass(this)

    override suspend fun createTask(task: ReminderTask): Result<Long> = safeApiCall(logger) {
        val now = System.currentTimeMillis()
        val entity = task.copy(
            createdAt = task.createdAt.takeIf { it > 0 } ?: now,
            updatedAt = now
        ).toEntity()

        val id = database.reminderTaskDao().upsertTask(entity)
        logger.i("Создана задача-напоминание id=$id, name=\"${task.name}\"")
        id
    }

    override suspend fun updateTask(task: ReminderTask): Result<Unit> = safeApiCall(logger) {
        requireNotNull(task.id) { "Task id is required for update" }
        val now = System.currentTimeMillis()
        val entity = task.copy(updatedAt = now).toEntity()
        database.reminderTaskDao().upsertTask(entity)
        logger.i("Обновлена задача-напоминание id=${task.id}")
    }

    override suspend fun deleteTask(id: Long): Result<Unit> = safeApiCall(logger) {
        database.reminderTaskDao().deleteById(id)
        logger.i("Удалена задача-напоминание id=$id")
    }

    override suspend fun getTasks(): Result<List<ReminderTask>> = safeApiCall(logger) {
        database.reminderTaskDao().getAll().map { it.toDomain() }
    }

    override suspend fun getTask(id: Long): Result<ReminderTask?> = safeApiCall(logger) {
        database.reminderTaskDao().getById(id)?.toDomain()
    }

    override suspend fun getDueTasks(nowMillis: Long): Result<List<ReminderTask>> = safeApiCall(logger) {
        database.reminderTaskDao().getDueTasks(nowMillis).map { it.toDomain() }
    }

    override suspend fun updateSchedule(taskId: Long, lastRunAt: Long?, nextRunAt: Long?): Result<Unit> =
        safeApiCall(logger) {
            val now = System.currentTimeMillis()
            database.reminderTaskDao().updateSchedule(taskId, lastRunAt, nextRunAt, now)
            logger.i("Обновлено расписание задачи id=$taskId: last=$lastRunAt, next=$nextRunAt")
        }

    override suspend fun saveResult(result: ReminderResult): Result<Long> = safeApiCall(logger) {
        val id = database.reminderResultDao().insert(result.toEntity())
        logger.i("Сохранён результат напоминания id=$id для taskId=${result.taskId} со статусом=${result.status}")
        id
    }

    override suspend fun getResults(taskId: Long, limit: Int, offset: Int): Result<List<ReminderResult>> =
        safeApiCall(logger) {
            database.reminderResultDao().getByTask(taskId, limit, offset).map { it.toDomain() }
        }

    override suspend fun getLatestResults(limit: Int): Result<List<ReminderResult>> = safeApiCall(logger) {
        database.reminderResultDao().getLatest(limit).map { it.toDomain() }
    }
}
