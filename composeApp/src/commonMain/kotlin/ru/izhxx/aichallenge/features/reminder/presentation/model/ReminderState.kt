package ru.izhxx.aichallenge.features.reminder.presentation.model

/**
 * UI-модель задачи для списка.
 */
data class ReminderUi(
    val id: Long,
    val name: String,
    val systemPrompt: String,
    val userPrompt: String,
    val periodUnit: PeriodUi,
    val periodValue: Int,
    val enabled: Boolean,
    val lastRunAt: Long?,
    val nextRunAt: Long?
)

/**
 * UI-модель результата выполнения задачи.
 */
data class ReminderResultUi(
    val id: Long,
    val taskId: Long,
    val runAt: Long,
    val status: String,
    val preview: String,
    val errorMessage: String?
)

/**
 * Состояние редактора задачи.
 */
data class EditorUiState(
    val id: Long? = null,
    val name: String = "",
    val systemPrompt: String = "",
    val userPrompt: String = "",
    val periodUnit: PeriodUi = PeriodUi.MINUTES,
    val periodValue: Int = 15,
    val enabled: Boolean = true
)

/**
 * Состояние экрана Reminder.
 */
data class ReminderState(
    val isLoading: Boolean = false,
    val tasks: List<ReminderUi> = emptyList(),
    val selectedTaskId: Long? = null,
    val resultsForSelected: List<ReminderResultUi> = emptyList(),
    val editor: EditorUiState? = null,
    val error: String? = null
)
