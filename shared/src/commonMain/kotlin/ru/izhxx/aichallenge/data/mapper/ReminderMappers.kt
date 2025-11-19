package ru.izhxx.aichallenge.data.mapper

import ru.izhxx.aichallenge.data.database.entity.ReminderResultEntity
import ru.izhxx.aichallenge.data.database.entity.ReminderTaskEntity
import ru.izhxx.aichallenge.domain.model.reminder.ReminderPeriodUnit
import ru.izhxx.aichallenge.domain.model.reminder.ReminderResult
import ru.izhxx.aichallenge.domain.model.reminder.ReminderStatus
import ru.izhxx.aichallenge.domain.model.reminder.ReminderTask

/**
 * Мапперы entity ↔ domain для фичи Reminder.
 */

/**
 * Преобразование сущности задачи в доменную модель.
 */
fun ReminderTaskEntity.toDomain(): ReminderTask {
    val unit = runCatching { ReminderPeriodUnit.valueOf(periodUnit) }.getOrElse { ReminderPeriodUnit.MINUTES }
    return ReminderTask(
        id = id,
        name = name,
        systemPrompt = systemPrompt,
        userPrompt = userPrompt,
        periodUnit = unit,
        periodValue = periodValue,
        enabled = enabled,
        lastRunAt = lastRunAt,
        nextRunAt = nextRunAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

/**
 * Преобразование доменной модели задачи в сущность БД.
 */
fun ReminderTask.toEntity(): ReminderTaskEntity {
    return ReminderTaskEntity(
        id = id ?: 0L,
        name = name,
        systemPrompt = systemPrompt,
        userPrompt = userPrompt,
        periodUnit = periodUnit.name,
        periodValue = periodValue,
        enabled = enabled,
        lastRunAt = lastRunAt,
        nextRunAt = nextRunAt,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

/**
 * Преобразование сущности результата в доменную модель.
 */
fun ReminderResultEntity.toDomain(): ReminderResult {
    val status = runCatching { ReminderStatus.valueOf(this.status) }.getOrElse { ReminderStatus.ERROR }
    return ReminderResult(
        id = id,
        taskId = taskId,
        runAt = runAt,
        status = status,
        responseText = responseText,
        rawToolTrace = rawToolTrace,
        errorMessage = errorMessage
    )
}

/**
 * Преобразование доменной модели результата в сущность БД.
 */
fun ReminderResult.toEntity(): ReminderResultEntity {
    return ReminderResultEntity(
        id = id ?: 0L,
        taskId = taskId,
        runAt = runAt,
        status = status.name,
        responseText = responseText,
        rawToolTrace = rawToolTrace,
        errorMessage = errorMessage
    )
}
