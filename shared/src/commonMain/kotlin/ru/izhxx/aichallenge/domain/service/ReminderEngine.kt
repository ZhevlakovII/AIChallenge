package ru.izhxx.aichallenge.domain.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import ru.izhxx.aichallenge.common.Logger
import ru.izhxx.aichallenge.domain.model.reminder.ReminderResult
import ru.izhxx.aichallenge.domain.model.reminder.ReminderStatus
import ru.izhxx.aichallenge.domain.model.reminder.ReminderTask
import ru.izhxx.aichallenge.domain.model.reminder.ReminderPeriodUnit
import ru.izhxx.aichallenge.domain.repository.ReminderRepository
import ru.izhxx.aichallenge.domain.usecase.ExecuteReminderTaskUseCase
import kotlin.math.max

/**
 * Движок напоминаний: параллельно исполняет задачи по расписанию.
 *
 * - Для каждой активной задачи создаёт отдельный job, который "спит" до nextRunAt,
 *   после чего выполняет задачу, сохраняет результат, пересчитывает nextRunAt и повторяет цикл.
 * - Параллелизм ограничивается семафором [maxParallelTasks].
 * - Обновление задач во время работы (включение/выключение/смена периода) возможно через [refresh()],
 *   который перечитает список задач и пересоздаст набор job'ов.
 */
class ReminderEngine(
    private val repository: ReminderRepository,
    private val executeReminderTask: ExecuteReminderTaskUseCase,
    private val notifier: ReminderNotifier,
    private val maxParallelTasks: Int = 3,
) {
    private val logger = Logger.forClass(this)

    private var scope: CoroutineScope? = null
    private val jobs: MutableMap<Long, Job> = mutableMapOf()
    private var semaphore = Semaphore(maxParallelTasks)

    /**
     * Запускает движок. Повторный вызов игнорируется.
     */
    fun start() {
        if (scope != null) {
            logger.i("ReminderEngine уже запущен")
            return
        }
        logger.i("Запуск ReminderEngine (maxParallel=$maxParallelTasks)")
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        // Инициализация первоначального набора задач
        scope!!.launch {
            setupTasksOnce()
        }
    }

    /**
     * Останавливает движок и все дочерние задачи.
     */
    suspend fun stop() {
        logger.i("Остановка ReminderEngine")
        val s = scope ?: return
        scope = null

        // Остановить все jobs
        jobs.values.forEach { it.cancel() }
        jobs.values.forEach { job ->
            runCatching { job.cancelAndJoin() }.onFailure { e ->
                logger.e("Ошибка при остановке job задачи", e)
            }
        }
        jobs.clear()

        // Остановить scope
        s.coroutineContext[Job]?.cancel()
    }

    /**
     * Перечитывает список задач и пересоздаёт job'ы (применение новых настроек/включений).
     */
    fun refresh() {
        val s = scope ?: return
        s.launch { setupTasksOnce() }
    }

    /**
     * Однократная настройка job'ов задач исходя из текущего состояния БД.
     */
    private suspend fun setupTasksOnce() {
        val s = scope ?: return
        val tasks = repository.getTasks()
            .onFailure { logger.e("Не удалось получить список задач reminder", it) }
            .getOrElse { emptyList() }

        val now = System.currentTimeMillis()

        // Создаём/обновляем job для каждой активной задачи
        tasks.forEach { task ->
            val id = task.id
            if (!task.enabled || id == null) {
                // Отключённые или без id — остановить job если был
                stopTaskJob(id)
                return@forEach
            }

            // Обновляем nextRunAt если он пуст или находится в прошлом/настоящем — чтобы избежать busy loop
            val nextRunAt = task.nextRunAt ?: (now + periodToMs(task.periodUnit, task.periodValue))
            if (task.nextRunAt == null) {
                repository.updateSchedule(id, task.lastRunAt, nextRunAt)
            }

            // Перезапустить job с актуальным состоянием
            restartTaskJob(task.copy(nextRunAt = nextRunAt))
        }

        // Остановить job'ы для задач, которых больше нет или они выключены
        val existingIds = tasks.mapNotNull { it.id }.toSet()
        val toRemove = jobs.keys.filter { it !in existingIds }
        toRemove.forEach { id -> stopTaskJob(id) }
    }

    private fun restartTaskJob(task: ReminderTask) {
        val id = requireNotNull(task.id)
        stopTaskJob(id)
        val s = scope ?: return
        jobs[id] = s.launch {
            runTaskLoop(taskId = id)
        }
        logger.i("Запущен job для задачи reminder id=$id")
    }

    private fun stopTaskJob(taskId: Long?) {
        if (taskId == null) return
        jobs.remove(taskId)?.let { job ->
            job.cancel()
            logger.i("Остановлен job задачи reminder id=$taskId")
        }
    }

    /**
     * Основной цикл исполнения одной задачи:
     * - ждёт до nextRunAt
     * - ограничивает параллелизм семафором
     * - выполняет use case, сохраняет результат, уведомляет и пересчитывает расписание
     * - повторяет цикл
     */
    private suspend fun runTaskLoop(taskId: Long) {
        val s = scope ?: return

        while (s.isActive) {
            // Получаем актуальное состояние задачи
            val task = repository.getTask(taskId)
                .onFailure { logger.e("Не удалось прочитать задачу id=$taskId", it) }
                .getOrNull()

            if (task == null) {
                logger.i("Задача id=$taskId не найдена, завершаем job")
                break
            }
            if (!task.enabled) {
                logger.i("Задача id=$taskId выключена, завершаем job")
                break
            }

            val now = System.currentTimeMillis()
            val targetTs = task.nextRunAt ?: (now + periodToMs(task.periodUnit, task.periodValue))
            val delayMs = max(0L, targetTs - now)

            if (delayMs > 0) {
                delay(delayMs)
                if (!s.isActive) break
            }

            // Ограничение параллелизма
            semaphore.withPermit {
                // Повторная проверка актуальности перед исполнением
                val current = repository.getTask(taskId).getOrNull()
                if (current == null || !current.enabled) {
                    logger.i("Задача id=$taskId отключена/удалена перед исполнением")
                    return@withPermit
                }

                // Выполнение и сохранение результата
                val result: ReminderResult = executeReminderTask(current)
                    .onFailure { logger.e("Ошибка ExecuteReminderTaskUseCase для taskId=$taskId", it) }
                    .getOrElse { err ->
                        ReminderResult(
                            id = null,
                            taskId = taskId,
                            runAt = System.currentTimeMillis(),
                            status = ReminderStatus.ERROR,
                            responseText = "",
                            rawToolTrace = null,
                            errorMessage = err.message
                        )
                    }

                val savedId = repository.saveResult(result)
                    .onFailure { logger.e("Не удалось сохранить результат taskId=$taskId", it) }
                    .getOrElse { -1L }

                // Уведомление пользователя
                if (savedId > 0L) {
                    val preview = result.responseText.take(160)
                    notifier.notifyResult(taskId, savedId, current.name, preview)
                }

                // Перерасчёт расписания
                val next = System.currentTimeMillis() + periodToMs(current.periodUnit, current.periodValue)
                repository.updateSchedule(taskId, lastRunAt = System.currentTimeMillis(), nextRunAt = next)
                    .onFailure { logger.e("Не удалось обновить расписание taskId=$taskId", it) }
            }
        }
    }

    /**
     * Переводит период в миллисекунды.
     */
    private fun periodToMs(unit: ReminderPeriodUnit, value: Int): Long {
        val v = value.coerceAtLeast(1)
        return when (unit) {
            ReminderPeriodUnit.MINUTES -> v * 60_000L
            ReminderPeriodUnit.HOURS -> v * 3_600_000L
            ReminderPeriodUnit.DAYS -> v * 86_400_000L
        }
    }
}
