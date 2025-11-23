package ru.izhxx.aichallenge.features.reminder.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.izhxx.aichallenge.common.Logger
import ru.izhxx.aichallenge.domain.model.reminder.ReminderPeriodUnit
import ru.izhxx.aichallenge.domain.model.reminder.ReminderResult
import ru.izhxx.aichallenge.domain.model.reminder.ReminderStatus
import ru.izhxx.aichallenge.domain.model.reminder.ReminderTask
import ru.izhxx.aichallenge.domain.repository.ReminderRepository
import ru.izhxx.aichallenge.domain.service.ReminderEngine
import ru.izhxx.aichallenge.domain.service.ReminderNotifier
import ru.izhxx.aichallenge.domain.usecase.ExecuteReminderTaskUseCase
import ru.izhxx.aichallenge.features.reminder.presentation.model.EditorUiState
import ru.izhxx.aichallenge.features.reminder.presentation.model.PeriodUi
import ru.izhxx.aichallenge.features.reminder.presentation.model.ReminderEvent
import ru.izhxx.aichallenge.features.reminder.presentation.model.ReminderResultUi
import ru.izhxx.aichallenge.features.reminder.presentation.model.ReminderState
import ru.izhxx.aichallenge.features.reminder.presentation.model.ReminderUi
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * ViewModel фичи Reminder (MVI).
 *
 * Управляет списком задач, редактором, запуском задач и чтением результатов.
 */
class ReminderViewModel(
    private val repository: ReminderRepository,
    private val executeReminderTask: ExecuteReminderTaskUseCase,
    private val notifier: ReminderNotifier,
    private val engine: ReminderEngine
) : ViewModel() {

    private val logger = Logger.forClass(this)

    private val _state = MutableStateFlow(ReminderState(isLoading = true))
    val state: StateFlow<ReminderState> = _state.asStateFlow()

    init {
        // Загружаем начальные данные и синхронизируем движок
        viewModelScope.launch {
            loadTasks()
            // Движок может быть уже запущен из Application, но refresh безопасен
            engine.refresh()
        }
    }

    fun processEvent(event: ReminderEvent) {
        when (event) {
            is ReminderEvent.Load -> viewModelScope.launch { loadTasks() }
            is ReminderEvent.OpenCreate -> openCreate()
            is ReminderEvent.OpenEdit -> openEdit(event.taskId)
            is ReminderEvent.CloseEditor -> closeEditor()
            is ReminderEvent.Save -> viewModelScope.launch { saveTask(event) }
            is ReminderEvent.Delete -> viewModelScope.launch { deleteTask(event.taskId) }
            is ReminderEvent.Toggle -> viewModelScope.launch { toggleTask(event.taskId, event.enabled) }
            is ReminderEvent.RunNow -> viewModelScope.launch { runNow(event.taskId) }
            is ReminderEvent.SelectTask -> viewModelScope.launch { selectTask(event.taskId) }
            is ReminderEvent.DismissError -> _state.update { it.copy(error = null) }
        }
    }

    // region Actions

    private suspend fun loadTasks() {
        _state.update { it.copy(isLoading = true) }
        val tasks = repository.getTasks()
            .onFailure { e -> logger.e("Не удалось получить задачи reminder", e) }
            .getOrElse { emptyList() }

        _state.update {
            it.copy(
                isLoading = false,
                tasks = tasks.map { t -> t.toUi() }
            )
        }

        // Если есть выбранная задача — перечитать её результаты
        state.value.selectedTaskId?.let { selectTask(it) }
    }

    private fun openCreate() {
        _state.update {
            it.copy(
                editor = EditorUiState(),
                error = null
            )
        }
    }

    private fun openEdit(taskId: Long) {
        val task = state.value.tasks.firstOrNull { it.id == taskId } ?: return
        _state.update {
            it.copy(
                editor = EditorUiState(
                    id = task.id,
                    name = task.name,
                    systemPrompt = task.systemPrompt,
                    userPrompt = task.userPrompt,
                    periodUnit = task.periodUnit,
                    periodValue = task.periodValue,
                    enabled = task.enabled
                ),
                error = null
            )
        }
    }

    private fun closeEditor() {
        _state.update { it.copy(editor = null) }
    }

    private suspend fun saveTask(event: ReminderEvent.Save) {
        _state.update { it.copy(isLoading = true, error = null) }
        val now = System.currentTimeMillis()
        val domain = ReminderTask(
            id = event.id,
            name = event.name.trim(),
            systemPrompt = event.systemPrompt.trim(),
            userPrompt = event.userPrompt.trim(),
            periodUnit = event.periodUnit.toDomain(),
            periodValue = event.periodValue.coerceAtLeast(1),
            enabled = event.enabled,
            lastRunAt = null.takeIf { event.id == null }, // не трогаем если обновление
            nextRunAt = null, // пересчитается движком при refresh()
            createdAt = now,
            updatedAt = now
        )

        val result = if (domain.id == null) repository.createTask(domain) else repository.updateTask(domain).map { domain.id!! }

        result.fold(
            onSuccess = {
                logger.i("Задача reminder сохранена id=$it")
                _state.update { st -> st.copy(editor = null) }
                loadTasks()
                engine.refresh()
            },
            onFailure = { e ->
                logger.e("Ошибка сохранения задачи reminder", e)
                _state.update { it.copy(isLoading = false, error = e.message ?: "Ошибка сохранения задачи") }
            }
        )
    }

    private suspend fun deleteTask(taskId: Long) {
        _state.update { it.copy(isLoading = true, error = null) }
        repository.deleteTask(taskId).fold(
            onSuccess = {
                selectFallbackAfterDelete(taskId)
                loadTasks()
                engine.refresh()
            },
            onFailure = { e ->
                logger.e("Ошибка удаления задачи reminder", e)
                _state.update { it.copy(isLoading = false, error = e.message ?: "Ошибка удаления задачи") }
            }
        )
    }

    private fun selectFallbackAfterDelete(taskId: Long) {
        if (state.value.selectedTaskId == taskId) {
            _state.update { it.copy(selectedTaskId = null, resultsForSelected = emptyList()) }
        }
    }

    private suspend fun toggleTask(taskId: Long, enabled: Boolean) {
        val orig = repository.getTask(taskId).getOrNull() ?: return
        val updated = orig.copy(enabled = enabled, updatedAt = System.currentTimeMillis())
        repository.updateTask(updated).onFailure { e -> logger.e("Ошибка переключения задачи", e) }
        loadTasks()
        engine.refresh()
    }

    private suspend fun runNow(taskId: Long) {
        val task = repository.getTask(taskId).getOrNull() ?: return
        val execResult = executeReminderTask(task)
        val final: ReminderResult = execResult.getOrElse { e ->
            ReminderResult(
                id = null,
                taskId = taskId,
                runAt = System.currentTimeMillis(),
                status = ReminderStatus.ERROR,
                responseText = "",
                rawToolTrace = null,
                errorMessage = e.message
            )
        }
        val savedId = repository.saveResult(final).getOrElse { -1L }
        if (savedId > 0) {
            notifier.notifyResult(taskId, savedId, task.name, (final.responseText.takeIf { it.isNotBlank() } ?: (final.errorMessage ?: ""))).also {
                selectTask(taskId)
            }
        }
    }

    private suspend fun selectTask(taskId: Long) {
        _state.update { it.copy(selectedTaskId = taskId, isLoading = true) }
        // Простая выборка последних 50 результатов
        val results = repository.getResults(taskId, limit = 50, offset = 0)
            .onFailure { e -> logger.e("Не удалось получить результаты для taskId=$taskId", e) }
            .getOrElse { emptyList() }

        _state.update {
            it.copy(
                isLoading = false,
                resultsForSelected = results.map { r -> r.toUi() }
            )
        }
    }

    // endregion

    // region Mapping

    private fun ReminderTask.toUi(): ReminderUi = ReminderUi(
        id = requireNotNull(id),
        name = name,
        systemPrompt = systemPrompt,
        userPrompt = userPrompt,
        periodUnit = periodUnit.toUi(),
        periodValue = periodValue,
        enabled = enabled,
        lastRunAt = lastRunAt?.let { formatDateTime(it) },
        nextRunAt = nextRunAt?.let { formatDateTime(it) }
    )

    private fun ReminderPeriodUnit.toUi(): PeriodUi = when (this) {
        ReminderPeriodUnit.MINUTES -> PeriodUi.MINUTES
        ReminderPeriodUnit.HOURS -> PeriodUi.HOURS
        ReminderPeriodUnit.DAYS -> PeriodUi.DAYS
    }

    private fun PeriodUi.toDomain(): ReminderPeriodUnit = when (this) {
        PeriodUi.MINUTES -> ReminderPeriodUnit.MINUTES
        PeriodUi.HOURS -> ReminderPeriodUnit.HOURS
        PeriodUi.DAYS -> ReminderPeriodUnit.DAYS
    }

    private fun ReminderResult.toUi(): ReminderResultUi =
        ReminderResultUi(
            id = requireNotNull(id),
            taskId = taskId,
            runAt = formatDateTime(runAt),
            status = status.name,
            preview = if (responseText.isNotBlank()) responseText.take(200) else (errorMessage ?: ""),
            errorMessage = errorMessage
        )

    @OptIn(ExperimentalTime::class)
    private fun formatDateTime(epochMillis: Long): String {
        val dt = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
        return "${pad2(dt.dayOfMonth)}.${pad2(dt.monthNumber)}.${dt.year} ${pad2(dt.hour)}:${pad2(dt.minute)}"
    }

    private fun pad2(v: Int): String = v.toString().padStart(2, '0')

    // endregion
}
