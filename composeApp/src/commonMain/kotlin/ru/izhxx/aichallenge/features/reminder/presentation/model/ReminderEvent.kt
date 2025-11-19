package ru.izhxx.aichallenge.features.reminder.presentation.model

/**
 * События MVI для экрана Reminder.
 */
sealed class ReminderEvent {
    data object Load : ReminderEvent()

    data object OpenCreate : ReminderEvent()
    data class OpenEdit(val taskId: Long) : ReminderEvent()
    data object CloseEditor : ReminderEvent()

    data class Save(
        val id: Long? = null,
        val name: String,
        val systemPrompt: String,
        val userPrompt: String,
        val periodUnit: PeriodUi,
        val periodValue: Int,
        val enabled: Boolean
    ) : ReminderEvent()

    data class Toggle(val taskId: Long, val enabled: Boolean) : ReminderEvent()
    data class Delete(val taskId: Long) : ReminderEvent()
    data class RunNow(val taskId: Long) : ReminderEvent()
    data class SelectTask(val taskId: Long) : ReminderEvent()

    data object DismissError : ReminderEvent()
}

/**
 * UI-модель единицы периода.
 */
enum class PeriodUi {
    MINUTES, HOURS, DAYS
}
