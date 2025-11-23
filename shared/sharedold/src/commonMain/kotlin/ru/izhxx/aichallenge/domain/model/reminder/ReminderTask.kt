package ru.izhxx.aichallenge.domain.model.reminder

/**
 * Доменная модель задачи-напоминания.
 *
 * Задача содержит промпты для LLM, расписание и служебные поля.
 */
data class ReminderTask(
    /**
     * Идентификатор задачи. Null для ещё не сохранённых задач.
     */
    val id: Long? = null,

    /**
     * Человекочитаемое имя задачи.
     */
    val name: String,

    /**
     * Системный промпт, который будет использоваться при выполнении задачи.
     */
    val systemPrompt: String,

    /**
     * Пользовательский промпт (запрос), который будет отправлен в LLM.
     */
    val userPrompt: String,

    /**
     * Единица периода запуска.
     */
    val periodUnit: ReminderPeriodUnit,

    /**
     * Значение периода (должно быть > 0).
     */
    val periodValue: Int,

    /**
     * Признак активности задачи.
     */
    val enabled: Boolean = true,

    /**
     * Метка времени последнего запуска (мс) или null, если ещё не запускалась.
     */
    val lastRunAt: Long? = null,

    /**
     * Метка времени следующего запуска (мс) или null, если задача выключена.
     */
    val nextRunAt: Long? = null,

    /**
     * Время создания (мс).
     */
    val createdAt: Long,

    /**
     * Время последнего обновления (мс).
     */
    val updatedAt: Long
)
